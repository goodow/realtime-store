/*
 * Copyright 2014 Goodow.com
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.goodow.realtime.store.server.impl;

import com.goodow.realtime.store.channel.Constants.Key;

import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.impl.DefaultFutureResult;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Container;

import java.util.List;

import javax.inject.Inject;

public class ElasticSearchDriver {
  private static final String DEFAULT_SEARCH_ADDRESS = "realtime.search";
  private static final long REPLY_TIMEOUT = 15 * 1000;
  private static final String INDEX = "realtime";
  private static final String _SNAPSHOT = "_snapshot";
  private static final String _OP = "_op";
  private static final String DOC_ID = "docId";
  static final String ROOT = "root";

  private final String address;
  private final EventBus eb;

  @Inject
  ElasticSearchDriver(Vertx vertx, final Container container) {
    eb = vertx.eventBus();
    address = container.config().getString("persistor_address", DEFAULT_SEARCH_ADDRESS);
  }

  public void getOps(String docType, String docId, long from, long to,
      final AsyncResultHandler<JsonArray> callback) {
    JsonObject termFilter =
        new JsonObject().putObject("term", new JsonObject().putString(DOC_ID, docId));
    JsonObject range = new JsonObject();
    JsonObject rangeFilter =
        new JsonObject().putObject("range", new JsonObject().putObject(Key.VERSION, range));
    range.putNumber("gte", from);
    if (to != -1) {
      range.putNumber("lt", to);
    }
    JsonObject filter =
        new JsonObject().putArray("and", new JsonArray().add(termFilter).add(rangeFilter));
    JsonObject sort =
        new JsonObject().putObject(Key.VERSION, new JsonObject()
            .putBoolean("ignore_unmapped", true));
    JsonObject search =
        new JsonObject().putString("action", "search").putString("_index", INDEX).putString(
            "_type", getOpsType(docType)).putObject(
            "source",
            new JsonObject().putObject("sort", sort).putNumber("size", 1000).putObject(
                "filter", filter));

    eb.sendWithTimeout(address, search, REPLY_TIMEOUT,
        new Handler<AsyncResult<Message<JsonObject>>>() {
          @SuppressWarnings("unchecked")
          @Override
          public void handle(AsyncResult<Message<JsonObject>> ar) {
            DefaultFutureResult<JsonArray> result =
                new DefaultFutureResult<JsonArray>().setHandler(callback);
            if (ar.failed()) {
              result.setFailure(ar.cause());
              return;
            }
            JsonArray ops = ar.result().body().getObject("hits").getArray("hits");
            for (Object op : ops) {
              JsonObject opData = (JsonObject) op;
              opData.putArray(Key.OP, new JsonArray((List<Object>) opData.removeField(_OP)));
            }
            result.setResult(ops);
          }
        });
  }

  /**
   * callback called with {v:, snapshot:[], root:{}} or null
   */
  public void getSnapshot(String docType, final String docId,
      final AsyncResultHandler<JsonObject> callback) {
    JsonObject get =
        new JsonObject().putString("action", "get").putString("_index", INDEX).putString("_type",
            docType).putString("_id", docId);
    eb.sendWithTimeout(address, get, REPLY_TIMEOUT, new AsyncResultHandler<Message<JsonObject>>() {
      @Override
      public void handle(AsyncResult<Message<JsonObject>> ar) {
        DefaultFutureResult<JsonObject> result =
            new DefaultFutureResult<JsonObject>().setHandler(callback);
        if (ar.failed()) {
          result.setFailure(ar.cause());
          return;
        }
        result.setResult(castToSnapshotData(ar.result().body()));
      }
    });
  }

  public void getVersion(String docType, String docId, final AsyncResultHandler<Long> callback) {
    JsonObject filter =
        new JsonObject().putObject("term", new JsonObject().putString(DOC_ID, docId));

    JsonObject sort =
        new JsonObject().putObject(Key.VERSION, new JsonObject().putString("order", "desc")
            .putBoolean("ignore_unmapped", true));
    JsonObject source =
        new JsonObject().putNumber("size", 1).putObject("sort", sort).putObject("filter", filter);
    JsonObject search =
        new JsonObject().putString("action", "search").putString("_index", INDEX).putString(
            "_type", getOpsType(docType)).putObject("source", source);

    eb.sendWithTimeout(address, search, REPLY_TIMEOUT,
        new Handler<AsyncResult<Message<JsonObject>>>() {
          @Override
          public void handle(AsyncResult<Message<JsonObject>> ar) {
            DefaultFutureResult<Long> result = new DefaultFutureResult<Long>().setHandler(callback);
            if (ar.failed()) {
              result.setFailure(ar.cause());
              return;
            }
            JsonObject body = ar.result().body();
            long v = 0;
            if (body.getObject("hits").getLong("total") != 0) {
              v =
                  body.getObject("hits").getArray("hits").<JsonObject> get(0).getObject("_source").getLong(Key.VERSION) + 1;
            }
            result.setResult(v);
          }
        });
  }

  /**
   * @param opData {v:, op:, sid:, seq:, uid:}
   */
  @SuppressWarnings("unchecked")
  public void writeOp(String docType, String docId, JsonObject opData,
      final AsyncResultHandler<Void> callback) {
    Long v = opData.getLong(Key.VERSION);
    assert v != null;
    JsonObject index =
        new JsonObject().putString("action", "index").putString("_index", INDEX).putString("_type",
            getOpsType(docType)).putString("_id", getOpId(docId, v)).putString("op_type", "create")
            .putBoolean("refresh", true).putObject("source",
                opData.putString(DOC_ID, docId).putArray(_OP,
                    new JsonArray((List<Object>) opData.removeField(Key.OP))));
    eb.sendWithTimeout(address, index, REPLY_TIMEOUT,
        new Handler<AsyncResult<Message<JsonObject>>>() {
          @Override
          public void handle(AsyncResult<Message<JsonObject>> ar) {
            handleVoidCallback(callback, ar);
          }
        });
  }

  /**
   * @param snapshotData {v:, snapshot:[], root:{}}
   */
  public void writeSnapshot(String docType, String docId, JsonObject snapshotData,
      final AsyncResultHandler<Void> callback) {
    JsonObject source =
        snapshotData.getObject(ROOT).putArray(_SNAPSHOT, snapshotData.getArray("snapshot"));
    JsonObject index =
        new JsonObject().putString("action", "index").putString("_index", INDEX).putString("_type",
            docType).putString("_id", docId).putString("version_type", "external").putNumber(
            "version", snapshotData.getLong(Key.VERSION)).putObject("source", source);
    eb.sendWithTimeout(address, index, REPLY_TIMEOUT,
        new Handler<AsyncResult<Message<JsonObject>>>() {
          @Override
          public void handle(AsyncResult<Message<JsonObject>> ar) {
            handleVoidCallback(callback, ar);
          }
        });
  }

  protected String getOpId(String docId, long v) {
    return docId + "_v" + v;
  }

  protected String getOpsType(String docType) {
    return docType + "_ops";
  }

  @SuppressWarnings("unchecked")
  private JsonObject castToSnapshotData(JsonObject body) {
    JsonObject source = body.getObject("_source");
    return !body.getBoolean("found") ? null : new JsonObject().putNumber(Key.VERSION,
        body.getLong("_version")).putArray("snapshot",
        new JsonArray((List<Object>) source.removeField(_SNAPSHOT))).putObject(ROOT, source);
  }

  private void handleVoidCallback(final AsyncResultHandler<Void> callback,
      AsyncResult<Message<JsonObject>> ar) {
    DefaultFutureResult<Void> result = new DefaultFutureResult<Void>().setHandler(callback);
    if (ar.failed()) {
      result.setFailure(ar.cause());
      return;
    }
    result.setResult(null);
  }
}
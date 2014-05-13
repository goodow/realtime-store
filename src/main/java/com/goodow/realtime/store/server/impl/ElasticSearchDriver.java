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

import javax.inject.Inject;

public class ElasticSearchDriver {
  private static final String DEFAULT_SEARCH_ADDRESS = "realtime.search";
  private static final long REPLY_TIMEOUT = 1 * 1000;
  private static final String INDEX = "realtime";
  private static final String _SNAPSHOT = "_snapshot";
  private static final String SNAPSHOT_ID = "snapshotId";

  private final String address;
  private final EventBus eb;

  @Inject
  ElasticSearchDriver(Vertx vertx, final Container container) {
    eb = vertx.eventBus();
    address = container.config().getString("persistor_address", DEFAULT_SEARCH_ADDRESS);
  }

  public void getOps(String type, String id, long from, long to,
      final AsyncResultHandler<JsonArray> callback) {
    // http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/query-dsl-range-filter.html
    JsonObject query =
        new JsonObject().putObject("term", new JsonObject().putString("snapshot", id));
    JsonObject range = new JsonObject();
    JsonObject filter = new JsonObject().putObject("range", new JsonObject().putObject("v", range));
    if (from != -1) {
      range.putNumber("gte", from);
    }
    if (to != -1) {
      range.putNumber("lt", to);
    }

    JsonObject search =
        new JsonObject().putString("action", "search").putString("_index", INDEX).putString(
            "_type", getOpsType(type)).putObject(
            "source",
            new JsonObject().putString("sort", "v").putObject("filtered",
                new JsonObject().putObject("query", query).putObject("filter", filter)));

    eb.sendWithTimeout(address, search, REPLY_TIMEOUT,
        new Handler<AsyncResult<Message<JsonObject>>>() {
          @Override
          public void handle(AsyncResult<Message<JsonObject>> ar) {
            DefaultFutureResult<JsonArray> result =
                new DefaultFutureResult<JsonArray>().setHandler(callback);
            if (ar.failed()) {
              result.setFailure(ar.cause());
              return;
            }
            JsonObject body = ar.result().body();
            result.setResult(body.getObject("hits").getArray("hits"));
          }
        });
  }

  /**
   * callback called with {v:, snapshot:[], root:{}}
   */
  public void getSnapshot(String type, final String id,
      final AsyncResultHandler<JsonObject> callback) {
    JsonObject get =
        new JsonObject().putString("action", "get").putString("_index", INDEX).putString("_type",
            type).putString("_id", id);
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

  public void getVersion(String type, String id, final AsyncResultHandler<Long> callback) {
    JsonObject filter =
        new JsonObject().putObject("term", new JsonObject().putString(SNAPSHOT_ID, id));
    JsonObject query =
        new JsonObject().putObject("constant_score", new JsonObject().putObject("filter", filter));

    JsonObject sort = new JsonObject().putString("v", "desc");
    JsonObject source =
        new JsonObject().putNumber("size", 1).putObject("sort", sort).putObject("query", query);
    JsonObject search =
        new JsonObject().putString("action", "search").putString("_index", INDEX).putString(
            "_type", getOpsType(type)).putObject("source", source);

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
            if (body.getObject("hits").getLong("total") == 1) {
              v = body.getObject("hits").getArray("hits").<JsonObject> get(0).getLong("v") + 1;
            }
            result.setResult(v);
          }
        });
  }

  /**
   * @param opData {v:, op:, sid:, seq:, uid:}
   */
  public void writeOp(String type, String id, JsonObject opData,
      final AsyncResultHandler<Void> callback) {
    Long v = opData.getLong("v");
    assert v != null;
    JsonObject index =
        new JsonObject().putString("action", "index").putString("_index", INDEX).putString("_type",
            getOpsType(type)).putString("_id", getOpId(id, v)).putString("op_type", "create")
            .putObject("source", opData.putString(SNAPSHOT_ID, id));
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
  public void writeSnapshot(String type, String id, JsonObject snapshotData,
      final AsyncResultHandler<Void> callback) {
    JsonObject source =
        snapshotData.getObject("root").putArray(_SNAPSHOT, snapshotData.getArray("snapshot"));
    JsonObject index =
        new JsonObject().putString("action", "index").putString("_index", INDEX).putString("_type",
            type).putString("_id", id).putString("version_type", "external").putNumber("version",
            snapshotData.getLong("v")).putObject("source", source);
    eb.sendWithTimeout(address, index, REPLY_TIMEOUT,
        new Handler<AsyncResult<Message<JsonObject>>>() {
          @Override
          public void handle(AsyncResult<Message<JsonObject>> ar) {
            handleVoidCallback(callback, ar);
          }
        });
  }

  protected String getOpId(String snapshotId, long v) {
    return snapshotId + "_v" + v;
  }

  protected String getOpsType(String snapshotType) {
    return snapshotType + "_ops";
  }

  private JsonObject castToSnapshotData(JsonObject body) {
    JsonObject source = body.getObject("_source");
    return new JsonObject().putString("id", body.getString("_id")).putNumber("v",
        body.getLong("_version")).putArray("snapshot", (JsonArray) source.removeField(_SNAPSHOT))
        .putObject("root", source);
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
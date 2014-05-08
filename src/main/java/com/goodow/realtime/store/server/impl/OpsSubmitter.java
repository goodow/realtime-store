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

import com.goodow.realtime.json.impl.JreJsonArray;
import com.goodow.realtime.json.impl.JreJsonObject;
import com.goodow.realtime.operation.Transformer;
import com.goodow.realtime.operation.impl.CollaborativeOperation;
import com.goodow.realtime.store.DocumentBridge;

import com.google.inject.Inject;

import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.eventbus.ReplyException;
import org.vertx.java.core.eventbus.ReplyFailure;
import org.vertx.java.core.impl.DefaultFutureResult;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class OpsSubmitter {
  @Inject private RedisDriver redis;
  @Inject private ElasticSearchDriver persistence;
  @Inject private Transformer<CollaborativeOperation> transformer;

  /**
   * opData should probably contain a v: field (if it doesn't, it defaults to the current version).
   * 
   * callback called with {v:, ops:, snapshot:}
   */
  public void submit(String type, String id, final JsonObject opData,
      final AsyncResultHandler<JsonObject> callback) {
    retrySubmit(type, id, opData, callback);
  }

  private CollaborativeOperation createOperation(JsonObject opData) {
    return transformer.createOperation(new JreJsonObject(opData.toMap()));
  }

  @SuppressWarnings("unchecked")
  private DocumentBridge createSnapshot(final String type, final String id, JsonObject snapshotData) {
    JreJsonArray snapshot =
        snapshotData.containsField("snapshot") ? new JreJsonArray(snapshotData.getArray("snapshot")
            .toList()) : null;
    final DocumentBridge bridge = new DocumentBridge(null, type + "/" + id, snapshot, null);
    return bridge;
  }

  /**
   * This is a fetch that doesn't check the oplog to see if the snapshot is out of date. It will be
   * higher performance, but in some error conditions it may return an outdated snapshot.
   */
  private void lazyFetch(String type, String id, final AsyncResultHandler<JsonObject> callback) {
    persistence.getSnapshot(type, id, new AsyncResultHandler<JsonObject>() {
      @Override
      public void handle(AsyncResult<JsonObject> ar) {
        if (ar.succeeded() && ar.result() == null) {
          callback.handle(new DefaultFutureResult<JsonObject>(new JsonObject().putNumber("v", 0)));
          return;
        }
        callback.handle(ar);
      }
    });
  }

  @SuppressWarnings("unchecked")
  private void retrySubmit(final String type, final String id, final JsonObject opData,
      final AsyncResultHandler<JsonObject> callback) {
    // First we'll get a doc snapshot. This wouldn't be necessary except that we need to check that
    // the operation is valid against the current document before accepting it.
    lazyFetch(type, id, new AsyncResultHandler<JsonObject>() {
      @Override
      public void handle(AsyncResult<JsonObject> ar) {
        if (ar.failed()) {
          callback.handle(ar);
          return;
        }
        final JsonObject snapshotData = ar.result();
        final DocumentBridge snapshot = createSnapshot(type, id, snapshotData);
        // Get all operations that might be relevant. We'll float the snapshot and the operation up
        // to the most recent version of the document, then try submitting.
        final Long opVersion = opData.getLong("v");
        final long from =
            opVersion != null ? Math.min(snapshotData.getLong("v"), opVersion) : snapshotData
                .getLong("v");
        redis.getOps(type, id, from, null, new AsyncResultHandler<JsonObject>() {
          @Override
          public void handle(AsyncResult<JsonObject> ar) {
            if (ar.failed()) {
              callback.handle(ar);
              return;
            }
            List<JsonObject> transformedOps = new ArrayList<JsonObject>();
            JsonArray ops = ar.result().getArray("ops");
            int transformStart =
                opVersion == null ? ops.size() : from == opVersion ? 0
                    : (int) (opVersion - snapshotData.getLong("v"));
            for (int i = 0, len = ops.size(); i < len; i++) {
              JsonObject op = ops.get(i);
              if (opData.containsField("seq") && opData.getString("sid") == op.getString("sid")
                  && opData.getLong("seq") == op.getLong("seq")) {
                // The op has already been submitted. There's a variety of ways this can happen. Its
                // important we don't transform it by itself & submit again.
                callback.handle(new DefaultFutureResult<JsonObject>(new ReplyException(
                    ReplyFailure.RECIPIENT_FAILURE, "Op already submitted")));
                return;
              }

              // Bring both the op and the snapshot up to date. At least one of these two
              // conditionals should be true.
              if (snapshotData.getLong("v").longValue() == op.getLong("v")) {
                try {
                  snapshot.consume(createOperation(op));
                } catch (Exception e) {
                  callback.handle(new DefaultFutureResult<JsonObject>(e));
                  return;
                }
                snapshotData.putNumber("v", snapshotData.getLong("v") + 1);
              }
              if (i >= transformStart) {
                transformedOps.add(op);
              }
            }
            if (transformStart < ops.size()) {
              try {
                CollaborativeOperation applied =
                    transformer.compose(new JreJsonArray(ops.toList().subList(transformStart,
                        ops.size())));
                CollaborativeOperation transformed =
                    createOperation(opData).transform(applied, false);
              } catch (Exception e) {
                callback.handle(new DefaultFutureResult<JsonObject>(e));
                return;
              }
            }
          }

        });
      }

    });
  }

  private void writeSnapshotAfterSubmit(String type, String id, JsonObject snapshot,
      JsonObject opData, AsyncResultHandler<Void> callback) {
    persistence.writeSnapshot(type, id, snapshot, callback);
  }
}

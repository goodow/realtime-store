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

public class OpSubmitter {
  @Inject private RedisDriver redis;
  @Inject private ElasticSearchDriver persistor;
  @Inject private Transformer<CollaborativeOperation> transformer;

  /**
   * opData should probably contain a v: field (if it doesn't, it defaults to the current version).
   * 
   * callback called with {v:, ops:, snapshot:}
   */
  public void submit(String type, String id, final JsonObject opData,
      final AsyncResultHandler<JsonObject> callback) {
    retrySubmit(new ArrayList<Object>(), type, id, createOperation(opData), opData.getLong("v"),
        callback);
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
   * Great - now we're in the situation that we can actually submit the operation to the database.
   * If this method succeeds, it should update any persistant oplogs before calling the callback to
   * tell us about the successful commit. I could make this API more complicated, enabling the
   * function to return actual operations and whatnot, but its quite rare to actually need to
   * transform data on the server at this point.
   */
  private void doSubmit(final List<Object> transformedOps, final String type, final String id,
      final CollaborativeOperation operation, final long applyAt, final DocumentBridge snapshot,
      final AsyncResultHandler<JsonObject> callback) {
    final JsonObject opData =
        new JsonObject(((JreJsonObject) operation.toJson()).toNative()).putNumber("v", applyAt);
    redis.atomicSubmit(type, id, opData, new AsyncResultHandler<Void>() {
      @Override
      public void handle(AsyncResult<Void> ar) {
        if (ar.failed()) {
          if ("Transform needed".equals(ar.cause().getMessage())) {
            retrySubmit(transformedOps, type, id, operation, applyAt, callback);
          } else {
            callback.handle(new DefaultFutureResult<JsonObject>(ar.cause()));
          }
          return;
        }
        final JsonObject root = new JsonObject(((JreJsonObject) snapshot.toJson()).toNative());
        JsonObject snapshotData =
            new JsonObject().putNumber("v", applyAt + 1).putObject("root", root).putArray(
                "snapshot", new JsonArray(((JreJsonArray) snapshot.toSnapshot()).toNative()));
        writeSnapshotAfterSubmit(type, id, snapshotData, opData, new AsyncResultHandler<Void>() {
          @Override
          public void handle(AsyncResult<Void> ar) {
            // What do we do if the snapshot write fails? We've already committed the operation -
            // its done and dusted. We probably shouldn't re-run polling queries now. Really, no
            // matter what we do here things are going to be a little bit broken, depending on the
            // behaviour we trap in finish.

            // Its sort of too late to error out if the snapshotdb can't take our op - the op has
            // been commited.

            // postSubmit is for things like publishing the operation over pubsub. We should
            // probably make this asyncronous.
            redis.postSubmit(type, id, opData, root);
            callback
                .handle(new DefaultFutureResult<JsonObject>(new JsonObject()
                    .putNumber("v", applyAt).putArray("ops", new JsonArray(transformedOps))
                    .putObject("snapshot", root)));
          }
        });
      }
    });
  }

  /**
   * This is a fetch that doesn't check the oplog to see if the snapshot is out of date. It will be
   * higher performance, but in some error conditions it may return an outdated snapshot.
   */
  private void lazyFetch(String type, String id, final AsyncResultHandler<JsonObject> callback) {
    persistor.getSnapshot(type, id, new AsyncResultHandler<JsonObject>() {
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
  private void retrySubmit(final List<Object> transformedOps, final String type, final String id,
      final CollaborativeOperation operation, final Long applyAt,
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
        final long snapshotVersion = snapshotData.getLong("v");
        // Get all operations that might be relevant. We'll float the snapshot and the operation up
        // to the most recent version of the document, then try submitting.
        final long from = applyAt != null ? Math.min(snapshotVersion, applyAt) : snapshotVersion;
        redis.getOps(type, id, from, null, new AsyncResultHandler<JsonObject>() {
          @Override
          public void handle(AsyncResult<JsonObject> ar) {
            if (ar.failed()) {
              callback.handle(ar);
              return;
            }
            final DocumentBridge snapshot = createSnapshot(type, id, snapshotData);
            JsonArray ops = ar.result().getArray("ops");
            long snapshotV = snapshotVersion;
            long opV = applyAt == null ? snapshotV + ops.size() : applyAt;
            for (Object op : ops) {
              JsonObject opData = (JsonObject) op;
              // if (opData.containsField("seq") && opData.getString("sid") == op.getString("sid")
              // && opData.getLong("seq") == op.getLong("seq")) {
              // // The op has already been submitted. There's a variety of ways this can happen.
              // Its
              // // important we don't transform it by itself & submit again.
              // callback.handle(new DefaultFutureResult<JsonObject>(new ReplyException(
              // ReplyFailure.RECIPIENT_FAILURE, "Op already submitted")));
              // return;
              // }

              // Bring both the op and the snapshot up to date. At least one of these two
              // conditionals should be true.
              long opVersion = opData.getLong("v");
              if (snapshotV == opVersion) {
                try {
                  snapshot.consume(createOperation(opData));
                } catch (Exception e) {
                  callback.handle(new DefaultFutureResult<JsonObject>(new ReplyException(
                      ReplyFailure.RECIPIENT_FAILURE, e.getMessage())));
                  return;
                }
                snapshotV++;
              }
              if (opV == opVersion) {
                transformedOps.add(opData);
                opV++;
              }
            }
            if (opV != snapshotV) {
              callback.handle(new DefaultFutureResult<JsonObject>(new ReplyException(
                  ReplyFailure.RECIPIENT_FAILURE, "Invalid opData version")));
              return;
            }
            CollaborativeOperation transformed = operation;
            if (applyAt != null && ops.size() > 0
                && applyAt <= ops.<JsonObject> get(ops.size() - 1).getLong("v")) {
              try {
                CollaborativeOperation applied =
                    transformer.compose(new JreJsonArray(ops.toList().subList(
                        (int) (applyAt - ops.<JsonObject> get(0).getLong("v")), ops.size())));
                transformed = operation.transform(applied, false);
              } catch (Exception e) {
                callback.handle(new DefaultFutureResult<JsonObject>(new ReplyException(
                    ReplyFailure.RECIPIENT_FAILURE, e.getMessage())));
                return;
              }
            }

            // Ok, now we can try to apply the op.
            try {
              snapshot.consume(transformed);
            } catch (Exception e) {
              callback.handle(new DefaultFutureResult<JsonObject>(new ReplyException(
                  ReplyFailure.RECIPIENT_FAILURE, e.getMessage())));
              return;
            }
            doSubmit(transformedOps, type, id, transformed, opV, snapshot, callback);
          }
        });
      }
    });
  }

  private void writeSnapshotAfterSubmit(String type, String id, JsonObject snapshotData,
      JsonObject opData, AsyncResultHandler<Void> callback) {
    persistor.writeSnapshot(type, id, snapshotData, callback);
  }
}

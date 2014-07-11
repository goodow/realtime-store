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

import com.google.inject.Inject;

import com.goodow.realtime.json.impl.JreJsonArray;
import com.goodow.realtime.json.impl.JreJsonObject;
import com.goodow.realtime.operation.Transformer;
import com.goodow.realtime.operation.impl.CollaborativeOperation;
import com.goodow.realtime.store.channel.Constants.Key;
import com.goodow.realtime.store.channel.Constants.Topic;
import com.goodow.realtime.store.impl.DocumentBridge;
import com.goodow.realtime.store.server.DeltaStorage;

import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.ReplyException;
import org.vertx.java.core.eventbus.ReplyFailure;
import org.vertx.java.core.impl.CountingCompletionHandler;
import org.vertx.java.core.impl.DefaultFutureResult;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Container;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This is an in-memory delta storage.
 *
 * Its main use is as an API example for people implementing storage adaptors.
 * This storage is fully functional, except it stores all documents and operations forever in
 * memory. As such, memory usage will grow without bound, it doesn't scale across multiple node
 * processes and you'll lose all your data if the server restarts. Use with care.
 */
public class MemoryDeltaStorage implements DeltaStorage {
  public static String getDocIdChannel(String prefix, String docType, String docId) {
    return prefix + "/" + docType + "/" + docId + Topic.WATCH;
  }

  public static String getDocTypeChannel(String prefix, String docType) {
    return prefix + "/" + docType + Topic.WATCH;
  }

  private static final Logger log  = Logger.getLogger(MemoryDeltaStorage.class.getName());

  private final EventBus eb;
  private final String address;
  @Inject private Transformer<CollaborativeOperation> transformer;
  // Map from collection docType -> docId -> snapshotData ({v:, snapshot:[], root:{}})
  private Map<String, Map<String, JsonObject>> snapshotDatas =
      new HashMap<String, Map<String, JsonObject>>();
  // Map from collection docType -> docId -> list of opData ({v:, op:[], sid:, uid:}).
  // Operations' version is simply the index in the list.
  private Map<String, Map<String, List<JsonObject>>> opDatas =
      new HashMap<String, Map<String, List<JsonObject>>>();

  // Cache of docType/docId -> current doc version. This is needed because there's a potential race
  // condition where getOps could be missing an operation thats just been processed and as a result
  // we'll accept the same op for the same document twice. Data in here should be cleared out
  // periodically (like, 15 seconds after nobody has submitted to the document), but that logic
  // hasn't been implemented yet.
  private Map<String, Long> versions = new HashMap<String, Long>();

  @Inject
  MemoryDeltaStorage(Vertx vertx, Container container) {
    eb = vertx.eventBus();
    address = container.config().getObject("realtime_store", new JsonObject())
        .getString("address", Topic.STORE);
  }

  @Override
  public void start(CountingCompletionHandler<Void> countDownLatch) {
  }

  @Override
  public void getSnapshot(final String docType, final String docId, final Long version,
                          final AsyncResultHandler<JsonObject> callback) {
    if (version == null) {
      JsonObject snapshotData = snapshotDatas.containsKey(docType)
                                ? snapshotDatas.get(docType).get(docId) : null;
      callback.handle(new DefaultFutureResult<JsonObject>(snapshotData));
      return;
    }

    getOps(docType, docId, 0L, Math.min(version, getSnapshotVersion(docType, docId))
        , new AsyncResultHandler<JsonObject>() {
      @Override
      public void handle(AsyncResult<JsonObject> ar) {
        if (ar.failed()) {
          callback.handle(ar);
          return;
        }
        JsonObject snapshotData = new JsonObject().putNumber(Key.VERSION, 0);
        DocumentBridge snapshot = OperationProcessor.createSnapshot(docType, docId, snapshotData);
        JsonArray ops = ar.result().getArray(Key.OPS);
        Long opVersion = null;
        for (Object op : ops) {
          JsonObject opData = (JsonObject) op;
          opVersion = opData.getLong(Key.VERSION);
          try {
            snapshot.consume(transformer.createOperation(new JreJsonObject(opData.toMap())));
          } catch (Exception e) {
            log.log(Level.WARNING, "Failed to consume operation", e);
            callback.handle(new DefaultFutureResult<JsonObject>(new ReplyException(
                ReplyFailure.RECIPIENT_FAILURE, e.getMessage())));
            return;
          }
        }
        JsonObject root = new JsonObject(((JreJsonObject) snapshot.toJson()).toNative());
        snapshotData.putNumber(Key.VERSION, opVersion + 1).putObject(DeltaStorage.ROOT, root).
           putArray(Key.SNAPSHOT, new JsonArray(((JreJsonArray) snapshot.toSnapshot()).toNative()));
        callback.handle(new DefaultFutureResult<JsonObject>(snapshotData));
      }
    });
  }

  @Override
  public void writeSnapshot(String docType, String docId, JsonObject snapshotData,
                            AsyncResultHandler<Void> callback) {
    Map<String, JsonObject> type = snapshotDatas.get(docType);
    if (type == null) {
      type = new HashMap<String, JsonObject>();
      snapshotDatas.put(docType, type);
    }
    type.put(docId, snapshotData);
    callback.handle(new DefaultFutureResult<Void>().setResult(null));
  }

  @Override
  public void writeOp(String docType, String docId, JsonObject opData,
                      AsyncResultHandler<Void> callback) {
    List<JsonObject> opLog = getOpLog(docType, docId);
    // This should never actually happen unless there's bugs in delta storage. (Or you try to
    // use this memory implementation with multiple frontend servers)
    if (opData.getLong(Key.VERSION) > opLog.size()) {
      callback.handle(new DefaultFutureResult<Void>(
          new ReplyException(ReplyFailure.RECIPIENT_FAILURE,
              "Internal consistancy error - mutation storage missing parent version")));
      return;
    } else if (opData.getLong(Key.VERSION) == opLog.size()) {
      opLog.add(opData);
    }

    callback.handle(new DefaultFutureResult<Void>().setResult(null));
  }

  @Override
  public void getVersion(String docType, String docId, AsyncResultHandler<Long> callback) {
    List<JsonObject> opLog = getOpLog(docType, docId);
    callback.handle(new DefaultFutureResult<Long>(Long.valueOf(opLog.size())));
  }

  @Override
  public void getOps(String docType, String docId, Long from, Long to,
                     AsyncResultHandler<JsonObject> callback) {
    List<JsonObject> opLog = getOpLog(docType, docId);
    if (to == null) {
      to = Long.valueOf(opLog.size());
    }
    JsonArray ops = new JsonArray();
    for (int i = from.intValue(); i < to; i++) {
      ops.addObject(opLog.get(i));
    }
    JsonObject toRtn = new JsonObject().putArray(Key.OPS, ops);
    callback.handle(new DefaultFutureResult<JsonObject>(toRtn));
  }

  @Override
  public void atomicSubmit(final String docType, final String docId, final JsonObject opData,
                           final AsyncResultHandler<Void> callback) {
    // This is easy because we're the only instance in the cluster, so anything that happens
    // synchronously is safe.

    long opVersion = opData.getNumber(Key.VERSION).longValue();
    Long docVersion = getSnapshotVersion(docType, docId);
    if (docVersion != null && opVersion < docVersion) {
      callback.handle(new DefaultFutureResult<Void>(
          new ReplyException(ReplyFailure.RECIPIENT_FAILURE, "Transform needed")));
      return;
    }

    versions.put(docType + "/" + docId, opVersion + 1);
    writeOp(docType, docId, opData, new AsyncResultHandler<Void>() {
      @Override
      public void handle(AsyncResult<Void> ar) {
        if (ar.succeeded()) {
          // Post the change to anyone who's interested.
          eb.publish(MemoryDeltaStorage.getDocIdChannel(address, docType, docId), opData);
        }
        callback.handle(ar);
      }
    });
  }

  @Override
  public void postSubmit(String docType, String docId, JsonObject opData, JsonObject snapshot) {
    eb.publish(MemoryDeltaStorage.getDocTypeChannel(address, docType), opData);
  }

  private List<JsonObject> getOpLog(String docType, String docId) {
    Map<String, List<JsonObject>> type = opDatas.get(docType);
    if (type == null) {
      type = new HashMap<String, List<JsonObject>>();
      opDatas.put(docType, type);
    }
    List<JsonObject> ops = type.get(docId);
    if (ops == null) {
      ops = new ArrayList<JsonObject>();
      type.put(docId, ops);
    }
    return ops;
  }

  private Long getSnapshotVersion(String docType, String docId) {
    return versions.get(docType + "/" + docId);
  }
}
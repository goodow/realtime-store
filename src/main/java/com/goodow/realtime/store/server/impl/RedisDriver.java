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
import com.google.inject.Singleton;

import com.goodow.realtime.store.channel.Constants.Addr;
import com.goodow.realtime.store.channel.Constants.Key;

import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.eventbus.ReplyException;
import org.vertx.java.core.eventbus.ReplyFailure;
import org.vertx.java.core.impl.CountingCompletionHandler;
import org.vertx.java.core.impl.DefaultFutureResult;
import org.vertx.java.core.impl.VertxInternal;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Container;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.vertx.java.redis.RedisClient;

@Singleton
public class RedisDriver {
  private static final Logger log = Logger.getLogger(RedisDriver.class.getName());
  private final String address;
  private String getOpsScript;
  private String submitScript;
  private String setExpireScript;
  @Inject private RedisClient redis;
  @Inject private ElasticSearchDriver persistor;
  private final Vertx vertx;
  private final EventBus eb;

  @Inject
  RedisDriver(Vertx vertx, final Container container) {
    this.vertx = vertx;
    eb = vertx.eventBus();
    address = container.config().getString("address", Addr.STORE);
  }

  public void atomicSubmit(final String docType, final String docId, final JsonObject opData,
      final AsyncResultHandler<Void> callback) {
    if (callback == null) {
      throw new RuntimeException("Callback missing in atomicSubmit");
    }
    final AsyncResultHandler<Void> callbackWrapper = new AsyncResultHandler<Void>() {
      @Override
      public void handle(AsyncResult<Void> ar) {
        if (ar.succeeded()) {
          // Great success. Before calling back, update the persistant oplog.
          eb.publish(getDocIdChannel(docType, docId), opData);
          writeOpToPersistence(docType, docId, opData, callback);
          return;
        }
        callback.handle(ar);
      }
    };
    redisSubmitScript(docType, docId, opData, null, new AsyncResultHandler<Void>() {
      @Override
      public void handle(AsyncResult<Void> ar) {
        if (ar.succeeded()) {
          callbackWrapper.handle(ar);
          return;
        }
        final String message = ar.cause().getMessage();
        if (!"Missing data".equals(message) && !"Version from the future".equals(message)) {
          // - 'Op already submitted': Return this error back to the user.
          // - 'Transform needed': Operation is old. Transform and retry.
          callback.handle(ar);
          return;
        }
        // - 'Missing data': Redis is not populated for this document
        // - 'Version from the future': Probably an error. Data in redis has been dumped. Reload
        // from oplog redis and retry.

        // The data in redis has been dumped. Fill redis with data from the oplog and retry.
        persistor.getVersion(docType, docId, new AsyncResultHandler<Long>() {
          @Override
          public void handle(AsyncResult<Long> ar) {
            if (ar.failed()) {
              callback.handle(new DefaultFutureResult<Void>(ar.cause()));
              return;
            }
            long docVersion = ar.result().longValue();
            log.finest("redisSubmitScript failed: " + message + " op version: " + opData
                .getLong(Key.VERSION) + " snapshot version: " + docVersion);
            if (docVersion < opData.getLong(Key.VERSION)) {
              // This is nate's awful hell error state. The oplog is basically
              // corrupted - the snapshot database is further in the future than
              // the oplog.
              //
              // In this case, we should write a no-op ramp to the snapshot
              // version, followed by a delete & a create to fill in the missing
              // ops.
              throw new RuntimeException("Missing oplog for " + docType + "/" + docId);
            }
            redisSubmitScript(docType, docId, opData, docVersion, callbackWrapper);
          }
        });
      }
    });
  }

  /**
   * Non inclusive - gets ops from [from, to). Ie, all relevant ops. If to is not defined (null or
   * undefined) then it returns all ops. Due to certain race conditions, its possible that this
   * misses operations at the end of the range. Callers have to deal with this case (semantically it
   * should be the same as an operation being submitted right after a getOps call)
   */
  public void getOps(final String docType, final String docId, final Long from, final Long to,
      final AsyncResultHandler<JsonObject> callback) {
    // First try to get the ops from redis.
    redisGetOps(docType, docId, from, to, new AsyncResultHandler<JsonObject>() {
      @Override
      public void handle(AsyncResult<JsonObject> ar) {
        if (ar.failed()) {
          callback.handle(ar);
          return;
        }
        // There are sort of three cases here:
        //
        // - Redis has no data at all: v is null
        // - Redis has some of the ops, but not all of them. v is set, and ops might not contain
        // everything we want.
        // - Redis has all of the operations we need

        // What should we do in this case, when redis returns ops but is missing
        // ops at the end of the requested range? It shouldn't be possible, but
        // we should probably detect that case at least.
        // if (to !== null && ops[ops.length - 1].v !== to)

        final Long docVersion = ar.result().getLong(Key.VERSION);
        final JsonArray ops = ar.result().getArray(Key.OPS);
        if ((docVersion != null && from >= docVersion)
            || (ops.size() > 0 && ops.<JsonObject> get(0).getLong(Key.VERSION).longValue() == from)) {
          // redis has all the ops we wanted.
          callback.handle(ar);
        } else if (ops.size() > 0) {
          // The ops we got from redis are at the end of the list of ops we need.
          persistenceGetOps(docType, docId, from, ops.<JsonObject> get(0).getLong(Key.VERSION),
              new AsyncResultHandler<JsonArray>() {
                @Override
                public void handle(AsyncResult<JsonArray> ar) {
                  if (ar.failed()) {
                    callback.handle(new DefaultFutureResult<JsonObject>(ar.cause()));
                    return;
                  }
                  JsonArray firstOps = ar.result();
                  for (Object op : ops) {
                    firstOps.addObject((JsonObject) op);
                  }
                  callback.handle(new DefaultFutureResult<JsonObject>(new JsonObject().putArray(
                      Key.OPS, firstOps).putNumber(Key.VERSION, docVersion)));
                }
              });
        } else {
          // No ops in redis. Just get all the ops from the oplog.
          persistenceGetOps(docType, docId, from, to, new AsyncResultHandler<JsonArray>() {
            @Override
            public void handle(AsyncResult<JsonArray> ar) {
              if (ar.failed()) {
                callback.handle(new DefaultFutureResult<JsonObject>(ar.cause()));
                return;
              }
              if (docVersion == null && to == null) {
                // I'm going to do a sneaky cache here if its not in redis.
                persistor.getVersion(docType, docId, new AsyncResultHandler<Long>() {
                  @Override
                  public void handle(AsyncResult<Long> ar) {
                    if (ar.succeeded()) {
                      redisCacheVersion(docType, docId, ar.result(), null);
                    }
                  }
                });
              }
              callback.handle(new DefaultFutureResult<JsonObject>(new JsonObject().putArray(
                  Key.OPS, ar.result()).putNumber(Key.VERSION, docVersion))); // v could be null.
            }
          });
        }
      }
    });
  }

  public void getVersion(final String docType, final String docId,
      final AsyncResultHandler<Long> callback) {
    redis.get(getVersionKey(docType, docId), new Handler<Message<JsonObject>>() {
      @Override
      public void handle(Message<JsonObject> reply) {
        JsonObject body = reply.body();
        if (!"ok".equals(body.getString("status"))) {
          callback.handle(new DefaultFutureResult<Long>(new ReplyException(
              ReplyFailure.RECIPIENT_FAILURE, body.getString("message"))));
          return;
        }
        String docVersion = body.getString("value");
        if (docVersion != null) {
          callback.handle(new DefaultFutureResult<Long>(Long.valueOf(docVersion)));
          return;
        }
        persistor.getVersion(docType, docId, callback);
      }
    });
  }

  public void postSubmit(final String docType, String docId, final JsonObject opData,
      final JsonObject snapshot) {
    // Publish the change to the type name (not the docId!) for queries.
    eb.publish(getDocTypeChannel(docType), opData);

    // Set the TTL on the document now that it has been written to the oplog.
    redisSetExpire(docType, docId, opData.getLong(Key.VERSION), new AsyncResultHandler<Void>() {
      @Override
      public void handle(AsyncResult<Void> ar) {
        // This shouldn't happen, but its non-fatal. It just means ops won't get flushed from redis.
        if (ar.failed()) {
          log.log(Level.FINE, "redisSetExpire error", ar.cause());
        }
      }
    });
  }

  public void start(final CountingCompletionHandler<Void> countDownLatch) {
    countDownLatch.incRequired();
    vertx.fileSystem().readFile("scripts/getOps.lua", new Handler<AsyncResult<Buffer>>() {
      @Override
      public void handle(AsyncResult<Buffer> ar) {
        if (ar.failed()) {
          countDownLatch.failed(ar.cause());
          return;
        }
        getOpsScript = ar.result().toString();
        countDownLatch.complete();
      }
    });
    countDownLatch.incRequired();
    vertx.fileSystem().readFile("scripts/submit.lua", new Handler<AsyncResult<Buffer>>() {
      @Override
      public void handle(AsyncResult<Buffer> ar) {
        if (ar.failed()) {
          countDownLatch.failed(ar.cause());
          return;
        }
        submitScript = ar.result().toString();
        countDownLatch.complete();
      }
    });
    countDownLatch.incRequired();
    vertx.fileSystem().readFile("scripts/setExpire.lua", new Handler<AsyncResult<Buffer>>() {
      @Override
      public void handle(AsyncResult<Buffer> ar) {
        if (ar.failed()) {
          countDownLatch.failed(ar.cause());
          return;
        }
        setExpireScript = ar.result().toString();
        countDownLatch.complete();
      }
    });
  }

  protected String getDocIdChannel(String docType, String docId) {
    return address + "/" + docType + "/" + docId + Addr.WATCH;
  }

  protected String getDocTypeChannel(String docType) {
    return address + "/" + docType + Addr.WATCH;
  }

  protected String getOpsKey(String docType, String docId) {
    return address + "/" + docType + "/" + docId + Addr.OPS;
  }

  protected String getSnapshotKey(String docType, String docId) {
    return address + "/" + docType + "/" + docId;
  }

  protected String getVersionKey(String docType, String docId) {
    return address + "/" + docType + "/" + docId + "/_v";
  }

  private void persistenceGetOps(String docType, String docId, final Long from, Long to,
      final AsyncResultHandler<JsonArray> callback) {
    if (to != null && to <= from) {
      callback.handle(new DefaultFutureResult<JsonArray>(new JsonArray()));
      return;
    }
    persistor.getOps(docType, docId, from == null ? 0 : from, to == null ? -1 : to,
        new AsyncResultHandler<JsonArray>() {
          @Override
          public void handle(AsyncResult<JsonArray> ar) {
            if (ar.succeeded()) {
              JsonArray ops = ar.result();
              if (ops.size() > 0
                  && ops.<JsonObject> get(0).getLong(Key.VERSION).longValue() != from) {
                throw new RuntimeException("Oplog is returning incorrect ops");
              }
            }
            callback.handle(ar);
          }
        });
  }

  /**
   * The ops are stored in redis as JSON strings without versions. They're returned with the final
   * version at the end of the lua table.
   */
  private JsonArray processRedisOps(long docVersion, List<String> ops) {
    long startVersion = docVersion - ops.size();
    JsonArray toRtn = new JsonArray();
    for (String op : ops) {
      toRtn.addObject(new JsonObject(op).putNumber(Key.VERSION, startVersion++));
    }
    return toRtn;
  }

  private void redisCacheVersion(final String docType, final String docId, final long docVersion,
      final AsyncResultHandler<Void> opt_callback) {
    // At some point it'll be worth caching the snapshot in redis as well and checking performance.
    redis.setnx(getVersionKey(docType, docId), docVersion, new Handler<Message<JsonObject>>() {
      @Override
      public void handle(Message<JsonObject> reply) {
        JsonObject body = reply.body();
        if (!"ok".equals(body.getString("status"))) {
          if (opt_callback != null) {
            opt_callback.handle(new DefaultFutureResult<Void>(new ReplyException(
                ReplyFailure.RECIPIENT_FAILURE, body.getString("message"))));
          }
          return;
        }
        if (body.getInteger("value") == 0) {
          if (opt_callback != null) {
            opt_callback.handle(new DefaultFutureResult<Void>((Void) null));
          }
          return;
        }
        // Just in case. The oplog shouldn't be in redis if the version isn't in redis, but
        // whatever.
        redisSetExpire(docType, docId, docVersion, opt_callback);
      }
    });
  }

  /**
   * Follows same semantics as getOps elsewhere - returns ops [from, to). May not return all
   * operations in this range.
   */
  private void redisGetOps(String docType, String docId, Long from, Long to,
      final AsyncResultHandler<JsonObject> callback) {
    final DefaultFutureResult<JsonObject> futureResult =
        new DefaultFutureResult<JsonObject>().setHandler(callback);
    if (to == null) {
      to = -1L;
    } else {
      // Early abort if the range is flat.
      if (to >= 0 && (from >= to || to == 0)) {
        futureResult.setResult(new JsonObject().putNumber(Key.VERSION, null).putArray(Key.OPS,
            new JsonArray()));
        return;
      }
      to--;
    }

    redis.eval(getOpsScript, 2, getVersionKey(docType, docId), getOpsKey(docType, docId), from, to,
        new Handler<Message<JsonObject>>() {
          @Override
          public void handle(Message<JsonObject> reply) {
            JsonObject body = reply.body();
            if (!"ok".equals(body.getString("status"))) {
              futureResult.setFailure(new ReplyException(ReplyFailure.RECIPIENT_FAILURE, body
                  .getString("message")));
              return;
            }
            JsonArray value = body.getArray("value");
            JsonObject result;
            if (value == null) {
              // No data in redis. Punt to the persistant oplog.
              result =
                  new JsonObject().putNumber(Key.VERSION, null).putArray(Key.OPS, new JsonArray());
            } else {
              // Version of the document is at the end of the results list.
              List<?> list = value.toList();
              Long docVersion = (Long) list.remove(list.size() - 1);
              @SuppressWarnings("unchecked")
              JsonArray ops = processRedisOps(docVersion, (List<String>) list);
              result = new JsonObject().putNumber(Key.VERSION, docVersion).putArray(Key.OPS, ops);
            }
            futureResult.setResult(result);
          }
        });
  }

  /**
   * After we submit an operation, reset redis's TTL so the data is allowed to expire.
   */
  private void redisSetExpire(String docType, String docId, long version,
      final AsyncResultHandler<Void> opt_callback) {
    redis.eval(setExpireScript, 2, getVersionKey(docType, docId), getOpsKey(docType, docId),
        version, new Handler<Message<JsonObject>>() {
          @Override
          public void handle(Message<JsonObject> reply) {
            if (opt_callback == null) {
              return;
            }
            JsonObject body = reply.body();
            if (!"ok".equals(body.getString("status"))) {
              opt_callback.handle(new DefaultFutureResult<Void>(new ReplyException(
                  ReplyFailure.RECIPIENT_FAILURE, body.getString("message"))));
              return;
            }
            opt_callback.handle(new DefaultFutureResult<Void>((Void) null));
          }
        });
  }

  /**
   * @param docVersion docVersion is optional - if specified, this is set & used if redis doesn't
   *          know the doc's version
   */
  private void redisSubmitScript(String docType, String docId, JsonObject opData, Long docVersion,
      final AsyncResultHandler<Void> callback) {
    redis.eval(submitScript, 3, opData.getString(Key.SESSION_ID), getVersionKey(docType, docId),
        getOpsKey(docType, docId), opData.getNumber("seq"), opData.getNumber(Key.VERSION), opData
            .encode(), // oplog entry
        docVersion, new Handler<Message<JsonObject>>() {
          @Override
          public void handle(Message<JsonObject> reply) {
            DefaultFutureResult<Void> result = new DefaultFutureResult<Void>().setHandler(callback);
            JsonObject body = reply.body();
            if (!"ok".equals(body.getString("status"))) {
              result.setFailure(new ReplyException(ReplyFailure.RECIPIENT_FAILURE, body
                  .getString("status")
                  + ": " + body.getString("message")));
              return;
            }
            if (body.getString("value") != null) {
              result.setFailure(new ReplyException(ReplyFailure.RECIPIENT_FAILURE, body
                  .getString("value")));
              return;
            }
            result.setResult(null);
          }
        });
  }

  /**
   * Internal method for updating the persistant oplog. This should only be called after
   * atomicSubmit (above).
   */
  private void writeOpToPersistence(final String docType, final String docId,
      final JsonObject opData, final AsyncResultHandler<Void> callback) {
    persistor.getVersion(docType, docId, new AsyncResultHandler<Long>() {
      @Override
      public void handle(AsyncResult<Long> ar) {
        if (ar.failed()) {
          callback.handle(new DefaultFutureResult<Void>(ar.cause()));
          return;
        }
        long docVersion = ar.result().longValue();
        long opVersion = opData.getLong(Key.VERSION).longValue();
        if (docVersion == opVersion) {
          persistor.writeOp(docType, docId, opData, callback);
        } else {
          assert docVersion < opVersion;
          // Its possible (though unlikely) that ops will be missing from the oplog if the redis
          // script succeeds but the process crashes before the persistant oplog is given the new
          // operations. In this case, backfill the persistant oplog with the data in redis.
          log.info("populating oplog [" + docVersion + ", " + opVersion + "]");
          redisGetOps(docType, docId, docVersion, opVersion, new AsyncResultHandler<JsonObject>() {
            @Override
            public void handle(AsyncResult<JsonObject> ar) {
              if (ar.failed()) {
                callback.handle(new DefaultFutureResult<Void>(ar.cause()));
                return;
              }
              JsonArray ops = ar.result().getArray(Key.OPS).addObject(opData);
              final CountingCompletionHandler<Void> countDownLatch =
                  new CountingCompletionHandler<Void>((VertxInternal) vertx, ops.size());
              countDownLatch.setHandler(callback);
              for (Object op : ops) {
                persistor.writeOp(docType, docId, (JsonObject) op, new AsyncResultHandler<Void>() {
                  @Override
                  public void handle(AsyncResult<Void> ar) {
                    if (ar.failed()) {
                      countDownLatch.failed(ar.cause());
                    } else {
                      countDownLatch.complete();
                    }
                  }
                });
              }
            }
          });
        }
      }
    });
  }
}
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

import com.goodow.realtime.store.server.StoreVerticle;

import com.google.inject.Inject;

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
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Container;

import java.util.List;

import io.vertx.java.redis.RedisClient;

public class RedisDriver {
  private final String address;
  private String getOpsScript;
  private String submitScript;
  private String setExpireScript;
  @Inject private RedisClient redis;
  @Inject private ElasticSearchDriver persistence;
  private final Vertx vertx;
  private final EventBus eb;
  private final Logger logger;

  @Inject
  RedisDriver(Vertx vertx, final Container container) {
    this.vertx = vertx;
    eb = vertx.eventBus();
    logger = container.logger();
    address =
        container.config().getObject("realtime_store", new JsonObject()).getString("address",
            StoreVerticle.DEFAULT_ADDRESS);
  }

  public void atomicSubmit(final String type, final String id, final JsonObject opData,
      final AsyncResultHandler<Void> callback) {
    if (callback == null) {
      throw new RuntimeException("Callback missing in atomicSubmit");
    }
    final AsyncResultHandler<Void> callbackWrapper = new AsyncResultHandler<Void>() {
      @Override
      public void handle(AsyncResult<Void> ar) {
        if (ar.succeeded()) {
          // Great success. Before calling back, update the persistant oplog.
          eb.publish(getDocumentChannel(type, id), opData);
          writeOpToPersistence(type, id, opData, callback);
          return;
        }
        callback.handle(ar);
      }
    };
    redisSubmitScript(type, id, opData, null, new AsyncResultHandler<Void>() {
      @Override
      public void handle(AsyncResult<Void> ar) {
        if (ar.succeeded()) {
          callbackWrapper.handle(ar);
          return;
        }
        String message = ar.cause().getMessage();
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
        persistence.getVersion(type, id, new AsyncResultHandler<Long>() {
          @Override
          public void handle(AsyncResult<Long> ar) {
            if (ar.failed()) {
              callback.handle(new DefaultFutureResult<Void>(ar.cause()));
              return;
            }
            long docVersion = ar.result().longValue();
            if (docVersion < opData.getLong("v")) {
              // This is nate's awful hell error state. The oplog is basically
              // corrupted - the snapshot database is further in the future than
              // the oplog.
              //
              // In this case, we should write a no-op ramp to the snapshot
              // version, followed by a delete & a create to fill in the missing
              // ops.
              throw new RuntimeException("Missing oplog for " + type + "/" + id);
            }
            redisSubmitScript(type, id, opData, docVersion, callbackWrapper);
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
  public void getOps(final String type, final String id, final Long from, final Long to,
      final AsyncResultHandler<JsonObject> callback) {
    // First try to get the ops from redis.
    redisGetOps(type, id, from, to, new AsyncResultHandler<JsonObject>() {
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

        final Long docVersion = ar.result().getLong("v");
        final JsonArray ops = ar.result().getArray("ops");
        if ((docVersion != null && from >= docVersion)
            || (ops.size() > 0 && ops.<JsonObject> get(0).getLong("v").longValue() == from)) {
          // redis has all the ops we wanted.
          callback.handle(ar);
        } else if (ops.size() > 0) {
          // The ops we got from redis are at the end of the list of ops we need.
          persistenceGetOps(type, id, from, ops.<JsonObject> get(0).getLong("v"),
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
                      "ops", firstOps).putNumber("v", docVersion)));
                }
              });
        } else {
          // No ops in redis. Just get all the ops from the oplog.
          persistenceGetOps(type, id, from, to, new AsyncResultHandler<JsonArray>() {
            @Override
            public void handle(AsyncResult<JsonArray> ar) {
              if (ar.failed()) {
                callback.handle(new DefaultFutureResult<JsonObject>(ar.cause()));
                return;
              }
              if (docVersion == null && to == null) {
                // I'm going to do a sneaky cache here if its not in redis.
                persistence.getVersion(type, id, new AsyncResultHandler<Long>() {
                  @Override
                  public void handle(AsyncResult<Long> ar) {
                    if (ar.succeeded()) {
                      redisCacheVersion(type, id, ar.result(), null);
                    }
                  }
                });
              }
              callback.handle(new DefaultFutureResult<JsonObject>(new JsonObject().putArray("ops",
                  ar.result()).putNumber("v", docVersion))); // v could be null.
            }
          });
        }
      }
    });
  }

  public void getVersion(final String type, final String id, final AsyncResultHandler<Long> callback) {
    redis.get(getVersionKey(type, id), new Handler<Message<JsonObject>>() {
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
        persistence.getVersion(type, id, callback);
      }
    });
  }

  public void postSubmit(final String type, String id, final JsonObject opData,
      final JsonObject snapshot) {
    // Publish the change to the type name (not the docId!) for queries.
    eb.publish(getTypeChannel(type), opData);

    // Set the TTL on the document now that it has been written to the oplog.
    redisSetExpire(type, id, opData.getLong("v"), new AsyncResultHandler<Void>() {
      @Override
      public void handle(AsyncResult<Void> ar) {
        // This shouldn't happen, but its non-fatal. It just means ops won't get flushed from redis.
        if (ar.failed()) {
          logger.trace("redisSetExpire error", ar.cause());
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

  protected String getDocumentChannel(String type, String id) {
    return address + "." + type + "." + id;
  }

  protected String getOpsKey(String type, String id) {
    return address + ":" + type + "/" + id + ":ops";
  }

  protected String getSnapshotKey(String type, String id) {
    return address + ":" + type + "/" + id + ":snapshot";
  }

  protected String getTypeChannel(String type) {
    return address + "." + type;
  }

  protected String getVersionKey(String type, String id) {
    return address + ":" + type + "/" + id + ":v";
  }

  private void persistenceGetOps(String type, String id, final Long from, Long to,
      final AsyncResultHandler<JsonArray> callback) {
    if (to != null && to <= from) {
      callback.handle(new DefaultFutureResult<JsonArray>(new JsonArray()));
      return;
    }
    persistence.getOps(type, id, from, to, new AsyncResultHandler<JsonArray>() {
      @Override
      public void handle(AsyncResult<JsonArray> ar) {
        if (ar.succeeded()) {
          JsonArray ops = ar.result();
          if (ops.size() > 0 && ops.<JsonObject> get(0).getLong("v").longValue() != from) {
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
      toRtn.addObject(new JsonObject(op).putNumber("v", startVersion++));
    }
    return toRtn;
  }

  private void redisCacheVersion(final String type, final String id, final long docVersion,
      final AsyncResultHandler<Void> opt_callback) {
    // At some point it'll be worth caching the snapshot in redis as well and checking performance.
    redis.setnx(getVersionKey(type, id), docVersion, new Handler<Message<JsonObject>>() {
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
        redisSetExpire(type, id, docVersion, opt_callback);
      }
    });
  }

  /**
   * Follows same semantics as getOps elsewhere - returns ops [from, to). May not return all
   * operations in this range.
   */
  private void redisGetOps(String type, String id, Long from, Long to,
      final AsyncResultHandler<JsonObject> callback) {
    final DefaultFutureResult<JsonObject> futureResult =
        new DefaultFutureResult<JsonObject>().setHandler(callback);
    if (to == null) {
      to = -1L;
    } else {
      // Early abort if the range is flat.
      if (to >= 0 && (from >= to || to == 0)) {
        futureResult.setResult(new JsonObject().putNumber("v", null).putArray("ops",
            new JsonArray()));
        return;
      }
      to--;
    }
    redis.eval(getOpsScript, 2, getVersionKey(type, id), getOpsKey(type, id), from, to,
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
              result = new JsonObject().putNumber("v", null).putArray("ops", new JsonArray());
            } else {
              // Version of the document is at the end of the results list.
              @SuppressWarnings("unchecked")
              List<String> list = value.toList();
              Long docVersion = Long.valueOf(list.remove(list.size() - 1));
              JsonArray ops = processRedisOps(docVersion, list);
              result = new JsonObject().putNumber("v", docVersion).putArray("ops", ops);
            }
            futureResult.setResult(result);
          }
        });
  }

  /**
   * After we submit an operation, reset redis's TTL so the data is allowed to expire.
   */
  private void redisSetExpire(String type, String id, long version,
      final AsyncResultHandler<Void> opt_callback) {
    redis.eval(setExpireScript, 2, getVersionKey(type, id), getOpsKey(type, id), version,
        new Handler<Message<JsonObject>>() {
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
  private void redisSubmitScript(String type, String id, JsonObject opData, Long docVersion,
      final AsyncResultHandler<Void> callback) {
    redis.eval(submitScript, 3, opData.getString("sid"), getVersionKey(type, id), getOpsKey(type,
        id), opData.getNumber("seq"), opData.getNumber("v"), opData.encode(), // oplog entry
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
  private void writeOpToPersistence(final String type, final String id, final JsonObject opData,
      final AsyncResultHandler<Void> callback) {
    persistence.getVersion(type, id, new AsyncResultHandler<Long>() {
      @Override
      public void handle(AsyncResult<Long> ar) {
        if (ar.failed()) {
          callback.handle(new DefaultFutureResult<Void>(ar.cause()));
          return;
        }
        long docVersion = ar.result().longValue();
        long opVersion = opData.getLong("v").longValue();
        if (docVersion == opVersion) {
          persistence.writeOp(type, id, opData, callback);
        } else {
          assert docVersion < opVersion;
          // Its possible (though unlikely) that ops will be missing from the oplog if the redis
          // script succeeds but the process crashes before the persistant oplog is given the new
          // operations. In this case, backfill the persistant oplog with the data in redis.
          redisGetOps(type, id, docVersion, opVersion, new AsyncResultHandler<JsonObject>() {
            @Override
            public void handle(AsyncResult<JsonObject> ar) {
              if (ar.failed()) {
                callback.handle(new DefaultFutureResult<Void>(ar.cause()));
                return;
              }
              JsonArray ops = ar.result().getArray("ops").addObject(opData);
              final CountingCompletionHandler<Void> countDownLatch =
                  new CountingCompletionHandler<Void>((VertxInternal) vertx, ops.size());
              countDownLatch.setHandler(callback);
              for (Object op : ops) {
                persistence.writeOp(type, id, (JsonObject) op, new AsyncResultHandler<Void>() {
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
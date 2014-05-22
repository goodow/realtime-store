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
import com.goodow.realtime.store.server.StoreVerticle;

import com.google.inject.Inject;

import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.eventbus.ReplyException;
import org.vertx.java.core.impl.CountingCompletionHandler;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Container;

public class SnapshotHandler {
  static <T> AsyncResultHandler<T> handleAsyncResult(final Message<JsonObject> resp) {
    return new AsyncResultHandler<T>() {
      @Override
      public void handle(AsyncResult<T> ar) {
        if (ar.failed()) {
          ReplyException cause = (ReplyException) ar.cause();
          resp.fail(cause.failureCode(), cause.getMessage());
          return;
        }
        resp.reply(ar.result());
      }
    };
  }

  @Inject private ElasticSearchDriver persistor;
  @Inject private RedisDriver redis;
  @Inject private OpSubmitter opSubmitter;
  @Inject private Vertx vertx;
  @Inject private Container container;
  private String address;

  public void start(final CountingCompletionHandler<Void> countDownLatch) {
    address = container.config().getString("address", StoreVerticle.DEFAULT_ADDRESS);

    countDownLatch.incRequired();
    vertx.eventBus().registerHandler(address, new Handler<Message<JsonObject>>() {
      @Override
      public void handle(Message<JsonObject> message) {
        JsonObject body = message.body();
        String id = body.getString(Key.ID);
        if (id == null) {
          message.fail(-1, "id must be specified");
          return;
        }
        String[] typeAndId = OpsHandler.getTypeAndId(id);
        String docType = typeAndId[0];
        String docId = typeAndId[1];
        String action = body.getString("action", "get");
        if ("head".equals(action)) {
          doHead(docType, docId, message);
        } else if ("post".equals(action)) {
          JsonObject opData = body.getObject(Key.OP_DATA);
          if (opData == null) {
            message.fail(-1, Key.OP_DATA + " must be specified");
            return;
          }
          doPost(docType, docId, opData, message);
        } else { // get
          doGet(docType, docId, message);
        }
      }
    }, new Handler<AsyncResult<Void>>() {
      @Override
      public void handle(AsyncResult<Void> ar) {
        if (ar.succeeded()) {
          countDownLatch.complete();
        } else {
          countDownLatch.failed(ar.cause());
        }
      }
    });
  }

  private void doGet(String docType, String docId, final Message<JsonObject> resp) {
    persistor.getSnapshot(docType, docId, SnapshotHandler.<JsonObject> handleAsyncResult(resp));
  }

  private void doHead(String docType, String docId, Message<JsonObject> resp) {
    redis.getVersion(docType, docId, SnapshotHandler.<Long> handleAsyncResult(resp));
  }

  private void doPost(String docType, String docId, JsonObject opData,
      final Message<JsonObject> resp) {
    opSubmitter.submit(docType, docId, opData, new AsyncResultHandler<JsonObject>() {
      @Override
      public void handle(AsyncResult<JsonObject> ar) {
        if (ar.failed()) {
          ReplyException cause = (ReplyException) ar.cause();
          resp.fail(cause.failureCode(), cause.getMessage());
          return;
        }
        JsonObject result = ar.result();
        result.removeField(Key.SNAPSHOT);
        resp.reply(result);
      }
    });
  }
}
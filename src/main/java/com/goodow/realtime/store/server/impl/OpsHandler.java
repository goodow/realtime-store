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

import com.goodow.realtime.store.channel.Constants.Addr;
import com.goodow.realtime.store.channel.Constants.Key;

import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.eventbus.ReplyException;
import org.vertx.java.core.impl.CountingCompletionHandler;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Container;

public class OpsHandler {
  static String[] getTypeAndId(String id) {
    int idx = id.indexOf('/');
    boolean hasType = idx != -1 && idx != id.length() - 1;
    return hasType ? new String[] {id.substring(0, idx), id.substring(idx + 1)} : new String[] {
        "test", id};
  }

  @Inject private RedisDriver redis;
  @Inject private Vertx vertx;
  @Inject private Container container;
  private String address;

  public void start(final CountingCompletionHandler<Void> countDownLatch) {
    address = container.config().getString("address", Addr.STORE) + Addr.OPS;

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
        String[] typeAndId = getTypeAndId(id);
        doGet(typeAndId[0], typeAndId[1], body.getLong("from", 0), body.getLong("to"), message);
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

  /**
   * Non inclusive - gets ops from [from, to). Ie, all relevant ops. If to is null then it returns
   * all ops.
   */
  private void doGet(String docType, String docId, Long from, Long to,
      final Message<JsonObject> resp) {
    if (from == null) {
      resp.fail(-1, "Invalid from field in getOps");
      return;
    }
    redis.getOps(docType, docId, from, to, new AsyncResultHandler<JsonObject>() {
      @Override
      public void handle(AsyncResult<JsonObject> ar) {
        if (ar.failed()) {
          ReplyException cause = (ReplyException) ar.cause();
          resp.fail(cause.failureCode(), cause.getMessage());
          return;
        }
        resp.reply(ar.result().getArray(Key.OPS));
      }
    });
  }
}
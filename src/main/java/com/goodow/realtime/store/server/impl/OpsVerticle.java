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

import com.goodow.realtime.store.server.StoreModule;
import com.goodow.realtime.store.server.StoreVerticle;

import com.alienos.guice.GuiceVerticleHelper;
import com.alienos.guice.GuiceVertxBinding;
import com.google.inject.Inject;

import org.vertx.java.busmods.BusModBase;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.Future;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.eventbus.ReplyException;
import org.vertx.java.core.json.JsonObject;

@GuiceVertxBinding(modules = {StoreModule.class})
public class OpsVerticle extends BusModBase {
  static String[] getTypeAndId(String docId) {
    int idx = docId.indexOf('/');
    boolean hasType = idx != -1 && idx != docId.length() - 1;
    return hasType ? new String[] {docId.substring(0, idx), docId.substring(idx + 1)}
        : new String[] {"test", docId};
  }

  private String address;
  @Inject private RedisDriver redis;

  @Override
  public void start(final Future<Void> startedResult) {
    GuiceVerticleHelper.inject(this, vertx, container);
    super.start();
    address =
        getOptionalObjectConfig("realtime_store", new JsonObject()).getString("address",
            StoreVerticle.DEFAULT_ADDRESS)
            + ".ops";

    eb.registerHandler(address, new Handler<Message<JsonObject>>() {
      @Override
      public void handle(Message<JsonObject> message) {
        JsonObject body = message.body();
        String docId = getMandatoryString("id", message);
        if (docId == null) {
          return;
        }
        String[] typeAndId = getTypeAndId(docId);
        String type = typeAndId[0];
        String id = typeAndId[1];
        doGet(type, id, body.getLong("from"), body.getLong("to"), message);
      }
    }, new Handler<AsyncResult<Void>>() {
      @Override
      public void handle(AsyncResult<Void> ar) {
        if (ar.succeeded()) {
          startedResult.setResult(null);
        } else {
          startedResult.setFailure(ar.cause());
        }
      }
    });
  }

  /**
   * Non inclusive - gets ops from [from, to). Ie, all relevant ops. If to is null then it returns
   * all ops.
   */
  private void doGet(String type, String id, Long from, Long to, final Message<JsonObject> resp) {
    if (from == null) {
      resp.fail(-1, "Invalid from field in getOps");
      return;
    }
    redis.getOps(type, id, from, to, new AsyncResultHandler<JsonObject>() {
      @Override
      public void handle(AsyncResult<JsonObject> ar) {
        if (ar.failed()) {
          ReplyException cause = (ReplyException) ar.cause();
          resp.fail(cause.failureCode(), cause.getMessage());
          return;
        }
        resp.reply(ar.result().getArray("ops"));
      }
    });
  }
}
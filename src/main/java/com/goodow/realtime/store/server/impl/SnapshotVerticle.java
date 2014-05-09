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
public class SnapshotVerticle extends BusModBase {
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

  @Inject private ElasticSearchDriver persistence;
  @Inject private RedisDriver redis;
  @Inject private OpsSubmitter opsSubmitter;
  private String address;

  @Override
  public void start(final Future<Void> startedResult) {
    GuiceVerticleHelper.inject(this, vertx, container);
    super.start();
    address = getOptionalStringConfig("address", StoreVerticle.DEFAULT_ADDRESS);

    eb.registerHandler(address, new Handler<Message<JsonObject>>() {
      @Override
      public void handle(Message<JsonObject> message) {
        String docId = getMandatoryString("id", message);
        if (docId == null) {
          return;
        }
        String[] typeAndId = OpsVerticle.getTypeAndId(docId);
        String type = typeAndId[0];
        String id = typeAndId[1];
        JsonObject body = message.body();
        String action = body.getString("action", "get");
        if ("head".equals(action)) {
          doHead(type, id, message);
        } else if ("post".equals(action)) {
          JsonObject opData = getMandatoryObject(Key.OP_DATA, message);
          if (opData == null) {
            return;
          }
          doPost(type, id, opData, message);
        } else { // get
          doGet(type, id, message);
        }
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

  private void doGet(String type, String id, final Message<JsonObject> resp) {
    persistence.getSnapshot(type, id, SnapshotVerticle.<JsonObject> handleAsyncResult(resp));
  }

  private void doHead(String type, String id, Message<JsonObject> resp) {
    redis.getVersion(type, id, SnapshotVerticle.<Long> handleAsyncResult(resp));
  }

  private void doPost(String type, String id, JsonObject opData, final Message<JsonObject> resp) {
    opsSubmitter.submit(type, id, opData, new AsyncResultHandler<JsonObject>() {
      @Override
      public void handle(AsyncResult<JsonObject> ar) {
        if (ar.failed()) {
          ReplyException cause = (ReplyException) ar.cause();
          resp.fail(cause.failureCode(), cause.getMessage());
          return;
        }
        JsonObject result = ar.result();
        result.removeField("snapshot");
        resp.reply(result);
      }
    });
  }
}
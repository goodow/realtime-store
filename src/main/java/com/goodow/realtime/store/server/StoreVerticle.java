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
package com.goodow.realtime.store.server;

import com.goodow.realtime.store.server.impl.OpsVerticle;
import com.goodow.realtime.store.server.impl.RedisDriver;
import com.goodow.realtime.store.server.impl.RestVerticle;
import com.goodow.realtime.store.server.impl.SnapshotVerticle;

import com.alienos.guice.GuiceVerticleHelper;
import com.alienos.guice.GuiceVertxBinding;
import com.google.inject.Inject;

import org.vertx.java.busmods.BusModBase;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.Future;
import org.vertx.java.core.Handler;
import org.vertx.java.core.impl.CountingCompletionHandler;
import org.vertx.java.core.impl.VertxInternal;
import org.vertx.java.core.json.JsonObject;

import io.vertx.java.redis.RedisClient;

@GuiceVertxBinding(modules = {StoreModule.class})
public class StoreVerticle extends BusModBase {
  public static final String DEFAULT_ADDRESS = "realtime.store";
  @Inject private RedisClient redis;
  @Inject private RedisDriver redisDriver;

  @Override
  public void start(final Future<Void> startedResult) {
    GuiceVerticleHelper.inject(this, vertx, container);
    super.start();

    final CountingCompletionHandler<Void> countDownLatch =
        new CountingCompletionHandler<Void>((VertxInternal) vertx, 6);
    countDownLatch.setHandler(new Handler<AsyncResult<Void>>() {
      @Override
      public void handle(AsyncResult<Void> ar) {
        if (ar.failed()) {
          startedResult.setFailure(ar.cause());
        } else if (ar.succeeded()) {
          startedResult.setResult(null);
        }
      }
    });
    AsyncResultHandler<String> doneHandler = new AsyncResultHandler<String>() {
      @Override
      public void handle(AsyncResult<String> ar) {
        if (ar.failed()) {
          countDownLatch.failed(ar.cause());
        } else {
          countDownLatch.complete();
        }
      }
    };

    JsonObject empty = new JsonObject();
    container.deployModule("com.goodow.realtime~realtime-channel~0.5.5-SNAPSHOT", config
        .getObject("realtime_channel"), doneHandler);
    container.deployModule("com.goodow.realtime~realtime-search~0.5.5-SNAPSHOT",
        getOptionalObjectConfig("realtime_search", empty), doneHandler);

    JsonObject store = getOptionalObjectConfig("realtime_store", empty);
    redis.deployModule(container, store.getObject("redis", empty), doneHandler);
    container.deployVerticle(SnapshotVerticle.class.getName(), store, doneHandler);
    container.deployVerticle(OpsVerticle.class.getName(), store, doneHandler);
    container.deployVerticle(RestVerticle.class.getName(), store, doneHandler);

    redisDriver.start(countDownLatch);
  }
}
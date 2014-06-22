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

import com.google.inject.Inject;

import com.alienos.guice.GuiceVerticleHelper;
import com.alienos.guice.GuiceVertxBinding;
import com.goodow.realtime.store.server.impl.OpsHandler;
import com.goodow.realtime.store.server.impl.PresenceHandler;
import com.goodow.realtime.store.server.impl.RedisDriver;
import com.goodow.realtime.store.server.impl.SnapshotHandler;

import org.vertx.java.busmods.BusModBase;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.Future;
import org.vertx.java.core.Handler;
import org.vertx.java.core.impl.CountingCompletionHandler;
import org.vertx.java.core.impl.VertxInternal;

@GuiceVertxBinding(modules = {StoreModule.class})
public class StoreVerticle extends BusModBase {
  @Inject private RedisDriver redisDriver;
  @Inject private SnapshotHandler snapshotHandler;
  @Inject private OpsHandler opsHandler;
  @Inject private PresenceHandler presenceHandler;

  @Override
  public void start(final Future<Void> startedResult) {
    GuiceVerticleHelper.inject(this, vertx, container);
    super.start();

    final CountingCompletionHandler<Void> countDownLatch =
        new CountingCompletionHandler<Void>((VertxInternal) vertx, 1);
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

    redisDriver.start(countDownLatch);
    snapshotHandler.start(countDownLatch);
    opsHandler.start(countDownLatch);
    presenceHandler.start(countDownLatch);
    container.deployVerticle(RestVerticle.class.getName(), config, doneHandler);
  }
}
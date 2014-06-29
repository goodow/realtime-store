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
import com.goodow.realtime.store.server.impl.OperationHandler;
import com.goodow.realtime.store.server.impl.PresenceHandler;
import com.goodow.realtime.store.server.impl.SnapshotHandler;

import org.vertx.java.busmods.BusModBase;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.Future;
import org.vertx.java.core.Handler;
import org.vertx.java.core.impl.CountingCompletionHandler;
import org.vertx.java.core.impl.VertxInternal;
import org.vertx.java.core.json.JsonObject;

@GuiceVertxBinding(modules = {StoreModule.class})
public class StoreVerticle extends BusModBase {
  @Inject private DeltaStorage storage;
  @Inject private SnapshotHandler snapshotHandler;
  @Inject private OperationHandler operationHandler;
  @Inject private PresenceHandler presenceHandler;

  @Override
  public void start(final Future<Void> startedResult) {
    GuiceVerticleHelper.inject(this, vertx, container);
    super.start();

    final CountingCompletionHandler<Void> countDownLatch =
        new CountingCompletionHandler<Void>((VertxInternal) vertx, 2);
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

    storage.start(countDownLatch);
    snapshotHandler.start(countDownLatch);
    operationHandler.start(countDownLatch);
    presenceHandler.start(countDownLatch);
    container.deployVerticle(RestVerticle.class.getName(),
        getOptionalObjectConfig("realtime_store", new JsonObject()), doneHandler);

    container.deployModule("com.goodow.realtime~realtime-channel~0.5.5-SNAPSHOT",
        getOptionalObjectConfig("realtime_channel", new JsonObject()), doneHandler);
  }
}
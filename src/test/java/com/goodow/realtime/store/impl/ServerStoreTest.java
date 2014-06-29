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
package com.goodow.realtime.store.impl;

import com.goodow.realtime.channel.server.impl.VertxBus;
import com.goodow.realtime.channel.server.impl.VertxPlatform;
import com.goodow.realtime.core.Handler;
import com.goodow.realtime.store.CollaborativeMap;
import com.goodow.realtime.store.CollaborativeString;
import com.goodow.realtime.store.Document;
import com.goodow.realtime.store.Model;
import com.goodow.realtime.store.Store;

import static org.vertx.testtools.VertxAssert.assertNotNull;
import static org.vertx.testtools.VertxAssert.assertTrue;

import org.junit.Test;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.testtools.TestVerticle;
import org.vertx.testtools.VertxAssert;

/**
 * To extend org.vertx.testtools.JavaClassRunner's default 5 minutes timeout, you can set timeout to
 * 1 hour by define a system property using -Dvertx.test.timeout=3600
 */
public class ServerStoreTest extends TestVerticle {
  private Store store;

  @Override
  public void start() {
    initialize();

    JsonObject realtimeStore = new JsonObject().putString("storage", "redis-elasticsearch");
    JsonObject elasticsearch = new JsonObject().putArray("transportAddresses",
        new JsonArray().add(new JsonObject().putString("host", "localhost")))
            .putBoolean("client_transport_sniff", false);
    JsonObject redis = new JsonObject().putString("host", "localhost");
    JsonObject config = new JsonObject().putObject("realtime_store", realtimeStore)
        .putObject("realtime_search", elasticsearch).putObject("redis", redis);
    container.deployModule(System.getProperty("vertx.modulename"), config,
        new AsyncResultHandler<String>() {
          @Override
          public void handle(AsyncResult<String> asyncResult) {
            assertTrue(asyncResult.succeeded());
            assertNotNull("deploymentID should not be null", asyncResult.result());

            VertxPlatform.register(vertx);
            store = new StoreImpl(new VertxBus(vertx.eventBus()));
            startTests();
          }
        });
  }

  @Test
  public void test() {
    store.load("users/larry", new Handler<Document>() {
      @Override
      public void handle(Document doc) {
        Model mod = doc.getModel();
        CollaborativeMap root = mod.getRoot();
        VertxAssert.assertEquals("Larry Tin", root.<CollaborativeString> get("name").getText());
        VertxAssert.testComplete();
      }
    }, new Handler<Model>() {
      @Override
      public void handle(Model mod) {
        CollaborativeString str = mod.createString("Larry Tin");
        mod.getRoot().set("name", str);
      }
    }, null);
  }
}
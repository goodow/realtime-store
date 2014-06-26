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

import com.goodow.realtime.channel.Bus;
import com.goodow.realtime.channel.Message;
import com.goodow.realtime.channel.impl.ReconnectBus;
import com.goodow.realtime.channel.impl.ReliableSubscribeBus;
import com.goodow.realtime.core.Handler;
import com.goodow.realtime.core.Platform;
import com.goodow.realtime.core.Registration;
import com.goodow.realtime.json.Json;
import com.goodow.realtime.json.JsonObject;
import com.goodow.realtime.operation.Transformer;
import com.goodow.realtime.operation.impl.CollaborativeOperation;
import com.goodow.realtime.operation.impl.CollaborativeTransformer;
import com.goodow.realtime.store.Document;
import com.goodow.realtime.store.Error;
import com.goodow.realtime.store.Model;
import com.goodow.realtime.store.channel.Constants;
import com.goodow.realtime.store.channel.Constants.Key;
import com.goodow.realtime.store.impl.DocumentBridge.OutputSink;

public class SubscribeOnlyStore extends MemoryStore {
  public SubscribeOnlyStore(Bus bus) {
    super(bus);
  }

  public SubscribeOnlyStore(String serverUri, JsonObject options) {
    this(new ReliableSubscribeBus(new ReconnectBus(serverUri, options), options));
  }

  @Override
  public void load(final String id, final Handler<Document> onLoaded,
      final Handler<Model> opt_initializer, final Handler<Error> opt_error) {
    bus.send(Constants.Topic.STORE, Json.createObject().set(Key.ID, id), new Handler<Message<JsonObject>>() {
      @Override
      public void handle(Message<JsonObject> message) {
        JsonObject body = message.body();
        if (!body.has(Key.VERSION)) {
          body.set(Key.VERSION, 0);
        }
        final DocumentBridge bridge =
            new DocumentBridge(SubscribeOnlyStore.this, id, body.getArray(Key.SNAPSHOT),
                               body.getArray(Key.COLLABORATORS), opt_error);
        onLoaded(id, opt_initializer, body.getNumber(Key.VERSION), bridge);
        if (body.getNumber(Key.VERSION) == 0) {
          bridge.createRoot();
          if (opt_initializer != null) {
            Platform.scheduler().handle(opt_initializer, bridge.getDocument().getModel());
          }
        }
        bridge.scheduleHandle(onLoaded, bridge.getDocument());
      }
    });
  }

  protected void onLoaded(final String id, Handler<Model> opt_initializer, double version,
      final DocumentBridge bridge) {
    String topic = Constants.Topic.STORE + "/" + id + Constants.Topic.WATCH;
    if (bus instanceof ReliableSubscribeBus) {
      ((ReliableSubscribeBus) bus).synchronizeSequenceNumber(topic, version - 1);
    }
    final Registration handlerReg =
        bus.subscribe(topic, new Handler<Message<JsonObject>>() {
          Transformer<CollaborativeOperation> transformer = new CollaborativeTransformer();

          @Override
          public void handle(Message<JsonObject> message) {
            JsonObject body = message.body();
            CollaborativeOperation op = transformer.createOperation(body);
            bridge.consume(op);
          }
        });
    bridge.setOutputSink(new OutputSink() {
      @Override
      public void close() {
        handlerReg.unregister();
      }

      @Override
      public void consume(CollaborativeOperation op) {
      }
    });
  }
}
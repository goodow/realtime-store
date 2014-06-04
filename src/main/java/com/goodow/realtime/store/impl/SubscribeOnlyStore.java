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
import com.goodow.realtime.channel.impl.BusProxy;
import com.goodow.realtime.channel.impl.ReconnectBus;
import com.goodow.realtime.channel.impl.ReliableSubscribeBus;
import com.goodow.realtime.channel.impl.WebSocketBus;
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
import com.goodow.realtime.store.Store;
import com.goodow.realtime.store.channel.Constants.Addr;
import com.goodow.realtime.store.channel.Constants.Key;
import com.goodow.realtime.store.impl.DocumentBridge.OutputSink;

public class SubscribeOnlyStore extends SimpleStore {
  public SubscribeOnlyStore(Bus bus) {
    super(bus);
  }

  public SubscribeOnlyStore(String serverAddress, JsonObject options) {
    this(new ReliableSubscribeBus(new ReconnectBus(serverAddress, options), options));
  }

  public Store authorize(String userId, String sessionId) {
    this.userId = userId;
    this.sessionId = sessionId;
    return this;
  }

  @Override
  public void load(final String id, final Handler<Document> onLoaded,
      final Handler<Model> opt_initializer, final Handler<Error> opt_error) {
    if (sessionId == null) {
      WebSocketBus webSocketBus = null;
      if (bus instanceof WebSocketBus) {
        webSocketBus = (WebSocketBus) bus;
      } else if (bus instanceof BusProxy && ((BusProxy) bus).getDelegate() instanceof WebSocketBus) {
        webSocketBus = (WebSocketBus) ((BusProxy) bus).getDelegate();
      }
      if (webSocketBus != null) {
        webSocketBus.login("", "", new Handler<JsonObject>() {
          @Override
          public void handle(JsonObject msg) {
            sessionId = msg.getString(Key.SESSION_ID);
            userId = msg.getString(Key.USER_ID);
            doLoad(id, onLoaded, opt_initializer, opt_error);
          }
        });
        return;
      }
    }
    doLoad(id, onLoaded, opt_initializer, opt_error);
  }

  protected void onLoaded(final String id, Handler<Model> opt_initializer, JsonObject snapshot,
      final DocumentBridge bridge) {
    String address = Addr.STORE + ":" + id;
    if (bus instanceof ReliableSubscribeBus) {
      ((ReliableSubscribeBus) bus).synchronizeSequenceNumber(address, snapshot
          .getNumber(Key.VERSION) - 1);
    }
    final Registration handlerReg =
        bus.registerHandler(address, new Handler<Message<JsonObject>>() {
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

  private void doLoad(final String id, final Handler<Document> onLoaded,
      final Handler<Model> opt_initializer, final Handler<Error> opt_error) {
    bus.send(Addr.STORE, Json.createObject().set(Key.ID, id), new Handler<Message<JsonObject>>() {
      @Override
      public void handle(Message<JsonObject> message) {
        JsonObject body = message.body();
        body = body == null ? Json.createObject().set(Key.VERSION, 0) : body;
        final DocumentBridge bridge =
            new DocumentBridge(SubscribeOnlyStore.this, id, body == null ? null : body
                .getArray(Key.SNAPSHOT), opt_error);
        onLoaded(id, opt_initializer, body, bridge);
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
}
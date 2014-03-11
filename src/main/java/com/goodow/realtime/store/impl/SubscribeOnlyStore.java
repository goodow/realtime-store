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

import com.goodow.realtime.channel.Message;
import com.goodow.realtime.channel.impl.ReliableBus;
import com.goodow.realtime.channel.impl.WebSocketBus;
import com.goodow.realtime.core.Handler;
import com.goodow.realtime.core.HandlerRegistration;
import com.goodow.realtime.json.Json;
import com.goodow.realtime.json.JsonObject;
import com.goodow.realtime.operation.RealtimeOperation;
import com.goodow.realtime.operation.Transformer;
import com.goodow.realtime.operation.TransformerImpl;
import com.goodow.realtime.store.Document;
import com.goodow.realtime.store.DocumentBridge;
import com.goodow.realtime.store.DocumentBridge.OutputSink;
import com.goodow.realtime.store.Error;
import com.goodow.realtime.store.Model;
import com.goodow.realtime.store.channel.Constants.Addr;
import com.goodow.realtime.store.channel.Constants.Key;
import com.goodow.realtime.store.util.ModelFactory;

import org.timepedia.exporter.client.Export;
import org.timepedia.exporter.client.ExportPackage;

@ExportPackage(ModelFactory.PACKAGE_PREFIX_REALTIME)
@Export
public class SubscribeOnlyStore extends SimpleStore {
  private String token;

  public SubscribeOnlyStore(ReliableBus bus) {
    super(bus);
  }

  public SubscribeOnlyStore(String serverAddress, JsonObject options) {
    this(new ReliableBus(new WebSocketBus(serverAddress, options)) {
      @Override
      protected double getSequenceNumber(String address, Object body) {
        return ((JsonObject) body).getNumber(Key.REVISION);
      }

      @Override
      protected boolean requireReliable(String address) {
        return address.startsWith(Addr.DELTA + ":");
      }
    });
  }

  public void authorize(String userId, String token) {
    this.userId = userId;
    this.token = token;
  }

  @Override
  public ReliableBus getBus() {
    return (ReliableBus) super.getBus();
  }

  /**
   * Returns an OAuth access token.
   * 
   * @return An OAuth 2.0 access token.
   */
  public String getToken() {
    return token;
  }

  @Override
  public void load(final String docId, final Handler<Document> onLoaded,
      final Handler<Model> opt_initializer, final Handler<Error> opt_error) {
    bus.send(Addr.SNAPSHOT, Json.createObject().set(Key.ID, docId).set(Key.ACCESS_TOKEN, token),
        new Handler<Message<JsonObject>>() {
          @Override
          public void handle(Message<JsonObject> message) {
            JsonObject body = message.body();
            sessionId = body.getString(Key.SESSION_ID);
            final DocumentBridge bridge =
                new DocumentBridge(SubscribeOnlyStore.this, docId, body.getArray(Key.SNAPSHOT),
                    opt_error);
            onLoad(docId, opt_initializer, body, bridge);
            bridge.scheduleHandle(onLoaded);
          }
        });
  }

  protected void onLoad(final String docId, Handler<Model> opt_initializer, JsonObject snapshot,
      final DocumentBridge bridge) {
    String address = Addr.DELTA + ":" + docId;
    getBus().synchronizeSequenceNumber(address, snapshot.getNumber(Key.REVISION));
    final HandlerRegistration handlerReg =
        bus.registerHandler(address, new Handler<Message<JsonObject>>() {
          Transformer<RealtimeOperation> transformer = new TransformerImpl<RealtimeOperation>();

          @Override
          public void handle(Message<JsonObject> message) {
            JsonObject body = message.body();
            RealtimeOperation op =
                transformer.createOperation(body.getString(Key.USER_ID), body
                    .getString(Key.SESSION_ID), body.getArray(Key.DELTAS));
            bridge.consume(op);
          }
        });
    bridge.setOutputSink(new OutputSink() {
      @Override
      public void close() {
        handlerReg.unregisterHandler();
      }

      @Override
      public void consume(RealtimeOperation op) {
      }
    });
  }
}
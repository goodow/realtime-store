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
import com.goodow.realtime.core.Handler;
import com.goodow.realtime.core.Platform;
import com.goodow.realtime.json.Json;
import com.goodow.realtime.json.JsonObject;
import com.goodow.realtime.store.DocumentBridge;
import com.goodow.realtime.store.Model;
import com.goodow.realtime.store.channel.Constants.Key;
import com.goodow.realtime.store.channel.OperationSucker;
import com.goodow.realtime.store.util.ModelFactory;

import org.timepedia.exporter.client.Export;
import org.timepedia.exporter.client.ExportPackage;

@ExportPackage(ModelFactory.PACKAGE_PREFIX_REALTIME)
@Export
public class DefaultStore extends SubscribeOnlyStore {

  public DefaultStore(Bus bus) {
    super(bus);
  }

  public DefaultStore(String serverAddress, JsonObject options) {
    super(serverAddress, options);
  }

  @Override
  protected void onLoaded(String docId, Handler<Model> opt_initializer, JsonObject snapshot,
      DocumentBridge bridge) {
    bridge.setUndoEnabled(true);

    snapshot = snapshot == null ? Json.createObject().set(Key.VERSION, 0).set(Key.SESSION_ID, sessionId) : snapshot;
    OperationSucker operationSucker = new OperationSucker(bus, docId);
    operationSucker.load(bridge, snapshot);
    if (snapshot.getNumber(Key.VERSION) == 0 && opt_initializer != null) {
      Platform.scheduler().handle(opt_initializer, bridge.getDocument().getModel());
    }
  }
}

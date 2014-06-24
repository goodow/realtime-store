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

import com.google.gwt.core.client.js.JsExport;
import com.google.gwt.core.client.js.JsNamespace;

import com.goodow.realtime.channel.Bus;
import com.goodow.realtime.core.Handler;
import com.goodow.realtime.json.JsonObject;
import com.goodow.realtime.store.Model;
import com.goodow.realtime.store.channel.OperationSucker;

@JsNamespace("$wnd.realtime.store")
public class StoreImpl extends SubscribeOnlyStore {

  public StoreImpl(Bus bus) {
    super(bus);
  }

  @JsExport
  public StoreImpl(String serverUri, JsonObject options) {
    super(serverUri, options);
  }

  @Override
  protected void onLoaded(String id, Handler<Model> opt_initializer, double version,
      DocumentBridge bridge) {
    bridge.setUndoEnabled(true);

    OperationSucker operationSucker = new OperationSucker(bus, id);
    operationSucker.load(bridge, version);
  }
}

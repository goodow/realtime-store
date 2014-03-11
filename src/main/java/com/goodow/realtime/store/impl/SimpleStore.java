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
import com.goodow.realtime.channel.impl.SimpleBus;
import com.goodow.realtime.core.Handler;
import com.goodow.realtime.core.Platform;
import com.goodow.realtime.json.Json;
import com.goodow.realtime.store.Document;
import com.goodow.realtime.store.DocumentBridge;
import com.goodow.realtime.store.Error;
import com.goodow.realtime.store.Model;
import com.goodow.realtime.store.Store;
import com.goodow.realtime.store.util.ModelFactory;

import org.timepedia.exporter.client.Export;
import org.timepedia.exporter.client.ExportPackage;

@ExportPackage(ModelFactory.PACKAGE_PREFIX_REALTIME)
@Export
public class SimpleStore implements Store {
  protected final Bus bus;
  protected String userId;
  protected String sessionId;

  public SimpleStore() {
    this(new SimpleBus(Json.createObject().set(SimpleBus.MODE_MIX, true)));
  }

  public SimpleStore(Bus bus) {
    this.bus = bus;
  }

  @Override
  public void close() {
    bus.close();
  }

  @Override
  public Bus getBus() {
    return bus;
  }

  @Override
  public final String getSessionId() {
    return sessionId;
  }

  @Override
  public String getUserId() {
    return userId;
  }

  @Override
  public void load(final String docId, final Handler<Document> onLoaded,
      final Handler<Model> opt_initializer, final Handler<Error> opt_error) {
    DocumentBridge bridge = new DocumentBridge(this, docId, null, opt_error);
    if (opt_initializer != null) {
      Platform.scheduler().handle(opt_initializer, bridge.getDocument().getModel());
    }
    bridge.scheduleHandle(onLoaded);
  }
}

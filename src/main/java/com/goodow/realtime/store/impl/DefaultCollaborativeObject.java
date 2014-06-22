/*
 * Copyright 2012 Goodow.com
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

import com.goodow.realtime.core.Handler;
import com.goodow.realtime.core.Registration;
import com.goodow.realtime.json.Json;
import com.goodow.realtime.json.JsonObject;
import com.goodow.realtime.operation.OperationComponent;
import com.goodow.realtime.store.BaseModelEvent;
import com.goodow.realtime.store.CollaborativeObject;
import com.goodow.realtime.store.EventType;
import com.goodow.realtime.store.ObjectChangedEvent;

abstract class DefaultCollaborativeObject implements CollaborativeObject {
  String id;
  final DefaultModel model;

  /**
   * @param model The document model.
   */
  protected DefaultCollaborativeObject(DefaultModel model) {
    this.model = model;
  }

  @Override public Registration addEventListener(EventType type, Handler<?> handler,
      boolean opt_capture) {
    return model.document.addEventListener(id, type, handler, opt_capture);
  }

  @Override public Registration onObjectChanged(Handler<ObjectChangedEvent> handler) {
    return addEventListener(EventType.OBJECT_CHANGED, handler, false);
  }

  @Override public String id() {
    return id;
  }

  @Override
  public String toString() {
    return toJson().toString();
  }

  abstract void consume(String userId, String sessionId, OperationComponent<?> component);

  <T> void consumeAndSubmit(OperationComponent<T> component) {
    model.bridge.consumeAndSubmit(component);
  }

  JsonObject event(String sessionId, String userId) {
    return Json.createObject().set("target", id).set("sessionId", sessionId).set("userId",
        userId).set("isLocal", model.bridge.isLocalSession(sessionId));
  }

  void fireEvent(BaseModelEvent event) {
    model.document.scheduleEvent(event);
  }

  abstract OperationComponent<?>[] toInitialization();
}

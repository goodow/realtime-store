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
package com.goodow.realtime;

import com.goodow.realtime.operation.Operation;
import com.goodow.realtime.operation.RealtimeOperation;
import com.goodow.realtime.util.NativeInterfaceFactory;

import org.timepedia.exporter.client.Export;
import org.timepedia.exporter.client.ExportPackage;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

/**
 * CollaborativeObject contains behavior common to all built in collaborative types. This class
 * should not be instantiated directly. Use the create* methods on {@link com.goodow.realtime.Model}
 * to create specific types of collaborative objects.
 */
@ExportPackage(NativeInterfaceFactory.PACKAGE_PREFIX_REALTIME)
@Export
public abstract class CollaborativeObject implements EventTarget {
  String id;
  final Model model;
  private static final Logger log = Logger.getLogger(CollaborativeObject.class.getName());

  /**
   * @param model The document model.
   */
  protected CollaborativeObject(Model model) {
    this.model = model;
  }

  @Override
  public void addEventListener(EventType type, EventHandler<?> handler, boolean opt_capture) {
    model.document.addEventListener(id, type, handler, opt_capture);
  }

  public void addObjectChangedListener(EventHandler<ObjectChangedEvent> handler) {
    addEventListener(EventType.OBJECT_CHANGED, handler, false);
  }

  /**
   * Returns the object id.
   * 
   * @return The id of the collaborative object. Readonly.
   */
  public String getId() {
    return id;
  }

  @Override
  public void removeEventListener(EventType type, EventHandler<?> handler, boolean opt_capture) {
    model.document.removeEventListener(id, type, handler, opt_capture);
  }

  public void removeObjectChangedListener(EventHandler<ObjectChangedEvent> handler) {
    removeEventListener(EventType.OBJECT_CHANGED, handler, false);
  }

  /**
   * Returns a string representation of this collaborative object.
   */
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    toString(new HashSet<String>(), sb);
    return sb.toString();
  }

  abstract void consume(RealtimeOperation operation);

  void consumeAndSubmit(Operation<?> op) {
    RealtimeOperation operation = new RealtimeOperation(id, model.document.sessionId, Realtime.getUserId(), op);
    consume(operation);
    submit(operation);
  }

  void fireEvent(BaseModelEvent event) {
    model.document.scheduleEvent(id, event.type, event);
  }

  abstract Operation<?> toInitialization();

  abstract void toString(Set<String> seen, StringBuilder sb);

  private void submit(RealtimeOperation operation) {
  }
}

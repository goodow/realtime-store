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
package com.goodow.realtime.store;

import com.goodow.realtime.core.Handler;
import com.goodow.realtime.core.HandlerRegistration;
import com.goodow.realtime.operation.OperationComponent;
import com.goodow.realtime.store.util.ModelFactory;

import org.timepedia.exporter.client.Export;
import org.timepedia.exporter.client.ExportPackage;

/**
 * CollaborativeObject contains behavior common to all built in collaborative types. This class
 * should not be instantiated directly. Use the create* methods on
 * {@link com.goodow.realtime.store.Model} to create specific types of collaborative objects.
 */
@ExportPackage(ModelFactory.PACKAGE_PREFIX_REALTIME)
@Export
public abstract class CollaborativeObject implements Disposable {
  String id;
  final Model model;

  /**
   * @param model The document model.
   */
  protected CollaborativeObject(Model model) {
    this.model = model;
  }

  /**
   * Adds an event listener to the event target. The same handler can only be added once per the
   * type. Even if you add the same handler multiple times using the same type then it will only be
   * called once when the event is dispatched.
   * 
   * @param type The type of the event to listen for.
   * @param handler The function to handle the event. The handler can also be an object that
   *          implements the handleEvent method which takes the event object as argument.
   * @param opt_capture In DOM-compliant browsers, this determines whether the listener is fired
   *          during the capture or bubble phase of the event.
   */
  public HandlerRegistration addEventListener(EventType type, Handler<?> handler,
      boolean opt_capture) {
    return model.document.addEventListener(id, type, handler, opt_capture);
  }

  public HandlerRegistration addObjectChangedListener(Handler<ObjectChangedEvent> handler) {
    return addEventListener(EventType.OBJECT_CHANGED, handler, false);
  }

  /**
   * Returns the object id.
   * 
   * @return The id of the collaborative object. Readonly.
   */
  public String getId() {
    return id;
  }

  public abstract Object toJson();

  /**
   * Returns a string representation of this collaborative object.
   */
  @Override
  public String toString() {
    return toJson().toString();
  }

  abstract void consume(String userId, String sessionId, OperationComponent<?> component);

  <T> void consumeAndSubmit(OperationComponent<T> component) {
    model.bridge.consumeAndSubmit(component);
  }

  void fireEvent(BaseModelEvent event) {
    model.document.scheduleEvent(event);
  }

  abstract OperationComponent<?>[] toInitialization();
}

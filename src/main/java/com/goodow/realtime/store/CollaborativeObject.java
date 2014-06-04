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
package com.goodow.realtime.store;

import com.goodow.realtime.core.Handler;
import com.goodow.realtime.core.Registration;
import com.google.gwt.core.client.js.JsInterface;
import com.google.gwt.core.client.js.JsProperty;

//@JsInterface
/**
 * CollaborativeObject contains behavior common to all built in collaborative types. This class
 * should not be instantiated directly. Use the create* methods on
 * {@link com.goodow.realtime.store.Model} to create specific types of collaborative objects.
 */
public interface CollaborativeObject extends EventTarget {
  Registration onObjectChanged(Handler<ObjectChangedEvent> handler);

  /**
   * Returns a string representation of this collaborative object.
   *
   * @return A string representation.
   */
  String toString();

  @JsProperty
  /**
   * Returns the object id.
   *
   * @return The ID of the collaborative object. Readonly.
   */
  String id();

  <T> T toJson();
}

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

import com.goodow.realtime.json.JsonArray;
import com.google.gwt.core.client.js.JsInterface;
import com.google.gwt.core.client.js.JsProperty;

//@JsInterface
/**
 * Event fired when a collaborative object changes. This event will bubble to all of the ancestors
 * of the changed object. It includes an array of events describing the specific changes.
 */
public interface ObjectChangedEvent extends BaseModelEvent {
  @JsProperty
  /* The specific events that document the changes that occurred on the object. */
  JsonArray events();
}

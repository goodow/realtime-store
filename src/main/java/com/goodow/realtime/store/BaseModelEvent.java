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

import com.google.gwt.core.client.js.JsType;
import com.google.gwt.core.client.js.JsProperty;

@JsType
/**
 * A base class for model events.
 */
public interface BaseModelEvent {
  @JsProperty
  /* Whether this event bubbles. */
  boolean bubbles();

  @JsProperty
  /* Whether this event originated in the local session. */
  boolean isLocal();

  @JsProperty
  /* The ID of the session that initiated the event. */
  String sessionId();

  @JsProperty
  /* The user ID of the user that initiated the event. */
  String userId();

  @JsProperty
  /* Event type. */
  EventType type();
}

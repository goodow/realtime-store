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

import com.goodow.realtime.json.JsonObject;
import com.goodow.realtime.store.BaseModelEvent;
import com.goodow.realtime.store.EventType;

abstract class BaseModelEventImpl implements BaseModelEvent {
  /**
   * Whether this event bubbles.
   */
  public final boolean bubbles;
  /**
   * Whether this event originated in the local session.
   */
  public final boolean isLocal;
  /**
   * The ID of the session that initiated the event.
   */
  public final String sessionId;
  /**
   * The user ID of the user that initiated the event.
   */
  public final String userId;
  /**
   * Event type.
   */
  public final EventType type;
  /**
   * The target object ID that generated the event.
   */
  final String target;

  /**
   * @param serialized The serialized event object.
   */
  protected BaseModelEventImpl(JsonObject serialized) {
    this.type = EventType.valueOf(serialized.getString("type"));
    this.target = serialized.getString("target");
    this.sessionId = serialized.getString("sessionId");
    this.userId = serialized.getString("userId");
    this.isLocal = serialized.getBoolean("isLocal");
    this.bubbles = serialized.getBoolean("bubbles");
  }

  @Override public String sessionId() {
    return sessionId;
  }

  @Override public EventType type() {
    return type;
  }

  @Override public String userId() {
    return userId;
  }

  @Override public boolean bubbles() {
    return bubbles;
  }

  @Override public boolean isLocal() {
    return isLocal;
  }
}

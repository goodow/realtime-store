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
import com.goodow.realtime.store.EventType;
import com.goodow.realtime.store.ValueChangedEvent;

class ValueChangedEventImpl extends BaseModelEventImpl implements ValueChangedEvent {
  /**
   * The new property value.
   */
  public final Object newValue;
  /**
   * The old property value.
   */
  public final Object oldValue;
  /**
   * The property whose value changed.
   */
  public final String property;

  /**
   * @param serialized The serialized event object.
   */
  public ValueChangedEventImpl(JsonObject serialized) {
    super(serialized.set("type", EventType.VALUE_CHANGED.name()).set("bubbles", false));
    this.property = serialized.getString("property");
    this.newValue = serialized.get("newValue");
    this.oldValue = serialized.get("oldValue");
  }

  @Override public Object newValue() {
    return newValue;
  }

  @Override public Object oldValue() {
    return oldValue;
  }

  @Override public String property() {
    return property;
  }
}

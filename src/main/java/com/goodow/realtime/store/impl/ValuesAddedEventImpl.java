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

import com.goodow.realtime.json.JsonArray;
import com.goodow.realtime.json.JsonObject;
import com.goodow.realtime.store.EventType;
import com.goodow.realtime.store.ValuesAddedEvent;

class ValuesAddedEventImpl extends BaseModelEventImpl implements ValuesAddedEvent {
  /**
   * The index of the first added value.
   */
  public final int index;
  /**
   * The values that were added.
   */
  public final JsonArray values;

  /**
   * @param serialized The serialized event object.
   */
  public ValuesAddedEventImpl(JsonObject serialized) {
    super(serialized.set("type", EventType.VALUES_ADDED.name()).set("bubbles", false));
    this.index = (int) serialized.getNumber("index");
    this.values = serialized.getArray("values");
  }

  @Override public int index() {
    return index;
  }

  @Override
  public JsonArray values() {
    return values;
  }
}

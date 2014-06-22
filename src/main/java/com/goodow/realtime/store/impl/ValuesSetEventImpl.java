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
import com.goodow.realtime.store.ValuesSetEvent;

class ValuesSetEventImpl extends BaseModelEventImpl implements ValuesSetEvent {
  /**
   * The index of the first value that was replaced.
   */
  public final int index;
  /**
   * The old values.
   */
  public final JsonArray oldValues;
  /**
   * The new values.
   */
  public final JsonArray newValues;

  /**
   * @param serialized The serialized event object.
   */
  public ValuesSetEventImpl(JsonObject serialized) {
    super(serialized.set("type", EventType.VALUES_SET.name()).set("bubbles", false));
    this.index = (int) serialized.getNumber("index");
    this.oldValues = serialized.getArray("oldValues");
    this.newValues = serialized.getArray("newValues");
  }

  @Override public int index() {
    return index;
  }

  @Override
  public JsonArray newValues() {
    return newValues;
  }

  @Override
  public JsonArray oldValues() {
    return oldValues;
  }
}

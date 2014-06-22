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
import com.goodow.realtime.store.ReferenceShiftedEvent;

class ReferenceShiftedEventImpl extends BaseModelEventImpl implements ReferenceShiftedEvent {
  /**
   * The new index.
   */
  public final int newIndex;
  /**
   * The previous index.
   */
  public final int oldIndex;

  /**
   * @param serialized The serialized event object.
   */
  public ReferenceShiftedEventImpl(JsonObject serialized) {
    super(serialized.set("type", EventType.REFERENCE_SHIFTED.name()).set("bubbles", false));
    this.oldIndex = (int) serialized.getNumber("oldIndex");
    this.newIndex = (int) serialized.getNumber("newIndex");
  }

  @Override public int newIndex() {
    return newIndex;
  }

  @Override public int oldIndex() {
    return oldIndex;
  }
}

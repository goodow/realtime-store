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
import com.goodow.realtime.store.Document;
import com.goodow.realtime.store.DocumentSaveStateChangedEvent;
import com.goodow.realtime.store.EventType;

public class DocumentSaveStateChangedEventImpl implements DocumentSaveStateChangedEvent {
  public final boolean isPending;
  public final boolean isSaving;

  /**
   * @param document The document being saved.
   * @param serialized The serialized event object.
   */
  public DocumentSaveStateChangedEventImpl(Document document, JsonObject serialized) {
    this.isSaving = serialized.getBoolean("isSaving");
    this.isPending = serialized.getBoolean("isPending");;
  }

  @Override public boolean isPending() {
    return isPending;
  }

  @Override public boolean isSaving() {
    return isSaving;
  }

  @Override
  public EventType type() {
    return EventType.DOCUMENT_SAVE_STATE_CHANGED;
  }
}

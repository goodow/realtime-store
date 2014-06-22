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
import com.goodow.realtime.store.Model;
import com.goodow.realtime.store.UndoRedoStateChangedEvent;

class UndoRedoStateChangedEventImpl implements UndoRedoStateChangedEvent {
  /**
   * True if you can currently redo, false otherwise.
   */
  public final boolean canRedo;
  /**
   * True if you can currently undo, false otherwise.
   */
  public final boolean canUndo;

  /**
   * @param source The source object.
   * @param canUndo A serialized undo/redo state changed event.
   */
  public UndoRedoStateChangedEventImpl(Model source, JsonObject serialized) {
    this.canUndo = serialized.getBoolean("canUndo");
    this.canRedo = serialized.getBoolean("canRedo");
  }

  @Override public boolean canRedo() {
    return canRedo;
  }

  @Override public boolean canUndo() {
    return canUndo;
  }
}

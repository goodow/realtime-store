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

@JsType
/**
 * An event indicating that canUndo or canRedo changed.
 */
public interface UndoRedoStateChangedEvent {
  /* True if you can currently redo, false otherwise. */
  boolean canRedo();

  /* True if you can currently undo, false otherwise. */
  boolean canUndo();
}

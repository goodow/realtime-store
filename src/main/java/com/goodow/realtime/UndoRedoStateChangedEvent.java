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
package com.goodow.realtime;

import com.goodow.realtime.util.NativeInterfaceFactory;

import org.timepedia.exporter.client.Export;
import org.timepedia.exporter.client.ExportPackage;

/**
 * An event indicating that canUndo or canRedo changed.
 */
@ExportPackage(NativeInterfaceFactory.PACKAGE_PREFIX_REALTIME)
@Export
public class UndoRedoStateChangedEvent implements Disposable {
  /**
   * True if you can currently redo, false otherwise.
   */
  public final boolean canRedo;
  /**
   * True if you can currently undo, false otherwise.
   */
  public final boolean canUndo;

  /**
   * @param model The model whose state changed.
   * @param canUndo True if you can currently undo.
   * @param canRedo True if you can currently redo.
   */
  public UndoRedoStateChangedEvent(Model model, boolean canUndo, boolean canRedo) {
    this.canUndo = canUndo;
    this.canRedo = canRedo;
  }
}

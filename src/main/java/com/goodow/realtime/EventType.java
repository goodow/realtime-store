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
import org.timepedia.exporter.client.Exportable;

/**
 * Events fired by the document or collaborative objects.
 */
@ExportPackage(NativeInterfaceFactory.PACKAGE_PREFIX_REALTIME)
@Export
public enum EventType implements Exportable {
  /**
   * A new collaborator joined the document. Listen on the {@link com.goodow.realtime.Document} for
   * these changes.
   */
  COLLABORATOR_JOINED,
  /**
   * A collaborator left the document. Listen on the {@link com.goodow.realtime.Document} for these
   * changes.
   */
  COLLABORATOR_LEFT,
  /**
   * The document save state changed. Listen on the {@link com.goodow.realtime.Document} for these
   * changes.
   */
  DOCUMENT_SAVE_STATE_CHANGED,
  /**
   * A collaborative object has changed. This event wraps a specific event, and bubbles to
   * ancestors.
   */
  OBJECT_CHANGED,
  /**
   * An index reference changed.
   */
  REFERENCE_SHIFTED,
  /**
   * Text has been removed from a string.
   */
  TEXT_DELETED,
  /**
   * Text has been inserted into a string.
   */
  TEXT_INSERTED,
  /**
   * New values have been added to the list.
   */
  VALUES_ADDED,
  /**
   * Values have been removed from the list.
   */
  VALUES_REMOVED,
  /**
   * Values in a list are changed in place.
   */
  VALUES_SET,
  /**
   * A map or custom object value has changed. Note this could be a new value or deleted value.
   */
  VALUE_CHANGED,
  /**
   * 
   */
  UNDO_REDO_STATE_CHANGED;

  @Override
  public String toString() {
    return name().toLowerCase();
  }
}

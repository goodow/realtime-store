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
package com.goodow.realtime.databinding;

import com.goodow.realtime.CollaborativeObject;
import com.goodow.realtime.Disposable;
import com.goodow.realtime.util.NativeInterfaceFactory;

import org.timepedia.exporter.client.Export;
import org.timepedia.exporter.client.ExportPackage;

/**
 * A binding between a collaborative object in the data model and a DOM element.
 */
@ExportPackage(NativeInterfaceFactory.PACKAGE_PREFIX_DATABINDING)
@Export
public class Binding implements Disposable {
  private final CollaborativeObject collaborativeObject;
  private final Object domElement;

  /**
   * @param collaborativeObject The collaborative object to bind.
   * @param domElement The DOM element to bind.
   */
  public Binding(CollaborativeObject collaborativeObject, Object domElement) {
    this.collaborativeObject = collaborativeObject;
    this.domElement = domElement;
  }

  /**
   * @return The collaborative object that this registration binds to the DOM element.
   */
  public CollaborativeObject getCollaborativeObject() {
    return collaborativeObject;
  }

  /**
   * @return The DOM element that this registration binds to the collaborative object.
   */
  public Object getDomElement() {
    return domElement;
  }

  /**
   * Unbinds the DOM element from the collaborative object.
   */
  public void unbind() {
  }
}

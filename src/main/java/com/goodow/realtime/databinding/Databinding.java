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

import com.goodow.realtime.CollaborativeString;
import com.goodow.realtime.util.NativeInterfaceFactory;

import org.timepedia.exporter.client.Export;
import org.timepedia.exporter.client.ExportPackage;
import org.timepedia.exporter.client.Exportable;

/**
 * A namespace that includes classes and methods for binding collaborative objects to UI elements.
 */
@ExportPackage("")
@Export(NativeInterfaceFactory.PACKAGE_PREFIX_DATABINDING)
public class Databinding implements Exportable {
  /**
   * Binds a text input element to an collaborative string. Once bound, any change to the
   * collaborative string (including changes from other remote collaborators) is immediately
   * displayed in the text editing control. Conversely, any change in the text editing control is
   * reflected in the data model.
   * 
   * @param string The collaborative string to bind.
   * @param textInputElement The text input element to bind. This must be a textarea element or an
   *          input type=text element.
   * @return A binding registration that can be later used to remove the binding.
   * @throws AlreadyBoundError
   */
  public static Binding bindString(CollaborativeString string, Object textInputElement)
      throws AlreadyBoundError {
    return null;
  }
}

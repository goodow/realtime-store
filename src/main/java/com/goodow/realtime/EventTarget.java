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
 * An event target which can dispatch events to interested listeners. Listeners subscribe via
 * addEventListener.
 */
@ExportPackage(NativeInterfaceFactory.PACKAGE_PREFIX_REALTIME)
@Export
public interface EventTarget extends Disposable {
  /**
   * Adds an event listener to the event target. The same handler can only be added once per the
   * type. Even if you add the same handler multiple times using the same type then it will only be
   * called once when the event is dispatched.
   * 
   * @param type The type of the event to listen for.
   * @param handler The function to handle the event. The handler can also be an object that
   *          implements the handleEvent method which takes the event object as argument.
   * @param opt_capture In DOM-compliant browsers, this determines whether the listener is fired
   *          during the capture or bubble phase of the event.
   */
  void addEventListener(EventType type, EventHandler<?> handler, boolean opt_capture);

  /**
   * Removes an event listener from the event target. The handler must be the same object as the one
   * added. If the handler has not been added then nothing is done.
   * 
   * @param type The type of the event to listen for.
   * @param handler The function to handle the event. The handler can also be an object that
   *          implements the handleEvent method which takes the event object as argument.
   * @param opt_capture In DOM-compliant browsers, this determines whether the listener is fired
   *          during the capture or bubble phase of the event.
   */
  void removeEventListener(EventType type, EventHandler<?> handler, boolean opt_capture);
}

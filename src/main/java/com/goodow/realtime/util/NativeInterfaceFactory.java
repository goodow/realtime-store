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
package com.goodow.realtime.util;

import com.goodow.realtime.CollaborativeString;

public interface NativeInterfaceFactory {
  String PACKAGE_PREFIX_REALTIME = "gdr";
  String PACKAGE_PREFIX_CUSTOM = "gdr.custom";
  String PACKAGE_PREFIX_DATABINDING = "gdr.databinding";
  String PACKAGE_PREFIX_OVERLAY = "gdr._ExportOverlay_";

  String JS_REGISTER_PROPERTIES = "J2ObjC blocked by JSNI";
  String JS_REGISTER_MATHODS = "J2ObjC blocked by JSNI";

  /**
   * A deferred command is executed after the event loop returns.
   */
  void scheduleDeferred(Runnable cmd);

  void setText(CollaborativeString str, String text);
}

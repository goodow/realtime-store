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
package com.goodow.realtime.store.util.impl;

import com.goodow.realtime.core.Handler;
import com.goodow.realtime.store.Disposable;
import com.goodow.realtime.store.util.ModelFactory;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.JavaScriptObject;

import org.timepedia.exporter.client.ExportClosure;
import org.timepedia.exporter.client.ExportOverlay;
import org.timepedia.exporter.client.ExportPackage;
import org.timepedia.exporter.client.ExporterUtil;

import java.util.Comparator;
import java.util.logging.Logger;

public class JsModelFactory implements ModelFactory, EntryPoint {
  @ExportPackage(ModelFactory.PACKAGE_PREFIX_OVERLAY)
  @ExportClosure
  public interface __ComparatorExportOverlay__ extends ExportOverlay<Comparator<Object>> {
    int compare(Object o1, Object o2);
  }
  @ExportPackage(ModelFactory.PACKAGE_PREFIX_OVERLAY)
  @ExportClosure
  public interface __HandlerExportOverlay__ extends ExportOverlay<Handler<Disposable>> {
    void handle(Disposable event);
  }

  private static final Logger log = Logger.getLogger(JsModelFactory.class.getName());

  static JavaScriptObject wrap(Object o) {
    if (o instanceof Number) {
      // return (JavaScriptObject) JsJsonNumber.create(((Number) o).doubleValue());
      // } else if (o instanceof Boolean) {
      // return (JavaScriptObject) JsJsonBoolean.create(((Boolean) o).booleanValue());
      // } else if (o instanceof JsonElement) {
      return (JavaScriptObject) o;
    } else {
      return ExporterUtil.wrap(o);
    }
  }

  @Override
  public void onModuleLoad() {
    ExporterUtil.exportAll();
    __jsniOnLoad__();
  }

  // @formatter:off
  private native void __jsniOnLoad__() /*-{
    $wnd.gdr = $wnd.gdr || $wnd.good.realtime;
  }-*/;
  // @formatter:on
}

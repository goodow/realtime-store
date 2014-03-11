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
package com.goodow.realtime.store;

import com.goodow.realtime.store.util.ModelFactory;

import com.google.common.annotations.GwtIncompatible;

import org.timepedia.exporter.client.Export;
import org.timepedia.exporter.client.ExportAfterCreateMethod;
import org.timepedia.exporter.client.ExportPackage;
import org.timepedia.exporter.client.NoExport;

/**
 * Event fired when items in a collaborative list are changed in place.
 */
@ExportPackage(ModelFactory.PACKAGE_PREFIX_REALTIME)
@Export(all = true)
public class ValuesSetEvent extends BaseModelEvent {
  @GwtIncompatible(ModelFactory.JS_REGISTER_PROPERTIES)
  @ExportAfterCreateMethod
  // @formatter:off
  public native static void __jsniRunAfter__() /*-{
    var _ = $wnd.good.realtime.ValuesSetEvent.prototype;
    _.getNewValues = function() {
      var values = this.g.@com.goodow.realtime.store.ValuesSetEvent::getNewValues()();
      var toRtn = [];
      for (var i=0, len=values.length; i<len; i++) {
        toRtn.push(@com.goodow.realtime.store.util.impl.JsModelFactory::wrap(Ljava/lang/Object;)(values[i]));
      }
      return toRtn;
    };
    _.getOldValues = function() {
      var values = this.g.@com.goodow.realtime.store.ValuesSetEvent::getOldValues()();
      var toRtn = [];
      for (var i=0, len=values.length; i<len; i++) {
        toRtn.push(@com.goodow.realtime.store.util.impl.JsModelFactory::wrap(Ljava/lang/Object;)(values[i]));
      }
      return toRtn;
    };
  }-*/;
  // @formatter:on

  /**
   * The index of the first value that was replaced.
   */
  public final int index;
  /**
   * The new values.
   */
  public final Object[] oldValues;
  /**
   * The old values.
   */
  public final Object[] newValues;

  /**
   * @param target The target object that generated the event.
   * @param sessionId The id of the session that initiated the event.
   * @param userId The user id of the user that initiated the event.
   * @param index The index of the change.
   * @param oldValues The old values.
   * @param newValues The new values.
   */
  public ValuesSetEvent(CollaborativeList target, String sessionId, String userId, int index,
      Object[] oldValues, Object[] newValues) {
    super(EventType.VALUES_SET, target, sessionId, userId, false);
    this.index = index;
    this.oldValues = oldValues;
    this.newValues = newValues;
  }

  public int getIndex() {
    return index;
  }

  @NoExport
  public Object[] getNewValues() {
    return newValues;
  }

  @NoExport
  public Object[] getOldValues() {
    return oldValues;
  }
}

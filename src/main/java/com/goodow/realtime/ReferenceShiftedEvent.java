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
 * Event fired when an index reference shifts
 */
@ExportPackage(NativeInterfaceFactory.PACKAGE_PREFIX_REALTIME)
@Export(all = true)
public class ReferenceShiftedEvent extends BaseModelEvent {
  /**
   * The new index.
   */
  public final int newIndex;
  /**
   * The previous index.
   */
  public final int oldIndex;

  /**
   * @param target The reference that shifted.
   * @param oldIndex The previous index.
   * @param newIndex The new index.
   * @param sessionId The id of the session.
   * @param userId The id of the user.
   */
  public ReferenceShiftedEvent(IndexReference target, int oldIndex, int newIndex, String sessionId,
      String userId) {
    super(EventType.REFERENCE_SHIFTED, target, sessionId, userId, false);
    this.oldIndex = oldIndex;
    this.newIndex = newIndex;
  }
}

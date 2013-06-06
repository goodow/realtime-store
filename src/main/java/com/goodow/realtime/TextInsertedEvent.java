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

import com.goodow.realtime.model.util.ModelFactory;

import org.timepedia.exporter.client.Export;
import org.timepedia.exporter.client.ExportPackage;

/**
 * Event fired when text is inserted into a string.
 */
@ExportPackage(ModelFactory.PACKAGE_PREFIX_REALTIME)
@Export(all = true)
public class TextInsertedEvent extends BaseModelEvent {
  /**
   * The index of the change.
   */
  public final int index;
  /**
   * The inserted text
   */
  public final String text;

  /**
   * @param target The target object that generated the event.
   * @param sessionId The id of the session that initated the event.
   * @param userId The user id of the user that initiated the event.
   * @param index The index of the change.
   * @param text The inserted text.
   */
  public TextInsertedEvent(CollaborativeString target, String sessionId, String userId, int index,
      String text) {
    super(EventType.TEXT_INSERTED, target, sessionId, userId, false);
    this.index = index;
    this.text = text;
  }
}

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
 * Event fired when a collaborative object changes. This event will bubble to all of the ancestors
 * of the changed object. It includes an array of events describing the specific changes.
 */
@ExportPackage(ModelFactory.PACKAGE_PREFIX_REALTIME)
@Export(all = true)
public class ObjectChangedEvent extends BaseModelEvent {
  /**
   * The specific events that document the changes that occurred on the object.
   */
  public final BaseModelEvent[] events;

  /**
   * @param target The target object that generated the event.
   * @param sessionId The source object that generated the event.
   * @param userId The user id of the user that initiated the event.
   * @param events The events that caused the object to change.
   */
  public ObjectChangedEvent(CollaborativeObject target, String sessionId, String userId,
      BaseModelEvent... events) {
    super(EventType.OBJECT_CHANGED, target, sessionId, userId, true);
    this.events = events;
  }
}

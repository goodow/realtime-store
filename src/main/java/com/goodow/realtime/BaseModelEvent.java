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
 * A base class for model events.
 */
@ExportPackage(NativeInterfaceFactory.PACKAGE_PREFIX_REALTIME)
@Export
public abstract class BaseModelEvent implements Disposable {

  /**
   * Whether this event should bubble to ancestors.
   */
  public final boolean bubbles;
  /**
   * Whether this event originated in the local session.
   */
  public final boolean isLocal;
  /**
   * The id of the session that initiated the event.
   */
  public final String sessionId;
  /**
   * Event type.
   */
  public final EventType type;
  /**
   * The user id of the user that initiated the event.
   */
  public final String userId;
  final CollaborativeObject target;

  /**
   * @param type The event type.
   * @param target The target object that generated the event.
   * @param sessionId The id of the session that initiated the event.
   * @param userId The user id of the user that initiated the event.
   * @param bubbles Whether or not this event should bubble to ancestors.
   */
  protected BaseModelEvent(EventType type, CollaborativeObject target, String sessionId,
      String userId, boolean bubbles) {
    this.type = type;
    this.target = target;
    this.sessionId = sessionId;
    this.userId = userId;
    this.isLocal = target.model.bridge.isLocalSession(sessionId);
    this.bubbles = bubbles;
  }
}

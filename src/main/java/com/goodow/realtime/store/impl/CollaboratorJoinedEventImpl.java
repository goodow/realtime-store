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
package com.goodow.realtime.store.impl;

import com.goodow.realtime.store.Collaborator;
import com.goodow.realtime.store.CollaboratorJoinedEvent;
import com.goodow.realtime.store.Document;
import com.goodow.realtime.store.EventType;

class CollaboratorJoinedEventImpl implements CollaboratorJoinedEvent {
  /**
   * The collaborator that joined.
   */
  public final Collaborator collaborator;

  /**
   * @param document The document the collaborator joined.
   * @param collaborator The collaborator that joined.
   */
  public CollaboratorJoinedEventImpl(Document document, Collaborator collaborator) {
    this.collaborator = collaborator;
  }

  /**
   * Creates an event from serialized JSON.
   * 
   * @param source The source object.
   * @param serialized A serialized content object.
   * @return A change event.
   */
  public CollaboratorJoinedEvent deserialize(Document source, Object serialized) {
    return null;
  }

  @Override public Collaborator collaborator() {
    return collaborator;
  }

  @Override
  public EventType type() {
    return EventType.COLLABORATOR_JOINED;
  }
}

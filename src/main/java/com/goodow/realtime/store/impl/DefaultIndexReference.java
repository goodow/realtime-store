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

import com.goodow.realtime.core.Handler;
import com.goodow.realtime.core.Registration;
import com.goodow.realtime.json.Json;
import com.goodow.realtime.json.JsonObject;
import com.goodow.realtime.operation.OperationComponent;
import com.goodow.realtime.operation.create.CreateComponent;
import com.goodow.realtime.operation.cursor.ReferenceShiftedComponent;
import com.goodow.realtime.store.CollaborativeObject;
import com.goodow.realtime.store.EventType;
import com.goodow.realtime.store.IndexReference;
import com.goodow.realtime.store.ReferenceShiftedEvent;

class DefaultIndexReference extends DefaultCollaborativeObject implements IndexReference {
  private String referencedObjectId;
  private int index = -1;
  private boolean canBeDeleted;

  /**
   * @param model The document model.
   */
  DefaultIndexReference(DefaultModel model) {
    super(model);
  }

  @Override public Registration onReferenceShifted(Handler<ReferenceShiftedEvent> handler) {
    return addEventListener(EventType.REFERENCE_SHIFTED, handler, false);
  }

  @Override public boolean canBeDeleted() {
    return canBeDeleted;
  }

  @Override public int index() {
    return index;
  }

  @Override public <T extends CollaborativeObject> T referencedObject() {
    return model.getObject(referencedObjectId);
  }

  @Override public void index(int index) {
    if (index == this.index) {
      return;
    }
    ReferenceShiftedComponent op =
        new ReferenceShiftedComponent(id, referencedObjectId, index, canBeDeleted, this.index);
    consumeAndSubmit(op);
  }

  @Override
  public JsonObject toJson() {
    JsonObject json = Json.createObject();
    json.set("id", id).set("referencedObjectId", referencedObjectId).set("index", index).set(
        "canBeDeleted", canBeDeleted);
    return json;
  }

  @Override
  protected void consume(final String userId, final String sessionId,
      OperationComponent<?> component) {
    ReferenceShiftedComponent op = (ReferenceShiftedComponent) component;
    assert op.oldIndex == index();
    referencedObjectId = op.referencedObjectId;
    index = op.newIndex;
    canBeDeleted = op.canBeDeleted;
    if (op.oldIndex != -1) {
      ReferenceShiftedEvent event =
          new DefaultReferenceShiftedEvent(event(sessionId, userId).set("oldIndex", op.oldIndex).set(
              "newIndex", op.newIndex));
      fireEvent(event);
    }
  }

  void setIndex(boolean isInsert, int index, int length, String sessionId, String userId) {
    int cursor = index();
    if (cursor < index) {
      return;
    }
    int newIndex = -2;
    if (isInsert) {
      newIndex = cursor + length;
    } else {
      if (cursor < index + length) {
        if (canBeDeleted) {
          newIndex = -1;
        } else {
          newIndex = index;
        }
      } else {
        newIndex = cursor - length;
      }
    }
    ReferenceShiftedComponent op =
        new ReferenceShiftedComponent(id, referencedObjectId, newIndex, canBeDeleted, cursor);
    consume(userId, sessionId, op);
  }

  @Override
  OperationComponent<?>[] toInitialization() {
    ReferenceShiftedComponent op =
        new ReferenceShiftedComponent(id, referencedObjectId, index, canBeDeleted, index);
    return new OperationComponent[] {new CreateComponent(id, CreateComponent.INDEX_REFERENCE), op};
  }
}

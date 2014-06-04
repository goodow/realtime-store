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

import com.goodow.realtime.channel.Message;
import com.goodow.realtime.core.Handler;
import com.goodow.realtime.core.Platform;
import com.goodow.realtime.core.Registration;
import com.goodow.realtime.core.Registrations;
import com.goodow.realtime.json.Json;
import com.goodow.realtime.json.JsonArray;
import com.goodow.realtime.json.JsonArray.ListIterator;
import com.goodow.realtime.json.JsonObject;
import com.goodow.realtime.json.JsonObject.MapIterator;
import com.goodow.realtime.store.BaseModelEvent;
import com.goodow.realtime.store.Collaborator;
import com.goodow.realtime.store.CollaboratorJoinedEvent;
import com.goodow.realtime.store.CollaboratorLeftEvent;
import com.goodow.realtime.store.Document;
import com.goodow.realtime.store.DocumentSaveStateChangedEvent;
import com.goodow.realtime.store.Error;
import com.goodow.realtime.store.EventType;
import com.goodow.realtime.store.channel.Constants.Addr;
import com.goodow.realtime.store.channel.Constants.Key;

class DefaultDocument implements Document {
  private final JsonArray collaborators;
  private final DefaultModel model;
  final Registrations handlerRegs;

  private boolean isEventsScheduled = false;
  private JsonArray eventsToFire; // ArrayList<BaseModelEvent>
  private final Handler<Void> eventsTask = new Handler<Void>() {
    private JsonArray evtsToFire; // ArrayList<BaseModelEvent>
    private JsonObject eventsById; // Map<String, List<BaseModelEvent>>

    @Override
    public void handle(Void ignore) {
      evtsToFire = eventsToFire;
      eventsToFire = null;
      isEventsScheduled = false;
      eventsById = Json.createObject();
      evtsToFire.forEach(new ListIterator<DefaultBaseModelEvent>() {
        @Override
        public void call(int index, DefaultBaseModelEvent event) {
          assert !event.bubbles;
          String id = event.target;
          bubblingToAncestors(id, event, Json.createArray());
          fireEvent(event);
        }
      });
      eventsById.forEach(new MapIterator<JsonArray>() {
        @Override
        public void call(String key, JsonArray events) {
          DefaultBaseModelEvent first = events.get(0);
          DefaultObjectChangedEvent objectChangedEvent =
              new DefaultObjectChangedEvent(Json.createObject().set("target", key).set("sessionId",
                  first.sessionId).set(Key.USER_ID, first.userId).set("isLocal",
                  model.bridge.isLocalSession(first.sessionId)).set("events", events));
          fireEvent(objectChangedEvent);
        }
      });
      evtsToFire = null;
      eventsById = null;
    }

    private void bubblingToAncestors(String id, final BaseModelEvent event, final JsonArray seen) {
      if (seen.indexOf(id) != -1) {
        return;
      }
      if (!eventsById.has(id)) {
        eventsById.set(id, Json.createArray().push(event));
      } else {
        eventsById.getArray(id).push(event);
      }
      seen.push(id);

      model.getParents(id).forEach(new ListIterator<String>() {
        @Override
        public void call(int index, String parent) {
          bubblingToAncestors(parent, event, seen);
        }
      });
    }

    private void fireEvent(DefaultBaseModelEvent event) {
      model.bridge.store.getBus().publishLocal(
          Addr.EVENT + event.type + ":" + model.bridge.id + ":" + event.target, event);
    }
  };

  /**
   * @param internalApi The driver for the GWT collaborative libraries.
   * @param errorHandler The third-party error handling function.
   */
  DefaultDocument(final DocumentBridge internalApi, final Handler<Error> errorHandler) {
    model = new DefaultModel(internalApi, this);
    collaborators = Json.createArray();
    handlerRegs = new Registrations();

    if (errorHandler != null) {
      handlerRegs.wrap(internalApi.store.getBus().registerLocalHandler(
          Addr.EVENT + Addr.DOCUMENT_ERROR + ":" + internalApi.id, new Handler<Message<com.goodow.realtime.store.Error>>() {
        @Override
        public void handle(Message<Error> message) {
          errorHandler.handle(message.body());
        }
      }));
    }
  }

  @Override
  public Registration onCollaboratorJoined(final Handler<CollaboratorJoinedEvent> handler) {
    return addEventListener(null, EventType.COLLABORATOR_JOINED, handler, false);
  }

  @Override
  public Registration onCollaboratorLeft(final Handler<CollaboratorLeftEvent> handler) {
    return addEventListener(null, EventType.COLLABORATOR_LEFT, handler, false);
  }

  @Override
  public Registration onDocumentSaveStateChanged(final Handler<DocumentSaveStateChangedEvent> handler) {
    return addEventListener(null, EventType.DOCUMENT_SAVE_STATE_CHANGED, handler, false);
  }

  @Override public void close() {
    model.bridge.outputSink.close();
    collaborators.clear();
    handlerRegs.unregister();
  }

  @Override public JsonArray getCollaborators() {
    return collaborators.copy();
  }

  @Override public DefaultModel getModel() {
    return model;
  }

  @SuppressWarnings("rawtypes")
  Registration addEventListener(String objectId, EventType type, final Handler handler,
      boolean opt_capture) {
    if (type == null || handler == null) {
      throw new NullPointerException((type == null ? "type" : "handler") + " was null.");
    }
    return handlerRegs.wrap(model.bridge.store.getBus().registerLocalHandler(
        Addr.EVENT + type + ":" + model.bridge.id + (objectId == null ? "" : (":" + objectId)),
        new Handler<Message<?>>() {
          @SuppressWarnings("unchecked")
          @Override
          public void handle(Message<?> message) {
            handler.handle(message.body());
          }
        }));
  }

  void onCollaboratorChanged(boolean isJoined, Collaborator collaborator) {
    int index = collaborators.indexOf(collaborator);
    if (isJoined) {
      if (index == -1) {
        collaborators.push(collaborator);
        model.bridge.store.getBus().publishLocal(
            Addr.EVENT + EventType.COLLABORATOR_JOINED + ":" + model.bridge.id,
            new DefaultCollaboratorJoinedEvent(this, collaborator));
      }
    } else {
      if (index != -1) {
        collaborators.remove(index);
        model.bridge.store.getBus().publishLocal(
            Addr.EVENT + EventType.COLLABORATOR_LEFT + ":" + model.bridge.id,
            new DefaultCollaboratorLeftEvent(this, collaborator));
      }
    }
  }

  void scheduleEvent(BaseModelEvent event) {
    assert !event.bubbles();
    if (eventsToFire == null) {
      eventsToFire = Json.createArray();
    }
    eventsToFire.push(event);
    if (!isEventsScheduled) {
      isEventsScheduled = true;
      Platform.scheduler().scheduleDeferred(eventsTask);
    }
  }
}

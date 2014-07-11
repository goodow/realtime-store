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

import com.goodow.realtime.channel.Bus;
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
import com.goodow.realtime.store.channel.Constants;
import com.goodow.realtime.store.channel.Constants.Key;

class DocumentImpl implements Document {
  private final ModelImpl model;
  final Registrations handlerRegs;
  final JsonObject collaborators;

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
      evtsToFire.forEach(new ListIterator<BaseModelEventImpl>() {
        @Override
        public void call(int index, BaseModelEventImpl event) {
          assert !event.bubbles;
          String id = event.target;
          bubblingToAncestors(id, event, Json.createArray());
        }
      });
      eventsById.forEach(new MapIterator<JsonArray>() {
        @Override
        public void call(String key, JsonArray events) {
          BaseModelEventImpl first = events.get(0);
          ObjectChangedEventImpl objectChangedEvent =
              new ObjectChangedEventImpl(Json.createObject().set("target", key).set("sessionId",
                  first.sessionId).set("userId", first.userId).set("isLocal",
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
  };

  /**
   * @param internalApi The driver for the GWT collaborative libraries.
   * @param errorHandler The third-party error handling function.
   */
  DocumentImpl(final DocumentBridge internalApi, final Handler<Error> errorHandler) {
    model = new ModelImpl(internalApi, this);
    handlerRegs = new Registrations();
    collaborators = Json.createObject();

    Bus bus = internalApi.store.getBus();
    if (errorHandler != null) {
      handlerRegs.wrap(bus.subscribeLocal(
          Constants.Topic.STORE + "/" + internalApi.id + "/" + Constants.Topic.DOCUMENT_ERROR,
          new Handler<Message<Error>>() {
            @Override
            public void handle(Message<Error> message) {
              Platform.scheduler().handle(errorHandler, message.body());
            }
          }));
    }

    handlerRegs.wrap(bus.subscribe(
        Constants.Topic.STORE + "/" + internalApi.id + Constants.Topic.PRESENCE
        + Constants.Topic.WATCH, new Handler<Message<JsonObject>>() {
          @Override
          public void handle(Message<JsonObject> message) {
            JsonObject body = message.body().set(Key.IS_ME, false);
            Collaborator collaborator = new CollaboratorImpl(body);
            boolean isJoined = !body.has(Key.IS_JOINED) || body.getBoolean(Key.IS_JOINED);
            String sessionId = collaborator.sessionId();
            if (isJoined) {
              if (!collaborators.has(sessionId)) {
                collaborators.set(sessionId, collaborator);
                model.bridge.store.getBus().publishLocal(
                    Constants.Topic.STORE + "/" + model.bridge.id + "/"
                    + EventType.COLLABORATOR_JOINED,
                    new CollaboratorJoinedEventImpl(DocumentImpl.this, collaborator));
              }
            } else {
              if (collaborators.has(sessionId)) {
                collaborators.remove(sessionId);
                model.bridge.store.getBus().publishLocal(
                    Constants.Topic.STORE + "/" + model.bridge.id + "/"
                    + EventType.COLLABORATOR_LEFT,
                    new CollaboratorLeftEventImpl(DocumentImpl.this, collaborator));
              }
            }
          }
        }));
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
  public Registration onDocumentSaveStateChanged(
      final Handler<DocumentSaveStateChangedEvent> handler) {
    return addEventListener(null, EventType.DOCUMENT_SAVE_STATE_CHANGED, handler, false);
  }

  @Override public void close() {
    model.bridge.outputSink.close();
    collaborators.clear();
    handlerRegs.unregister();
  }

  @Override public JsonArray getCollaborators() {
    final JsonArray toRtn = Json.createArray();
    collaborators.forEach(new MapIterator<Collaborator>() {
      @Override
      public void call(String key, Collaborator collaborator) {
        toRtn.push(collaborator);
      }
    });
    return toRtn;
  }

  @Override public ModelImpl getModel() {
    return model;
  }

  @SuppressWarnings("rawtypes")
  Registration addEventListener(String objectId, EventType type, final Handler handler,
      boolean opt_capture) {
    if (type == null || handler == null) {
      throw new NullPointerException((type == null ? "type" : "handler") + " was null.");
    }
    return handlerRegs.wrap(model.bridge.store.getBus().subscribeLocal(
        Constants.Topic.STORE + "/" + model.bridge.id + "/" +
        (objectId == null ? "" : (objectId + "/")) + type, new Handler<Message<?>>() {
       @SuppressWarnings(
           "unchecked")
       @Override
       public void handle(Message<?> message) {
         Platform.scheduler().handle(handler,message.body());
       }
     }));
  }

  void scheduleEvent(BaseModelEvent event) {
    assert !event.bubbles();
    if (eventsToFire == null) {
      eventsToFire = Json.createArray();
    }
    eventsToFire.push(event);
    fireEvent((BaseModelEventImpl) event);
    if (!isEventsScheduled) {
      isEventsScheduled = true;
      Platform.scheduler().scheduleDeferred(eventsTask);
    }
  }

  private void fireEvent(BaseModelEventImpl event) {
    model.bridge.store.getBus().publishLocal(
        Constants.Topic.STORE + "/" + model.bridge.id + "/" + event.target + "/" + event.type, event);
  }
}

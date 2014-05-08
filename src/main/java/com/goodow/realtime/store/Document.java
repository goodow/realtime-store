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

import com.goodow.realtime.channel.Bus;
import com.goodow.realtime.channel.Message;
import com.goodow.realtime.core.Handler;
import com.goodow.realtime.core.HandlerRegistration;
import com.goodow.realtime.core.HandlerRegistrations;
import com.goodow.realtime.core.Platform;
import com.goodow.realtime.json.Json;
import com.goodow.realtime.json.JsonArray;
import com.goodow.realtime.json.JsonArray.ListIterator;
import com.goodow.realtime.json.JsonObject;
import com.goodow.realtime.json.JsonObject.MapIterator;
import com.goodow.realtime.store.channel.Constants.Addr;
import com.goodow.realtime.store.util.ModelFactory;

import org.timepedia.exporter.client.Export;
import org.timepedia.exporter.client.ExportPackage;

/**
 * A Realtime document. A document consists of a Realtime model and a set of collaborators. Listen
 * on the document for the following events:
 * <ul>
 * <li>
 * <p>
 * {@link com.goodow.realtime.store.EventType#COLLABORATOR_LEFT}
 * <li>
 * <p>
 * {@link com.goodow.realtime.store.EventType#COLLABORATOR_JOINED}
 * <li>
 * <p>
 * {@link com.goodow.realtime.store.EventType#DOCUMENT_SAVE_STATE_CHANGED}
 * </ul>
 * <p>
 * This class should not be instantiated directly. The document object is generated during the
 * document load process.
 */
@ExportPackage(ModelFactory.PACKAGE_PREFIX_REALTIME)
@Export(all = true)
public class Document implements Disposable {
  private final JsonArray collaborators;
  private final Model model;
  final HandlerRegistrations handlerRegs;

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
      evtsToFire.forEach(new ListIterator<BaseModelEvent>() {
        @Override
        public void call(int index, BaseModelEvent event) {
          assert !event.bubbles;
          String id = event.target.id;
          bubblingToAncestors(id, event, Json.createArray());
          fireEvent(event);
        }
      });
      eventsById.forEach(new MapIterator<JsonArray>() {
        @Override
        public void call(String key, JsonArray events) {
          BaseModelEvent first = events.<BaseModelEvent> get(0);
          ObjectChangedEvent objectChangedEvent =
              new ObjectChangedEvent(model.getObject(key), first.sessionId, first.userId, events);
          fireEvent(objectChangedEvent);
        }
      });
      evtsToFire = null;
      eventsById = null;
    }

    private void bubblingToAncestors(String id, BaseModelEvent event, JsonArray seen) {
      if (seen.indexOf(id) != -1) {
        return;
      }
      if (!eventsById.has(id)) {
        eventsById.set(id, Json.createArray().push(event));
      } else {
        eventsById.getArray(id).push(event);
      }
      seen.push(id);

      String[] parents = model.getParents(id);
      if (parents != null) {
        for (String parent : parents) {
          bubblingToAncestors(parent, event, seen);
        }
      }
    }

    private void fireEvent(BaseModelEvent event) {
      model.bridge.store.getBus().publish(
          Bus.LOCAL + Addr.EVENT + event.type + ":" + model.bridge.docId + ":" + event.target.id,
          event);
    }
  };

  /**
   * @param bridge The driver for the GWT collaborative libraries.
   */
  Document(final DocumentBridge bridge) {
    model = new Model(bridge, this);
    collaborators = Json.createArray();
    handlerRegs = new HandlerRegistrations();
  }

  public HandlerRegistration addCollaboratorJoinedListener(
      final Handler<CollaboratorJoinedEvent> handler) {
    return addEventListener(null, EventType.COLLABORATOR_JOINED, handler, false);
  }

  public HandlerRegistration addCollaboratorLeftListener(
      final Handler<CollaboratorLeftEvent> handler) {
    return addEventListener(null, EventType.COLLABORATOR_LEFT, handler, false);
  }

  public HandlerRegistration addDocumentSaveStateListener(
      final Handler<DocumentSaveStateChangedEvent> handler) {
    return addEventListener(null, EventType.DOCUMENT_SAVE_STATE_CHANGED, handler, false);
  }

  /**
   * Closes the document and disconnects from the server. After this function is called, event
   * listeners will no longer fire and attempts to access the document, model, or model objects will
   * throw a {@link com.goodow.realtime.store.DocumentClosedError}. Calling this function after the
   * document has been closed will have no effect.
   * 
   * @throws DocumentClosedError
   */
  public void close() {
    model.bridge.outputSink.close();
    collaborators.clear();
    handlerRegs.unregisterHandler();
  }

  /**
   * Gets an array of collaborators active in this session. Each collaborator has these properties:
   * sessionId, userId, displayName, color, isMe, isAnonymous, photoUrl.
   * 
   * @return An array of collaborators.
   */
  public JsonArray getCollaborators() {
    return collaborators.copy();
  }

  /**
   * Gets the collaborative model associated with this document.
   * 
   * @return The collaborative model for this document.
   */
  public Model getModel() {
    return model;
  }

  @SuppressWarnings("rawtypes")
  HandlerRegistration addEventListener(String objectId, EventType type, final Handler handler,
      boolean opt_capture) {
    if (type == null || handler == null) {
      throw new NullPointerException((type == null ? "type" : "handler") + " was null.");
    }
    return handlerRegs.wrap(model.bridge.store.getBus().registerHandler(
        Addr.EVENT + type + ":" + model.bridge.docId + (objectId == null ? "" : (":" + objectId)),
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
        model.bridge.store.getBus().publish(
            Bus.LOCAL + Addr.EVENT + EventType.COLLABORATOR_JOINED + ":" + model.bridge.docId,
            new CollaboratorJoinedEvent(this, collaborator));
      }
    } else {
      if (index != -1) {
        collaborators.remove(index);
        model.bridge.store.getBus().publish(
            Bus.LOCAL + Addr.EVENT + EventType.COLLABORATOR_LEFT + ":" + model.bridge.docId,
            new CollaboratorLeftEvent(this, collaborator));
      }
    }
  }

  void scheduleEvent(BaseModelEvent event) {
    assert !event.bubbles;
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

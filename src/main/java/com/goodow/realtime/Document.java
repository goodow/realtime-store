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

import com.goodow.realtime.Error.ErrorHandler;
import com.goodow.realtime.channel.constant.Constants.Params;
import com.goodow.realtime.channel.util.ChannelNative;
import com.goodow.realtime.model.util.JsonSerializer;
import com.goodow.realtime.model.util.ModelFactory;
import com.goodow.realtime.operation.util.JsonUtility;
import com.goodow.realtime.operation.util.Pair;

import org.timepedia.exporter.client.Export;
import org.timepedia.exporter.client.ExportPackage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

/**
 * A Realtime document. A document consists of a Realtime model and a set of collaborators. Listen
 * on the document for the following events:
 * <ul>
 * <li>
 * <p>
 * {@link com.goodow.realtime.EventType#COLLABORATOR_LEFT}
 * <li>
 * <p>
 * {@link com.goodow.realtime.EventType#COLLABORATOR_JOINED}
 * <li>
 * <p>
 * {@link com.goodow.realtime.EventType#DOCUMENT_SAVE_STATE_CHANGED}
 * </ul>
 * <p>
 * This class should not be instantiated directly. The document object is generated during the
 * document load process.
 */
@ExportPackage(ModelFactory.PACKAGE_PREFIX_REALTIME)
@Export(all = true)
public class Document implements EventTarget {
  static final String EVENT_HANDLER_KEY = "document";
  private static final Logger log = Logger.getLogger(Document.class.getName());
  private final List<Collaborator> collaborators = new ArrayList<Collaborator>();
  private Model model;
  private Map<Pair<String, EventType>, List<EventHandler<?>>> handlers;
  private final Map<String, List<String>> parents = new HashMap<String, List<String>>();

  private boolean isEventsScheduled = false;
  private List<Pair<Pair<String, EventType>, Disposable>> events;
  private Map<String, List<BaseModelEvent>> eventsById;
  private final Runnable eventsTask = new Runnable() {
    private List<Pair<Pair<String, EventType>, Disposable>> evts;
    private Map<String, List<BaseModelEvent>> evtsById;

    @Override
    public void run() {
      evts = events;
      evtsById = eventsById;
      events = null;
      eventsById = null;
      isEventsScheduled = false;
      for (int i = 0, len = evts.size(); i < len; i++) {
        Pair<Pair<String, EventType>, Disposable> evt = evts.get(i);
        produceObjectChangedEvent(evt.first.first, evt.second);
      }
      for (Pair<Pair<String, EventType>, Disposable> evt : evts) {
        fireEvent(evt.first, evt.second);
      }
      assert evtsById.isEmpty();
      evts = null;
      evtsById = null;
    }

    private void bubblingToAncestors(String id, ObjectChangedEvent objectChangedEvent,
        Set<String> seen) {
      if (seen.contains(id)) {
        return;
      }
      seen.add(id);
      evts.add(Pair.of(Pair.of(id, objectChangedEvent.type), (Disposable) objectChangedEvent));

      String[] parents = getParents(id);
      if (parents != null) {
        for (String parent : parents) {
          bubblingToAncestors(parent, objectChangedEvent, seen);
        }
      }
    }

    @SuppressWarnings({"unchecked", "cast"})
    private void fireEvent(Pair<String, EventType> key, Disposable event) {
      List<EventHandler<?>> handlers = getEventHandlers(key, false);
      if (handlers == null) {
        return;
      }
      for (int i = 0, len = handlers.size(); i < len; i++) {
        EventHandler<?> handler = handlers.get(i);
        if (handler instanceof EventHandler) {
          ((EventHandler<Disposable>) handler).handleEvent(event);
        } else {
          __ocniFireEvent__(handler, event);
        }
      }
    }

    private void produceObjectChangedEvent(String id, Disposable event) {
      if (!evtsById.containsKey(id)) {
        return;
      }
      BaseModelEvent evt = (BaseModelEvent) event;
      assert !evt.bubbles;
      List<BaseModelEvent> eventsPerId = evtsById.get(id);
      evtsById.remove(id);
      ObjectChangedEvent objectChangedEvent =
          new ObjectChangedEvent(evt.target, evt.sessionId, evt.userId, eventsPerId
              .toArray(new BaseModelEvent[0]));
      Set<String> seen = new HashSet<String>();
      bubblingToAncestors(id, objectChangedEvent, seen);
    }
  };
  private DocumentBridge bridge;

  /**
   * @param bridge The driver for the GWT collaborative libraries.
   * @param commService The communication service to dispose when this document is disposed.
   * @param errorHandlerFn The third-party error handling function.
   */
  Document(DocumentBridge bridge, Disposable commService, ErrorHandler errorHandlerFn) {
    this.bridge = bridge;
    model = new Model(bridge, this);
  }

  public void addCollaboratorJoinedListener(EventHandler<CollaboratorJoinedEvent> handler) {
    addEventListener(EventType.COLLABORATOR_JOINED, handler, false);
  }

  public void addCollaboratorLeftListener(EventHandler<CollaboratorLeftEvent> handler) {
    addEventListener(EventType.COLLABORATOR_LEFT, handler, false);
  }

  public void addDocumentSaveStateListener(EventHandler<DocumentSaveStateChangedEvent> handler) {
    addEventListener(EventType.DOCUMENT_SAVE_STATE_CHANGED, handler, false);
  }

  @Override
  public void addEventListener(EventType type, EventHandler<?> handler, boolean opt_capture) {
    addEventListener(EVENT_HANDLER_KEY, type, handler, opt_capture);
  }

  /**
   * Closes the document and disconnects from the server. After this function is called, event
   * listeners will no longer fire and attempts to access the document, model, or model objects will
   * throw a {@link com.goodow.realtime.DocumentClosedError}. Calling this function after the
   * document has been closed will have no effect.
   * 
   * @throws DocumentClosedError
   */
  public void close() {
    bridge.outputSink.close();
    bridge = null;
    model = null;
  }

  /**
   * Exports the document to a JSON format.
   * 
   * @param successFn A function that the exported JSON will be passed to when it is available.
   * @param failureFn A function that will be called if the export fails.
   */
  public void exportDocument(Disposable successFn, Disposable failureFn) {

  }

  /**
   * Gets an array of collaborators active in this session. Each collaborator is a jsMap with these
   * fields: sessionId, userId, displayName, color, isMe, isAnonymous.
   * 
   * @return An array of collaborators.
   */
  public Collaborator[] getCollaborators() {
    return collaborators.toArray(new Collaborator[0]);
  }

  /**
   * Gets the collaborative model associated with this document.
   * 
   * @return The collaborative model for this document.
   */
  public Model getModel() {
    return model;
  }

  public void removeCollaboratorJoinedListener(EventHandler<CollaboratorJoinedEvent> handler) {
    removeEventListener(EventType.COLLABORATOR_JOINED, handler, false);
  }

  public void removeCollaboratorLeftListener(EventHandler<CollaboratorLeftEvent> handler) {
    removeEventListener(EventType.COLLABORATOR_LEFT, handler, false);
  }

  @Override
  public void removeEventListener(EventType type, EventHandler<?> handler, boolean opt_capture) {
    removeEventListener(EVENT_HANDLER_KEY, type, handler, opt_capture);
  }

  void addEventListener(String id, EventType type, EventHandler<?> handler, boolean opt_capture) {
    if (id == null || type == null || handler == null) {
      throw new NullPointerException((id == null ? "id" : type == null ? "type" : "handler")
          + " was null.");
    }
    List<EventHandler<?>> handlersPerType = getEventHandlers(Pair.of(id, type), true);
    if (handlersPerType.contains(handler)) {
      log.warning("The same handler can only be added once per the type.");
    } else {
      handlersPerType.add(handler);
    }
  }

  void addOrRemoveParent(JsonValue childOrNull, String parentId, boolean isAdd) {
    if (JsonUtility.isNull(childOrNull)) {
      return;
    }
    JsonArray child = (JsonArray) childOrNull;
    if (child.getNumber(0) == JsonSerializer.REFERENCE_TYPE) {
      String childId = child.getString(1);
      List<String> list = parents.get(childId);
      if (isAdd) {
        if (list == null) {
          list = new ArrayList<String>();
          parents.put(childId, list);
        }
        list.add(parentId);
      } else {
        assert list != null && list.contains(parentId);
        list.remove(parentId);
        if (list.isEmpty()) {
          parents.remove(childId);
        }
      }
    }
  }

  void checkStatus() throws DocumentClosedError {
    if (bridge == null) {
      throw new DocumentClosedError();
    }
  }

  void onCollaboratorChanged(boolean isJoined, JsonObject json) {
    Collaborator collaborator =
        new Collaborator(json.getString(Params.USER_ID), json.getString(Params.SESSION_ID), json
            .getString(Params.DISPLAY_NAME), json.getString(Params.COLOR), json
            .getBoolean(Params.IS_ME), json.getBoolean(Params.IS_ANONYMOUS), json
            .getString(Params.PHOTO_URL));
    if (isJoined) {
      collaborators.add(collaborator);
      CollaboratorJoinedEvent event = new CollaboratorJoinedEvent(this, collaborator);
      scheduleEvent(Document.EVENT_HANDLER_KEY, EventType.COLLABORATOR_JOINED, event);
    } else {
      collaborators.remove(collaborator);
      CollaboratorLeftEvent event = new CollaboratorLeftEvent(this, collaborator);
      scheduleEvent(Document.EVENT_HANDLER_KEY, EventType.COLLABORATOR_LEFT, event);
    }
  }

  void removeEventListener(String id, EventType type, EventHandler<?> handler, boolean opt_capture) {
    if (handlers == null || handler == null) {
      return;
    }
    List<EventHandler<?>> handlersPerType = handlers.get(Pair.of(id, type));
    if (handlersPerType == null) {
      return;
    }
    handlersPerType.remove(handler);
    if (handlersPerType.isEmpty()) {
      handlers.remove(handlersPerType);
      if (handlers.isEmpty()) {
        handlers = null;
      }
    }
  }

  void scheduleEvent(String id, EventType type, Disposable event) {
    if (events == null) {
      initializeEvents();
    }
    events.add(Pair.of(Pair.of(id, type), event));
    if (event instanceof BaseModelEvent) {
      BaseModelEvent evt = (BaseModelEvent) event;
      assert !evt.bubbles;
      List<BaseModelEvent> eventsPerId = eventsById.get(id);
      if (eventsPerId == null) {
        eventsPerId = new ArrayList<BaseModelEvent>();
        eventsById.put(id, eventsPerId);
      }
      eventsPerId.add(evt);
    }
    if (!isEventsScheduled) {
      isEventsScheduled = true;
      ChannelNative.get().scheduleDeferred(eventsTask);
    }
  }

  // @formatter:off
  private native void __ocniFireEvent__(Object handler, Object event) /*-[
    GDREventBlock block = (GDREventBlock)handler;
    block(event);
  ]-*/ /*-{
  }-*/;
  // @formatter:on

  private List<EventHandler<?>> getEventHandlers(Pair<String, EventType> key,
      boolean createIfNotExist) {
    if (handlers == null) {
      if (!createIfNotExist) {
        return null;
      }
      handlers = new HashMap<Pair<String, EventType>, List<EventHandler<?>>>();
    }
    List<EventHandler<?>> handlersPerType = handlers.get(key);
    if (handlersPerType == null) {
      if (!createIfNotExist) {
        return null;
      }
      handlersPerType = new ArrayList<EventHandler<?>>();
      handlers.put(key, handlersPerType);
    }
    return handlersPerType;
  }

  private String[] getParents(String objectId) {
    List<String> list = parents.get(objectId);
    if (list == null) {
      return null;
    }
    Set<String> set = new HashSet<String>(list);
    return set.toArray(new String[0]);
  }

  private void initializeEvents() {
    events = new ArrayList<Pair<Pair<String, EventType>, Disposable>>();
    eventsById = new HashMap<String, List<BaseModelEvent>>();
  }
}

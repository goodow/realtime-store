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

import com.goodow.realtime.channel.util.IdGenerator;
import com.goodow.realtime.core.Handler;
import com.goodow.realtime.core.Registration;
import com.goodow.realtime.json.Json;
import com.goodow.realtime.json.JsonArray;
import com.goodow.realtime.json.JsonArray.ListIterator;
import com.goodow.realtime.json.JsonObject;
import com.goodow.realtime.operation.create.CreateComponent;
import com.goodow.realtime.operation.cursor.ReferenceShiftedComponent;
import com.goodow.realtime.operation.list.json.JsonInsertComponent;
import com.goodow.realtime.operation.list.string.StringInsertComponent;
import com.goodow.realtime.operation.map.json.JsonMapComponent;
import com.goodow.realtime.store.CollaborativeList;
import com.goodow.realtime.store.CollaborativeMap;
import com.goodow.realtime.store.CollaborativeObject;
import com.goodow.realtime.store.CollaborativeString;
import com.goodow.realtime.store.EventType;
import com.goodow.realtime.store.IndexReference;
import com.goodow.realtime.store.Model;
import com.goodow.realtime.store.UndoRedoStateChangedEvent;

import java.util.logging.Logger;

class DefaultModel implements Model {
  private static final String ROOT_ID = "root";
  private static final Logger log = Logger.getLogger(Model.class.getName());
  /* The mode of the document. If true, the document is readonly. If false it is editable. */
  private boolean isReadOnly;
  boolean canUndo;
  boolean canRedo;
  final JsonObject objects = Json.createObject(); // LinkedHashMap<String, CollaborativeObject>
  private final JsonObject parents = Json.createObject(); // HashMap<String, List<String>>
  private JsonObject indexReferences; // HashMap<String, List<String>>
  final DefaultDocument document;
  final DocumentBridge bridge;
  /* An estimate of the number of bytes used by data stored in the model. */
  double bytesUsed;
  /**
   * The current server version number for this model. The version number begins at 1 (the initial
   * empty model) and is incremented each time the model is changed on the server (either by the
   * current session or any other collaborator). Because this version number includes only changes
   * that the server knows about, it is only updated while this client is connected to the Realtime
   * API server and it does not include changes that have not yet been saved to the server.
   */
  double serverVersion;

  /**
   * @param bridge Internal utilities for the Realtime API.
   * @param document The document that this model belongs to.
   */
  DefaultModel(DocumentBridge bridge, DefaultDocument document) {
    this.bridge = bridge;
    this.document = document;
  }

  @Override public Registration onUndoRedoStateChanged(final Handler<UndoRedoStateChangedEvent> handler) {
    return document.addEventListener(null, EventType.UNDO_REDO_STATE_CHANGED, handler, false);
  }

  /**
   * Starts a compound operation. If a name is given, that name will be recorded in the mutation for
   * use in revision history, undo menus, etc. When beginCompoundOperation() is called, all
   * subsequent edits to the data model will be batched together in the undo stack and revision
   * history until endCompoundOperation() is called. Compound operations may be nested inside other
   * compound operations. Note that the compound operation MUST start and end in the same
   * synchronous execution block. If this invariant is violated, the data model will become invalid
   * and all future changes will fail.
   * 
   * @param opt_name An optional name for this compound operation.
   */
  public void beginCompoundOperation(String opt_name) {
    log.info("beginCompoundOperation" + (opt_name == null ? "" : (" " + opt_name)));
  }

  @Override public boolean canRedo() {
    return canRedo;
  }

  @Override public boolean canUndo() {
    return canUndo;
  }

  /**
   * Creates and returns a new collaborative object. This can be used to create custom collaborative
   * objects. For built in types, use the specific create* functions.
   *
   * @param ref An object constructor or type name.
   * @param var_args Arguments to the newly-created object's initialize() method.
   * @return A new collaborative object.
   */
  public Object create(String ref, Object... var_args) {
    throw new UnsupportedOperationException();
  }

  @Override public CollaborativeList createList(JsonArray opt_initialValue) {
    String id = generateObjectId();
    beginCreationCompoundOperation();
    bridge.consumeAndSubmit(new CreateComponent(id, CreateComponent.LIST));
    if (opt_initialValue != null && opt_initialValue.length() > 0) {
      JsonArray values = JsonSerializer.serializeObjects(opt_initialValue);
      JsonInsertComponent op = new JsonInsertComponent(id, 0, values);
      bridge.consumeAndSubmit(op);
    }
    endCompoundOperation();
    return getObject(id);
  }

  @Override
  public CollaborativeMap createMap(JsonObject opt_initialValue) {
    final String id = generateObjectId();
    beginCreationCompoundOperation();
    bridge.consumeAndSubmit(new CreateComponent(id, CreateComponent.MAP));
    if (opt_initialValue != null && opt_initialValue.size() != 0) {
      opt_initialValue.forEach(new JsonObject.MapIterator<Object>() {
        @Override
        public void call(String key, Object value) {
          JsonArray serializedValue = JsonSerializer.serializeObject(value);
          if (serializedValue == null) {
            return;
          }
          JsonMapComponent op = new JsonMapComponent(id, key, null, serializedValue);
          bridge.consumeAndSubmit(op);
        }
      });
    }
    endCompoundOperation();
    return getObject(id);
  }

  @Override public CollaborativeString createString(String opt_initialValue) {
    String id = generateObjectId();
    beginCreationCompoundOperation();
    bridge.consumeAndSubmit(new CreateComponent(id, CreateComponent.STRING));
    if (opt_initialValue != null && !opt_initialValue.isEmpty()) {
      StringInsertComponent op = new StringInsertComponent(id, 0, opt_initialValue);
      bridge.consumeAndSubmit(op);
    }
    endCompoundOperation();
    return getObject(id);
  }

  /**
   * Ends a compound operation. This method will throw an exception if no compound operation is in
   * progress.
   */
  public void endCompoundOperation() {
    log.info("endCompoundOperation");
  }

  public <T extends CollaborativeObject> T getObject(String objectId) {
    return objects.<T> get(objectId);
  }

  public JsonArray getParents(String objectId) {
    JsonArray list = parents.getArray(objectId);
    final JsonArray toRtn = Json.createArray();
    if (list != null) {
      list.forEach(new ListIterator<String>() {
        @Override
        public void call(int index, String parent) {
          if (toRtn.indexOf(parent) == -1) {
            toRtn.push(parent);
          }
        }
      });
    }
    return toRtn;
  }

  @Override public CollaborativeMap getRoot() {
    return getObject(ROOT_ID);
  }

  /**
   * @return Whether the model is initialized.
   */
  public boolean isInitialized() {
    return false;
  }

  /* The mode of the document. If true, the document is readonly. If false it is editable. */
  public boolean isReadOnly() {
    return isReadOnly;
  }

  @Override public void redo() {
    bridge.redo();
  }

  @Override public void undo() {
    bridge.undo();
  }

  void addOrRemoveParent(JsonArray childOrNull, String parentId, boolean isAdd) {
    if (childOrNull == null) {
      return;
    }
    if (childOrNull.getNumber(0) == JsonSerializer.REFERENCE_TYPE) {
      String childId = childOrNull.getString(1);
      JsonArray list = parents.getArray(childId);
      if (isAdd) {
        if (list == null) {
          list = Json.createArray();
          parents.set(childId, list);
        }
        list.push(parentId);
      } else {
        assert list != null && list.indexOf(parentId) != -1;
        list.removeValue(parentId);
        if (list.length() == 0) {
          parents.remove(childId);
        }
      }
    }
  }

  IndexReference createIndexReference(String referencedObjectId, int index, boolean canBeDeleted) {
    String id = generateObjectId();
    ReferenceShiftedComponent op =
        new ReferenceShiftedComponent(id, referencedObjectId, index, canBeDeleted, -1);
    beginCreationCompoundOperation();
    bridge.consumeAndSubmit(new CreateComponent(id, CreateComponent.INDEX_REFERENCE));
    bridge.consumeAndSubmit(op);
    registerIndexReference(id, referencedObjectId);
    endCompoundOperation();
    return getObject(id);
  }

  void createRoot() {
    beginCreationCompoundOperation();
    bridge.consumeAndSubmit(new CreateComponent(ROOT_ID, CreateComponent.MAP));
    endCompoundOperation();
  }

  void setIndexReferenceIndex(String referencedObject, final boolean isInsert, final int index,
      final int length, final String sessionId, final String userId) {
    if (indexReferences == null) {
      return;
    }
    JsonArray cursors = indexReferences.getArray(referencedObject);
    if (cursors != null) {
      cursors.forEach(new ListIterator<String>() {
        @Override
        public void call(int idx, String indexReferenceId) {
          DefaultIndexReference indexReference = getObject(indexReferenceId);
          indexReference.setIndex(isInsert, index, length, sessionId, userId);
        }
      });
    }
  }

  /**
   * Starts a compound operation for the creation of the document's initial state.
   */
  private void beginCreationCompoundOperation() {
    beginCompoundOperation("initialize");
  }

  private String generateObjectId() {
    return "gde" + new IdGenerator().next(14);
  }

  private void registerIndexReference(String indexReference, String referencedObject) {
    if (indexReferences == null) {
      indexReferences = Json.createObject();
    }
    JsonArray list = indexReferences.getArray(referencedObject);
    if (list == null) {
      list = Json.createArray();
      indexReferences.set(referencedObject, list);
    }
    list.push(indexReference);
  }
}
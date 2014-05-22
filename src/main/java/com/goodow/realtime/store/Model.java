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
import com.goodow.realtime.store.util.JsonSerializer;
import com.goodow.realtime.store.util.ModelFactory;

import com.google.common.annotations.GwtIncompatible;

import org.timepedia.exporter.client.Export;
import org.timepedia.exporter.client.ExportAfterCreateMethod;
import org.timepedia.exporter.client.ExportPackage;
import org.timepedia.exporter.client.NoExport;

import java.util.Map;
import java.util.logging.Logger;

/**
 * The collaborative model is the data model for a Realtime document. The document's object graph
 * should be added to the model under the root object. All objects that are part of the model must
 * be accessible from this root.
 * <p>
 * The model class is also used to create instances of built in and custom collaborative objects via
 * the appropriate create method.
 * <p>
 * Listen on the model for the following events:
 * <ul>
 * <li>{@link com.goodow.realtime.store.EventType#UNDO_REDO_STATE_CHANGED}
 * </ul>
 * <p>
 * This class should not be instantiated directly. The collaborative model is generated during the
 * document load process. The model can be initialized by passing an initializer function to
 * com.goodow.realtime.store.load.
 */
@ExportPackage(ModelFactory.PACKAGE_PREFIX_REALTIME)
@Export(all = true)
public class Model implements Disposable {
  private static final String ROOT_ID = "root";
  private static final Logger log = Logger.getLogger(Model.class.getName());

  @GwtIncompatible(ModelFactory.JS_REGISTER_PROPERTIES)
  @ExportAfterCreateMethod
  // @formatter:off
  public native static void __jsniRunAfter__() /*-{
    var _ = $wnd.good.realtime.Model.prototype;
//    Object.defineProperties(_, {
//      canRedo : {
//        get : function() {
//          return this.g.@com.goodow.realtime.store.Model::canRedo()();
//        }
//      },
//      canUndo : {
//        get : function() {
//          return this.g.@com.goodow.realtime.store.Model::canUndo()();
//        }
//      },
//      isReadOnly : {
//        get : function() {
//          return this.g.@com.goodow.realtime.store.Model::isReadOnly()();
//        }
//      }
//    });
    _.createMap = function(opt_initialValue) {
      var jsMap;
      if (opt_initialValue !== undefined) {
        jsMap = {};
        for ( var key in opt_initialValue) {
          if (Object.prototype.hasOwnProperty.call(opt_initialValue, key) && key != '$H') {
            jsMap[key] = @org.timepedia.exporter.client.ExporterUtil::gwtInstance(Ljava/lang/Object;)(opt_initialValue[key]);
          }
        }
      }
      var v = this.g.@com.goodow.realtime.store.Model::__jsniCreateMap__()(jsMap);
      return @org.timepedia.exporter.client.ExporterUtil::wrap(Ljava/lang/Object;)(v);
    };
    _.getObject = function(objectId) {
      var v = this.g.@com.goodow.realtime.store.Model::getObject(Ljava/lang/String;)(objectId);
      return @org.timepedia.exporter.client.ExporterUtil::wrap(Ljava/lang/Object;)(v);
    };
  }-*/;
  // @formatter:on

  private boolean isReadOnly;
  boolean canUndo;
  boolean canRedo;
  final JsonObject objects = Json.createObject(); // LinkedHashMap<String, CollaborativeObject>
  private final JsonObject parents = Json.createObject(); // HashMap<String, List<String>>
  private JsonObject indexReferences; // HashMap<String, List<String>>
  final Document document;
  final DocumentBridge bridge;
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
   * @param bridge The driver for the GWT collaborative libraries.
   * @param document The document that this model belongs to.
   */
  Model(DocumentBridge bridge, Document document) {
    this.bridge = bridge;
    this.document = document;
  }

  public Registration addUndoRedoStateChangedListener(
      final Handler<UndoRedoStateChangedEvent> handler) {
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

  /**
   * @return True if the model can currently redo.
   */
  public boolean canRedo() {
    return canRedo;
  }

  /**
   * @return True if the model can currently undo.
   */
  public boolean canUndo() {
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

  /**
   * Creates a collaborative list.
   * 
   * @param opt_initialValue Initial value for the list.
   * @return A collaborative list.
   */
  public CollaborativeList createList(Object... opt_initialValue) {
    String id = generateObjectId();
    beginCreationCompoundOperation();
    bridge.consumeAndSubmit(new CreateComponent(id, CreateComponent.LIST));
    if (opt_initialValue != null && opt_initialValue.length > 0) {
      JsonArray[] values = JsonSerializer.serializeObjects(opt_initialValue);
      JsonInsertComponent op = new JsonInsertComponent(id, 0, values);
      bridge.consumeAndSubmit(op);
    }
    endCompoundOperation();
    return getObject(id);
  }

  /**
   * Creates a collaborative map.
   * 
   * @param opt_initialValue Initial value for the map.
   * @return A collaborative map.
   */
  @NoExport
  public CollaborativeMap createMap(Map<String, ?> opt_initialValue) {
    String id = generateObjectId();
    beginCreationCompoundOperation();
    bridge.consumeAndSubmit(new CreateComponent(id, CreateComponent.MAP));
    if (opt_initialValue != null && !opt_initialValue.isEmpty()) {
      for (Map.Entry<String, ?> entry : opt_initialValue.entrySet()) {
        JsonArray serializedValue = JsonSerializer.serializeObject(entry.getValue());
        if (serializedValue == null) {
          continue;
        }
        JsonMapComponent op = new JsonMapComponent(id, entry.getKey(), null, serializedValue);
        bridge.consumeAndSubmit(op);
      }
    }
    endCompoundOperation();
    return getObject(id);
  }

  /**
   * Creates a collaborative string.
   * 
   * @param opt_initialValue Sets the initial value for this string.
   * @return A collaborative string.
   */
  public CollaborativeString createString(String opt_initialValue) {
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

  public double getBytesUsed() {
    return bytesUsed;
  }

  @NoExport
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

  /**
   * Returns the root of the object model.
   * 
   * @return The root of the object model.
   */
  public CollaborativeMap getRoot() {
    return getObject(ROOT_ID);
  }

  /**
   * Returns whether the model is initialized.
   * 
   * @return Whether the model is initialized.
   */
  public boolean isInitialized() {
    return false;
  }

  /**
   * @return The mode of the document. If true, the document is readonly. If false it is editable.
   */
  public boolean isReadOnly() {
    return isReadOnly;
  }

  /**
   * Redo the last thing the active collaborator undid.
   */
  public void redo() {
    bridge.redo();
  }

  /**
   * Undo the last thing the active collaborator did.
   */
  public void undo() {
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
          IndexReference indexReference = getObject(indexReferenceId);
          indexReference.setIndex(isInsert, index, length, sessionId, userId);
        }
      });
    }
  }

  @GwtIncompatible(ModelFactory.JS_REGISTER_MATHODS)
  private CollaborativeMap __jsniCreateMap__() {
    // if (map == null) {
    return createMap(null);
    // }
    // HashMap<String, Object> opt_initialValue = new HashMap<String, Object>();
    // elemental.util.ArrayOfString keys = map.keys();
    // for (int i = 0, len = keys.length(); i < len; i++) {
    // String key = keys.get(i);
    // opt_initialValue.set(key, map.get(key));
    // }
    // return createMap(opt_initialValue);
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

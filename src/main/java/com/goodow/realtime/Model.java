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

import com.goodow.realtime.model.util.JsonSerializer;
import com.goodow.realtime.model.util.ModelFactory;
import com.goodow.realtime.operation.CreateOperation;
import com.goodow.realtime.operation.Operation;
import com.goodow.realtime.operation.ReferenceShiftedOperation;
import com.goodow.realtime.operation.id.IdGenerator;
import com.goodow.realtime.operation.list.ArrayOp;
import com.goodow.realtime.operation.list.StringOp;
import com.goodow.realtime.operation.list.algorithm.ListOp;
import com.goodow.realtime.operation.map.MapOp;
import com.goodow.realtime.operation.util.JsonUtility;

import com.google.common.annotations.GwtIncompatible;

import org.timepedia.exporter.client.Export;
import org.timepedia.exporter.client.ExportAfterCreateMethod;
import org.timepedia.exporter.client.ExportPackage;
import org.timepedia.exporter.client.NoExport;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import elemental.js.util.JsMapFromStringTo;
import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonValue;
import elemental.util.ArrayOfString;

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
 * <li>{@link com.goodow.realtime.EventType#UNDO_REDO_STATE_CHANGED}
 * </ul>
 * <p>
 * This class should not be instantiated directly. The collaborative model is generated during the
 * document load process. The model can be initialized by passing an initializer function to
 * com.goodow.realtime.load.
 */
@ExportPackage(ModelFactory.PACKAGE_PREFIX_REALTIME)
@Export(all = true)
public class Model implements EventTarget {
  private static final String ROOT_ID = "root";
  private static final String EVENT_HANDLER_KEY = "model";
  private static final Logger log = Logger.getLogger(Model.class.getName());

  @GwtIncompatible(ModelFactory.JS_REGISTER_PROPERTIES)
  @ExportAfterCreateMethod
  // @formatter:off
  public native static void __jsniRunAfter__() /*-{
    var _ = $wnd.good.realtime.Model.prototype;
//    Object.defineProperties(_, {
//      canRedo : {
//        get : function() {
//          return this.g.@com.goodow.realtime.Model::canRedo()();
//        }
//      },
//      canUndo : {
//        get : function() {
//          return this.g.@com.goodow.realtime.Model::canUndo()();
//        }
//      },
//      isReadOnly : {
//        get : function() {
//          return this.g.@com.goodow.realtime.Model::isReadOnly()();
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
      var v = this.g.@com.goodow.realtime.Model::__jsniCreateMap__(Lelemental/js/util/JsMapFromStringTo;)(jsMap);
      return @org.timepedia.exporter.client.ExporterUtil::wrap(Ljava/lang/Object;)(v);
    };
    _.getObject = function(objectId) {
      var v = this.g.@com.goodow.realtime.Model::getObject(Ljava/lang/String;)(objectId);
      return @org.timepedia.exporter.client.ExporterUtil::wrap(Ljava/lang/Object;)(v);
    };
  }-*/;

  // @formatter:on

  private boolean isReadOnly;
  private boolean canRedo;
  private boolean canUndo;

  final Map<String, CollaborativeObject> objects = new LinkedHashMap<String, CollaborativeObject>();
  private final Map<String, List<String>> parents = new HashMap<String, List<String>>();
  private Map<String, List<String>> indexReferences;
  final Document document;
  final DocumentBridge bridge;

  /**
   * @param bridge The driver for the GWT collaborative libraries.
   * @param document The document that this model belongs to.
   */
  Model(DocumentBridge bridge, Document document) {
    this.bridge = bridge;
    this.document = document;
  }

  @Override
  public void addEventListener(EventType type, EventHandler<?> handler, boolean opt_capture) {
    document.addEventListener(EVENT_HANDLER_KEY, type, handler, opt_capture);
  }

  public void addUndoRedoStateChangedListener(EventHandler<UndoRedoStateChangedEvent> handler) {
    addEventListener(EventType.UNDO_REDO_STATE_CHANGED, handler, false);
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
    ArrayOp op = null;
    if (opt_initialValue != null && opt_initialValue.length > 0) {
      op = new ArrayOp();
      JsonArray array = Json.createArray();
      for (int i = 0, len = opt_initialValue.length; i < len; i++) {
        JsonArray serializedValue = JsonSerializer.serializeObject(opt_initialValue[i]);
        array.set(i, serializedValue);
      }
      op.insert(array);
    }
    String id = generateObjectId();
    initializeCreate(CreateOperation.COLLABORATIVE_LIST, op, id);
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
    MapOp op = null;
    if (opt_initialValue != null && !opt_initialValue.isEmpty()) {
      op = new MapOp();
      for (String key : opt_initialValue.keySet()) {
        JsonArray serializedValue = JsonSerializer.serializeObject(opt_initialValue.get(key));
        if (serializedValue == null) {
          continue;
        }
        op.update(key, null, serializedValue);
      }
    }
    String id = generateObjectId();
    initializeCreate(CreateOperation.COLLABORATIVE_MAP, op, id);
    return getObject(id);
  }

  /**
   * Creates a collaborative string.
   * 
   * @param opt_initialValue Sets the initial value for this string.
   * @return A collaborative string.
   */
  public CollaborativeString createString(String opt_initialValue) {
    ListOp<String> op = null;
    if (opt_initialValue != null && !opt_initialValue.isEmpty()) {
      op = new StringOp().insert(opt_initialValue);
    }
    String id = generateObjectId();
    initializeCreate(CreateOperation.COLLABORATIVE_STRING, op, id);
    return getObject(id);
  }

  /**
   * Ends a compound operation. This method will throw an exception if no compound operation is in
   * progress.
   */
  public void endCompoundOperation() {
    log.info("endCompoundOperation");
  }

  @SuppressWarnings("unchecked")
  @NoExport
  public <T extends CollaborativeObject> T getObject(String objectId) {
    return (T) objects.get(objectId);
  }

  public String[] getParents(String objectId) {
    List<String> list = parents.get(objectId);
    if (list == null) {
      return null;
    }
    Set<String> set = new HashSet<String>(list);
    return set.toArray(new String[0]);
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
    throw new UnsupportedOperationException();
  }

  @Override
  public void removeEventListener(EventType type, EventHandler<?> handler, boolean opt_capture) {
    document.removeEventListener(EVENT_HANDLER_KEY, type, handler, opt_capture);
  }

  /**
   * Undo the last thing the active collaborator did.
   */
  public void undo() {
    throw new UnsupportedOperationException();
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

  /**
   * Starts a compound operation for the creation of the document's initial state.
   */
  void beginCreationCompoundOperation() {
    beginCompoundOperation("initialize");
  }

  IndexReference createIndexReference(String referencedObject, int index, boolean canBeDeleted) {
    ReferenceShiftedOperation op =
        new ReferenceShiftedOperation(referencedObject, index, canBeDeleted, -1);
    String id = generateObjectId();
    initializeCreate(CreateOperation.INDEX_REFERENCE, op, id);
    registerIndexReference(id, referencedObject);
    return getObject(id);
  }

  void createRoot() {
    initializeCreate(CreateOperation.COLLABORATIVE_MAP, null, ROOT_ID);
  }

  void setIndexReferenceIndex(String referencedObject, boolean isInsert, int index, int length,
      String sessionId, String userId) {
    if (indexReferences == null) {
      return;
    }
    List<String> list = indexReferences.get(referencedObject);
    if (list != null) {
      for (String indexReferenceId : list) {
        IndexReference indexReference = getObject(indexReferenceId);
        indexReference.setIndex(isInsert, index, length, sessionId, userId);
      }
    }
  }

  @GwtIncompatible(ModelFactory.JS_REGISTER_MATHODS)
  private CollaborativeMap __jsniCreateMap__(JsMapFromStringTo<?> map) {
    if (map == null) {
      return createMap(null);
    }
    HashMap<String, Object> opt_initialValue = new HashMap<String, Object>();
    ArrayOfString keys = map.keys();
    for (int i = 0, len = keys.length(); i < len; i++) {
      String key = keys.get(i);
      opt_initialValue.put(key, map.get(key));
    }
    return createMap(opt_initialValue);
  }

  private String generateObjectId() {
    return "gde" + new IdGenerator().next(14);
  }

  private void initializeCreate(int type, Operation<?> opt_initialValue, String id) {
    beginCreationCompoundOperation();
    CreateOperation op = new CreateOperation(type, id);
    bridge.consumeAndSubmit(op);
    if (opt_initialValue != null) {
      opt_initialValue.setId(id);
      bridge.consumeAndSubmit(opt_initialValue);
    }
    endCompoundOperation();
  }

  private void registerIndexReference(String indexReference, String referencedObject) {
    if (indexReferences == null) {
      indexReferences = new HashMap<String, List<String>>();
    }
    List<String> list = indexReferences.get(referencedObject);
    if (list == null) {
      list = new ArrayList<String>();
      indexReferences.put(referencedObject, list);
    }
    list.add(indexReference);
  }
}

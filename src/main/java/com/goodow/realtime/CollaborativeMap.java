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

import com.goodow.realtime.operation.Operation;
import com.goodow.realtime.operation.RealtimeOperation;
import com.goodow.realtime.operation.map.MapOp;
import com.goodow.realtime.operation.map.MapTarget;
import com.goodow.realtime.util.JsonSerializer;
import com.goodow.realtime.util.NativeInterfaceFactory;

import com.google.common.annotations.GwtIncompatible;

import org.timepedia.exporter.client.Export;
import org.timepedia.exporter.client.ExportAfterCreateMethod;
import org.timepedia.exporter.client.ExportPackage;
import org.timepedia.exporter.client.NoExport;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

/**
 * A collaborative map. A map's key must be a string. The values can contain other Realtime
 * collaborative objects, custom collaborative objects, primitive values or objects that can be
 * serialized to JSON.
 * <p>
 * Changes to the map will automatically be synced with the server and other collaborators. To
 * listen for changes, add EventListeners for the
 * {@link com.goodow.realtime.EventType#VALUE_CHANGED} event type.
 * <p>
 * This class should not be instantiated directly. To create a new map, use
 * {@link com.goodow.realtime.Model#createMap(Object...)}.
 */
@ExportPackage(NativeInterfaceFactory.PACKAGE_PREFIX_REALTIME)
@Export(all = true)
public class CollaborativeMap extends CollaborativeObject {
  @GwtIncompatible(NativeInterfaceFactory.JS_REGISTER_PROPERTIES)
  @ExportAfterCreateMethod
  public native static void __jsRunAfter__() /*-{
    var _ = $wnd.gdr.CollaborativeMap.prototype;
    Object.defineProperties(_, {
      id : {
        get : function() {
          return this.g.@com.goodow.realtime.CollaborativeObject::id;
        }
      },
      size : {
        get : function() {
          return this.g.@com.goodow.realtime.CollaborativeMap::size()();
        }
      }
    });
    _.get = function(key) {
      this.g.@com.goodow.realtime.CollaborativeMap::checkKey(Ljava/lang/String;)(key)
      var p = this.g.@com.goodow.realtime.CollaborativeMap::snapshot[key];
      if (p === undefined) {
        return undefined;
      } else if (p[0] != @com.goodow.realtime.util.JsonSerializer::REFERENCE_TYPE) {
        return p[1];
      } else {
        var v = this.g.@com.goodow.realtime.CollaborativeMap::get(Ljava/lang/String;)(key);
        return @org.timepedia.exporter.client.ExporterUtil::wrap(Ljava/lang/Object;)(v);
      }
    };
    _.remove = function(key) {
      var old = this.get(key);
      this.g.@com.goodow.realtime.CollaborativeMap::remove(Ljava/lang/String;)(key);
      return old;
    };
    _.values = function() {
      var keys = this.keys();
      var values = [];
      for ( var i in keys) {
        values[i] = this.get(keys[i]);
      }
      return values;
    };
    _.items = function() {
      var items = [];
      var keys = this.keys();
      for ( var i in keys) {
        items[i] = [ keys[i], this.get(keys[i]) ];
      }
      return items;
    };
    _.set = function(key, value) {
      var old = this.get(key);
      var v = @org.timepedia.exporter.client.ExporterUtil::gwtInstance(Ljava/lang/Object;)(value);
      this.g.@com.goodow.realtime.CollaborativeMap::set(Ljava/lang/String;Ljava/lang/Object;)(key,v);
      return old;
    };
  }-*/;

  private JsonObject snapshot;

  CollaborativeMap(Model model) {
    super(model);
  }

  public void addValueChangedListener(EventHandler<ValueChangedEvent> handler) {
    addEventListener(EventType.VALUE_CHANGED, handler, false);
  }

  /**
   * Removes all entries.
   */
  public void clear() {
    model.beginCompoundOperation("map.clear");
    for (String key : keys()) {
      remove(key);
    }
    model.endCompoundOperation();
  }

  /**
   * Returns the value mapped to the given key.
   * 
   * @param key The key to look up.
   * @return The value mapped to the given key.
   * @exception java.lang.IllegalArgumentException
   */
  @SuppressWarnings("unchecked")
  @NoExport
  public <T> T get(String key) {
    checkKey(key);
    return (T) JsonSerializer.jsonToObj(snapshot.getArray(key), model.objects);
  }

  /**
   * Checks if this map contains an entry for the given key.
   * 
   * @param key The key to check.
   * @return Returns true if this map contains a mapping for the given key.
   * @exception java.lang.IllegalArgumentException
   */
  public boolean has(String key) {
    checkKey(key);
    return snapshot.hasKey(key);
  }

  /**
   * Returns whether this map is empty.
   * 
   * @return Returns true if this map is empty.
   */
  public boolean isEmpty() {
    return size() == 0;
  }

  /**
   * Returns an array containing a copy of the items in this map. Modifications to the returned
   * array do not modify this collaborative map.
   * 
   * @return The items in this map. Each item is a [key, value] pair.
   */
  @NoExport
  public Object[][] items() {
    Object[][] items = new Object[size()][2];
    String[] keys = keys();
    for (int i = 0, len = size(); i < len; i++) {
      Object[] item = new Object[2];
      item[0] = keys[i];
      item[1] = get(keys[i]);
      items[i] = item;
    }
    return items;
  }

  /**
   * Returns an array containing a copy of the keys in this map. Modifications to the returned array
   * do not modify this collaborative map.
   * 
   * @return The keys in this map.
   */
  public String[] keys() {
    return snapshot.keys();
  }

  /**
   * Removes the entry for the given key (if such an entry exists).
   * 
   * @param key The key to unmap.
   * @return The value that was mapped to this key, or null if there was no existing value.
   * @exception java.lang.IllegalArgumentException
   */
  @NoExport
  public Object remove(String key) {
    checkKey(key);
    Object oldValue = get(key);
    if (oldValue == null) {
      return null;
    }
    MapOp op = new MapOp();
    op.update(key, snapshot.getArray(key), null);
    consumeAndSubmit(op);
    return oldValue;
  }

  public void removeValueChangedListener(EventHandler<ValueChangedEvent> handler) {
    removeEventListener(EventType.VALUE_CHANGED, handler, false);
  }

  /**
   * Put the value into the map with the given key, overwriting an existing value for that key.
   * 
   * @param key The map key.
   * @param value The map value.
   * @return The old map value, if any, that used to be mapped to the given key.
   * @exception java.lang.IllegalArgumentException
   */
  @NoExport
  public Object set(String key, Object value) {
    checkKey(key);
    MapOp op = new MapOp();
    JsonArray newValue = JsonSerializer.objToJson(value);
    if (newValue == null && !has(key)) {
      return null;
    }
    Object oldObject = get(key);
    op.update(key, snapshot.getArray(key), newValue);
    consumeAndSubmit(op);
    return oldObject;
  }

  /**
   * @return The number of keys in the map.
   */
  public int size() {
    return keys().length;
  }

  /**
   * Returns an array containing a copy of the values in this map. Modifications to the returned
   * array do not modify this collaborative map.
   * 
   * @return The values in this map.
   */
  @NoExport
  public <T> List<T> values() {
    List<T> values = new ArrayList<T>();
    String[] keys = keys();
    for (int i = 0, len = size(); i < len; i++) {
      values.add(this.<T> get(keys[i]));
    }
    return values;
  }

  @Override
  protected void consume(final RealtimeOperation operation) {
    operation.<MapTarget> getOp().apply(new MapTarget() {
      @Override
      public MapTarget update(String key, JsonValue oldValue, JsonValue newValue) {
        assert oldValue == null || JsonSerializer.jsonEqual(snapshot.get(key), oldValue);
        if (newValue == null) {
          removeAndFireEvent(key, operation.getSessionId(), operation.getUserId());
        } else {
          putAndFireEvent(key, newValue, operation.getSessionId(), operation.getUserId());
        }
        return null;
      }
    });
  }

  void initialize(String id, JsonObject snapshot) {
    this.id = id;
    this.snapshot = snapshot;
    model.objects.put(id, this);
    for (String key : keys()) {
      model.document.addOrRemoveParent(snapshot.getArray(key), id, true);
    }
  }

  void initializeCreate(String id, Map<String, ?> opt_initialValue) {
    JsonObject snapshot = Json.createObject();
    if (opt_initialValue != null) {
      for (Map.Entry<String, ?> entry : opt_initialValue.entrySet()) {
        JsonArray array = JsonSerializer.objToJson(entry.getValue());
        if (array == null) {
          continue;
        }
        snapshot.put(entry.getKey(), array);
      }
    }
    initialize(id, snapshot);
  }

  @Override
  Operation<?> toInitialization() {
    if (isEmpty()) {
      return null;
    }
    MapOp op = new MapOp();
    for (String key : keys()) {
      op.update(key, null, snapshot.get(key));
    }
    return op;
  }

  @Override
  void toString(Set<String> seen, StringBuilder sb) {
    if (seen.contains(id)) {
      sb.append("<Map: ").append(id).append(">");
      return;
    }
    seen.add(id);
    sb.append("{");
    boolean isFirst = true;
    for (String key : keys()) {
      if (!isFirst) {
        sb.append(", ");
      }
      isFirst = false;
      sb.append(key).append(": ");
      Object value = get(key);
      if (value instanceof CollaborativeObject) {
        CollaborativeObject obj = (CollaborativeObject) value;
        obj.toString(seen, sb);
      } else {
        sb.append("[JsonValue " + snapshot.getArray(key).get(1).toJson() + "]");
      }
    }
    sb.append("}");
  }

  private void checkKey(String key) {
    if (key == null) {
      throw new IllegalArgumentException("Expected string for key, but was: null");
    }
  }

  private void putAndFireEvent(String key, JsonValue newValue, String sessionId, String userId) {
    assert null != newValue && Json.createNull() != newValue;
    JsonArray oldValue = snapshot.getArray(key);
    Object newObject = JsonSerializer.jsonToObj(newValue, model.objects);
    ValueChangedEvent event =
        new ValueChangedEvent(this, sessionId, userId, key, newObject, get(key));
    snapshot.put(key, newValue);
    model.document.addOrRemoveParent(oldValue, id, false);
    model.document.addOrRemoveParent(newValue, id, true);
    fireEvent(event);
  }

  private void removeAndFireEvent(String key, String sessionId, String userId) {
    assert has(key);
    JsonArray oldValue = snapshot.getArray(key);
    ValueChangedEvent event = new ValueChangedEvent(this, sessionId, userId, key, null, get(key));
    snapshot.remove(key);
    model.document.addOrRemoveParent(oldValue, id, false);
    fireEvent(event);
  }
}

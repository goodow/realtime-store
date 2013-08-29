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
import com.goodow.realtime.operation.Operation;
import com.goodow.realtime.operation.create.CreateOperation;
import com.goodow.realtime.operation.map.MapTarget;
import com.goodow.realtime.operation.map.json.JsonMapOperation;

import com.google.common.annotations.GwtIncompatible;

import org.timepedia.exporter.client.Export;
import org.timepedia.exporter.client.ExportAfterCreateMethod;
import org.timepedia.exporter.client.ExportPackage;
import org.timepedia.exporter.client.NoExport;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.JsonType;
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
@ExportPackage(ModelFactory.PACKAGE_PREFIX_REALTIME)
@Export(all = true)
public class CollaborativeMap extends CollaborativeObject {
  @GwtIncompatible(ModelFactory.JS_REGISTER_PROPERTIES)
  @ExportAfterCreateMethod
  // @formatter:off
  public native static void __jsniRunAfter__() /*-{
    var _ = $wnd.good.realtime.CollaborativeMap.prototype;
//    Object.defineProperties(_, {
//      id : {
//        get : function() {
//          return this.g.@com.goodow.realtime.CollaborativeObject::id;
//        }
//      },
//      size : {
//        get : function() {
//          return this.g.@com.goodow.realtime.CollaborativeMap::size()();
//        }
//      }
//    });
    _.get = function(key) {
      this.g.@com.goodow.realtime.CollaborativeMap::checkKey(Ljava/lang/String;)(key)
      var p = this.g.@com.goodow.realtime.CollaborativeMap::snapshot[key];
      if (p === undefined) {
        return undefined;
      } else if (p[0] != @com.goodow.realtime.model.util.JsonSerializer::REFERENCE_TYPE) {
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
  // @formatter:on

  private final JsonObject snapshot;

  CollaborativeMap(Model model) {
    super(model);
    snapshot = Json.createObject();
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
    return (T) JsonSerializer.deserializeObject(snapshot.getArray(key), model.objects);
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
  public <T> T remove(String key) {
    checkKey(key);
    T oldValue = this.<T> get(key);
    if (oldValue == null) {
      return null;
    }
    JsonMapOperation op = new JsonMapOperation(id, key, snapshot.getArray(key), null);
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
  public <T> T set(String key, Object value) {
    checkKey(key);
    JsonArray serializedValue = JsonSerializer.serializeObject(value);
    T oldObject = this.<T> get(key);
    JsonArray oldValue = snapshot.getArray(key);
    if (!JsonMapOperation.jsonEquals(oldValue, serializedValue)) {
      JsonMapOperation op = new JsonMapOperation(id, key, oldValue, serializedValue);
      consumeAndSubmit(op);
    }
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

  @SuppressWarnings("unchecked")
  @Override
  protected void consume(final String userId, final String sessionId, final Operation<?> operation) {
    ((Operation<MapTarget<JsonValue>>) operation).apply(new MapTarget<JsonValue>() {
      @Override
      public void set(String key, JsonValue newValue) {
        if (newValue == null) {
          removeAndFireEvent(key, sessionId, userId);
          model.bytesUsed -= operation.toString().length();
          model.bytesUsed -= 2;
        } else {
          putAndFireEvent(key, newValue, sessionId, userId);
        }
      }
    });
  }

  @Override
  Operation<?>[] toInitialization() {
    Operation<?>[] toRtn = new Operation[1 + size()];
    toRtn[0] = new CreateOperation(id, CreateOperation.MAP);
    if (!isEmpty()) {
      int i = 1;
      for (String key : keys()) {
        toRtn[i++] = new JsonMapOperation(id, key, null, snapshot.get(key));
      }
    }
    return toRtn;
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
      } else {
        isFirst = false;
      }
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
    assert null != newValue && JsonType.NULL != newValue.getType();
    Object newObject = JsonSerializer.deserializeObject(newValue, model.objects);
    ValueChangedEvent event =
        new ValueChangedEvent(this, sessionId, userId, key, newObject, get(key));
    if (snapshot.hasKey(key)) {
      JsonArray oldValue = snapshot.getArray(key);
      model.addOrRemoveParent(oldValue, id, false);
      model.bytesUsed -= oldValue.toJson().length();
    }
    snapshot.put(key, newValue);
    model.addOrRemoveParent(newValue, id, true);
    fireEvent(event);
    model.bytesUsed += newValue.toJson().length();
  }

  private void removeAndFireEvent(String key, String sessionId, String userId) {
    assert has(key);
    JsonArray oldValue = snapshot.getArray(key);
    ValueChangedEvent event = new ValueChangedEvent(this, sessionId, userId, key, null, get(key));
    snapshot.remove(key);
    model.addOrRemoveParent(oldValue, id, false);
    fireEvent(event);
    model.bytesUsed -= oldValue.toJson().length();
  }
}

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

import com.goodow.realtime.core.Handler;
import com.goodow.realtime.core.Registration;
import com.goodow.realtime.json.Json;
import com.goodow.realtime.json.JsonArray;
import com.goodow.realtime.json.JsonObject;
import com.goodow.realtime.json.JsonObject.MapIterator;
import com.goodow.realtime.operation.Operation;
import com.goodow.realtime.operation.OperationComponent;
import com.goodow.realtime.operation.create.CreateComponent;
import com.goodow.realtime.operation.map.MapTarget;
import com.goodow.realtime.operation.map.json.JsonMapComponent;
import com.goodow.realtime.store.util.JsonSerializer;
import com.goodow.realtime.store.util.ModelFactory;

import com.google.common.annotations.GwtIncompatible;

import org.timepedia.exporter.client.Export;
import org.timepedia.exporter.client.ExportAfterCreateMethod;
import org.timepedia.exporter.client.ExportPackage;
import org.timepedia.exporter.client.NoExport;

/**
 * A collaborative map. A map's key must be a string. The values can contain other Realtime
 * collaborative objects, custom collaborative objects, primitive values or objects that can be
 * serialized to JSON.
 * <p>
 * Changes to the map will automatically be synced with the server and other collaborators. To
 * listen for changes, add EventListeners for the
 * {@link com.goodow.realtime.store.EventType#VALUE_CHANGED} event type.
 * <p>
 * This class should not be instantiated directly. To create a new map, use
 * {@link com.goodow.realtime.store.Model#createMap(Object...)}.
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
//          return this.g.@com.goodow.realtime.store.CollaborativeObject::id;
//        }
//      },
//      size : {
//        get : function() {
//          return this.g.@com.goodow.realtime.store.CollaborativeMap::size()();
//        }
//      }
//    });
    _.get = function(key) {
      this.g.@com.goodow.realtime.store.CollaborativeMap::checkKey(Ljava/lang/String;)(key)
      var p = this.g.@com.goodow.realtime.store.CollaborativeMap::snapshot[key];
      if (p === undefined) {
        return undefined;
      } else if (p[0] != @com.goodow.realtime.store.util.JsonSerializer::REFERENCE_TYPE) {
        return p[1];
      } else {
        var v = this.g.@com.goodow.realtime.store.CollaborativeMap::get(Ljava/lang/String;)(key);
        return @org.timepedia.exporter.client.ExporterUtil::wrap(Ljava/lang/Object;)(v);
      }
    };
    _.remove = function(key) {
      var old = this.get(key);
      this.g.@com.goodow.realtime.store.CollaborativeMap::remove(Ljava/lang/String;)(key);
      return old;
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
      this.g.@com.goodow.realtime.store.CollaborativeMap::set(Ljava/lang/String;Ljava/lang/Object;)(key,v);
      return old;
    };
  }-*/;
  // @formatter:on

  private final JsonObject snapshot;

  CollaborativeMap(Model model) {
    super(model);
    snapshot = Json.createObject();
  }

  public Registration addValueChangedListener(Handler<ValueChangedEvent> handler) {
    return addEventListener(EventType.VALUE_CHANGED, handler, false);
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
    return snapshot.has(key);
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
    final Object[][] items = new Object[size()][2];
    snapshot.forEach(new MapIterator<JsonArray>() {
      int i = 0;

      @Override
      public void call(String key, JsonArray value) {
        Object[] item = new Object[2];
        item[0] = key;
        item[1] = get(key);
        items[i++] = item;
      }
    });
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
    JsonMapComponent op = new JsonMapComponent(id, key, snapshot.getArray(key), null);
    consumeAndSubmit(op);
    return oldValue;
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
    if (!JsonMapComponent.jsonEquals(oldValue, serializedValue)) {
      JsonMapComponent op = new JsonMapComponent(id, key, oldValue, serializedValue);
      consumeAndSubmit(op);
    }
    return oldObject;
  }

  /**
   * @return The number of keys in the map.
   */
  public int size() {
    return snapshot.size();
  }

  @Override
  public JsonObject toJson() {
    final JsonObject json = Json.createObject();
    snapshot.forEach(new MapIterator<JsonArray>() {
      @Override
      public void call(String key, JsonArray value) {
        Object val = get(key);
        if (val instanceof CollaborativeObject) {
          json.set(key, ((CollaborativeObject) val).toJson());
        } else {
          json.set(key, snapshot.getArray(key).get(1));
        }
      }
    });
    return json;
  }

  /**
   * Returns an array containing a copy of the values in this map. Modifications to the returned
   * array do not modify this collaborative map.
   * 
   * @return The values in this map.
   */
  public JsonArray values() {
    final JsonArray values = Json.createArray();
    snapshot.forEach(new MapIterator<JsonArray>() {
      @Override
      public void call(String key, JsonArray value) {
        values.push(get(key));
      }
    });
    return values;
  }

  @SuppressWarnings("unchecked")
  @Override
  protected void consume(final String userId, final String sessionId,
      final OperationComponent<?> component) {
    ((Operation<MapTarget<JsonArray>>) component).apply(new MapTarget<JsonArray>() {
      @Override
      public void set(String key, JsonArray newValue) {
        if (newValue == null) {
          removeAndFireEvent(key, sessionId, userId);
          model.bytesUsed -= component.toString().length();
          model.bytesUsed -= 2;
        } else {
          putAndFireEvent(key, newValue, sessionId, userId);
        }
      }
    });
  }

  @Override
  OperationComponent<?>[] toInitialization() {
    final OperationComponent<?>[] toRtn = new OperationComponent[1 + size()];
    toRtn[0] = new CreateComponent(id, CreateComponent.MAP);
    if (!isEmpty()) {
      snapshot.forEach(new MapIterator<JsonArray>() {
        int i = 1;

        @Override
        public void call(String key, JsonArray value) {
          toRtn[i++] = new JsonMapComponent(id, key, null, snapshot.getArray(key));
        }
      });
    }
    return toRtn;
  }

  private void checkKey(String key) {
    if (key == null) {
      throw new IllegalArgumentException("Expected string for key, but was: null");
    }
  }

  private void putAndFireEvent(String key, JsonArray newValue, String sessionId, String userId) {
    assert null != newValue;
    Object newObject = JsonSerializer.deserializeObject(newValue, model.objects);
    ValueChangedEvent event =
        new ValueChangedEvent(event(sessionId, userId).set("property", key).set("oldValue",
            get(key)).set("newValue", newObject));
    if (snapshot.has(key)) {
      JsonArray oldValue = snapshot.getArray(key);
      model.addOrRemoveParent(oldValue, id, false);
      model.bytesUsed -= oldValue.toJsonString().length();
    }
    snapshot.set(key, newValue);
    model.addOrRemoveParent(newValue, id, true);
    fireEvent(event);
    model.bytesUsed += newValue.toJsonString().length();
  }

  private void removeAndFireEvent(String key, String sessionId, String userId) {
    assert has(key);
    JsonArray oldValue = snapshot.getArray(key);
    ValueChangedEvent event =
        new ValueChangedEvent(event(sessionId, userId).set("property", key).set("oldValue",
            get(key)).set("newValue", null));
    snapshot.remove(key);
    model.addOrRemoveParent(oldValue, id, false);
    fireEvent(event);
    model.bytesUsed -= oldValue.toJsonString().length();
  }
}

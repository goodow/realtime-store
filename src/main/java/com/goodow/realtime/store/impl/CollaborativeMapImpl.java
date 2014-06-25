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
import com.goodow.realtime.store.CollaborativeMap;
import com.goodow.realtime.store.CollaborativeObject;
import com.goodow.realtime.store.EventType;
import com.goodow.realtime.store.ValueChangedEvent;

class CollaborativeMapImpl extends CollaborativeObjectImpl implements CollaborativeMap {
  private final JsonObject snapshot;

  CollaborativeMapImpl(ModelImpl model) {
    super(model);
    snapshot = Json.createObject();
  }

  @Override public Registration onValueChanged(Handler<ValueChangedEvent> handler) {
    return addEventListener(EventType.VALUE_CHANGED, handler, false);
  }

  @Override public void clear() {
    model.beginCompoundOperation("map.clear");
    keys().forEach(new JsonArray.ListIterator<String>() {
      @Override
      public void call(int index, String key) {
        remove(key);
      }
    });
    model.endCompoundOperation();
  }

  @Override@SuppressWarnings("unchecked")
  public <T> T get(String key) {
    checkKey(key);
    return (T) JsonSerializer.deserializeObject(snapshot.getArray(key), model.objects);
  }

  @Override public boolean has(String key) {
    checkKey(key);
    return snapshot.has(key);
  }

  @Override public boolean isEmpty() {
    return size() == 0;
  }

  @Override
  public JsonArray items() {
    final JsonArray items = Json.createArray();
    snapshot.forEach(new MapIterator<JsonArray>() {
      @Override
      public void call(String key, JsonArray value) {
        JsonArray item = Json.createArray().push(key).push(get(key));
        items.push(item);
      }
    });
    return items;
  }

  @Override public JsonArray keys() {
    return snapshot.keys();
  }

  @Override
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

  @Override
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

  @Override public int size() {
    return snapshot.size();
  }

  @Override
  public JsonObject toJson() {
    final JsonObject json = Json.createObject();
    snapshot.forEach(new MapIterator<JsonArray>() {
      @Override
      public void call(String key, JsonArray value) {
        Object val = get(key);
        if (val instanceof CollaborativeObjectImpl) {
          json.set(key, ((CollaborativeObject) val).toJson());
        } else {
          json.set(key, snapshot.getArray(key).get(1));
        }
      }
    });
    return json;
  }

  @Override public JsonArray values() {
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
        new ValueChangedEventImpl(event(sessionId, userId).set("property", key).set("oldValue",
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
        new ValueChangedEventImpl(event(sessionId, userId).set("property", key).set("oldValue",
            get(key)).set("newValue", null));
    snapshot.remove(key);
    model.addOrRemoveParent(oldValue, id, false);
    fireEvent(event);
    model.bytesUsed -= oldValue.toJsonString().length();
  }
}

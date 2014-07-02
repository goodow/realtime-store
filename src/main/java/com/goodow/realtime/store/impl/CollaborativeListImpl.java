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
import com.goodow.realtime.json.JsonArray.ListIterator;
import com.goodow.realtime.operation.OperationComponent;
import com.goodow.realtime.operation.create.CreateComponent;
import com.goodow.realtime.operation.list.AbstractListComponent;
import com.goodow.realtime.operation.list.ListTarget;
import com.goodow.realtime.operation.list.json.JsonDeleteComponent;
import com.goodow.realtime.operation.list.json.JsonInsertComponent;
import com.goodow.realtime.operation.list.json.JsonReplaceComponent;
import com.goodow.realtime.operation.map.json.JsonMapComponent;
import com.goodow.realtime.store.CollaborativeList;
import com.goodow.realtime.store.CollaborativeObject;
import com.goodow.realtime.store.EventType;
import com.goodow.realtime.store.IndexReference;
import com.goodow.realtime.store.ValuesAddedEvent;
import com.goodow.realtime.store.ValuesRemovedEvent;
import com.goodow.realtime.store.ValuesSetEvent;

import java.util.Comparator;

class CollaborativeListImpl extends CollaborativeObjectImpl implements CollaborativeList {
  private final JsonArray snapshot;

  /**
   * @param model The document model.
   */
  CollaborativeListImpl(ModelImpl model) {
    super(model);
    snapshot = Json.createArray();
  }

  @Override public Registration onValuesAdded(Handler<ValuesAddedEvent> handler) {
    return addEventListener(EventType.VALUES_ADDED, handler, false);
  }

  @Override public Registration onValuesRemoved(Handler<ValuesRemovedEvent> handler) {
    return addEventListener(EventType.VALUES_REMOVED, handler, false);
  }

  @Override public Registration onValuesSet(Handler<ValuesSetEvent> handler) {
    return addEventListener(EventType.VALUES_SET, handler, false);
  }

  @Override public JsonArray asArray() {
    final JsonArray objects = Json.createArray();
    snapshot.forEach(new ListIterator<JsonArray>() {
      @Override
      public void call(int index, JsonArray value) {
        objects.push(get(index));
      }
    });
    return objects;
  }

  @Override public void clear() {
    int length = length();
    if (length == 0) {
      return;
    }
    removeRange(0, length);
  }

  @Override@SuppressWarnings("unchecked")
  public <T> T get(int index) {
    checkIndex(index, false);
    return (T) JsonSerializer.deserializeObject(snapshot.getArray(index), model.objects);
  }

  @Override
  public int indexOf(Object value, Comparator<Object> opt_comparator) {
    if (opt_comparator == null) {
      JsonArray serializedValue;
      try {
        serializedValue = JsonSerializer.serializeObject(value);
      } catch (ClassCastException e) {
        return -1;
      }
      for (int i = 0, len = length(); i < len; i++) {
        if (JsonMapComponent.jsonEquals(serializedValue, snapshot.getArray(i))) {
          return i;
        }
      }
    } else {
      for (int i = 0, len = length(); i < len; i++) {
        if (compare(opt_comparator, value, get(i)) == 0) {
          return i;
        }
      }
    }
    return -1;
  }

  @Override
  public void insert(int index, Object value) {
    insertAll(index, Json.createArray().push(value));
  }

  @Override public void insertAll(int index, JsonArray values) {
    assert values != null;
    checkIndex(index, true);
    if (values.length() == 0) {
      return;
    } else {
      JsonArray array = JsonSerializer.serializeObjects(values);
      JsonInsertComponent op = new JsonInsertComponent(id, index, array);
      consumeAndSubmit(op);
    }
  }

  @Override
  public int lastIndexOf(Object value, Comparator<Object> opt_comparator) {
    if (opt_comparator == null) {
      JsonArray serializedValue;
      try {
        serializedValue = JsonSerializer.serializeObject(value);
      } catch (ClassCastException e) {
        return -1;
      }
      for (int i = length() - 1; i >= 0; i--) {
        if (JsonMapComponent.jsonEquals(serializedValue, snapshot.getArray(i))) {
          return i;
        }
      }
    } else {
      for (int i = length() - 1; i >= 0; i--) {
        if (compare(opt_comparator, value, get(i)) == 0) {
          return i;
        }
      }
    }
    return -1;
  }

  @Override public int length() {
    return snapshot.length();
  }

  @Override
  public int push(Object value) {
    insert(length(), value);
    return length();
  }

  @Override public void pushAll(JsonArray values) {
    insertAll(length(), values);
  }

  @Override public IndexReference registerReference(int index, boolean canBeDeleted) {
    checkIndex(index, true);
    return model.createIndexReference(id, index, canBeDeleted);
  }

  @Override public void remove(int index) {
    removeRange(index, index + 1);
  }

  @Override public void removeRange(int startIndex, int endIndex) {
    if (startIndex < 0 || startIndex >= endIndex || endIndex > length()) {
      throw new ArrayIndexOutOfBoundsException("StartIndex: " + startIndex + ", EndIndex: "
          + endIndex + ", Size: " + length());
    }
    JsonArray values = subValues(startIndex, endIndex - startIndex);
    JsonDeleteComponent op = new JsonDeleteComponent(id, startIndex, values);
    consumeAndSubmit(op);
  }

  @Override
  public boolean removeValue(Object value) {
    int index = indexOf(value, null);
    if (index == -1) {
      return false;
    }
    remove(index);
    return true;
  }

  @Override public void replaceRange(int index, JsonArray values) {
    assert values != null;
    if (values.length() == 0) {
      throw new UnsupportedOperationException(
          "At least one value must be specified for a set mutation.");
    }
    checkIndex(index + values.length(), true);
    JsonArray oldValues = subValues(index, values.length());
    JsonArray newValues = JsonSerializer.serializeObjects(values);
    if (oldValues.equals(newValues)) {
      return;
    }
    JsonReplaceComponent op = new JsonReplaceComponent(id, index, oldValues, newValues);
    consumeAndSubmit(op);
  }

  @Override
  public void set(int index, Object value) {
    replaceRange(index, Json.createArray().push(value));
  }

  @Override
  public void setLength(int length) {
    checkIndex(length, true);
    int total = length();
    if (length == total) {
      return;
    }
    removeRange(length, total);
  }

  @Override
  public JsonArray toJson() {
    final JsonArray json = Json.createArray();
    snapshot.forEach(new ListIterator<JsonArray>() {
      @Override
      public void call(int index, JsonArray value) {
        Object val = get(index);
        if (val == null) {
          json.push(null);
        } else if (val instanceof CollaborativeObjectImpl) {
          json.push(((CollaborativeObject) val).toJson());
        } else {
          json.push(snapshot.getArray(index).get(1));
        }
      }
    });
    return json;
  }

  @SuppressWarnings("unchecked")
  @Override
  protected void consume(final String userId, final String sessionId,
      OperationComponent<?> component) {
    final AbstractListComponent<JsonArray> op = (AbstractListComponent<JsonArray>) component;
    op.apply(new ListTarget<JsonArray>() {
      @Override
      public void delete(int startIndex, int length) {
        removeAndFireEvent(startIndex, length, sessionId, userId);
        model.transformCursor(op, userId, sessionId);
      }

      @Override
      public void insert(int startIndex, JsonArray values) {
        insertAndFireEvent(startIndex, values, sessionId, userId);
        model.transformCursor(op, userId, sessionId);
      }

      @Override
      public void replace(int startIndex, JsonArray values) {
        replaceAndFireEvent(startIndex, values, sessionId, userId);
      }
    });
  }

  @Override
  OperationComponent<?>[] toInitialization() {
    int length = length();
    OperationComponent<?>[] toRtn = new OperationComponent[1 + (length == 0 ? 0 : 1)];
    toRtn[0] = new CreateComponent(id, CreateComponent.LIST);
    if (length != 0) {
      toRtn[1] = new JsonInsertComponent(id, 0, subValues(0, length));
    }
    return toRtn;
  }

  // @formatter:off
  private native int __ocniCompare__(Object comparator, Object object1, Object object2) /*-[
    NSComparator block = (NSComparator)comparator;
    return block(object1, object2);
  ]-*/ /*-{
  }-*/;
  // @formatter:on

  private void checkIndex(int index, boolean endBoundIsValid) {
    int length = length();
    if (index < 0 || (endBoundIsValid ? index > length : index >= length)) {
      throw new ArrayIndexOutOfBoundsException("Index: " + index + ", Size: " + length);
    }
  }

  @SuppressWarnings("cast")
  private int compare(Comparator<Object> comparator, Object object1, Object object2) {
    if (comparator instanceof Comparator) {
      return comparator.compare(object1, object2);
    } else {
      return __ocniCompare__(comparator, object1, object2);
    }
  }

  private void insertAndFireEvent(final int index, JsonArray values, String sessionId,
                                  String userId) {
    assert index <= length();
    final JsonArray objects = Json.createArray();
    values.forEach(new ListIterator<JsonArray>() {
      @Override
      public void call(int idx, JsonArray value) {
        objects.push(JsonSerializer.deserializeObject(value, model.objects));
        snapshot.insert(index + idx, value);
        model.addOrRemoveParent(value, id, true);
        model.bytesUsed += (value == null ? "null" : value.toJsonString()).length();
      }
    });
    ValuesAddedEvent event =
        new ValuesAddedEventImpl(event(sessionId, userId).set("index", index).set("values", objects));
    fireEvent(event);
  }

  private void removeAndFireEvent(int index, int length, String sessionId, String userId) {
    assert index + length <= length();
    JsonArray objects = Json.createArray();
    for (int i = 0; i < length; i++) {
      objects.push(get(index));
      JsonArray value = snapshot.getArray(index);
      snapshot.remove(index);
      model.addOrRemoveParent(value, id, false);
      model.bytesUsed -= value.toJsonString().length();
    }
    ValuesRemovedEvent event =
        new ValuesRemovedEventImpl(event(sessionId, userId).set("index", index).set("values", objects));
    fireEvent(event);
  }

  private void replaceAndFireEvent(final int index, JsonArray values, String sessionId,
                                   String userId) {
    assert index + values.length() <= length();
    final JsonArray oldObjects = Json.createArray();
    final JsonArray newObjects = Json.createArray();
    values.forEach(new ListIterator<JsonArray>() {
      @Override
      public void call(int idx, JsonArray newValue) {
        oldObjects.push(get(index + idx));
        newObjects.push(JsonSerializer.deserializeObject(newValue, model.objects));
        JsonArray oldValue = snapshot.getArray(index + idx);
        snapshot.remove(index + idx);
        snapshot.insert(index + idx++, newValue);
        model.addOrRemoveParent(oldValue, id, false);
        model.addOrRemoveParent(newValue, id, true);
        model.bytesUsed -= oldValue.toJsonString().length();
        model.bytesUsed += newValue.toJsonString().length();
      }
    });
    ValuesSetEvent event =
        new ValuesSetEventImpl(event(sessionId, userId).set("index", index).set("oldValues",
            oldObjects).set("newValues", newObjects));
    fireEvent(event);
  }

  private JsonArray subValues(int startIndex, int length) {
    JsonArray array = Json.createArray();
    for (int i = startIndex; i < startIndex + length; i++) {
      array.push(snapshot.getArray(i));
    }
    return array;
  }
}
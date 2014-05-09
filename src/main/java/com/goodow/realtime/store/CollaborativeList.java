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
import com.goodow.realtime.core.HandlerRegistration;
import com.goodow.realtime.json.Json;
import com.goodow.realtime.json.JsonArray;
import com.goodow.realtime.json.JsonArray.ListIterator;
import com.goodow.realtime.operation.Operation;
import com.goodow.realtime.operation.OperationComponent;
import com.goodow.realtime.operation.create.CreateComponent;
import com.goodow.realtime.operation.list.ListTarget;
import com.goodow.realtime.operation.list.json.JsonDeleteComponent;
import com.goodow.realtime.operation.list.json.JsonInsertComponent;
import com.goodow.realtime.operation.list.json.JsonReplaceComponent;
import com.goodow.realtime.operation.map.json.JsonMapComponent;
import com.goodow.realtime.store.util.JsonSerializer;
import com.goodow.realtime.store.util.ModelFactory;

import com.google.common.annotations.GwtIncompatible;

import org.timepedia.exporter.client.Export;
import org.timepedia.exporter.client.ExportAfterCreateMethod;
import org.timepedia.exporter.client.ExportPackage;
import org.timepedia.exporter.client.NoExport;

import java.util.Comparator;

/**
 * A collaborative list. A list can contain other Realtime collaborative objects, custom
 * collaborative objects, primitive values, or objects that can be serialized to JSON.
 * <p>
 * Changes to the list will automatically be synced with the server and other collaborators. To
 * listen for changes, add EventListeners for the following event types:
 * <ul>
 * <li>{@link com.goodow.realtime.store.EventType#VALUES_ADDED}
 * <li>{@link com.goodow.realtime.store.EventType#VALUES_REMOVED}
 * <li>{@link com.goodow.realtime.store.EventType#VALUES_SET}
 * </ul>
 * <p>
 * This class should not be instantiated directly. To create a new list, use
 * {@link com.goodow.realtime.store.Model#createList(Object...)}.
 */
@ExportPackage(ModelFactory.PACKAGE_PREFIX_REALTIME)
@Export(all = true)
public class CollaborativeList extends CollaborativeObject {
  @GwtIncompatible(ModelFactory.JS_REGISTER_PROPERTIES)
  @ExportAfterCreateMethod
  // @formatter:off
  public native static void __jsniRunAfter__() /*-{
    var _ = $wnd.good.realtime.CollaborativeList.prototype;
//    Object.defineProperties(_, {
//      id : {
//        get : function() {
//          return this.g.@com.goodow.realtime.store.CollaborativeObject::id;
//        }
//      },
//      length : {
//        get : function() {
//          return this.g.@com.goodow.realtime.store.CollaborativeList::length()();
//        },
//        set : function(length) {
//          return this.g.@com.goodow.realtime.store.CollaborativeList::setLength(I)(length);
//        }
//      }
//    });
    _.get = function(index) {
      this.g.@com.goodow.realtime.store.CollaborativeList::checkIndex(IZ)(index, false)
      var p = this.g.@com.goodow.realtime.store.CollaborativeList::snapshot[index];
      if (p === undefined) {
        return undefined;
      } else if (p[0] != @com.goodow.realtime.store.model.util.JsonSerializer::REFERENCE_TYPE) {
        return p[1];
      } else {
        var v = this.g.@com.goodow.realtime.store.CollaborativeList::get(I)(index);
        return @org.timepedia.exporter.client.ExporterUtil::wrap(Ljava/lang/Object;)(v);
      }
    };
    _.indexOf = function(value, opt_comparatorFn) {
      if (opt_comparatorFn === undefined) {
        var v = @org.timepedia.exporter.client.ExporterUtil::gwtInstance(Ljava/lang/Object;)(value);
        return this.g.@com.goodow.realtime.store.CollaborativeList::indexOf(Ljava/lang/Object;Ljava/util/Comparator;)(v, null);
      } else {
        for ( var i = 0, len = this.length(); i < len; i++) {
          if (opt_comparatorFn(value, this.get(i)) == 0) {
            return i;
          }
        }
        return -1;
      }
    };
    _.insert = function(index, value) {
      var v = @org.timepedia.exporter.client.ExporterUtil::gwtInstance(Ljava/lang/Object;)(value);
      this.g.@com.goodow.realtime.store.CollaborativeList::insert(ILjava/lang/Object;)(index,v);
    };
    _.lastIndexOf = function(value, opt_comparatorFn) {
      if (opt_comparatorFn === undefined) {
        var v = @org.timepedia.exporter.client.ExporterUtil::gwtInstance(Ljava/lang/Object;)(value);
        return this.g.@com.goodow.realtime.store.CollaborativeList::lastIndexOf(Ljava/lang/Object;Ljava/util/Comparator;)(v, null);
      } else {
        for ( var i = this.length() - 1; i >= 0; i--) {
          if (opt_comparatorFn(value, this.get(i)) == 0) {
            return i;
          }
        }
        return -1;
      }
    };
    _.push = function(value) {
      this.insert(this.length(), value);
      return this.length();
    };
    _.removeValue = function(value) {
      var v = @org.timepedia.exporter.client.ExporterUtil::gwtInstance(Ljava/lang/Object;)(value);
      return this.g.@com.goodow.realtime.store.CollaborativeList::removeValue(Ljava/lang/Object;)(v);
    };
    _.set = function(index, value) {
      var v = @org.timepedia.exporter.client.ExporterUtil::gwtInstance(Ljava/lang/Object;)(value);
      this.g.@com.goodow.realtime.store.CollaborativeList::set(ILjava/lang/Object;)(index,v);
    };
  }-*/;
  // @formatter:on

  private final JsonArray snapshot;

  /**
   * @param model The document model.
   */
  CollaborativeList(Model model) {
    super(model);
    snapshot = Json.createArray();
  }

  public HandlerRegistration addValuesAddedListener(Handler<ValuesAddedEvent> handler) {
    return addEventListener(EventType.VALUES_ADDED, handler, false);
  }

  public HandlerRegistration addValuesRemovedListener(Handler<ValuesRemovedEvent> handler) {
    return addEventListener(EventType.VALUES_REMOVED, handler, false);
  }

  public HandlerRegistration addValuesSetListener(Handler<ValuesSetEvent> handler) {
    return addEventListener(EventType.VALUES_SET, handler, false);
  }

  /**
   * Returns a copy of the contents of this collaborative list as an array. Changes to the returned
   * object will not affect the original collaborative list.
   * 
   * @return A copy of the contents of this collaborative list.
   */
  public JsonArray asArray() {
    final JsonArray objects = Json.createArray();
    snapshot.forEach(new ListIterator<JsonArray>() {
      @Override
      public void call(int index, JsonArray value) {
        objects.push(get(index));
      }
    });
    return objects;
  }

  /**
   * Removes all values from the list.
   */
  public void clear() {
    int length = length();
    if (length == 0) {
      return;
    }
    removeRange(0, length);
  }

  /**
   * Gets the value at the given index.
   * 
   * @param index The index.
   * @return The value at the given index.
   * @exception java.lang.ArrayIndexOutOfBoundsException
   */
  @SuppressWarnings("unchecked")
  @NoExport
  public <T> T get(int index) {
    checkIndex(index, false);
    return (T) JsonSerializer.deserializeObject(snapshot.getArray(index), model.objects);
  }

  /**
   * Returns the first index of the given value, or -1 if it cannot be found.
   * 
   * @param value The value to find.
   * @param opt_comparator Optional comparator function used to determine the equality of two items.
   * @return The index of the given value, or -1 if it cannot be found.
   */
  @NoExport
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

  /**
   * Inserts an item into the list at a given index.
   * 
   * @param index The index to insert at.
   * @param value The value to add.
   * @exception java.lang.ArrayIndexOutOfBoundsException
   */
  @NoExport
  public void insert(int index, Object value) {
    insertAll(index, value);
  }

  /**
   * Inserts a list of items into the list at a given index.
   * 
   * @param index The index at which to insert.
   * @param values The values to insert.
   * @exception java.lang.ArrayIndexOutOfBoundsException
   */
  public void insertAll(int index, Object... values) {
    assert values != null;
    checkIndex(index, true);
    if (values.length == 0) {
      return;
    } else {
      JsonArray[] array = JsonSerializer.serializeObjects(values);
      JsonInsertComponent op = new JsonInsertComponent(id, index, array);
      consumeAndSubmit(op);
    }
  }

  /**
   * Returns the last index of the given value, or -1 if it cannot be found.
   * 
   * @param value The value to find.
   * @param opt_comparator Optional comparator function used to determine the equality of two items.
   * @return The index of the given value, or -1 if it cannot be found.
   */
  @NoExport
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

  /**
   * @return The number of entries in the list. Assign to this field to reduce the size of the list.
   *         Note that the length given must be < or equal to the current size. The length of a list
   *         cannot be extended in this way.
   */
  public int length() {
    return snapshot.length();
  }

  /**
   * Adds an item to the end of the list.
   * 
   * @param value The value to add.
   * @return The new array length.
   */
  @NoExport
  public int push(Object value) {
    insert(length(), value);
    return length();
  }

  /**
   * Adds an array of values to the end of the list.
   * 
   * @param values The values to add.
   */
  public void pushAll(Object... values) {
    insertAll(length(), values);
  }

  /**
   * Creates an IndexReference at the given index. If canBeDeleted is true, then a delete over the
   * index will delete the reference. Otherwise the reference will shift to the beginning of the
   * deleted range.
   * 
   * @param index The index of the reference.
   * @param canBeDeleted Whether this index is deleted when there is a delete of a range covering
   *          this index.
   * @return The newly registered reference.
   * @exception java.lang.ArrayIndexOutOfBoundsException
   */
  public IndexReference registerReference(int index, boolean canBeDeleted) {
    checkIndex(index, true);
    return model.createIndexReference(id, index, canBeDeleted);
  }

  /**
   * Removes the item at the given index from the list.
   * 
   * @param index The index of the item to remove.
   * @exception java.lang.ArrayIndexOutOfBoundsException
   */
  public void remove(int index) {
    removeRange(index, index + 1);
  }

  /**
   * Removes the items between startIndex (inclusive) and endIndex (exclusive).
   * 
   * @param startIndex The start index of the range to remove (inclusive).
   * @param endIndex The end index of the range to remove (exclusive).
   * @exception java.lang.ArrayIndexOutOfBoundsException
   */
  public void removeRange(int startIndex, int endIndex) {
    if (startIndex < 0 || startIndex >= endIndex || endIndex > length()) {
      throw new ArrayIndexOutOfBoundsException("StartIndex: " + startIndex + ", EndIndex: "
          + endIndex + ", Size: " + length());
    }
    JsonArray[] values = subValues(startIndex, endIndex - startIndex);
    JsonDeleteComponent op = new JsonDeleteComponent(id, startIndex, values);
    consumeAndSubmit(op);
  }

  /**
   * Removes the first instance of the given value from the list.
   * 
   * @param value The value to remove.
   * @return Whether the item was removed.
   */
  @NoExport
  public boolean removeValue(Object value) {
    int index = indexOf(value, null);
    if (index == -1) {
      return false;
    }
    remove(index);
    return true;
  }

  /**
   * Replaces items in the list with the given items, starting at the given index.
   * 
   * @param index The index to set at.
   * @param values The values to insert.
   * @exception java.lang.ArrayIndexOutOfBoundsException
   */
  public void replaceRange(int index, Object... values) {
    assert values != null;
    if (values.length == 0) {
      throw new UnsupportedOperationException(
          "At least one value must be specified for a set mutation.");
    }
    checkIndex(index + values.length, true);
    JsonReplaceComponent op =
        new JsonReplaceComponent(id, index, subValues(index, values.length), JsonSerializer
            .serializeObjects(values));
    consumeAndSubmit(op);
  }

  /**
   * Sets the item at the given index
   * 
   * @param index The index to insert at.
   * @param value The value to set.
   * @exception java.lang.ArrayIndexOutOfBoundsException
   */
  @NoExport
  public void set(int index, Object value) {
    replaceRange(index, value);
  }

  /**
   * @see #length()
   * @param length the new length of the array
   * @exception java.lang.ArrayIndexOutOfBoundsException
   */
  @NoExport
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
        } else if (val instanceof CollaborativeObject) {
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
    ((Operation<ListTarget<JsonArray[]>>) component).apply(new ListTarget<JsonArray[]>() {
      @Override
      public void delete(int startIndex, int length) {
        removeAndFireEvent(startIndex, length, sessionId, userId);
      }

      @Override
      public void insert(int startIndex, JsonArray[] values) {
        insertAndFireEvent(startIndex, values, sessionId, userId);
      }

      @Override
      public void replace(int startIndex, JsonArray[] values) {
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

  private void insertAndFireEvent(int index, JsonArray[] values, String sessionId, String userId) {
    assert index <= length();
    Object[] objects = new Object[values.length];
    int i = 0;
    for (JsonArray value : values) {
      objects[i] = JsonSerializer.deserializeObject(value, model.objects);
      snapshot.insert(index + i++, value);
      model.addOrRemoveParent(value, id, true);
      model.bytesUsed += (value == null ? "null" : value.toJsonString()).length();
    }
    ValuesAddedEvent event = new ValuesAddedEvent(this, sessionId, userId, index, objects);
    fireEvent(event);
    model.setIndexReferenceIndex(id, true, index, values.length, sessionId, userId);
  }

  private void removeAndFireEvent(int index, int length, String sessionId, String userId) {
    assert index + length <= length();
    Object[] objects = new Object[length];
    for (int i = 0; i < length; i++) {
      objects[i] = get(index);
      JsonArray value = snapshot.getArray(index);
      snapshot.remove(index);
      model.addOrRemoveParent(value, id, false);
      model.bytesUsed -= value.toJsonString().length();
    }
    ValuesRemovedEvent event = new ValuesRemovedEvent(this, sessionId, userId, index, objects);
    fireEvent(event);
    model.setIndexReferenceIndex(id, false, index, length, sessionId, userId);
  }

  private void replaceAndFireEvent(int index, JsonArray[] values, String sessionId, String userId) {
    assert index + values.length <= length();
    Object[] oldObjects = new Object[values.length];
    Object[] newObjects = new Object[values.length];
    int i = 0;
    for (JsonArray newValue : values) {
      oldObjects[i] = get(index + i);
      newObjects[i] = JsonSerializer.deserializeObject(newValue, model.objects);
      JsonArray oldValue = snapshot.getArray(index + i);
      snapshot.remove(index + i);
      snapshot.insert(index + i++, newValue);
      model.addOrRemoveParent(oldValue, id, false);
      model.addOrRemoveParent(newValue, id, true);
      model.bytesUsed -= oldValue.toJsonString().length();
      model.bytesUsed += newValue.toJsonString().length();
    }
    ValuesSetEvent event =
        new ValuesSetEvent(this, sessionId, userId, index, oldObjects, newObjects);
    fireEvent(event);
  }

  private JsonArray[] subValues(int startIndex, int length) {
    JsonArray[] array = new JsonArray[length];
    for (int i = 0; i < length; i++) {
      array[i] = snapshot.getArray(startIndex + i);
    }
    return array;
  }
}
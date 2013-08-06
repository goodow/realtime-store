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
import com.goodow.realtime.operation.RealtimeOperation;
import com.goodow.realtime.operation.list.ArrayOp;
import com.goodow.realtime.operation.list.algorithm.ListTarget;
import com.goodow.realtime.operation.util.JsonUtility;

import com.google.common.annotations.GwtIncompatible;

import org.timepedia.exporter.client.Export;
import org.timepedia.exporter.client.ExportAfterCreateMethod;
import org.timepedia.exporter.client.ExportPackage;
import org.timepedia.exporter.client.NoExport;

import java.util.Comparator;
import java.util.Set;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonValue;

/**
 * A collaborative list. A list can contain other Realtime collaborative objects, custom
 * collaborative objects, primitive values, or objects that can be serialized to JSON.
 * <p>
 * Changes to the list will automatically be synced with the server and other collaborators. To
 * listen for changes, add EventListeners for the following event types:
 * <ul>
 * <li>{@link com.goodow.realtime.EventType#VALUES_ADDED}
 * <li>{@link com.goodow.realtime.EventType#VALUES_REMOVED}
 * <li>{@link com.goodow.realtime.EventType#VALUES_SET}
 * </ul>
 * <p>
 * This class should not be instantiated directly. To create a new list, use
 * {@link com.goodow.realtime.Model#createList(Object...)}.
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
//          return this.g.@com.goodow.realtime.CollaborativeObject::id;
//        }
//      },
//      length : {
//        get : function() {
//          return this.g.@com.goodow.realtime.CollaborativeList::length()();
//        },
//        set : function(length) {
//          return this.g.@com.goodow.realtime.CollaborativeList::setLength(I)(length);
//        }
//      }
//    });
    _.asArray = function() {
      var values = [];
      for ( var i = 0, len = this.length(); i < len; i++) {
        values[i] = this.get(i);
      }
      return values;
    };
    _.get = function(index) {
      this.g.@com.goodow.realtime.CollaborativeList::checkIndex(IZ)(index, false)
      var p = this.g.@com.goodow.realtime.CollaborativeList::snapshot[index];
      if (p === undefined) {
        return undefined;
      } else if (p[0] != @com.goodow.realtime.model.util.JsonSerializer::REFERENCE_TYPE) {
        return p[1];
      } else {
        var v = this.g.@com.goodow.realtime.CollaborativeList::get(I)(index);
        return @org.timepedia.exporter.client.ExporterUtil::wrap(Ljava/lang/Object;)(v);
      }
    };
    _.indexOf = function(value, opt_comparatorFn) {
      if (opt_comparatorFn === undefined) {
        var v = @org.timepedia.exporter.client.ExporterUtil::gwtInstance(Ljava/lang/Object;)(value);
        return this.g.@com.goodow.realtime.CollaborativeList::indexOf(Ljava/lang/Object;Ljava/util/Comparator;)(v, null);
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
      this.g.@com.goodow.realtime.CollaborativeList::insert(ILjava/lang/Object;)(index,v);
    };
    _.lastIndexOf = function(value, opt_comparatorFn) {
      if (opt_comparatorFn === undefined) {
        var v = @org.timepedia.exporter.client.ExporterUtil::gwtInstance(Ljava/lang/Object;)(value);
        return this.g.@com.goodow.realtime.CollaborativeList::lastIndexOf(Ljava/lang/Object;Ljava/util/Comparator;)(v, null);
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
      return this.g.@com.goodow.realtime.CollaborativeList::removeValue(Ljava/lang/Object;)(v);
    };
    _.set = function(index, value) {
      var v = @org.timepedia.exporter.client.ExporterUtil::gwtInstance(Ljava/lang/Object;)(value);
      this.g.@com.goodow.realtime.CollaborativeList::set(ILjava/lang/Object;)(index,v);
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

  public void addValuesAddedListener(EventHandler<ValuesAddedEvent> handler) {
    addEventListener(EventType.VALUES_ADDED, handler, false);
  }

  public void addValuesRemovedListener(EventHandler<ValuesRemovedEvent> handler) {
    addEventListener(EventType.VALUES_REMOVED, handler, false);
  }

  public void addValuesSetListener(EventHandler<ValuesSetEvent> handler) {
    addEventListener(EventType.VALUES_SET, handler, false);
  }

  /**
   * Returns a copy of the contents of this collaborative list as an array. Changes to the returned
   * object will not affect the original collaborative list.
   * 
   * @return A copy of the contents of this collaborative list.
   */
  @SuppressWarnings("unchecked")
  @NoExport
  public <T> T[] asArray() {
    int length = length();
    Object[] objects = new Object[length];
    for (int i = 0; i < length; i++) {
      objects[i] = get(i);
    }
    return (T[]) objects;
  }

  /**
   * Removes all values from the list.
   */
  public void clear() {
    if (length() == 0) {
      return;
    }
    removeRange(0, length());
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
    return (T) JsonSerializer.deserializeObject(snapshot.get(index), model.objects);
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
        if (JsonUtility.jsonEqual(serializedValue, snapshot.get(i))) {
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
    checkIndex(index, true);
    JsonArray array = Json.createArray();
    if (values == null) {
      array.set(0, (JsonValue) null);
    } else if (values.length == 0) {
      return;
    } else {
      for (int i = 0, len = values.length; i < len; i++) {
        array.set(i, JsonSerializer.serializeObject(values[i]));
      }
    }
    ArrayOp op = new ArrayOp(true, index, array, length());
    consumeAndSubmit(op);
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
        if (JsonUtility.jsonEqual(serializedValue, snapshot.get(i))) {
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

  public void removeListListener(EventHandler<?> handler) {
    removeEventListener(EventType.VALUES_ADDED, handler, false);
    removeEventListener(EventType.VALUES_REMOVED, handler, false);
    removeEventListener(EventType.VALUES_SET, handler, false);
  }

  /**
   * Removes the items between startIndex (inclusive) and endIndex (exclusive).
   * 
   * @param startIndex The start index of the range to remove (inclusive).
   * @param endIndex The end index of the range to remove (exclusive).
   * @exception java.lang.ArrayIndexOutOfBoundsException
   */
  public void removeRange(int startIndex, int endIndex) {
    int length = length();
    if (startIndex < 0 || startIndex >= length || endIndex <= startIndex || endIndex > length) {
      throw new ArrayIndexOutOfBoundsException("StartIndex: " + startIndex + ", EndIndex: "
          + endIndex + ", Size: " + length);
    }
    JsonArray array = Json.createArray();
    for (int i = startIndex; i < endIndex; i++) {
      array.set(i - startIndex, snapshot.get(i));
    }
    ArrayOp op = new ArrayOp(false, startIndex, array, length);
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
    model.beginCompoundOperation("list.replaceRange");
    int endIndex = index + values.length;
    int length = length();
    removeRange(index, endIndex > length ? length : endIndex);
    insertAll(index, values);
    model.endCompoundOperation();
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
    checkIndex(index, false);
    model.beginCompoundOperation("list.set");
    remove(index);
    insert(index, value);
    model.endCompoundOperation();
  }

  /**
   * @see #length()
   * @param length the new length of the array
   * @exception java.lang.ArrayIndexOutOfBoundsException
   */
  @NoExport
  public void setLength(int length) {
    checkIndex(length, true);
    int len = length();
    if (length == len) {
      return;
    }
    removeRange(length, len);
  }

  @Override
  protected void consume(final RealtimeOperation<?> operation) {
    operation.<ListTarget<JsonArray>> getOp().apply(new ListTarget<JsonArray>() {
      private int cursor;

      @Override
      public ListTarget<JsonArray> delete(JsonArray list) {
        assert list.length() > 0;
        assert cursor + list.length() <= length();
        removeAndFireEvent(cursor, list, operation.sessionId, operation.userId);
        return null;
      }

      @Override
      public ListTarget<JsonArray> insert(JsonArray list) {
        assert list.length() > 0;
        assert cursor <= length();
        insertAndFireEvent(cursor, list, operation.sessionId, operation.userId);
        cursor += list.length();
        return null;
      }

      @Override
      public ListTarget<JsonArray> retain(int length) {
        cursor += length;
        return null;
      }
    });
  }

  @Override
  Operation<?>[] toInitialization() {
    ArrayOp op = null;
    if (length() != 0) {
      op = new ArrayOp();
      op.insert(snapshot);
    }
    return new Operation[] {new CreateOperation(CreateOperation.COLLABORATIVE_LIST, id), op};
  }

  @Override
  void toString(Set<String> seen, StringBuilder sb) {
    if (seen.contains(id)) {
      sb.append("<List: ").append(id).append(">");
      return;
    }
    seen.add(id);
    sb.append("[");
    boolean isFirst = true;
    for (int i = 0, len = length(); i < len; i++) {
      if (!isFirst) {
        sb.append(", ");
      }
      isFirst = false;
      Object value = get(i);
      if (value == null) {
        sb.append("null");
      } else if (value instanceof CollaborativeObject) {
        CollaborativeObject obj = (CollaborativeObject) value;
        obj.toString(seen, sb);
      } else {
        sb.append("[JsonValue " + snapshot.getArray(i).get(1).toJson() + "]");
      }
    }
    sb.append("]");
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

  private void insertAndFireEvent(int index, JsonArray array, String sessionId, String userId) {
    int length = array.length();
    Object[] objects = new Object[length];
    for (int i = 0; i < length; i++) {
      JsonValue value = array.get(i);
      objects[i] = JsonSerializer.deserializeObject(value, model.objects);
      snapshot.insert(index + i, value);
      model.addOrRemoveParent(value, id, true);
    }
    ValuesAddedEvent event = new ValuesAddedEvent(this, sessionId, userId, index, objects);
    fireEvent(event);
    model.setIndexReferenceIndex(id, true, index, length, sessionId, userId);
  }

  private void removeAndFireEvent(int index, JsonArray array, String sessionId, String userId) {
    int length = array.length();
    Object[] objects = new Object[length];
    for (int i = 0; i < length; i++) {
      assert JsonUtility.jsonEqual(snapshot.get(index), array.get(i));
      objects[i] = get(index);
      snapshot.remove(index);
      model.addOrRemoveParent(array.get(i), id, false);
    }
    ValuesRemovedEvent event = new ValuesRemovedEvent(this, sessionId, userId, index, objects);
    fireEvent(event);
    model.setIndexReferenceIndex(id, false, index, length, sessionId, userId);
  }
}

/*
 * Copyright 2014 Goodow.com
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
import com.goodow.realtime.json.JsonArray;
import com.google.gwt.core.client.js.JsInterface;
import com.google.gwt.core.client.js.JsProperty;

import java.util.Comparator;

//@JsInterface
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
 * {@link com.goodow.realtime.store.Model#createList(JsonArray)}.
 */
public interface CollaborativeList extends CollaborativeObject {
  Registration onValuesAdded(Handler<ValuesAddedEvent> handler);

  Registration onValuesRemoved(Handler<ValuesRemovedEvent> handler);

  Registration onValuesSet(Handler<ValuesSetEvent> handler);

  /**
   * Returns a copy of the contents of this collaborative list as a Json array. Changes to the returned
   * object will not affect the original collaborative list.
   *
   * @return A copy of the contents of this collaborative list.
   */
  JsonArray asArray();

  /**
   * Removes all values from the list.
   */
  void clear();

  /**
   * Gets the value at the given index.
   *
   * @param index The index.
   * @return The value at the given index.
   * @exception ArrayIndexOutOfBoundsException
   */
  <T> T get(int index);

  /**
   * Returns the first index of the given value, or -1 if it cannot be found.
   *
   * @param value The value to find.
   * @param opt_comparator Optional comparator function used to determine the equality of two items.
   * @return The index of the given value, or -1 if it cannot be found.
   */
  int indexOf(Object value, Comparator<Object> opt_comparator);

  /**
   * Inserts an item into the list at a given index.
   *
   * @param index The index to insert at.
   * @param value The value to add.
   * @exception ArrayIndexOutOfBoundsException
   */
  void insert(int index, Object value);

  /**
   * Inserts a list of items into the list at a given index.
   *
   * @param index The index at which to insert.
   * @param values The values to insert.
   * @exception ArrayIndexOutOfBoundsException
   */
  void insertAll(int index, JsonArray values);

  /**
   * Returns the last index of the given value, or -1 if it cannot be found.
   *
   * @param value The value to find.
   * @param opt_comparator Optional comparator function used to determine the equality of two items.
   * @return The index of the given value, or -1 if it cannot be found.
   */
  int lastIndexOf(Object value, Comparator<Object> opt_comparator);

  /**
   * Adds an item to the end of the list.
   *
   * @param value The value to add.
   * @return The new array length.
   */
  int push(Object value);

  /**
   * Adds an array of values to the end of the list.
   *
   * @param values The values to add.
   */
  void pushAll(JsonArray values);

  /**
   * Creates an IndexReference at the given index. If canBeDeleted is true, then a delete over the
   * index will delete the reference. Otherwise the reference will shift to the beginning of the
   * deleted range.
   *
   * @param index The index of the reference.
   * @param canBeDeleted Whether this index is deleted when there is a delete of a range covering
   *          this index.
   * @return The newly registered reference.
   * @exception ArrayIndexOutOfBoundsException
   */
  IndexReference registerReference(int index, boolean canBeDeleted);

  /**
   * Removes the item at the given index from the list.
   *
   * @param index The index of the item to remove.
   * @exception ArrayIndexOutOfBoundsException
   */
  void remove(int index);

  /**
   * Removes the items between startIndex (inclusive) and endIndex (exclusive).
   *
   * @param startIndex The start index of the range to remove (inclusive).
   * @param endIndex The end index of the range to remove (exclusive).
   * @exception ArrayIndexOutOfBoundsException
   */
  void removeRange(int startIndex, int endIndex);

  /**
   * Removes the first instance of the given value from the list.
   *
   * @param value The value to remove.
   * @return Whether the item was removed.
   */
  boolean removeValue(Object value);

  /**
   * Replaces items in the list with the given items, starting at the given index.
   *
   * @param index The index to set at.
   * @param values The values to insert.
   * @exception ArrayIndexOutOfBoundsException
   */
  void replaceRange(int index, JsonArray values);

  /**
   * Sets the item at the given index
   *
   * @param index The index to insert at.
   * @param value The value to set.
   * @exception ArrayIndexOutOfBoundsException
   */
  void set(int index, Object value);

  @JsProperty
  /**
   * @return The number of entries in the list. Assign to this field to reduce the size of the list.
   *         Note that the length given must be < or equal to the current size. The length of a list
   *         cannot be extended in this way.
   */
  int length();

  @JsProperty
  /**
   * @see #length()
   * @param length the new length of the array
   * @exception ArrayIndexOutOfBoundsException
   */
  void length(int length);
}

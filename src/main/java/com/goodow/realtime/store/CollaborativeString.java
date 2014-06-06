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
import com.google.gwt.core.client.js.JsInterface;
import com.google.gwt.core.client.js.JsProperty;

//@JsInterface
/**
 * Creates a new collaborative string. Unlike regular strings, collaborative strings are mutable.
 * <p>
 * Changes to the string will automatically be synced with the server and other collaborators. To
 * listen for changes, add EventListeners for the following event types:
 * <ul>
 * <li>{@link com.goodow.realtime.store.EventType#TEXT_INSERTED}
 * <li>{@link com.goodow.realtime.store.EventType#TEXT_DELETED}
 * </ul>
 * <p>
 * This class should not be instantiated directly. To create a new collaborative string, use
 * {@link com.goodow.realtime.store.Model#createString(String)}
 */
public interface CollaborativeString extends CollaborativeObject {
  Registration onTextDeleted(Handler<TextDeletedEvent> handler);

  Registration onTextInserted(Handler<TextInsertedEvent> handler);

  /**
   * Appends a string to the end of this one.
   *
   * @param text The new text to append.
   * @exception IllegalArgumentException
   */
  void append(String text);

  /**
   * Gets a string representation of the collaborative string.
   *
   * @return A string representation of the collaborative string.
   */
  String getText();

  /**
   * Inserts a string into the collaborative string at a specific index.
   *
   * @param index The index to insert at.
   * @param text The new text to insert.
   * @exception IllegalArgumentException
   * @exception StringIndexOutOfBoundsException
   */
  void insertString(int index, String text);

  /**
   * Creates an IndexReference at the given {@code index}. If {@code canBeDeleted} is set, then a
   * delete over the index will delete the reference. Otherwise the reference will shift to the
   * beginning of the deleted range.
   *
   * @param index The index of the reference.
   * @param canBeDeleted Whether this index is deleted when there is a delete of a range covering
   *          this index.
   * @return The newly registered reference.
   */
  IndexReference registerReference(int index, boolean canBeDeleted);

  /**
   * Deletes the text between startIndex (inclusive) and endIndex (exclusive).
   *
   * @param startIndex The start index of the range to delete (inclusive).
   * @param endIndex The end index of the range to delete (exclusive).
   * @exception StringIndexOutOfBoundsException
   */
  void removeRange(int startIndex, int endIndex);

  /**
   * Sets the contents of this collaborative string. Note that this method performs a text diff
   * between the current string contents and the new contents so that the string will be modified
   * using the minimum number of text inserts and deletes possible to change the current contents to
   * the newly-specified contents.
   *
   * @param text The new value of the string.
   */
  void setText(String text);

  @JsProperty
  /**
   * @return The length of the string. Read only.
   */
  int length();
}

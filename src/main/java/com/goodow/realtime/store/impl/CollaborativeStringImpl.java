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

import com.goodow.realtime.core.Diff;
import com.goodow.realtime.core.Handler;
import com.goodow.realtime.core.Platform;
import com.goodow.realtime.core.Registration;
import com.goodow.realtime.operation.Operation;
import com.goodow.realtime.operation.OperationComponent;
import com.goodow.realtime.operation.create.CreateComponent;
import com.goodow.realtime.operation.list.ListTarget;
import com.goodow.realtime.operation.list.string.StringDeleteComponent;
import com.goodow.realtime.operation.list.string.StringInsertComponent;
import com.goodow.realtime.store.CollaborativeString;
import com.goodow.realtime.store.EventType;
import com.goodow.realtime.store.IndexReference;
import com.goodow.realtime.store.TextDeletedEvent;
import com.goodow.realtime.store.TextInsertedEvent;

class CollaborativeStringImpl extends CollaborativeObjectImpl implements CollaborativeString {
  private final StringBuilder snapshot;

  CollaborativeStringImpl(ModelImpl model) {
    super(model);
    snapshot = new StringBuilder();
  }

  @Override public Registration onTextDeleted(Handler<TextDeletedEvent> handler) {
    return addEventListener(EventType.TEXT_DELETED, handler, false);
  }

  @Override public Registration onTextInserted(Handler<TextInsertedEvent> handler) {
    return addEventListener(EventType.TEXT_INSERTED, handler, false);
  }

  @Override public void append(String text) {
    insertString(length(), text);
  }

  @Override public String getText() {
    return snapshot.toString();
  }

  @Override public void insertString(int index, String text) {
    if (text == null || text.isEmpty()) {
      throw new IllegalArgumentException(
          "At least one value must be specified for an insert mutation. text: " + text);
    }
    checkIndex(index);
    StringInsertComponent op = new StringInsertComponent(id, index, text);
    consumeAndSubmit(op);
  }

  @Override public int length() {
    return snapshot.length();
  }

  @Override public IndexReference registerReference(int index, boolean canBeDeleted) {
    checkIndex(index);
    return model.createIndexReference(id, index, canBeDeleted);
  }

  @Override public void removeRange(int startIndex, int endIndex) {
    int length = length();
    if (startIndex < 0 || startIndex >= length || endIndex <= startIndex || endIndex > length) {
      throw new StringIndexOutOfBoundsException("StartIndex: " + startIndex + ", EndIndex: "
          + endIndex + ", Size: " + length);
    }
    StringDeleteComponent op =
        new StringDeleteComponent(id, startIndex, snapshot.substring(startIndex, endIndex));
    consumeAndSubmit(op);
  }

  @Override public void setText(String text) {
    if (text == null) {
      throw new IllegalArgumentException("Expected string for text, but was: null");
    }
    model.beginCompoundOperation("replaceText");
    Platform.diff().diff(getText(), text, new Diff.ListTarget<String>() {
      @Override
      public void insert(int startIndex, String values) {
        insertString(startIndex, values);
      }

      @Override
      public void remove(int startIndex, int length) {
        removeRange(startIndex, startIndex + length);
      }

      @Override
      public void replace(int startIndex, String values) {
        throw new UnsupportedOperationException();
      }

      @Override
      public void move(int fromIndex, int toIndex, int length) {
        throw new UnsupportedOperationException();
      }
    });
    model.endCompoundOperation();
  }

  @Override
  public String toJson() {
    return getText();
  }

  @SuppressWarnings("unchecked")
  @Override
  protected void consume(final String userId, final String sessionId,
      OperationComponent<?> component) {
    ((Operation<ListTarget<String>>) component).apply(new ListTarget<String>() {
      @Override
      public void delete(int startIndex, int length) {
        deleteAndFireEvent(startIndex, length, sessionId, userId);
      }

      @Override
      public void insert(int startIndex, String values) {
        insertAndFireEvent(startIndex, values, sessionId, userId);
      }

      @Override
      public void replace(int startIndex, String values) {
        throw new UnsupportedOperationException();
      }
    });
  }

  @Override
  OperationComponent<?>[] toInitialization() {
    OperationComponent<?>[] toRtn = new OperationComponent[1 + (length() == 0 ? 0 : 1)];
    toRtn[0] = new CreateComponent(id, CreateComponent.STRING);
    if (length() != 0) {
      toRtn[1] = new StringInsertComponent(id, 0, getText());
    }
    return toRtn;
  }

  private void checkIndex(int index) {
    int length = length();
    if (index < 0 || index > length) {
      throw new StringIndexOutOfBoundsException("Index: " + index + ", Size: " + length);
    }
  }

  private void deleteAndFireEvent(int startIndex, int length, String sessionId, String userId) {
    int endIndex = startIndex + length;
    assert length > 0 && endIndex <= length();
    String toDelete = snapshot.substring(startIndex, endIndex);
    TextDeletedEvent event =
        new TextDeletedEventImpl(event(sessionId, userId).set("index", startIndex)
            .set("text", toDelete));
    snapshot.delete(startIndex, endIndex);
    fireEvent(event);
    model.setIndexReferenceIndex(id, false, startIndex, length, sessionId, userId);
    model.bytesUsed -= length;
  }

  private void insertAndFireEvent(int index, String text, String sessionId, String userId) {
    assert index <= length();
    TextInsertedEvent event =
        new TextInsertedEventImpl(event(sessionId, userId).set("index", index).set("text", text));
    snapshot.insert(index, text);
    fireEvent(event);
    model.setIndexReferenceIndex(id, true, index, text.length(), sessionId, userId);
    model.bytesUsed += text.length();
  }
}

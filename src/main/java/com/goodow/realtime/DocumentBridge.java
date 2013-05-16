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

import com.goodow.realtime.operation.CreateOperation;
import com.goodow.realtime.operation.Operation;
import com.goodow.realtime.operation.OperationSink;
import com.goodow.realtime.operation.RealtimeOperation;
import com.goodow.realtime.operation.RealtimeTransformer;
import com.goodow.realtime.operation.basic.NoOp;

import elemental.json.JsonArray;

public class DocumentBridge implements OperationSink<RealtimeOperation<?>> {
  private static final OperationSink<RealtimeOperation<?>> VOID =
      new OperationSink<RealtimeOperation<?>>() {
        @Override
        public void consume(RealtimeOperation<?> op) {
        }
      };
  public String sessionId;
  public String userId;
  public OperationSink<RealtimeOperation<?>> outputSink = VOID;

  public final Document document;
  public final Model model;
  private final RealtimeTransformer transformer;

  public DocumentBridge() {
    this(null);
    createRoot();
  }

  public DocumentBridge(JsonArray snapshot) {
    transformer = new RealtimeTransformer();
    document = new Document(this, null, null);
    model = document.getModel();
    if (snapshot != null) {
      for (int i = 0, len = snapshot.length(); i < len; i++) {
        JsonArray serializedOp = snapshot.getArray(i);
        Operation<?> op = transformer.createOp(serializedOp);
        @SuppressWarnings({"rawtypes", "unchecked"})
        RealtimeOperation<?> operation = new RealtimeOperation(op, userId, sessionId);
        consume(operation);
      }
    }
  }

  @Override
  public void consume(RealtimeOperation<?> operation) {
    int type = operation.getType();
    if (type == NoOp.TYPE) {
      return;
    }
    if (type == CreateOperation.TYPE) {
      CreateOperation op = (CreateOperation) operation.<DocumentBridge> getOp();
      CollaborativeObject obj;
      switch (op.type) {
        case CreateOperation.COLLABORATIVE_MAP:
          obj = new CollaborativeMap(model);
          break;
        case CreateOperation.COLLABORATIVE_LIST:
          obj = new CollaborativeList(model);
          break;
        case CreateOperation.COLLABORATIVE_STRING:
          obj = new CollaborativeString(model);
          break;
        case CreateOperation.INDEX_REFERENCE:
          obj = new IndexReference(model);
          break;
        default:
          throw new RuntimeException("Shouldn't reach here!");
      }
      obj.id = operation.getId();
      model.objects.put(obj.id, obj);
      return;
    }
    model.getObject(operation.getId()).consume(operation);
  }

  public void createRoot() {
    model.createRoot();
  }

  public void fireDocumentSaveStateChangedEvent(DocumentSaveStateChangedEvent event) {
    document
        .scheduleEvent(Document.EVENT_HANDLER_KEY, EventType.DOCUMENT_SAVE_STATE_CHANGED, event);
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("[");
    boolean isFirst = true;
    for (String id : model.objects.keySet()) {
      CollaborativeObject collaborativeObject = model.getObject(id);
      Operation<?>[] initializeOp = collaborativeObject.toInitialization();
      for (Operation<?> op : initializeOp) {
        if (op == null) {
          continue;
        }
        op.setId(id);
        if (!isFirst) {
          sb.append(",");
        }
        isFirst = false;
        sb.append(new RealtimeOperation(op).toString());
      }
    }
    sb.append("]");
    return sb.toString();
  }

  void consumeAndSubmit(Operation<?> op) {
    @SuppressWarnings({"rawtypes", "unchecked"})
    RealtimeOperation<?> operation = new RealtimeOperation(op, userId, sessionId);
    consume(operation);
    outputSink.consume(operation);
  }

  boolean isLocalSession(String sessionId) {
    return this.sessionId == null || this.sessionId.equals(sessionId);
  }
}

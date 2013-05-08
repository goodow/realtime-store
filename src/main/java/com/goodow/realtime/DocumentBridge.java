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

import com.goodow.realtime.operation.InitializeOperation;
import com.goodow.realtime.operation.Operation;
import com.goodow.realtime.operation.OperationSink;
import com.goodow.realtime.operation.RealtimeOperation;
import com.goodow.realtime.operation.ReferenceShiftedOperation;
import com.goodow.realtime.operation.list.ArrayOp;
import com.goodow.realtime.operation.list.StringOp;
import com.goodow.realtime.operation.list.algorithm.ListOp;
import com.goodow.realtime.operation.map.MapOp;

import elemental.json.JsonArray;

public class DocumentBridge implements OperationSink<RealtimeOperation<?>> {
  public final Document document;
  public final Model model;
  final String sessionId;
  private final String modelId;

  public DocumentBridge(String sessionId, int revision, String modelId, JsonArray snapshot) {
    this.sessionId = sessionId;
    this.modelId = modelId;
    document = new Document(this, null, null);
    model = document.getModel();
    for (int i = 0, len = snapshot.length(); i < len; i++) {
      JsonArray serializedOp = snapshot.getArray(i);
      Operation<?> op = createOp(serializedOp);
      @SuppressWarnings({"rawtypes", "unchecked"})
      RealtimeOperation<?> operation =
          new RealtimeOperation(op, Realtime.getUserId(), revision, sessionId);
      consume(operation);
    }
    if (model.getRoot() == null) {
      model.createRoot();
    }
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  @Override
  public void consume(RealtimeOperation<?> operation) {
    if (operation.getType() == InitializeOperation.TYPE) {
      InitializeOperation op = (InitializeOperation) operation.<DocumentBridge> getOp();
      CollaborativeObject obj;
      switch (op.type) {
        case InitializeOperation.COLLABORATIVE_MAP:
          obj = new CollaborativeMap(model);
          break;
        case InitializeOperation.COLLABORATIVE_LIST:
          obj = new CollaborativeList(model);
          break;
        case InitializeOperation.COLLABORATIVE_STRING:
          obj = new CollaborativeString(model);
          break;
        case InitializeOperation.INDEX_REFERENCE:
          obj = new IndexReference(model);
          break;
        default:
          throw new RuntimeException("Shouldn't reach here!");
      }
      obj.id = operation.getId();
      model.objects.put(obj.id, obj);
      if (op.opt_initialValue == null) {
        return;
      }
      operation =
          new RealtimeOperation(op.opt_initialValue, operation.userId, operation.revision,
              operation.sessionId);
    }
    model.getObject(operation.getId()).consume(operation);
  }

  public Operation<?> createOp(JsonArray serialized) {
    Operation<?> op = null;
    String id = serialized.getString(1);
    switch ((int) serialized.getNumber(0)) {
      case InitializeOperation.TYPE:
        op = new InitializeOperation(serialized.get(2), this);
        break;
      case MapOp.TYPE:
        op = new MapOp(serialized.getArray(2));
        break;
      case ListOp.TYPE:
        if (model.getObject(id) instanceof CollaborativeString) {
          op = new StringOp(serialized.getArray(2));
        } else {
          op = new ArrayOp(serialized.getArray(2));
        }
        break;
      case ReferenceShiftedOperation.TYPE:
        op = new ReferenceShiftedOperation(serialized.getArray(2));
        break;
      default:
        throw new UnsupportedOperationException("Unknow operation type: " + serialized.toJson());
    }
    op.setId(id);
    return op;
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  public RealtimeOperation<?> createRealtimeOperation(JsonArray serialized) {
    // double timestamp;
    // int requestNumber;
    Operation<?> op = createOp(serialized.getArray(0));
    return new RealtimeOperation(op, serialized.getString(1), (int) serialized.getNumber(2),
        serialized.getString(3));
  }

  void consumeAndSubmit(Operation<?> op) {
    @SuppressWarnings({"rawtypes", "unchecked"})
    RealtimeOperation<?> operation = new RealtimeOperation(op, Realtime.getUserId(), -1, sessionId);
    consume(operation);
    // submit(operation);
  }

  boolean isLocalSession(String sessionId) {
    return this.sessionId == null || this.sessionId.equals(sessionId);
  }
}

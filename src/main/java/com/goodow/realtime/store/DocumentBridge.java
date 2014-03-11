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

import com.goodow.realtime.channel.Bus;
import com.goodow.realtime.channel.Message;
import com.goodow.realtime.core.Handler;
import com.goodow.realtime.core.Platform;
import com.goodow.realtime.json.Json;
import com.goodow.realtime.json.JsonArray;
import com.goodow.realtime.operation.AbstractOperation;
import com.goodow.realtime.operation.Operation;
import com.goodow.realtime.operation.OperationSink;
import com.goodow.realtime.operation.RealtimeOperation;
import com.goodow.realtime.operation.TransformerImpl;
import com.goodow.realtime.operation.create.CreateOperation;
import com.goodow.realtime.operation.undo.UndoManager;
import com.goodow.realtime.operation.undo.UndoManagerFactory;
import com.goodow.realtime.store.channel.Constants.Addr;

import java.util.List;

public class DocumentBridge implements OperationSink<RealtimeOperation> {
  public interface OutputSink extends OperationSink<RealtimeOperation> {
    OutputSink VOID = new OutputSink() {
      @Override
      public void close() {
      }

      @Override
      public void consume(RealtimeOperation op) {
      }
    };

    void close();
  }

  final Store store;
  final String id;
  private final Document document;
  private final Model model;
  private UndoManager<RealtimeOperation> undoManager = UndoManagerFactory.getNoOp();
  OutputSink outputSink = OutputSink.VOID;

  public DocumentBridge(Store store, String id, JsonArray snapshot,
      final Handler<Error> errorHandler) {
    this.store = store;
    this.id = id;
    document = new Document(this);
    model = document.getModel();

    if (errorHandler != null) {
      document.handlerRegs.wrap(store.getBus().registerHandler(
          Addr.EVENT + Addr.DOCUMENT_ERROR + ":" + id, new Handler<Message<Error>>() {
            @Override
            public void handle(Message<Error> message) {
              errorHandler.handle(message.body());
            }
          }));
    }

    if (snapshot == null || snapshot.length() == 0) {
      model.createRoot();
    } else {
      TransformerImpl<AbstractOperation<?>> transformer =
          new TransformerImpl<AbstractOperation<?>>();
      for (int i = 0, len = snapshot.length(); i < len; i++) {
        JsonArray serializedOp = snapshot.getArray(i);
        Operation<?> op = transformer.createOperation(serializedOp);
        RealtimeOperation operation = new RealtimeOperation(null, null, op);
        applyLocally(operation);
      }
    }
  }

  /*
   * Incoming operations from remote
   */
  @Override
  public void consume(RealtimeOperation operation) {
    applyLocally(operation);
    nonUndoableOp(operation);
  }

  public Document getDocument() {
    return document;
  }

  public void onCollaboratorChanged(boolean isJoined, Collaborator collaborator) {
    document.onCollaboratorChanged(isJoined, collaborator);
  }

  public void scheduleHandle(final Handler<Document> onLoaded) {
    Platform.scheduler().scheduleDeferred(new Handler<Void>() {
      @Override
      public void handle(Void ignore) {
        Platform.scheduler().handle(onLoaded, document);
      }
    });
  }

  public void setOutputSink(OutputSink outputSink) {
    this.outputSink = outputSink;
  }

  public void setUndoEnabled(boolean undoEnabled) {
    undoManager =
        undoEnabled ? UndoManagerFactory.createUndoManager() : UndoManagerFactory
            .<RealtimeOperation> getNoOp();
  }

  public JsonArray toJson() {
    JsonArray ops = Json.createArray();
    int createOpIdx = 0;
    for (CollaborativeObject object : model.objects.values()) {
      Operation<?>[] initializeOp = object.toInitialization();
      boolean isCreateOp = true;
      for (Operation<?> op : initializeOp) {
        if (isCreateOp) {
          ops.insert(createOpIdx++, op.toJson());
          isCreateOp = false;
        } else {
          ops.push(op.toJson());
        }
      }
    }
    return ops;
  }

  @Override
  public String toString() {
    return toJson().toJsonString();
  }

  void consumeAndSubmit(Operation<?> op) {
    RealtimeOperation operation =
        new RealtimeOperation(store.getUserId(), store.getSessionId(), op);
    applyLocally(operation);
    undoManager.checkpoint();
    undoableOp(operation);
    outputSink.consume(operation);
  }

  boolean isLocalSession(String sessionId) {
    String local = store.getSessionId();
    return sessionId == null ? local == null : sessionId.equals(local);
  }

  void redo() {
    bypassUndoStack(undoManager.redo());
  }

  void undo() {
    bypassUndoStack(undoManager.undo());
  }

  @SuppressWarnings("unchecked")
  private void applyLocally(RealtimeOperation operation) {
    List<AbstractOperation<?>> ops = (List<AbstractOperation<?>>) operation.operations;
    for (AbstractOperation<?> op : ops) {
      if (op.type == CreateOperation.TYPE) {
        CollaborativeObject obj;
        switch (((CreateOperation) op).subType) {
          case CreateOperation.MAP:
            obj = new CollaborativeMap(model);
            break;
          case CreateOperation.LIST:
            obj = new CollaborativeList(model);
            break;
          case CreateOperation.STRING:
            obj = new CollaborativeString(model);
            break;
          case CreateOperation.INDEX_REFERENCE:
            obj = new IndexReference(model);
            break;
          default:
            throw new RuntimeException("Shouldn't reach here!");
        }
        obj.id = op.id;
        model.objects.put(obj.id, obj);
        model.bytesUsed += op.toString().length();
        model.bytesUsed++;
        continue;
      }
      model.getObject(op.id).consume(operation.userId, operation.sessionId, op);
    }
  }

  /**
   * Applies an op locally and send it bypassing the undo stack. This is necessary with operations
   * popped from the undoManager as they are automatically applied.
   * 
   * @param operations
   */
  private void bypassUndoStack(List<RealtimeOperation> operations) {
    for (RealtimeOperation operation : operations) {
      applyLocally(operation);
      outputSink.consume(operation);
    }
    mayUndoRedoStateChanged();
  }

  private void mayUndoRedoStateChanged() {
    boolean canUndo = undoManager.canUndo();
    boolean canRedo = undoManager.canRedo();
    if (model.canUndo() != canUndo || model.canRedo() != canRedo) {
      model.canUndo = canUndo;
      model.canRedo = canRedo;
      UndoRedoStateChangedEvent event = new UndoRedoStateChangedEvent(model, canUndo, canRedo);
      store.getBus().publish(Bus.LOCAL + Addr.EVENT + EventType.UNDO_REDO_STATE_CHANGED + ":" + id,
          event);
    }
  }

  private void nonUndoableOp(RealtimeOperation op) {
    undoManager.nonUndoableOp(op);
  }

  private void undoableOp(RealtimeOperation op) {
    undoManager.undoableOp(op);
    mayUndoRedoStateChanged();
  }
}
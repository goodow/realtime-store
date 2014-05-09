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
import com.goodow.realtime.json.JsonArray.ListIterator;
import com.goodow.realtime.json.JsonObject;
import com.goodow.realtime.json.JsonObject.MapIterator;
import com.goodow.realtime.operation.OperationComponent;
import com.goodow.realtime.operation.OperationSink;
import com.goodow.realtime.operation.create.CreateComponent;
import com.goodow.realtime.operation.impl.AbstractComponent;
import com.goodow.realtime.operation.impl.CollaborativeOperation;
import com.goodow.realtime.operation.impl.CollaborativeTransformer;
import com.goodow.realtime.operation.undo.UndoManager;
import com.goodow.realtime.operation.undo.UndoManagerFactory;
import com.goodow.realtime.store.channel.Constants.Addr;

public class DocumentBridge implements OperationSink<CollaborativeOperation> {
  public interface OutputSink extends OperationSink<CollaborativeOperation> {
    OutputSink VOID = new OutputSink() {
      @Override
      public void close() {
      }

      @Override
      public void consume(CollaborativeOperation op) {
      }
    };

    void close();
  }

  final Store store;
  final String docId;
  private final Document document;
  private final Model model;
  private UndoManager<CollaborativeOperation> undoManager = UndoManagerFactory.getNoOp();
  OutputSink outputSink = OutputSink.VOID;

  public DocumentBridge(Store store, String docId, JsonArray components,
      final Handler<Error> errorHandler) {
    this.store = store;
    this.docId = docId;
    document = new Document(this);
    model = document.getModel();

    if (errorHandler != null) {
      document.handlerRegs.wrap(store.getBus().registerHandler(
          Addr.EVENT + Addr.DOCUMENT_ERROR + ":" + docId, new Handler<Message<Error>>() {
            @Override
            public void handle(Message<Error> message) {
              errorHandler.handle(message.body());
            }
          }));
    }

    if (components == null || components.length() == 0) {
      model.createRoot();
    } else {
      final CollaborativeTransformer transformer = new CollaborativeTransformer();
      CollaborativeOperation operation =
          transformer.createOperation(Json.createObject().set("op", components));
      applyLocally(operation);
    }
  }

  /*
   * Incoming operations from remote
   */
  @Override
  public void consume(CollaborativeOperation operation) {
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
            .<CollaborativeOperation> getNoOp();
  }

  public JsonObject toJson() {
    return model.getRoot().toJson();
  }

  public JsonArray toSnapshot() {
    final JsonArray createComponents = Json.createArray();
    final JsonArray components = Json.createArray();
    model.objects.forEach(new MapIterator<CollaborativeObject>() {
      @Override
      public void call(String key, CollaborativeObject object) {
        OperationComponent<?>[] initializeComponents = object.toInitialization();
        boolean isCreateOp = true;
        for (OperationComponent<?> component : initializeComponents) {
          if (isCreateOp) {
            createComponents.push(component.toJson());
            isCreateOp = false;
          } else {
            components.push(component.toJson());
          }
        }
      }
    });
    components.forEach(new ListIterator<OperationComponent<?>>() {
      @Override
      public void call(int index, OperationComponent<?> component) {
        createComponents.push(component);
      }
    });
    return components;
  }

  @Override
  public String toString() {
    return toJson().toJsonString();
  }

  void consumeAndSubmit(OperationComponent<?> component) {
    CollaborativeOperation operation =
        new CollaborativeOperation(store.getUserId(), store.getSessionId(), Json.createArray()
            .push(component));
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

  private void applyLocally(final CollaborativeOperation operation) {
    operation.components.forEach(new ListIterator<AbstractComponent<?>>() {
      @Override
      public void call(int index, AbstractComponent<?> component) {
        if (component.type != CreateComponent.TYPE) {
          model.getObject(component.id).consume(operation.userId, operation.sessionId, component);
          return;
        }
        CollaborativeObject obj;
        switch (((CreateComponent) component).subType) {
          case CreateComponent.MAP:
            obj = new CollaborativeMap(model);
            break;
          case CreateComponent.LIST:
            obj = new CollaborativeList(model);
            break;
          case CreateComponent.STRING:
            obj = new CollaborativeString(model);
            break;
          case CreateComponent.INDEX_REFERENCE:
            obj = new IndexReference(model);
            break;
          default:
            throw new RuntimeException("Shouldn't reach here!");
        }
        obj.id = component.id;
        model.objects.set(obj.id, obj);
        model.bytesUsed += component.toString().length();
        model.bytesUsed++;
      }
    });
  }

  /**
   * Applies an op locally and send it bypassing the undo stack. This is necessary with operations
   * popped from the undoManager as they are automatically applied.
   * 
   * @param operations
   */
  private void bypassUndoStack(CollaborativeOperation operation) {
    applyLocally(operation);
    outputSink.consume(operation);
    mayUndoRedoStateChanged();
  }

  private void mayUndoRedoStateChanged() {
    boolean canUndo = undoManager.canUndo();
    boolean canRedo = undoManager.canRedo();
    if (model.canUndo() != canUndo || model.canRedo() != canRedo) {
      model.canUndo = canUndo;
      model.canRedo = canRedo;
      UndoRedoStateChangedEvent event = new UndoRedoStateChangedEvent(model, canUndo, canRedo);
      store.getBus().publish(
          Bus.LOCAL + Addr.EVENT + EventType.UNDO_REDO_STATE_CHANGED + ":" + docId, event);
    }
  }

  private void nonUndoableOp(CollaborativeOperation op) {
    undoManager.nonUndoableOp(op);
  }

  private void undoableOp(CollaborativeOperation op) {
    undoManager.undoableOp(op);
    mayUndoRedoStateChanged();
  }
}
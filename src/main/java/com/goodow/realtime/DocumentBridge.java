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

import com.goodow.realtime.Error.ErrorHandler;
import com.goodow.realtime.channel.operation.OperationSucker;
import com.goodow.realtime.channel.operation.OperationSucker.OutputSink;
import com.goodow.realtime.channel.util.ChannelNative;
import com.goodow.realtime.operation.AbstractOperation;
import com.goodow.realtime.operation.Operation;
import com.goodow.realtime.operation.RealtimeOperation;
import com.goodow.realtime.operation.TransformerImpl;
import com.goodow.realtime.operation.create.CreateOperation;
import com.goodow.realtime.operation.undo.UndoManager;
import com.goodow.realtime.operation.undo.UndoManagerFactory;

import java.util.List;

import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

/*-[
 #import "GDRRealtime+OCNI.h"
 #import "GDRError+OCNI.h"
 ]-*/
public class DocumentBridge implements OperationSucker.Listener {
  private static final OperationSucker.OutputSink VOID = new OperationSucker.OutputSink() {
    @Override
    public void close() {
    }

    @Override
    public void consume(RealtimeOperation op) {
    }
  };

  @SuppressWarnings("cast")
  static void initializeModel(ModelInitializerHandler opt_initializer, Model model) {
    if (opt_initializer instanceof ModelInitializerHandler) {
      opt_initializer.onInitializer(model);
    } else {
      __ocniInitializeModel__(opt_initializer, model);
    }
  }

  @SuppressWarnings("cast")
  static void loadDoucument(final DocumentLoadedHandler onLoaded, final Document document) {
    ChannelNative.get().scheduleDeferred(new Runnable() {
      @Override
      public void run() {
        if (onLoaded instanceof DocumentLoadedHandler) {
          onLoaded.onLoaded(document);
        } else {
          __ocniLoadDoucument__(onLoaded, document);
        }
      }
    });
  }

  //@formatter:off
   private static native void __ocniHandleError__(Object opt_error, Error error) /*-[
     GDRErrorBlock block = (GDRErrorBlock) opt_error;
     return block(error);
   ]-*/ /*-{
   }-*/;
  private static native void __ocniInitializeModel__(Object opt_initializer, Model model) /*-[
     GDRModelInitializerBlock block = (GDRModelInitializerBlock) opt_initializer;
     return block(model);
   ]-*/ /*-{
   }-*/;
   private static native void __ocniLoadDoucument__(Object onLoaded, Document document) /*-[
     GDRDocumentLoadedBlock block = (GDRDocumentLoadedBlock) onLoaded;
     return block(document);
   ]-*/ /*-{
   }-*/;

  // @formatter:on
  @SuppressWarnings("cast")
  private static void handlerError(ErrorHandler opt_error, Error error) {
    if (opt_error instanceof ErrorHandler) {
      opt_error.handleError(error);
    } else {
      __ocniHandleError__(opt_error, error);
    }
  }

  String sessionId;
  OutputSink outputSink = VOID;
  private Document document;
  private Model model;
  private UndoManager<RealtimeOperation> undoManager = UndoManagerFactory.getNoOp();

  public DocumentBridge(JsonArray snapshot) {
    createSnapshot(snapshot);
  }

  DocumentBridge() {
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

  @Override
  public void onCollaboratorChanged(boolean isJoined, JsonObject json) {
    document.onCollaboratorChanged(isJoined, json);
  }

  @Override
  public void onSaveStateChanged(boolean isSaving, boolean isPending) {
    DocumentSaveStateChangedEvent event =
        new DocumentSaveStateChangedEvent(document, isSaving, isPending);
    document
        .scheduleEvent(Document.EVENT_HANDLER_KEY, EventType.DOCUMENT_SAVE_STATE_CHANGED, event);
  }

  public void setOutputSink(OutputSink outputSink) {
    this.outputSink = outputSink;
  }

  @Override
  public String toString() {
    StringBuilder sb1 = new StringBuilder();
    StringBuilder sb2 = new StringBuilder();
    boolean isFirst = true;
    for (CollaborativeObject object : model.objects.values()) {
      Operation<?>[] initializeOp = object.toInitialization();
      boolean isCreate = true;
      for (Operation<?> op : initializeOp) {
        StringBuilder sb;
        if (isCreate) {
          sb = sb1;
          isCreate = false;
        } else {
          sb = sb2;
        }
        if (!isFirst) {
          sb.append(",");
        } else {
          isFirst = false;
        }
        sb.append(op.toString());
      }
    }
    return "[" + sb1.toString() + sb2.toString() + "]";
  }

  void consumeAndSubmit(Operation<?> op) {
    RealtimeOperation operation = new RealtimeOperation(Realtime.USERID, sessionId, op);
    applyLocally(operation);
    undoManager.checkpoint();
    undoableOp(operation);
    outputSink.consume(operation);
  }

  void createSnapshot(JsonValue serialized) {
    TransformerImpl<AbstractOperation<?>> transformer = new TransformerImpl<AbstractOperation<?>>();
    document = new Document(this, null, null);
    model = document.getModel();
    JsonArray snapshot = (JsonArray) serialized;
    if (snapshot == null || snapshot.length() == 0) {
      model.createRoot();
    } else {
      for (int i = 0, len = snapshot.length(); i < len; i++) {
        JsonArray serializedOp = snapshot.getArray(i);
        Operation<?> op = transformer.createOperation(serializedOp);
        RealtimeOperation operation = new RealtimeOperation(null, null, op);
        applyLocally(operation);
      }
    }
  }

  boolean isLocalSession(String sessionId) {
    return sessionId != null && sessionId.equals(this.sessionId);
  }

  void redo() {
    bypassUndoStack(undoManager.redo());
  }

  void setUndoEnabled(boolean undoEnabled) {
    undoManager =
        undoEnabled ? UndoManagerFactory.createUndoManager() : UndoManagerFactory
            .<RealtimeOperation> getNoOp();
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
   * Applies an op locally and send it bypassing the undo stack. This is neccessary with operations
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
      document.scheduleEvent(Model.EVENT_HANDLER_KEY, EventType.UNDO_REDO_STATE_CHANGED, event);
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
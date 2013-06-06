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
import com.goodow.realtime.channel.operation.RealtimeOperationSucker;
import com.goodow.realtime.operation.CreateOperation;
import com.goodow.realtime.operation.Operation;
import com.goodow.realtime.operation.OperationSink;
import com.goodow.realtime.operation.RealtimeOperation;
import com.goodow.realtime.operation.RealtimeTransformer;
import com.goodow.realtime.operation.basic.NoOp;

import elemental.json.JsonArray;
import elemental.json.JsonValue;

/*-[
 #import "GDRRealtime+OCNI.h"
 #import "GDRError+OCNI.h"
 ]-*/
public class DocumentBridge implements RealtimeOperationSucker.Listener {
  private static final OperationSink<RealtimeOperation<?>> VOID =
      new OperationSink<RealtimeOperation<?>>() {
        @Override
        public void consume(RealtimeOperation<?> op) {
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
  static void loadDoucument(final DocumentLoadedHandler onLoaded, Document document) {
    if (onLoaded instanceof DocumentLoadedHandler) {
      onLoaded.onLoaded(document);
    } else {
      __ocniLoadDoucument__(onLoaded, document);
    }
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
  OperationSink<RealtimeOperation<?>> outputSink = VOID;
  private Document document;
  private Model model;

  public DocumentBridge(JsonArray snapshot) {
    createSnapshot(snapshot);
  }

  DocumentBridge() {
  }

  @Override
  public void consume(RealtimeOperation<?> operation) {
    int type = operation.getType();
    if (type == NoOp.TYPE) {
      return;
    }
    if (type == CreateOperation.TYPE) {
      CreateOperation op = (CreateOperation) operation.<Void> getOp();
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

  public Document getDocument() {
    return document;
  }

  @Override
  public void onSaveStateChanged(boolean isSaving, boolean isPending) {
    DocumentSaveStateChangedEvent event =
        new DocumentSaveStateChangedEvent(document, isSaving, isPending);
    getDocument().scheduleEvent(Document.EVENT_HANDLER_KEY, EventType.DOCUMENT_SAVE_STATE_CHANGED,
        event);
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
    RealtimeOperation<?> operation = new RealtimeOperation(op, Realtime.USERID, sessionId);
    consume(operation);
    outputSink.consume(operation);
  }

  void createSnapshot(JsonValue serialized) {
    RealtimeTransformer transformer = new RealtimeTransformer();
    document = new Document(this, null, null);
    model = document.getModel();
    JsonArray snapshot = (JsonArray) serialized;
    if (snapshot == null || snapshot.length() == 0) {
      model.createRoot();
    } else {
      for (int i = 0, len = snapshot.length(); i < len; i++) {
        JsonArray serializedOp = snapshot.getArray(i);
        Operation<?> op = transformer.createOp(serializedOp);
        @SuppressWarnings({"rawtypes", "unchecked"})
        RealtimeOperation<?> operation = new RealtimeOperation(op, null, null);
        consume(operation);
      }
    }
  }

  boolean isLocalSession(String sessionId) {
    return sessionId != null && sessionId.equals(this.sessionId);
  }
}

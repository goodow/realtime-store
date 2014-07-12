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
package com.goodow.realtime.store.channel;

import com.goodow.realtime.channel.Bus;
import com.goodow.realtime.core.Handler;
import com.goodow.realtime.core.Platform;
import com.goodow.realtime.json.Json;
import com.goodow.realtime.operation.Transformer;
import com.goodow.realtime.operation.impl.CollaborativeOperation;
import com.goodow.realtime.operation.impl.CollaborativeTransformer;
import com.goodow.realtime.store.ErrorType;
import com.goodow.realtime.store.EventType;
import com.goodow.realtime.store.impl.DocumentBridge;
import com.goodow.realtime.store.impl.DocumentBridge.OutputSink;
import com.goodow.realtime.store.impl.DocumentSaveStateChangedEventImpl;
import com.goodow.realtime.store.impl.ErrorImpl;

import java.util.logging.Level;
import java.util.logging.Logger;

public class OperationSucker implements OperationChannel.Listener<CollaborativeOperation> {
  private static final Logger logger = Logger.getLogger(OperationSucker.class.getName());

  private final String id;
  private final OperationChannel<CollaborativeOperation> channel;
  private final Transformer<CollaborativeOperation> transformer;

  private final Bus bus;
  private DocumentBridge bridge;

  public OperationSucker(Bus bus, final String id) {
    this.bus = bus;
    this.id = id;
    transformer = new CollaborativeTransformer();
    channel = new OperationChannel<CollaborativeOperation>(id, transformer, bus, this);
  }

  public void load(final DocumentBridge bridge, double version) {
    this.bridge = bridge;
    bridge.setOutputSink(new OutputSink() {
      @Override
      public void close() {
        channel.disconnect();
      }

      @Override
      public void consume(CollaborativeOperation op) {
        channel.send(op);
      }
    });

    channel.connect(version);
  }

  @Override
  public void onAck(CollaborativeOperation serverHistoryOp, boolean clean) {
  }

  @Override
  public void onError(Throwable e) {
    logger.log(Level.WARNING, "Channel error occurs", e);
    bus.publishLocal(Constants.Topic.STORE + "/" + id + "/" + Constants.Topic.DOCUMENT_ERROR,
                     new ErrorImpl(ErrorType.SERVER_ERROR, "Channel error occurs", true));
  }

  @Override
  public void onRemoteOp(CollaborativeOperation serverHistoryOp) {
    nextTick();
  }

  @Override
  public void onSaveStateChanged(boolean isSaving, boolean isPending) {
    bus.publishLocal(Constants.Topic.STORE + "/" + id + "/" + EventType.DOCUMENT_SAVE_STATE_CHANGED,
        new DocumentSaveStateChangedEventImpl(bridge.getDocument(),
            Json.createObject().set("isSaving", isSaving).set("isPending", isPending)));
  }

  private void nextTick() {
    Platform.scheduler().scheduleDeferred(new Handler<Void>() {
      @Override
      public void handle(Void event) {
        if (channel.peek() != null) {
          bridge.consume(channel.receive());
        }
        if (channel.peek() != null) {
          nextTick();
        }
      }
    });
  }
}
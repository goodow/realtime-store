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
import com.goodow.realtime.channel.Message;
import com.goodow.realtime.channel.impl.ReliableBus;
import com.goodow.realtime.core.Handler;
import com.goodow.realtime.core.HandlerRegistration;
import com.goodow.realtime.json.JsonObject;
import com.goodow.realtime.operation.RealtimeOperation;
import com.goodow.realtime.operation.Transformer;
import com.goodow.realtime.operation.TransformerImpl;
import com.goodow.realtime.store.Collaborator;
import com.goodow.realtime.store.DocumentBridge;
import com.goodow.realtime.store.DocumentBridge.OutputSink;
import com.goodow.realtime.store.DocumentSaveStateChangedEvent;
import com.goodow.realtime.store.Error;
import com.goodow.realtime.store.ErrorType;
import com.goodow.realtime.store.EventType;
import com.goodow.realtime.store.channel.Constants.Addr;
import com.goodow.realtime.store.channel.Constants.Key;

import java.util.logging.Level;
import java.util.logging.Logger;

public class OperationSucker implements OperationChannel.Listener<RealtimeOperation> {
  private static final Logger logger = Logger.getLogger(OperationSucker.class.getName());

  private final String id;
  private final OperationChannel<RealtimeOperation> channel;
  private final Transformer<RealtimeOperation> transformer;

  private final Bus bus;
  private HandlerRegistration collaboratorChangedReg;
  private DocumentBridge bridge;

  public OperationSucker(ReliableBus bus, final String id) {
    this.bus = bus;
    this.id = id;
    transformer = new TransformerImpl<RealtimeOperation>();
    channel = new OperationChannel<RealtimeOperation>(id, transformer, bus, this);
  }

  public void load(final DocumentBridge bridge, JsonObject snapshot) {
    this.bridge = bridge;
    bridge.setOutputSink(new OutputSink() {
      @Override
      public void close() {
        collaboratorChangedReg.unregisterHandler();
        channel.disconnect();
      }

      @Override
      public void consume(RealtimeOperation op) {
        channel.send(op);
      }
    });
    collaboratorChangedReg =
        bus.registerHandler(Addr.COLLABORATOR + id, new Handler<Message<JsonObject>>() {
          @Override
          public void handle(Message<JsonObject> message) {
            JsonObject body = message.body();
            Collaborator collaborator =
                new Collaborator(body.getString(Key.USER_ID), body.getString(Key.SESSION_ID), body
                    .getString(Key.DISPLAY_NAME), body.getString(Key.COLOR), body
                    .getBoolean(Key.IS_ME), body.getBoolean(Key.IS_ANONYMOUS), body
                    .getString(Key.PHOTO_URL));
            boolean isJoined = !body.has(Key.IS_JOINED) || body.getBoolean(Key.IS_JOINED);
            bridge.onCollaboratorChanged(isJoined, collaborator);
          }
        });

    channel.connect((int) snapshot.getNumber(Key.REVISION), snapshot.getString(Key.SESSION_ID));
  }

  @Override
  public void onAck(RealtimeOperation serverHistoryOp, boolean clean) {
  }

  @Override
  public void onError(Throwable e) {
    logger.log(Level.WARNING, "Channel error occurs", e);
    bus.publish(Bus.LOCAL + Addr.EVENT + Addr.DOCUMENT_ERROR + ":" + id, new Error(
        ErrorType.SERVER_ERROR, "Channel error occurs", true));
  }

  @Override
  public void onRemoteOp(RealtimeOperation serverHistoryOp) {
    while (channel.peek() != null) {
      bridge.consume(channel.receive());
    }
  }

  @Override
  public void onSaveStateChanged(boolean isSaving, boolean isPending) {
    bus.publish(Bus.LOCAL + Addr.EVENT + EventType.DOCUMENT_SAVE_STATE_CHANGED + ":" + id,
        new DocumentSaveStateChangedEvent(bridge.getDocument(), isSaving, isPending));
  }
}
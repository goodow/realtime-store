/*
 * Copyright 2013 Goodow.com
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
import com.goodow.realtime.channel.impl.ReliableSubscribeBus;
import com.goodow.realtime.core.Handler;
import com.goodow.realtime.core.Platform;
import com.goodow.realtime.core.Registration;
import com.goodow.realtime.json.Json;
import com.goodow.realtime.json.JsonObject;
import com.goodow.realtime.operation.Operation;
import com.goodow.realtime.operation.Transformer;
import com.goodow.realtime.store.channel.Constants.Key;

import java.util.EnumSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service that handles transportation and transforming of client and server operations.
 * 
 * @param <O> Mutation type.
 */
public class OperationChannel<O extends Operation<?>> {

  /**
   * Notifies when operations and acknowledgments come in. The values passed to the methods can be
   * used to reconstruct the exact server history.
   * 
   * <p>
   * WARNING: The server history ops cannot be applied to local client state, because they have not
   * been transformed properly. Server history ops are for other uses. To get the server ops to
   * apply locally, use {@link OperationChannel#receive()}
   */
  public interface Listener<O> {
    /**
     * A local op is acknowledged as applied at this point in the server history op stream.
     * 
     * @param serverHistoryOp the operation as it appears in the server history, not necessarily as
     *          it was when passed into the channel.
     * @param clean true if the channel is now clean.
     */
    void onAck(O serverHistoryOp, boolean clean);

    /**
     * Called when some unrecoverable problem occurs.
     */
    void onError(Throwable e);

    /**
     * A remote op has been received. Do not use the parameter to apply to local state, instead use
     * {@link OperationChannel#receive()}.
     * 
     * @param serverHistoryOp the operation as it appears in the server history (do not apply this
     *          to local state).
     */
    void onRemoteOp(O serverHistoryOp);

    void onSaveStateChanged(boolean isSaving, boolean isPending);
  }

  enum State {
    /**
     * Cannot send ops in this state. All states can transition here if either explicitly requested,
     * or if there is a permanent failure.
     */
    UNINITIALISED,

    /**
     * No unacked ops. There may be queued ops though.
     */
    ACKED,

    /**
     * Waiting for an ack for sent ops. Will transition back to ALL_ACKED if successful, or to
     * DELAY_RESYNC if there is a retryable failure.
     */
    WAITING_ACK;

    private EnumSet<State> to;
    static {
      UNINITIALISED.transitionsTo(ACKED);
      ACKED.transitionsTo(WAITING_ACK);
      WAITING_ACK.transitionsTo(ACKED);
    }

    private void transitionsTo(State... validTransitionStates) {
      // Also, everything may transition to UNINITIALISED
      to = EnumSet.of(UNINITIALISED, validTransitionStates);
    }
  }

  private boolean isMaybeSendTaskScheduled;
  private final Handler<Void> maybeSendTask = new Handler<Void>() {
    @Override
    public void handle(Void ignore) {
      isMaybeSendTaskScheduled = false;
      maybeSend();
    }
  };

  private static final Logger logger = Logger.getLogger(OperationChannel.class.getName());
  private final Listener<O> listener;

  // State variables
  private State state = State.UNINITIALISED;
  private final TransformQueue<O> queue;
  private final String id;
  private final Bus bus;
  private Registration handlerRegistration;
  private final Transformer<O> transformer;

  public OperationChannel(String id, Transformer<O> transformer, Bus bus, Listener<O> listener) {
    this.id = id;
    this.transformer = transformer;
    this.bus = bus;
    this.queue = new TransformQueue<O>(transformer);
    this.listener = listener;
  }

  public void connect(double version) {
    assert !isConnected() : "Already connected";
    assert version >= 0 : "Invalid version, " + version;
    String addr = Constants.Topic.STORE + "/" + id + Constants.Topic.WATCH;
    if (bus instanceof ReliableSubscribeBus) {
      ((ReliableSubscribeBus) bus).synchronizeSequenceNumber(addr, version - 1);
    }
    handlerRegistration = bus.subscribe(addr, new Handler<Message<JsonObject>>() {
      @Override
      public void handle(Message<JsonObject> message) {
        if (!isConnected()) {
          return;
        }
        JsonObject body = message.body();
        O op = transformer.createOperation(body);
        double appliedAt = body.getNumber(Key.VERSION);
        if (bus.getSessionId().equals(body.getString(Key.SESSION_ID))) {
          onAckOwnOperation(appliedAt, op);
        } else {
          onIncomingOperation(appliedAt, op);
        }
      }
    });

    queue.init(version);
    setState(State.ACKED);
  }

  public void disconnect() {
    if(isConnected()) {
      handlerRegistration.unregister();
      handlerRegistration = null;
      setState(State.UNINITIALISED);
    }
  }

  public O peek() {
    checkConnected();
    return queue.hasServerOp() ? queue.peekServerOp() : null;
  }

  public O receive() {
    checkConnected();
    return queue.hasServerOp() ? queue.removeServerOp() : null;
  }

  public void send(O operation) {
    checkConnected();
    queue.clientOp(operation);
    // Defer the send to allow multiple ops to batch up, and to avoid waiting for the browser's
    // network stack in case we are in a time critical piece of code. Note, we could even go further
    // and avoid doing the transform inside the queue.
    if (!isMaybeSendTaskScheduled && queue.unackedClientOp() == null) {
      assert state == State.ACKED;
      isMaybeSendTaskScheduled = true;
      Platform.scheduler().scheduleDeferred(maybeSendTask);
    }
  }

  public double version() {
    checkConnected();
    return queue.version();
  }

  private void acked() {
    setState(State.ACKED);
    if (!isMaybeSendTaskScheduled && queue.hasQueuedClientOps()) {
      isMaybeSendTaskScheduled = true;
      Platform.scheduler().scheduleDeferred(maybeSendTask);
    }
  }

  private void checkConnected() {
    assert isConnected() : "Not connected";
  }

  private void checkState(State newState) {
    switch (newState) {
      case UNINITIALISED:
        break;
      case ACKED:
        assert queue.version() >= 0;
        assert queue.unackedClientOp() == null;
        break;
      case WAITING_ACK:
        assert !isMaybeSendTaskScheduled;
        break;
      default:
        throw new AssertionError("State " + state + " not implemented");
    }
  }

  /**
   * @return true if there are no queued or unacknowledged ops
   */
  private boolean isClean() {
    checkConnected();
    boolean ret = !queue.hasQueuedClientOps() && queue.unackedClientOp() == null;
    // isClean() implies ALL_ACKED
    assert !ret || state == State.ACKED;
    return ret;
  }

  private boolean isConnected() {
    return state != State.UNINITIALISED;
  }

  private void maybeEagerlyHandleAck(double appliedAt) {
    final O ownOp = queue.ackOpIfVersionMatches(appliedAt);
    if (ownOp == null) {
      return;
    }

    logger.log(Level.INFO, "Eagerly acked @" + appliedAt);

    // Special optimization: there were no concurrent ops on the server,
    // so we don't need to wait for them or even our own ops on the channel.
    // We just throw back our own ops to our listeners as if we had
    // received them from the server (we expect they should exactly
    // match the server history we will shortly receive on the channel).

    acked();
    listener.onAck(ownOp, isClean());
  }

  private void maybeSend() {
    if (queue.unackedClientOp() != null) {
      logger.log(Level.INFO, state + ", Has 1 unacked...");
      return;
    }

    if (queue.hasQueuedClientOps()) {
      queue.pushQueuedOpsToUnacked();
      sendUnackedOps();
    }
  }

  private void onAckOwnOperation(double appliedAt, O ackedOp) {
    boolean alreadyAckedByXhr = queue.expectedAck(appliedAt);
    if (alreadyAckedByXhr) {
      // Nothing to do, just receiving expected operations that we've already handled by the
      // optimization in maybeEagerlyHandleAck()
      return;
    }

    queue.ackClientOp(appliedAt);
    logger.log(Level.INFO, "Ack @" + appliedAt);

    // If we have more ops to send and no unacknowledged ops, then schedule a send.
    acked();
    listener.onAck(ackedOp, isClean());
  }

  private void onIncomingOperation(double appliedAt, O operation) {
    logger.log(Level.FINE, "Incoming applied @" + appliedAt + " " + state);
    queue.serverOp(appliedAt, operation);
    listener.onRemoteOp(operation);
  }

  /**
   * Sends unacknowledged ops and transitions to the WAITING_ACK state
   */
  private void sendUnackedOps() {
    O unackedClientOp = queue.unackedClientOp();
    assert unackedClientOp != null;
    logger.log(Level.FINE, "Sending " + unackedClientOp + " @" + queue.version());

    JsonObject delta =
        Json.createObject().set("action", "post").set(Key.ID, id).set(Key.OP_DATA,
            ((JsonObject) unackedClientOp.toJson()).set(Key.VERSION, queue.version()));
    bus.send(Constants.Topic.STORE, delta, new Handler<Message<JsonObject>>() {
      @Override
      public void handle(Message<JsonObject> message) {
        if (!isConnected()) {
          return;
        }
        maybeEagerlyHandleAck(message.body().getNumber(Key.VERSION));
      }
    });
    setState(State.WAITING_ACK);
  }

  /**
   * Brings the state variable to the given value.
   * 
   * <p>
   * Verifies that other member variables are are in the correct state.
   */
  private void setState(State newState) {
    // Check transitioning from valid old state
    State oldState = state;
    assert oldState.to.contains(newState) : "Invalid state transition " + oldState + " -> "
        + newState;

    // Check consistency of variables with new state
    checkState(newState);
    state = newState;

    switch (newState) {
      case ACKED:
        if (oldState != State.UNINITIALISED) {
          listener.onSaveStateChanged(false, queue.hasQueuedClientOps());
        }
        break;
      case WAITING_ACK:
        listener.onSaveStateChanged(true, queue.hasQueuedClientOps());
        break;
      default:
        break;
    }
  }
}
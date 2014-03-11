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

import com.goodow.realtime.channel.Message;
import com.goodow.realtime.channel.impl.ReliableBus;
import com.goodow.realtime.core.Handler;
import com.goodow.realtime.core.HandlerRegistration;
import com.goodow.realtime.core.Platform;
import com.goodow.realtime.json.Json;
import com.goodow.realtime.json.JsonObject;
import com.goodow.realtime.operation.Transformer;
import com.goodow.realtime.store.channel.Constants.Addr;
import com.goodow.realtime.store.channel.Constants.Key;

import java.util.EnumSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service that handles transportation and transforming of client and server operations.
 * 
 * @param <M> Mutation type.
 */
public class OperationChannel<M> {

  /**
   * Notifies when operations and acknowledgments come in. The values passed to the methods can be
   * used to reconstruct the exact server history.
   * 
   * <p>
   * WARNING: The server history ops cannot be applied to local client state, because they have not
   * been transformed properly. Server history ops are for other uses. To get the server ops to
   * apply locally, use {@link OperationChannel#receive()}
   */
  public interface Listener<M> {
    /**
     * A local op is acknowledged as applied at this point in the server history op stream.
     * 
     * @param serverHistoryOp the operation as it appears in the server history, not necessarily as
     *          it was when passed into the channel.
     * @param clean true if the channel is now clean.
     */
    void onAck(M serverHistoryOp, boolean clean);

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
    void onRemoteOp(M serverHistoryOp);

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
    ALL_ACKED,

    /**
     * Waiting for an ack for sent ops. Will transition back to ALL_ACKED if successful, or to
     * DELAY_RESYNC if there is a retryable failure.
     */
    WAITING_ACK,

    /**
     * Waiting to attempt a resync/reconnect (delay can be large due to exponential backoff). Will
     * transition to WAITING_SYNC when the delay is up and we send off the version request, or to
     * ALL_ACKED if all ops get acked while waiting.
     */
    DELAY_RESYNC,

    /**
     * Waiting for our version sync. If it turns out that all ops get acked down the channel in the
     * meantime, we can return to ALL_ACKED. Otherwise, we can resend and go to WAITING_ACK. If
     * there is a retryable failure, we will go to DELAY_RESYNC
     */
    WAITING_SYNC;

    private EnumSet<State> to;
    static {
      UNINITIALISED.transitionsTo(ALL_ACKED);
      ALL_ACKED.transitionsTo(WAITING_ACK);
      WAITING_ACK.transitionsTo(ALL_ACKED, DELAY_RESYNC);
      DELAY_RESYNC.transitionsTo(ALL_ACKED, WAITING_SYNC);
      WAITING_SYNC.transitionsTo(ALL_ACKED, WAITING_ACK, DELAY_RESYNC);
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
  private final Listener<M> listener;

  // State variables
  private State state = State.UNINITIALISED;
  private final TransformQueue<M> queue;
  private String sessionId;
  private int retryVersion = -1;
  private final String id;
  private final ReliableBus bus;
  private HandlerRegistration handlerRegistration;
  private final Transformer<M> transformer;

  public OperationChannel(String id, Transformer<M> transformer, ReliableBus bus,
      Listener<M> listener) {
    this.id = id;
    this.transformer = transformer;
    this.bus = bus;
    this.queue = new TransformQueue<M>(transformer);
    this.listener = listener;
  }

  public void connect(int revision, final String sessionId) {
    assert !isConnected() : "Already connected";
    assert sessionId != null : "Null sessionId";
    assert revision >= 0 : "Invalid revision, " + revision;
    this.sessionId = sessionId;
    String addr = Addr.DELTA + ":" + id;
    bus.synchronizeSequenceNumber(addr, revision);
    handlerRegistration = bus.registerHandler(addr, new Handler<Message<JsonObject>>() {
      @Override
      public void handle(Message<JsonObject> message) {
        if (!isConnected()) {
          return;
        }
        JsonObject body = message.body();
        String sid = body.getString(Key.SESSION_ID);
        M op =
            transformer
                .createOperation(body.getString(Key.USER_ID), sid, body.getArray(Key.DELTAS));
        double resultingRevision = body.getNumber(Key.REVISION);
        if (sessionId.equals(sid)) {
          onAckOwnOperation((int) resultingRevision, op);
        } else {
          onIncomingOperation((int) resultingRevision, op);
        }
        maybeSynced();
      }
    });

    queue.init(revision);
    setState(State.ALL_ACKED);
  }

  public void disconnect() {
    checkConnected();
    handlerRegistration.unregisterHandler();
    sessionId = null;
    setState(State.UNINITIALISED);
  }

  /**
   * @return true if there are no queued or unacknowledged ops
   */
  public boolean isClean() {
    checkConnected();
    boolean ret = !queue.hasQueuedClientOps() && !queue.hasUnacknowledgedClientOps();
    // isClean() implies ALL_ACKED
    assert !ret || state == State.ALL_ACKED;
    return ret;
  }

  public boolean isConnected() {
    // UNINITIALISED implies sessionId == null.
    assert state != State.UNINITIALISED || sessionId == null;
    return sessionId != null;
  }

  public M peek() {
    checkConnected();
    return queue.hasServerOp() ? queue.peekServerOp() : null;
  }

  public M receive() {
    checkConnected();
    return queue.hasServerOp() ? queue.removeServerOp() : null;
  }

  public int revision() {
    checkConnected();
    return queue.revision();
  }

  public void send(M operation) {
    checkConnected();
    queue.clientOp(operation);
    // Defer the send to allow multiple ops to batch up, and
    // to avoid waiting for the browser's network stack in case
    // we are in a time critical piece of code. Note, we could even
    // go further and avoid doing the transform inside the queue.
    if (!isMaybeSendTaskScheduled && !queue.hasUnacknowledgedClientOps()) {
      assert state == State.ALL_ACKED;
      isMaybeSendTaskScheduled = true;
      Platform.scheduler().scheduleDeferred(maybeSendTask);
    }
  }

  State getState() {
    return state;
  }

  private void allAcked() {

    // This also counts as an early sync
    synced();

    setState(State.ALL_ACKED);
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
        assert sessionId == null;
        break;
      case ALL_ACKED:
        assert sessionId != null;
        assert queue.revision() >= 0;
        assert retryVersion == -1;
        assert !queue.hasUnacknowledgedClientOps();
        break;
      case WAITING_ACK:
        assert retryVersion == -1;
        assert !isMaybeSendTaskScheduled;
        break;
      case DELAY_RESYNC:
        assert retryVersion == -1;
        assert !isMaybeSendTaskScheduled;
        break;
      case WAITING_SYNC:
        assert !isMaybeSendTaskScheduled;
        break;
      default:
        throw new AssertionError("State " + state + " not implemented");
    }
  }

  private void maybeEagerlyHandleAck(int appliedRevision) {
    List<M> ownOps = queue.ackOpsIfVersionMatches(appliedRevision);
    if (ownOps == null) {
      return;
    }

    logger.log(Level.INFO, "Eagerly acked @", appliedRevision);

    // Special optimization: there were no concurrent ops on the server,
    // so we don't need to wait for them or even our own ops on the channel.
    // We just throw back our own ops to our listeners as if we had
    // received them from the server (we expect they should exactly
    // match the server history we will shortly receive on the channel).

    assert !queue.hasUnacknowledgedClientOps();
    allAcked();

    boolean isClean = isClean();
    for (int i = 0; i < ownOps.size(); i++) {
      boolean isLast = i == ownOps.size() - 1;
      listener.onAck(ownOps.get(i), isClean && isLast);
    }
  }

  private void maybeSend() {
    if (queue.hasUnacknowledgedClientOps()) {
      logger.log(Level.INFO, state + ", Has " + queue.unackedClientOpsCount() + " unacked...");
      return;
    }

    if (queue.hasQueuedClientOps()) {
      queue.pushQueuedOpsToUnacked();
      if (queue.hasUnacknowledgedClientOps()) {
        sendUnackedOps();
      }
    }
  }

  private void maybeSynced() {
    if (state == State.WAITING_SYNC && retryVersion != -1 && queue.revision() >= retryVersion) {

      // Our ping has returned.
      synced();

      if (queue.hasUnacknowledgedClientOps()) {
        // We've synced and didn't see our unacked ops, so they never made it (we
        // are not handling the case of ops that hang around on the network and
        // make it after a very long time, i.e. after a sync round trip. This
        // scenario most likely extremely rare).

        // Send the unacked ops again.
        sendUnackedOps();
      }
    }
  }

  private void onAckOwnOperation(int resultingRevision, M ackedOp) {
    boolean alreadyAckedByXhr = queue.expectedAck(resultingRevision);
    if (alreadyAckedByXhr) {
      // Nothing to do, just receiving expected operations that we've
      // already handled by the optimization in maybeEagerlyHandleAck()
      return;
    }

    boolean allAcked = queue.ackClientOp(resultingRevision);
    logger.log(Level.INFO, "Ack @" + resultingRevision + ", " + queue.unackedClientOpsCount()
        + " ops remaining");

    // If we have more ops to send and no unacknowledged ops,
    // then schedule a send.
    if (allAcked) {
      allAcked();
    }

    listener.onAck(ackedOp, isClean());
  }

  private void onIncomingOperation(int revision, M operation) {
    logger.log(Level.INFO, "Incoming " + revision + " " + state);
    queue.serverOp(revision, operation);
    listener.onRemoteOp(operation);
  }

  /**
   * Sends unacknowledged ops and transitions to the WAITING_ACK state
   */
  private void sendUnackedOps() {
    List<M> ops = queue.unackedClientOps();
    assert ops.size() > 0;
    logger.log(Level.INFO, "Sending " + ops.size() + " ops @" + queue.revision());

    JsonObject delta =
        Json.createObject().set(Key.ID, id).set(Key.SESSION_ID, sessionId).set(Key.REVISION,
            queue.revision()).set(Key.DELTAS, Json.parse(ops.toString()));
    bus.send(Addr.DELTA, delta, new Handler<Message<JsonObject>>() {
      @Override
      public void handle(Message<JsonObject> message) {
        if (!isConnected()) {
          return;
        }
        maybeEagerlyHandleAck((int) message.body().getNumber(Key.REVISION));
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
      case ALL_ACKED:
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

  /**
   * We have reached a state where we are confident we know whether any unacked ops made it to the
   * server.
   */
  private void synced() {
    logger.log(Level.INFO, "synced @" + queue.revision());
    retryVersion = -1;
  }
}

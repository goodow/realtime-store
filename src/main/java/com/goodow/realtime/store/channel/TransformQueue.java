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

import com.goodow.realtime.operation.Transformer;
import com.goodow.realtime.operation.util.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Simple implementation of main concurrency control logic, independent of transport concerns.
 * 
 * <p>
 * For efficiency, client ops are also compacted before transforming and before sending.
 */
public class TransformQueue<M> {

  private final Transformer<M> transformer;

  private int revision = -1;

  int expectedAckedClientOps = 0;
  List<M> serverOps = new LinkedList<M>();
  List<M> queuedClientOps = new LinkedList<M>();
  List<M> unackedClientOps = Collections.emptyList();
  boolean newClientOpSinceTransform = false;

  public TransformQueue(Transformer<M> transformer) {
    this.transformer = transformer;
  }

  /**
   * @param resultingRevision
   * @return true if all unacked ops are now acked
   */
  public boolean ackClientOp(int resultingRevision) {
    checkRevision(resultingRevision);

    assert expectedAckedClientOps == 0 : "must call expectedAck, there are "
        + expectedAckedClientOps + " expectedAckedClientOps";
    assert !unackedClientOps.isEmpty() : this + ": unackedClientOps is empty; resultingRevision="
        + resultingRevision;

    this.revision = resultingRevision;

    unackedClientOps.remove(0);

    return unackedClientOps.isEmpty();
  }

  public List<M> ackOpsIfVersionMatches(int newRevision) {
    if (newRevision == revision + unackedClientOps.size()) {
      List<M> expectedAckingClientOps = unackedClientOps;
      expectedAckedClientOps += expectedAckingClientOps.size();
      unackedClientOps = new LinkedList<M>();
      revision = newRevision;
      return expectedAckingClientOps;
    }

    return null;
  }

  public void clientOp(M clientOp) {
    transformer.transform(queuedClientOps, clientOp, serverOps, 0, true);
    newClientOpSinceTransform = true;
  }

  public boolean expectedAck(int resultingRevision) {
    if (expectedAckedClientOps == 0) {
      return false;
    }

    assert resultingRevision == revision - expectedAckedClientOps + 1 : "bad rev "
        + resultingRevision + ", current rev " + revision + ", expected remaining "
        + expectedAckedClientOps;

    expectedAckedClientOps--;

    return true;
  }

  public boolean hasQueuedClientOps() {
    return !queuedClientOps.isEmpty();
  }

  public boolean hasServerOp() {
    return !serverOps.isEmpty();
  }

  public boolean hasUnacknowledgedClientOps() {
    return !unackedClientOps.isEmpty();
  }

  public void init(int revision) {
    assert this.revision == -1 : "Already at a revision (" + this.revision + "), can't init at "
        + revision + ")";
    assert revision >= 0 : "Initial revision must be >= 0, not " + revision;
    this.revision = revision;
  }

  public M peekServerOp() {
    assert hasServerOp() : "No server ops";
    return serverOps.get(0);
  }

  /**
   * Pushes the queued client ops into the unacked ops, clearing the queued ops.
   * 
   * @return see {@link #unackedClientOps()}
   */
  public List<M> pushQueuedOpsToUnacked() {
    assert unackedClientOps.isEmpty() : "Queue contains unacknowledged operations: "
        + unackedClientOps;

    unackedClientOps = new LinkedList<M>(transformer.compact(queuedClientOps));
    queuedClientOps = new LinkedList<M>();

    return unackedClientOps();
  }

  public M removeServerOp() {
    assert hasServerOp() : "No server ops";
    return serverOps.remove(0);
  }

  public int revision() {
    return revision;
  }

  public void serverOp(int resultingRevision, M serverOp) {
    checkRevision(resultingRevision);

    assert expectedAckedClientOps == 0 : "server op arrived @" + resultingRevision
        + " while expecting " + expectedAckedClientOps + " client ops";

    this.revision = resultingRevision;

    List<M> transformedServerOps = new ArrayList<M>();
    transformer.transform(transformedServerOps, serverOp, unackedClientOps, 0, false);

    if (!queuedClientOps.isEmpty()) {
      if (newClientOpSinceTransform) {
        queuedClientOps = transformer.compact(queuedClientOps);
        newClientOpSinceTransform = false;
      }
      Pair<List<M>, List<M>> pair = transformer.transform(queuedClientOps, transformedServerOps);
      queuedClientOps = pair.first;
      transformedServerOps = pair.second;
    }

    serverOps.addAll(transformedServerOps);
  }

  @Override
  public String toString() {
    return "TQ{ " + revision + "\n  s:" + serverOps + "\n  exp: " + expectedAckedClientOps
        + "\n  u:" + unackedClientOps + "\n  q:" + queuedClientOps + "\n}";
  }

  /**
   * @return the current unacked client ops. Note: the behavior of this list after calling mutating
   *         methods on the transform queue is undefined. This method should be called each time
   *         immediately before use.
   */
  public List<M> unackedClientOps() {
    return Collections.unmodifiableList(unackedClientOps);
  }

  public int unackedClientOpsCount() {
    return unackedClientOps.size();
  }

  private void checkRevision(int resultingRevision) {
    assert resultingRevision >= 1 : "New revision " + resultingRevision + " must be >= 1";
    assert this.revision == resultingRevision - 1 : "Revision mismatch: at " + this.revision
        + ", received " + resultingRevision;
  }
}

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

import com.goodow.realtime.json.Json;
import com.goodow.realtime.json.JsonArray;
import com.goodow.realtime.json.JsonArray.ListIterator;
import com.goodow.realtime.operation.Operation;
import com.goodow.realtime.operation.Transformer;
import com.goodow.realtime.operation.util.Pair;

/**
 * Simple implementation of main concurrency control logic, independent of transport concerns.
 * 
 * <p>
 * For efficiency, client ops are also compacted before transforming and before sending.
 */
@SuppressWarnings("rawtypes")
public class TransformQueue<O extends Operation> {

  private final Transformer<O> transformer;
  private double version = -1;
  private double expectedAckedClientOps = 0;
  private JsonArray serverOps = Json.createArray();
  private JsonArray queuedClientOps = Json.createArray();
  private O unackedClientOp;

  public TransformQueue(Transformer<O> transformer) {
    this.transformer = transformer;
  }

  public void ackClientOp(double appliedAt) {
    checkAppliedVersion(appliedAt);
    assert expectedAckedClientOps == 0 : "must call expectedAck, there are "
        + expectedAckedClientOps + " expectedAckedClientOps";
    assert unackedClientOp != null : this + ": unackedClientOp is null; applied @" + appliedAt;

    version++;
    unackedClientOp = null;
  }

  public O ackOpIfVersionMatches(double appliedAt) {
    if (appliedAt == version) {
      assert unackedClientOp != null;
      O toRtn = unackedClientOp;
      unackedClientOp = null;
      expectedAckedClientOps++;
      version++;
      return toRtn;
    }
    return null;
  }

  public void clientOp(O clientOp) {
    if (serverOps.length() > 0) {
      final JsonArray newServerOps = Json.createArray().push(clientOp);
      serverOps.forEach(new ListIterator<O>() {
        @SuppressWarnings("unchecked")
        @Override
        public void call(int index, O serverOp) {
          Pair<O, O> pair =
              transformer.transform((O) newServerOps.remove(newServerOps.length() - 1), serverOp);
          newServerOps.push(pair.second).push(pair.first);
        }
      });
      clientOp = newServerOps.remove(newServerOps.length() - 1);
      serverOps = newServerOps;
    }

    queuedClientOps.push(clientOp);
  }

  public boolean expectedAck(double appliedAt) {
    if (expectedAckedClientOps == 0) {
      return false;
    }
    assert appliedAt == version - expectedAckedClientOps : "bad applied @" + appliedAt
        + ", current @" + version + ", expected remaining " + expectedAckedClientOps;

    expectedAckedClientOps--;
    return true;
  }

  public boolean hasQueuedClientOps() {
    return queuedClientOps.length() > 0;
  }

  public boolean hasServerOp() {
    return serverOps.length() > 0;
  }

  public void init(double version) {
    assert this.version == -1 : "Already at a version (" + this.version + "), can't init at "
        + version + ")";
    assert version >= 0 : "Initial version must be >= 0, not " + version;
    this.version = version;
  }

  public O peekServerOp() {
    assert hasServerOp() : "No server ops";
    return serverOps.<O> get(0);
  }

  /**
   * Pushes the queued client ops into the unacked ops, clearing the queued ops.
   * 
   * @return see {@link #unackedClientOp()}
   */
  public O pushQueuedOpsToUnacked() {
    assert unackedClientOp == null : "Queue contains unacknowledged operation: " + unackedClientOp;

    unackedClientOp = transformer.compose(queuedClientOps);
    queuedClientOps = Json.createArray();

    return unackedClientOp();
  }

  public O removeServerOp() {
    assert hasServerOp() : "No server ops";
    return serverOps.<O> remove(0);
  }

  public void serverOp(double appliedVersion, O serverOp) {
    checkAppliedVersion(appliedVersion);
    assert expectedAckedClientOps == 0 : "server op applied @" + appliedVersion
        + " while expecting " + expectedAckedClientOps + " client ops";
    version++;

    if (unackedClientOp != null) {
      Pair<O, O> pair = transformer.transform(unackedClientOp, serverOp);
      unackedClientOp = pair.first;
      serverOp = pair.second;
    }
    if (queuedClientOps.length() > 0) {
      O composedClientOp = transformer.compose(queuedClientOps);
      Pair<O, O> pair = transformer.transform(composedClientOp, serverOp);
      queuedClientOps = Json.createArray().push(pair.first);
      serverOps.push(pair.second);
    }
  }

  @Override
  public String toString() {
    return "TQ{ " + version + "\n  s:" + serverOps + "\n  exp: " + expectedAckedClientOps
        + "\n  u:" + unackedClientOp + "\n  q:" + queuedClientOps + "\n}";
  }

  /**
   * @return the current unacked client op. Note: the behavior of this op after calling mutating
   *         methods on the transform queue is undefined. This method should be called each time
   *         immediately before use.
   */
  public O unackedClientOp() {
    return unackedClientOp;
  }

  public double version() {
    return version;
  }

  private void checkAppliedVersion(double appliedVersion) {
    assert appliedVersion >= 0 : "Applied version " + appliedVersion + " must be >= 0";
    assert this.version == appliedVersion : "Version mismatch: @" + this.version + ", applied @"
        + appliedVersion;
  }
}

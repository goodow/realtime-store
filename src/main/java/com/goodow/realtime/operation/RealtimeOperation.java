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
package com.goodow.realtime.operation;

import com.goodow.realtime.operation.list.algorithm.ListOp;
import com.goodow.realtime.util.Pair;

public class RealtimeOperation<T> implements Operation<T> {

  public final Operation<T> op;
  public final String userId;
  public final int revision;

  public final String sessionId;

  public RealtimeOperation(Operation<T> op, String userId, int revision, String sessionId) {
    this.op = op;
    this.userId = userId;
    this.revision = revision;
    this.sessionId = sessionId;
  }

  @Override
  public void apply(T target) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Operation<T> composeWith(Operation<T> operation) {
    assert operation instanceof RealtimeOperation;
    assert !(operation instanceof ListOp);
    assert !isNoOp() && !operation.isNoOp();
    RealtimeOperation<T> op = (RealtimeOperation<T>) operation;
    return new RealtimeOperation<T>(this.op.composeWith(op.<T> getOp()), userId, revision,
        sessionId);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof RealtimeOperation)) {
      return false;
    }
    @SuppressWarnings("rawtypes")
    RealtimeOperation other = (RealtimeOperation) obj;
    if (op == null) {
      if (other.op != null) {
        return false;
      }
    } else if (!op.equals(other.op)) {
      return false;
    }
    if (revision != other.revision) {
      return false;
    }
    if (sessionId == null) {
      if (other.sessionId != null) {
        return false;
      }
    } else if (!sessionId.equals(other.sessionId)) {
      return false;
    }
    if (userId == null) {
      if (other.userId != null) {
        return false;
      }
    } else if (!userId.equals(other.userId)) {
      return false;
    }
    return true;
  }

  @Override
  public String getId() {
    return op.getId();
  }

  @SuppressWarnings("unchecked")
  public <O> Operation<O> getOp() {
    return (Operation<O>) op;
  }

  @Override
  public int getType() {
    return op.getType();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((op == null) ? 0 : op.hashCode());
    result = prime * result + revision;
    result = prime * result + ((sessionId == null) ? 0 : sessionId.hashCode());
    result = prime * result + ((userId == null) ? 0 : userId.hashCode());
    return result;
  }

  @Override
  public Operation<T> invert() {
    return new RealtimeOperation<T>(op.invert(), userId, revision, sessionId);
  }

  @Override
  public boolean isNoOp() {
    return op.isNoOp();
  }

  @Override
  public void setId(String id) {
    op.setId(id);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    if (revision != -1) {
      sb.append("[");
    }
    sb.append("[").append(op.getType()).append(",\"").append(getId()).append("\",").append(
        op.toString()).append("]");
    if (revision != -1) {
      sb.append(",").append("\"").append(userId).append("\",").append(revision).append(",\"")
          .append(sessionId).append("\"").append("]");
    }
    return sb.toString();
  }

  @SuppressWarnings("unchecked")
  @Override
  public Pair<RealtimeOperation<T>, ? extends RealtimeOperation<?>> transformWith(
      Operation<?> clientOp) {
    assert !isNoOp() && !clientOp.isNoOp();
    assert clientOp instanceof RealtimeOperation;
    RealtimeOperation<T> op = (RealtimeOperation<T>) clientOp;
    if (!getId().equals(op.getId())) {
      return Pair.of(this, op);
    }
    assert getType() == op.getType();
    Pair<? extends Operation<T>, ? extends Operation<?>> pair = this.op.transformWith(op.getOp());
    RealtimeOperation<T> transformedServerOp =
        new RealtimeOperation<T>(pair.first, userId, revision, sessionId);
    @SuppressWarnings("rawtypes")
    RealtimeOperation<?> transformedClientOp =
        new RealtimeOperation(pair.second, op.userId, op.revision, op.sessionId);
    return Pair.of(transformedServerOp, transformedClientOp);
  }

}

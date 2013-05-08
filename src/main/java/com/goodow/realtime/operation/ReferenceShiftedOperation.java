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

import com.goodow.realtime.IndexReference;
import com.goodow.realtime.util.Pair;

import elemental.json.JsonArray;

public class ReferenceShiftedOperation implements Operation<IndexReference> {
  public static final int TYPE = 25;
  private String id;
  public final String referencedObject;
  public final int newIndex;
  public final boolean canBeDeleted;
  public final int oldIndex;

  public ReferenceShiftedOperation(JsonArray serialized) {
    this(serialized.getString(0), (int) serialized.getNumber(1), serialized.getBoolean(2),
        (int) serialized.getNumber(3));
  }

  public ReferenceShiftedOperation(String referencedObject, int newIndex, boolean canBeDeleted,
      int oldIndex) {
    this.referencedObject = referencedObject;
    this.newIndex = newIndex;
    this.canBeDeleted = canBeDeleted;
    this.oldIndex = oldIndex;
  }

  @Override
  public void apply(IndexReference target) {
    throw new IllegalStateException();
  }

  @Override
  public Operation<IndexReference> composeWith(Operation<IndexReference> op) {
    assert op instanceof ReferenceShiftedOperation;
    ReferenceShiftedOperation o = (ReferenceShiftedOperation) op;
    assert referencedObject.equals(o.referencedObject);
    return new ReferenceShiftedOperation(referencedObject, o.newIndex, canBeDeleted, oldIndex);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof ReferenceShiftedOperation)) {
      return false;
    }
    ReferenceShiftedOperation other = (ReferenceShiftedOperation) obj;
    if (canBeDeleted != other.canBeDeleted) {
      return false;
    }
    if (id == null) {
      if (other.id != null) {
        return false;
      }
    } else if (!id.equals(other.id)) {
      return false;
    }
    if (newIndex != other.newIndex) {
      return false;
    }
    if (oldIndex != other.oldIndex) {
      return false;
    }
    if (referencedObject == null) {
      if (other.referencedObject != null) {
        return false;
      }
    } else if (!referencedObject.equals(other.referencedObject)) {
      return false;
    }
    return true;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public int getType() {
    return TYPE;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (canBeDeleted ? 1231 : 1237);
    result = prime * result + ((id == null) ? 0 : id.hashCode());
    result = prime * result + newIndex;
    result = prime * result + oldIndex;
    result = prime * result + ((referencedObject == null) ? 0 : referencedObject.hashCode());
    return result;
  }

  @Override
  public Operation<IndexReference> invert() {
    return new ReferenceShiftedOperation(referencedObject, oldIndex, canBeDeleted, newIndex);
  }

  @Override
  public boolean isNoOp() {
    return newIndex == oldIndex;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  @Override
  public String toString() {
    return "[\"" + referencedObject + "\"," + newIndex + "," + canBeDeleted + "," + oldIndex + "]";
  }

  @Override
  public Pair<? extends Operation<IndexReference>, ? extends Operation<?>> transformWith(
      Operation<?> clientOp) {
    assert clientOp instanceof ReferenceShiftedOperation;
    ReferenceShiftedOperation op = (ReferenceShiftedOperation) clientOp;
    assert referencedObject.equals(op.referencedObject);
    if (op.oldIndex != oldIndex) {
      throw new TransformException("Mismatched initial value: attempt to transform " + toString()
          + " with " + op.toString());
    }
    return Pair.of(new ReferenceShiftedOperation(referencedObject, op.newIndex, canBeDeleted,
        op.newIndex), new ReferenceShiftedOperation(referencedObject, op.newIndex, canBeDeleted,
        newIndex));
  }
}

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

import com.goodow.realtime.DocumentBridge;
import com.goodow.realtime.operation.basic.NoOp;
import com.goodow.realtime.util.Pair;

import elemental.json.JsonArray;

public class CreateOperation implements Operation<DocumentBridge> {
  public static final int TYPE = 7;
  public static final int COLLABORATIVE_MAP = 0;
  public static final int COLLABORATIVE_LIST = 1;
  public static final int COLLABORATIVE_STRING = 2;
  public static final int INDEX_REFERENCE = 4;
  public final int type;
  private String id;

  public CreateOperation(int type, String id) {
    this.type = type;
    this.id = id;
  }

  public CreateOperation(JsonArray serialized) {
    this((int) serialized.getNumber(2), serialized.getString(1));
  }

  @Override
  public void apply(DocumentBridge target) {
    throw new IllegalStateException();
  }

  @Override
  public CreateOperation composeWith(Operation<DocumentBridge> op) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof CreateOperation)) {
      return false;
    }
    CreateOperation other = (CreateOperation) obj;
    if (id == null) {
      if (other.id != null) {
        return false;
      }
    } else if (!id.equals(other.id)) {
      return false;
    }
    if (type != other.type) {
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
    result = prime * result + ((id == null) ? 0 : id.hashCode());
    result = prime * result + type;
    return result;
  }

  @Override
  public Operation<DocumentBridge> invert() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isNoOp() {
    return false;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(type);
    return sb.toString();
  }

  @Override
  public Pair<? extends Operation<DocumentBridge>, ? extends Operation<?>> transformWith(
      Operation<?> clientOp) {
    assert equals(clientOp);
    return Pair.of(NoOp.<DocumentBridge> get(), NoOp.get());
  }
}

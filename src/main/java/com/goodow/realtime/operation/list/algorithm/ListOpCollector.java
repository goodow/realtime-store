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
package com.goodow.realtime.operation.list.algorithm;

import elemental.util.ArrayOf;
import elemental.util.Collections;

/**
 * A class that collects list operations together and composes them in an efficient manner.
 */
public final class ListOpCollector<T> {

  private final ArrayOf<ListOp<T>> ops = Collections.arrayOf();
  private final ListHelper<T> helper;

  ListOpCollector(ListHelper<T> helper) {
    this.helper = helper;
  }

  public ListOpCollector<T> add(ListOp<T> op) {
    //
    // The algorithm below ensures that the compose() calls for a sequence of
    // operations are performed in a tree-like manner. In an operation sequence,
    // it is generally faster to compose two adjacent ops than it is to compose
    // an op with the composition of all ops before it, so this distribution is
    // expected to be faster than sequential composition.
    //
    // The time complexity for composing N ops of total size n is O(n log N),
    // rather than O(n^2) for sequential composition.
    //
    // Illustration of first 8 steps, demonstrating the adjacent composition:
    // [a]
    // [0, (a;b)]
    // [c, (a;b)]
    // [0, 0, (a;b);(c;d)]
    // [e, 0, (a;b);(c;d)]
    // [0, (e;f), (a;b);(c;d)]
    // [g, (e;f), (a;b);(c;d)]
    // [0, 0, 0, ((a;b);(c;d));((e;f);(g;h)))]
    //
    // See https://groups.google.com/d/topic/wave-protocol/hbvYL0jLybE/discussion
    //
    for (int i = 0, len = ops.length(); i < len; i++) {
      ListOp<T> nextOperation = ops.get(i);
      if (nextOperation == null) {
        ops.set(i, op);
        return this;
      }
      ops.set(i, null);
      op = compose(nextOperation, op);
    }
    ops.push(op);
    return this;
  }

  public ListOp<T> composeAll() {
    ListOp<T> result = null;
    for (int i = 0, len = ops.length(); i < len; i++) {
      ListOp<T> op = ops.get(i);
      if (op != null) {
        result = (result != null) ? compose(op, result) : op;
      }
    }
    ops.setLength(0);
    return result;
  }

  ListOp<T> compose(ListOp<T> op1, ListOp<T> op2) {
    return new ListOpComposer<T>(helper).compose(op1, op2);
  }
}
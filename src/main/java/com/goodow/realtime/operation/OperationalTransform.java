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

import com.goodow.realtime.util.Pair;

import elemental.util.ArrayOf;
import elemental.util.Collections;

public class OperationalTransform {
  public Pair<ArrayOf<RealtimeOperation<?>>, ArrayOf<RealtimeOperation<?>>> transform(
      ArrayOf<RealtimeOperation<?>> serverOps, ArrayOf<RealtimeOperation<?>> clientOps) {
    ArrayOf<RealtimeOperation<?>> sOps =
        Collections.<RealtimeOperation<?>> arrayOf().concat(serverOps);
    ArrayOf<RealtimeOperation<?>> cOps =
        Collections.<RealtimeOperation<?>> arrayOf().concat(clientOps);
    sLoop : for (int i = 0; i < sOps.length(); i++) {
      RealtimeOperation<?> serverOp = sOps.get(i);
      assert !serverOp.isNoOp();
      for (int j = 0; j < cOps.length(); j++) {
        RealtimeOperation<?> clientOp = cOps.get(j);
        assert !clientOp.isNoOp();
        Pair<? extends RealtimeOperation<?>, ? extends RealtimeOperation<?>> pair =
            serverOp.transformWith(clientOp);
        serverOp = pair.first;
        clientOp = pair.second;
        if (serverOp.isNoOp()) {
          sOps.removeByIndex(i--);
          if (clientOp.isNoOp()) {
            cOps.removeByIndex(j--);
          }
          continue sLoop;
        } else if (clientOp.isNoOp()) {
          cOps.removeByIndex(j--);
          continue;
        }
        cOps.set(j, clientOp);
      }
      sOps.set(i, serverOp);
    }
    return Pair.of(sOps, cOps);
  }

}

/*
 * Copyright 2014 Goodow.com
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
package com.goodow.realtime.store.server;

import com.google.inject.ImplementedBy;

import com.goodow.realtime.store.server.impl.MemoryDeltaStorage;

import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.impl.CountingCompletionHandler;
import org.vertx.java.core.json.JsonObject;

@ImplementedBy(MemoryDeltaStorage.class)
public interface DeltaStorage {

  String ROOT = "root";

  void start(CountingCompletionHandler<Void> countDownLatch);

  /**
   * Get the named document from the storage.
   *
   * @param callback called with {v:, snapshot:[], root:{}} or null if the document has never been
   *                 created in the storage.
   */
  void getSnapshot(String docType, String docId, AsyncResultHandler<JsonObject> callback);

  /**
   * @param snapshotData {v:, snapshot:[], root:{}}
   */
  void writeSnapshot(String docType, String docId, JsonObject snapshotData,
                     AsyncResultHandler<Void> callback);

  /**
   * This is used to store an operation.
   *
   * Its possible writeOp will be called multiple times with the same operation (at the same
   * version). In this case, the function can safely do nothing (or overwrite the existing identical
   * data). It MUST NOT change the version number.
   *
   * Its guaranteed that writeOp calls will be in order - that is, the storage will never be asked
   * to store operation 10 before it has received operation 9. It may receive operation 9 on a
   * different server.
   *
   * @param opData {v:, op:[], sid:, uid:} should probably contain a v: field (if it doesn't, it
   *               defaults to the current version).
   */
  void writeOp(String docType, String docId, JsonObject opData, AsyncResultHandler<Void> callback);

  /**
   * Get the current version of the document, which is one more than the version number of the last
   * operation the storage stores.
   */
  void getVersion(String docType, String docId, AsyncResultHandler<Long> callback);

  /**
   * Get operations between [from, to) noninclusively. (Ie, the range should contain start but not
   * end).
   *
   * If end is null, this should return all operations from from onwards.
   *
   * The operations that getOps returns don't need to have a version: field.
   * The version will be inferred from the parameters if it is missing.
   *
   * Due to certain race conditions, its possible that this misses operations at the end of the
   * range. Callers have to deal with this case (semantically it should be the same as an operation
   * being submitted right after a getOps call)
   *
   * @param callback called with {v:, ops:[]}
   */
  void getOps(String docType, String docId, Long from, Long to,
              AsyncResultHandler<JsonObject> callback);

  void atomicSubmit(String docType, String docId, JsonObject opData,
                    AsyncResultHandler<Void> callback);

  void postSubmit(String docType, String docId, JsonObject opData, JsonObject snapshot);
}

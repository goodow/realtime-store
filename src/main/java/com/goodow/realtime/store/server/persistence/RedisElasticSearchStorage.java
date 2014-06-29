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
package com.goodow.realtime.store.server.persistence;

import com.google.inject.Inject;

import com.goodow.realtime.store.server.DeltaStorage;

import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.impl.CountingCompletionHandler;
import org.vertx.java.core.json.JsonObject;

public class RedisElasticSearchStorage implements DeltaStorage {
  @Inject private RedisDriver redis;
  @Inject private ElasticSearchDriver elasticSearch;

  @Inject
  RedisElasticSearchStorage() {
  }

  @Override
  public void start(CountingCompletionHandler<Void> countDownLatch) {
    redis.start(countDownLatch);
    elasticSearch.start(countDownLatch);
  }

  @Override
  public void getSnapshot(String docType, String docId,
                          AsyncResultHandler<JsonObject> callback) {
    elasticSearch.getSnapshot(docType, docId, callback);
  }

  @Override
  public void writeSnapshot(String docType, String docId, JsonObject snapshotData,
                            AsyncResultHandler<Void> callback) {
    elasticSearch.writeSnapshot(docType, docId, snapshotData, callback);
  }

  @Override
  public void writeOp(String docType, String docId, JsonObject opData,
                      AsyncResultHandler<Void> callback) {
    elasticSearch.writeOp(docType, docId, opData, callback);
  }

  @Override
  public void getVersion(String docType, String docId, AsyncResultHandler<Long> callback) {
    redis.getVersion(docType, docId, callback);
  }

  @Override
  public void getOps(String docType, String docId, Long from, Long to,
                     AsyncResultHandler<JsonObject> callback) {
    redis.getOps(docType, docId, from, to, callback);
  }

  @Override
  public void atomicSubmit(String docType, String docId, JsonObject opData,
                           AsyncResultHandler<Void> callback) {
    redis.atomicSubmit(docType, docId, opData, callback);
  }

  @Override
  public void postSubmit(String docType, String docId, JsonObject opData, JsonObject snapshot) {
    redis.postSubmit(docType, docId, opData, snapshot);
  }
}

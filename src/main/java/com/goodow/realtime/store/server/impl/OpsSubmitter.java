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
package com.goodow.realtime.store.server.impl;

import com.google.inject.Inject;

import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.json.JsonObject;

public class OpsSubmitter {
  @Inject private RedisDriver redis;

  public void submit(String type, String id, JsonObject opData, AsyncResultHandler<Void> callback) {
    redis.atomicSubmit(type, id, opData, callback);
  }
}

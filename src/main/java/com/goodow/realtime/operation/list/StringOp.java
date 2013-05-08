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
package com.goodow.realtime.operation.list;

import com.goodow.realtime.operation.list.algorithm.ListHelper;
import com.goodow.realtime.operation.list.algorithm.ListOp;

import elemental.json.JsonArray;
import elemental.json.JsonString;
import elemental.json.JsonValue;

public class StringOp extends ListOp<String> {

  public StringOp() {
  }

  public StringOp(boolean isInsert, int idx, String str, int initLength) {
    super(isInsert, idx, str, initLength);
  }

  public StringOp(JsonArray serialized) {
    super(serialized);
  }

  @Override
  protected ListHelper<String> createListHelper() {
    return new StringHelper();
  }

  @Override
  protected String fromJson(JsonValue json) {
    return ((JsonString) json).getString();
  }

  @Override
  protected String toJson(String list) {
    return "\"" + list + "\"";
  }
}
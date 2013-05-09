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
import com.goodow.realtime.operation.list.algorithm.ListNormalizer;
import com.goodow.realtime.operation.list.algorithm.ListNormalizer.Appender;
import com.goodow.realtime.operation.list.algorithm.ListOp;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonValue;

public class ArrayOp extends ListOp<JsonArray> {
  private static class ArrayAppender implements Appender<JsonArray> {
    private JsonArray array = Json.createArray();

    @Override
    public void append(JsonArray list) {
      for (int i = 0, len = list.length(); i < len; i++) {
        array.set(array.length(), list.get(i));
      }
    }

    @Override
    public JsonArray flush() {
      try {
        return array;
      } finally {
        array = Json.createArray();
      }
    }
  }
  private static class ArrayHelper implements ListHelper<JsonArray> {
    @Override
    public ListNormalizer<JsonArray> createNormalizer() {
      return new ArrayNormalizer();
    }

    @Override
    public int length(JsonArray list) {
      return list.length();
    }

    @Override
    public ListOp<JsonArray> newOp() {
      return new ArrayOp();
    }

    @Override
    public boolean startsWith(JsonArray list, JsonArray prefix) {
      assert list.length() >= prefix.length();
      for (int i = 0, len = prefix.length(); i < len; i++) {
        if ((prefix.get(i) == null && list.get(i) != null)
            || (prefix.get(i) != null && list.get(i) == null)
            || !prefix.get(i).toJson().equals(list.get(i).toJson())) {
          return false;
        }
      }
      return true;
    }

    @Override
    public JsonArray subset(JsonArray list, int beginIdx) {
      return subset(list, beginIdx, list.length());
    }

    @Override
    public JsonArray subset(JsonArray list, int beginIdx, int endIdx) {
      JsonArray array = Json.createArray();
      for (int i = beginIdx; i < endIdx; i++) {
        array.set(i - beginIdx, list.get(i));
      }
      return array;
    }
  }
  private static class ArrayNormalizer extends ListNormalizer<JsonArray> {
    protected ArrayNormalizer() {
      super(new ArrayOp(), new ArrayAppender());
    }

    @Override
    protected boolean isEmpty(JsonArray list) {
      return list.length() == 0;
    }
  }

  @SuppressWarnings("hiding")
  public static final int TYPE = 11;

  public ArrayOp() {
  }

  public ArrayOp(boolean isInsert, int idx, JsonArray list, int initLength) {
    super(isInsert, idx, list, initLength);
  }

  public ArrayOp(JsonArray serialized) {
    super(serialized);
  }

  @Override
  public int getType() {
    return TYPE;
  }

  @Override
  protected ListHelper<JsonArray> createListHelper() {
    return new ArrayHelper();
  }

  @Override
  protected JsonArray fromJson(JsonValue json) {
    return (JsonArray) json;
  }

  @Override
  protected String toJson(JsonArray list) {
    return list.toJson();
  }
}
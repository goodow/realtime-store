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
package com.goodow.realtime.util;

import com.goodow.realtime.CollaborativeObject;

import java.util.Map;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonType;
import elemental.json.JsonValue;

public class JsonSerializer {
  public static final int REFERENCE_TYPE = 2;
  private static final int VALUE_TYPE = 21;

  public static boolean isNull(JsonValue json) {
    return json == null || JsonType.NULL == json.getType();
  }

  public static boolean jsonEqual(JsonValue a, JsonValue b) {
    if (isNull(a)) {
      return isNull(b);
    } else {
      return isNull(b) ? false : a.toJson().equals(b.toJson());
    }
  }

  public static Object jsonToObj(JsonValue arrayOrNull, Map<String, CollaborativeObject> objects) {
    if (isNull(arrayOrNull)) {
      return null;
    }
    JsonArray array = (JsonArray) arrayOrNull;
    switch ((int) array.getNumber(0)) {
      case VALUE_TYPE:
        switch (array.get(1).getType()) {
          case BOOLEAN:
            return array.getBoolean(1);
          case NUMBER:
            return array.getNumber(1);
          case STRING:
            return array.getString(1);
          case OBJECT:
          case ARRAY:
            return array.get(1);
          case NULL:
          default:
            throw new RuntimeException("Should not reach here!");
        }
      case REFERENCE_TYPE:
        return objects.get(array.getString(1));
      default:
        throw new UnsupportedOperationException();
    }
  }

  public static JsonArray objToJson(Object obj) {
    if (obj == null) {
      return null;
    }
    JsonArray array = Json.createArray();
    int type = VALUE_TYPE;
    JsonValue val;
    if (obj instanceof String) {
      val = Json.create((String) obj);
    } else if (obj instanceof Number) {
      val = Json.create(((Number) obj).doubleValue());
    } else if (obj instanceof Boolean) {
      val = Json.create((Boolean) obj);
    } else if (obj instanceof JsonValue) {
      val = (JsonValue) obj;
      if (JsonType.NULL == val.getType()) {
        return null;
      }
    } else if (obj instanceof CollaborativeObject) {
      type = REFERENCE_TYPE;
      val = Json.create(((CollaborativeObject) obj).getId());
    } else {
      throw new ClassCastException("Unsupported class type: " + obj.getClass().getName());
    }
    array.set(0, type);
    array.set(1, val);
    return array;
  }
}

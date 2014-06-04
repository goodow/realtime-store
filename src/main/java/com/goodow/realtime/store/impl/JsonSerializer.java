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
package com.goodow.realtime.store.impl;

import com.goodow.realtime.json.Json;
import com.goodow.realtime.json.JsonArray;
import com.goodow.realtime.json.JsonElement;
import com.goodow.realtime.json.JsonObject;
import com.goodow.realtime.store.CollaborativeObject;

class JsonSerializer {
  public static final int REFERENCE_TYPE = 2;
  private static final int VALUE_TYPE = 21;

  public static Object deserializeObject(JsonArray arrayOrNull, JsonObject objects) {
    if (arrayOrNull == null) {
      return null;
    }
    switch ((int) arrayOrNull.getNumber(0)) {
      case VALUE_TYPE:
        switch (arrayOrNull.getType(1)) {
          case BOOLEAN:
            return arrayOrNull.getBoolean(1);
          case NUMBER:
            return arrayOrNull.getNumber(1);
          case STRING:
            return arrayOrNull.getString(1);
          case OBJECT:
            return arrayOrNull.getObject(1).copy();
          case ARRAY:
            return arrayOrNull.getArray(1).copy();
          case NULL:
          default:
            throw new RuntimeException("Should not reach here!");
        }
      case REFERENCE_TYPE:
        return objects.get(arrayOrNull.getString(1));
      default:
        throw new UnsupportedOperationException();
    }
  }

  public static JsonArray serializeObject(Object obj) {
    if (obj == null) {
      return null;
    }
    JsonArray array = Json.createArray();
    if (obj instanceof Number) {
      array.push(VALUE_TYPE).push(((Number) obj).doubleValue());
    } else if (obj instanceof Boolean) {
      array.push(VALUE_TYPE).push(((Boolean) obj).booleanValue());
    } else if (obj instanceof CollaborativeObject) {
      array.push(REFERENCE_TYPE).push(((CollaborativeObject) obj).id());
    } else if (obj instanceof String) {
      array.push(VALUE_TYPE).push(obj);
    } else if (obj instanceof JsonObject) {
      array.push(VALUE_TYPE).push(((JsonObject) obj).copy());
    } else if (obj instanceof JsonArray) {
      array.push(VALUE_TYPE).push(((JsonArray) obj).copy());
    } else {
      throw new IllegalArgumentException("Invalid JSON type: " + obj.getClass().getName());
    }
    return array;
  }

  public static JsonArray serializeObjects(JsonArray values) {
    final JsonArray array = Json.createArray();
    values.forEach(new JsonArray.ListIterator<Object>() {
      @Override
      public void call(int index, Object obj) {
        array.push(serializeObject(obj));
      }
    });
    return array;
  }
}

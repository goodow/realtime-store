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
package com.goodow.realtime.operation.map;

import com.goodow.realtime.operation.ComposeException;
import com.goodow.realtime.operation.Operation;
import com.goodow.realtime.operation.TransformException;
import com.goodow.realtime.util.JsonSerializer;
import com.goodow.realtime.util.Pair;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.JsonValue;
import elemental.util.ArrayOfString;
import elemental.util.Collections;
import elemental.util.MapFromStringTo;

public class MapOp implements Operation<MapTarget>, MapTarget {
  public static final String TYPE = "o";
  private static final String INSERT = "i";
  private static final String DELETE = "d";

  protected final MapFromStringTo<Pair<JsonValue, JsonValue>> components;

  public MapOp() {
    components = Collections.mapFromStringTo();
  }

  public MapOp(String json) {
    this();
    JsonArray components = Json.instance().parse(json);
    for (int i = 0, len = components.length(); i < len; i++) {
      JsonArray component = components.getArray(i);
      if (component.length() == 3) {
        update(component.getString(0), component.get(1), component.get(2));
      } else {
        assert component.length() == 2;
        JsonObject obj = component.getObject(1);
        assert obj.keys().length == 1;
        if (INSERT.equals(obj.keys()[0])) {
          update(component.getString(0), null, obj.get(INSERT));
        } else {
          assert DELETE.equals(obj.keys()[0]);
          update(component.getString(0), obj.get(DELETE), null);
        }
      }
    }
  }

  @Override
  public void apply(MapTarget target) {
    ArrayOfString keys = components.keys();
    for (int i = 0, len = keys.length(); i < len; i++) {
      String key = keys.get(i);
      target.update(key, components.get(key).first, components.get(key).second);
    }
  }

  @Override
  public MapOp composeWith(Operation<MapTarget> op) {
    assert op instanceof MapOp;
    MapOp toRtn = copy();
    MapOp o = (MapOp) op;
    ArrayOfString keys = o.components.keys();
    for (int i = 0, len = keys.length(); i < len; i++) {
      String key = keys.get(i);
      toRtn.update(key, o.components.get(key).first, o.components.get(key).second);
    }
    return toRtn;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof MapOp)) {
      return false;
    }
    return toString().equals(obj.toString());
  }

  @Override
  public String getType() {
    return TYPE;
  }

  @Override
  public MapOp invert() {
    MapOp op = newInstance();
    ArrayOfString keys = components.keys();
    for (int i = 0, len = keys.length(); i < len; i++) {
      String key = keys.get(i);
      op.update(key, components.get(key).second, components.get(key).first);
    }
    return op;
  }

  @Override
  public boolean isNoOp() {
    return components.keys().isEmpty();
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("[");
    ArrayOfString keys = components.keys();
    for (int i = 0, len = keys.length(); i < len; i++) {
      String key = keys.get(i);
      if (i != 0) {
        sb.append(",");
      }
      sb.append(toJson(key, components.get(key).first, components.get(key).second));
    }
    sb.append("]");
    return sb.toString();
  }

  @Override
  public Pair<MapOp, MapOp> transformWith(Operation<?> clientOp) {
    assert clientOp instanceof MapOp;
    MapOp op = (MapOp) clientOp;
    MapOp transformedClientOp = newInstance();
    ArrayOfString clientKeys = op.components.keys();
    for (int i = 0, len = clientKeys.length(); i < len; i++) {
      String clientKey = clientKeys.get(i);
      JsonValue clientOldValue = op.components.get(clientKey).first;
      JsonValue clientNewValue = op.components.get(clientKey).second;
      if (!components.hasKey(clientKey)) {
        transformedClientOp.update(clientKey, clientOldValue, clientNewValue);
        continue;
      }
      JsonValue serverOldValue = components.get(clientKey).first;
      JsonValue serverNewValue = components.get(clientKey).second;
      if (!JsonSerializer.jsonEqual(serverOldValue, clientOldValue)) {
        throw new TransformException("Mismatched initial value: attempt to transform "
            + toJson(clientKey, serverOldValue, serverNewValue) + " with "
            + toJson(clientKey, clientOldValue, clientNewValue));
      }
      if (JsonSerializer.jsonEqual(serverNewValue, clientNewValue)) {
        continue;
      }
      transformedClientOp.update(clientKey, serverNewValue, clientNewValue);
    }
    MapOp transformedServerOp = exclude(op);
    return Pair.of(transformedServerOp, transformedClientOp);
  }

  @Override
  public MapOp update(String key, JsonValue oldValue, JsonValue newValue) {
    assert key != null : "Null key";
    if (JsonSerializer.jsonEqual(oldValue, newValue)) {
      return this;
    }
    if (!components.hasKey(key)) {
      components.put(key, Pair.of(oldValue, newValue));
      return this;
    }
    if (!JsonSerializer.jsonEqual(components.get(key).second, oldValue)) {
      throw new ComposeException("Mismatched value: attempt to compose "
          + toJson(key, components.get(key).first, components.get(key).second) + " with "
          + toJson(key, oldValue, newValue));
    }
    if (JsonSerializer.jsonEqual(components.get(key).first, newValue)) {
      components.remove(key);
    } else {
      components.put(key, Pair.of(components.get(key).first, newValue));
    }
    return this;
  }

  protected MapOp copy() {
    MapOp toRtn = newInstance();
    ArrayOfString keys = components.keys();
    for (int i = 0, len = keys.length(); i < len; i++) {
      String key = keys.get(i);
      toRtn.update(key, components.get(key).first, components.get(key).second);
    }
    return toRtn;
  }

  protected MapOp newInstance() {
    return new MapOp();
  }

  private MapOp exclude(MapOp clientOp) {
    MapOp transformedServerOp = newInstance();
    ArrayOfString keys = components.keys();
    for (int i = 0, len = keys.length(); i < len; i++) {
      String key = keys.get(i);
      if (!clientOp.components.hasKey(key)) {
        transformedServerOp.update(key, components.get(key).first, components.get(key).second);
      }
    }
    return transformedServerOp;
  }

  private String toJson(String key, JsonValue oldVal, JsonValue newVal) {
    StringBuilder sb = new StringBuilder("[\"").append(key).append("\"").append(",");
    if (oldVal == null) {
      assert newVal != null;
      sb.append("{\"" + INSERT + "\":").append(newVal.toJson()).append("}");
    } else if (newVal == null) {
      sb.append("{\"" + DELETE + "\":").append(oldVal.toJson()).append("}");
    } else {
      sb.append(oldVal.toJson()).append(",");
      sb.append(newVal.toJson());
    }
    return sb.append("]").toString();
  }
}
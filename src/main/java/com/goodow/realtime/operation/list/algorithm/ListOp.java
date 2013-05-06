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

import com.goodow.realtime.operation.Operation;
import com.goodow.realtime.util.Pair;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.JsonType;
import elemental.json.JsonValue;
import elemental.util.ArrayOf;
import elemental.util.Collections;

public abstract class ListOp<T> implements Operation<ListTarget<T>>, ListTarget<T> {
  enum ComponentType {
    INSERT, DELETE, RETAIN;
  }
  private interface Component<T> {
    void apply(ListTarget<T> target);

    ComponentType getComponentType();
  }
  private class Delete implements Component<T> {
    private static final String KEY = "d";
    private final T list;

    public Delete(T list) {
      this.list = list;
    }

    @Override
    public void apply(ListTarget<T> target) {
      target.delete(list);
    }

    @Override
    public ComponentType getComponentType() {
      return ComponentType.DELETE;
    }

    @Override
    public String toString() {
      return "{\"" + KEY + "\":" + toJson(list) + "}";
    }
  }
  private class Insert implements Component<T> {
    private static final String KEY = "i";
    private final T list;

    public Insert(T list) {
      this.list = list;
    }

    @Override
    public void apply(ListTarget<T> target) {
      target.insert(list);
    }

    @Override
    public ComponentType getComponentType() {
      return ComponentType.INSERT;
    }

    @Override
    public String toString() {
      return "{\"" + KEY + "\":" + toJson(list) + "}";
    }
  }
  private class Retain implements Component<T> {
    private final int length;

    public Retain(int length) {
      this.length = length;
    }

    @Override
    public void apply(ListTarget<T> target) {
      target.retain(length);
    }

    @Override
    public ComponentType getComponentType() {
      return ComponentType.RETAIN;
    }

    @Override
    public String toString() {
      return "" + length;
    }
  }

  public static final String TYPE = "l";
  private boolean frozen;
  protected final ArrayOf<Component<T>> components;
  private ListHelper<T> helper;

  protected ListOp() {
    components = Collections.arrayOf();
  }

  protected ListOp(boolean isInsert, int idx, T list, int initLength) {
    this();
    if (list == null || getListHelper().length(list) <= 0) {
      if (initLength > 0) {
        retain(initLength);
      }
      return;
    }
    if (idx > 0) {
      retain(idx);
    }
    if (isInsert) {
      insert(list);
    } else {
      delete(list);
      idx += getListHelper().length(list);
    }
    assert idx <= initLength;
    if (initLength > idx) {
      retain(initLength - idx);
    }
    frozen = true;
  }

  protected ListOp(String json) {
    this();
    JsonArray components = Json.instance().parse(json);
    for (int i = 0, len = components.length(); i < len; i++) {
      JsonValue component = components.get(i);
      if (JsonType.NUMBER == component.getType()) {
        this.components.push(new Retain((int) component.asNumber()));
      } else {
        assert JsonType.OBJECT == component.getType();
        JsonObject c = (JsonObject) component;
        assert c.keys().length == 1;
        String key = c.keys()[0];
        if (Insert.KEY.equals(key)) {
          this.components.push(new Insert(fromJson(c.get(key))));
        } else if (Delete.KEY.equals(key)) {
          this.components.push(new Delete(fromJson(c.get(key))));
        } else {
          throw new IllegalStateException("Cannot parse ListOp component from json: " + c.toJson());
        }
      }
    }
    frozen = true;
  }

  @Override
  public void apply(ListTarget<T> target) {
    for (int i = 0, len = size(); i < len; i++) {
      applyComponent(i, target);
    }
  }

  @Override
  public ListOp<T> composeWith(Operation<ListTarget<T>> op) {
    // Use createOpCollector() instead.
    throw new UnsupportedOperationException();
  }

  public ListOpCollector<T> createOpCollector() {
    return new ListOpCollector<T>(getListHelper());
  }

  @Override
  public ListOp<T> delete(T list) {
    assert !frozen;
    assert list != null && getListHelper().length(list) > 0;
    components.push(new Delete(list));
    return this;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof ListOp)) {
      return false;
    }
    return toString().equals(obj.toString());
  }

  @Override
  public String getType() {
    return TYPE;
  }

  @Override
  public ListOp<T> insert(T list) {
    assert !frozen;
    assert list != null && getListHelper().length(list) > 0;
    components.push(new Insert(list));
    return this;
  }

  @Override
  public ListOp<T> invert() {
    return new ListOpInverter<T>(this, getListHelper().newOp()).finish();
  }

  @Override
  public boolean isNoOp() {
    return components.isEmpty();
  }

  @Override
  public ListOp<T> retain(int length) {
    assert !frozen;
    assert length > 0;
    components.push(new Retain(length));
    return this;
  }

  @Override
  public String toString() {
    return "[" + components.join() + "]";
  }

  @SuppressWarnings("unchecked")
  @Override
  public Pair<? extends ListOp<T>, ? extends ListOp<T>> transformWith(Operation<?> clientOp) {
    assert clientOp instanceof ListOp;
    return new ListOpTransformer<T>(getListHelper()).transform(this, (ListOp<T>) clientOp);
  }

  protected abstract ListHelper<T> createListHelper();

  protected abstract T fromJson(JsonValue json);

  protected abstract String toJson(T list);

  void applyComponent(int i, ListTarget<T> target) {
    assert i < size();
    components.get(i).apply(target);
  }

  int size() {
    return components.length();
  }

  private ListHelper<T> getListHelper() {
    if (helper == null) {
      helper = createListHelper();
    }
    return helper;
  }
}
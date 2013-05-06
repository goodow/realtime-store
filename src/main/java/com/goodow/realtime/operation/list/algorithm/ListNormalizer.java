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

import com.goodow.realtime.operation.list.algorithm.ListOp.ComponentType;

public abstract class ListNormalizer<T> implements ListTarget<T> {

  public interface Appender<T> {
    void append(T list);

    T flush();
  }

  private final ListOp<T> output;
  private final Appender<T> appender;
  private ComponentType component;
  private int retain;

  protected ListNormalizer(ListOp<T> output, Appender<T> appender) {
    this.output = output;
    this.appender = appender;
  }

  @Override
  public ListNormalizer<T> delete(T list) {
    if (!isEmpty(list)) {
      if (component != ComponentType.DELETE) {
        flush();
      }
      component = ComponentType.DELETE;
      appender.append(list);
    }
    return this;
  }

  public ListOp<T> finish() {
    flush();
    return output;
  }

  @Override
  public ListNormalizer<T> insert(T list) {
    if (!isEmpty(list)) {
      if (component != ComponentType.INSERT) {
        flush();
      }
      component = ComponentType.INSERT;
      appender.append(list);
    }
    return this;
  }

  @Override
  public ListNormalizer<T> retain(int length) {
    if (length > 0) {
      if (component != ComponentType.RETAIN) {
        flush();
      }
      component = ComponentType.RETAIN;
      retain += length;
    }
    return this;
  }

  @Override
  public String toString() {
    return output.toString() + "\n" + component + ": "
        + (component == ComponentType.RETAIN ? "" + retain : appender.toString());
  }

  protected abstract boolean isEmpty(T list);

  private void flush() {
    if (component == null) {
      return;
    }
    switch (component) {
      case INSERT:
        output.insert(appender.flush());
        break;
      case DELETE:
        output.delete(appender.flush());
        break;
      case RETAIN:
        output.retain(retain);
        retain = 0;
        break;
      default:
        throw new UnsupportedOperationException(component.name());
    }
  }
}
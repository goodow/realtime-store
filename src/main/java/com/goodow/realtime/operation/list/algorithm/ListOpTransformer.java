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

import static com.goodow.realtime.operation.list.algorithm.ListOpComposer.initialDocumentLength;

import com.goodow.realtime.operation.TransformException;
import com.goodow.realtime.operation.list.algorithm.ListOpComposer.Target;
import com.goodow.realtime.util.Pair;

class ListOpTransformer<T> {
  private final class DefaultPreTarget extends PreTarget {
    @Override
    public ListTarget<T> delete(T list) {
      target = new DeletePostTarget(list);
      return null;
    }

    @Override
    public ListTarget<T> retain(int length) {
      target = new RetainPostTarget(length);
      return null;
    }
  }

  private final class DeletePostTarget extends PostTarget {
    private T list;

    DeletePostTarget(T list) {
      this.list = list;
    }

    @Override
    public ListTarget<T> delete(T list) {
      if (helper.length(list) <= helper.length(this.list)) {
        assert helper.startsWith(this.list, list);
        cancelDelete(helper.length(list));
      } else {
        assert helper.startsWith(list, this.list);
        target = new DeletePreTarget(helper.subset(list, helper.length(this.list)));
      }
      return null;
    }

    @Override
    public ListTarget<T> retain(int length) {
      if (length <= helper.length(list)) {
        serverNormalizer.delete(helper.subset(list, 0, length));
        cancelDelete(length);
      } else {
        serverNormalizer.delete(list);
        target = new RetainPreTarget(length - helper.length(list));
      }
      return null;
    }

    private void cancelDelete(int size) {
      if (size < helper.length(list)) {
        list = helper.subset(list, size);
      } else {
        target = defaultTarget;
      }
    }
  }

  private final class DeletePreTarget extends PreTarget {
    private T list;

    DeletePreTarget(T list) {
      this.list = list;
    }

    @Override
    public ListTarget<T> delete(T list) {
      if (helper.length(list) <= helper.length(this.list)) {
        assert helper.startsWith(this.list, list);
        cancelDelete(helper.length(list));
      } else {
        assert helper.startsWith(list, this.list);
        target = new DeletePostTarget(helper.subset(list, helper.length(this.list)));
      }
      return null;
    }

    @Override
    public ListTarget<T> retain(int length) {
      if (length <= helper.length(list)) {
        clientNormalizer.delete(helper.subset(list, 0, length));
        cancelDelete(length);
      } else {
        clientNormalizer.delete(list);
        target = new RetainPostTarget(length - helper.length(list));
      }
      return null;
    }

    private void cancelDelete(int size) {
      if (size < helper.length(list)) {
        list = helper.subset(list, size);
      } else {
        target = defaultTarget;
      }
    }
  }

  private abstract class PostTarget extends Target<T> {
    @Override
    public ListTarget<T> insert(T list) {
      serverNormalizer.retain(helper.length(list));
      clientNormalizer.insert(list);
      return null;
    }

    @Override
    boolean isPostTarget() {
      return true;
    }
  }

  private abstract class PreTarget extends Target<T> {
    @Override
    public ListTarget<T> insert(T list) {
      serverNormalizer.insert(list);
      clientNormalizer.retain(helper.length(list));
      return null;
    }

    @Override
    boolean isPostTarget() {
      return false;
    }
  }

  private final class RetainPostTarget extends PostTarget {
    private int length;

    RetainPostTarget(int length) {
      this.length = length;
    }

    @Override
    public ListTarget<T> delete(T list) {
      if (helper.length(list) <= length) {
        clientNormalizer.delete(list);
        cancelRetain(helper.length(list));
      } else {
        clientNormalizer.delete(helper.subset(list, 0, length));
        target = new DeletePreTarget(helper.subset(list, length));
      }
      return null;
    }

    @Override
    public ListTarget<T> retain(int length) {
      if (length <= this.length) {
        serverNormalizer.retain(length);
        clientNormalizer.retain(length);
        cancelRetain(length);
      } else {
        serverNormalizer.retain(this.length);
        clientNormalizer.retain(this.length);
        target = new RetainPreTarget(length - this.length);
      }
      return null;
    }

    private void cancelRetain(int size) {
      if (size < length) {
        length -= size;
      } else {
        target = defaultTarget;
      }
    }
  }

  private final class RetainPreTarget extends PreTarget {
    private int length;

    RetainPreTarget(int length) {
      this.length = length;
    }

    @Override
    public ListTarget<T> delete(T list) {
      if (helper.length(list) <= length) {
        serverNormalizer.delete(list);
        cancelRetain(helper.length(list));
      } else {
        serverNormalizer.delete(helper.subset(list, 0, length));
        target = new DeletePostTarget(helper.subset(list, length));
      }
      return null;
    }

    @Override
    public ListTarget<T> retain(int length) {
      if (length <= this.length) {
        serverNormalizer.retain(length);
        clientNormalizer.retain(length);
        cancelRetain(length);
      } else {
        serverNormalizer.retain(this.length);
        clientNormalizer.retain(this.length);
        target = new RetainPostTarget(length - this.length);
      }
      return null;
    }

    private void cancelRetain(int size) {
      if (size < length) {
        length -= size;
      } else {
        target = defaultTarget;
      }
    }
  }

  private final ListHelper<T> helper;
  private final ListNormalizer<T> serverNormalizer;
  private final ListNormalizer<T> clientNormalizer;
  private final Target<T> defaultTarget = new DefaultPreTarget();
  private Target<T> target;

  ListOpTransformer(ListHelper<T> helper) {
    this.helper = helper;
    serverNormalizer = helper.createNormalizer();
    clientNormalizer = helper.createNormalizer();
  }

  Pair<ListOp<T>, ListOp<T>> transform(ListOp<T> serverOp, ListOp<T> clientOp) {
    target = defaultTarget;
    int serverIndex = 0;
    int clientIndex = 0;
    while (serverIndex < serverOp.size()) {
      serverOp.applyComponent(serverIndex++, target);
      while (target.isPostTarget()) {
        if (clientIndex >= clientOp.size()) {
          throw new TransformException("Document size mismatch: " + "serverOp initial length="
              + initialDocumentLength(serverOp, helper) + ", clientOp initial length="
              + initialDocumentLength(clientOp, helper));
        }
        clientOp.applyComponent(clientIndex++, target);
      }
    }
    if (clientIndex < clientOp.size()) {
      clientOp.applyComponent(clientIndex++, target);
    }
    if (target != defaultTarget) {
      throw new TransformException("Illegal transformation: Document size mismatch");
    }
    return Pair.of(serverNormalizer.finish(), clientNormalizer.finish());
  }
}

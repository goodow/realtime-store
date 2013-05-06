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

import com.goodow.realtime.operation.ComposeException;

class ListOpComposer<T> {
  static abstract class Target<T> implements ListTarget<T> {
    abstract boolean isPostTarget();
  }

  private final class DefaultPreTarget extends PreTarget {

    @Override
    public ListTarget<T> insert(T list) {
      target = new InsertPostTarget(list);
      return null;
    }

    @Override
    public ListTarget<T> retain(int length) {
      target = new RetainPostTarget(length);
      return null;
    }
  }

  private final class DeletePreTarget extends PreTarget {
    private T list;

    DeletePreTarget(T list) {
      this.list = list;
    }

    @Override
    public ListTarget<T> insert(T list) {
      if (helper.length(list) <= helper.length(this.list)) {
        assert helper.startsWith(this.list, list);
        cancelDelete(helper.length(list));
      } else {
        assert helper.startsWith(list, this.list);
        target = new InsertPostTarget(helper.subset(list, helper.length(this.list)));
      }
      return null;
    }

    @Override
    public ListTarget<T> retain(int length) {
      if (length <= helper.length(list)) {
        normalizer.delete(helper.subset(list, 0, length));
        cancelDelete(length);
      } else {
        normalizer.delete(list);
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

  private final class FinisherPostTarget extends PostTarget {
    @Override
    public ListTarget<T> delete(T list) {
      throw new ComposeException("Illegal composition");
    }

    @Override
    public ListTarget<T> retain(int length) {
      throw new ComposeException("Illegal composition");
    }
  }

  private final class InsertPostTarget extends PostTarget {
    private T list;

    InsertPostTarget(T list) {
      this.list = list;
    }

    @Override
    public ListTarget<T> delete(T list) {
      if (helper.length(list) <= helper.length(this.list)) {
        assert helper.startsWith(this.list, list);
        cancelInsert(helper.length(list));
      } else {
        assert helper.startsWith(list, this.list);
        target = new DeletePreTarget(helper.subset(list, helper.length(this.list)));
      }
      return null;
    }

    @Override
    public ListTarget<T> retain(int length) {
      if (length <= helper.length(list)) {
        normalizer.insert(helper.subset(list, 0, length));
        cancelInsert(length);
      } else {
        normalizer.insert(list);
        target = new RetainPreTarget(length - helper.length(list));
      }
      return null;
    }

    private void cancelInsert(int size) {
      if (size < helper.length(list)) {
        list = helper.subset(list, size);
      } else {
        target = defaultTarget;
      }
    }
  }

  private abstract class PostTarget extends Target<T> {
    @Override
    public final ListTarget<T> insert(T list) {
      normalizer.insert(list);
      return null;
    }

    @Override
    final boolean isPostTarget() {
      return true;
    }
  }

  private abstract class PreTarget extends Target<T> {
    @Override
    public final ListTarget<T> delete(T list) {
      normalizer.delete(list);
      return null;
    }

    @Override
    final boolean isPostTarget() {
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
        normalizer.delete(list);
        cancelRetain(helper.length(list));
      } else {
        normalizer.delete(helper.subset(list, 0, length));
        target = new DeletePreTarget(helper.subset(list, length));
      }
      return null;
    }

    @Override
    public ListTarget<T> retain(int length) {
      if (length <= this.length) {
        normalizer.retain(length);
        cancelRetain(length);
      } else {
        normalizer.retain(this.length);
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
    public ListTarget<T> insert(T list) {
      if (helper.length(list) <= length) {
        normalizer.insert(list);
        cancelRetain(helper.length(list));
      } else {
        normalizer.insert(helper.subset(list, 0, length));
        target = new InsertPostTarget(helper.subset(list, length));
      }
      return null;
    }

    @Override
    public ListTarget<T> retain(int length) {
      if (length <= this.length) {
        normalizer.retain(length);
        cancelRetain(length);
      } else {
        normalizer.retain(this.length);
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

  /**
   * Computes the number of items of the document that an op applies to, prior to its application.
   */
  static <T> int initialDocumentLength(ListOp<T> op, final ListHelper<T> helper) {
    final int[] size = {0};
    op.apply(new ListTarget<T>() {
      @Override
      public ListTarget<T> delete(T list) {
        size[0] += helper.length(list);
        return null;
      }

      @Override
      public ListTarget<T> insert(T list) {
        return null;
      }

      @Override
      public ListTarget<T> retain(int length) {
        size[0] += length;
        return null;
      }
    });
    return size[0];
  }

  /**
   * Computes the number of items of the document that an op produces when applied.
   */
  static <T> int resultingDocumentLength(ListOp<T> op, final ListHelper<T> helper) {
    final int[] size = {0};
    op.apply(new ListTarget<T>() {
      @Override
      public ListTarget<T> delete(T list) {
        return null;
      }

      @Override
      public ListTarget<T> insert(T list) {
        size[0] += helper.length(list);
        return null;
      }

      @Override
      public ListTarget<T> retain(int length) {
        size[0] += length;
        return null;
      }
    });
    return size[0];
  }

  private final ListHelper<T> helper;
  private final ListNormalizer<T> normalizer;

  private final Target<T> defaultTarget = new DefaultPreTarget();

  private Target<T> target;

  ListOpComposer(ListHelper<T> helper) {
    this.helper = helper;
    normalizer = helper.createNormalizer();
  }

  /**
   * Incrementally apply the two operations in a linearly-ordered interleaving fashion.
   */
  ListOp<T> compose(ListOp<T> op1, ListOp<T> op2) {
    target = defaultTarget;
    int op1Index = 0, op2Index = 0;
    while (op1Index < op1.size()) {
      op1.applyComponent(op1Index++, target);
      while (target.isPostTarget()) {
        if (op2Index >= op2.size()) {
          throw new ComposeException("Document size mismatch: " + "op1 resulting length="
              + resultingDocumentLength(op1, helper) + ", op2 initial length="
              + initialDocumentLength(op2, helper));
        }
        op2.applyComponent(op2Index++, target);
      }
    }
    if (op2Index < op2.size()) {
      target = new FinisherPostTarget();
      while (op2Index < op2.size()) {
        op2.applyComponent(op2Index++, target);
      }
    }
    return normalizer.finish();
  }
}
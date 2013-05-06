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

import com.goodow.realtime.operation.TransformException;
import com.goodow.realtime.operation.list.StringOp;
import com.goodow.realtime.operation.list.algorithm.ListOp;
import com.goodow.realtime.util.Pair;

import junit.framework.TestCase;

public class ListOpTransformerTest extends TestCase {
  private static final class ReversibleTestParameters extends TestParameters {

    @Override
    TestParameters clientOp(ListOp<String> clientOp) {
      super.clientOp(clientOp);
      return this;
    }

    @Override
    void run() {
      singleTest(serverOp, clientOp, transformedServerOp, transformedClientOp);
      singleTest(clientOp, serverOp, transformedClientOp, transformedServerOp);
    }

    @Override
    TestParameters serverOp(ListOp<String> serverOp) {
      super.serverOp(serverOp);
      return this;
    }

    @Override
    TestParameters transformedClientOp(ListOp<String> transformedClientOp) {
      super.transformedClientOp(transformedClientOp);
      return this;
    }

    @Override
    TestParameters transformedServerOp(ListOp<String> transformedServerOp) {
      super.transformedServerOp(transformedServerOp);
      return this;
    }
  }
  private static class TestParameters {

    ListOp<String> serverOp;
    ListOp<String> clientOp;
    ListOp<String> transformedServerOp;
    ListOp<String> transformedClientOp;

    TestParameters clientOp(ListOp<String> clientOp) {
      this.clientOp = clientOp;
      return this;
    }

    void run() {
      singleTest(serverOp, clientOp, transformedServerOp, transformedClientOp);
    }

    TestParameters serverOp(ListOp<String> serverOp) {
      this.serverOp = serverOp;
      return this;
    }

    TestParameters transformedClientOp(ListOp<String> transformedClientOp) {
      this.transformedClientOp = transformedClientOp;
      return this;
    }

    TestParameters transformedServerOp(ListOp<String> transformedServerOp) {
      this.transformedServerOp = transformedServerOp;
      return this;
    }
  }

  private static void singleTest(ListOp<String> serverOp, ListOp<String> clientOp,
      ListOp<String> transformedServerOp, ListOp<String> transformedClientOp) {
    Pair<? extends ListOp<String>, ? extends ListOp<String>> pair =
        serverOp.transformWith(clientOp);
    assertEquals(transformedServerOp, pair.first);
    assertEquals(transformedClientOp, pair.second);
  }

  /**
   * Performs tests for transforming text deletions against text deletions.
   */
  public void testDeleteVsDelete() {
    // A's deletion spatially before B's deletion
    new ReversibleTestParameters().serverOp(new StringOp(false, 1, "abcde", 20)).clientOp(
        new StringOp(false, 7, "fg", 20)).transformedServerOp(new StringOp(false, 1, "abcde", 18))
        .transformedClientOp(new StringOp(false, 2, "fg", 15)).run();
    // A's deletion spatially adjacent to and before B's deletion
    new ReversibleTestParameters().serverOp(new StringOp(false, 1, "abcde", 20)).clientOp(
        new StringOp(false, 6, "fg", 20)).transformedServerOp(new StringOp(false, 1, "abcde", 18))
        .transformedClientOp(new StringOp(false, 1, "fg", 15)).run();
    // A's deletion overlaps B's deletion
    new ReversibleTestParameters().serverOp(new StringOp(false, 1, "abcde", 20)).clientOp(
        new StringOp(false, 3, "cdefghi", 20))
        .transformedServerOp(new StringOp(false, 1, "ab", 13)).transformedClientOp(
            new StringOp(false, 1, "fghi", 15)).run();
    // A's deletion a subset of B's deletion
    new ReversibleTestParameters().serverOp(new StringOp(false, 1, "abcdefg", 20)).clientOp(
        new StringOp(false, 3, "cd", 20)).transformedServerOp(new StringOp(false, 1, "abefg", 18))
        .transformedClientOp(new StringOp().retain(13)).run();
    // A's deletion identical to B's deletion
    new ReversibleTestParameters().serverOp(new StringOp(false, 1, "abcdefg", 20)).clientOp(
        new StringOp(false, 1, "abcdefg", 20)).transformedServerOp(new StringOp().retain(13))
        .transformedClientOp(new StringOp().retain(13)).run();
  }

  public void testDocumentLengthMismatch() {
    StringOp op = new StringOp();
    op.retain(1);
    try {
      op.transformWith(new StringOp());
      fail();
    } catch (TransformException e) {
      // ok
    }
    try {
      new StringOp().transformWith(op);
      fail();
    } catch (TransformException e) {
      // ok
    }
  }

  /**
   * Performs tests for transforming text insertions against text deletions.
   */
  public void testInsertVsDelete() {
    // A's insertion spatially before B's deletion
    new ReversibleTestParameters().serverOp(new StringOp(true, 1, "abc", 20)).clientOp(
        new StringOp(false, 2, "de", 20)).transformedServerOp(new StringOp(true, 1, "abc", 18))
        .transformedClientOp(new StringOp(false, 5, "de", 23)).run();
    // A's insertion spatially inside B's deletion
    new ReversibleTestParameters().serverOp(new StringOp(true, 2, "abc", 20)).clientOp(
        new StringOp(false, 1, "ce", 20)).transformedServerOp(new StringOp(true, 1, "abc", 18))
        .transformedClientOp(new StringOp().retain(1).delete("c").retain(3).delete("e").retain(17))
        .run();
    // A's insertion spatially at the start of B's deletion
    new ReversibleTestParameters().serverOp(new StringOp(true, 1, "abc", 20)).clientOp(
        new StringOp(false, 1, "de", 20)).transformedServerOp(new StringOp(true, 1, "abc", 18))
        .transformedClientOp(new StringOp(false, 4, "de", 23)).run();
    // A's insertion spatially at the end of B's deletion
    new ReversibleTestParameters().serverOp(new StringOp(true, 3, "abc", 20)).clientOp(
        new StringOp(false, 1, "de", 20)).transformedServerOp(new StringOp(true, 1, "abc", 18))
        .transformedClientOp(new StringOp(false, 1, "de", 23)).run();
    // A's insertion spatially after B's deletion
    new ReversibleTestParameters().serverOp(new StringOp(true, 4, "abc", 20)).clientOp(
        new StringOp(false, 1, "de", 20)).transformedServerOp(new StringOp(true, 2, "abc", 18))
        .transformedClientOp(new StringOp(false, 1, "de", 23)).run();
  }

  /**
   * Performs tests for transforming text insertions against text insertions.
   */
  public void testInsertVsInsert() {
    // A's insertion spatially before B's insertion
    new ReversibleTestParameters().serverOp(new StringOp(true, 1, "a", 20)).clientOp(
        new StringOp(true, 2, "1", 20)).transformedServerOp(new StringOp(true, 1, "a", 21))
        .transformedClientOp(new StringOp(true, 3, "1", 21)).run();
    // A's insertion spatially at the same location as B's insertion
    new TestParameters().serverOp(new StringOp(true, 2, "abc", 20)).clientOp(
        new StringOp(true, 2, "123", 20)).transformedServerOp(new StringOp(true, 2, "abc", 23))
        .transformedClientOp(new StringOp(true, 5, "123", 23)).run();
  }
}

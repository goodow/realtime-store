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
import com.goodow.realtime.operation.list.StringOp;
import com.goodow.realtime.operation.list.algorithm.ListOp;
import com.goodow.realtime.operation.list.algorithm.ListOpCollector;

import junit.framework.TestCase;

public class ListOpComposerTest extends TestCase {

  public void testDocumentLengthMismatch() {
    StringOp op = new StringOp();
    op.retain(1);
    try {
      new StringOp().createOpCollector().compose(new StringOp(), op);
      fail();
    } catch (ComposeException e) {
      // ok
    }
    try {
      new StringOp().createOpCollector().compose(op, new StringOp());
      fail();
    } catch (ComposeException e) {
      // ok
    }
  }

  // There is no Operation object that can represent a universal no-op, so null
  // needs to be used instead. To work correctly, the collector needs to compose
  // an empty collection to a universal no-op.
  public void testEmptyCollectionComposesToNull() {
    assertNull(new StringOp().createOpCollector().composeAll());
  }

  public void testInsertThenDelete() {
    StringOp ibcd = new StringOp(true, 1, "bcd", 25);
    StringOp dc = new StringOp(false, 2, "c", 28);
    StringOp dde = new StringOp(false, 2, "de", 27);
    StringOp ief = new StringOp(true, 2, "ef", 25);
    StringOp i = new StringOp(true, 23, "ab", 27);
    StringOp d = new StringOp(false, 11, "abc", 29);

    ListOpCollector<String> collector = ibcd.createOpCollector();
    collector.add(ibcd).add(dc).add(dde).add(ief).add(i).add(d);

    ListOp<String> expected =
        new StringOp().retain(1).insert("b").delete("e").insert("ef").retain(7).delete("abc")
            .retain(9).insert("ab").retain(4);

    assertEquals(expected, collector.composeAll());

    collector.add(expected).add(expected.invert());
    expected =
        new StringOp().retain(1).delete("e").insert("e").retain(7).delete("abc").insert("abc")
            .retain(13);
    assertEquals(expected, collector.composeAll());
  }

  public void testSimpleMonotonicDeleteComposition() {
    StringOp b = new StringOp(false, 1, "b", 5);
    StringOp c = new StringOp(false, 1, "c", 4);
    StringOp d = new StringOp(false, 1, "d", 3);

    ListOpCollector<String> collector = b.createOpCollector();
    collector.add(b).add(c).add(d);
    ListOp<String> op = collector.composeAll();

    StringOp expected = new StringOp(false, 1, "bcd", 5);
    assertEquals(expected, op);

    collector.add(expected).add(expected.invert());
    expected = (StringOp) new StringOp().retain(1).delete("bcd").insert("bcd").retain(1);
    assertEquals(expected, collector.composeAll());
  }

  public void testSimpleMonotonicInsertComposition() {
    StringOp a = new StringOp(true, 0, "a", 0);
    StringOp b = new StringOp(true, 1, "b", 1);
    StringOp c = new StringOp(true, 2, "c", 2);
    StringOp d = new StringOp(true, 3, "d", 3);

    ListOpCollector<String> collector = a.createOpCollector().add(a).add(b).add(c).add(d);
    ListOp<String> op = collector.composeAll();

    StringOp expected = new StringOp(true, 0, "abcd", 0);
    assertEquals(expected, op);

    collector.add(expected).add(expected.invert());
    assertTrue(collector.composeAll().isNoOp());
  }
}
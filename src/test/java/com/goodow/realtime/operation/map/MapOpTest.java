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
import com.goodow.realtime.operation.TransformException;
import com.goodow.realtime.operation.map.MapOp;
import com.goodow.realtime.util.Pair;

import junit.framework.TestCase;

import elemental.json.Json;
import elemental.json.JsonValue;

public class MapOpTest extends TestCase {
  private static final class ReversibleTestParameters extends TestParameters {
    ReversibleTestParameters(MapOp serverOp, MapOp clientOp, MapOp transformedServerOp,
        MapOp transformedClientOp) {
      super(serverOp, clientOp, transformedServerOp, transformedClientOp);
    }

    @Override
    void run() {
      singleTest(serverOp, clientOp, transformedServerOp, transformedClientOp);
      singleTest(clientOp, serverOp, transformedClientOp, transformedServerOp);
    }
  }

  private static class TestParameters {
    final MapOp serverOp;
    final MapOp clientOp;
    final MapOp transformedServerOp;
    final MapOp transformedClientOp;

    TestParameters(MapOp serverOp, MapOp clientOp, MapOp transformedServerOp,
        MapOp transformedClientOp) {
      this.serverOp = serverOp;
      this.clientOp = clientOp;
      this.transformedServerOp = transformedServerOp;
      this.transformedClientOp = transformedClientOp;
    }

    void run() {
      singleTest(serverOp, clientOp, transformedServerOp, transformedClientOp);
    }
  }

  private static void singleTest(MapOp serverOp, MapOp clientOp, MapOp transformedServerOp,
      MapOp transformedClientOp) {
    Pair<MapOp, MapOp> pair = serverOp.transformWith(clientOp);
    assertEquals(transformedServerOp, pair.first);
    assertEquals(transformedClientOp, pair.second);
  }

  public void testComposeDifferentKey() {
    MapOp op = new MapOp().update("a", null, val("new a"));
    op = op.composeWith(new MapOp().update("b", val("old b"), null));
    MapOp expected = new MapOp().update("a", null, val("new a")).update("b", val("old b"), null);
    assertEquals(expected, op);
  }

  public void testComposeException() {
    try {
      new MapOp().update("a", val(""), val("should same")).composeWith(
          new MapOp().update("a", val("should same but diff"), val("")));
      fail();
    } catch (ComposeException e) {
      // ok
    }
  }

  public void testComposeNoOp() {
    MapOp op = new MapOp().update("a", val("initial"), val("should same"));
    op = op.composeWith(new MapOp().update("a", val("should same"), val("initial")));
    assertTrue(op.isNoOp());

    op = new MapOp().update("a", val("same"), val("same"));
    assertTrue(op.isNoOp());
  }

  public void testComposeSameKey() {
    MapOp op = new MapOp().update("a", val("old a"), val("should same"));
    op = op.composeWith(new MapOp().update("a", val("should same"), val("new b")));
    MapOp expected = new MapOp().update("a", val("old a"), val("new b"));
    assertEquals(expected, op);
  }

  public void testInvert() {
    MapOp op =
        new MapOp().update("a", null, val("new a")).update("b", val("old b"), null).update("c",
            val("old c"), val("new c"));
    MapOp expected =
        new MapOp().update("a", val("new a"), null).update("b", null, val("old b")).update("c",
            val("new c"), val("old c"));
    assertEquals(expected, op.invert());
    assertTrue(op.composeWith(op.invert()).isNoOp());
  }

  public void testInvertNoOp() {
    assertEquals(new MapOp(), new MapOp().invert());
  }

  public void testParseFromJson() {
    MapOp op =
        new MapOp().update("a", val("old a"), null)
            .update("b", Json.create(false), Json.create(1f)).update("c", Json.create(true),
                Json.create(-2.2));
    assertEquals(op, new MapOp(op.toString()));
  }

  public void testTransformDifferentKey() {
    new ReversibleTestParameters(new MapOp().update("a", val("initial"), val("new a")), new MapOp()
        .update("b", val("initial"), val("new b")), new MapOp().update("a", val("initial"),
        val("new a")), new MapOp().update("b", val("initial"), val("new b"))).run();
  }

  public void testTransformException() {
    try {
      new MapOp().update("a", val("should same"), val("new a")).transformWith(
          new MapOp().update("a", val("should same but diff"), val("new b")));
      fail();
    } catch (TransformException e) {
      // ok
    }
  }

  public void testTransformNoOp() {
    new ReversibleTestParameters(new MapOp().update("a", val("initial"), val("new a")),
        new MapOp(), new MapOp().update("a", val("initial"), val("new a")), new MapOp()).run();
  }

  public void testTransformSameKey() {
    new TestParameters(new MapOp().update("a", val("initial"), val("new a")), new MapOp().update(
        "a", val("initial"), val("new b")), new MapOp(), new MapOp().update("a", val("new a"),
        val("new b"))).run();
    new TestParameters(new MapOp().update("a", val("initial"), val("new a")), new MapOp().update(
        "a", val("initial"), val("new a")), new MapOp(), new MapOp()).run();
  }

  JsonValue val(String str) {
    return Json.create(str);
  }
}

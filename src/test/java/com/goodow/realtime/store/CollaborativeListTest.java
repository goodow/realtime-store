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
package com.goodow.realtime.store;

import com.goodow.realtime.channel.server.impl.VertxPlatform;
import com.goodow.realtime.core.Handler;
import com.goodow.realtime.json.Json;
import com.goodow.realtime.json.JsonArray;
import com.goodow.realtime.json.JsonObject;
import com.goodow.realtime.store.impl.SimpleStore;

import org.junit.Test;
import org.vertx.testtools.TestVerticle;
import org.vertx.testtools.VertxAssert;

public class CollaborativeListTest extends TestVerticle {
  private Model mod;
  private CollaborativeList list;

  @Override
  public void start() {
    initialize();
    VertxPlatform.register(vertx);

    Store store = new SimpleStore();
    store.load("docId", new Handler<Document>() {
      @Override
      public void handle(Document doc) {
        mod = doc.getModel();
        list = mod.createList();

        startTests();
      }
    }, null, null);
  }

  @Test
  public void testAsArray() {
    list = mod.createList("v1", 1, true, null);
    JsonArray array = list.asArray();
    VertxAssert.assertEquals("v1", array.getString(0));
    VertxAssert.assertEquals(1d, array.getNumber(1), 0d);
    VertxAssert.assertEquals(true, array.getBoolean(2));
    VertxAssert.assertNull(array.get(3));

    VertxAssert.testComplete();
  }

  @Test
  public void testClear() {
    list.push("a");
    list.push(1);
    list.clear();
    VertxAssert.assertEquals(0, list.length());

    VertxAssert.testComplete();
  }

  @Test
  public void testIndexOf() {
    list = mod.createList(2, 1, true, null);
    VertxAssert.assertEquals(1, list.indexOf(1, null));
    VertxAssert.assertEquals(3, list.indexOf(null, null));

    VertxAssert.testComplete();
  }

  @Test
  public void testIndexOutOfBoundsException() {
    try {
      list.get(-1);
      VertxAssert.fail();
    } catch (ArrayIndexOutOfBoundsException e) {
    }
    try {
      list.get(0);
      VertxAssert.fail();
    } catch (ArrayIndexOutOfBoundsException e) {
    }
    try {
      list.set(0, "a");
      VertxAssert.fail();
    } catch (ArrayIndexOutOfBoundsException e) {
    }
    try {
      list.removeRange(0, 1);
      VertxAssert.fail();
    } catch (ArrayIndexOutOfBoundsException e) {
    }
    try {
      list.push(0);
      list.removeRange(-1, 0);
      VertxAssert.fail();
    } catch (ArrayIndexOutOfBoundsException e) {
    }
    try {
      list.removeRange(0, 0);
      VertxAssert.fail();
    } catch (ArrayIndexOutOfBoundsException e) {
    }
    try {
      list.removeRange(0, 2);
      VertxAssert.fail();
    } catch (ArrayIndexOutOfBoundsException e) {
    }
    try {
      list.push(1);
      list.removeRange(1, 0);
      VertxAssert.fail();
    } catch (ArrayIndexOutOfBoundsException e) {
    }

    VertxAssert.testComplete();
  }

  @Test
  public void testInitialize() {
    VertxAssert.assertSame(list, mod.getObject(list.getId()));
    VertxAssert.assertEquals(0, list.length());

    JsonObject v4 = Json.createObject();
    v4.set("subKey", "subValue");
    list = mod.createList("v1", 1, true, v4, null);
    VertxAssert.assertEquals(5, list.length());
    VertxAssert.assertEquals("v1", list.get(0));
    VertxAssert.assertEquals(1d, (Double) list.get(1), 0d);
    VertxAssert.assertEquals(true, list.get(2));
    VertxAssert.assertTrue(v4.equals(list.get(3)));
    VertxAssert.assertNull(list.get(4));

    VertxAssert.testComplete();
  }

  @Test
  public void testInsert() {
    JsonObject v4 = Json.createObject();
    v4.set("subKey", "subValue");
    list.insertAll(0, "v1", 1, true, v4, null);
    list.insert(0, null);
    Object obj = null;
    list.insertAll(2, obj);
    list.insertAll(0);
    VertxAssert.assertEquals(7, list.length());
    VertxAssert.assertNull(list.get(0));
    VertxAssert.assertNull(list.get(2));
    VertxAssert.assertNull(list.get(6));

    VertxAssert.testComplete();
  }

  @Test
  public void testLastIndexOf() {
    list = mod.createList("v1", 1, true, null, null);
    VertxAssert.assertEquals(2, list.lastIndexOf(true, null));
    VertxAssert.assertEquals(4, list.lastIndexOf(null, null));

    VertxAssert.testComplete();
  }
}

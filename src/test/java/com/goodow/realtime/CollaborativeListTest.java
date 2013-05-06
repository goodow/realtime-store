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
package com.goodow.realtime;

import junit.framework.TestCase;

import elemental.json.Json;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

public class CollaborativeListTest extends TestCase {
  private Model mod;
  private CollaborativeList list;

  public void testAsArray() {
    list = mod.createList("v1", 1, true, null, Json.createNull());
    Object[] array = list.asArray();
    assertEquals("v1", array[0]);
    assertEquals(1d, (Double) array[1], 0d);
    assertEquals(true, array[2]);
    assertNull(array[3]);
    assertNull(array[4]);
  }

  public void testClear() {
    list.push("a");
    list.push(1);
    list.clear();
    assertEquals(0, list.length());
  }

  public void testIndexOf() {
    list = mod.createList(2, 1, true, null, Json.createNull());
    assertEquals(1, list.indexOf(1, null));
    assertEquals(3, list.indexOf(null, null));
    assertEquals(3, list.indexOf(Json.createNull(), null));
  }

  public void testIndexOutOfBoundsException() {
    try {
      list.get(-1);
      fail();
    } catch (ArrayIndexOutOfBoundsException e) {
    }
    try {
      list.get(0);
      fail();
    } catch (ArrayIndexOutOfBoundsException e) {
    }
    try {
      list.set(0, "a");
      fail();
    } catch (ArrayIndexOutOfBoundsException e) {
    }
    try {
      list.removeRange(0, 1);
      fail();
    } catch (ArrayIndexOutOfBoundsException e) {
    }
    try {
      list.push(0);
      list.removeRange(-1, 0);
      fail();
    } catch (ArrayIndexOutOfBoundsException e) {
    }
    try {
      list.removeRange(0, 0);
      fail();
    } catch (ArrayIndexOutOfBoundsException e) {
    }
    try {
      list.removeRange(0, 2);
      fail();
    } catch (ArrayIndexOutOfBoundsException e) {
    }
    try {
      list.push(1);
      list.removeRange(1, 0);
      fail();
    } catch (ArrayIndexOutOfBoundsException e) {
    }
  }

  public void testInitialize() {
    assertSame(list, mod.getObject(list.getId()));
    assertEquals(0, list.length());

    JsonObject v4 = Json.createObject();
    v4.put("subKey", "subValue");
    list = mod.createList("v1", 1, true, v4, null, Json.createNull());
    assertEquals(6, list.length());
    assertEquals("v1", list.get(0));
    assertEquals(1d, (Double) list.get(1), 0d);
    assertEquals(true, list.get(2));
    assertTrue(v4.jsEquals((JsonValue) list.get(3)));
    assertNull(list.get(4));
    assertNull(list.get(5));
  }

  public void testInsert() {
    JsonObject v4 = Json.createObject();
    v4.put("subKey", "subValue");
    list.insertAll(0, "v1", 1, true, v4, null, Json.createNull());
    list.insert(0, null);
    list.insertAll(2, (Object[]) null);
    list.insertAll(0);
    assertEquals(8, list.length());
    assertNull(list.get(0));
    assertNull(list.get(2));
    assertNull(list.get(6));
    assertNull(list.get(7));
  }

  public void testLastIndexOf() {
    list = mod.createList("v1", 1, true, null, Json.createNull());
    assertEquals(2, list.lastIndexOf(true, null));
    assertEquals(4, list.lastIndexOf(null, null));
    assertEquals(4, list.lastIndexOf(Json.createNull(), null));
  }

  @Override
  protected void setUp() throws Exception {
    DocumentBridge bridge = new DocumentBridge();
    Document doc = bridge.create(Json.createArray());
    mod = doc.getModel();
    list = mod.createList();
  }
}

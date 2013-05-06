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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

public class CollaborativeMapTest extends TestCase {
  Model mod;
  CollaborativeMap map;

  public void testClear() {
    map.set("k1", "v1");
    map.set("k2", "v2");
    map.clear();
    assertEquals(0, map.size());
  }

  public void testEventHandler() {
    final Object[] mapEvent = new Object[4];
    final Object[] objectChanged = new Object[3];
    EventHandler<ValueChangedEvent> valueChangedHandler = new EventHandler<ValueChangedEvent>() {

      @Override
      public void handleEvent(ValueChangedEvent event) {
        assertFalse((Boolean) mapEvent[3]);
        mapEvent[3] = true;
        objectChanged[0] = event;
        assertSame(map, event.target);
        assertEquals(EventType.VALUE_CHANGED, event.type);
        assertTrue(event.isLocal);
        assertEquals(mapEvent[0], event.property);
        assertEquals(mapEvent[1], event.oldValue);
        assertEquals(mapEvent[2], event.newValue);
      }
    };
    map.addValueChangedListener(valueChangedHandler);
    mapEvent[0] = "a";
    mapEvent[1] = null;
    mapEvent[2] = 3.0;
    mapEvent[3] = false;
    map.set("a", 3);
    assertTrue((Boolean) mapEvent[3]);

    mapEvent[0] = "a";
    mapEvent[1] = 3.0;
    JsonObject json = Json.createObject();
    json.put("a", true);
    json.put("b", 1.0);
    mapEvent[2] = json;
    mapEvent[3] = false;
    map.set("a", json);
    assertTrue((Boolean) mapEvent[3]);

    mapEvent[0] = "a";
    mapEvent[1] = json;
    mapEvent[2] = null;
    mapEvent[3] = false;
    map.remove("a");
    assertTrue((Boolean) mapEvent[3]);

    final CollaborativeString str = mod.createString("abc");
    EventHandler<ObjectChangedEvent> objectChangedHandler = new EventHandler<ObjectChangedEvent>() {

      @Override
      public void handleEvent(ObjectChangedEvent event) {
        assertFalse((Boolean) objectChanged[1]);
        objectChanged[1] = true;
        assertSame(objectChanged[2], event.target);
        assertEquals(EventType.OBJECT_CHANGED, event.type);
        assertTrue(event.isLocal);
        BaseModelEvent[] events = event.events;
        assertEquals(1, events.length);
        assertSame(objectChanged[0], events[0]);
      }
    };
    map.addObjectChangedListener(objectChangedHandler);
    mapEvent[0] = "b";
    mapEvent[1] = null;
    mapEvent[2] = str;
    mapEvent[3] = false;
    objectChanged[1] = false;
    objectChanged[2] = map;
    map.set("b", str);
    assertTrue((Boolean) mapEvent[3]);
    assertTrue((Boolean) objectChanged[1]);

    str.addTextInsertedListener(new EventHandler<TextInsertedEvent>() {

      @Override
      public void handleEvent(TextInsertedEvent event) {
        objectChanged[0] = event;
        objectChanged[2] = event.target;
      }
    });
    mapEvent[3] = false;
    objectChanged[1] = false;
    str.append("c");
    assertFalse((Boolean) mapEvent[3]);
    assertTrue((Boolean) objectChanged[1]);

    map.removeValueChangedListener(valueChangedHandler);
    map.removeEventListener(EventType.OBJECT_CHANGED, objectChangedHandler, false);
    CollaborativeList list = mod.createList();
    CollaborativeMap map2 = mod.createMap(null);
    list.push(map2);
    map.set("cc", list);
    map.addObjectChangedListener(objectChangedHandler);
    map2.addValueChangedListener(new EventHandler<ValueChangedEvent>() {

      @Override
      public void handleEvent(ValueChangedEvent event) {
        objectChanged[0] = event;
        objectChanged[2] = event.target;
      }
    });

    mapEvent[3] = false;
    objectChanged[1] = false;
    map2.set("x", 2);
    assertFalse((Boolean) mapEvent[3]);
    assertTrue((Boolean) objectChanged[1]);
  }

  public void testIllegalArgumentException() {
    try {
      map.set(null, "");
      fail();
    } catch (IllegalArgumentException e) {
    }
  }

  public void testInitialize() {
    assertSame(map, mod.getObject(map.getId()));
    assertEquals(0, map.size());
    assertTrue(map.isEmpty());

    JsonObject v4 = Json.createObject();
    v4.put("subKey", "subValue");
    Map<String, Object> initialValue = new HashMap<String, Object>();
    initialValue.put("k1", "v1");
    initialValue.put("k2", 2);
    initialValue.put("k3", true);
    initialValue.put("k4", v4);
    map = mod.createMap(initialValue);
    assertEquals(4, map.size());
    assertEquals("v1", map.get("k1"));
    assertEquals(2d, (Double) map.get("k2"), 0d);
    assertEquals(true, map.get("k3"));
    assertTrue(v4.jsEquals((JsonValue) map.get("k4")));
    assertFalse(map.isEmpty());
  }

  public void testItems() {
    map.set("k1", "v1");
    map.set("k2", "v2");
    Object[][] items = map.items();
    assertEquals("k1", items[0][0]);
    assertEquals("v1", items[0][1]);
    assertEquals("k2", items[1][0]);
    assertEquals("v2", items[1][1]);
  }

  public void testRemove() {
    map.set("k1", "v1");
    assertEquals("v1", map.remove("k1"));
    assertNull(map.remove("k2"));
    assertEquals(0, map.size());
  }

  public void testSet() {
    JsonArray v4 = Json.createArray();
    v4.set(0, "abc");
    assertNull(map.set("k1", "v1"));
    assertNull(map.set("k2", 4));
    assertNull(map.set("k3", false));
    assertNull(map.set("k4", v4));
    assertNull(map.set("k5", null));
    assertNull(map.set("k6", Json.createNull()));

    assertEquals(4, map.size());
    assertEquals("v1", map.get("k1"));
    assertEquals(4d, (Double) map.get("k2"), 0d);
    assertEquals(false, map.get("k3"));
    assertTrue(v4.jsEquals((JsonValue) map.get("k4")));

    assertEquals("v1", map.set("k1", ""));
    assertEquals(4.0, map.set("k2", null));
    assertEquals(false, map.set("k3", Json.createNull()));
    assertEquals(2, map.size());
    assertEquals("", map.get("k1"));
    assertFalse(map.has("k2"));
    assertFalse(map.has("k3"));
  }

  public void testValues() {
    map.set("k1", "v1");
    map.set("k2", "v2");
    List<Object> values = map.values();
    assertEquals("v1", values.get(0));
    assertEquals("v2", values.get(1));
  }

  @Override
  protected void setUp() throws Exception {
    DocumentBridge bridge = new DocumentBridge();
    Document doc = bridge.create(Json.createArray());
    mod = doc.getModel();
    map = mod.createMap(null);
  }
}

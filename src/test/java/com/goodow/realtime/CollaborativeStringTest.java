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

public class CollaborativeStringTest extends TestCase {
  CollaborativeString str;
  Model mod;

  public void testEventHandler() {
    final Object[] textEvent = new Object[3];
    final Object[] objectChanged = new Object[3];
    EventHandler<TextInsertedEvent> insertHandler = new EventHandler<TextInsertedEvent>() {

      @Override
      public void handleEvent(TextInsertedEvent event) {
        assertFalse((Boolean) textEvent[2]);
        textEvent[2] = true;
        assertSame(str, event.target);
        assertEquals(EventType.TEXT_INSERTED, event.type);
        assertTrue(event.isLocal);
        assertEquals(textEvent[0], event.index);
        assertEquals(textEvent[1], event.text);
      }
    };
    str.addTextInsertedListener(insertHandler);
    str.addTextDeletedListener(new EventHandler<TextDeletedEvent>() {

      @Override
      public void handleEvent(TextDeletedEvent event) {
        assertFalse((Boolean) textEvent[2]);
        textEvent[2] = true;
        objectChanged[0] = event;
        objectChanged[2] = event.target;
        assertSame(str, event.target);
        assertEquals(EventType.TEXT_DELETED, event.type);
        assertTrue(event.isLocal);
        assertEquals(textEvent[0], event.index);
        assertEquals(textEvent[1], event.text);
      }
    });
    textEvent[0] = 0;
    textEvent[1] = "abc";
    textEvent[2] = false;
    str.insertString(0, "abc");
    assertTrue((Boolean) textEvent[2]);

    textEvent[0] = 2;
    textEvent[1] = "2";
    textEvent[2] = false;
    str.insertString(2, "2");
    assertTrue((Boolean) textEvent[2]);

    textEvent[0] = 2;
    textEvent[1] = "2c";
    textEvent[2] = false;
    str.removeRange(2, 4);
    assertTrue((Boolean) textEvent[2]);

    str.removeStringListener(insertHandler);
    textEvent[2] = false;
    str.insertString(2, "cde");
    assertFalse((Boolean) textEvent[2]);

    str.addObjectChangedListener(new EventHandler<ObjectChangedEvent>() {

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
    });

    textEvent[0] = 0;
    textEvent[1] = "abc";
    textEvent[2] = false;
    objectChanged[1] = false;
    str.removeRange(0, 3);
    assertTrue((Boolean) textEvent[2]);
    assertTrue((Boolean) objectChanged[1]);
  }

  public void testIllegalArgumentException() {
    try {
      str.insertString(0, null);
      fail();
    } catch (IllegalArgumentException e) {
    }
    try {
      str.insertString(0, "");
      fail();
    } catch (IllegalArgumentException e) {
    }
  }

  public void testIndexOutOfBoundsException() {
    try {
      str.insertString(1, "ab");
      fail();
    } catch (StringIndexOutOfBoundsException e) {
    }
    try {
      str.insertString(-1, "ab");
      fail();
    } catch (StringIndexOutOfBoundsException e) {
    }
    try {
      str.removeRange(0, 0);
      fail();
    } catch (StringIndexOutOfBoundsException e) {
    }
    try {
      str.insertString(1, "ab");
      str.removeRange(1, 0);
      fail();
    } catch (StringIndexOutOfBoundsException e) {
    }
    try {
      str.insertString(1, "ab");
      str.removeRange(2, 3);
      fail();
    } catch (StringIndexOutOfBoundsException e) {
    }
    try {
      str.insertString(1, "ab");
      str.removeRange(0, 3);
      fail();
    } catch (StringIndexOutOfBoundsException e) {
    }
  }

  public void testInitialize() {
    assertSame(str, mod.getObject(str.getId()));
    assertEquals("", str.getText());

    str = mod.createString("abcd");
    assertEquals("abcd", str.getText());
  }

  public void testInsertRemove() {
    str.insertString(0, "abcdef");
    assertEquals(6, str.length());
    str.removeRange(2, 5);
    assertEquals(3, str.length());
    str.append("gh");
    assertEquals("abfgh", str.getText());
  }

  public void testRegisterReference() {
    str.append("ab");
    final ReferenceShiftedEvent[] e = new ReferenceShiftedEvent[1];
    IndexReference ref = str.registerReference(1, true);
    EventHandler<ReferenceShiftedEvent> handler = new EventHandler<ReferenceShiftedEvent>() {
      @Override
      public void handleEvent(ReferenceShiftedEvent event) {
        assertNull(e[0]);
        e[0] = event;
      }
    };
    ref.addReferenceShiftedListener(handler);
    e[0] = null;
    str.insertString(2, "2"); // "ab2"
    assertNull(e[0]);
    e[0] = null;
    str.insertString(1, "1"); // "a1b2"
    assertEquals(1, e[0].oldIndex);
    assertEquals(2, e[0].newIndex);
    e[0] = null;
    str.removeRange(3, 4);// "a1b"
    assertNull(e[0]);
    e[0] = null;
    str.removeRange(1, 2); // "ab"
    assertEquals(2, e[0].oldIndex);
    assertEquals(1, e[0].newIndex);
    e[0] = null;
    str.removeRange(1, 2); // "a"
    assertEquals(1, e[0].oldIndex);
    assertEquals(-1, e[0].newIndex);

    ref.removeReferenceShiftedListener(handler);
    str.setText("ab");
    ref = str.registerReference(1, false);
    ref.addReferenceShiftedListener(handler);
    e[0] = null;
    str.insertString(2, "2"); // "ab2"
    assertNull(e[0]);
    e[0] = null;
    str.insertString(1, "1"); // "a1b2"
    assertEquals(1, e[0].oldIndex);
    assertEquals(2, e[0].newIndex);
    e[0] = null;
    str.removeRange(3, 4); // "a1b"
    assertNull(e[0]);
    e[0] = null;
    str.removeRange(1, 2); // "ab"
    assertEquals(2, e[0].oldIndex);
    assertEquals(1, e[0].newIndex);
    e[0] = null;
    str.removeRange(1, 2); // "a"
    assertEquals(1, e[0].oldIndex);
    assertEquals(1, e[0].newIndex);
  }

  public void testSetText() {
    str.setText("abc");
    assertEquals("abc", str.getText());
    str.setText("ddabd");
    assertEquals("ddabd", str.getText());
  }

  @Override
  protected void setUp() throws Exception {
    DocumentBridge bridge = new DocumentBridge();
    Document doc = bridge.create(Json.createArray());
    mod = doc.getModel();
    str = mod.createString(null);
  }
}

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
import com.goodow.realtime.store.impl.SimpleStore;

import org.junit.Test;
import org.vertx.testtools.TestVerticle;
import org.vertx.testtools.VertxAssert;

public class CollaborativeStringTest extends TestVerticle {
  static void assertArraySame(JsonArray expecteds, JsonArray actuals) {
    VertxAssert.assertEquals(expecteds.length(), actuals.length());
    for (int i = 0, len = expecteds.length(); i < len; i++) {
      VertxAssert.assertSame(expecteds.get(i), actuals.get(i));
    }
  }

  CollaborativeString str;

  Model mod;

  @Override
  public void start() {
    initialize();
    VertxPlatform.register(vertx);

    Store store = new SimpleStore();
    store.load("docId", new Handler<Document>() {
      @Override
      public void handle(Document doc) {
        mod = doc.getModel();
        str = mod.createString(null);

        startTests();
      }
    }, null, null);
  }

  @Test
  public void testEventHandler() {
    final Object[] objectChanged = new Object[2];
    final int[] count = {0};

    str.addObjectChangedListener(new Handler<ObjectChangedEvent>() {
      @Override
      public void handle(ObjectChangedEvent event) {
        count[0]++;
        VertxAssert.assertSame(str.id, event.target);
        VertxAssert.assertEquals(EventType.OBJECT_CHANGED, event.type);
        VertxAssert.assertTrue(event.isLocal);
        JsonArray events = event.events;
        VertxAssert.assertEquals(2, events.length());
        VertxAssert.assertSame(objectChanged[0], events.<TextInsertedEvent> get(0));
        VertxAssert.assertSame(objectChanged[1], events.<TextDeletedEvent> get(1));

        VertxAssert.assertEquals(3, count[0]);
        VertxAssert.testComplete();
      }
    });

    str.addTextInsertedListener(new Handler<TextInsertedEvent>() {
      @Override
      public void handle(TextInsertedEvent event) {
        count[0]++;
        objectChanged[0] = event;
        VertxAssert.assertSame(str.id, event.target);
        VertxAssert.assertEquals(EventType.TEXT_INSERTED, event.type);
        VertxAssert.assertTrue(event.isLocal);
        VertxAssert.assertEquals(0, event.index);
        VertxAssert.assertEquals("abcdef", event.text);
      }
    });
    str.append("abcdef");

    str.addTextDeletedListener(new Handler<TextDeletedEvent>() {
      @Override
      public void handle(TextDeletedEvent event) {
        count[0]++;
        objectChanged[1] = event;
        VertxAssert.assertSame(str.id, event.target);
        VertxAssert.assertEquals(EventType.TEXT_DELETED, event.type);
        VertxAssert.assertTrue(event.isLocal);
        VertxAssert.assertEquals(2, event.index);
        VertxAssert.assertEquals("cd", event.text);
      }
    });
    str.removeRange(2, 4);
  }

  @Test
  public void testIllegalArgumentException() {
    try {
      str.insertString(0, null);
      VertxAssert.fail();
    } catch (IllegalArgumentException e) {
    }
    try {
      str.insertString(0, "");
      VertxAssert.fail();
    } catch (IllegalArgumentException e) {
    }

    VertxAssert.testComplete();
  }

  @Test
  public void testIndexOutOfBoundsException() {
    try {
      str.insertString(1, "ab");
      VertxAssert.fail();
    } catch (StringIndexOutOfBoundsException e) {
    }
    try {
      str.insertString(-1, "ab");
      VertxAssert.fail();
    } catch (StringIndexOutOfBoundsException e) {
    }
    try {
      str.removeRange(0, 0);
      VertxAssert.fail();
    } catch (StringIndexOutOfBoundsException e) {
    }
    try {
      str.insertString(1, "ab");
      str.removeRange(1, 0);
      VertxAssert.fail();
    } catch (StringIndexOutOfBoundsException e) {
    }
    try {
      str.insertString(1, "ab");
      str.removeRange(2, 3);
      VertxAssert.fail();
    } catch (StringIndexOutOfBoundsException e) {
    }
    try {
      str.insertString(1, "ab");
      str.removeRange(0, 3);
      VertxAssert.fail();
    } catch (StringIndexOutOfBoundsException e) {
    }

    VertxAssert.testComplete();
  }

  @Test
  public void testInitialize() {
    VertxAssert.assertSame(str, mod.getObject(str.getId()));
    VertxAssert.assertEquals("", str.getText());

    str = mod.createString("abcd");
    VertxAssert.assertEquals("abcd", str.getText());

    VertxAssert.testComplete();
  }

  @Test
  public void testInsertRemove() {
    str.insertString(0, "abcdef");
    VertxAssert.assertEquals(6, str.length());
    str.removeRange(2, 5);
    VertxAssert.assertEquals(3, str.length());
    str.append("gh");
    VertxAssert.assertEquals("abfgh", str.getText());

    VertxAssert.testComplete();
  }

  @Test
  public void testRegisterReferenceCanBeDeleted() {
    str.append("ab");
    final JsonArray events = Json.createArray();
    IndexReference indexReference = str.registerReference(1, true);
    indexReference.addReferenceShiftedListener(new Handler<ReferenceShiftedEvent>() {
      @Override
      public void handle(ReferenceShiftedEvent event) {
        events.push(event);
      }
    });
    indexReference.addObjectChangedListener(new Handler<ObjectChangedEvent>() {
      @Override
      public void handle(ObjectChangedEvent event) {
        assertArraySame(events, event.events);
        VertxAssert.assertEquals(1, events.<ReferenceShiftedEvent> get(0).oldIndex);
        VertxAssert.assertEquals(2, events.<ReferenceShiftedEvent> get(0).newIndex);
        VertxAssert.assertEquals(2, events.<ReferenceShiftedEvent> get(1).oldIndex);
        VertxAssert.assertEquals(1, events.<ReferenceShiftedEvent> get(1).newIndex);
        VertxAssert.assertEquals(1, events.<ReferenceShiftedEvent> get(2).oldIndex);
        VertxAssert.assertEquals(-1, events.<ReferenceShiftedEvent> get(2).newIndex);

        VertxAssert.testComplete();
      }
    });
    str.insertString(2, "2"); // "ab2"
    str.insertString(1, "1"); // "a1b2" events[0]
    str.removeRange(3, 4);// "a1b"
    str.removeRange(1, 2); // "ab" events[1]
    str.removeRange(1, 2); // "a" events[2]
  }

  @Test
  public void testRegisterReferenceCannotBeDeleted() {
    str.append("ab");
    final JsonArray events = Json.createArray();
    IndexReference indexReference = str.registerReference(1, false);
    indexReference.addReferenceShiftedListener(new Handler<ReferenceShiftedEvent>() {
      @Override
      public void handle(ReferenceShiftedEvent event) {
        events.push(event);
      }
    });
    indexReference.addObjectChangedListener(new Handler<ObjectChangedEvent>() {
      @Override
      public void handle(ObjectChangedEvent event) {
        assertArraySame(events, event.events);
        VertxAssert.assertEquals(1, events.<ReferenceShiftedEvent> get(0).oldIndex);
        VertxAssert.assertEquals(2, events.<ReferenceShiftedEvent> get(0).newIndex);
        VertxAssert.assertEquals(2, events.<ReferenceShiftedEvent> get(1).oldIndex);
        VertxAssert.assertEquals(1, events.<ReferenceShiftedEvent> get(1).newIndex);
        VertxAssert.assertEquals(1, events.<ReferenceShiftedEvent> get(2).oldIndex);
        VertxAssert.assertEquals(1, events.<ReferenceShiftedEvent> get(2).newIndex);

        VertxAssert.testComplete();
      }
    });
    str.insertString(2, "2"); // "ab2"
    str.insertString(1, "1"); // "a1b2" events[0]
    str.removeRange(3, 4);// "a1b"
    str.removeRange(1, 2); // "ab" events[1]
    str.removeRange(1, 2); // "a" events[2]
  }

  // @Test
  public void testSetText() {
    str.setText("abc");
    VertxAssert.assertEquals("abc", str.getText());
    str.setText("ddabd");
    VertxAssert.assertEquals("ddabd", str.getText());

    VertxAssert.testComplete();
  }
}

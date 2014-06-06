/*
 * Copyright 2014 Goodow.com
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

import com.goodow.realtime.core.Handler;
import com.goodow.realtime.core.Registration;
import com.goodow.realtime.json.JsonArray;
import com.goodow.realtime.json.JsonObject;
import com.google.gwt.core.client.js.JsInterface;
import com.google.gwt.core.client.js.JsProperty;

//@JsInterface
/**
 * A collaborative map. A map's key must be a string. The values can contain other Realtime
 * collaborative objects, custom collaborative objects, primitive values or objects that can be
 * serialized to JSON.
 * <p>
 * Changes to the map will automatically be synced with the server and other collaborators. To
 * listen for changes, add EventListeners for the
 * {@link com.goodow.realtime.store.EventType#VALUE_CHANGED} event type.
 * <p>
 * This class should not be instantiated directly. To create a new map, use
 * {@link com.goodow.realtime.store.Model#createMap(JsonObject)}.
 */
public interface CollaborativeMap extends CollaborativeObject {
  Registration onValueChanged(Handler<ValueChangedEvent> handler);

  /**
   * Removes all entries.
   */
  void clear();

  /**
   * Returns the value mapped to the given key.
   *
   * @param key The key to look up.
   * @return The value mapped to the given key.
   * @exception IllegalArgumentException
   */
  <T> T get(String key);

  /**
   * Checks if this map contains an entry for the given key.
   *
   * @param key The key to check.
   * @return Returns true if this map contains a mapping for the given key.
   * @exception IllegalArgumentException
   */
  boolean has(String key);

  /**
   * Returns whether this map is empty.
   *
   * @return Returns true if this map is empty.
   */
  boolean isEmpty();

  /**
   * Returns an array containing a copy of the items in this map. Modifications to the returned
   * array do not modify this collaborative map.
   *
   * @return The items in this map. Each item is a [key, value] pair.
   */
  JsonArray items();

  /**
   * Returns an array containing a copy of the keys in this map. Modifications to the returned array
   * do not modify this collaborative map.
   *
   * @return The keys in this map.
   */
  JsonArray keys();

  /**
   * Removes the entry for the given key (if such an entry exists).
   *
   * @param key The key to unmap.
   * @return The value that was mapped to this key, or null if there was no existing value.
   * @exception IllegalArgumentException
   */
  <T> T remove(String key);

  /**
   * Put the value into the map with the given key, overwriting an existing value for that key.
   *
   * @param key The map key.
   * @param value The map value.
   * @return The old map value, if any, that used to be mapped to the given key.
   * @exception IllegalArgumentException
   */
  <T> T set(String key, Object value);

  /**
   * Returns an array containing a copy of the values in this map. Modifications to the returned
   * array do not modify this collaborative map.
   *
   * @return The values in this map.
   */
  JsonArray values();

  @JsProperty
  /**
   * @return The number of keys in the map.
   */
  int size();
}

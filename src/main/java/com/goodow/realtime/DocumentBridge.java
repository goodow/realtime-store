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

import elemental.json.JsonArray;

public class DocumentBridge {
  public Document create(JsonArray snapshot) {
    Document document = new Document(this, null, null);
    Model model = document.getModel();
    for (int i = 0, len = snapshot.length(); i < len; i++) {
      JsonArray array = snapshot.getArray(i);
      String key = array.getString(0);
      switch ((int) array.getNumber(1)) {
        case 0:
          CollaborativeMap map = new CollaborativeMap(model);
          map.initialize(key, array.getObject(2));
          break;
        case 1:
          CollaborativeList list = new CollaborativeList(model);
          list.initialize(key, array.getArray(2));
          break;
        case 2:
          CollaborativeString string = new CollaborativeString(model);
          string.initialize(key, array.getString(2));
        case 4:
          JsonArray idxRef = array.getArray(2);
          IndexReference indexReference =
              new IndexReference(model, model.getObject(idxRef.getString(0)), idxRef.getBoolean(2));
          indexReference.initialize(key, (int) idxRef.getNumber(1));
        default:
          throw new RuntimeException("Shouldn't reach here!");
      }
    }
    if (model.getRoot() == null) {
      model.createRoot();
    }
    return document;
  }
}

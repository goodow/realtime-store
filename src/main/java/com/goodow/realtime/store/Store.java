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

import com.goodow.realtime.channel.Bus;
import com.goodow.realtime.core.Handler;
import com.google.gwt.core.client.js.JsInterface;
import com.google.gwt.core.client.js.JsProperty;

/**
 * The Goodow Realtime Store API.
 */
@JsInterface
public interface Store {
  void close();

  Bus getBus();

  @JsProperty
  String sessionId();

  @JsProperty
  String userId();

  /**
   * Loads the realtime data model associated with {@code docId}. If no realtime data model is
   * associated with {@code docId}, a new realtime document will be created and
   * {@code opt_initializer} will be called (if it is provided).
   * 
   * @param id The ID of the document to load.
   * @param onLoaded A callback that will be called when the realtime document is ready. The created
   *          or opened realtime document object will be passed to this function.
   * @param opt_initializer An optional initialization function that will be called before
   *          {@code onLoaded} only the first time that the document is loaded. The document's
   *          {@link com.goodow.realtime.store.Model} object will be passed to this function.
   * @param opt_error An optional error handling function that will be called if an error occurs
   *          while the document is being loaded or edited. A
   *          {@link com.goodow.realtime.store.Error} object describing the error will be passed to
   *          this function.
   */
  void load(String id, Handler<Document> onLoaded, Handler<Model> opt_initializer,
      Handler<Error> opt_error);
}

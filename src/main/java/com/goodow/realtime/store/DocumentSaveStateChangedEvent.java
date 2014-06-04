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

import com.google.gwt.core.client.js.JsInterface;
import com.google.gwt.core.client.js.JsProperty;

@JsInterface
/**
 * An event that indicates that the save state of a document has changed. If both isSaving and
 * isPending are false, the document is completely saved and up to date.
 */
public interface DocumentSaveStateChangedEvent {
  @JsProperty
  /**
   * If true, the client has mutations that have not yet been sent to the server. If false, all
   * mutations have been sent to the server, but some may not yet have been acked.
   */
  boolean isPending();

  @JsProperty
  /**
   * If true, the document is in the process of saving. Mutations have been sent to the server, but
   * we have not yet received an ack. If false, nothing is in the process of being sent.
   */
  boolean isSaving();
}

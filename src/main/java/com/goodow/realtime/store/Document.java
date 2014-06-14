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
import com.google.gwt.core.client.js.JsInterface;

@JsInterface
/**
 * A Realtime document. A document consists of a Realtime model and a set of collaborators. Listen
 * on the document for the following events:
 * <ul>
 * <li>{@link com.goodow.realtime.store.EventType#COLLABORATOR_LEFT}
 * <li>{@link com.goodow.realtime.store.EventType#COLLABORATOR_JOINED}
 * <li>{@link com.goodow.realtime.store.EventType#DOCUMENT_SAVE_STATE_CHANGED}
 * </ul>
 * <p>
 * This class should not be instantiated directly. The document object is generated during the
 * document load process.
 */
public interface Document {
  Registration onCollaboratorJoined(Handler<CollaboratorJoinedEvent> handler);

  Registration onCollaboratorLeft(Handler<CollaboratorLeftEvent> handler);

  Registration onDocumentSaveStateChanged(Handler<DocumentSaveStateChangedEvent> handler);

  /**
   * Closes the document and disconnects from the server. After this function is called, event
   * listeners will no longer fire and attempts to access the document, model, or model objects will
   * throw a {@link DocumentClosedError}. Calling this function after the
   * document has been closed will have no effect.
   *
   * @throws com.goodow.realtime.store.DocumentClosedError
   */
  void close();

  /**
   * Gets an array of collaborators active in this session. Each collaborator has these properties:
   * sessionId, userId, displayName, color, isMe, isAnonymous, photoUrl.
   *
   * @return An array of collaborators.
   */
  JsonArray getCollaborators();

  /**
   * Gets the collaborative model associated with this document.
   *
   * @return The collaborative model for this document.
   */
  Model getModel();
}

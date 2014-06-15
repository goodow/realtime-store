package com.goodow.realtime.store;

import com.goodow.realtime.core.Handler;
import com.goodow.realtime.core.Registration;
import com.goodow.realtime.json.JsonArray;
import com.goodow.realtime.json.JsonObject;
import com.google.gwt.core.client.js.JsType;
import com.google.gwt.core.client.js.JsProperty;

@JsType
/**
 * The collaborative model is the data model for a Realtime document. The document's object graph
 * should be added to the model under the root object. All objects that are part of the model must
 * be accessible from this root.
 * <p>
 * The model class is also used to create instances of built in and custom collaborative objects via
 * the appropriate create method.
 * <p>
 * Listen on the model for the following events:
 * <ul>
 * <li>{@link com.goodow.realtime.store.EventType#UNDO_REDO_STATE_CHANGED}
 * </ul>
 * <p>
 * This class should not be instantiated directly. The collaborative model is generated during the
 * document load process. The model can be initialized by passing an initializer function to
 * {@link com.goodow.realtime.store.Store#load(String, Handler, Handler, Handler)}.
 */
public interface Model {
  Registration onUndoRedoStateChanged(Handler<UndoRedoStateChangedEvent> handler);

  /**
   * Creates a collaborative list.
   *
   * @param opt_initialValue Initial value for the list.
   * @return A collaborative list.
   */
  CollaborativeList createList(JsonArray opt_initialValue);

  /**
   * Creates a collaborative map.
   *
   * @param opt_initialValue Initial value for the map.
   * @return A collaborative map.
   */
  CollaborativeMap createMap(JsonObject opt_initialValue);

  /**
   * Creates a collaborative string.
   *
   * @param opt_initialValue Sets the initial value for this string.
   * @return A collaborative string.
   */
  CollaborativeString createString(String opt_initialValue);

  /**
   * @return The root of the object model.
   */
  CollaborativeMap getRoot();

  /**
   * Redo the last thing the active collaborator undid.
   */
  void redo();

  /**
   * Undo the last thing the active collaborator did.
   */
  void undo();

  @JsProperty
  /**
   * @return True if the model can currently redo.
   */
  boolean canRedo();

  @JsProperty
  /**
   * @return True if the model can currently undo.
   */
  boolean canUndo();
}

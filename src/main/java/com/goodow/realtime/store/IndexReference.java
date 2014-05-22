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

import com.goodow.realtime.core.Handler;
import com.goodow.realtime.core.Registration;
import com.goodow.realtime.json.Json;
import com.goodow.realtime.json.JsonObject;
import com.goodow.realtime.operation.OperationComponent;
import com.goodow.realtime.operation.create.CreateComponent;
import com.goodow.realtime.operation.cursor.ReferenceShiftedComponent;
import com.goodow.realtime.store.util.ModelFactory;

import com.google.common.annotations.GwtIncompatible;

import org.timepedia.exporter.client.Export;
import org.timepedia.exporter.client.ExportAfterCreateMethod;
import org.timepedia.exporter.client.ExportPackage;

/**
 * An IndexReference is a pointer to a specific location in a collaborative list or string. This
 * pointer automatically shifts as new elements are added to and removed from the object.
 * <p>
 * To listen for changes to the referenced index, add an EventListener for
 * <ul>
 * <li>{@link com.goodow.realtime.store.EventType#REFERENCE_SHIFTED}
 * </ul>
 * <p>
 * This class should not be instantiated directly. To create an index reference, call
 * registerReference on the appropriate string or list.
 */
@ExportPackage(ModelFactory.PACKAGE_PREFIX_REALTIME)
@Export(all = true)
public class IndexReference extends CollaborativeObject {
  @GwtIncompatible(ModelFactory.JS_REGISTER_PROPERTIES)
  @ExportAfterCreateMethod
  // @formatter:off
  public native static void __jsniRunAfter__() /*-{
//    var _ = $wnd.good.realtime.IndexReference.prototype;
//    Object.defineProperties(_, {
//      id : {
//        get : function() {
//          return this.g.@com.goodow.realtime.store.CollaborativeObject::id;
//        }
//      },
//      canBeDeleted : {
//        get : function() {
//          return this.g.@com.goodow.realtime.store.IndexReference::canBeDeleted()();
//        }
//      },
//      index : {
//        get : function() {
//          return this.g.@com.goodow.realtime.store.IndexReference::index()();
//        },
//        set : function(index) {
//          this.g.@com.goodow.realtime.store.IndexReference::setIndex(I)(index);
//        }
//      },
//      referencedObject : {
//        get : function() {
//          var v = this.g.@com.goodow.realtime.store.IndexReference::referencedObject()();
//          return @org.timepedia.exporter.client.ExporterUtil::wrap(Ljava/lang/Object;)(v);
//        }
//      }
//    });
  }-*/;
  // @formatter:on

  private String referencedObjectId;
  private int index = -1;
  private boolean canBeDeleted;

  /**
   * @param model The document model.
   */
  IndexReference(Model model) {
    super(model);
  }

  public Registration addReferenceShiftedListener(Handler<ReferenceShiftedEvent> handler) {
    return addEventListener(EventType.REFERENCE_SHIFTED, handler, false);
  }

  /**
   * @return Whether this reference can be deleted. Read-only. This property affects the behavior of
   *         the index reference when the index the reference points to is deleted. If this is true,
   *         the index reference will be deleted. If it is false, the index reference will move to
   *         point at the beginning of the deleted range.
   */
  public boolean canBeDeleted() {
    return canBeDeleted;
  }

  /**
   * @return The index of the current location the reference points to. Write to this property to
   *         change the referenced index.
   */
  public int getIndex() {
    return index;
  }

  /**
   * @return The object this reference points to. Read-only.
   */
  public <T extends CollaborativeObject> T getReferencedObject() {
    return model.getObject(referencedObjectId);
  }

  /**
   * Change the referenced index.
   * 
   * @see #index()
   * @param index the new referenced index.
   */
  public void setIndex(int index) {
    if (index == this.index) {
      return;
    }
    ReferenceShiftedComponent op =
        new ReferenceShiftedComponent(id, referencedObjectId, index, canBeDeleted, this.index);
    consumeAndSubmit(op);
  }

  @Override
  public JsonObject toJson() {
    JsonObject json = Json.createObject();
    json.set("id", id).set("referencedObjectId", referencedObjectId).set("index", index).set(
        "canBeDeleted", canBeDeleted);
    return json;
  }

  @Override
  protected void consume(final String userId, final String sessionId,
      OperationComponent<?> component) {
    ReferenceShiftedComponent op = (ReferenceShiftedComponent) component;
    assert op.oldIndex == getIndex();
    referencedObjectId = op.referencedObjectId;
    index = op.newIndex;
    canBeDeleted = op.canBeDeleted;
    if (op.oldIndex != -1) {
      ReferenceShiftedEvent event =
          new ReferenceShiftedEvent(event(sessionId, userId).set("oldIndex", op.oldIndex).set(
              "newIndex", op.newIndex));
      fireEvent(event);
    }
  }

  void setIndex(boolean isInsert, int index, int length, String sessionId, String userId) {
    int cursor = getIndex();
    if (cursor < index) {
      return;
    }
    int newIndex = -2;
    if (isInsert) {
      newIndex = cursor + length;
    } else {
      if (cursor < index + length) {
        if (canBeDeleted) {
          newIndex = -1;
        } else {
          newIndex = index;
        }
      } else {
        newIndex = cursor - length;
      }
    }
    ReferenceShiftedComponent op =
        new ReferenceShiftedComponent(id, referencedObjectId, newIndex, canBeDeleted, cursor);
    consume(userId, sessionId, op);
  }

  @Override
  OperationComponent<?>[] toInitialization() {
    ReferenceShiftedComponent op =
        new ReferenceShiftedComponent(id, referencedObjectId, index, canBeDeleted, index);
    return new OperationComponent[] {new CreateComponent(id, CreateComponent.INDEX_REFERENCE), op};
  }
}

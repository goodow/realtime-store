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

import com.goodow.realtime.model.util.ModelFactory;
import com.goodow.realtime.operation.CreateOperation;
import com.goodow.realtime.operation.Operation;
import com.goodow.realtime.operation.RealtimeOperation;
import com.goodow.realtime.operation.ReferenceShiftedOperation;

import com.google.common.annotations.GwtIncompatible;

import org.timepedia.exporter.client.Export;
import org.timepedia.exporter.client.ExportAfterCreateMethod;
import org.timepedia.exporter.client.ExportPackage;

import java.util.Set;

/**
 * An IndexReference is a pointer to a specific location in a collaborative list or string. This
 * pointer automatically shifts as new elements are added to and removed from the object.
 * <p>
 * To listen for changes to the referenced index, add an EventListener for
 * <ul>
 * <li>{@link com.goodow.realtime.EventType#REFERENCE_SHIFTED}
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
//          return this.g.@com.goodow.realtime.CollaborativeObject::id;
//        }
//      },
//      canBeDeleted : {
//        get : function() {
//          return this.g.@com.goodow.realtime.IndexReference::canBeDeleted()();
//        }
//      },
//      index : {
//        get : function() {
//          return this.g.@com.goodow.realtime.IndexReference::index()();
//        },
//        set : function(index) {
//          this.g.@com.goodow.realtime.IndexReference::setIndex(I)(index);
//        }
//      },
//      referencedObject : {
//        get : function() {
//          var v = this.g.@com.goodow.realtime.IndexReference::referencedObject()();
//          return @org.timepedia.exporter.client.ExporterUtil::wrap(Ljava/lang/Object;)(v);
//        }
//      }
//    });
  }-*/;
  // @formatter:on

  private String referencedObject;
  private int index = -1;
  private boolean canBeDeleted;

  /**
   * @param model The document model.
   */
  IndexReference(Model model) {
    super(model);
  }

  public void addReferenceShiftedListener(EventHandler<ReferenceShiftedEvent> handler) {
    addEventListener(EventType.REFERENCE_SHIFTED, handler, false);
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
  public CollaborativeObject getReferencedObject() {
    return model.getObject(referencedObject);
  }

  public void removeReferenceShiftedListener(EventHandler<ReferenceShiftedEvent> handler) {
    removeEventListener(EventType.REFERENCE_SHIFTED, handler, false);
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
    ReferenceShiftedOperation op =
        new ReferenceShiftedOperation(referencedObject, index, canBeDeleted, this.index);
    consumeAndSubmit(op);
  }

  @Override
  protected void consume(RealtimeOperation<?> operation) {
    ReferenceShiftedOperation op = (ReferenceShiftedOperation) operation.<Void> getOp();
    assert op.oldIndex == getIndex();
    ReferenceShiftedEvent event =
        new ReferenceShiftedEvent(this, op.oldIndex, op.newIndex, operation.sessionId,
            operation.userId);
    referencedObject = op.referencedObject;
    index = op.newIndex;
    canBeDeleted = op.canBeDeleted;
    fireEvent(event);
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
    ReferenceShiftedOperation op =
        new ReferenceShiftedOperation(referencedObject, newIndex, canBeDeleted, cursor);
    op.setId(id);
    RealtimeOperation<Void> operation = new RealtimeOperation<Void>(op, userId, sessionId);
    consume(operation);
  }

  @Override
  Operation<?>[] toInitialization() {
    ReferenceShiftedOperation op =
        new ReferenceShiftedOperation(referencedObject, index, canBeDeleted, index);
    return new Operation[] {new CreateOperation(CreateOperation.INDEX_REFERENCE, id), op};
  }

  @Override
  void toString(Set<String> seen, StringBuilder sb) {
    if (seen.contains(id)) {
      sb.append("<IndexReference: ").append(id).append(">");
      return;
    }
    seen.add(id);
    sb.append("DefaultIndexReference [");
    sb.append("id=").append(getId());
    sb.append(", objectId=").append(referencedObject);
    sb.append(", index=").append(getIndex());
    sb.append(", canBeDeleted=").append(canBeDeleted);
    sb.append("]");
  }
}

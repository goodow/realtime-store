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

import com.goodow.realtime.operation.Operation;
import com.goodow.realtime.operation.RealtimeOperation;
import com.goodow.realtime.util.NativeInterfaceFactory;

import com.google.common.annotations.GwtIncompatible;

import org.timepedia.exporter.client.Export;
import org.timepedia.exporter.client.ExportAfterCreateMethod;
import org.timepedia.exporter.client.ExportPackage;
import org.timepedia.exporter.client.NoExport;

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
@ExportPackage(NativeInterfaceFactory.PACKAGE_PREFIX_REALTIME)
@Export(all = true)
public class IndexReference extends CollaborativeObject {
  @GwtIncompatible(NativeInterfaceFactory.JS_REGISTER_PROPERTIES)
  @ExportAfterCreateMethod
  public native static void __jsRunAfter__() /*-{
    var _ = $wnd.gdr.IndexReference.prototype;
    Object.defineProperties(_, {
      id : {
        get : function() {
          return this.g.@com.goodow.realtime.CollaborativeObject::id;
        }
      },
      index : {
        get : function() {
          return this.g.@com.goodow.realtime.IndexReference::index()();
        },
        set : function(index) {
          this.g.@com.goodow.realtime.IndexReference::setIndex(I)(index);
        }
      }
    });
  }-*/;

  /**
   * Whether this reference can be deleted. Read-only. This property affects the behavior of the
   * index reference when the index the reference points to is deleted. If this is true, the index
   * reference will be deleted. If it is false, the index reference will move to point at the
   * beginning of the deleted range.
   */
  public final boolean canBeDeleted;
  /**
   * The object this reference points to. Read-only.
   */
  public final CollaborativeObject referencedObject;
  private int index;

  /**
   * @param model The document model.
   */
  IndexReference(Model model, CollaborativeObject referencedObject, boolean canBeDeleted) {
    super(model);
    this.canBeDeleted = canBeDeleted;
    this.referencedObject = referencedObject;
  }

  public void addReferenceShiftedListener(EventHandler<ReferenceShiftedEvent> handler) {
    addEventListener(EventType.REFERENCE_SHIFTED, handler, false);
  }

  /**
   * @return The index of the current location the reference points to. Write to this property to
   *         change the referenced index.
   */
  @NoExport
  public int index() {
    return index;
  }

  public void removeReferenceShiftedListener(EventHandler<ReferenceShiftedEvent> handler) {
    removeEventListener(EventType.REFERENCE_SHIFTED, handler, false);
  }

  public void setIndex(int index) {
    ReferenceShiftedEvent event =
        new ReferenceShiftedEvent(this, this.index, index, model.document.sessionId, Realtime
            .getUserId());
    this.index = index;
    fireEvent(event);
  }

  @Override
  protected void consume(RealtimeOperation operation) {
    throw new UnsupportedOperationException();
  }

  void initialize(String id, int index) {
    this.id = id;
    this.index = index;
    model.objects.put(id, this);
    model.registerIndexReference(id, referencedObject.id);
  }

  void setIndex(boolean isInsert, int index, int length, String sessionId, String userId) {
    int cursor = index();
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
    ReferenceShiftedEvent event =
        new ReferenceShiftedEvent(this, cursor, newIndex, sessionId, userId);
    this.index = newIndex;
    fireEvent(event);
  }

  @Override
  Operation<?> toInitialization() {
    return null;
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
    sb.append(", objectId=").append(referencedObject.getId());
    sb.append(", index=").append(index());
    sb.append(", canBeDeleted=").append(canBeDeleted);
    sb.append("]");
  }
}

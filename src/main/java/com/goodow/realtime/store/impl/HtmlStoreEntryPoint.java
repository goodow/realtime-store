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
package com.goodow.realtime.store.impl;

import com.google.gwt.core.client.EntryPoint;


class HtmlStoreEntryPoint implements EntryPoint {

  @Override
  public void onModuleLoad() {
    patchPrototype(BaseModelEventImpl.class, "BaseModelEvent");
    patchPrototype(CollaborativeListImpl.class, "CollaborativeList");
    patchPrototype(CollaborativeMapImpl.class, "CollaborativeMap");
    patchPrototype(CollaborativeObjectImpl.class, "CollaborativeObject");
    patchPrototype(CollaborativeStringImpl.class, "CollaborativeString");
    patchPrototype(CollaboratorImpl.class, "Collaborator");
    patchPrototype(CollaboratorJoinedEventImpl.class, "CollaboratorJoinedEvent");
    patchPrototype(CollaboratorLeftEventImpl.class, "CollaboratorLeftEvent");
    patchPrototype(DocumentImpl.class, "Document");
//    patchPrototype(DocumentClosedError.class, "DocumentClosedError");
    patchPrototype(DocumentSaveStateChangedEventImpl.class, "DocumentSaveStateChangedEvent");
    patchPrototype(ErrorImpl.class, "Error");
//    patchPrototype(ErrorType.class, "ErrorType");
//    patchPrototype(EventTarget.class, "EventTarget");
//    patchPrototype(EventType.class, "EventType");
    patchPrototype(IndexReferenceImpl.class, "IndexReference");
    patchPrototype(ModelImpl.class, "Model");
    patchPrototype(ObjectChangedEventImpl.class, "ObjectChangedEvent");
    patchPrototype(ReferenceShiftedEventImpl.class, "ReferenceShiftedEvent");
    patchPrototype(StoreImpl.class, "Store");
    patchPrototype(TextDeletedEventImpl.class, "TextDeletedEvent");
    patchPrototype(TextInsertedEventImpl.class, "TextInsertedEvent");
    patchPrototype(UndoRedoStateChangedEventImpl.class, "UndoRedoStateChangedEvent");
    patchPrototype(ValueChangedEventImpl.class, "ValueChangedEvent");
    patchPrototype(ValuesAddedEventImpl.class, "ValuesAddedEvent");
    patchPrototype(ValuesRemovedEventImpl.class, "ValuesRemovedEvent");
    patchPrototype(ValuesSetEventImpl.class, "ValuesSetEvent");
  }

  private void patchPrototype(Class<?> myClass) {
    patchPrototype(myClass, myClass.getSimpleName());
  }

  private native void patchPrototype(Class<?> myClass, String name) /*-{
    $wnd.realtime.store[name] = function() {};
    $wnd.realtime.store[name].prototype = @java.lang.Class::getPrototypeForClass(Ljava/lang/Class;)(myClass);
  }-*/;
}

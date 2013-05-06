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

import com.goodow.realtime.util.NativeInterfaceFactory;

import org.timepedia.exporter.client.Export;
import org.timepedia.exporter.client.ExportPackage;

/**
 * An event that indicates that the save state of a document has changed. If both isSaving and
 * isPending are false, the document is completely saved and up to date.
 */
@ExportPackage(NativeInterfaceFactory.PACKAGE_PREFIX_REALTIME)
@Export
public class DocumentSaveStateChangedEvent implements Disposable {
  /**
   * If true, the client has mutations that have not yet been sent to the server. If false, all
   * mutations have been sent to the server, but some may not yet have been acked.
   */
  public final boolean isPending;
  /**
   * If true, the document is in the process of saving. Mutations have been sent to the server, but
   * we have not yet received an ack. If false, nothing is in the process of being sent.
   */
  public final boolean isSaving;

  /**
   * @param document The document being saved.
   * @param isSaving The saving state.
   * @param isPending The state of pending mutations.
   */
  public DocumentSaveStateChangedEvent(Document document, boolean isSaving, boolean isPending) {
    this.isSaving = isSaving;
    this.isPending = isPending;
  }
}

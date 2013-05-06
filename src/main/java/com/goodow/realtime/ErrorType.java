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
import org.timepedia.exporter.client.Exportable;

/**
 * Errors that can occur while loading or collaborating on a document.
 */
@ExportPackage(NativeInterfaceFactory.PACKAGE_PREFIX_REALTIME)
@Export
public enum ErrorType implements Exportable {
  /**
   * An internal error occurred in the Drive Realtime API client.
   */
  CLIENT_ERROR,
  /**
   * Another user created the document's initial state after gapi.drive.realtime.load was called but
   * before the local creation was saved.
   */
  CONCURRENT_CREATION,
  /**
   * The user associated with the provided OAuth token is not authorized to access the provided
   * document ID.
   */
  FORBIDDEN,
  /**
   * A compound operation was still open at the end of a synchronous block. Compound operations must
   * always be ended in the same synchronous block that they are started.
   */
  INVALID_COMPOUND_OPERATION,
  /**
   * The provided document ID could not be found.
   */
  NOT_FOUND,
  /**
   * An internal error occurred in the Drive Realtime API server.
   */
  SERVER_ERROR,
  /**
   * The provided OAuth token is no longer valid and must be refreshed.
   */
  TOKEN_REFRESH_REQUIRED,
}

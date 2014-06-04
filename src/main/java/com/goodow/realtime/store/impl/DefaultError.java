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
package com.goodow.realtime.store.impl;

import com.goodow.realtime.store.ErrorType;

public class DefaultError extends RuntimeException implements com.goodow.realtime.store.Error {
  /**
   * Whether the error is fatal. Fatal errors cannot be recovered from and require the document to
   * be reloaded.
   */
  public final boolean isFatal;
  public final String message;
  /**
   * The type of the error that occurred.
   */
  public final ErrorType type;

  /**
   * @param type The type of the error that occurred.
   * @param message A message describing the error.
   * @param isFatal Whether the error is fatal.
   */
  public DefaultError(ErrorType type, String message, boolean isFatal) {
    super(message);
    this.type = type;
    this.message = message;
    this.isFatal = isFatal;
  }

  @Override public String message() {
    return message;
  }

  @Override public ErrorType type() {
    return type;
  }

  @Override public boolean isFatal() {
    return isFatal;
  }
}

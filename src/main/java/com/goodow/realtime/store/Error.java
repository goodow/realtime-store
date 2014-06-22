package com.goodow.realtime.store;

import com.google.gwt.core.client.js.JsType;

@JsType
/**
 * An error affecting the realtime document.
 */
public interface Error {
  /* Whether the error is fatal. Fatal errors cannot be recovered from and require the document to be reloaded. */
  boolean isFatal();

  /* A message describing the error. */
  String message();

  /* The type of the error that occurred. */
  ErrorType type();
}

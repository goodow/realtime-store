package com.goodow.realtime.store;

import com.google.gwt.core.client.js.JsInterface;
import com.google.gwt.core.client.js.JsProperty;

@JsInterface
/**
 * An error affecting the realtime document.
 */
public interface Error {
  @JsProperty
  /* Whether the error is fatal. Fatal errors cannot be recovered from and require the document to be reloaded. */
  boolean isFatal();

  @JsProperty
  /* A message describing the error. */
  String message();

  @JsProperty
  /* The type of the error that occurred. */
  ErrorType type();
}

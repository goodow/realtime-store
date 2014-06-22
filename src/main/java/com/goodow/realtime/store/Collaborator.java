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
package com.goodow.realtime.store;

import com.google.gwt.core.client.js.JsType;

@JsType
/**
 * A collaborator on the document.
 */
public interface Collaborator {
  /* The color associated with the collaborator. */
  String color();

  /* The display name of the collaborator. */
  String displayName();

  /* True if the collaborator is anonymous, false otherwise. */
  boolean isAnonymous();

  /* True if the collaborator is the local user, false otherwise. */
  boolean isMe();

  /* A url that points to the profile photo of the user. */
  String photoUrl();

  /* The sessionId of the collaborator. */
  String sessionId();

  /* The userId of the collaborator. */
  String userId();
}

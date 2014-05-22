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

import com.goodow.realtime.json.JsonObject;
import com.goodow.realtime.store.channel.Constants.Key;
import com.goodow.realtime.store.util.ModelFactory;

import org.timepedia.exporter.client.Export;
import org.timepedia.exporter.client.ExportPackage;
import org.timepedia.exporter.client.Exportable;

/**
 * A collaborator on the document.
 */
@ExportPackage(ModelFactory.PACKAGE_PREFIX_REALTIME)
@Export
public class Collaborator implements Exportable {
  /**
   * The color associated with the collaborator.
   */
  public final String color;
  /**
   * The display name of the collaborator.
   */
  public final String displayName;
  /**
   * True if the collaborator is anonymous, false otherwise.
   */
  public final boolean isAnonymous;
  /**
   * True if the collaborator is the local user, false otherwise.
   */
  public final boolean isMe;
  /**
   * A url that points to the profile photo of the user.
   */
  public final String photoUrl;
  /**
   * The sessionId of the collaborator.
   */
  public final String sessionId;
  /**
   * The userId of the collaborator.
   */
  public final String userId;

  /**
   * @param serialized A serialized collaborator object
   */
  public Collaborator(JsonObject serialized) {
    this.userId = serialized.getString(Key.USER_ID);
    this.sessionId = serialized.getString(Key.SESSION_ID);
    this.displayName = serialized.getString(Key.DISPLAY_NAME);
    this.color = serialized.getString(Key.COLOR);
    this.isMe = serialized.getBoolean(Key.IS_ME);
    this.isAnonymous = serialized.getBoolean(Key.IS_ANONYMOUS);
    this.photoUrl = serialized.getString(Key.PHOTO_URL);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    Collaborator other = (Collaborator) obj;
    if (sessionId == null) {
      if (other.sessionId != null) {
        return false;
      }
    } else if (!sessionId.equals(other.sessionId)) {
      return false;
    }
    if (userId == null) {
      if (other.userId != null) {
        return false;
      }
    } else if (!userId.equals(other.userId)) {
      return false;
    }
    return true;
  }

  public String getColor() {
    return color;
  }

  public String getDisplayName() {
    return displayName;
  }

  public String getPhotoUrl() {
    return photoUrl;
  }

  public String getSessionId() {
    return sessionId;
  }

  public String getUserId() {
    return userId;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((sessionId == null) ? 0 : sessionId.hashCode());
    result = prime * result + ((userId == null) ? 0 : userId.hashCode());
    return result;
  }

  public boolean isAnonymous() {
    return isAnonymous;
  }

  public boolean isMe() {
    return isMe;
  }
}
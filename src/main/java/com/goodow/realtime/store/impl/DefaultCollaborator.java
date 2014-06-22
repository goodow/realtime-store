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

import com.goodow.realtime.json.JsonObject;
import com.goodow.realtime.store.Collaborator;
import com.goodow.realtime.store.channel.Constants.Key;

public class DefaultCollaborator implements Collaborator {
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
  public DefaultCollaborator(JsonObject serialized) {
    this.userId = serialized.getString("userId");
    this.sessionId = serialized.getString("sessionId");
    this.displayName = serialized.getString("displayName");
    this.color = serialized.getString("color");
    this.isMe = serialized.getBoolean(Key.IS_ME);
    this.isAnonymous = serialized.getBoolean("isAnonymous");
    this.photoUrl = serialized.getString("photoUrl");
  }

  public DefaultCollaborator(String userId, String sessionId, String displayName, String color,
                      boolean isMe, boolean isAnonymous, String photoUrl) {
    this.userId = userId;
    this.sessionId = sessionId;
    this.displayName = displayName;
    this.color = color;
    this.isMe = isMe;
    this.isAnonymous = isAnonymous;
    this.photoUrl = photoUrl;
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
      if (other.sessionId() != null) {
        return false;
      }
    } else if (!sessionId.equals(other.sessionId())) {
      return false;
    }
    if (userId == null) {
      if (other.userId() != null) {
        return false;
      }
    } else if (!userId.equals(other.userId())) {
      return false;
    }
    return true;
  }

  @Override public String color() {
    return color;
  }

  @Override public String displayName() {
    return displayName;
  }

  @Override public String photoUrl() {
    return photoUrl;
  }

  @Override public String sessionId() {
    return sessionId;
  }

  @Override public String userId() {
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

  @Override public boolean isAnonymous() {
    return isAnonymous;
  }

  @Override public boolean isMe() {
    return isMe;
  }
}
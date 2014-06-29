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
package com.goodow.realtime.store.server.impl;

import com.google.inject.Inject;

import com.goodow.realtime.channel.impl.WebSocketBus;
import com.goodow.realtime.channel.server.impl.BridgeHook;
import com.goodow.realtime.json.impl.JacksonUtil;
import com.goodow.realtime.json.impl.JreJsonObject;
import com.goodow.realtime.store.Collaborator;
import com.goodow.realtime.store.channel.Constants.Key;
import com.goodow.realtime.store.channel.Constants.Topic;
import com.goodow.realtime.store.impl.CollaboratorImpl;

import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.impl.CountingCompletionHandler;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Container;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PresenceHandler {
  @Inject private Vertx vertx;
  @Inject private Container container;
  @Inject private AnonymousUsers anonymousUsers;
  private String address;
  private Map<String, JsonObject> collaborators = new HashMap<String, JsonObject>();

  public void start(final CountingCompletionHandler<Void> countDownLatch) {
    final EventBus eb = vertx.eventBus();
    address = container.config().getObject("realtime_store", new JsonObject())
        .getString("address", Topic.STORE);
    final Pattern pattern = Pattern.compile(address + "((/[^/]+){2})" + Topic.WATCH);

    countDownLatch.incRequired();
    eb.registerHandler(address + Topic.PRESENCE, new Handler<Message<JsonObject>>() {
      @Override
      public void handle(Message<JsonObject> message) {
        JsonObject body = message.body();
        String id = body.getString(Key.ID);
        if (id == null) {
          message.fail(-1, "id must be specified");
          return;
        }
        Set<String> sessions = vertx.sharedData()
            .getSet(BridgeHook.getSessionsKey(address + "/" + id + Topic.WATCH));
        if (body.containsField(WebSocketBus.SESSION)) {
          sessions.add(body.getString(WebSocketBus.SESSION));
        }
        JsonArray collaborators = new JsonArray();
        for (String sessionId : sessions) {
          collaborators.addObject(getCollaborator(sessionId));
        }
        message.reply(collaborators);
      }
    }, new Handler<AsyncResult<Void>>() {
      @Override
      public void handle(AsyncResult<Void> ar) {
        if (ar.succeeded()) {
          countDownLatch.complete();
        } else {
          countDownLatch.failed(ar.cause());
        }
      }
    });

    countDownLatch.incRequired();
    eb.registerHandler(BridgeHook.SESSION_WATCH_ADDR, new Handler<Message<JsonObject>>() {
      @Override
      public void handle(Message<JsonObject> message) {
        JsonObject body = message.body();
        String topic = body.getString(BridgeHook.TOPIC);
        Matcher matcher = pattern.matcher(topic);
        if (!matcher.matches()) {
          return;
        }
        JsonObject collaborator = getCollaborator(body.getString(WebSocketBus.SESSION));
        collaborator.putBoolean(Key.IS_JOINED, body.getBoolean(Key.IS_JOINED));
        eb.publish(address + matcher.group(1) + Topic.PRESENCE + Topic.WATCH, collaborator);
      }
    }, new Handler<AsyncResult<Void>>() {
      @Override
      public void handle(AsyncResult<Void> ar) {
        if (ar.succeeded()) {
          countDownLatch.complete();
        } else {
          countDownLatch.failed(ar.cause());
        }
      }
    });
  }

  private JsonObject getCollaborator(String sessionId) {
    JsonObject toRtn = collaborators.get(sessionId);
    if (toRtn == null) {
      String displyName = anonymousUsers.getDisplyName();
      Collaborator collaborator =
          new CollaboratorImpl(anonymousUsers.getUserId(), sessionId, displyName,
                                  anonymousUsers.getColor(), false, true,
                                  anonymousUsers.getPhotoUrl(displyName));
      toRtn = new JsonObject(JacksonUtil.<JreJsonObject>convert(collaborator).toNative());
      toRtn.removeField(Key.IS_ME);
      collaborators.put(sessionId, toRtn);
    }
    return toRtn;
  }
}
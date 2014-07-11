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
package com.goodow.realtime.store.server;

import com.goodow.realtime.store.channel.Constants.Key;
import com.goodow.realtime.store.channel.Constants.Topic;

import org.vertx.java.busmods.BusModBase;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.Future;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

public class RestVerticle extends BusModBase {
  private String address;

  @Override
  public void start(final Future<Void> result) {
    super.start();
    address = getOptionalStringConfig("address", Topic.STORE);
    JsonObject rest = getOptionalObjectConfig("rest", new JsonObject());
    String path = rest.getString("prefix", "/store");
    HttpServer server = vertx.createHttpServer().setCompressionSupported(true);
    RouteMatcher matcher = new RouteMatcher();

    String snapshot = path + "/:docType/:docId";
    String ops = path + "/:docType/:docId" + Topic.OPS;
    Handler<HttpServerRequest> handler = new Handler<HttpServerRequest>() {
      @Override
      public void handle(final HttpServerRequest req) {
        JsonObject message = parseRequest(req);
        eb.sendWithTimeout(address, message, StoreModule.REPLY_TIMEOUT,
            new Handler<AsyncResult<Message<Object>>>() {
              @Override
              public void handle(AsyncResult<Message<Object>> ar) {
                if (ar.failed()) {
                  req.response().setStatusCode(500).setStatusMessage(ar.cause().getMessage()).end();
                  return;
                }
                Object body = ar.result().body();
                if (body == null) {
                  req.response().setStatusCode(404).end();
                  return;
                }
                if ("HEAD".equals(req.method())) {
                  req.response().end(body.toString());
                } else {
                  req.response().headers().set("Content-Type", "application/json");
                  req.response().end(((JsonObject) body).encode());
                }
              }
            });
      }
    };
    matcher.get(snapshot, handler);
    matcher.head(snapshot, handler);
    matcher.post(snapshot, new Handler<HttpServerRequest>() {
      @Override
      public void handle(final HttpServerRequest req) {
        final JsonObject message = parseRequest(req);
        req.bodyHandler(new Handler<Buffer>() {
          @Override
          public void handle(Buffer body) {
            message.putObject(Key.OP_DATA, new JsonObject(body.toString()));
            eb.sendWithTimeout(address, message, StoreModule.REPLY_TIMEOUT,
                new Handler<AsyncResult<Message<JsonObject>>>() {
                  @Override
                  public void handle(AsyncResult<Message<JsonObject>> ar) {
                    if (ar.failed()) {
                      req.response().setStatusCode(500).setStatusMessage(ar.cause().getMessage())
                          .end();
                      return;
                    }
                    JsonObject body = ar.result().body();
                    req.response().headers().set("Content-Type", "application/json");
                    req.response().end(body.encode());
                  }
                });
          }
        });
      }
    });

    matcher.get(ops, new Handler<HttpServerRequest>() {
      @Override
      public void handle(final HttpServerRequest req) {
        final JsonObject message = parseRequest(req);
        Long from = null;
        Long to = null;
        if (req.params().contains("from")) {
          from = Long.valueOf(req.params().get("from"));
        }
        if (req.params().contains("to")) {
          to = Long.valueOf(req.params().get("to"));
        }
        if (req.params().contains(Key.VERSION)) {
          from = Long.valueOf(req.params().get(Key.VERSION));
          to = from + 1;
        }
        message.putNumber("from", from).putNumber("to", to);
        eb.sendWithTimeout(address + Topic.OPS, message, StoreModule.REPLY_TIMEOUT,
            new Handler<AsyncResult<Message<JsonArray>>>() {
              @Override
              public void handle(AsyncResult<Message<JsonArray>> ar) {
                if (ar.failed()) {
                  req.response().setStatusCode(500).setStatusMessage(ar.cause().getMessage()).end();
                  return;
                }
                req.response().headers().set("Content-Type", "application/json");
                req.response().end(ar.result().body().encode());
              }
            });
      }
    });

    server.requestHandler(matcher).listen(rest.getInteger("port", 1987),
        rest.getString("host", "0.0.0.0"), new AsyncResultHandler<HttpServer>() {
          @Override
          public void handle(AsyncResult<HttpServer> ar) {
            if (!ar.succeeded()) {
              result.setFailure(ar.cause());
            } else {
              result.setResult(null);
            }
          }
        });
  }

  private JsonObject parseRequest(final HttpServerRequest req) {
    String id = req.params().get("docType") + "/" + req.params().get("docId");
    JsonObject message = new JsonObject().putString("action", req.method().toLowerCase())
        .putString(Key.ID, id);
    if (req.params().contains(Key.VERSION)) {
      message.putNumber(Key.VERSION, Long.valueOf(req.params().get(Key.VERSION)));
    }
    return message;
  }
}
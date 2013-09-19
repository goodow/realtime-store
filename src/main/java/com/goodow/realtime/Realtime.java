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

import com.goodow.realtime.channel.ChannelDemuxer;
import com.goodow.realtime.channel.http.HttpTransport;
import com.goodow.realtime.channel.operation.OperationSucker;
import com.goodow.realtime.channel.rpc.SnapshotService.Callback;
import com.goodow.realtime.model.util.ModelFactory;

import org.timepedia.exporter.client.Export;
import org.timepedia.exporter.client.ExportPackage;
import org.timepedia.exporter.client.Exportable;

import elemental.json.JsonValue;

/**
 * The Goodow Realtime API.
 */
@ExportPackage("")
@Export(ModelFactory.PACKAGE_PREFIX_REALTIME)
public class Realtime implements Exportable {
  private static final ChannelDemuxer demuxer = ChannelDemuxer.get();
  static String USERID;

  public static void authorize(String userId, String token) {
    USERID = userId;
    demuxer.setAccessToken(token);
  }

  /**
   * Returns an OAuth access token.
   * 
   * @return An OAuth 2.0 access token.
   */
  public static String getToken() {
    return demuxer.getAccessToken();
  }

  /**
   * Loads the realtime data model associated with {@code docId}. If no realtime data model is
   * associated with {@code docId}, a new realtime document will be created and
   * {@code opt_initializer} will be called (if it is provided).
   * 
   * @param docId The ID of the document to load.
   * @param onLoaded A callback that will be called when the realtime document is ready. The created
   *          or opened realtime document object will be passed to this function.
   * @param opt_initializer An optional initialization function that will be called before
   *          {@code onLoaded} only the first time that the document is loaded. The document's
   *          {@link com.goodow.realtime.Model} object will be passed to this function.
   * @param opt_error An optional error handling function that will be called if an error occurs
   *          while the document is being loaded or edited. A {@link com.goodow.realtime.Error}
   *          object describing the error will be passed to this function.
   */
  public static void load(String docId, final DocumentLoadedHandler onLoaded,
      final ModelInitializerHandler opt_initializer, final ErrorHandler opt_error) {
    DocumentBridge snapshot = (DocumentBridge) demuxer.getSnapshot(docId);
    if (snapshot != null) {
      snapshot.addErrorHandler(opt_error);
      snapshot.loadDoucument(onLoaded);
      return;
    }
    final OperationSucker operationSucker = new OperationSucker(docId);
    final DocumentBridge bridge = new DocumentBridge();
    bridge.setUndoEnabled(true);
    operationSucker.load(bridge, new Callback() {
      @Override
      public void onSuccess(JsonValue snapshot, String sessionId, int revision) {
        bridge.sessionId = sessionId;
        bridge.setOutputSink(operationSucker.getOutputSink());
        bridge.addErrorHandler(opt_error);
        bridge.createSnapshot(snapshot);

        if (revision == 0) {
          if (opt_initializer != null) {
            bridge.initializeModel(opt_initializer);
          }
        }
        bridge.loadDoucument(onLoaded);
      }
    });
  }

  public static void setServerAddress(String serverAddress) {
    HttpTransport.CHANNEL = serverAddress;
  }
}

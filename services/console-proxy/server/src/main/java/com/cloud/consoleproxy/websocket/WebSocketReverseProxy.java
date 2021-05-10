// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.consoleproxy.websocket;

import com.cloud.consoleproxy.util.Logger;
import org.eclipse.jetty.websocket.api.Session;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.extensions.DefaultExtension;
import org.java_websocket.handshake.ServerHandshake;
import org.java_websocket.protocols.Protocol;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collections;

/**
 * Acts as a websocket reverse proxy between the remoteSession and the connected endpoint
 * - Connects to a websocket endpoint and sends the received data to the remoteSession endpoint
 * - Receives data from the remoteSession through the receiveProxiedMsg() method and forwards it to the connected endpoint
 *
 *  remoteSession           WebSocketReverseProxy           websocket endpoint
 *  data -----------------> receiveProxiedMsg() -----------> data
 *  data <----------------- onMessage() <------------------- data
 */
public class WebSocketReverseProxy extends WebSocketClient {

    private static final Protocol protocol = new Protocol("binary");
    private static final DefaultExtension defaultExtension = new DefaultExtension();
    private static final Draft_6455 draft = new Draft_6455(Collections.singletonList(defaultExtension), Collections.singletonList(protocol));

    private static final Logger logger = Logger.getLogger(WebSocketReverseProxy.class);
    private Session remoteSession;

    private void acceptAllCerts() {
        TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return new java.security.cert.X509Certificate[]{};
            }
            public void checkClientTrusted(X509Certificate[] chain,
                                           String authType) throws CertificateException {
            }
            public void checkServerTrusted(X509Certificate[] chain,
                                           String authType) throws CertificateException {
            }
        }};
        SSLContext sc;
        try {
            sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            SSLSocketFactory factory = sc.getSocketFactory();
            this.setSocketFactory(factory);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public WebSocketReverseProxy(URI wsUrl, Session session) {
        super(wsUrl, draft);
        this.remoteSession = session;
        acceptAllCerts();
        setConnectionLostTimeout(0);
    }

    @Override
    public void onOpen(ServerHandshake serverHandshake) {
    }

    @Override
    public void onMessage(String message) {
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        logger.info("Closing connection to websocket: reason=" + reason + " code=" + code + " remote=" + remote);
    }

    @Override
    public void onError(Exception ex) {
        logger.error("Error on connection to websocket: " + ex.getLocalizedMessage());
        ex.printStackTrace();
    }

    @Override
    public void onMessage(ByteBuffer bytes) {
        try {
            this.remoteSession.getRemote().sendBytes(bytes);
        } catch (IOException e) {
            logger.error("Error proxing msg from websocket to client side: " + e.getLocalizedMessage());
            e.printStackTrace();
        }
    }

    public void proxyMsgFromRemoteSessionToEndpoint(ByteBuffer msg) {
        this.getConnection().send(msg);
    }
}

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
package com.cloud.consoleproxy;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketException;
import org.eclipse.jetty.websocket.api.extensions.Frame;

import java.awt.Image;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.cloud.consoleproxy.vnc.NoVncClient;

public class ConsoleProxyNoVncClient implements ConsoleProxyClient {
    private static final Logger s_logger = Logger.getLogger(ConsoleProxyNoVncClient.class);
    private static int nextClientId = 0;

    private NoVncClient client;
    private Session session;

    protected int clientId = getNextClientId();
    protected long ajaxSessionId = 0;

    protected long createTime = System.currentTimeMillis();
    protected long lastFrontEndActivityTime = System.currentTimeMillis();

    private boolean connectionAlive;

    private ConsoleProxyClientParam clientParam;
    private String sessionUuid;

    public ConsoleProxyNoVncClient(Session session) {
        this.session = session;
    }

    private int getNextClientId() {
        return ++nextClientId;
    }

    @Override
    public void sendClientRawKeyboardEvent(InputEventType event, int code, int modifiers) {
    }

    @Override
    public void sendClientMouseEvent(InputEventType event, int x, int y, int code, int modifiers) {
    }

    @Override
    public boolean isHostConnected() {
        return connectionAlive;
    }

    @Override
    public boolean isFrontEndAlive() {
        if (!connectionAlive || System.currentTimeMillis()
                - getClientLastFrontEndActivityTime() > ConsoleProxy.VIEWER_LINGER_SECONDS * 1000) {
            s_logger.info("Front end has been idle for too long");
            return false;
        }
        return true;
    }

    public void sendClientFrame(Frame f) throws IOException {
        byte[] data = new byte[f.getPayloadLength()];
        f.getPayload().get(data);
        client.write(data);
    }

    @Override
    public void initClient(ConsoleProxyClientParam param) {
        setClientParam(param);
        client = new NoVncClient();
        connectionAlive = true;
        this.sessionUuid = param.getSessionUuid();

        updateFrontEndActivityTime();
        Thread worker = new Thread(new Runnable() {
            public void run() {
                try {

                    String tunnelUrl = param.getClientTunnelUrl();
                    String tunnelSession = param.getClientTunnelSession();
                    String websocketUrl = param.getWebsocketUrl();

                    connectClientToVNCServer(tunnelUrl, tunnelSession, websocketUrl);

                    authenticateToVNCServer();

                    int readBytes;
                    byte[] b;
                    while (connectionAlive) {
                        if (client.isVncOverWebSocketConnection()) {
                            if (client.isVncOverWebSocketConnectionOpen()) {
                                updateFrontEndActivityTime();
                            }
                            connectionAlive = client.isVncOverWebSocketConnectionAlive();
                            try {
                                Thread.sleep(1);
                            } catch (Exception e) {
                                s_logger.warn("Error on sleep for vnc over websocket", e);
                            }
                        } else if (client.isVncOverNioSocket()) {
                            byte[] bytesArr;
                            int nextBytes = client.getNextBytes();
                            bytesArr = new byte[nextBytes];
                            client.readBytes(bytesArr, nextBytes);
                            if (nextBytes > 0) {
                                session.getRemote().sendBytes(ByteBuffer.wrap(bytesArr));
                                updateFrontEndActivityTime();
                            }
                        } else {
                            b = new byte[100];
                            readBytes = client.read(b);
                            if (readBytes == -1 || (readBytes > 0 && !sendReadBytesToNoVNC(b, readBytes))) {
                                connectionAlive = false;
                            }
                        }
                    }
                    connectionAlive = false;
                } catch (IOException e) {
                    s_logger.error("Error on VNC client", e);
                }
            }

        });
        worker.start();
    }

    private boolean sendReadBytesToNoVNC(byte[] b, int readBytes) {
        try {
            session.getRemote().sendBytes(ByteBuffer.wrap(b, 0, readBytes));
            updateFrontEndActivityTime();
        } catch (WebSocketException | IOException e) {
            s_logger.debug("Connection exception", e);
            return false;
        }
        return true;
    }

    /**
     * Authenticate to VNC server when not using websockets
     *
     * Since we are supporting the 3.8 version of the RFB protocol, there are changes on the stages:
     * 1. Handshake:
     *    1.a. Protocol version
     *    1.b. Security types
     * 2. Security types
     * 3. Initialisation
     *
     * Reference: https://github.com/rfbproto/rfbproto/blob/master/rfbproto.rst#7protocol-messages
     */
    private void authenticateToVNCServer() throws IOException {
        if (client.isVncOverWebSocketConnection()) {
            return;
        }

        if (!client.isVncOverNioSocket()) {
            String ver = client.handshake();
            session.getRemote().sendBytes(ByteBuffer.wrap(ver.getBytes(), 0, ver.length()));

            byte[] b = client.authenticateTunnel(getClientHostPassword());
            session.getRemote().sendBytes(ByteBuffer.wrap(b, 0, 4));
        } else {
            authenticateVNCServerThroughNioSocket();
        }
    }

    /**
     * Handshaking messages consist on 3 phases:
     * - ProtocolVersion
     * - Security
     * - SecurityResult
     *
     * Reference: https://github.com/rfbproto/rfbproto/blob/master/rfbproto.rst#71handshaking-messages
     */
    protected void handshakePhase() {
        handshakeProtocolVersion();
        int securityType = handshakeSecurity();
        handshakeSecurityResult(securityType);

        client.waitForNoVNCReply();
    }

    protected void handshakeSecurityResult(int secType) {
        client.processHandshakeSecurityType(secType, getClientHostPassword(),
                getClientHostAddress(), getClientHostPort());

        client.processSecurityResultMsg();
        byte[] securityResultToClient = new byte[] { 0, 0, 0, 0 };
        sendMessageToVNCClient(securityResultToClient, 4);
        client.setWaitForNoVnc(true);
    }

    protected int handshakeSecurity() {
        int secType = client.handshakeSecurityType();
        byte[] numberTypesToClient = new byte[] { 1, (byte) secType };
        sendMessageToVNCClient(numberTypesToClient, 2);
        return secType;
    }

    protected void handshakeProtocolVersion() {
        ByteBuffer verStr = client.handshakeProtocolVersion();
        sendMessageToVNCClient(verStr.array(), 12);
    }

    protected void authenticateVNCServerThroughNioSocket() {
        handshakePhase();
        initialisationPhase();
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Authenticated successfully");
        }
    }

    /**
     * Initialisation messages consist on:
     * - ClientInit
     * - ServerInit
     *
     * Reference: https://github.com/rfbproto/rfbproto/blob/master/rfbproto.rst#73initialisation-messages
     */
    private void initialisationPhase() {
        byte[] serverInitByteArray = client.readServerInit();

        String displayNameForVM = String.format("%s %s", clientParam.getClientDisplayName(),
                client.isTLSConnectionEstablished() ? "(TLS backend)" : "");
        byte[] bytesServerInit = rewriteServerNameInServerInit(serverInitByteArray, displayNameForVM);

        sendMessageToVNCClient(bytesServerInit, bytesServerInit.length);
        client.setWaitForNoVnc(true);
        client.waitForNoVNCReply();
    }

    /**
     * Send a message to the noVNC client
     */
    private void sendMessageToVNCClient(byte[] arr, int length) {
        try {
            session.getRemote().sendBytes(ByteBuffer.wrap(arr, 0, length));
        } catch (IOException e) {
            s_logger.error("Error sending a message to the noVNC client", e);
        }
    }

    protected static byte[] rewriteServerNameInServerInit(byte[] serverInitBytes, String serverName) {
        byte[] serverNameBytes = serverName.getBytes(StandardCharsets.UTF_8);
        ByteBuffer serverInitBuffer = ByteBuffer.allocate(24 + serverNameBytes.length);
        serverInitBuffer.put(serverInitBytes, 0, 20);
        serverInitBuffer.putInt(serverNameBytes.length);
        serverInitBuffer.put(serverNameBytes);
        return serverInitBuffer.array();
    }

    /**
     * Connect to a VNC server in one of three possible ways:
     * - When tunnelUrl and tunnelSession are not empty -> via tunnel
     * - When websocketUrl is not empty -> connect to websocket
     * - Otherwise -> connect to TCP port on host directly
     */
    private void connectClientToVNCServer(String tunnelUrl, String tunnelSession, String websocketUrl) {
        try {
            if (StringUtils.isNotBlank(websocketUrl)) {
                s_logger.info(String.format("Connect to VNC over websocket URL: %s", websocketUrl));
                client.connectToWebSocket(websocketUrl, session);
            } else if (tunnelUrl != null && !tunnelUrl.isEmpty() && tunnelSession != null
                    && !tunnelSession.isEmpty()) {
                URI uri = new URI(tunnelUrl);
                s_logger.info(String.format("Connect to VNC server via tunnel. url: %s, session: %s",
                        tunnelUrl, tunnelSession));

                ConsoleProxy.ensureRoute(uri.getHost());
                client.connectTo(uri.getHost(), uri.getPort(), uri.getPath() + "?" + uri.getQuery(),
                        tunnelSession, "https".equalsIgnoreCase(uri.getScheme()));
            } else {
                s_logger.info(String.format("Connect to VNC server directly. host: %s, port: %s",
                        getClientHostAddress(), getClientHostPort()));
                ConsoleProxy.ensureRoute(getClientHostAddress());
                client.connectTo(getClientHostAddress(), getClientHostPort());
            }
        } catch (Throwable e) {
            s_logger.error("Unexpected exception", e);
        }
    }

    private void setClientParam(ConsoleProxyClientParam param) {
        this.clientParam = param;
    }

    @Override
    public void closeClient() {
        this.connectionAlive = false;
        ConsoleProxy.removeViewer(this);
    }

    @Override
    public String getSessionUuid() {
        return sessionUuid;
    }

    @Override
    public int getClientId() {
        return this.clientId;
    }

    @Override
    public long getAjaxSessionId() {
        return this.ajaxSessionId;
    }

    @Override
    public AjaxFIFOImageCache getAjaxImageCache() {
        // Unimplemented
        return null;
    }

    @Override
    public Image getClientScaledImage(int width, int height) {
        // Unimplemented
        return null;
    }

    @Override
    public String onAjaxClientStart(String title, List<String> languages, String guest) {
        // Unimplemented
        return null;
    }

    @Override
    public String onAjaxClientUpdate() {
        // Unimplemented
        return null;
    }

    @Override
    public String onAjaxClientKickoff() {
        // Unimplemented
        return null;
    }

    @Override
    public long getClientCreateTime() {
        return createTime;
    }

    public void updateFrontEndActivityTime() {
        lastFrontEndActivityTime = System.currentTimeMillis();
    }

    @Override
    public long getClientLastFrontEndActivityTime() {
        return lastFrontEndActivityTime;
    }

    @Override
    public String getClientHostAddress() {
        return clientParam.getClientHostAddress();
    }

    @Override
    public int getClientHostPort() {
        return clientParam.getClientHostPort();
    }

    @Override
    public String getClientHostPassword() {
        return clientParam.getClientHostPassword();
    }

    @Override
    public String getClientTag() {
        if (clientParam.getClientTag() != null)
            return clientParam.getClientTag();
        return "";
    }

    public Session getSession() {
        return session;
    }

}

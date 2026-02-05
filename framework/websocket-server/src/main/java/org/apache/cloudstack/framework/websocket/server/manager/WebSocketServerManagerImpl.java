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

package org.apache.cloudstack.framework.websocket.server.manager;

import javax.inject.Inject;

import org.apache.cloudstack.framework.websocket.server.WebSocketServer;
import org.apache.cloudstack.framework.websocket.server.common.WebSocketHandler;
import org.apache.cloudstack.framework.websocket.server.common.WebSocketRouter;
import org.apache.cloudstack.utils.server.ServerPropertiesUtil;
import org.apache.commons.lang3.StringUtils;

import com.cloud.utils.Pair;
import com.cloud.utils.component.ManagerBase;

public class WebSocketServerManagerImpl extends ManagerBase implements WebSocketServerManager {

    @Inject
    WebSocketRouter webSocketRouter;

    protected enum ServerMode {
        STANDALONE,
        EMBEDDED
    }

    private boolean serverEnabled = false;
    private boolean serverSslEnabled = false;
    private int serverPort;
    private ServerMode serverMode = ServerMode.STANDALONE;
    private WebSocketServer webSocketServer;

    protected boolean isServerRunning() {
        return webSocketServer != null && webSocketServer.isRunning();
    }

    protected void startWebSocketServer() {
        if (!serverEnabled) {
            return;
        }
        if (isServerRunning()) {
            logger.info("WebSocket Server is already running on port {}!", webSocketServer.getPort());
            return;
        }
        if (!ServerMode.STANDALONE.equals(serverMode)) {
            logger.info("Standalone WebSocket Server not started as it is not configured!");
            return;
        }
        webSocketServer = new WebSocketServer(serverPort, webSocketRouter, serverSslEnabled);
        try {
            webSocketServer.start();
        } catch (IllegalArgumentException | InterruptedException e) {
            logger.error("Failed to start WebSocket Server", e);
            webSocketServer = null;
        }
    }

    protected void stopWebSocketServer(Integer maxWaitSeconds) {
        if (webSocketServer == null || !webSocketServer.isRunning()) {
            logger.info("WebSocket Server is already stopped!");
            return;
        }
        webSocketServer.stop(maxWaitSeconds == null ? 5 : maxWaitSeconds);
        webSocketServer = null;
    }

    protected void initializeServerModeAndPort() {
        if (!serverEnabled) {
            return;
        }
        final String webSocketServerPort = ServerPropertiesUtil.getProperty(ServerPropertiesUtil.KEY_WEBSOCKET_PORT);
        final Pair<Boolean, Integer> mainServerModeAndPort = ServerPropertiesUtil.getServerModeAndPort();
        serverSslEnabled = mainServerModeAndPort.first();
        final int mainServerPort = mainServerModeAndPort.second();
        if (StringUtils.isBlank(webSocketServerPort)) {
            logger.info("WebSocket Server port is not configured, WebSocket Server will not be started!");
            serverPort = mainServerPort;
            serverMode = ServerMode.EMBEDDED;
            return;
        }
        try {
            serverPort = Integer.parseInt(webSocketServerPort);
            if (serverPort == mainServerPort) {
                logger.info("WebSocket Server port {} is same as main server port {}, " +
                        "standalone WebSocket Server will not be started!", serverPort, mainServerPort);
                serverMode = ServerMode.EMBEDDED;
            }
        } catch (NumberFormatException nfe) {
            logger.error(
                    "WebSocket Server port is not a valid number: {}, WebSocket Server will not be started!",
                    webSocketServerPort, nfe);
            serverEnabled = false;
        }
    }

    protected static boolean looksRegex(String s) {
        // starts with ^ or ends with $ is a strong hint
        if (s.startsWith("^") || s.endsWith("$")) return true;
        // common meta characters
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '.' || c == '*' || c == '[' || c == ']' || c == '(' || c == ')' || c == '|' || c == '\\') {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isServerEnabled() {
        return serverEnabled;
    }

    @Override
    public int getServerPort() {
        return serverPort;
    }

    @Override
    public String getWebSocketBasePath() {
        StringBuilder sb = new StringBuilder(WebSocketRouter.WEBSOCKET_PATH_PREFIX);
        if (!ServerMode.STANDALONE.equals(serverMode)) {
            String contextPath = ServerPropertiesUtil.getProperty("context.path", "/client").trim();
            sb.insert(0, contextPath);
        }
        return sb.toString();
    }

    @Override
    public boolean isServerSslEnabled() {
        return serverSslEnabled;
    }

    @Override
    public void registerRoute(String pathSpec, WebSocketHandler handler, long idleTimeoutSeconds) {
        if (pathSpec == null || pathSpec.isEmpty()) {
            throw new IllegalArgumentException("pathSpec must not be empty");
        }
        final String norm = WebSocketRouter.ensureLeadingSlash(pathSpec);

        if (looksRegex(norm)) {
            webSocketRouter.registerRegex(norm, handler, idleTimeoutSeconds);
            logger.info("Registered REGEX route: {} (idle={}s)", norm, idleTimeoutSeconds);
        } else if (norm.endsWith("/")) {
            webSocketRouter.registerPrefix(norm, handler, idleTimeoutSeconds);
            logger.info("Registered PREFIX route: {} (idle={}s)", norm, idleTimeoutSeconds);
        } else {
            webSocketRouter.registerExact(norm, handler, idleTimeoutSeconds);
            logger.info("Registered EXACT route: {} (idle={}s)", norm, idleTimeoutSeconds);
        }
    }

    @Override
    public void unregisterRoute(String pathSpec) {
        if (pathSpec == null || pathSpec.isEmpty()) return;
        final String key = WebSocketRouter.ensureLeadingSlash(pathSpec);
        webSocketRouter.unregister(key);
        logger.info("Unregistered route: {}", key);
    }

    @Override
    public boolean start() {
        super.start();
        serverEnabled = Boolean.parseBoolean(ServerPropertiesUtil.getProperty(
                ServerPropertiesUtil.KEY_WEBSOCKET_ENABLE, "false"));
        initializeServerModeAndPort();
        startWebSocketServer();
        return true;
    }

    @Override
    public boolean stop() {
        stopWebSocketServer(1);
        return true;
    }
}

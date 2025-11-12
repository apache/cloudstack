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

package org.apache.cloudstack.websocket;

import java.io.IOException;
import java.util.Map;

import javax.servlet.ServletException;

import org.apache.cloudstack.framework.websocket.server.common.WebSocketHandler;
import org.apache.cloudstack.framework.websocket.server.common.WebSocketRouter;
import org.apache.cloudstack.framework.websocket.server.common.WebSocketSession;
import org.apache.cloudstack.utils.server.ServerPropertiesUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeResponse;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;
import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

import com.cloud.utils.Pair;
import com.cloud.utils.component.ComponentContext;

public class JettyWebSocketServlet extends WebSocketServlet {
    protected static final Logger LOGGER = LogManager.getLogger(JettyWebSocketServlet.class);
    private boolean enabled;

    public static boolean isWebSocketServletEnabled() {
        return Boolean.parseBoolean(ServerPropertiesUtil.getProperty(ServerPropertiesUtil.KEY_WEBSOCKET_ENABLE,
                "false"));
    }

    public static Integer getWebSocketServletPort() {
        final String webSocketServerPort = ServerPropertiesUtil.getProperty(ServerPropertiesUtil.KEY_WEBSOCKET_PORT);
        final Pair<Boolean, Integer> mainServerModeAndPort = ServerPropertiesUtil.getServerModeAndPort();
        final int mainServerPort = mainServerModeAndPort.second();
        return (StringUtils.isBlank(webSocketServerPort) ||
                webSocketServerPort.equals(String.valueOf(mainServerPort))) ?
                mainServerPort : null;
    }

    @Override
    public void init() throws ServletException {
        LOGGER.info("Initializing JettyWebSocketServlet");
        if (!isWebSocketServletEnabled()) {
            enabled = false;
            LOGGER.info("WebSocket Server is not enabled, embedded WebSocket Server will not be running");
        }
        Integer port = getWebSocketServletPort();
        if (port == null) {
            enabled = false;
            LOGGER.info("WebSocket Server port is configured, embedded WebSocket Server will not be running");
            return;
        }
        enabled = true;
        LOGGER.info("Embedded WebSocket Server initialized at {}/* with port: {}",
                WebSocketRouter.WEBSOCKET_PATH_PREFIX, port);
        super.init();
    }


    @Override
    public void configure(WebSocketServletFactory factory) {
        if (!enabled) {
            return;
        }
        LOGGER.info("Configuring JettyWebSocketServlet (idle=120s, maxText=131072, maxBin=131072)");
        factory.getPolicy().setIdleTimeout(120_000);
        factory.getPolicy().setMaxTextMessageSize(131_072);
        factory.getPolicy().setMaxBinaryMessageSize(131_072);

        WebSocketRouter webSocketRouter = ComponentContext.getDelegateComponentOfType(WebSocketRouter.class);
        factory.setCreator(new Creator(webSocketRouter));
        LOGGER.info("JettyWebSocketServlet configured, Creator installed");
    }

    static final class Creator implements WebSocketCreator {
        private final WebSocketRouter router;

        Creator(WebSocketRouter router) {
            this.router = router;
        }

        @Override
        public Object createWebSocket(ServletUpgradeRequest req, ServletUpgradeResponse resp) {
            String ctx = req.getHttpServletRequest().getContextPath();
            String full = req.getRequestPath();
            String path = (ctx != null && !ctx.isEmpty() && full.startsWith(ctx)) ? full.substring(ctx.length()) : full;
            LOGGER.debug("WebSocket connection for path: {}, query: {}", path, req.getQueryString());

            path = WebSocketRouter.stripWebSocketPathPrefix(path);

            WebSocketRouter.ResolvedRoute rr = router.resolve(path.startsWith("/") ? path : ("/" + path));
            if (rr == null || rr.getHandler() == null) {
                try {
                    resp.sendForbidden("No route for " + path);
                } catch (IOException ignore) {
                }
                return null;
            }

            WebSocketHandler handler = rr.getHandler();
            return new WebSocketAdapter(handler, path, req.getQueryString());
        }
    }

    /**
     * Adapts Jetty-native events to your WebSocketHandler
     */
    static final class WebSocketAdapter extends org.eclipse.jetty.websocket.api.WebSocketAdapter {
        private final WebSocketHandler handler;
        private final String routePath;
        private final String rawQuery;
        private WebSocketSession session;

        private Map<String, String> parse(String q) {
            if (q == null || q.isEmpty()) return java.util.Collections.emptyMap();
            java.util.Map<String, String> m = new java.util.HashMap<>();
            for (String kv : q.split("&")) {
                int i = kv.indexOf('=');
                String k = i >= 0 ? kv.substring(0, i) : kv;
                String v = i >= 0 ? kv.substring(i + 1) : "";
                m.put(java.net.URLDecoder.decode(k, java.nio.charset.StandardCharsets.UTF_8),
                        java.net.URLDecoder.decode(v, java.nio.charset.StandardCharsets.UTF_8));
            }
            return m;
        }

        WebSocketAdapter(WebSocketHandler handler, String routePath, String rawQuery) {
            this.handler = handler;
            this.routePath = routePath;
            this.rawQuery = rawQuery;
        }

        @Override
        public void onWebSocketConnect(Session jettySess) {
            super.onWebSocketConnect(jettySess);
            this.session = JettyWebSocketSession.adapt(jettySess, routePath, parse(rawQuery));
            try {
                handler.onOpen(session);
            } catch (Throwable t) {
                try {
                    handler.onError(session, t);
                } finally {
                    jettySess.close();
                }
            }
        }

        @Override
        public void onWebSocketText(String message) {
            try {
                handler.onText(session, message);
            } catch (Throwable t) {
                handler.onError(session, t);
            }
        }

        @Override
        public void onWebSocketBinary(byte[] payload, int offset, int len) {
            try {
                handler.onBinary(session, java.nio.ByteBuffer.wrap(payload, offset, len));
            } catch (Throwable t) {
                handler.onError(session, t);
            }
        }

        @Override
        public void onWebSocketClose(int statusCode, String reason) {
            try {
                handler.onClose(session, statusCode, reason);
            } finally {
                super.onWebSocketClose(statusCode, reason);
            }
        }

        @Override
        public void onWebSocketError(Throwable cause) {
            try {
                handler.onError(session, cause);
            } finally { /* no-op */ }
        }
    }
}

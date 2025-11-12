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

package org.apache.cloudstack.logsws.server;

import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.cloudstack.framework.websocket.server.common.WebSocketHandler;
import org.apache.cloudstack.framework.websocket.server.common.WebSocketSession;
import org.apache.cloudstack.logsws.LogsWebSession;
import org.apache.cloudstack.logsws.LogsWebSessionTokenPayload;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.util.AttributeKey;

/**
 * Transport-agnostic handler for loggerroutes.
 * <p>
 * Responsibilities:
 * - Extract and validate the dynamic route from the request path (after serverPath/)
 * - Validate access token from the route
 * - Enforce single active connection per route (close older one)
 * - Register/unregister route with LogsWebSocketRouteManager
 * <p>
 * Notes:
 * - If the transport can provide the remote address, set it in session attrs under "remoteAddress"
 * before calling onOpen(..). (Netty initializer can do this easily; Jetty can omit and skip that check.)
 */
public final class LogsWebSocketRoutingHandler implements WebSocketHandler {
    public static final AttributeKey<String> LOGGER_ROUTE_ATTR = AttributeKey.valueOf("loggerRoute");
    private static final Logger LOGGER = LogManager.getLogger(LogsWebSocketRoutingHandler.class);

    /**
     * Session attr keys
     */
    public static final String ATTR_ROUTE = "loggerRoute";
    public static final String ATTR_REMOTE_ADDR = "remoteAddress";
    private static final String ATTR_LOGS_STREAM = "logsStreamer";
    public static final String ATTR_LOGS_SESSION = "logsSession";

    private final LogsWebSocketRouteManager routeManager;
    private final LogsWebSocketServerHelper serverHelper;

    /**
     * Keep at most one active connection per route
     */
    private final ConcurrentMap<String, WebSocketSession> activeByRoute = new ConcurrentHashMap<>();

    /**
     * Base WS path for this module, e.g. "/logger" (no trailing slash).
     */
    private final String serverPath;

    public LogsWebSocketRoutingHandler(LogsWebSocketRouteManager routeManager,
                                       LogsWebSocketServerHelper serverHelper) {
        this.routeManager = Objects.requireNonNull(routeManager, "routeManager");
        this.serverHelper = Objects.requireNonNull(serverHelper, "serverHelper");
        String p = Objects.requireNonNull(serverHelper.getServerPath(), "serverPath");
        this.serverPath = p.endsWith("/") ? p.substring(0, p.length() - 1) : p;
    }


    private LogsWebSession getValidSession(final String route, final WebSocketSession session) {
        final LogsWebSessionTokenPayload tokenPayload = serverHelper.parseToken(route);
        if (tokenPayload == null) {
            LOGGER.error("Decrypted token payload is null for route: {}", route);
            return null;
        }

        final String sessionUuid = tokenPayload.getSessionUuid();
        if (isBlank(sessionUuid)) {
            LOGGER.error("Session UUID is blank in token payload for route: {}", route);
            return null;
        }

        final String creatorAddress = tokenPayload.getCreatorAddress();
        if (!isBlank(creatorAddress)) {
            final String remote = session.getAttr(ATTR_REMOTE_ADDR);
            if (remote != null && !remote.contains(creatorAddress)) {
                LOGGER.error("Remote address '{}' does not match creator address '{}' for session {}",
                        remote, creatorAddress, sessionUuid);
                return null;
            }
        } else {
            LOGGER.warn("Creator address is blank in token payload (skipping remote verification).");
        }

        final LogsWebSession logsSession = serverHelper.getSession(sessionUuid);
        if (logsSession == null) {
            LOGGER.error("No server-side LogsWebSession for uuid {} (route {})", sessionUuid, route);
            return null;
        }
        return logsSession;
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static void safeClose(WebSocketSession s, int code, String reason) {
        try {
            s.close(code, reason);
        } catch (Throwable ignore) {
        }
    }

    private static void close(WebSocketSession s, int code, String reason) {
        LOGGER.debug("Closing session {} with code {}, reason: {}", s.id(), code, reason);
        try {
            s.close(code, reason);
        } catch (Throwable ignore) {
        }
    }

    @Override
    public void onOpen(WebSocketSession session) {
        final String path = session.path();
        if (path == null || !path.startsWith(serverPath + "/")) {
            LOGGER.warn("Invalid request path '{}', expected prefix '{}/*'", path, serverPath);
            safeClose(session, 4000, "Invalid request path");
            return;
        }

        final String route = path.substring((serverPath + "/").length());
        if (route.isEmpty()) {
            LOGGER.warn("Empty route in request path '{}'", path);
            safeClose(session, 4001, "Empty route");
            return;
        }

        final LogsWebSession logsSession = getValidSession(route, session);
        if (logsSession == null) {
            LOGGER.warn("Unauthorized connection attempt for route: {}", route);
            safeClose(session, 4003, "Unauthorized");
            return;
        }

        // Enforce single connection per route
        WebSocketSession prev = activeByRoute.put(route, session);
        if (prev != null && prev != session) {
            LOGGER.debug("Closing existing connection for route: {}", route);
            try {
                prev.close(4008, "Superseded by a new connection");
            } catch (Throwable ignored) {
            }
        }

        // Register with route manager (idempotent add)
        try {
            routeManager.addRoute(route);
        } catch (Throwable t) {
            LOGGER.error("Failed to add route '{}' to routeManager", route, t);
            activeByRoute.remove(route, session);
            safeClose(session, 1011, "Route registration failed");
            return;
        }

        LOGGER.trace("Starting LogsStreamer for route: {}", route);
        LogsStreamer streamer = new LogsStreamer(logsSession, serverHelper);
        try {
            streamer.start(session, route);
        } catch (Throwable t) {
            LOGGER.error("Failed to start logs streamer for route {}", route, t);
            try {
                streamer.close();
            } catch (Throwable ignore) {
            }
            activeByRoute.remove(route, session);
            try {
                routeManager.removeRoute(route);
            } catch (Throwable ignore) {
            }
            close(session, 1011, "Stream start failed");
            return;
        }

        // Stash per-connection state on session
        session.setAttr(ATTR_LOGS_STREAM, streamer);
        session.setAttr(ATTR_ROUTE, route);
        session.setAttr(ATTR_LOGS_SESSION, logsSession);

        LOGGER.debug("Logs WS connected. route={}, sessionId={}", route, session.id());
    }

    @Override
    public void onText(WebSocketSession session, String text) {
        if (text == null) {
            return;
        }
        if ("ping".equalsIgnoreCase(text.trim())) {
            session.sendText("pong");
        } else {
            LOGGER.debug("Ignoring client text message on logs route: {} bytes", text.length());
        }
    }

    @Override
    public void onBinary(WebSocketSession session, ByteBuffer bin) {
        // Usually unused for logs; consume or ignore.
        if (bin != null) {
            LOGGER.debug("Ignoring client binary message on logs route: {} bytes", bin.remaining());
        }
    }

    @Override
    public void onClose(WebSocketSession session, int code, String reason) {
        final String route = session.getAttr(ATTR_ROUTE);
        if (route != null) {
            // Remove only if this exact session is the current owner
            activeByRoute.compute(route, (r, current) -> (current == session) ? null : current);
            try {
                routeManager.removeRoute(route);
            } catch (Throwable t) {
                LOGGER.debug("Error while removing route '{}' on close (ignored)", route, t);
            }
            LOGGER.debug("Logs WS closed. route={}, code={}, reason={}", route, code, reason);
        }
    }

    @Override
    public void onError(WebSocketSession session, Throwable t) {
        final String route = session.getAttr(ATTR_ROUTE);
        LOGGER.error("Exception in LogsWebSocketRoutingHandler (route={})", route, t);
        try {
            session.close(1011, "Internal error");
        } catch (Throwable ignore) {
        }
    }
}

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

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.cloudstack.framework.websocket.server.common.WebSocketSession;
import org.apache.cloudstack.logsws.LogsWebSession;
import org.apache.cloudstack.logsws.logreader.FilteredLogTailerListener;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.input.ReversedLinesFileReader;
import org.apache.commons.io.input.Tailer;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

public class LogsStreamer implements AutoCloseable {
    protected static Logger LOGGER = LogManager.getLogger(LogsStreamer.class);
    private Tailer tailer;
    private ExecutorService tailerExecutor;
    private final LogsWebSession logsWebSession;
    private final LogsWebSocketServerHelper serverHelper;
    private ScheduledExecutorService testBroadcasterExecutor;

    public LogsStreamer(final LogsWebSession logsWebSession, final LogsWebSocketServerHelper serverHelper) {
        this.logsWebSession = logsWebSession;
        this.serverHelper = serverHelper;
        testBroadcasterExecutor = Executors.newSingleThreadScheduledExecutor();
    }

    @Nullable
    protected File getValidatedLogFile() {
        File logFile = new File(serverHelper.getLogFile());
        if (!logFile.exists()) {
            LOGGER.error("Log file {} does not exist", logFile.getAbsolutePath());
            return null;
        }
        if (!logFile.canRead()) {
            LOGGER.error("Log file {} is not readable", logFile.getAbsolutePath());
            return null;
        }
        return logFile;
    }

    /**
     * For testing purpose - Start broadcasting messages to the given session at fixed intervals.
     *
     * @param session WebSocket session to send test messages to
     */
    protected void startTestBroadcasting(final WebSocketSession session) {
        testBroadcasterExecutor.scheduleAtFixedRate(() -> {
            String msg = String.format("Hello from Logger broadcaster! Route: %s", session.path());
            session.sendText(msg);
            LOGGER.debug("Broadcasting message: '{}' for context: {}", msg, session.path());
        }, 0, 2, TimeUnit.SECONDS);
    }

    /**
     * Process existing lines from the log file and send matching lines to the session.
     *
     * @param session WebSocket session to send matching lines to
     * @param logFile log file to read existing lines from
     * @param filters filter strings for matching; may be null or empty
     */
    protected void processExistingLines(final WebSocketSession session, File logFile, final List<String> filters) {
        try (ReversedLinesFileReader reader = new ReversedLinesFileReader(logFile, StandardCharsets.UTF_8)) {
            List<String> lastLines = new ArrayList<>();
            String line;
            int count = 0;
            while ((line = reader.readLine()) != null && count < serverHelper.getMaxReadExistingLines()) {
                lastLines.add(line);
                count++;
            }
            Collections.reverse(lastLines);
            boolean isFilterEmpty = CollectionUtils.isEmpty(filters);
            boolean isLastLineValid = false;
            for (String l : lastLines) {
                if (FilteredLogTailerListener.isValidLine(l, isFilterEmpty, isLastLineValid, filters)) {
                    session.sendText(l);
                    isLastLineValid = true;
                } else {
                    isLastLineValid = false;
                }
            }
        } catch (IOException e) {
            session.sendText(String.format("Error reading existing log lines: %s", e.getMessage()));
        }
    }

    /**
     * Tail the given log file and stream new matching lines to the session.
     *
     * @param session WebSocket session to send matching lines to
     * @param filters filter strings for the tailer listener; may be null or empty
     * @param logFile log file to tail (must be readable)
     */
    protected void startLogTailing(WebSocketSession session, List<String> filters, File logFile) {
        FilteredLogTailerListener listener = new FilteredLogTailerListener(session, filters);
        tailer = new Tailer(logFile, listener, 50, true);
        tailerExecutor = Executors.newSingleThreadExecutor();
        tailerExecutor.submit(tailer);
    }

    /**
     * Asynchronously extracts and normalizes the remote address from the provided
     * WebSocketSession and updates the connection info on the server helper.
     *
     * <p>The method reads the session attribute {@code "remoteAddress"}, strips a
     * leading slash if present, handles bracketed IPv6 addresses, and attempts to
     * resolve the host to an IP address. Any errors during parsing or resolution
     * are caught and logged at debug level so the caller is not impacted.
     *
     * @param session the web socket session containing the {@code "remoteAddress"} attribute
     */
    protected void updateSessionWithRemoteAddressAsync(WebSocketSession session) {
        CompletableFuture.runAsync(() -> {
            try {
                String remoteStr = session.getAttr(WebSocketSession.ATTR_REMOTE_ADDR);
                if (StringUtils.isEmpty(remoteStr)) {
                    return;
                }
                String s = remoteStr.startsWith("/") ? remoteStr.substring(1) : remoteStr;
                String host;
                if (s.startsWith("[")) {
                    int end = s.indexOf(']');
                    host = end > 0 ? s.substring(1, end) : s;
                } else {
                    int lastColon = s.lastIndexOf(':');
                    if (lastColon > 0 && s.indexOf(':') == lastColon) {
                        host = s.substring(0, lastColon);
                    } else {
                        host = s;
                    }
                }
                try {
                    host = InetAddress.getByName(host).getHostAddress();
                } catch (Exception ignore) {}
                serverHelper.updateSessionConnection(logsWebSession.getId(), host);
            } catch (Throwable t) {
                LOGGER.debug("Failed to update remote address asynchronously", t);
            }
        });
    }

    /**
     * Starts log streaming for the given WebSocket session and route.
     * First, it sends the existing log lines that match the session's filters,
     * then it starts tailing the log file to stream new matching lines.
     *
     * @param session the WebSocket session to stream logs to
     * @param route the route associated with the log streaming session
     */
    public void start(WebSocketSession session, String route) {
        LOGGER.debug("Starting log streaming for route: {}", route);
        File logFile = getValidatedLogFile();
        if (logFile == null) {
            session.sendText("Log file not available or cannot be read.");
            return;
        }
        updateSessionWithRemoteAddressAsync(session);

        processExistingLines(session, logFile, logsWebSession.getFilters());

        startLogTailing(session, logsWebSession.getFilters(), logFile);
    }

    @Override
    public void close() {
        try {
            if (tailer != null) {
                tailer.stop();
                tailer = null;
            }
        } catch (Throwable ignore) {
        }
        if (tailerExecutor != null && !tailerExecutor.isShutdown()) {
            tailerExecutor.shutdownNow();
            tailerExecutor = null;
        }
        try {
            if (logsWebSession != null) {
                serverHelper.updateSessionConnection(logsWebSession.getId(), null);
            }
        } catch (Throwable ignore) {
        }
        if (testBroadcasterExecutor != null && !testBroadcasterExecutor.isShutdown()) {
            testBroadcasterExecutor.shutdownNow();
            testBroadcasterExecutor = null;
        }
    }
}

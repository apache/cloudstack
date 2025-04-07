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
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.cloudstack.framework.websocket.server.common.WebSocketSession;
import org.apache.cloudstack.logsws.LogsWebSession;
import org.apache.cloudstack.logsws.logreader.FilteredLogTailerListener;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.input.ReversedLinesFileReader;
import org.apache.commons.io.input.Tailer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

public class LogsStreamer implements AutoCloseable {
    protected static Logger LOGGER = LogManager.getLogger(LogsStreamer.class);
    private String route;
    private Tailer tailer;
    private ExecutorService tailerExecutor;
    private final LogsWebSession logsWebSession;
    private final LogsWebSocketServerHelper serverHelper;

    public LogsStreamer(final LogsWebSession logsWebSession, final LogsWebSocketServerHelper serverHelper) {
        this.logsWebSession = logsWebSession;
        this.serverHelper = serverHelper;
    }

    private void startTestBroadcasting(final ChannelHandlerContext ctx) {
        String route = ctx.channel().attr(LogsWebSocketRoutingHandler.LOGGER_ROUTE_ATTR).get();
        // Schedule a periodic task to send messages every 5 seconds.
        ctx.executor().scheduleAtFixedRate(() -> {
            if (ctx.channel().isActive()) {
                String msg = String.format("Hello from Logger broadcaster! Route: %s", route);
                ctx.writeAndFlush(new TextWebSocketFrame(msg));
                LOGGER.debug("Broadcasting message: '{}' for context: {}", msg, ctx.hashCode());
            }
        }, 0, 5, TimeUnit.SECONDS);
    }

    private void processExistingLines(final WebSocketSession session, File logFile, final List<String> filters) {
        try (ReversedLinesFileReader reader = new ReversedLinesFileReader(logFile, StandardCharsets.UTF_8)) {
            List<String> lastLines = new ArrayList<>();
            String line;
            int count = 0;
            // Read lines in reverse order up to LogsWebSocketServerHelper.getMaxReadExistingLines lines
            while ((line = reader.readLine()) != null && count < serverHelper.getMaxReadExistingLines()) {
                lastLines.add(line);
                count++;
            }
            // Reverse to restore original order
            Collections.reverse(lastLines);
            // Process each line that matches the filter
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

    private void startLogTailing(WebSocketSession session, List<String> filters, File logFile) {
        // Create the listener to filter new log lines
        FilteredLogTailerListener listener = new FilteredLogTailerListener(session, filters);
        // Use 'true' to start tailing from the end of the file (since we've already processed existing lines)
        tailer = new Tailer(logFile, listener, 100, true);

        // Use an executor service for managing the tailer thread
        tailerExecutor = Executors.newSingleThreadExecutor();
        tailerExecutor.submit(tailer);
    }

    public void start(WebSocketSession session, String route) {
        LOGGER.debug("Starting log streaming for route: {}", route);
        // Resolve log file
        File logFile = new File(serverHelper.getLogFile());
        if (!logFile.exists() || !logFile.canRead()) {
            session.sendText("Log file not available or cannot be read.");
            return;
        }

        // (Optional) update server-side session with remote address if available
        try {
            String remoteStr = session.getAttr("remoteAddress");
            if (remoteStr != null && remoteStr.contains("/")) {
                // Netty: "/1.2.3.4:5678"
                String host = new InetSocketAddress(remoteStr, 0).getAddress().getHostAddress();
                serverHelper.updateSessionConnection(logsWebSession.getId(), host);
            } else if (remoteStr != null) {
                serverHelper.updateSessionConnection(logsWebSession.getId(), remoteStr);
            }
        } catch (Throwable ignore) {
        }

        // 1) Send backlog
        processExistingLines(session, logFile, logsWebSession.getFilters());

        // 2) Start tailer from end (since backlog was already sent)
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
    }

    private void stopLogsBroadcasting() {
        if (tailer != null) {
            tailer.stop();
        }
        if (tailerExecutor != null && !tailerExecutor.isShutdown()) {
            tailerExecutor.shutdownNow();
        }
        if (logsWebSession != null) {
            serverHelper.updateSessionConnection(logsWebSession.getId(), null);
        }
    }
}

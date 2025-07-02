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

import org.apache.cloudstack.logsws.LogsWebSession;
import org.apache.cloudstack.logsws.logreader.FilteredLogTailerListener;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.input.ReversedLinesFileReader;
import org.apache.commons.io.input.Tailer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.ReferenceCountUtil;

public class LogsWebSocketBroadcastHandler extends ChannelInboundHandlerAdapter {
    protected static Logger LOGGER = LogManager.getLogger(LogsWebSocketBroadcastHandler.class);
    private String route;
    private Tailer tailer;
    private ExecutorService tailerExecutor;
    private final LogsWebSocketServerHelper serverHelper;
    private LogsWebSession logsWebSession;

    public LogsWebSocketBroadcastHandler(final LogsWebSocketServerHelper serverHelper) {
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

    private void processExistingLines(final ChannelHandlerContext ctx, File logFile, final List<String> filters) {
        try (ReversedLinesFileReader reader = new ReversedLinesFileReader(logFile, StandardCharsets.UTF_8)) {
            List<String> lastLines = new ArrayList<>();
            String line;
            int count = 0;
            // Read lines in reverse order up to 200 lines
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
                    ctx.writeAndFlush(new TextWebSocketFrame(l));
                    isLastLineValid = true;
                } else {
                    isLastLineValid = false;
                }
            }
        } catch (IOException e) {
            ctx.writeAndFlush(new TextWebSocketFrame("Error reading existing log lines: " + e.getMessage()));
        }
    }

    private void startLogTailing(ChannelHandlerContext ctx, List<String> filters, File logFile) {
        // Create the listener to filter new log lines
        FilteredLogTailerListener listener = new FilteredLogTailerListener(filters, ctx.channel());
        // Use 'true' to start tailing from the end of the file (since we've already processed existing lines)
        tailer = new Tailer(logFile, listener, 100, true);

        // Use an executor service for managing the tailer thread
        tailerExecutor = Executors.newSingleThreadExecutor();
        tailerExecutor.submit(tailer);
    }

    private void startLogsBroadcasting(final ChannelHandlerContext ctx) {
        route = ctx.channel().attr(LogsWebSocketRoutingHandler.LOGGER_ROUTE_ATTR).get();
        logsWebSession = serverHelper.getSession(route);
        if (logsWebSession == null) {
            LOGGER.warn("Unauthorized session for route: {}", route);
            ctx.close();
            return;
        }
        File logFile = new File(serverHelper.getLogFile());
        if (!logFile.exists() || !logFile.canRead()) {
            ctx.channel().writeAndFlush(new TextWebSocketFrame("Log file not available or cannot be read."));
            return;
        }
        InetSocketAddress clientAddress = (InetSocketAddress) ctx.channel().remoteAddress();
        serverHelper.updateSessionConnection(logsWebSession.getId(), clientAddress.getAddress().getHostAddress());
        processExistingLines(ctx, logFile, logsWebSession.getFilters());
        startLogTailing(ctx, logsWebSession.getFilters(), logFile);
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

    @Override
    public void channelActive(final ChannelHandlerContext ctx) throws Exception {
        LOGGER.debug("Channel is active, context: {}", ctx.hashCode());
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        LOGGER.debug("Channel is being closed for route: {}, context: {}", route, ctx.hashCode());
        stopLogsBroadcasting();
        super.channelInactive(ctx);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        LOGGER.debug("User event triggered: {}, context: {}", evt, ctx.hashCode());
        if (evt instanceof WebSocketServerProtocolHandler.HandshakeComplete) {
            startLogsBroadcasting(ctx);
        } else if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            if (IdleState.WRITER_IDLE.equals(event.state())) {
                ctx.channel().writeAndFlush(new TextWebSocketFrame("Connection idle for 1 minute, closing connection."));
                ctx.close();
                return;
            }
        }
        super.userEventTriggered(ctx, evt);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        // Discard any messages received from the client.
        ReferenceCountUtil.release(msg);
    }
}

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

import org.apache.cloudstack.logsws.LogsWebSession;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.group.ChannelGroup;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.util.AttributeKey;

public class LogsWebSocketRoutingHandler extends ChannelInboundHandlerAdapter {
    protected static Logger LOGGER = LogManager.getLogger(LogsWebSocketRoutingHandler.class);
    public static final AttributeKey<String> LOGGER_ROUTE_ATTR = AttributeKey.valueOf("loggerRoute");
    private final LogsWebSocketRouteManager routeManager;
    private final LogsWebSocketServerHelper serverHelper;

    public LogsWebSocketRoutingHandler(LogsWebSocketRouteManager routeManager,
                                       LogsWebSocketServerHelper serverHelper) {
        this.routeManager = routeManager;
        this.serverHelper = serverHelper;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (!(msg instanceof FullHttpRequest)) {
            ctx.fireChannelRead(msg);
            return;
        }
        FullHttpRequest req = (FullHttpRequest) msg;
        String uri = req.uri();
        LOGGER.debug("Original URI: {}", uri);
        final String serverPath = serverHelper.getServerPath();
        final String expectedPathPrefix = serverPath + "/";
        if (!uri.startsWith(expectedPathPrefix)) {
            ctx.close();
            return;
        }
        // Extract the route portion.
        String route = uri.substring(expectedPathPrefix.length());
        if (route.isEmpty()) {
            ctx.close();
            return;
        }

        LogsWebSession session = serverHelper.getSession(route);
        if (session == null) {
            LOGGER.warn("Unauthorized connection attempt for route: {}", route);
            ctx.close();
            return;
        }
        // Retrieve or add the route.
        ChannelGroup group = routeManager.getRouteGroup(route);
        if (group == null) {
            routeManager.addRoute(route);
            group = routeManager.getRouteGroup(route);
        } else {
            // If there's already a connection, close it to allow only one connection per route.
            if (!group.isEmpty()) {
                LOGGER.debug("Closing existing connection(s) for route: {}", route);
                group.close(); // This will close all existing channels in the group.
            }
        }

        LOGGER.debug("Connecting to route: {} for context: {}", route, ctx.hashCode());
        ctx.channel().attr(LOGGER_ROUTE_ATTR).set(route);
        group.add(ctx.channel());

        // Rewrite the URI so that the handshake matches the expected sever path
        if (req instanceof DefaultFullHttpRequest) {
            ((DefaultFullHttpRequest) req).setUri(serverPath);
        } else {
            DefaultFullHttpRequest newReq = new DefaultFullHttpRequest(
                    req.protocolVersion(), req.method(), serverPath, req.content().retain());
            newReq.headers().setAll(req.headers());
            req.release();
            req = newReq;
        }
        LOGGER.debug("Rewritten URI: {}", req.uri());
        ctx.fireChannelRead(req);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        LOGGER.error("Exception in LoggerWebSocketRoutingHandler", cause);
        ctx.close();
    }
}

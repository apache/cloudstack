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

import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.timeout.IdleStateHandler;

public class LogsWebSocketServer {

    protected static Logger LOGGER = LogManager.getLogger(LogsWebSocketServer.class);

    private final int port;
    private final String path;
    private final int idleTimeoutSeconds;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;
    private boolean running;
    private final LogsWebSocketRouteManager routeManager;
    private final LogsWebSocketServerHelper serverHelper;

    public LogsWebSocketServer(final int port, final String path, final int idleTimeoutSeconds,
                               final LogsWebSocketServerHelper serverHelper) {
        this.port = port;
        this.path = path;
        this.idleTimeoutSeconds = idleTimeoutSeconds;
        this.serverHelper = serverHelper;
        this.routeManager = new LogsWebSocketRouteManager();
    }

    public void start() throws InterruptedException {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();
        ServerBootstrap b = new ServerBootstrap();
        b.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(new HttpServerCodec());
                        pipeline.addLast(new HttpObjectAggregator(65536));
                        pipeline.addLast(new LogsWebSocketRoutingHandler(routeManager, serverHelper));
                        pipeline.addLast(new WebSocketServerProtocolHandler(path, null, true));
                        pipeline.addLast("idleStateHandler", new IdleStateHandler(0, idleTimeoutSeconds, 0, TimeUnit.SECONDS));
                        pipeline.addLast(new LogsWebSocketBroadcastHandler(serverHelper));
                    }
                });

        // Bind and store the server channel.
        serverChannel = b.bind(port).sync().channel();
        LOGGER.debug("Logger WebSocket server started on port {}", port);
        // Note: We do not block here with serverChannel.closeFuture().sync()
        running = true;
    }

    // Stop the server gracefully.
    public void stop() {
        stop(5);
    }

    public void stop(long maxWaitSeconds) {
        try {
            if (serverChannel != null) {
                serverChannel.close().sync();
            }
            if (bossGroup != null) {
                bossGroup.shutdownGracefully(0, maxWaitSeconds, TimeUnit.SECONDS).sync();
            }
            if (workerGroup != null) {
                workerGroup.shutdownGracefully(0, maxWaitSeconds, TimeUnit.SECONDS).sync();
            }
        } catch (InterruptedException e) {
            LOGGER.error("Failed to stop WebSocket server properly with timeout {}s, forcefully stopping",
                    maxWaitSeconds, e);
            if (serverChannel != null && serverChannel.isOpen()) {
                serverChannel.close();
            }
            if (bossGroup != null && !bossGroup.isTerminated()) {
                bossGroup.shutdownGracefully(0, 0, TimeUnit.SECONDS);
            }
            if (workerGroup != null && !workerGroup.isTerminated()) {
                workerGroup.shutdownGracefully(0, 0, TimeUnit.SECONDS);
            }
        }
        LOGGER.debug("Logger WebSocket server stopped");
        running = false;
    }

    public boolean isRunning() {
        return running;
    }
}

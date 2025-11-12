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

package org.apache.cloudstack.framework.websocket.server;

import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.cloudstack.framework.websocket.server.common.WebSocketHandler;
import org.apache.cloudstack.framework.websocket.server.common.WebSocketRouter;
import org.apache.cloudstack.framework.websocket.server.common.WebSocketSession;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.timeout.IdleStateHandler;

public class WebSocketServerRoutingHandler extends SimpleChannelInboundHandler<WebSocketFrame> {
    protected static Logger LOGGER = LogManager.getLogger(WebSocketServerRoutingHandler.class);


    private final WebSocketRouter router;
    private WebSocketHandler handler;
    private WebSocketSession session;

    public WebSocketServerRoutingHandler(WebSocketRouter router) {
        this.router = router;
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof WebSocketServerProtocolHandler.HandshakeComplete) {
            WebSocketServerProtocolHandler.HandshakeComplete hc =
                    (WebSocketServerProtocolHandler.HandshakeComplete) evt;

            final String requestUri = hc.requestUri();
            final URI uri = URI.create(requestUri);
            final String rawQuery = uri.getQuery();
            String path = uri.getPath();
            LOGGER.trace("WebSocket connection for path: {}, query: {}", path, rawQuery);

            path = WebSocketRouter.stripWebSocketPathPrefix(uri.getPath());

            WebSocketRouter.ResolvedRoute rr = router.resolve(path);
            if (rr == null || rr.getHandler() == null) {
                ctx.close();
                return;
            }
            handler = rr.getHandler();

            long idleMs = (rr.getConfig() != null) ? rr.getConfig().getIdleTimeoutMillis() : 0L;
            if (idleMs > 0) {
                ctx.pipeline().addBefore(ctx.name(), "ws-idle",
                        new IdleStateHandler(0, 0, (int) TimeUnit.MILLISECONDS.toSeconds(idleMs)));
            }

            session = new NettyWebSocketSession(ctx.channel(), path, QueryUtils.parse(rawQuery));
            session.setAttr("remoteAddress", String.valueOf(ctx.channel().remoteAddress()));

            try {
                handler.onOpen(session);
            } catch (Throwable t) {
                try {
                    session.close(1011, "Open failed");
                } catch (Throwable ignore) {
                }
                ctx.close();
            }
        }

        super.userEventTriggered(ctx, evt);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame frame) {
        if (handler == null || session == null) {
            frame.release();
            return;
        }
        try {
            if (frame instanceof TextWebSocketFrame) {
                handler.onText(session, ((TextWebSocketFrame) frame).text());
            } else if (frame instanceof BinaryWebSocketFrame) {
                ByteBuffer buf = frame.content().nioBuffer();
                handler.onBinary(session, buf);
            } else if (frame instanceof CloseWebSocketFrame) {
                CloseWebSocketFrame c = (CloseWebSocketFrame) frame.retain();
                handler.onClose(session, c.statusCode(), c.reasonText());
                ctx.close();
            } else if (frame instanceof PingWebSocketFrame) {
                ctx.writeAndFlush(new PongWebSocketFrame(frame.content().retain()));
            }
        } catch (Throwable t) {
            try {
                handler.onError(session, t);
            } catch (Throwable ignore) {
            }
            ctx.close();
        } finally {
            frame.release();
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        LOGGER.debug("Channel inactive, closing session");
        if (handler != null && session != null) {
            try {
                handler.onClose(session, 1006, "Channel inactive");
            } catch (Throwable ignore) {
            }
        }
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (handler != null && session != null) {
            try {
                handler.onError(session, cause);
            } catch (Throwable ignore) {
            }
        }
        ctx.close();
    }

    // tiny query parser
    static final class QueryUtils {
        static Map<String, String> parse(String q) {
            if (q == null || q.isEmpty()) return java.util.Collections.emptyMap();
            java.util.Map<String, String> m = new java.util.HashMap<>();
            for (String kv : q.split("&")) {
                int i = kv.indexOf('=');
                String k = i >= 0 ? kv.substring(0, i) : kv;
                String v = i >= 0 ? kv.substring(i + 1) : "";
                m.put(urlDecode(k), urlDecode(v));
            }
            return m;
        }

        static String urlDecode(String s) {
            try {
                return java.net.URLDecoder.decode(s, StandardCharsets.UTF_8);
            } catch (Exception e) {
                return s;
            }
        }
    }
}

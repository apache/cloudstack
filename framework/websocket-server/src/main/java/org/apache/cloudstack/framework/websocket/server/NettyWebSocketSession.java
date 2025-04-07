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

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

import org.apache.cloudstack.framework.websocket.server.common.WebSocketSession;

import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

final class NettyWebSocketSession implements WebSocketSession {
    private final Channel ch;
    private final String path;
    private final Map<String, String> query;

    NettyWebSocketSession(Channel ch, String path, Map<String, String> query) {
        this.ch = Objects.requireNonNull(ch, "channel");
        this.path = path == null ? "" : path;
        this.query = (query == null) ? Collections.emptyMap() : Collections.unmodifiableMap(query);
    }

    @Override
    public String id() {
        return ch.id().asShortText();
    }

    @Override
    public String path() {
        return path;
    }

    @Override
    public Map<String, String> query() {
        return query;
    }

    @Override
    public void sendText(String text) {
        ch.writeAndFlush(new TextWebSocketFrame(text));
    }

    @Override
    public void sendBinary(ByteBuffer buf) {
        io.netty.buffer.ByteBuf bb = io.netty.buffer.Unpooled.wrappedBuffer(buf);
        ch.writeAndFlush(new BinaryWebSocketFrame(bb));
    }

    @Override
    public void close(int code, String reason) {
        ch.writeAndFlush(new io.netty.handler.codec.http.websocketx.CloseWebSocketFrame(code, reason))
                .addListener(io.netty.channel.ChannelFutureListener.CLOSE);
    }

    @Override
    public <T> void setAttr(String key, T val) {
        ch.attr(io.netty.util.AttributeKey.valueOf(key)).set(val);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getAttr(String key) {
        return (T) ch.attr(io.netty.util.AttributeKey.valueOf(key)).get();
    }

    Channel unwrap() {
        return ch;
    }
}
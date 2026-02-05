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
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.cloudstack.framework.websocket.server.common.WebSocketSession;
import org.eclipse.jetty.websocket.api.Session;

public final class JettyWebSocketSession implements WebSocketSession {
    private final Session jetty;
    private final String path;
    private final Map<String, String> query;
    private final String id;
    private final ConcurrentHashMap<String, Object> attrs = new ConcurrentHashMap<>();

    private JettyWebSocketSession(Session jetty, String path, Map<String, String> query) {
        this.jetty = jetty;
        this.path = path;
        this.query = query;
        // Make an opaque stable id; Jetty's Session doesn't expose a UUID
        this.id = Long.toHexString(System.identityHashCode(jetty)) + "-" + System.nanoTime();
    }

    public static JettyWebSocketSession adapt(Session s, String path, Map<String, String> query) {
        return new JettyWebSocketSession(s, path, query);
    }

    @Override
    public String id() {
        return id;
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
        try {
            // blocking send; if you prefer async: jetty.getRemote().sendStringByFuture(text).get();
            jetty.getRemote().sendString(text);
        } catch (IOException e) {
            throw new RuntimeException("sendText failed", e);
        }
    }

    @Override
    public void sendBinary(ByteBuffer buf) {
        try {
            jetty.getRemote().sendBytes(buf);
        } catch (IOException e) {
            throw new RuntimeException("sendBinary failed", e);
        }
    }

    @Override
    public void close(int code, String reason) {
        jetty.close(code, reason == null ? "" : reason);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getAttr(String key) {
        return (T) attrs.get(key);
    }

    @Override
    public <T> void setAttr(String key, T val) {
        if (val == null) attrs.remove(key);
        else attrs.put(key, val);
    }
}

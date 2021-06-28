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
package com.cloud.consoleproxy;

import java.io.IOException;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.cloud.consoleproxy.util.Logger;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketFrame;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.api.extensions.Frame;
import org.eclipse.jetty.websocket.server.WebSocketHandler;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

@WebSocket
public class ConsoleProxyNoVNCHandler extends WebSocketHandler {

    private ConsoleProxyNoVncClient viewer = null;
    private static final Logger s_logger = Logger.getLogger(ConsoleProxyNoVNCHandler.class);

    public ConsoleProxyNoVNCHandler() {
        super();
    }

    @Override
    public void configure(WebSocketServletFactory webSocketServletFactory) {
        webSocketServletFactory.register(ConsoleProxyNoVNCHandler.class);
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {

        if (this.getWebSocketFactory().isUpgradeRequest(request, response)) {
            response.addHeader("Sec-WebSocket-Protocol", "binary");
            if (this.getWebSocketFactory().acceptWebSocket(request, response)) {
                baseRequest.setHandled(true);
                return;
            }

            if (response.isCommitted()) {
                return;
            }
        }

        super.handle(target, baseRequest, request, response);
    }

    @OnWebSocketConnect
    public void onConnect(final Session session) throws IOException, InterruptedException {

        String queries = session.getUpgradeRequest().getQueryString();
        Map<String, String> queryMap = ConsoleProxyHttpHandlerHelper.getQueryMap(queries);

        String host = queryMap.get("host");
        String portStr = queryMap.get("port");
        String sid = queryMap.get("sid");
        String tag = queryMap.get("tag");
        String ticket = queryMap.get("ticket");
        String ajaxSessionIdStr = queryMap.get("sess");
        String console_url = queryMap.get("consoleurl");
        String console_host_session = queryMap.get("sessionref");
        String vm_locale = queryMap.get("locale");
        String hypervHost = queryMap.get("hypervHost");
        String username = queryMap.get("username");
        String password = queryMap.get("password");
        String sourceIP = queryMap.get("sourceIP");
        String websocketUrl = queryMap.get("websocketUrl");

        if (tag == null)
            tag = "";

        long ajaxSessionId = 0;
        int port;

        if (host == null || portStr == null || sid == null)
            throw new IllegalArgumentException();

        try {
            port = Integer.parseInt(portStr);
        } catch (NumberFormatException e) {
            s_logger.warn("Invalid number parameter in query string: " + portStr);
            throw new IllegalArgumentException(e);
        }

        if (ajaxSessionIdStr != null) {
            try {
                ajaxSessionId = Long.parseLong(ajaxSessionIdStr);
            } catch (NumberFormatException e) {
                s_logger.warn("Invalid number parameter in query string: " + ajaxSessionIdStr);
                throw new IllegalArgumentException(e);
            }
        }

        if (! checkSessionSourceIp(session, sourceIP)) {
            return;
        }

        try {
            ConsoleProxyClientParam param = new ConsoleProxyClientParam();
            param.setClientHostAddress(host);
            param.setClientHostPort(port);
            param.setClientHostPassword(sid);
            param.setClientTag(tag);
            param.setTicket(ticket);
            param.setClientTunnelUrl(console_url);
            param.setClientTunnelSession(console_host_session);
            param.setLocale(vm_locale);
            param.setHypervHost(hypervHost);
            param.setUsername(username);
            param.setPassword(password);
            param.setWebsocketUrl(websocketUrl);
            viewer = ConsoleProxy.getNoVncViewer(param, ajaxSessionIdStr, session);
        } catch (Exception e) {
            s_logger.warn("Failed to create viewer due to " + e.getMessage(), e);
            return;
        } finally {
            if (viewer == null) {
                session.disconnect();
            }
        }
    }

    private boolean checkSessionSourceIp(final Session session, final String sourceIP) throws IOException {
        // Verify source IP
        String sessionSourceIP = session.getRemoteAddress().getAddress().getHostAddress();
        s_logger.info("Get websocket connection request from remote IP : " + sessionSourceIP);
        if (ConsoleProxy.isSourceIpCheckEnabled && (sessionSourceIP == null || ! sessionSourceIP.equals(sourceIP))) {
            s_logger.warn("Failed to access console as the source IP to request the console is " + sourceIP);
            session.disconnect();
            return false;
        }
        return true;
    }

    @OnWebSocketClose
    public void onClose(Session session, int statusCode, String reason) throws IOException, InterruptedException {
        if (viewer != null) {
            ConsoleProxy.removeViewer(viewer);
        }
    }

    @OnWebSocketFrame
    public void onFrame(Frame f) throws IOException {
        viewer.sendClientFrame(f);
    }
}

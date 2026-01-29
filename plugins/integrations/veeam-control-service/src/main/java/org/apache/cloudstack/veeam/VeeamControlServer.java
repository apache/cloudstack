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

package org.apache.cloudstack.veeam;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.List;

import javax.servlet.DispatcherType;
import javax.servlet.http.HttpServletRequest;

import org.apache.cloudstack.utils.server.ServerPropertiesUtil;
import org.apache.cloudstack.veeam.api.ApiService;
import org.apache.cloudstack.veeam.filter.BearerOrBasicAuthFilter;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.RequestLog;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.RequestLogHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.jetbrains.annotations.NotNull;

public class VeeamControlServer {
    private static final Logger LOGGER = LogManager.getLogger(VeeamControlServer.class);

    private Server server;
    private List<RouteHandler> routeHandlers;

    public VeeamControlServer(List<RouteHandler> routeHandlers) {
        this.routeHandlers = new ArrayList<>(routeHandlers);
        this.routeHandlers.sort((a, b) -> Integer.compare(b.priority(), a.priority()));
    }

    public void startIfEnabled() throws Exception {
        final boolean enabled = VeeamControlService.Enabled.value();
        if (!enabled) {
            LOGGER.info("Veeam Control API server is disabled");
            return;
        }

        final String keystorePath = ServerPropertiesUtil.getKeystoreFile();
        final String keystorePassword = ServerPropertiesUtil.getKeystorePassword();
        final String keyManagerPassword = ServerPropertiesUtil.getKeystorePassword();
        final boolean sslConfigured = StringUtils.isNotEmpty(keystorePath) &&
                StringUtils.isNotEmpty(keystorePassword) &&
                StringUtils.isNotEmpty(keyManagerPassword) &&
                Files.exists(Paths.get(keystorePath));
        final String bind = VeeamControlService.BindAddress.value();
        final int port = VeeamControlService.Port.value();
        String ctxPath = VeeamControlService.ContextPath.value();
        LOGGER.info("Veeam Control server - bind: {}, port: {}, context: {} with {} handlers", bind, port, ctxPath,
                routeHandlers != null ? routeHandlers.size() : 0);


        server = new Server();

        if (sslConfigured) {
            final SslContextFactory.Server sslContextFactory = new SslContextFactory.Server();
            sslContextFactory.setKeyStorePath(keystorePath);
            sslContextFactory.setKeyStorePassword(keystorePassword);
            sslContextFactory.setKeyManagerPassword(keyManagerPassword);

            final HttpConfiguration https = new HttpConfiguration();
            https.setSecureScheme("https");
            https.setSecurePort(port);
            https.addCustomizer(new SecureRequestCustomizer());

            final ServerConnector httpsConnector = new ServerConnector(
                    server,
                    new SslConnectionFactory(sslContextFactory, "http/1.1"),
                    new HttpConnectionFactory(https)
            );
            httpsConnector.setHost(bind);
            httpsConnector.setPort(port);
            server.addConnector(httpsConnector);

            LOGGER.info("Veeam Control API server HTTPS enabled on {}:{}", bind, port);
        } else {
            final HttpConfiguration http = new HttpConfiguration();
            final ServerConnector httpConnector = new ServerConnector(server, new HttpConnectionFactory(http));
            httpConnector.setHost(bind);
            httpConnector.setPort(port);
            server.addConnector(httpConnector);

            LOGGER.warn("Veeam Control API server HTTPS is NOT configured (missing keystore path/passwords). " +
                    "Starting HTTP on {}:{} instead.", bind, port);
        }

        final ServletContextHandler ctx =
                new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
        ctx.setContextPath(ctxPath);

        // Bearer or Basic Auth for all routes
        ctx.addFilter(BearerOrBasicAuthFilter.class, ApiService.BASE_ROUTE + "/*", EnumSet.of(DispatcherType.REQUEST));

        // Front controller servlet
        ctx.addServlet(new ServletHolder(new VeeamControlServlet(routeHandlers)), "/*");

        // Create a RequestLog that logs every request handled by the server (all contexts/paths)
        server.setHandler(buildContextHandler(ctx));

        server.start();

        LOGGER.info("Started Veeam Control API server on {}:{} with context {}", bind, port, ctxPath);
    }

    @NotNull
    private static Handler buildContextHandler(ServletContextHandler ctx) {
        // Handler for root ('/') path
        final ServletContextHandler root = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
        root.setContextPath("/");
        root.addServlet(new ServletHolder(new javax.servlet.http.HttpServlet() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void doGet(javax.servlet.http.HttpServletRequest req, javax.servlet.http.HttpServletResponse resp)
                    throws java.io.IOException {
                resp.setContentType("text/plain");
                resp.setStatus(javax.servlet.http.HttpServletResponse.SC_OK);
                resp.getWriter().println("Veeam Control API");
            }

            @Override
            protected void doPost(javax.servlet.http.HttpServletRequest req, javax.servlet.http.HttpServletResponse resp)
                    throws java.io.IOException {
                doGet(req, resp);
            }
        }), "/*");

        final RequestLog requestLog = (request, response) -> {
            final String uri = request.getRequestURI() +
                    (request.getQueryString() != null ? "?" + request.getQueryString() : "");
            LOGGER.info("Request - remoteAddr: {}, method: {}, uri: {}, headers: {}, status: {}",
                    request.getRemoteAddr(),
                    request.getMethod(),
                    uri,
                    dumpRequestHeaders(request),
                    response.getStatus());
        };

        final RequestLogHandler requestLogHandler = new RequestLogHandler();
        requestLogHandler.setRequestLog(requestLog);

        // Attach both the configured context and the root handler; keep ctx first so contextPath has priority
        final HandlerList handlers = new HandlerList();
        handlers.setHandlers(new Handler[] { ctx, root });
        requestLogHandler.setHandler(handlers);
        return requestLogHandler;
    }

    public void stop() throws Exception {
    if (server != null) {
            server.stop();
            server = null;
        }
    }

    private static String dumpRequestHeaders(HttpServletRequest request) {
        final StringBuilder sb = new StringBuilder();
        final Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            final String name = headerNames.nextElement();
            final Enumeration<String> values = request.getHeaders(name);
            while (values.hasMoreElements()) {
                sb.append(name).append("=").append(values.nextElement()).append("; ");
            }
        }
        return sb.toString();
    }
}

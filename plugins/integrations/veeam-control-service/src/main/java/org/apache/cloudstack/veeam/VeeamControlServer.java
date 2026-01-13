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

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import javax.servlet.DispatcherType;

import org.apache.cloudstack.veeam.filter.BasicAuthFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

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

        final String bind = VeeamControlService.BindAddress.value();
        final int port = VeeamControlService.Port.value();
        String ctxPath = VeeamControlService.ContextPath.value();
        LOGGER.info("Veeam Control server - bind: {}, port: {}, context: {} with {} handlers", bind, port, ctxPath,
                routeHandlers != null ? routeHandlers.size() : 0);


        server = new Server(new InetSocketAddress(bind, port));

        final ServletContextHandler ctx =
                new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
        ctx.setContextPath(ctxPath);

        // Basic Auth for all routes
        ctx.addFilter(BasicAuthFilter.class, "/*", EnumSet.of(DispatcherType.REQUEST));

        // Front controller servlet
        ctx.addServlet(new ServletHolder(new VeeamControlServlet(routeHandlers)), "/*");

        server.setHandler(ctx);
        server.start();

        LOGGER.info("Started Veeam Control API server on {}:{} with context {}", bind, port, ctxPath);
    }

    public void stop() throws Exception {
    if (server != null) {
            server.stop();
            server = null;
        }
    }
}
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


import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.cloudstack.veeam.utils.Mapper;
import org.apache.cloudstack.veeam.utils.Negotiation;
import org.apache.cloudstack.veeam.utils.ResponseWriter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class VeeamControlServlet extends HttpServlet {
    private static final Logger LOGGER = LogManager.getLogger(VeeamControlServlet.class);
    private static final boolean LOG_REQUESTS = false;

    private final ResponseWriter writer;
    private final Mapper mapper;
    private final List<RouteHandler> routeHandlers;

    public VeeamControlServlet(List<RouteHandler> routeHandlers) {
        this.routeHandlers = routeHandlers;
        mapper = new Mapper();
        writer = new ResponseWriter(mapper);
    }

    public ResponseWriter getWriter() {
        return writer;
    }

    public Mapper getMapper() {
        return mapper;
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        String method = req.getMethod();
        String path = normalize(req.getPathInfo());
        Negotiation.OutFormat outFormat = Negotiation.responseFormat(req);

        LOGGER.info("Received {} request for {} with out format: {}", method, path, outFormat);

        logRequest(req, method, path);

        try {
            if ("/".equals(path)) {
                handleRoot(req, resp, outFormat);
                return;
            }

            if (CollectionUtils.isNotEmpty(this.routeHandlers)) {
                for (RouteHandler handler : this.routeHandlers) {
                    if (handler.canHandle(method, path)) {
                        handler.handle(req, resp, path, outFormat, this);
                        return;
                    }
                }
            }
            notFound(resp, null, outFormat);
        } catch (Error e) {
            writer.writeFault(resp, e.status, e.message, null, outFormat);
        }
    }

    private static void logRequest(HttpServletRequest req, String method, String path) {
        if (!LOG_REQUESTS) {
            return;
        }
        // Add a log to give all info about the request
        try {
            StringBuilder details = new StringBuilder();
            details.append("Request details: Method: ").append(method).append(", Path: ").append(path);
            details.append(", Query: ").append(req.getQueryString() == null ? "" : req.getQueryString());
            details.append(", Headers: ");
            java.util.Enumeration<String> headerNames = req.getHeaderNames();
            while (headerNames != null && headerNames.hasMoreElements()) {
                String name = headerNames.nextElement();
                details.append(name).append("=").append(req.getHeader(name)).append("; ");
            }
//            String body = "";
//            if (!"GET".equalsIgnoreCase(method)) {
//                StringBuilder bodySb = new StringBuilder();
//                java.io.BufferedReader reader = req.getReader();
//                if (reader != null) {
//                    String line;
//                    while ((line = reader.readLine()) != null) {
//                        bodySb.append(line).append('\n');
//                    }
//                }
//                body = bodySb.toString().trim();
//            }
//            details.append(", Body: ").append(body);
            LOGGER.debug(details.toString());
        } catch (Exception e) {
            LOGGER.debug("Failed to capture request details", e);
        }
    }

    private String normalize(String pathInfo) {
        if (pathInfo == null || pathInfo.isBlank()) return "/";
        return pathInfo;
    }

    protected void handleRoot(HttpServletRequest req, HttpServletResponse resp, Negotiation.OutFormat outFormat)
            throws IOException {

        String method = req.getMethod();
        if (!"GET".equals(method) && !"POST".equals(method)) {
            // You didn’t list 405; keep it simple with 400
            throw Error.badRequest("Unsupported method for root: " + method);
        }

        writer.write(resp, 200, Map.of(
                "name", "CloudStack Veeam Control Service",
                "pluginVersion", "0.1"), outFormat);
    }

    public void methodNotAllowed(final HttpServletResponse resp, final String allow, final Negotiation.OutFormat outFormat) throws IOException {
        resp.setHeader("Allow", allow);
        writer.writeFault(resp, HttpServletResponse.SC_METHOD_NOT_ALLOWED, "Method Not Allowed", "Allowed methods: " + allow, outFormat);
    }

    public void badRequest(final HttpServletResponse resp, String detail, Negotiation.OutFormat outFormat) throws IOException {
        writer.writeFault(resp, HttpServletResponse.SC_BAD_REQUEST, "Bad request", detail, outFormat);
    }


    public void notFound(final HttpServletResponse resp, String detail, Negotiation.OutFormat outFormat) throws IOException {
        writer.writeFault(resp, HttpServletResponse.SC_NOT_FOUND, "Not found", detail, outFormat);
    }

    public static class Error extends RuntimeException {
        final int status;
        final String message;
        public Error(int status, String message) {
            super(message);
            this.status = status;
            this.message = message;
        }
        public static Error badRequest(String msg) { return new Error(400, msg); }
        public static Error unauthorized(String msg) { return new Error(401, msg); }
    }
}

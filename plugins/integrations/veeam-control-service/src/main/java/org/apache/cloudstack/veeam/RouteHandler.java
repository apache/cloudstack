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

import java.io.BufferedReader;
import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.cloudstack.veeam.api.dto.ResourceAction;
import org.apache.cloudstack.veeam.utils.Negotiation;
import org.apache.logging.log4j.Logger;

import com.cloud.utils.component.Adapter;

public interface RouteHandler extends Adapter {
    default int priority() { return 0; }
    boolean canHandle(String method, String path) throws IOException;
    void handle(HttpServletRequest req, HttpServletResponse resp, String path, Negotiation.OutFormat outFormat, VeeamControlServlet io)
            throws IOException;

    default String getSanitizedPath(String path) {
        // remove query params if exists
        int qIdx = path.indexOf('?');
        if (qIdx != -1) {
            return path.substring(0, qIdx);
        }
        return path;
    }

    static String getRequestData(HttpServletRequest req, Logger logger) {
        String data = RouteHandler.getRequestData(req);
        if (VeeamControlService.DeveloperLogs.value()) {
            logger.debug("Received method: {} request. Request-data: {}", req.getMethod(), data);
        }
        return data;
    }

    static String getRequestData(HttpServletRequest req) {
        String contentType = req.getContentType();
        if (contentType == null) {
            return null;
        }
        String mime = contentType.split(";")[0].trim().toLowerCase();
        if (!"application/json".equals(mime) && !"application/x-www-form-urlencoded".equals(mime)) {
            return null;
        }
        try {
            StringBuilder data = new StringBuilder();
            String line;
            try (BufferedReader reader = req.getReader()) {
                while ((line = reader.readLine()) != null) {
                    data.append(line);
                }
            }
            return data.toString();
        } catch (IOException ignored) {
            return null;
        }
    }

    /**
     * Interprets true/false (case-insensitive) and 1/0 as boolean.
     * Returns defaultValue when the value is null, empty or unrecognized.
     */
    static boolean parseBoolean(String value, boolean defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        String trimmed = value.trim();
        if ("1".equals(trimmed) || Boolean.TRUE.toString().equalsIgnoreCase(trimmed)) {
            return true;
        }
        if ("0".equals(trimmed) || Boolean.FALSE.toString().equalsIgnoreCase(trimmed)) {
            return false;
        }
        return defaultValue;
    }

    /**
     * Returns whether the request asks for async processing. Mimics oVirt: the async parameter present
     * without a value (e.g. ?async) means true, while a missing or unrecognized value means false.
     */
    static boolean isRequestAsync(HttpServletRequest req) {
        String async = req.getParameter("async");
        if (async != null && async.isEmpty()) {
            return true;
        }
        return parseBoolean(async, false);
    }

    /**
     * Writes the response for a successful DELETE the way oVirt does: 202 Accepted without a body for
     * async requests, otherwise 200 OK with a completed action, linking the job when there is one.
     */
    static void writeDeleteResponse(final HttpServletResponse resp, final boolean async, final ResourceAction action,
                final Negotiation.OutFormat outFormat, final VeeamControlServlet io) throws IOException {
        if (async) {
            io.getWriter().write(resp, HttpServletResponse.SC_ACCEPTED, null, outFormat);
            return;
        }
        ResourceAction response = new ResourceAction();
        if (action != null) {
            response.setJob(action.getJob());
        }
        response.setStatus("complete");
        io.getWriter().write(resp, HttpServletResponse.SC_OK, response, outFormat);
    }
}

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

package org.apache.cloudstack.veeam.sso;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.cloudstack.veeam.RouteHandler;
import org.apache.cloudstack.veeam.VeeamControlService;
import org.apache.cloudstack.veeam.VeeamControlServlet;
import org.apache.cloudstack.veeam.utils.JwtUtil;
import org.apache.cloudstack.veeam.utils.Negotiation;
import org.apache.commons.lang3.StringUtils;

import com.cloud.utils.component.ManagerBase;

public class SsoService extends ManagerBase implements RouteHandler {
    private static final String BASE_ROUTE = "/sso";
    private static final long DEFAULT_TTL_SECONDS = 3600;
    public static final List<String> REQUIRED_SCOPES = List.of("ovirt-app-admin", "ovirt-app-portal");
    public static final String HMAC_SECRET = "change-this-super-secret-key-change-this";

    @Inject
    VeeamControlService veeamControlService;

    @Override
    public boolean canHandle(String method, String path) {
        return getSanitizedPath(path).startsWith(BASE_ROUTE);
    }

    @Override
    public void handle(HttpServletRequest req, HttpServletResponse resp, String path, Negotiation.OutFormat outFormat,
                       VeeamControlServlet io) throws IOException {
        final String sanitizedPath = getSanitizedPath(path);
        if (sanitizedPath.equals(BASE_ROUTE + "/oauth/token")) {
            handleToken(req, resp, outFormat, io);
            return;
        }

        io.notFound(resp, null, outFormat);
    }

    protected void handleToken(HttpServletRequest req, HttpServletResponse resp,
                               Negotiation.OutFormat outFormat, VeeamControlServlet io) throws IOException {

        if (!"POST".equalsIgnoreCase(req.getMethod())) {
            resp.setHeader("Allow", "POST");
            io.getWriter().write(resp, HttpServletResponse.SC_METHOD_NOT_ALLOWED,
                    Map.of("error", "method_not_allowed",
                            "message", "token endpoint requires POST"), outFormat);
            return;
        }

        final String grantType = trimToNull(req.getParameter("grant_type"));
        final String scope = trimToNull(req.getParameter("scope"));
        final String username = trimToNull(req.getParameter("username"));
        final String password = trimToNull(req.getParameter("password"));

        if (grantType == null) {
            io.getWriter().write(resp, HttpServletResponse.SC_BAD_REQUEST,
                    Map.of("error", "invalid_request",
                            "error_description", "Missing parameter: grant_type"), outFormat);
            return;
        }
        if (!"password".equals(grantType)) {
            io.getWriter().write(resp, HttpServletResponse.SC_BAD_REQUEST,
                    Map.of("error", "unsupported_grant_type",
                            "error_description", "Only grant_type=password is supported"), outFormat);
            return;
        }
        if (username == null || password == null) {
            io.getWriter().write(resp, HttpServletResponse.SC_BAD_REQUEST,
                    Map.of("error", "invalid_request",
                            "error_description", "Missing username/password"), outFormat);
            return;
        }

        if (!veeamControlService.validateCredentials(username, password)) {
            io.getWriter().write(resp, HttpServletResponse.SC_UNAUTHORIZED,
                    Map.of("error", "invalid_grant",
                            "error_description", "Invalid credentials"), outFormat);
            return;
        }

        final String effectiveScope = (scope == null) ? StringUtils.join(REQUIRED_SCOPES, " ") : scope;

        final long ttl = DEFAULT_TTL_SECONDS;
        long nowMillis = Instant.now().toEpochMilli();
        long expMillis = nowMillis + ttl * 1000L;
        final String token;
        try {
            token = JwtUtil.issueHs256Jwt(username, effectiveScope, ttl, HMAC_SECRET);
        } catch (Exception e) {
            io.getWriter().write(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    Map.of("error", "server_error",
                            "error_description", "Failed to issue token"), outFormat);
            return;
        }

        final Map<String, Object> payload = new HashMap<>();
        payload.put("access_token", token);
        payload.put("token_type", "bearer");
        payload.put("expires_in", ttl);
        payload.put("exp", expMillis);
        payload.put("scope", effectiveScope);

        io.getWriter().write(resp, HttpServletResponse.SC_OK, payload, outFormat);
    }

    private static String trimToNull(String s) {
        if (s == null) return null;
        s = s.trim();
        return s.isEmpty() ? null : s;
    }
}

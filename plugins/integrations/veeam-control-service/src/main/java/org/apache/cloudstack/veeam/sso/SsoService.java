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
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.cloudstack.veeam.RouteHandler;
import org.apache.cloudstack.veeam.VeeamControlService;
import org.apache.cloudstack.veeam.VeeamControlServlet;
import org.apache.cloudstack.veeam.filter.BearerOrBasicAuthFilter;
import org.apache.cloudstack.veeam.utils.Negotiation;

import com.cloud.utils.component.ManagerBase;

public class SsoService extends ManagerBase implements RouteHandler {
    private static final String BASE_ROUTE = "/sso";
    private static final long DEFAULT_TTL_SECONDS = 3600;

    // Replace with your real credential validation (CloudStack account, config, etc.)
    private final PasswordAuthenticator authenticator = new StaticPasswordAuthenticator();

    @Override
    public boolean canHandle(String method, String path) {
        return getSanitizedPath(path).startsWith(BASE_ROUTE);
    }

    @Override
    public void handle(HttpServletRequest req, HttpServletResponse resp, String path, Negotiation.OutFormat outFormat, VeeamControlServlet io) throws IOException {
        final String sanitizedPath = getSanitizedPath(path);
        if (sanitizedPath.equals(BASE_ROUTE + "/oauth/token")) {
            handleToken(req, resp, outFormat, io);
            return;
        }

        resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Not found");
    }

    protected void handleToken(HttpServletRequest req, HttpServletResponse resp,
                               Negotiation.OutFormat outFormat, VeeamControlServlet io) throws IOException {

        if (!"POST".equalsIgnoreCase(req.getMethod())) {
            // oVirt-like: 405 is fine; if you strictly want 400, change to SC_BAD_REQUEST
            resp.setHeader("Allow", "POST");
            io.getWriter().write(resp, HttpServletResponse.SC_METHOD_NOT_ALLOWED,
                    Map.of("error", "method_not_allowed", "message", "token endpoint requires POST"), outFormat);
            return;
        }

        // OAuth password grant uses x-www-form-urlencoded. With servlet containers this usually populates getParameter().
        final String grantType = trimToNull(req.getParameter("grant_type"));
        final String scope = trimToNull(req.getParameter("scope")); // typically "ovirt-app-api"
        final String username = trimToNull(req.getParameter("username"));
        final String password = trimToNull(req.getParameter("password"));

        if (grantType == null) {
            io.getWriter().write(resp, HttpServletResponse.SC_BAD_REQUEST,
                    Map.of("error", "invalid_request", "error_description", "Missing parameter: grant_type"), outFormat);
            return;
        }
        if (!"password".equals(grantType)) {
            io.getWriter().write(resp, HttpServletResponse.SC_BAD_REQUEST,
                    Map.of("error", "unsupported_grant_type", "error_description", "Only grant_type=password is supported"), outFormat);
            return;
        }
        if (username == null || password == null) {
            io.getWriter().write(resp, HttpServletResponse.SC_BAD_REQUEST,
                    Map.of("error", "invalid_request", "error_description", "Missing username/password"), outFormat);
            return;
        }

        if (!authenticator.authenticate(username, password)) {
            // 401 for bad creds
            io.getWriter().write(resp, HttpServletResponse.SC_UNAUTHORIZED,
                    Map.of("error", "invalid_grant", "error_description", "Invalid credentials"), outFormat);
            return;
        }

        final String effectiveScope = (scope == null) ? "ovirt-app-api" : scope;

        final long ttl = DEFAULT_TTL_SECONDS;
        final String token;
        try {
            token = JwtUtil.issueHs256Jwt(BearerOrBasicAuthFilter.ISSUER, username, effectiveScope, ttl,
                    BearerOrBasicAuthFilter.HMAC_SECRET);
        } catch (Exception e) {
            io.getWriter().write(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    Map.of("error", "server_error", "error_description", "Failed to issue token"), outFormat);
            return;
        }

        final Map<String, Object> payload = new HashMap<>();
        payload.put("access_token", token);
        payload.put("token_type", "bearer");
        payload.put("expires_in", ttl);
        payload.put("scope", effectiveScope);

        io.getWriter().write(resp, HttpServletResponse.SC_OK, payload, outFormat);
    }

    private static String trimToNull(String s) {
        if (s == null) return null;
        s = s.trim();
        return s.isEmpty() ? null : s;
    }

    // ---------- Minimal auth helpers (replace later) ----------

    interface PasswordAuthenticator {
        boolean authenticate(String username, String password);
    }

    static final class StaticPasswordAuthenticator implements PasswordAuthenticator {
        StaticPasswordAuthenticator() {
        }
        @Override
        public boolean authenticate(String username, String password) {
            return VeeamControlService.Username.value().equals(username) &&
                    VeeamControlService.Password.value().equals(password);
        }
    }

    // ---------- Minimal JWT HS256 without extra libs ----------
    // (If you prefer Nimbus, I can convert this to nimbus-jose-jwt; this keeps dependencies tiny.)

    static final class JwtUtil {
        static String issueHs256Jwt(String issuer, String subject, String scope, long ttlSeconds, String secret) throws Exception {
            long now = Instant.now().getEpochSecond();
            long exp = now + ttlSeconds;

            String headerJson = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
            String payloadJson =
                    "{"
                            + "\"iss\":\"" + jsonEscape(issuer) + "\","
                            + "\"sub\":\"" + jsonEscape(subject) + "\","
                            + "\"scope\":\"" + jsonEscape(scope) + "\","
                            + "\"iat\":" + now + ","
                            + "\"exp\":" + exp
                            + "}";

            String header = b64Url(headerJson.getBytes("UTF-8"));
            String payload = b64Url(payloadJson.getBytes("UTF-8"));
            String signingInput = header + "." + payload;

            byte[] sig = hmacSha256(signingInput.getBytes("UTF-8"), secret.getBytes("UTF-8"));
            return signingInput + "." + b64Url(sig);
        }

        static byte[] hmacSha256(byte[] data, byte[] key) throws Exception {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(data);
        }

        static String b64Url(byte[] in) {
            return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(in);
        }

        static String jsonEscape(String s) {
            return s.replace("\\", "\\\\").replace("\"", "\\\"");
        }
    }
}

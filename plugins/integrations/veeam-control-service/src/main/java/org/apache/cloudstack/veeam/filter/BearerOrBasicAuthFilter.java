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

package org.apache.cloudstack.veeam.filter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.cloudstack.veeam.VeeamControlService;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class BearerOrBasicAuthFilter implements Filter {

    // Keep these aligned with SsoService (move to ConfigKeys later)
    public static final List<String> REQUIRED_SCOPES = List.of("ovirt-app-admin", "ovirt-app-portal");
    public static final String ISSUER = "veeam-control";
    public static final String HMAC_SECRET = "change-this-super-secret-key-change-this";

    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    @Override public void init(FilterConfig filterConfig) {}
    @Override public void destroy() {}

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        final HttpServletRequest req = (HttpServletRequest) request;
        final HttpServletResponse resp = (HttpServletResponse) response;

        final String auth = req.getHeader("Authorization");
        if (auth != null && auth.regionMatches(true, 0, "Bearer ", 0, 7)) {
            final String token = auth.substring(7).trim();
            if (token.isEmpty()) {
                unauthorized(req, resp, "invalid_token", "Missing Bearer token");
                return;
            }
            if (!verifyJwtHs256(token)) {
                unauthorized(req, resp, "invalid_token", "Invalid or expired token");
                return;
            }
            chain.doFilter(request, response);
            return;
        }

        // Optional fallback: Basic (handy for manual testing).
        if (auth != null && auth.regionMatches(true, 0, "Basic ", 0, 6)) {
            if (!verifyBasic(auth.substring(6))) {
                unauthorized(req, resp, "invalid_client", "Invalid Basic credentials");
                return;
            }
            chain.doFilter(request, response);
            return;
        }

        unauthorized(req, resp, "invalid_token", "Missing Authorization");
    }

    private boolean verifyBasic(String b64) {
        final String expectedUser = VeeamControlService.Username.value();
        final String expectedPass = VeeamControlService.Password.value();

        final String decoded;
        try {
            decoded = new String(Base64.getDecoder().decode(b64), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return false;
        }

        final int idx = decoded.indexOf(':');
        if (idx <= 0) return false;

        final String user = decoded.substring(0, idx);
        final String pass = decoded.substring(idx + 1);

        return constantTimeEquals(user, expectedUser) && constantTimeEquals(pass, expectedPass);
    }

    /**
     * Minimal JWT verification:
     * - HS256 signature
     * - "iss" matches
     * - "exp" not expired
     * - "scope" contains REQUIRED_SCOPES (space-separated)
     *
     * NOTE: This does not parse JSON robustly; it’s sufficient for the token you mint in SsoService.
     * If you want robust parsing, switch to Nimbus and keep the rest the same.
     */
    private boolean verifyJwtHs256(String token) {
        final String[] parts = token.split("\\.");
        if (parts.length != 3) return false;

        final String headerB64 = parts[0];
        final String payloadB64 = parts[1];
        final String sigB64 = parts[2];

        final byte[] expectedSig;
        try {
            expectedSig = hmacSha256((headerB64 + "." + payloadB64).getBytes(StandardCharsets.UTF_8),
                    HMAC_SECRET.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            return false;
        }

        final byte[] providedSig;
        try {
            providedSig = Base64.getUrlDecoder().decode(sigB64);
        } catch (IllegalArgumentException e) {
            return false;
        }

        if (!constantTimeEquals(expectedSig, providedSig)) return false;

        Map<String, Object> payloadMap;
        try {
            String payloadJson = new String(Base64.getUrlDecoder().decode(payloadB64), StandardCharsets.UTF_8);
            payloadMap = JSON_MAPPER.readValue(
                    payloadJson,
                    new TypeReference<>() {}
            );
        } catch (IllegalArgumentException | JsonProcessingException e) {
            return false;
        }

        final String iss = (String)payloadMap.get("iss");
        final String scope = (String)payloadMap.get("scope");
        final Object expObj = payloadMap.get("exp");
        Long exp = null;
        if (expObj instanceof Number) {
            exp = ((Number) expObj).longValue();
        } else if (expObj instanceof String) {
            try {
                exp = Long.parseLong((String) expObj);
            } catch (NumberFormatException ignored) {}
        }

        if (!ISSUER.equals(iss)) {
            return false;
        }
        if (exp == null || Instant.now().getEpochSecond() >= exp) {
            return false;
        }
        return scope != null && hasRequiredScopes(scope);
    }

    private static boolean hasRequiredScopes(String scope) {
        String[] scopes = scope.split("\\s+");
        for (String required : REQUIRED_SCOPES) {
            if (!hasScope(scopes, required)) return false;
        }
        return true;
    }

    private static boolean hasScope(String[] scopes, String required) {
        for (String scope : scopes) {
            if (scope.equals(required)) {
                return true;
            }
        }
        return false;
    }

    private static byte[] hmacSha256(byte[] data, byte[] key) throws Exception {
        final Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(data);
    }

    private static void unauthorized(HttpServletRequest req, HttpServletResponse resp,
                                     String error, String desc) throws IOException {

        // IMPORTANT: don’t throw (your current filter throws and Jetty turns it into 500) :contentReference[oaicite:3]{index=3}
        resp.resetBuffer();
        resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        // Helpful for OAuth clients:
        resp.setHeader("WWW-Authenticate",
                "Bearer realm=\"Veeam Integration\", error=\"" + esc(error) + "\", error_description=\"" + esc(desc) + "\"");

        final String accept = req.getHeader("Accept");
        final boolean wantsJson = accept != null && accept.toLowerCase().contains("application/json");

        resp.setCharacterEncoding("UTF-8");
        if (wantsJson) {
            resp.setContentType("application/json; charset=UTF-8");
            resp.getWriter().write("{\"error\":\"" + esc(error) + "\",\"error_description\":\"" + esc(desc) + "\"}");
        } else {
            resp.setContentType("text/html; charset=UTF-8");
            resp.getWriter().write("<html><head><title>Error</title></head><body>Unauthorized</body></html>");
        }
        resp.getWriter().flush();
    }

    private static String esc(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) return false;
        return constantTimeEquals(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }

    private static boolean constantTimeEquals(byte[] x, byte[] y) {
        if (x.length != y.length) return false;
        int r = 0;
        for (int i = 0; i < x.length; i++) r |= x[i] ^ y[i];
        return r == 0;
    }
}

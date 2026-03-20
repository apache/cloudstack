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
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.cloudstack.veeam.VeeamControlService;
import org.apache.cloudstack.veeam.sso.SsoService;
import org.apache.cloudstack.veeam.utils.DataUtil;
import org.apache.cloudstack.veeam.utils.JwtUtil;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class BearerOrBasicAuthFilter implements Filter {
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    private final VeeamControlService veeamControlService;

    public BearerOrBasicAuthFilter(VeeamControlService veeamControlService) {
        this.veeamControlService = veeamControlService;
    }

    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public void destroy() {
    }

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

        return veeamControlService != null && veeamControlService.validateCredentials(user, pass);
    }

    /**
     * Minimal JWT verification:
     * - HS256 signature
     * - "iss" matches
     * - "exp" not expired
     * - "scope" contains REQUIRED_SCOPES (space-separated)
     */
    private boolean verifyJwtHs256(String token) {
        final String[] parts = token.split("\\.");
        if (parts.length != 3) return false;

        final String headerB64 = parts[0];
        final String payloadB64 = parts[1];
        final String sigB64 = parts[2];

        final byte[] expectedSig;
        try {
            expectedSig = JwtUtil.hmacSha256((headerB64 + "." + payloadB64).getBytes(StandardCharsets.UTF_8),
                    SsoService.HMAC_SECRET.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            return false;
        }

        final byte[] providedSig;
        try {
            providedSig = Base64.getUrlDecoder().decode(sigB64);
        } catch (IllegalArgumentException e) {
            return false;
        }

        if (!DataUtil.constantTimeEquals(expectedSig, providedSig)) return false;

        Map<String, Object> payloadMap;
        try {
            String payloadJson = new String(Base64.getUrlDecoder().decode(payloadB64), StandardCharsets.UTF_8);
            payloadMap = JSON_MAPPER.readValue(
                    payloadJson,
                    new TypeReference<>() {
                    }
            );
        } catch (IllegalArgumentException | JsonProcessingException e) {
            return false;
        }

        final String iss = (String) payloadMap.get("iss");
        final String scope = (String) payloadMap.get("scope");
        final Object expObj = payloadMap.get("exp");
        Long exp = null;
        if (expObj instanceof Number) {
            exp = ((Number) expObj).longValue();
        } else if (expObj instanceof String) {
            try {
                exp = Long.parseLong((String) expObj);
            } catch (NumberFormatException ignored) {
            }
        }

        if (!JwtUtil.ISSUER.equals(iss)) {
            return false;
        }
        if (exp == null || Instant.now().getEpochSecond() >= exp) {
            return false;
        }
        return scope != null && hasRequiredScopes(scope);
    }

    private static boolean hasRequiredScopes(String scope) {
        String[] scopes = scope.split("\\s+");
        for (String required : SsoService.REQUIRED_SCOPES) {
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

    private static void unauthorized(HttpServletRequest req, HttpServletResponse resp,
                                     String error, String desc) throws IOException {
        resp.resetBuffer();
        resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        // Helpful for OAuth clients:
        resp.setHeader("WWW-Authenticate",
                "Bearer realm=\"Veeam Integration\", error=\"" + DataUtil.jsonEscape(error) +
                        "\", error_description=\"" + DataUtil.jsonEscape(desc) + "\"");

        final String accept = req.getHeader("Accept");
        final boolean wantsJson = accept != null && accept.toLowerCase().contains("application/json");

        resp.setCharacterEncoding("UTF-8");
        if (wantsJson) {
            resp.setContentType("application/json; charset=UTF-8");
            resp.getWriter().write("{\"error\":\"" + DataUtil.jsonEscape(error) +
                    "\",\"error_description\":\"" + DataUtil.jsonEscape(desc) + "\"}");
        } else {
            resp.setContentType("text/html; charset=UTF-8");
            resp.getWriter().write("<html><head><title>Error</title></head><body>Unauthorized</body></html>");
        }
        resp.getWriter().flush();
    }
}

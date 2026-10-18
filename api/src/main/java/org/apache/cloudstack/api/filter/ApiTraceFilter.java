/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cloudstack.api.filter;

import org.apache.cloudstack.context.LogContext;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.UUID;
import javax.servlet.FilterConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

public class ApiTraceFilter implements Filter {

    // Cap the accepted trace id length to avoid log/DB bloat from a crafted header.
    private static final int MAX_TRACE_ID_LENGTH = 128;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            HttpServletRequest httpReq = (HttpServletRequest) request;
            String traceId = sanitizeTraceId(httpReq.getHeader(LogContext.TRACEID_KEY));
            if (StringUtils.isBlank(traceId)) {
                traceId = UUID.randomUUID().toString();
            }

            LogContext.current().putContextParameter(LogContext.TRACEID_KEY, traceId);
            chain.doFilter(request, response);
        } finally {
            LogContext.current().removeContextParameter(LogContext.TRACEID_KEY);
        }
    }

    /**
     * Returns the caller-supplied trace id only if it is safe to log and store: no control
     * characters (prevents log forging) and within a bounded length. Otherwise returns null so a
     * fresh id is generated.
     */
    private String sanitizeTraceId(String traceId) {
        if (traceId == null) {
            return null;
        }
        String trimmed = traceId.trim();
        if (trimmed.isEmpty() || trimmed.length() > MAX_TRACE_ID_LENGTH) {
            return null;
        }
        for (int i = 0; i < trimmed.length(); i++) {
            if (Character.isISOControl(trimmed.charAt(i))) {
                return null;
            }
        }
        return trimmed;
    }

    @Override
    public void destroy() {
    }
}

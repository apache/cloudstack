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
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            HttpServletRequest httpReq = (HttpServletRequest) request;
            String traceId = httpReq.getHeader(LogContext.X_B3_TRACEID_KEY);
            if (StringUtils.isBlank(traceId)) {
                traceId = UUID.randomUUID().toString();
            }

            LogContext.current().putContextParameter(LogContext.X_B3_TRACEID_KEY, traceId);
            chain.doFilter(request, response);
        } finally {
            LogContext.current().removeContextParameter(LogContext.X_B3_TRACEID_KEY);
        }
    }

    @Override
    public void destroy() {
    }
}

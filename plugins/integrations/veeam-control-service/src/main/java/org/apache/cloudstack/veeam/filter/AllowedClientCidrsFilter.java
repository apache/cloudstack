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
import java.net.InetAddress;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.cloudstack.veeam.VeeamControlService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cloud.utils.net.NetUtils;

public class AllowedClientCidrsFilter implements Filter {

    private static final Logger LOGGER = LogManager.getLogger(AllowedClientCidrsFilter.class);

    private final VeeamControlService veeamControlService;

    public AllowedClientCidrsFilter(VeeamControlService veeamControlService) {
        this.veeamControlService = veeamControlService;
    }

    @Override
    public void init(FilterConfig filterConfig) {
        // no-op
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {
            chain.doFilter(request, response);
            return;
        }

        final HttpServletRequest req = (HttpServletRequest) request;
        final HttpServletResponse resp = (HttpServletResponse) response;

        if (veeamControlService == null) {
            LOGGER.warn("Failed to inject VeeamControlService, allowing request by default");
            chain.doFilter(request, response);
            return;
        }

        final List<String> cidrList = veeamControlService.getAllowedClientCidrs();
        if (CollectionUtils.isEmpty(cidrList)) {
            chain.doFilter(request, response);
            return;
        }

        final String remoteAddr = req.getRemoteAddr();
        try {
            final InetAddress clientIp = InetAddress.getByName(remoteAddr);
            final boolean allowed = NetUtils.isIpInCidrList(clientIp, cidrList.toArray(new String[0]));
            if (!allowed) {
                LOGGER.warn("Rejected request from client IP {} not in allowed CIDRs {}", remoteAddr, cidrList);
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Forbidden");
                return;
            }
        } catch (Exception e) {
            LOGGER.warn("Rejected request failed to parse client IP {}: {}", remoteAddr, e.getMessage());
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Forbidden");
            return;
        }

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        // no-op
    }
}

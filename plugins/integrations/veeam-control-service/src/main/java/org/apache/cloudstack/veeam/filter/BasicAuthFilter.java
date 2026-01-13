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
import java.util.Base64;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.cloudstack.veeam.VeeamControlService;
import org.apache.cloudstack.veeam.VeeamControlServlet;

public class BasicAuthFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // no-op
    }

    @Override
    public void destroy() {
        // no-op
    }

    @Override
    public void doFilter(
            ServletRequest request,
            ServletResponse response,
            FilterChain chain
    ) throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        String expectedUser = VeeamControlService.Username.value();
        String expectedPass = VeeamControlService.Password.value();

        String auth = req.getHeader("Authorization");
        if (auth == null || !auth.regionMatches(true, 0, "Basic ", 0, 6)) {
            unauthorized(resp);
            return;
        }

        String decoded;
        try {
            decoded = new String(
                    Base64.getDecoder().decode(auth.substring(6)),
                    StandardCharsets.UTF_8
            );
        } catch (IllegalArgumentException e) {
            unauthorized(resp);
            return;
        }

        int idx = decoded.indexOf(':');
        if (idx <= 0) {
            unauthorized(resp);
            return;
        }

        String user = decoded.substring(0, idx);
        String pass = decoded.substring(idx + 1);

        if (!constantTimeEquals(user, expectedUser)
                || !constantTimeEquals(pass, expectedPass)) {
            unauthorized(resp);
            return;
        }

        chain.doFilter(request, response);
    }

    private void unauthorized(HttpServletResponse resp) {
        throw VeeamControlServlet.Error.unauthorized("Unauthorized");
    }

    private boolean constantTimeEquals(String a, String b) {
        byte[] x = a.getBytes(StandardCharsets.UTF_8);
        byte[] y = b.getBytes(StandardCharsets.UTF_8);
        if (x.length != y.length) return false;
        int r = 0;
        for (int i = 0; i < x.length; i++) {
            r |= x[i] ^ y[i];
        }
        return r == 0;
    }
}

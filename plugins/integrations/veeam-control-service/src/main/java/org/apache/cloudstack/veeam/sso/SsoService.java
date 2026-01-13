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
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.cloudstack.veeam.RouteHandler;
import org.apache.cloudstack.veeam.VeeamControlServlet;
import org.apache.cloudstack.veeam.utils.Negotiation;

import com.cloud.utils.component.ManagerBase;

public class SsoService extends ManagerBase implements RouteHandler {

    @Override
    public boolean canHandle(String method, String path) {
        return path.startsWith("/sso");
    }

    @Override
    public void handle(HttpServletRequest req, HttpServletResponse resp, String path, Negotiation.OutFormat outFormat, VeeamControlServlet io) throws IOException {
        if ("/sso/oauth/token".equals(path)) {
            handleToken(req, resp, outFormat, io);
            return;
        }

        resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Not found");
    }

    protected void handleToken(HttpServletRequest req, HttpServletResponse resp, Negotiation.OutFormat outFormat, VeeamControlServlet io)
            throws IOException {

        // Typically POST; you only listed 200/400/401 -> treat others as 400
        if (!"POST".equals(req.getMethod())) {
            throw VeeamControlServlet.Error.badRequest("token endpoint requires POST");
        }

        // Assume x-www-form-urlencoded for OAuth token requests (common)
        String grantType = req.getParameter("grant_type");
        if (grantType == null || grantType.isBlank()) {
            throw VeeamControlServlet.Error.badRequest("Missing parameter: grant_type");
        }

        // NOTE: 401 is normally handled by BasicAuthFilter; keep hook here if you later move auth here.
        // if (!authorized) throw VeeamControlServlet.Error.unauthorized("Unauthorized");

        io.getWriter().write(resp, 200, Map.of(
                "access_token", "dummy-token",
                "token_type", "bearer",
                "expires_in", 3600
        ), outFormat);
    }
}

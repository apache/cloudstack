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

package org.apache.cloudstack.veeam.api;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.cloudstack.veeam.RouteHandler;
import org.apache.cloudstack.veeam.VeeamControlServlet;
import org.apache.cloudstack.veeam.adapter.ServerAdapter;
import org.apache.cloudstack.veeam.api.dto.Host;
import org.apache.cloudstack.veeam.api.dto.Hosts;
import org.apache.cloudstack.veeam.utils.Negotiation;
import org.apache.cloudstack.veeam.utils.PathUtil;
import org.apache.commons.collections.CollectionUtils;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.utils.component.ManagerBase;

public class HostsRouteHandler extends ManagerBase implements RouteHandler {
    public static final String BASE_ROUTE = "/api/hosts";

    @Inject
    ServerAdapter serverAdapter;

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public int priority() {
        return 5;
    }

    @Override
    public boolean canHandle(String method, String path) {
        return getSanitizedPath(path).startsWith(BASE_ROUTE);
    }

    @Override
    public void handle(HttpServletRequest req, HttpServletResponse resp, String path, Negotiation.OutFormat outFormat, VeeamControlServlet io) throws IOException {
        final String method = req.getMethod();
        if (!"GET".equalsIgnoreCase(method)) {
            io.methodNotAllowed(resp, "GET", outFormat);
            return;
        }
        final String sanitizedPath = getSanitizedPath(path);
        if (sanitizedPath.equals(BASE_ROUTE)) {
            handleGet(req, resp, outFormat, io);
            return;
        }

        List<String> idAndSubPath = PathUtil.extractIdAndSubPath(sanitizedPath, BASE_ROUTE);
        if (CollectionUtils.isNotEmpty(idAndSubPath)) {
            String id = idAndSubPath.get(0);
            if (idAndSubPath.size() == 1) {
                handleGetById(id, resp, outFormat, io);
                return;
            }
        }

        io.notFound(resp, null, outFormat);
    }

    protected void handleGet(final HttpServletRequest req, final HttpServletResponse resp,
                          Negotiation.OutFormat outFormat, VeeamControlServlet io) throws IOException {
        final List<Host> result = serverAdapter.listAllHosts();
        final Hosts response = new Hosts(result);

        io.getWriter().write(resp, HttpServletResponse.SC_OK, response, outFormat);
    }

    protected void handleGetById(final String id, final HttpServletResponse resp, final Negotiation.OutFormat outFormat,
                              final VeeamControlServlet io) throws IOException {
        try {
            Host response = serverAdapter.getHost(id);
            io.getWriter().write(resp, HttpServletResponse.SC_OK, response, outFormat);
        } catch (InvalidParameterValueException e) {
            io.notFound(resp, e.getMessage(), outFormat);
        }
    }
}

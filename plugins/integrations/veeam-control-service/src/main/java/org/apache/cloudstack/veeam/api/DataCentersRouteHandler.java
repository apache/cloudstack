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
import org.apache.cloudstack.veeam.api.dto.DataCenter;
import org.apache.cloudstack.veeam.api.dto.NamedList;
import org.apache.cloudstack.veeam.api.dto.Network;
import org.apache.cloudstack.veeam.api.dto.StorageDomain;
import org.apache.cloudstack.veeam.api.request.ListQuery;
import org.apache.cloudstack.veeam.utils.Negotiation;
import org.apache.cloudstack.veeam.utils.PathUtil;
import org.apache.commons.collections.CollectionUtils;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.utils.component.ManagerBase;

public class DataCentersRouteHandler extends ManagerBase implements RouteHandler {
    public static final String BASE_ROUTE = "/api/datacenters";

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
            } else if (idAndSubPath.size() == 2) {
                String subPath = idAndSubPath.get(1);
                if ("storagedomains".equals(subPath)) {
                    handleGetStorageDomainsByDcId(id, req, resp, outFormat, io);
                    return;
                }
                if ("networks".equals(subPath)) {
                    handleGetNetworksByDcId(id, req, resp, outFormat, io);
                    return;
                }
            }
        }

        io.notFound(resp, null, outFormat);
    }

    protected void handleGet(final HttpServletRequest req, final HttpServletResponse resp,
                          Negotiation.OutFormat outFormat, VeeamControlServlet io) throws IOException {
        ListQuery query = ListQuery.fromRequest(req);
        final List<DataCenter> result = serverAdapter.listAllDataCenters(query.getOffset(), query.getLimit());
        NamedList<DataCenter> response = NamedList.of("data_center", result);
        io.getWriter().write(resp, HttpServletResponse.SC_OK, response, outFormat);
    }

    protected void handleGetById(final String id, final HttpServletResponse resp, final Negotiation.OutFormat outFormat,
                              final VeeamControlServlet io) throws IOException {
        try {
            DataCenter response = serverAdapter.getDataCenter(id);
            io.getWriter().write(resp, HttpServletResponse.SC_OK, response, outFormat);
        } catch (InvalidParameterValueException e) {
            io.notFound(resp, e.getMessage(), outFormat);
        }
    }

    protected void handleGetStorageDomainsByDcId(final String id, final HttpServletRequest req,
             final HttpServletResponse resp, final Negotiation.OutFormat outFormat, final VeeamControlServlet io)
            throws IOException {
        try {
            ListQuery query = ListQuery.fromRequest(req);
            List<StorageDomain> storageDomains = serverAdapter.listStorageDomainsByDcId(id, query.getPage(),
                    query.getMax());
            NamedList<StorageDomain> response = NamedList.of("storage_domain", storageDomains);
            io.getWriter().write(resp, HttpServletResponse.SC_OK, response, outFormat);
        } catch (InvalidParameterValueException e) {
            io.notFound(resp, e.getMessage(), outFormat);
        } catch (PermissionDeniedException e) {
            io.badRequest(resp, e.getMessage(), outFormat);
        }
    }

    protected void handleGetNetworksByDcId(final String id, final HttpServletRequest req,
           final HttpServletResponse resp, final Negotiation.OutFormat outFormat, final VeeamControlServlet io)
            throws IOException {
        try {
            ListQuery query = ListQuery.fromRequest(req);
            List<Network> networks = serverAdapter.listNetworksByDcId(id, query.getPage(),
                    query.getMax());
            NamedList<Network> response = NamedList.of("network", networks);
            io.getWriter().write(resp, HttpServletResponse.SC_OK, response, outFormat);
        } catch (InvalidParameterValueException e) {
            io.notFound(resp, e.getMessage(), outFormat);
        } catch (PermissionDeniedException e) {
            io.badRequest(resp, e.getMessage(), outFormat);
        }
    }
}

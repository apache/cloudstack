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
import org.apache.cloudstack.veeam.api.dto.Disk;
import org.apache.cloudstack.veeam.api.dto.NamedList;
import org.apache.cloudstack.veeam.api.request.ListQuery;
import org.apache.cloudstack.veeam.utils.Negotiation;
import org.apache.cloudstack.veeam.utils.PathUtil;
import org.apache.commons.collections.CollectionUtils;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.exception.CloudRuntimeException;
import com.fasterxml.jackson.core.JsonProcessingException;

public class DisksRouteHandler extends ManagerBase implements RouteHandler {
    public static final String BASE_ROUTE = "/api/disks";

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
        final String sanitizedPath = getSanitizedPath(path);
        if (sanitizedPath.equals(BASE_ROUTE)) {
            if ("GET".equalsIgnoreCase(method)) {
                handleGet(req, resp, outFormat, io);
                return;
            }
            if ("POST".equalsIgnoreCase(method)) {
                handlePost(req, resp, outFormat, io);
                return;
            }
        }

        List<String> idAndSubPath = PathUtil.extractIdAndSubPath(sanitizedPath, BASE_ROUTE);
        if (CollectionUtils.isNotEmpty(idAndSubPath)) {
            String id = idAndSubPath.get(0);
            if (idAndSubPath.size() == 1) {
                if (!"GET".equalsIgnoreCase(method) && !"DELETE".equalsIgnoreCase(method) &&
                        !"PUT".equalsIgnoreCase(method)) {
                    io.methodNotAllowed(resp, "GET, DELETE, PUT", outFormat);
                    return;
                }
                if ("GET".equalsIgnoreCase(method)) {
                    handleGetById(id, resp, outFormat, io);
                    return;
                }
                if ("DELETE".equalsIgnoreCase(method)) {
                    handleDeleteById(id, resp, outFormat, io);
                    return;
                }
                if ("PUT".equalsIgnoreCase(method)) {
                    handlePutById(id, req, resp, outFormat, io);
                    return;
                }
            } else if (idAndSubPath.size() == 2) {
                String subPath = idAndSubPath.get(1);
                if ("copy".equals(subPath)) {
                    if ("POST".equalsIgnoreCase(method)) {
                        handlePostDiskCopy(id, req, resp, outFormat, io);
                    } else {
                        io.methodNotAllowed(resp, "POST", outFormat);
                    }
                    return;
                } else if ("reduce".equals(subPath)) {
                    if ("POST".equalsIgnoreCase(method)) {
                        handlePostDiskReduce(id, req, resp, outFormat, io);
                    } else {
                        io.methodNotAllowed(resp, "POST", outFormat);
                    }
                    return;
                }
            }
        }

        io.notFound(resp, null, outFormat);
    }

    protected void handleGet(final HttpServletRequest req, final HttpServletResponse resp,
                          Negotiation.OutFormat outFormat, VeeamControlServlet io) throws IOException {
        try {
            ListQuery query = ListQuery.fromRequest(req);
            final List<Disk> result = serverAdapter.listAllDisks(query.getOffset(), query.getLimit());
            NamedList<Disk> response = NamedList.of("disk", result);
            io.getWriter().write(resp, HttpServletResponse.SC_OK, response, outFormat);
        } catch (CloudRuntimeException e) {
            io.badRequest(resp, e.getMessage(), outFormat);
        }
    }

    protected void handlePost(final HttpServletRequest req, final HttpServletResponse resp,
                          Negotiation.OutFormat outFormat, VeeamControlServlet io) throws IOException {
        String data = RouteHandler.getRequestData(req, logger);
        try {
            Disk request = io.getMapper().jsonMapper().readValue(data, Disk.class);
            Disk response = serverAdapter.createDisk(request);
            io.getWriter().write(resp, HttpServletResponse.SC_CREATED, response, outFormat);
        } catch (JsonProcessingException | CloudRuntimeException e) {
            io.badRequest(resp, e.getMessage(), outFormat);
        }
    }

    protected void handleGetById(final String id, final HttpServletResponse resp, final Negotiation.OutFormat outFormat,
                              final VeeamControlServlet io) throws IOException {
        try {
            Disk response = serverAdapter.getDisk(id);
            io.getWriter().write(resp, HttpServletResponse.SC_OK, response, outFormat);
        } catch (InvalidParameterValueException e) {
            io.notFound(resp, e.getMessage(), outFormat);
        }
    }

    protected void handleDeleteById(final String id, final HttpServletResponse resp, final Negotiation.OutFormat outFormat,
                                 final VeeamControlServlet io) throws IOException {
        try {
            serverAdapter.deleteDisk(id);
            io.getWriter().write(resp, HttpServletResponse.SC_OK, "Deleted disk ID: " + id, outFormat);
        } catch (InvalidParameterValueException e) {
            io.badRequest(resp, e.getMessage(), outFormat);
        }
    }

    protected void handlePutById(final String id, final HttpServletRequest req, final HttpServletResponse resp,
                         final Negotiation.OutFormat outFormat, final VeeamControlServlet io) throws IOException {
        String data = RouteHandler.getRequestData(req, logger);
        throw new InvalidParameterValueException("Put Disk with ID " + id + " not implemented");
    }

    protected void handlePostDiskCopy(final String id, final HttpServletRequest req, final HttpServletResponse resp,
                                  final Negotiation.OutFormat outFormat, final VeeamControlServlet io) throws IOException {
        String data = RouteHandler.getRequestData(req, logger);
        try {
            Disk response = serverAdapter.copyDisk(id);
            io.getWriter().write(resp, HttpServletResponse.SC_OK, response, outFormat);
        } catch (InvalidParameterValueException e) {
            io.badRequest(resp, e.getMessage(), outFormat);
        }
    }

    protected void handlePostDiskReduce(final String id, final HttpServletRequest req, final HttpServletResponse resp,
                                  final Negotiation.OutFormat outFormat, final VeeamControlServlet io) throws IOException {
        String data = RouteHandler.getRequestData(req, logger);
        try {
            Disk response = serverAdapter.reduceDisk(id);
            io.getWriter().write(resp, HttpServletResponse.SC_OK, response, outFormat);
        } catch (InvalidParameterValueException e) {
            io.badRequest(resp, e.getMessage(), outFormat);
        }
    }
}

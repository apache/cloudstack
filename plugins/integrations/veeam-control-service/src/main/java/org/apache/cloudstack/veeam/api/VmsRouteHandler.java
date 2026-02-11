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
import java.util.Set;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.cloudstack.veeam.RouteHandler;
import org.apache.cloudstack.veeam.VeeamControlServlet;
import org.apache.cloudstack.veeam.adapter.ServerAdapter;
import org.apache.cloudstack.veeam.api.dto.DiskAttachment;
import org.apache.cloudstack.veeam.api.dto.DiskAttachments;
import org.apache.cloudstack.veeam.api.dto.Nic;
import org.apache.cloudstack.veeam.api.dto.Nics;
import org.apache.cloudstack.veeam.api.dto.Snapshot;
import org.apache.cloudstack.veeam.api.dto.Snapshots;
import org.apache.cloudstack.veeam.api.dto.Vm;
import org.apache.cloudstack.veeam.api.dto.VmAction;
import org.apache.cloudstack.veeam.api.dto.Vms;
import org.apache.cloudstack.veeam.api.request.VmListQuery;
import org.apache.cloudstack.veeam.api.request.VmSearchExpr;
import org.apache.cloudstack.veeam.api.request.VmSearchFilters;
import org.apache.cloudstack.veeam.api.request.VmSearchParser;
import org.apache.cloudstack.veeam.utils.Negotiation;
import org.apache.cloudstack.veeam.utils.PathUtil;
import org.apache.commons.collections.CollectionUtils;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.exception.CloudRuntimeException;
import com.fasterxml.jackson.core.JsonProcessingException;

public class VmsRouteHandler extends ManagerBase implements RouteHandler {
    public static final String BASE_ROUTE = "/api/vms";
    private static final int DEFAULT_MAX = 50;
    private static final int HARD_CAP_MAX = 1000;
    private static final int DEFAULT_PAGE = 1;

    @Inject
    ServerAdapter serverAdapter;

    private VmSearchParser searchParser;

    @Override
    public boolean start() {

        this.searchParser = new VmSearchParser(Set.of(
                "id", "name", "status", "cluster", "host", "template"
        ));
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
            if (!"GET".equalsIgnoreCase(method) && !"POST".equalsIgnoreCase(method) && !"DELETE".equalsIgnoreCase(method)) {
                io.methodNotAllowed(resp, "GET, POST, DELETE", outFormat);
                return;
            }
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
                if (!"GET".equalsIgnoreCase(method) && !"PUT".equalsIgnoreCase(method) && !"DELETE".equalsIgnoreCase(method)) {
                    io.methodNotAllowed(resp, "GET, PUT, DELETE", outFormat);
                } else if ("GET".equalsIgnoreCase(method)) {
                    handleGetById(id, resp, outFormat, io);
                } else if ("PUT".equalsIgnoreCase(method)) {
                    handleUpdateById(id, req, resp, outFormat, io);
                } else if ("DELETE".equalsIgnoreCase(method)) {
                    handleDeleteById(id, resp, outFormat, io);
                }
                return;
            } else if (idAndSubPath.size() == 2) {
                String subPath = idAndSubPath.get(1);
                if ("start".equals(subPath)) {
                    if ("POST".equalsIgnoreCase(method)) {
                        handleStartVmById(id, req, resp, outFormat, io);
                    } else {
                         io.methodNotAllowed(resp, "POST", outFormat);
                    }
                    return;
                } else if ("stop".equals(subPath)) {
                    if ("POST".equalsIgnoreCase(method)) {
                        handleStopVmById(id, req, resp, outFormat, io);
                    } else {
                        io.methodNotAllowed(resp, "POST", outFormat);
                    }
                    return;
                } else if ("shutdown".equals(subPath)) {
                    if ("POST".equalsIgnoreCase(method)) {
                        handleShutdownVmById(id, req, resp, outFormat, io);
                    } else {
                        io.methodNotAllowed(resp, "POST", outFormat);
                    }
                    return;
                } else if ("diskattachments".equals(subPath)) {
                    if (!"GET".equalsIgnoreCase(method) && !"POST".equalsIgnoreCase(method)) {
                        io.methodNotAllowed(resp, "GET, POST", outFormat);
                    } else if ("GET".equalsIgnoreCase(method)) {
                        handleGetDiskAttachmentsByVmId(id, resp, outFormat, io);
                    } else if ("POST".equalsIgnoreCase(method)) {
                        handlePostDiskAttachmentForVmId(id, req, resp, outFormat, io);
                    }
                    return;
                } else if ("nics".equals(subPath)) {
                    if (!"GET".equalsIgnoreCase(method) && !"POST".equalsIgnoreCase(method)) {
                        io.methodNotAllowed(resp, "GET, POST", outFormat);
                    } else if ("GET".equalsIgnoreCase(method)) {
                        handleGetNicsByVmId(id, resp, outFormat, io);
                    } else if ("POST".equalsIgnoreCase(method)) {
                        handlePostNicForVmId(id, req, resp, outFormat, io);
                    }
                    return;
                } else if ("snapshots".equals(subPath)) {
                    if (!"GET".equalsIgnoreCase(method) && !"POST".equalsIgnoreCase(method)) {
                        io.methodNotAllowed(resp, "GET, POST", outFormat);
                    } else if ("GET".equalsIgnoreCase(method)) {
                        handleGetSnapshotsByVmId(id, resp, outFormat, io);
                    } else if ("POST".equalsIgnoreCase(method)) {
                        handlePostSnapshotForVmId(id, req, resp, outFormat, io);
                    }
                    return;
                }
            } else if (idAndSubPath.size() == 3) {
                String subPath = idAndSubPath.get(1);
                String subId = idAndSubPath.get(2);
                if ("snapshots".equals(subPath)) {
                    if (!"GET".equalsIgnoreCase(method) && !"DELETE".equalsIgnoreCase(method)) {
                        io.methodNotAllowed(resp, "GET, DELETE", outFormat);
                    } else if ("GET".equalsIgnoreCase(method)) {
                        handleGetSnapshotsById(subId, resp, outFormat, io);
                    } else if ("DELETE".equalsIgnoreCase(method)) {
                        handleDeleteSnapshotById(subId, req, resp, outFormat, io);
                    }
                    return;
                }
            } else if (idAndSubPath.size() == 4) {
                String subPath = idAndSubPath.get(1);
                String subId = idAndSubPath.get(2);
                String action = idAndSubPath.get(3);
                if ("snapshots".equals(subPath) && "restore".equals(action)) {
                    if ("POST".equalsIgnoreCase(method)) {
                        handleRestoreSnapshotById(subId, req, resp, outFormat, io);
                    } else {
                        io.methodNotAllowed(resp, "POST", outFormat);
                    }
                    return;
                }
            }
        }

        io.notFound(resp, null, outFormat);
    }

    protected String getRequestData(final HttpServletRequest req) {
        String data = RouteHandler.getRequestData(req);
        logger.info("Received method: {} request. Request-data: {}", req.getMethod(), data);
        return data;
    }

    protected void handleGet(final HttpServletRequest req, final HttpServletResponse resp,
          Negotiation.OutFormat outFormat, VeeamControlServlet io) throws IOException {
        final VmListQuery q = fromRequest(req);

        // Validate max/page early (optional strictness)
        if (q.getMax() != null && q.getMax() <= 0) {
            io.notFound(resp, "Invalid 'max' (must be > 0)", outFormat);
            return;
        }
        if (q.getPage() != null && q.getPage() <= 0) {
            io.notFound(resp, "Invalid 'page' (must be > 0)", outFormat);
            return;
        }

        final int limit = q.resolvedMax(DEFAULT_MAX, HARD_CAP_MAX);
        final int offset = q.offset(DEFAULT_MAX, HARD_CAP_MAX, DEFAULT_PAGE);

        final VmSearchExpr expr;
        try {
            expr = searchParser.parse(q.getSearch());
        } catch (VmSearchParser.VmSearchParseException e) {
            io.notFound(resp, "Invalid search: " + e.getMessage(), outFormat);
            return;
        }

        final VmSearchFilters filters;
        try {
            filters = VmSearchFilters.fromAndOnly(expr); // AND-only v1
        } catch (VmSearchParser.VmSearchParseException e) {
            io.notFound(resp, "Unsupported search: " + e.getMessage(), outFormat);
            return;
        }

        final List<Vm> result = serverAdapter.listAllInstances();
        final Vms response = new Vms(result);

        io.getWriter().write(resp, HttpServletResponse.SC_OK, response, outFormat);
    }

    protected static VmListQuery fromRequest(final HttpServletRequest req) {
        final VmListQuery q = new VmListQuery();
        q.setSearch(req.getParameter("search"));
        q.setMax(parseIntOrNull(req.getParameter("max")));
        q.setPage(parseIntOrNull(req.getParameter("page")));
        return q;
    }

    protected static Integer parseIntOrNull(final String s) {
        if (s == null || s.trim().isEmpty()) return null;
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return Integer.valueOf(-1); // will be rejected by validation above
        }
    }

    protected void handlePost(final HttpServletRequest req, final HttpServletResponse resp,
                              Negotiation.OutFormat outFormat, VeeamControlServlet io) throws IOException {
        String data = getRequestData(req);
        try {
            Vm request = io.getMapper().jsonMapper().readValue(data, Vm.class);
            Vm response = serverAdapter.createInstance(request);
            io.getWriter().write(resp, HttpServletResponse.SC_CREATED, response, outFormat);
        } catch (JsonProcessingException | CloudRuntimeException e) {
            io.badRequest(resp, e.getMessage(), outFormat);
        }
    }

    protected void handleGetById(final String id, final HttpServletResponse resp, final Negotiation.OutFormat outFormat,
          final VeeamControlServlet io) throws IOException {
        try {
            Vm response = serverAdapter.getInstance(id);
            io.getWriter().write(resp, HttpServletResponse.SC_OK, response, outFormat);
        } catch (InvalidParameterValueException e) {
            io.notFound(resp, e.getMessage(), outFormat);
        }
    }

    protected void handleUpdateById(final String id, final HttpServletRequest req, final HttpServletResponse resp, final Negotiation.OutFormat outFormat,
                                    final VeeamControlServlet io) throws IOException {
        String data = RouteHandler.getRequestData(req);
        logger.info("Received PUT request. Request-data: {}", data);
        try {
            Vm request = io.getMapper().jsonMapper().readValue(data, Vm.class);
            Vm response = serverAdapter.updateInstance(id, request);
            io.getWriter().write(resp, HttpServletResponse.SC_OK, response, outFormat);
        } catch (InvalidParameterValueException e) {
            io.notFound(resp, e.getMessage(), outFormat);
        }
    }

    protected void handleDeleteById(final String id, final HttpServletResponse resp, final Negotiation.OutFormat outFormat,
                                 final VeeamControlServlet io) throws IOException {
        try {
            serverAdapter.deleteInstance(id);
            io.getWriter().write(resp, HttpServletResponse.SC_OK, "", outFormat);
        } catch (CloudRuntimeException e) {
            io.notFound(resp, e.getMessage(), outFormat);
        }
    }

    protected void handleStartVmById(final String id, final HttpServletRequest req, final HttpServletResponse resp, final Negotiation.OutFormat outFormat,
            final VeeamControlServlet io) throws IOException {
        try {
            VmAction vm = serverAdapter.startInstance(id);
            io.getWriter().write(resp, HttpServletResponse.SC_ACCEPTED, vm, outFormat);
        } catch (CloudRuntimeException e) {
            io.notFound(resp, e.getMessage(), outFormat);
        }
    }

    protected void handleStopVmById(final String id, final HttpServletRequest req, final HttpServletResponse resp, final Negotiation.OutFormat outFormat,
                                     final VeeamControlServlet io) throws IOException {
        try {
            VmAction vm = serverAdapter.stopInstance(id);
            io.getWriter().write(resp, HttpServletResponse.SC_ACCEPTED, vm, outFormat);
        } catch (CloudRuntimeException e) {
            io.notFound(resp, e.getMessage(), outFormat);
        }
    }

    protected void handleShutdownVmById(final String id, final HttpServletRequest req, final HttpServletResponse resp, final Negotiation.OutFormat outFormat,
                                    final VeeamControlServlet io) throws IOException {
        try {
            VmAction vm = serverAdapter.shutdownInstance(id);
            io.getWriter().write(resp, HttpServletResponse.SC_ACCEPTED, vm, outFormat);
        } catch (CloudRuntimeException e) {
            io.notFound(resp, e.getMessage(), outFormat);
        }
    }

    protected void handleGetDiskAttachmentsByVmId(final String id, final HttpServletResponse resp, final Negotiation.OutFormat outFormat,
                                                  final VeeamControlServlet io) throws IOException {
        try {
            List<DiskAttachment> disks = serverAdapter.listDiskAttachmentsByInstanceUuid(id);
            DiskAttachments response = new DiskAttachments(disks);
            io.getWriter().write(resp, HttpServletResponse.SC_OK, response, outFormat);
        } catch (InvalidParameterValueException e) {
            io.notFound(resp, e.getMessage(), outFormat);
        }
    }

    protected void handlePostDiskAttachmentForVmId(final String id, final HttpServletRequest req,
             final HttpServletResponse resp, final Negotiation.OutFormat outFormat, final VeeamControlServlet io)
            throws IOException {
        String data = getRequestData(req);
        try {
            DiskAttachment request = io.getMapper().jsonMapper().readValue(data, DiskAttachment.class);
            DiskAttachment response = serverAdapter.handleInstanceAttachDisk(id, request);
            io.getWriter().write(resp, HttpServletResponse.SC_CREATED, response, outFormat);
        } catch (JsonProcessingException | CloudRuntimeException e) {
            io.badRequest(resp, e.getMessage(), outFormat);
        }
    }

    protected void handleGetNicsByVmId(final String id, final HttpServletResponse resp, final Negotiation.OutFormat outFormat,
                                                  final VeeamControlServlet io) throws IOException {
        try {
            List<Nic> nics = serverAdapter.listNicsByInstanceUuid(id);
            Nics response = new Nics(nics);
            io.getWriter().write(resp, HttpServletResponse.SC_OK, response, outFormat);
        } catch (InvalidParameterValueException e) {
            io.notFound(resp, e.getMessage(), outFormat);
        }
    }

    protected void handlePostNicForVmId(final String id, final HttpServletRequest req,
               final HttpServletResponse resp, final Negotiation.OutFormat outFormat, final VeeamControlServlet io)
            throws IOException {
        String data = getRequestData(req);
        try {
            Nic request = io.getMapper().jsonMapper().readValue(data, Nic.class);
            Nic response = serverAdapter.handleAttachInstanceNic(id, request);
            io.getWriter().write(resp, HttpServletResponse.SC_CREATED, response, outFormat);
        } catch (JsonProcessingException | CloudRuntimeException e) {
            io.badRequest(resp, e.getMessage(), outFormat);
        }
    }

    protected void handleGetSnapshotsByVmId(final String id, final HttpServletResponse resp,
                final Negotiation.OutFormat outFormat, final VeeamControlServlet io) throws IOException {
        try {
            List<Snapshot> snapshots = serverAdapter.listSnapshotsByInstanceUuid(id);
            Snapshots response = new Snapshots(snapshots);
            io.getWriter().write(resp, HttpServletResponse.SC_OK, response, outFormat);
        } catch (InvalidParameterValueException e) {
            io.notFound(resp, e.getMessage(), outFormat);
        }
    }

    protected void handlePostSnapshotForVmId(final String id, final HttpServletRequest req,
                 final HttpServletResponse resp, final Negotiation.OutFormat outFormat, final VeeamControlServlet io)
            throws IOException {
        String data = getRequestData(req);
        try {
            Snapshot request = io.getMapper().jsonMapper().readValue(data, Snapshot.class);
            Snapshot response = serverAdapter.handleCreateInstanceSnapshot(id, request);
            io.getWriter().write(resp, HttpServletResponse.SC_ACCEPTED, response, outFormat);
        } catch (JsonProcessingException | CloudRuntimeException e) {
            io.badRequest(resp, e.getMessage(), outFormat);
        }
    }

    protected void handleGetSnapshotsById(final String id, final HttpServletResponse resp,
                  final Negotiation.OutFormat outFormat, final VeeamControlServlet io) throws IOException {
        try {
            Snapshot response = serverAdapter.getSnapshot(id);
            io.getWriter().write(resp, HttpServletResponse.SC_OK, response, outFormat);
        } catch (InvalidParameterValueException e) {
            io.notFound(resp, e.getMessage(), outFormat);
        }
    }

    protected void handleDeleteSnapshotById(final String id, final HttpServletRequest req,
                final HttpServletResponse resp, final Negotiation.OutFormat outFormat, final VeeamControlServlet io)
            throws IOException {
        boolean async = Boolean.parseBoolean(req.getParameter("async"));
        try {
            Snapshot snapshot = serverAdapter.deleteSnapshot(id, async);
            if (snapshot != null) {
                io.getWriter().write(resp, HttpServletResponse.SC_ACCEPTED, null, outFormat);
            } else {
                io.getWriter().write(resp, HttpServletResponse.SC_ACCEPTED, null, outFormat);
            }
        } catch (CloudRuntimeException e) {
            io.badRequest(resp, e.getMessage(), outFormat);
        }
    }

    protected void handleRestoreSnapshotById(final String id, final HttpServletRequest req,
                final HttpServletResponse resp, final Negotiation.OutFormat outFormat, final VeeamControlServlet io)
            throws IOException {
        String data = getRequestData(req);
        io.badRequest(resp, "Not implemented", outFormat);
    }
}

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
import org.apache.cloudstack.veeam.api.dto.Backup;
import org.apache.cloudstack.veeam.api.dto.Checkpoint;
import org.apache.cloudstack.veeam.api.dto.Disk;
import org.apache.cloudstack.veeam.api.dto.DiskAttachment;
import org.apache.cloudstack.veeam.api.dto.NamedList;
import org.apache.cloudstack.veeam.api.dto.Nic;
import org.apache.cloudstack.veeam.api.dto.ResourceAction;
import org.apache.cloudstack.veeam.api.dto.Snapshot;
import org.apache.cloudstack.veeam.api.dto.Vm;
import org.apache.cloudstack.veeam.api.dto.VmAction;
import org.apache.cloudstack.veeam.api.request.ListQuery;
import org.apache.cloudstack.veeam.utils.Negotiation;
import org.apache.cloudstack.veeam.utils.PathUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.exception.CloudRuntimeException;
import com.fasterxml.jackson.core.JsonProcessingException;

public class VmsRouteHandler extends ManagerBase implements RouteHandler {
    public static final String BASE_ROUTE = "/api/vms";

    @Inject
    ServerAdapter serverAdapter;

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
                    handleGetById(id, req, resp, outFormat, io);
                } else if ("PUT".equalsIgnoreCase(method)) {
                    handleUpdateById(id, req, resp, outFormat, io);
                } else if ("DELETE".equalsIgnoreCase(method)) {
                    handleDeleteById(id, req, resp, outFormat, io);
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
                } else if ("backups".equals(subPath)) {
                    if (!"GET".equalsIgnoreCase(method) && !"POST".equalsIgnoreCase(method)) {
                        io.methodNotAllowed(resp, "GET, POST", outFormat);
                    } else if ("GET".equalsIgnoreCase(method)) {
                        handleGetBackupsByVmId(id, resp, outFormat, io);
                    } else if ("POST".equalsIgnoreCase(method)) {
                        handlePostBackupForVmId(id, req, resp, outFormat, io);
                    }
                    return;
                } else if ("checkpoints".equals(subPath)) {
                    if ("GET".equalsIgnoreCase(method)) {
                        handleGetCheckpointsByVmId(id, resp, outFormat, io);
                    } else {
                        io.methodNotAllowed(resp, "GET, POST", outFormat);
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
                        handleGetSnapshotById(subId, resp, outFormat, io);
                    } else if ("DELETE".equalsIgnoreCase(method)) {
                        handleDeleteSnapshotById(subId, req, resp, outFormat, io);
                    }
                    return;
                } else if ("backups".equals(subPath)) {
                    if ("GET".equalsIgnoreCase(method)) {
                        handleGetBackupById(subId, resp, outFormat, io);
                    } else {
                        io.methodNotAllowed(resp, "GET", outFormat);
                    }
                    return;
                } else if ("checkpoints".equals(subPath)) {
                    if ("DELETE".equalsIgnoreCase(method)) {
                        handleDeleteCheckpoint(id, subId, resp, outFormat, io);
                    } else {
                        io.methodNotAllowed(resp, "DELETE", outFormat);
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
                } else if ("backups".equals(subPath) && "disks".equals(action)) {
                    if ("GET".equalsIgnoreCase(method)) {
                        handleGetBackupDisksById(subId, req, resp, outFormat, io);
                    } else {
                        io.methodNotAllowed(resp, "GET", outFormat);
                    }
                    return;
                } else if ("backups".equals(subPath) && "finalize".equals(action)) {
                    if ("POST".equalsIgnoreCase(method)) {
                        handleFinalizeBackupById(id, subId, req, resp, outFormat, io);
                    } else {
                        io.methodNotAllowed(resp, "POST", outFormat);
                    }
                    return;
                }
            }
        }

        io.notFound(resp, null, outFormat);
    }

    protected static boolean isRequestAsync(HttpServletRequest req) {
        String asyncStr = req.getParameter("async");
        return Boolean.TRUE.toString().equals(asyncStr);
    }

    protected void handleGet(final HttpServletRequest req, final HttpServletResponse resp,
          Negotiation.OutFormat outFormat, VeeamControlServlet io) throws IOException {
        try {
            ListQuery query = ListQuery.fromRequest(req);
            final List<Vm> result = serverAdapter.listAllInstances(query.getOffset(), query.getLimit());
            NamedList<Vm> response = NamedList.of("vm", result);
            io.getWriter().write(resp, HttpServletResponse.SC_OK, response, outFormat);
        } catch (CloudRuntimeException e) {
            io.badRequest(resp, e.getMessage(), outFormat);
        }
    }

    protected void handlePost(final HttpServletRequest req, final HttpServletResponse resp,
                              Negotiation.OutFormat outFormat, VeeamControlServlet io) throws IOException {
        String data = RouteHandler.getRequestData(req, logger);
        try {
            Vm request = io.getMapper().jsonMapper().readValue(data, Vm.class);
            Vm response = serverAdapter.createInstance(request);
            io.getWriter().write(resp, HttpServletResponse.SC_CREATED, response, outFormat);
        } catch (JsonProcessingException | CloudRuntimeException e) {
            io.badRequest(resp, e.getMessage(), outFormat);
        }
    }

    protected void handleGetById(final String id, final HttpServletRequest req, final HttpServletResponse resp,
             final Negotiation.OutFormat outFormat, final VeeamControlServlet io) throws IOException {
        String followStr = req.getParameter("follow");
        boolean includeTags = false;
        boolean includeDisks = false;
        boolean includeNics = false;
        if (StringUtils.isNotBlank(followStr)) {
            Set<String> followParts = java.util.Arrays.stream(followStr.split(","))
                   .map(String::trim)
                   .filter(s -> !s.isEmpty())
                   .collect(java.util.stream.Collectors.toSet());
            includeTags = followParts.contains("tags");
            includeDisks = followParts.contains("disk_attachments.disk");
            includeNics = followParts.contains("nics.reporteddevices");
        }
        boolean allContent = Boolean.parseBoolean(req.getParameter("all_content"));
        try {
            Vm response = serverAdapter.getInstance(id, includeTags, includeDisks, includeNics, allContent);
            io.getWriter().write(resp, HttpServletResponse.SC_OK, response, outFormat);
        } catch (InvalidParameterValueException e) {
            io.notFound(resp, e.getMessage(), outFormat);
        }
    }

    protected void handleUpdateById(final String id, final HttpServletRequest req, final HttpServletResponse resp, final Negotiation.OutFormat outFormat,
                                    final VeeamControlServlet io) throws IOException {
        String data = RouteHandler.getRequestData(req, logger);
        try {
            Vm request = io.getMapper().jsonMapper().readValue(data, Vm.class);
            Vm response = serverAdapter.updateInstance(id, request);
            io.getWriter().write(resp, HttpServletResponse.SC_OK, response, outFormat);
        } catch (InvalidParameterValueException e) {
            io.notFound(resp, e.getMessage(), outFormat);
        }
    }

    protected void handleDeleteById(final String id, final HttpServletRequest req, final HttpServletResponse resp,
                final Negotiation.OutFormat outFormat, final VeeamControlServlet io) throws IOException {
        boolean async = isRequestAsync(req);
        try {
            VmAction vm = serverAdapter.deleteInstance(id, async);
            io.getWriter().write(resp, HttpServletResponse.SC_OK, vm, outFormat);
        } catch (CloudRuntimeException e) {
            io.notFound(resp, e.getMessage(), outFormat);
        }
    }

    protected void handleStartVmById(final String id, final HttpServletRequest req, final HttpServletResponse resp,
             final Negotiation.OutFormat outFormat, final VeeamControlServlet io) throws IOException {
        boolean async = isRequestAsync(req);
        try {
            VmAction vm = serverAdapter.startInstance(id, async);
            io.getWriter().write(resp, HttpServletResponse.SC_ACCEPTED, vm, outFormat);
        } catch (CloudRuntimeException e) {
            io.notFound(resp, e.getMessage(), outFormat);
        }
    }

    protected void handleStopVmById(final String id, final HttpServletRequest req, final HttpServletResponse resp,
                final Negotiation.OutFormat outFormat, final VeeamControlServlet io) throws IOException {
        boolean async = isRequestAsync(req);
        try {
            VmAction vm = serverAdapter.stopInstance(id, async);
            io.getWriter().write(resp, HttpServletResponse.SC_ACCEPTED, vm, outFormat);
        } catch (CloudRuntimeException e) {
            io.notFound(resp, e.getMessage(), outFormat);
        }
    }

    protected void handleShutdownVmById(final String id, final HttpServletRequest req, final HttpServletResponse resp,
                final Negotiation.OutFormat outFormat, final VeeamControlServlet io) throws IOException {
        boolean async = isRequestAsync(req);
        try {
            VmAction vm = serverAdapter.shutdownInstance(id, async);
            io.getWriter().write(resp, HttpServletResponse.SC_ACCEPTED, vm, outFormat);
        } catch (CloudRuntimeException e) {
            io.notFound(resp, e.getMessage(), outFormat);
        }
    }

    protected void handleGetDiskAttachmentsByVmId(final String id, final HttpServletResponse resp,
                  final Negotiation.OutFormat outFormat, final VeeamControlServlet io) throws IOException {
        try {
            List<DiskAttachment> disks = serverAdapter.listDiskAttachmentsByInstanceUuid(id);
            NamedList<DiskAttachment> response = NamedList.of("disk_attachment", disks);
            io.getWriter().write(resp, HttpServletResponse.SC_OK, response, outFormat);
        } catch (InvalidParameterValueException e) {
            io.notFound(resp, e.getMessage(), outFormat);
        }
    }

    protected void handlePostDiskAttachmentForVmId(final String id, final HttpServletRequest req,
             final HttpServletResponse resp, final Negotiation.OutFormat outFormat, final VeeamControlServlet io)
            throws IOException {
        String data = RouteHandler.getRequestData(req, logger);
        try {
            DiskAttachment request = io.getMapper().jsonMapper().readValue(data, DiskAttachment.class);
            DiskAttachment response = serverAdapter.attachInstanceDisk(id, request);
            io.getWriter().write(resp, HttpServletResponse.SC_CREATED, response, outFormat);
        } catch (JsonProcessingException | CloudRuntimeException e) {
            io.badRequest(resp, e.getMessage(), outFormat);
        }
    }

    protected void handleGetNicsByVmId(final String id, final HttpServletResponse resp,
                   final Negotiation.OutFormat outFormat, final VeeamControlServlet io) throws IOException {
        try {
            List<Nic> nics = serverAdapter.listNicsByInstanceUuid(id);
            NamedList<Nic> response = NamedList.of("nic", nics);
            io.getWriter().write(resp, HttpServletResponse.SC_OK, response, outFormat);
        } catch (InvalidParameterValueException e) {
            io.notFound(resp, e.getMessage(), outFormat);
        }
    }

    protected void handlePostNicForVmId(final String id, final HttpServletRequest req,
               final HttpServletResponse resp, final Negotiation.OutFormat outFormat, final VeeamControlServlet io)
            throws IOException {
        String data = RouteHandler.getRequestData(req, logger);
        try {
            Nic request = io.getMapper().jsonMapper().readValue(data, Nic.class);
            Nic response = serverAdapter.attachInstanceNic(id, request);
            io.getWriter().write(resp, HttpServletResponse.SC_CREATED, response, outFormat);
        } catch (JsonProcessingException | CloudRuntimeException e) {
            io.badRequest(resp, e.getMessage(), outFormat);
        }
    }

    protected void handleGetSnapshotsByVmId(final String id, final HttpServletResponse resp,
                final Negotiation.OutFormat outFormat, final VeeamControlServlet io) throws IOException {
        try {
            List<Snapshot> snapshots = serverAdapter.listSnapshotsByInstanceUuid(id);
            NamedList<Snapshot> response = NamedList.of("snapshot", snapshots);
            io.getWriter().write(resp, HttpServletResponse.SC_OK, response, outFormat);
        } catch (InvalidParameterValueException e) {
            io.notFound(resp, e.getMessage(), outFormat);
        }
    }

    protected void handlePostSnapshotForVmId(final String id, final HttpServletRequest req,
                 final HttpServletResponse resp, final Negotiation.OutFormat outFormat, final VeeamControlServlet io)
            throws IOException {
        String data = RouteHandler.getRequestData(req, logger);
        try {
            Snapshot request = io.getMapper().jsonMapper().readValue(data, Snapshot.class);
            Snapshot response = serverAdapter.createInstanceSnapshot(id, request);
            io.getWriter().write(resp, HttpServletResponse.SC_ACCEPTED, response, outFormat);
        } catch (JsonProcessingException | CloudRuntimeException e) {
            io.badRequest(resp, e.getMessage(), outFormat);
        }
    }

    protected void handleGetSnapshotById(final String id, final HttpServletResponse resp,
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
        boolean async = isRequestAsync(req);
        try {
            ResourceAction action = serverAdapter.deleteSnapshot(id, async);
            if (action != null) {
                io.getWriter().write(resp, HttpServletResponse.SC_ACCEPTED, action, outFormat);
            } else {
                io.getWriter().write(resp, HttpServletResponse.SC_OK, null, outFormat);
            }
        } catch (CloudRuntimeException e) {
            io.badRequest(resp, e.getMessage(), outFormat);
        }
    }

    protected void handleRestoreSnapshotById(final String id, final HttpServletRequest req,
                final HttpServletResponse resp, final Negotiation.OutFormat outFormat, final VeeamControlServlet io)
            throws IOException {
        boolean async = isRequestAsync(req);
        String data = RouteHandler.getRequestData(req, logger);
        try {
            ResourceAction response = serverAdapter.revertInstanceToSnapshot(id, async);
            io.getWriter().write(resp, HttpServletResponse.SC_ACCEPTED, response, outFormat);
        } catch (CloudRuntimeException e) {
            io.badRequest(resp, e.getMessage(), outFormat);
        }
    }

    protected void handleGetBackupsByVmId(final String id, final HttpServletResponse resp,
              final Negotiation.OutFormat outFormat, final VeeamControlServlet io) throws IOException {
        try {
            List<Backup> backups = serverAdapter.listBackupsByInstanceUuid(id);
            NamedList<Backup> response = NamedList.of("backup", backups);
            io.getWriter().write(resp, HttpServletResponse.SC_OK, response, outFormat);
        } catch (InvalidParameterValueException e) {
            io.notFound(resp, e.getMessage(), outFormat);
        }
    }

    protected void handlePostBackupForVmId(final String id, final HttpServletRequest req,
               final HttpServletResponse resp, final Negotiation.OutFormat outFormat, final VeeamControlServlet io)
            throws IOException {
        String data = RouteHandler.getRequestData(req, logger);
        try {
            Backup request = io.getMapper().jsonMapper().readValue(data, Backup.class);
            Backup response = serverAdapter.createInstanceBackup(id, request);
            io.getWriter().write(resp, HttpServletResponse.SC_OK, response, outFormat);
        } catch (JsonProcessingException | CloudRuntimeException e) {
            io.badRequest(resp, e.getMessage(), outFormat);
        }
    }

    protected void handleGetBackupById(final String id, final HttpServletResponse resp,
              final Negotiation.OutFormat outFormat, final VeeamControlServlet io) throws IOException {
        try {
            Backup response = serverAdapter.getBackup(id);
            io.getWriter().write(resp, HttpServletResponse.SC_OK, response, outFormat);
        } catch (InvalidParameterValueException e) {
            io.notFound(resp, e.getMessage(), outFormat);
        }
    }

    protected void handleGetBackupDisksById(final String id, final HttpServletRequest req,
            final HttpServletResponse resp, final Negotiation.OutFormat outFormat, final VeeamControlServlet io)
            throws IOException {
        try {
            List<Disk> disks = serverAdapter.listDisksByBackupUuid(id);
            NamedList<Disk> response = NamedList.of("disk", disks);
            io.getWriter().write(resp, HttpServletResponse.SC_OK, response, outFormat);
        } catch (InvalidParameterValueException e) {
            io.notFound(resp, e.getMessage(), outFormat);
        }
    }

    protected void handleFinalizeBackupById(final String vmId, final String backupId, final HttpServletRequest req,
                final HttpServletResponse resp, final Negotiation.OutFormat outFormat, final VeeamControlServlet io)
            throws IOException {
        try {
            Backup backup = serverAdapter.finalizeBackup(vmId, backupId);
            io.getWriter().write(resp, HttpServletResponse.SC_OK, backup, outFormat);
        } catch (CloudRuntimeException e) {
            io.badRequest(resp, e.getMessage(), outFormat);
        }
    }

    protected void handleGetCheckpointsByVmId(final String id, final HttpServletResponse resp,
              final Negotiation.OutFormat outFormat, final VeeamControlServlet io) throws IOException {
        try {
            List<Checkpoint> checkpoints = serverAdapter.listCheckpointsByInstanceUuid(id);
            NamedList<Checkpoint> response = NamedList.of("checkpoints", checkpoints);
            io.getWriter().write(resp, HttpServletResponse.SC_OK, response, outFormat);
        } catch (InvalidParameterValueException e) {
            io.notFound(resp, e.getMessage(), outFormat);
        }
    }

    protected void handleDeleteCheckpoint(final String vmId, final String checkpointId,
                final HttpServletResponse resp, final Negotiation.OutFormat outFormat, final VeeamControlServlet io)
            throws IOException {
        try {
            serverAdapter.deleteCheckpoint(vmId, checkpointId);
            io.getWriter().write(resp, HttpServletResponse.SC_OK, null, outFormat);
        } catch (CloudRuntimeException e) {
            io.badRequest(resp, e.getMessage(), outFormat);
        }
    }
}

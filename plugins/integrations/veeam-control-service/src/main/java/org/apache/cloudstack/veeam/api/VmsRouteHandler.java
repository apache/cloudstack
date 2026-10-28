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
import org.apache.cloudstack.veeam.adapter.AsyncJobFailedException;
import org.apache.cloudstack.veeam.adapter.ServerAdapter;
import org.apache.cloudstack.veeam.api.converter.AsyncJobJoinVOToJobConverter;
import org.apache.cloudstack.veeam.api.dto.Backup;
import org.apache.cloudstack.veeam.api.dto.Checkpoint;
import org.apache.cloudstack.veeam.api.dto.Disk;
import org.apache.cloudstack.veeam.api.dto.DiskAttachment;
import org.apache.cloudstack.veeam.api.dto.Fault;
import org.apache.cloudstack.veeam.api.dto.NamedList;
import org.apache.cloudstack.veeam.api.dto.Nic;
import org.apache.cloudstack.veeam.api.dto.ResourceAction;
import org.apache.cloudstack.veeam.api.dto.Snapshot;
import org.apache.cloudstack.veeam.api.dto.Tag;
import org.apache.cloudstack.veeam.api.dto.Vm;
import org.apache.cloudstack.veeam.api.dto.VmAction;
import org.apache.cloudstack.veeam.api.request.ListQuery;
import org.apache.cloudstack.veeam.utils.Negotiation;
import org.apache.cloudstack.veeam.utils.PathUtil;
import org.apache.commons.collections.CollectionUtils;

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
                } else if ("tags".equals(subPath)) {
                    if ("POST".equalsIgnoreCase(method)) {
                        handlePostTagForVmId(id, req, resp, outFormat, io);
                    } else {
                        io.methodNotAllowed(resp, "POST", outFormat);
                    }
                    return;
                }
            } else if (idAndSubPath.size() == 3) {
                String subPath = idAndSubPath.get(1);
                String subId = idAndSubPath.get(2);
                if ("diskattachments".equals(subPath)) {
                    if (!"DELETE".equalsIgnoreCase(method)) {
                        io.methodNotAllowed(resp, "DELETE", outFormat);
                    } else  {
                        handleDeleteDiskAttachmentForVmId(id, subId, req, resp, outFormat, io);
                    }
                    return;
                } else if ("nics".equals(subPath)) {
                    if ("GET".equalsIgnoreCase(method)) {
                        handleGetNicById(id, subId, resp, outFormat, io);
                    } else if ("DELETE".equalsIgnoreCase(method)) {
                        handleDeleteNicById(id, subId, req, resp, outFormat, io);
                    } else {
                        io.methodNotAllowed(resp, "GET, DELETE", outFormat);
                    }
                    return;
                } else if ("snapshots".equals(subPath)) {
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
                        handleDeleteCheckpoint(id, subId, req, resp, outFormat, io);
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

    protected void handleGet(final HttpServletRequest req, final HttpServletResponse resp,
          Negotiation.OutFormat outFormat, VeeamControlServlet io) throws IOException {
        try {
            ListQuery query = ListQuery.fromRequest(req);
            final List<Vm> result = serverAdapter.listAllInstances(query.followContains("tags"),
                    query.followContains("disk_attachments.disk"),
                    query.followContains("nics.reporteddevices"),
                    query.isAllContent(),
                    query.getOffset(),
                    query.getLimit());
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
        try {
            ListQuery query = ListQuery.fromRequest(req);
            Vm response = serverAdapter.getInstance(id,
                    query.followContains("tags"),
                    query.followContains("disk_attachments.disk"),
                    query.followContains("nics.reporteddevices"),
                    query.isAllContent());
            io.getWriter().write(resp, HttpServletResponse.SC_OK, response, outFormat);
        } catch (InvalidParameterValueException e) {
            io.notFound(resp, e.getMessage(), outFormat);
        } catch (CloudRuntimeException e) {
            io.badRequest(resp, e.getMessage(), outFormat);
        }
    }

    protected void handleUpdateById(final String id, final HttpServletRequest req, final HttpServletResponse resp, final Negotiation.OutFormat outFormat,
                                    final VeeamControlServlet io) throws IOException {
        String data = RouteHandler.getRequestData(req, logger);
        try {
            Vm request = io.getMapper().jsonMapper().readValue(data, Vm.class);
            Vm response = serverAdapter.updateInstance(id, request);
            io.getWriter().write(resp, HttpServletResponse.SC_OK, response, outFormat);
        } catch (CloudRuntimeException e) {
            io.badRequest(resp, e.getMessage(), outFormat);
        }
    }

    protected void handleDeleteById(final String id, final HttpServletRequest req, final HttpServletResponse resp,
                final Negotiation.OutFormat outFormat, final VeeamControlServlet io) throws IOException {
        boolean async = RouteHandler.isRequestAsync(req);
        boolean deleteVolumes = !isRequestDetachOnly(req, false);
        try {
            VmAction vm = serverAdapter.deleteInstance(id, deleteVolumes, async);
            RouteHandler.writeDeleteResponse(resp, async, vm, outFormat, io);
        } catch (CloudRuntimeException e) {
            handleDeleteError(resp, e, outFormat, io);
        }
    }

    protected void handleStartVmById(final String id, final HttpServletRequest req, final HttpServletResponse resp,
             final Negotiation.OutFormat outFormat, final VeeamControlServlet io) throws IOException {
        boolean async = RouteHandler.isRequestAsync(req);
        try {
            VmAction vm = serverAdapter.startInstance(id, async);
            io.getWriter().write(resp, HttpServletResponse.SC_ACCEPTED, vm, outFormat);
        } catch (CloudRuntimeException e) {
            handleActionError(resp, e, outFormat, io);
        }
    }

    protected void handleStopVmById(final String id, final HttpServletRequest req, final HttpServletResponse resp,
                final Negotiation.OutFormat outFormat, final VeeamControlServlet io) throws IOException {
        boolean async = RouteHandler.isRequestAsync(req);
        try {
            VmAction vm = serverAdapter.stopInstance(id, async);
            io.getWriter().write(resp, HttpServletResponse.SC_ACCEPTED, vm, outFormat);
        } catch (CloudRuntimeException e) {
            handleActionError(resp, e, outFormat, io);
        }
    }

    protected void handleShutdownVmById(final String id, final HttpServletRequest req, final HttpServletResponse resp,
                final Negotiation.OutFormat outFormat, final VeeamControlServlet io) throws IOException {
        boolean async = RouteHandler.isRequestAsync(req);
        try {
            VmAction vm = serverAdapter.shutdownInstance(id, async);
            io.getWriter().write(resp, HttpServletResponse.SC_ACCEPTED, vm, outFormat);
        } catch (CloudRuntimeException e) {
            handleActionError(resp, e, outFormat, io);
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
        } catch (CloudRuntimeException e) {
            io.badRequest(resp, e.getMessage(), outFormat);
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
        } catch (CloudRuntimeException e) {
            io.badRequest(resp, e.getMessage(), outFormat);
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

    protected void handleGetNicById(final String vmId, final String nicId, final HttpServletResponse resp,
                final Negotiation.OutFormat outFormat, final VeeamControlServlet io) throws IOException {
        try {
            Nic response = serverAdapter.getInstanceNic(vmId, nicId);
            io.getWriter().write(resp, HttpServletResponse.SC_OK, response, outFormat);
        } catch (InvalidParameterValueException e) {
            io.notFound(resp, e.getMessage(), outFormat);
        } catch (CloudRuntimeException e) {
            io.badRequest(resp, e.getMessage(), outFormat);
        }
    }

    protected void handleDeleteNicById(final String vmId, final String nicId, final HttpServletRequest req,
                final HttpServletResponse resp, final Negotiation.OutFormat outFormat, final VeeamControlServlet io)
            throws IOException {
        boolean async = RouteHandler.isRequestAsync(req);
        try {
            serverAdapter.detachInstanceNic(vmId, nicId);
            RouteHandler.writeDeleteResponse(resp, async, null, outFormat, io);
        } catch (InvalidParameterValueException e) {
            io.notFound(resp, e.getMessage(), outFormat);
        } catch (CloudRuntimeException e) {
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
        } catch (CloudRuntimeException e) {
            io.badRequest(resp, e.getMessage(), outFormat);
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

    protected void handleDeleteDiskAttachmentForVmId(final String vmId, final String diskId,
             final HttpServletRequest req, final HttpServletResponse resp,
             final Negotiation.OutFormat outFormat, final VeeamControlServlet io) throws IOException {
        boolean async = RouteHandler.isRequestAsync(req);
        boolean delete = !isRequestDetachOnly(req, true);
        try {
            serverAdapter.detachInstanceDisk(vmId, diskId);
            ResourceAction action = null;
            if (delete) {
                action = serverAdapter.deleteDisk(diskId, async);
            }
            RouteHandler.writeDeleteResponse(resp, async, action, outFormat, io);
        } catch (InvalidParameterValueException e) {
            io.notFound(resp, e.getMessage(), outFormat);
        } catch (CloudRuntimeException e) {
            handleDeleteError(resp, e, outFormat, io);
        }
    }

    protected void handleGetSnapshotById(final String id, final HttpServletResponse resp,
                 final Negotiation.OutFormat outFormat, final VeeamControlServlet io) throws IOException {
        try {
            Snapshot response = serverAdapter.getSnapshot(id);
            io.getWriter().write(resp, HttpServletResponse.SC_OK, response, outFormat);
        } catch (InvalidParameterValueException e) {
            io.notFound(resp, e.getMessage(), outFormat);
        } catch (CloudRuntimeException e) {
            io.badRequest(resp, e.getMessage(), outFormat);
        }
    }

    protected void handleDeleteSnapshotById(final String id, final HttpServletRequest req,
                final HttpServletResponse resp, final Negotiation.OutFormat outFormat, final VeeamControlServlet io)
            throws IOException {
        boolean async = RouteHandler.isRequestAsync(req);
        try {
            ResourceAction action = serverAdapter.deleteSnapshot(id, async);
            RouteHandler.writeDeleteResponse(resp, async, action, outFormat, io);
        } catch (CloudRuntimeException e) {
            handleDeleteError(resp, e, outFormat, io);
        }
    }

    protected void handleRestoreSnapshotById(final String id, final HttpServletRequest req,
                final HttpServletResponse resp, final Negotiation.OutFormat outFormat, final VeeamControlServlet io)
            throws IOException {
        boolean async = RouteHandler.isRequestAsync(req);
        String data = RouteHandler.getRequestData(req, logger);
        try {
            ResourceAction response = serverAdapter.revertInstanceToSnapshot(id, async);
            io.getWriter().write(resp, HttpServletResponse.SC_ACCEPTED, response, outFormat);
        } catch (CloudRuntimeException e) {
            handleActionError(resp, e, outFormat, io);
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
        } catch (CloudRuntimeException e) {
            io.badRequest(resp, e.getMessage(), outFormat);
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
        } catch (CloudRuntimeException e) {
            io.badRequest(resp, e.getMessage(), outFormat);
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
        } catch (CloudRuntimeException e) {
            io.badRequest(resp, e.getMessage(), outFormat);
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
        } catch (CloudRuntimeException e) {
            io.badRequest(resp, e.getMessage(), outFormat);
        }
    }

    protected void handleDeleteCheckpoint(final String vmId, final String checkpointId, final HttpServletRequest req,
                final HttpServletResponse resp, final Negotiation.OutFormat outFormat, final VeeamControlServlet io)
            throws IOException {
        boolean async = RouteHandler.isRequestAsync(req);
        try {
            serverAdapter.deleteCheckpoint(vmId, checkpointId);
            RouteHandler.writeDeleteResponse(resp, async, null, outFormat, io);
        } catch (CloudRuntimeException e) {
            io.badRequest(resp, e.getMessage(), outFormat);
        }
    }

    protected void handlePostTagForVmId(final String id, final HttpServletRequest req,
               final HttpServletResponse resp, final Negotiation.OutFormat outFormat, final VeeamControlServlet io)
            throws IOException {
        String data = RouteHandler.getRequestData(req, logger);
        try {
            Tag request = io.getMapper().jsonMapper().readValue(data, Tag.class);
            Tag response = serverAdapter.createInstanceTag(id, request);
            io.getWriter().write(resp, HttpServletResponse.SC_OK, response, outFormat);
        } catch (JsonProcessingException | CloudRuntimeException e) {
            io.badRequest(resp, e.getMessage(), outFormat);
        }
    }

    /**
     * Writes the error for a VM or snapshot action. When the action's async job failed, or waiting for it
     * timed out, responds like oVirt does for a failed blocking action: the action with a "failed" status,
     * a fault and the job link. Other errors are written as a plain bad request.
     */
    protected static void handleActionError(final HttpServletResponse resp, final CloudRuntimeException e,
                final Negotiation.OutFormat outFormat, final VeeamControlServlet io) throws IOException {
        final AsyncJobFailedException jobFailure = findAsyncJobFailure(e);
        if (jobFailure != null) {
            io.getWriter().write(resp, HttpServletResponse.SC_BAD_REQUEST,
                    AsyncJobJoinVOToJobConverter.toFailedAction(jobFailure.getJobUuid(), e.getMessage()), outFormat);
            return;
        }
        io.badRequest(resp, e.getMessage(), outFormat);
    }

    /**
     * Writes the error for a DELETE. When its async job failed, or waiting for it timed out, responds like
     * oVirt does for a failed DELETE: a plain "Operation Failed" fault, without an action. Other errors are
     * written as a plain bad request.
     */
    protected static void handleDeleteError(final HttpServletResponse resp, final CloudRuntimeException e,
                final Negotiation.OutFormat outFormat, final VeeamControlServlet io) throws IOException {
        if (findAsyncJobFailure(e) != null) {
            io.getWriter().writeFault(resp, HttpServletResponse.SC_BAD_REQUEST, Fault.OPERATION_FAILED, e.getMessage(), outFormat);
            return;
        }
        io.badRequest(resp, e.getMessage(), outFormat);
    }

    protected static AsyncJobFailedException findAsyncJobFailure(final Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof AsyncJobFailedException) {
                return (AsyncJobFailedException) t;
            }
        }
        return null;
    }

    /**
     * Returns whether disks should only be detached and preserved, as per the detach_only request parameter.
     * Mimics oVirt: a parameter present without a value (e.g. ?detach_only) means true, while a missing or
     * unrecognized value falls back to valueIfMissing, which differs per endpoint (false for VM removal,
     * true for disk attachment removal).
     */
    protected static boolean isRequestDetachOnly(final HttpServletRequest req, final boolean valueIfMissing) {
        String detachOnly = req.getParameter("detach_only");
        if (detachOnly != null && detachOnly.isEmpty()) {
            return true;
        }
        return RouteHandler.parseBoolean(detachOnly, valueIfMissing);
    }
}

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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.apache.cloudstack.veeam.adapter.AsyncJobFailedException;
import org.apache.cloudstack.veeam.api.dto.Backup;
import org.apache.cloudstack.veeam.api.dto.Checkpoint;
import org.apache.cloudstack.veeam.api.dto.Disk;
import org.apache.cloudstack.veeam.api.dto.DiskAttachment;
import org.apache.cloudstack.veeam.api.dto.Nic;
import org.apache.cloudstack.veeam.api.dto.Ref;
import org.apache.cloudstack.veeam.api.dto.ResourceAction;
import org.apache.cloudstack.veeam.api.dto.Snapshot;
import org.apache.cloudstack.veeam.api.dto.Vm;
import org.apache.cloudstack.veeam.api.dto.VmAction;
import org.apache.cloudstack.veeam.utils.Negotiation;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.utils.exception.CloudRuntimeException;

public class VmsRouteHandlerTest extends RouteHandlerTestSupport {

    @Test
    public void testHandleGetListUsesFollowFlags() throws Exception {
        final VmsRouteHandler handler = new VmsRouteHandler();
        handler.serverAdapter = mock(org.apache.cloudstack.veeam.adapter.ServerAdapter.class);
        when(handler.serverAdapter.listAllInstances(true, true, true, true, null, 10L))
                .thenReturn(List.of(withId(new Vm(), "vm-1")));

        final ResponseCapture response = newResponse();
        handler.handle(newRequest("GET", Map.of(
                        "max", "10",
                        "all_content", "true",
                        "follow", "tags,disk_attachments.disk,nics.reporteddevices"), null, null),
                response.response, "/api/vms", Negotiation.OutFormat.JSON, newServlet());

        verify(handler.serverAdapter).listAllInstances(true, true, true, true, null, 10L);
        verify(response.response).setStatus(200);
        assertContains(response.body(), "\"vm\":[");
        assertContains(response.body(), "vm-1");
    }

    @Test
    public void testHandlePostAndUpdateParseVmJson() throws Exception {
        final VmsRouteHandler handler = new VmsRouteHandler();
        handler.serverAdapter = mock(org.apache.cloudstack.veeam.adapter.ServerAdapter.class);

        final ArgumentCaptor<Vm> createCaptor = ArgumentCaptor.forClass(Vm.class);
        final Vm created = withId(new Vm(), "vm-created");
        created.setName("vm-created");
        when(handler.serverAdapter.createInstance(createCaptor.capture())).thenReturn(created);

        final ResponseCapture post = newResponse();
        handler.handle(newRequest("POST", Map.of(), "application/json", "{\"name\":\"vm-created\"}"), post.response,
                "/api/vms", Negotiation.OutFormat.JSON, newServlet());
        verify(post.response).setStatus(201);
        assertEquals("vm-created", createCaptor.getValue().getName());
        assertContains(post.body(), "vm-created");

        final ArgumentCaptor<Vm> updateCaptor = ArgumentCaptor.forClass(Vm.class);
        final Vm updated = withId(new Vm(), "vm-1");
        updated.setName("vm-updated");
        when(handler.serverAdapter.updateInstance(eq("vm-1"), updateCaptor.capture())).thenReturn(updated);

        when(handler.serverAdapter.instanceExists("vm-1")).thenReturn(true);
        final ResponseCapture put = newResponse();
        handler.handle(newRequest("PUT", Map.of(), "application/json", "{\"name\":\"vm-updated\"}"), put.response,
                "/api/vms/vm-1", Negotiation.OutFormat.JSON, newServlet());
        verify(put.response).setStatus(200);
        assertEquals("vm-updated", updateCaptor.getValue().getName());
        assertContains(put.body(), "vm-updated");
    }

    @Test
    public void testHandleGetByIdDeleteAndPowerActions() throws Exception {
        final VmsRouteHandler handler = new VmsRouteHandler();
        handler.serverAdapter = mock(org.apache.cloudstack.veeam.adapter.ServerAdapter.class);
        when(handler.serverAdapter.instanceExists("vm-1")).thenReturn(true);
        when(handler.serverAdapter.getInstance("vm-1", true, true, true, true)).thenReturn(withId(new Vm(), "vm-1"));

        final VmAction deleteAction = new VmAction();
        deleteAction.setStatus("deleted");
        final VmAction startAction = new VmAction();
        startAction.setStatus("starting");
        final VmAction stopAction = new VmAction();
        stopAction.setStatus("stopping");
        final VmAction shutdownAction = new VmAction();
        shutdownAction.setStatus("shutting_down");
        when(handler.serverAdapter.deleteInstance("vm-1", true, true)).thenReturn(deleteAction);
        when(handler.serverAdapter.startInstance("vm-1", false)).thenReturn(startAction);
        when(handler.serverAdapter.stopInstance("vm-1", false)).thenReturn(stopAction);
        when(handler.serverAdapter.shutdownInstance("vm-1", false)).thenReturn(shutdownAction);

        final ResponseCapture get = newResponse();
        handler.handle(newRequest("GET", Map.of(
                        "all_content", "true",
                        "follow", "tags,disk_attachments.disk,nics.reporteddevices"), null, null),
                get.response, "/api/vms/vm-1", Negotiation.OutFormat.JSON, newServlet());
        verify(get.response).setStatus(200);
        assertContains(get.body(), "\"id\":\"vm-1\"");

        final ResponseCapture delete = newResponse();
        handler.handle(newRequest("DELETE", Map.of("async", "true", "detach_only", "false"), null, null), delete.response,
                "/api/vms/vm-1", Negotiation.OutFormat.JSON, newServlet());
        verify(handler.serverAdapter).deleteInstance("vm-1", true, true);
        verify(delete.response).setStatus(202);
        assertEquals("", delete.body());

        final ResponseCapture start = newResponse();
        handler.handle(newRequest("POST"), start.response, "/api/vms/vm-1/start", Negotiation.OutFormat.JSON, newServlet());
        verify(start.response).setStatus(202);
        assertContains(start.body(), "starting");

        final ResponseCapture stop = newResponse();
        handler.handle(newRequest("POST"), stop.response, "/api/vms/vm-1/stop", Negotiation.OutFormat.JSON, newServlet());
        verify(stop.response).setStatus(202);
        assertContains(stop.body(), "stopping");

        final ResponseCapture shutdown = newResponse();
        handler.handle(newRequest("POST"), shutdown.response, "/api/vms/vm-1/shutdown", Negotiation.OutFormat.JSON, newServlet());
        verify(shutdown.response).setStatus(202);
        assertContains(shutdown.body(), "shutting_down");
    }

    @Test
    public void testHandleVmRoutesReturnNotFoundForMissingVm() throws Exception {
        final VmsRouteHandler handler = new VmsRouteHandler();
        handler.serverAdapter = mock(org.apache.cloudstack.veeam.adapter.ServerAdapter.class);
        when(handler.serverAdapter.instanceExists("vm-missing")).thenReturn(false);

        final String[][] routes = {
                {"GET", "/api/vms/vm-missing"},
                {"PUT", "/api/vms/vm-missing"},
                {"DELETE", "/api/vms/vm-missing"},
                {"POST", "/api/vms/vm-missing/start"},
                {"POST", "/api/vms/vm-missing/stop"},
                {"POST", "/api/vms/vm-missing/shutdown"},
                {"GET", "/api/vms/vm-missing/diskattachments"},
                {"POST", "/api/vms/vm-missing/diskattachments"},
                {"DELETE", "/api/vms/vm-missing/diskattachments/disk-1"},
                {"GET", "/api/vms/vm-missing/nics"},
                {"POST", "/api/vms/vm-missing/nics"},
                {"GET", "/api/vms/vm-missing/snapshots"},
                {"POST", "/api/vms/vm-missing/snapshots"},
                {"GET", "/api/vms/vm-missing/snapshots/snap-1"},
                {"DELETE", "/api/vms/vm-missing/snapshots/snap-1"},
                {"POST", "/api/vms/vm-missing/snapshots/snap-1/restore"},
                {"GET", "/api/vms/vm-missing/backups"},
                {"POST", "/api/vms/vm-missing/backups"},
                {"GET", "/api/vms/vm-missing/backups/backup-1"},
                {"GET", "/api/vms/vm-missing/backups/backup-1/disks"},
                {"POST", "/api/vms/vm-missing/backups/backup-1/finalize"},
                {"GET", "/api/vms/vm-missing/checkpoints"},
                {"DELETE", "/api/vms/vm-missing/checkpoints/chk-1"},
                {"POST", "/api/vms/vm-missing/tags"}
        };
        for (String[] route : routes) {
            final ResponseCapture response = newResponse();
            handler.handle(newRequest(route[0], Map.of(), "application/json", "{}"), response.response, route[1],
                    Negotiation.OutFormat.JSON, newServlet());
            verify(response.response).setStatus(404);
            assertContains(response.body(), "VM with ID vm-missing not found");
        }

        verify(handler.serverAdapter, times(routes.length)).instanceExists("vm-missing");
        verifyNoMoreInteractions(handler.serverAdapter);
    }

    @Test
    public void testHandleVmRouteWrongMethodSkipsExistenceCheck() throws Exception {
        final VmsRouteHandler handler = new VmsRouteHandler();
        handler.serverAdapter = mock(org.apache.cloudstack.veeam.adapter.ServerAdapter.class);

        final ResponseCapture response = newResponse();
        handler.handle(newRequest("PATCH"), response.response, "/api/vms/vm-missing", Negotiation.OutFormat.JSON, newServlet());
        verify(response.response).setStatus(405);

        final ResponseCapture start = newResponse();
        handler.handle(newRequest("GET"), start.response, "/api/vms/vm-missing/start", Negotiation.OutFormat.JSON, newServlet());
        verify(start.response).setStatus(405);

        verify(handler.serverAdapter, never()).instanceExists(anyString());
    }

    @Test
    public void testHandleDiskAttachmentAndNicRoutes() throws Exception {
        final VmsRouteHandler handler = new VmsRouteHandler();
        handler.serverAdapter = mock(org.apache.cloudstack.veeam.adapter.ServerAdapter.class);
        when(handler.serverAdapter.instanceExists("vm-1")).thenReturn(true);
        when(handler.serverAdapter.listDiskAttachmentsByInstanceUuid("vm-1"))
                .thenReturn(List.of(withId(new DiskAttachment(), "attach-1")));
        when(handler.serverAdapter.listNicsByInstanceUuid("vm-1"))
                .thenReturn(List.of(withId(new Nic(), "nic-1")));

        final ArgumentCaptor<DiskAttachment> diskAttachmentCaptor = ArgumentCaptor.forClass(DiskAttachment.class);
        final DiskAttachment createdAttachment = withId(new DiskAttachment(), "attach-created");
        createdAttachment.setActive("true");
        when(handler.serverAdapter.attachInstanceDisk(eq("vm-1"), diskAttachmentCaptor.capture())).thenReturn(createdAttachment);

        final ArgumentCaptor<Nic> nicCaptor = ArgumentCaptor.forClass(Nic.class);
        final Nic createdNic = withId(new Nic(), "nic-created");
        createdNic.setName("nic-created");
        when(handler.serverAdapter.attachInstanceNic(eq("vm-1"), nicCaptor.capture())).thenReturn(createdNic);

        final ResponseCapture attachments = newResponse();
        handler.handle(newRequest("GET"), attachments.response, "/api/vms/vm-1/diskattachments", Negotiation.OutFormat.JSON, newServlet());
        verify(attachments.response).setStatus(200);
        assertContains(attachments.body(), "\"disk_attachment\":[");

        final ResponseCapture postAttachment = newResponse();
        handler.handle(newRequest("POST", Map.of(), "application/json", "{\"active\":\"true\"}"), postAttachment.response,
                "/api/vms/vm-1/diskattachments", Negotiation.OutFormat.JSON, newServlet());
        verify(postAttachment.response).setStatus(201);
        assertEquals("true", diskAttachmentCaptor.getValue().getActive());
        assertContains(postAttachment.body(), "attach-created");

        final ResponseCapture nics = newResponse();
        handler.handle(newRequest("GET"), nics.response, "/api/vms/vm-1/nics", Negotiation.OutFormat.JSON, newServlet());
        verify(nics.response).setStatus(200);
        assertContains(nics.body(), "\"nic\":[");

        final ResponseCapture postNic = newResponse();
        handler.handle(newRequest("POST", Map.of(), "application/json", "{\"name\":\"nic-created\"}"), postNic.response,
                "/api/vms/vm-1/nics", Negotiation.OutFormat.JSON, newServlet());
        verify(postNic.response).setStatus(201);
        assertEquals("nic-created", nicCaptor.getValue().getName());
        assertContains(postNic.body(), "nic-created");
    }

    @Test
    public void testHandleSnapshotRoutes() throws Exception {
        final VmsRouteHandler handler = new VmsRouteHandler();
        handler.serverAdapter = mock(org.apache.cloudstack.veeam.adapter.ServerAdapter.class);
        when(handler.serverAdapter.instanceExists("vm-1")).thenReturn(true);
        when(handler.serverAdapter.listSnapshotsByInstanceUuid("vm-1")).thenReturn(List.of(withId(new Snapshot(), "snap-1")));
        when(handler.serverAdapter.getSnapshot("snap-1")).thenReturn(withId(new Snapshot(), "snap-1"));

        final ArgumentCaptor<Snapshot> createCaptor = ArgumentCaptor.forClass(Snapshot.class);
        final Snapshot createdSnapshot = withId(new Snapshot(), "snap-created");
        createdSnapshot.setDescription("created snapshot");
        when(handler.serverAdapter.createInstanceSnapshot(eq("vm-1"), createCaptor.capture())).thenReturn(createdSnapshot);

        final ResourceAction deleteAction = new ResourceAction();
        deleteAction.setStatus("deleted");
        final ResourceAction restoreAction = new ResourceAction();
        restoreAction.setStatus("restored");
        when(handler.serverAdapter.deleteSnapshot("snap-1", true)).thenReturn(deleteAction);
        when(handler.serverAdapter.revertInstanceToSnapshot("snap-1", false)).thenReturn(restoreAction);

        final ResponseCapture list = newResponse();
        handler.handle(newRequest("GET"), list.response, "/api/vms/vm-1/snapshots", Negotiation.OutFormat.JSON, newServlet());
        verify(list.response).setStatus(200);
        assertContains(list.body(), "\"snapshot\":[");

        final ResponseCapture post = newResponse();
        handler.handle(newRequest("POST", Map.of(), "application/json", "{\"description\":\"created snapshot\"}"), post.response,
                "/api/vms/vm-1/snapshots", Negotiation.OutFormat.JSON, newServlet());
        verify(post.response).setStatus(202);
        assertEquals("created snapshot", createCaptor.getValue().getDescription());
        assertContains(post.body(), "snap-created");

        final ResponseCapture get = newResponse();
        handler.handle(newRequest("GET"), get.response, "/api/vms/vm-1/snapshots/snap-1", Negotiation.OutFormat.JSON, newServlet());
        verify(get.response).setStatus(200);
        assertContains(get.body(), "snap-1");

        final ResponseCapture delete = newResponse();
        handler.handle(newRequest("DELETE", Map.of("async", "true"), null, null), delete.response,
                "/api/vms/vm-1/snapshots/snap-1", Negotiation.OutFormat.JSON, newServlet());
        verify(delete.response).setStatus(202);
        assertEquals("", delete.body());

        final ResponseCapture restore = newResponse();
        handler.handle(newRequest("POST"), restore.response, "/api/vms/vm-1/snapshots/snap-1/restore", Negotiation.OutFormat.JSON, newServlet());
        verify(restore.response).setStatus(202);
        assertContains(restore.body(), "restored");
    }

    @Test
    public void testHandleBackupRoutes() throws Exception {
        final VmsRouteHandler handler = new VmsRouteHandler();
        handler.serverAdapter = mock(org.apache.cloudstack.veeam.adapter.ServerAdapter.class);
        when(handler.serverAdapter.instanceExists("vm-1")).thenReturn(true);
        when(handler.serverAdapter.listBackupsByInstanceUuid("vm-1")).thenReturn(List.of(withId(new Backup(), "backup-1")));
        when(handler.serverAdapter.getBackup("backup-1")).thenReturn(withId(new Backup(), "backup-1"));
        when(handler.serverAdapter.listDisksByBackupUuid("backup-1")).thenReturn(List.of(withId(new Disk(), "disk-1")));

        final ArgumentCaptor<Backup> createCaptor = ArgumentCaptor.forClass(Backup.class);
        final Backup createdBackup = withId(new Backup(), "backup-created");
        createdBackup.setName("backup-created");
        when(handler.serverAdapter.createInstanceBackup(eq("vm-1"), createCaptor.capture())).thenReturn(createdBackup);

        final Backup finalizedBackup = withId(new Backup(), "backup-1");
        finalizedBackup.setPhase("finalized");
        when(handler.serverAdapter.finalizeBackup("vm-1", "backup-1")).thenReturn(finalizedBackup);

        final ResponseCapture list = newResponse();
        handler.handle(newRequest("GET"), list.response, "/api/vms/vm-1/backups", Negotiation.OutFormat.JSON, newServlet());
        verify(list.response).setStatus(200);
        assertContains(list.body(), "\"backup\":[");

        final ResponseCapture post = newResponse();
        handler.handle(newRequest("POST", Map.of(), "application/json", "{\"name\":\"backup-created\"}"), post.response,
                "/api/vms/vm-1/backups", Negotiation.OutFormat.JSON, newServlet());
        verify(post.response).setStatus(200);
        assertEquals("backup-created", createCaptor.getValue().getName());
        assertContains(post.body(), "backup-created");

        final ResponseCapture get = newResponse();
        handler.handle(newRequest("GET"), get.response, "/api/vms/vm-1/backups/backup-1", Negotiation.OutFormat.JSON, newServlet());
        verify(get.response).setStatus(200);
        assertContains(get.body(), "backup-1");

        final ResponseCapture disks = newResponse();
        handler.handle(newRequest("GET"), disks.response, "/api/vms/vm-1/backups/backup-1/disks", Negotiation.OutFormat.JSON, newServlet());
        verify(disks.response).setStatus(200);
        assertContains(disks.body(), "\"disk\":[");

        final ResponseCapture finalize = newResponse();
        handler.handle(newRequest("POST"), finalize.response, "/api/vms/vm-1/backups/backup-1/finalize", Negotiation.OutFormat.JSON, newServlet());
        verify(finalize.response).setStatus(200);
        assertContains(finalize.body(), "finalized");
    }

    @Test
    public void testHandleCheckpointRoutes() throws Exception {
        final VmsRouteHandler handler = new VmsRouteHandler();
        handler.serverAdapter = mock(org.apache.cloudstack.veeam.adapter.ServerAdapter.class);
        when(handler.serverAdapter.instanceExists("vm-1")).thenReturn(true);
        when(handler.serverAdapter.listCheckpointsByInstanceUuid("vm-1")).thenReturn(List.of(withId(new Checkpoint(), "chk-1")));

        final ResponseCapture list = newResponse();
        handler.handle(newRequest("GET"), list.response, "/api/vms/vm-1/checkpoints", Negotiation.OutFormat.JSON, newServlet());
        verify(list.response).setStatus(200);
        assertContains(list.body(), "\"checkpoints\":[");

        final ResponseCapture delete = newResponse();
        handler.handle(newRequest("DELETE"), delete.response, "/api/vms/vm-1/checkpoints/chk-1", Negotiation.OutFormat.JSON, newServlet());
        verify(handler.serverAdapter).deleteCheckpoint("vm-1", "chk-1");
        verify(delete.response).setStatus(200);
    }

    @Test
    public void testIsRequestDetachOnly_EmptyValueIsTrueRegardlessOfDefault() throws Exception {
        assertTrue(VmsRouteHandler.isRequestDetachOnly(newRequest("DELETE", Map.of("detach_only", ""), null, null), false));
        assertTrue(VmsRouteHandler.isRequestDetachOnly(newRequest("DELETE", Map.of("detach_only", ""), null, null), true));
    }

    @Test
    public void testIsRequestDetachOnly_MissingOrInvalidValueReturnsValueIfMissing() throws Exception {
        assertFalse(VmsRouteHandler.isRequestDetachOnly(newRequest("DELETE"), false));
        assertTrue(VmsRouteHandler.isRequestDetachOnly(newRequest("DELETE"), true));
        assertFalse(VmsRouteHandler.isRequestDetachOnly(newRequest("DELETE", Map.of("detach_only", "invalid"), null, null), false));
        assertTrue(VmsRouteHandler.isRequestDetachOnly(newRequest("DELETE", Map.of("detach_only", "invalid"), null, null), true));
    }

    @Test
    public void testIsRequestDetachOnly_ExplicitValueOverridesValueIfMissing() throws Exception {
        for (String value : new String[] {"true", "TRUE", "1"}) {
            assertTrue(VmsRouteHandler.isRequestDetachOnly(newRequest("DELETE", Map.of("detach_only", value), null, null), false));
        }
        for (String value : new String[] {"false", "False", "0"}) {
            assertFalse(VmsRouteHandler.isRequestDetachOnly(newRequest("DELETE", Map.of("detach_only", value), null, null), true));
        }
    }

    @Test
    public void testHandleDeleteById_DeletesVolumesWhenDetachOnlyMissing() throws Exception {
        final VmsRouteHandler handler = new VmsRouteHandler();
        handler.serverAdapter = mock(org.apache.cloudstack.veeam.adapter.ServerAdapter.class);
        when(handler.serverAdapter.deleteInstance("vm-1", true, false)).thenReturn(new VmAction());

        final ResponseCapture delete = newResponse();
        handler.handle(newRequest("DELETE"), delete.response, "/api/vms/vm-1", Negotiation.OutFormat.JSON, newServlet());
        verify(handler.serverAdapter).deleteInstance("vm-1", true, false);
        verify(delete.response).setStatus(200);
    }

    @Test
    public void testHandleDeleteById_KeepsVolumesWhenDetachOnlyEmptyOrTrue() throws Exception {
        for (String value : new String[] {"", "true", "1"}) {
            final VmsRouteHandler handler = new VmsRouteHandler();
            handler.serverAdapter = mock(org.apache.cloudstack.veeam.adapter.ServerAdapter.class);
            when(handler.serverAdapter.deleteInstance("vm-1", false, false)).thenReturn(new VmAction());

            final ResponseCapture delete = newResponse();
            handler.handle(newRequest("DELETE", Map.of("detach_only", value), null, null), delete.response,
                    "/api/vms/vm-1", Negotiation.OutFormat.JSON, newServlet());
            verify(handler.serverAdapter).deleteInstance("vm-1", false, false);
            verify(delete.response).setStatus(200);
        }
    }

    @Test
    public void testHandleDeleteDiskAttachment_OnlyDetachesWhenDetachOnlyMissing() throws Exception {
        final VmsRouteHandler handler = new VmsRouteHandler();
        handler.serverAdapter = mock(org.apache.cloudstack.veeam.adapter.ServerAdapter.class);

        final ResponseCapture delete = newResponse();
        handler.handle(newRequest("DELETE"), delete.response, "/api/vms/vm-1/diskattachments/disk-1",
                Negotiation.OutFormat.JSON, newServlet());
        verify(handler.serverAdapter).detachInstanceDisk("vm-1", "disk-1");
        verify(handler.serverAdapter, never()).deleteDisk(anyString(), anyBoolean());
        verify(delete.response).setStatus(200);
    }

    @Test
    public void testHandleDeleteDiskAttachment_DeletesDiskWhenDetachOnlyFalse() throws Exception {
        for (String value : new String[] {"false", "0"}) {
            final VmsRouteHandler handler = new VmsRouteHandler();
            handler.serverAdapter = mock(org.apache.cloudstack.veeam.adapter.ServerAdapter.class);

            final ResponseCapture delete = newResponse();
            handler.handle(newRequest("DELETE", Map.of("detach_only", value), null, null), delete.response,
                    "/api/vms/vm-1/diskattachments/disk-1", Negotiation.OutFormat.JSON, newServlet());
            verify(handler.serverAdapter).detachInstanceDisk("vm-1", "disk-1");
            verify(handler.serverAdapter).deleteDisk("disk-1", false);
            verify(delete.response).setStatus(200);
        }
    }

    @Test
    public void testHandleDeleteDiskAttachment_DeleteReturnsDiskDeletionJob() throws Exception {
        final VmsRouteHandler handler = new VmsRouteHandler();
        handler.serverAdapter = mock(org.apache.cloudstack.veeam.adapter.ServerAdapter.class);
        final ResourceAction action = new ResourceAction();
        action.setJob(Ref.of("/ovirt-engine/api/jobs/job-1", "job-1"));
        when(handler.serverAdapter.deleteDisk("disk-1", false)).thenReturn(action);

        final ResponseCapture delete = newResponse();
        handler.handle(newRequest("DELETE", Map.of("detach_only", "false"), null, null), delete.response,
                "/api/vms/vm-1/diskattachments/disk-1", Negotiation.OutFormat.JSON, newServlet());

        verify(delete.response).setStatus(200);
        assertContains(delete.body(), "\"status\":\"complete\"");
        assertContains(delete.body(), "\"id\":\"job-1\"");
    }

    @Test
    public void testHandleActions_AsyncJobFailureReturnsFailedActionWithFaultAndJob() throws Exception {
        final VmsRouteHandler handler = new VmsRouteHandler();
        handler.serverAdapter = mock(org.apache.cloudstack.veeam.adapter.ServerAdapter.class);
        final CloudRuntimeException failure = new CloudRuntimeException("Failed to start VM: Timed out waiting for job job-1",
                new AsyncJobFailedException("job-1", "Timed out waiting for job job-1"));
        when(handler.serverAdapter.startInstance("vm-1", false)).thenThrow(failure);

        final ResponseCapture start = newResponse();
        handler.handle(newRequest("POST"), start.response, "/api/vms/vm-1/start", Negotiation.OutFormat.JSON, newServlet());

        verify(start.response).setStatus(400);
        assertContains(start.body(), "\"status\":\"failed\"");
        assertContains(start.body(), "\"reason\":\"Operation Failed\"");
        assertContains(start.body(), "Failed to start VM: Timed out waiting for job job-1");
        assertContains(start.body(), "/api/jobs/job-1");
    }

    @Test
    public void testHandleActions_AsyncJobFailureXmlContainsFaultAndJob() throws Exception {
        final VmsRouteHandler handler = new VmsRouteHandler();
        handler.serverAdapter = mock(org.apache.cloudstack.veeam.adapter.ServerAdapter.class);
        when(handler.serverAdapter.stopInstance("vm-1", false)).thenThrow(new CloudRuntimeException(
                "Failed to stop VM: Timed out waiting for job job-1",
                new AsyncJobFailedException("job-1", "Timed out waiting for job job-1")));

        final ResponseCapture stop = newResponse();
        handler.handle(newRequest("POST"), stop.response, "/api/vms/vm-1/stop", Negotiation.OutFormat.XML, newServlet());

        verify(stop.response).setStatus(400);
        assertContains(stop.body(), "<status>failed</status>");
        assertContains(stop.body(), "<reason>Operation Failed</reason>");
        assertContains(stop.body(), "<detail>Failed to stop VM: Timed out waiting for job job-1</detail>");
        assertContains(stop.body(), "job-1");
    }

    @Test
    public void testHandleDeleteById_AsyncJobFailureReturnsPlainFault() throws Exception {
        final VmsRouteHandler handler = new VmsRouteHandler();
        handler.serverAdapter = mock(org.apache.cloudstack.veeam.adapter.ServerAdapter.class);
        final CloudRuntimeException failure = new CloudRuntimeException("Failed to delete VM: Timed out waiting for job job-1",
                new AsyncJobFailedException("job-1", "Timed out waiting for job job-1"));
        when(handler.serverAdapter.deleteInstance("vm-1", true, false)).thenThrow(failure);

        final ResponseCapture delete = newResponse();
        handler.handle(newRequest("DELETE"), delete.response, "/api/vms/vm-1", Negotiation.OutFormat.JSON, newServlet());

        verify(delete.response).setStatus(400);
        assertContains(delete.body(), "\"reason\":\"Operation Failed\"");
        assertContains(delete.body(), "Failed to delete VM: Timed out waiting for job job-1");
        assertFalse(delete.body().contains("\"status\""));
        assertFalse(delete.body().contains("\"job\""));
    }

    @Test
    public void testHandleDeleteSnapshot_AsyncJobFailureReturnsOperationFailedFault() throws Exception {
        final VmsRouteHandler handler = new VmsRouteHandler();
        handler.serverAdapter = mock(org.apache.cloudstack.veeam.adapter.ServerAdapter.class);
        final CloudRuntimeException failure = new CloudRuntimeException("Failed to delete snapshot: Job job-2 failed after 3s: boom",
                new AsyncJobFailedException("job-2", "Job job-2 failed after 3s: boom"));
        when(handler.serverAdapter.deleteSnapshot("snap-1", false)).thenThrow(failure);

        final ResponseCapture delete = newResponse();
        handler.handle(newRequest("DELETE"), delete.response, "/api/vms/vm-1/snapshots/snap-1",
                Negotiation.OutFormat.JSON, newServlet());

        verify(delete.response).setStatus(400);
        assertContains(delete.body(), "\"reason\":\"Operation Failed\"");
        assertContains(delete.body(), "Failed to delete snapshot: Job job-2 failed after 3s: boom");
        assertFalse(delete.body().contains("\"status\""));
    }

    @Test
    public void testHandleDeleteById_OtherErrorReturnsBadRequestFault() throws Exception {
        final VmsRouteHandler handler = new VmsRouteHandler();
        handler.serverAdapter = mock(org.apache.cloudstack.veeam.adapter.ServerAdapter.class);
        when(handler.serverAdapter.deleteInstance("vm-1", true, false))
                .thenThrow(new CloudRuntimeException("Failed to delete VM: boom"));

        final ResponseCapture delete = newResponse();
        handler.handle(newRequest("DELETE"), delete.response, "/api/vms/vm-1", Negotiation.OutFormat.JSON, newServlet());

        verify(delete.response).setStatus(400);
        assertContains(delete.body(), "Failed to delete VM: boom");
        assertFalse(delete.body().contains("Operation Failed"));
    }

    @Test
    public void testHandleDeleteById_SyncReturnsCompletedActionWithJob() throws Exception {
        final VmsRouteHandler handler = new VmsRouteHandler();
        handler.serverAdapter = mock(org.apache.cloudstack.veeam.adapter.ServerAdapter.class);
        final VmAction action = new VmAction();
        action.setJob(Ref.of("/ovirt-engine/api/jobs/job-1", "job-1"));
        action.setVm(withId(new Vm(), "vm-1"));
        action.setStatus("in_progress");
        when(handler.serverAdapter.deleteInstance("vm-1", true, false)).thenReturn(action);

        final ResponseCapture delete = newResponse();
        handler.handle(newRequest("DELETE"), delete.response, "/api/vms/vm-1", Negotiation.OutFormat.JSON, newServlet());

        verify(delete.response).setStatus(200);
        assertContains(delete.body(), "\"status\":\"complete\"");
        assertContains(delete.body(), "/api/jobs/job-1");
        assertFalse(delete.body().contains("\"vm\""));
    }

    @Test
    public void testHandleDelete_AsyncReturnsAcceptedWithoutBody() throws Exception {
        for (String async : new String[] {"true", "1"}) {
            for (String path : new String[] {"/api/vms/vm-1", "/api/vms/vm-1/diskattachments/disk-1",
                    "/api/vms/vm-1/snapshots/snap-1", "/api/vms/vm-1/checkpoints/chk-1", "/api/vms/vm-1/nics/nic-1"}) {
                final VmsRouteHandler handler = new VmsRouteHandler();
                handler.serverAdapter = mock(org.apache.cloudstack.veeam.adapter.ServerAdapter.class);
                when(handler.serverAdapter.deleteInstance("vm-1", true, true)).thenReturn(new VmAction());
                when(handler.serverAdapter.deleteSnapshot("snap-1", true)).thenReturn(new ResourceAction());

                final ResponseCapture delete = newResponse();
                handler.handle(newRequest("DELETE", Map.of("async", async), null, null), delete.response, path,
                        Negotiation.OutFormat.JSON, newServlet());

                verify(delete.response).setStatus(202);
                assertEquals("Expected empty body for " + path, "", delete.body());
            }
        }
    }

    @Test
    public void testHandleDelete_SyncWithoutJobReturnsCompletedAction() throws Exception {
        for (String path : new String[] {"/api/vms/vm-1/diskattachments/disk-1", "/api/vms/vm-1/checkpoints/chk-1",
                "/api/vms/vm-1/nics/nic-1"}) {
            final VmsRouteHandler handler = new VmsRouteHandler();
            handler.serverAdapter = mock(org.apache.cloudstack.veeam.adapter.ServerAdapter.class);

            final ResponseCapture delete = newResponse();
            handler.handle(newRequest("DELETE"), delete.response, path, Negotiation.OutFormat.JSON, newServlet());

            verify(delete.response).setStatus(200);
            assertEquals("{\"status\":\"complete\"}", delete.body());
        }
    }

    @Test
    public void testHandleActions_OtherErrorReturnsPlainFault() throws Exception {
        final VmsRouteHandler handler = new VmsRouteHandler();
        handler.serverAdapter = mock(org.apache.cloudstack.veeam.adapter.ServerAdapter.class);
        when(handler.serverAdapter.startInstance("vm-1", false)).thenThrow(new CloudRuntimeException("Failed to start VM: boom"));

        final ResponseCapture start = newResponse();
        handler.handle(newRequest("POST"), start.response, "/api/vms/vm-1/start", Negotiation.OutFormat.JSON, newServlet());

        verify(start.response).setStatus(400);
        assertContains(start.body(), "Failed to start VM: boom");
        assertFalse(start.body().contains("\"status\":\"failed\""));
    }

    @Test
    public void testHandleGetNicById() throws Exception {
        final VmsRouteHandler handler = new VmsRouteHandler();
        handler.serverAdapter = mock(org.apache.cloudstack.veeam.adapter.ServerAdapter.class);
        when(handler.serverAdapter.getInstanceNic("vm-1", "nic-1")).thenReturn(withId(new Nic(), "nic-1"));

        final ResponseCapture get = newResponse();
        handler.handle(newRequest("GET"), get.response, "/api/vms/vm-1/nics/nic-1", Negotiation.OutFormat.JSON, newServlet());

        verify(handler.serverAdapter).getInstanceNic("vm-1", "nic-1");
        verify(get.response).setStatus(200);
        assertContains(get.body(), "\"id\":\"nic-1\"");
    }

    @Test
    public void testHandleNicById_NotFoundReturns404() throws Exception {
        final VmsRouteHandler handler = new VmsRouteHandler();
        handler.serverAdapter = mock(org.apache.cloudstack.veeam.adapter.ServerAdapter.class);
        when(handler.serverAdapter.getInstanceNic("vm-1", "nic-x"))
                .thenThrow(new InvalidParameterValueException("NIC with ID nic-x not found for VM with ID vm-1"));
        doThrow(new InvalidParameterValueException("NIC with ID nic-x not found for VM with ID vm-1"))
                .when(handler.serverAdapter).detachInstanceNic("vm-1", "nic-x");

        for (String method : new String[] {"GET", "DELETE"}) {
            final ResponseCapture response = newResponse();
            handler.handle(newRequest(method), response.response, "/api/vms/vm-1/nics/nic-x", Negotiation.OutFormat.JSON, newServlet());
            verify(response.response).setStatus(404);
            assertContains(response.body(), "NIC with ID nic-x not found for VM with ID vm-1");
        }
    }

    @Test
    public void testHandleDeleteNicById_FailureReturns400() throws Exception {
        final VmsRouteHandler handler = new VmsRouteHandler();
        handler.serverAdapter = mock(org.apache.cloudstack.veeam.adapter.ServerAdapter.class);
        doThrow(new CloudRuntimeException("Failed to remove NIC: it is the default nic"))
                .when(handler.serverAdapter).detachInstanceNic("vm-1", "nic-1");

        final ResponseCapture delete = newResponse();
        handler.handle(newRequest("DELETE"), delete.response, "/api/vms/vm-1/nics/nic-1", Negotiation.OutFormat.JSON, newServlet());

        verify(delete.response).setStatus(400);
        assertContains(delete.body(), "Failed to remove NIC: it is the default nic");
    }

    @Test
    public void testHandleNicById_UnsupportedMethodReturns405() throws Exception {
        final VmsRouteHandler handler = new VmsRouteHandler();
        handler.serverAdapter = mock(org.apache.cloudstack.veeam.adapter.ServerAdapter.class);

        final ResponseCapture put = newResponse();
        handler.handle(newRequest("PUT"), put.response, "/api/vms/vm-1/nics/nic-1", Negotiation.OutFormat.JSON, newServlet());

        verify(put.response).setStatus(405);
        verify(put.response).setHeader("Allow", "GET, DELETE");
    }
}

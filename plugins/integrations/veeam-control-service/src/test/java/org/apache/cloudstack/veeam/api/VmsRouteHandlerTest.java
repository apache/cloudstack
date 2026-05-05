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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.apache.cloudstack.veeam.api.dto.Backup;
import org.apache.cloudstack.veeam.api.dto.Checkpoint;
import org.apache.cloudstack.veeam.api.dto.Disk;
import org.apache.cloudstack.veeam.api.dto.DiskAttachment;
import org.apache.cloudstack.veeam.api.dto.Nic;
import org.apache.cloudstack.veeam.api.dto.ResourceAction;
import org.apache.cloudstack.veeam.api.dto.Snapshot;
import org.apache.cloudstack.veeam.api.dto.Vm;
import org.apache.cloudstack.veeam.api.dto.VmAction;
import org.apache.cloudstack.veeam.utils.Negotiation;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

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
        when(handler.serverAdapter.getInstance("vm-1", true, true, true, true)).thenReturn(withId(new Vm(), "vm-1"));

        final VmAction deleteAction = new VmAction();
        deleteAction.setStatus("deleted");
        final VmAction startAction = new VmAction();
        startAction.setStatus("starting");
        final VmAction stopAction = new VmAction();
        stopAction.setStatus("stopping");
        final VmAction shutdownAction = new VmAction();
        shutdownAction.setStatus("shutting_down");
        when(handler.serverAdapter.deleteInstance("vm-1", true)).thenReturn(deleteAction);
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
        handler.handle(newRequest("DELETE", Map.of("async", "true"), null, null), delete.response,
                "/api/vms/vm-1", Negotiation.OutFormat.JSON, newServlet());
        verify(handler.serverAdapter).deleteInstance("vm-1", true);
        verify(delete.response).setStatus(200);
        assertContains(delete.body(), "deleted");

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
    public void testHandleDiskAttachmentAndNicRoutes() throws Exception {
        final VmsRouteHandler handler = new VmsRouteHandler();
        handler.serverAdapter = mock(org.apache.cloudstack.veeam.adapter.ServerAdapter.class);
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
        assertContains(delete.body(), "deleted");

        final ResponseCapture restore = newResponse();
        handler.handle(newRequest("POST"), restore.response, "/api/vms/vm-1/snapshots/snap-1/restore", Negotiation.OutFormat.JSON, newServlet());
        verify(restore.response).setStatus(202);
        assertContains(restore.body(), "restored");
    }

    @Test
    public void testHandleBackupRoutes() throws Exception {
        final VmsRouteHandler handler = new VmsRouteHandler();
        handler.serverAdapter = mock(org.apache.cloudstack.veeam.adapter.ServerAdapter.class);
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
}

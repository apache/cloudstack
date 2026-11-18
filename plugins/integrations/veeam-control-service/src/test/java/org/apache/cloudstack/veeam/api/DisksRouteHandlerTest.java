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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.apache.cloudstack.veeam.api.dto.Disk;
import org.apache.cloudstack.veeam.utils.Negotiation;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class DisksRouteHandlerTest extends RouteHandlerTestSupport {

    @Test
    public void testHandleGetListAndGetById() throws Exception {
        final DisksRouteHandler handler = new DisksRouteHandler();
        handler.serverAdapter = mock(org.apache.cloudstack.veeam.adapter.ServerAdapter.class);
        when(handler.serverAdapter.listAllDisks(null, 10L)).thenReturn(List.of(withId(new Disk(), "disk-1")));
        when(handler.serverAdapter.getDisk("disk-1")).thenReturn(withId(new Disk(), "disk-1"));

        final ResponseCapture list = newResponse();
        handler.handle(newRequest("GET", Map.of("max", "10"), null, null), list.response,
                "/api/disks", Negotiation.OutFormat.JSON, newServlet());
        verify(list.response).setStatus(200);
        assertContains(list.body(), "\"disk\":[");

        final ResponseCapture item = newResponse();
        handler.handle(newRequest("GET"), item.response, "/api/disks/disk-1", Negotiation.OutFormat.JSON, newServlet());
        verify(item.response).setStatus(200);
        assertContains(item.body(), "\"id\":\"disk-1\"");
    }

    @Test
    public void testHandlePostAndPutParseDiskJson() throws Exception {
        final DisksRouteHandler handler = new DisksRouteHandler();
        handler.serverAdapter = mock(org.apache.cloudstack.veeam.adapter.ServerAdapter.class);

        final ArgumentCaptor<Disk> createCaptor = ArgumentCaptor.forClass(Disk.class);
        final Disk created = withId(new Disk(), "disk-created");
        created.setName("created-disk");
        when(handler.serverAdapter.createDisk(createCaptor.capture())).thenReturn(created);

        final ResponseCapture post = newResponse();
        handler.handle(newRequest("POST", Map.of(), "application/json", "{\"name\":\"created-disk\"}"), post.response,
                "/api/disks", Negotiation.OutFormat.JSON, newServlet());
        verify(post.response).setStatus(201);
        assertEquals("created-disk", createCaptor.getValue().getName());
        assertContains(post.body(), "disk-created");

        final ArgumentCaptor<Disk> updateCaptor = ArgumentCaptor.forClass(Disk.class);
        final Disk updated = withId(new Disk(), "disk-1");
        updated.setName("updated-disk");
        when(handler.serverAdapter.updateDisk(org.mockito.ArgumentMatchers.eq("disk-1"), updateCaptor.capture())).thenReturn(updated);

        final ResponseCapture put = newResponse();
        handler.handle(newRequest("PUT", Map.of(), "application/json", "{\"name\":\"updated-disk\"}"), put.response,
                "/api/disks/disk-1", Negotiation.OutFormat.JSON, newServlet());
        verify(put.response).setStatus(200);
        assertEquals("updated-disk", updateCaptor.getValue().getName());
        assertContains(put.body(), "updated-disk");
    }

    @Test
    public void testHandleDeleteCopyAndReduceRoutes() throws Exception {
        final DisksRouteHandler handler = new DisksRouteHandler();
        handler.serverAdapter = mock(org.apache.cloudstack.veeam.adapter.ServerAdapter.class);
        when(handler.serverAdapter.copyDisk("disk-1")).thenReturn(withId(new Disk(), "copy-1"));
        when(handler.serverAdapter.reduceDisk("disk-1")).thenReturn(withId(new Disk(), "reduced-1"));

        final ResponseCapture delete = newResponse();
        handler.handle(newRequest("DELETE"), delete.response, "/api/disks/disk-1", Negotiation.OutFormat.JSON, newServlet());
        verify(handler.serverAdapter).deleteDisk("disk-1");
        verify(delete.response).setStatus(200);
        assertContains(delete.body(), "Deleted disk ID: disk-1");

        final ResponseCapture copy = newResponse();
        handler.handle(newRequest("POST", Map.of(), "application/json", "{}"), copy.response,
                "/api/disks/disk-1/copy", Negotiation.OutFormat.JSON, newServlet());
        verify(copy.response).setStatus(200);
        assertContains(copy.body(), "copy-1");

        final ResponseCapture reduce = newResponse();
        handler.handle(newRequest("POST", Map.of(), "application/json", "{}"), reduce.response,
                "/api/disks/disk-1/reduce", Negotiation.OutFormat.JSON, newServlet());
        verify(reduce.response).setStatus(200);
        assertContains(reduce.body(), "reduced-1");
    }

    @Test
    public void testHandleCopyRejectsUnsupportedMethod() throws Exception {
        final DisksRouteHandler handler = new DisksRouteHandler();
        handler.serverAdapter = mock(org.apache.cloudstack.veeam.adapter.ServerAdapter.class);

        final ResponseCapture response = newResponse();
        handler.handle(newRequest("GET"), response.response, "/api/disks/disk-1/copy", Negotiation.OutFormat.JSON, newServlet());

        verify(response.response).setHeader("Allow", "POST");
        verify(response.response).setStatus(405);
        assertContains(response.body(), "Method Not Allowed");
    }
}

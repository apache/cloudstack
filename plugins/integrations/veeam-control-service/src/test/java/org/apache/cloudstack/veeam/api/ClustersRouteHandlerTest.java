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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import com.cloud.exception.InvalidParameterValueException;
import org.apache.cloudstack.veeam.api.dto.Cluster;
import org.apache.cloudstack.veeam.utils.Negotiation;
import org.junit.Test;

public class ClustersRouteHandlerTest extends RouteHandlerTestSupport {

    @Test
    public void testHandleGetListReturnsNamedClusterList() throws Exception {
        final ClustersRouteHandler handler = new ClustersRouteHandler();
        handler.serverAdapter = mock(org.apache.cloudstack.veeam.adapter.ServerAdapter.class);
        when(handler.serverAdapter.listAllClusters(null, 25L)).thenReturn(List.of(withId(new Cluster(), "cl-1")));

        final ResponseCapture response = newResponse();
        handler.handle(newRequest("GET", Map.of("max", "25"), null, null), response.response,
                "/api/clusters", Negotiation.OutFormat.JSON, newServlet());

        verify(handler.serverAdapter).listAllClusters(null, 25L);
        verify(response.response).setStatus(200);
        assertContains(response.body(), "\"cluster\":[");
        assertContains(response.body(), "\"id\":\"cl-1\"");
    }

    @Test
    public void testHandleGetByIdReturnsClusterAndMissingClusterIsNotFound() throws Exception {
        final ClustersRouteHandler handler = new ClustersRouteHandler();
        handler.serverAdapter = mock(org.apache.cloudstack.veeam.adapter.ServerAdapter.class);
        when(handler.serverAdapter.getCluster("cl-1")).thenReturn(withId(new Cluster(), "cl-1"));
        when(handler.serverAdapter.getCluster("missing")).thenThrow(new InvalidParameterValueException("missing"));

        final ResponseCapture ok = newResponse();
        handler.handle(newRequest("GET"), ok.response, "/api/clusters/cl-1", Negotiation.OutFormat.JSON, newServlet());
        verify(ok.response).setStatus(200);
        assertContains(ok.body(), "\"id\":\"cl-1\"");

        final ResponseCapture missing = newResponse();
        handler.handle(newRequest("GET"), missing.response, "/api/clusters/missing", Negotiation.OutFormat.JSON, newServlet());
        verify(missing.response).setStatus(404);
        assertContains(missing.body(), "missing");
    }

    @Test
    public void testHandleRejectsUnsupportedMethod() throws Exception {
        final ClustersRouteHandler handler = new ClustersRouteHandler();
        handler.serverAdapter = mock(org.apache.cloudstack.veeam.adapter.ServerAdapter.class);

        final ResponseCapture response = newResponse();
        handler.handle(newRequest("POST"), response.response, "/api/clusters", Negotiation.OutFormat.JSON, newServlet());

        verify(response.response).setHeader("Allow", "GET");
        verify(response.response).setStatus(405);
        assertContains(response.body(), "Method Not Allowed");
    }
}

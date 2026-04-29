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
import org.apache.cloudstack.veeam.api.dto.DataCenter;
import org.apache.cloudstack.veeam.api.dto.Network;
import org.apache.cloudstack.veeam.api.dto.StorageDomain;
import org.apache.cloudstack.veeam.utils.Negotiation;
import org.junit.Test;

public class DataCentersRouteHandlerTest extends RouteHandlerTestSupport {

    @Test
    public void testHandleGetListAndById() throws Exception {
        final DataCentersRouteHandler handler = new DataCentersRouteHandler();
        handler.serverAdapter = mock(org.apache.cloudstack.veeam.adapter.ServerAdapter.class);
        when(handler.serverAdapter.listAllDataCenters(null, 20L)).thenReturn(List.of(withId(new DataCenter(), "dc-1")));
        when(handler.serverAdapter.getDataCenter("dc-1")).thenReturn(withId(new DataCenter(), "dc-1"));

        final ResponseCapture list = newResponse();
        handler.handle(newRequest("GET", Map.of("max", "20"), null, null), list.response,
                "/api/datacenters", Negotiation.OutFormat.JSON, newServlet());
        verify(list.response).setStatus(200);
        assertContains(list.body(), "\"data_center\":[");

        final ResponseCapture item = newResponse();
        handler.handle(newRequest("GET"), item.response, "/api/datacenters/dc-1", Negotiation.OutFormat.JSON, newServlet());
        verify(item.response).setStatus(200);
        assertContains(item.body(), "\"id\":\"dc-1\"");
    }

    @Test
    public void testHandleGetStorageDomainsAndNetworksByDataCenterId() throws Exception {
        final DataCentersRouteHandler handler = new DataCentersRouteHandler();
        handler.serverAdapter = mock(org.apache.cloudstack.veeam.adapter.ServerAdapter.class);
        when(handler.serverAdapter.listStorageDomainsByDcId("dc-1", null, 15L))
                .thenReturn(List.of(withId(new StorageDomain(), "sd-1")));
        when(handler.serverAdapter.listNetworksByDcId("dc-1", null, 15L))
                .thenReturn(List.of(withId(new Network(), "net-1")));

        final ResponseCapture storageDomains = newResponse();
        handler.handle(newRequest("GET", Map.of("max", "15"), null, null), storageDomains.response,
                "/api/datacenters/dc-1/storagedomains", Negotiation.OutFormat.JSON, newServlet());
        verify(storageDomains.response).setStatus(200);
        assertContains(storageDomains.body(), "\"storage_domain\":[");
        assertContains(storageDomains.body(), "sd-1");

        final ResponseCapture networks = newResponse();
        handler.handle(newRequest("GET", Map.of("max", "15"), null, null), networks.response,
                "/api/datacenters/dc-1/networks", Negotiation.OutFormat.JSON, newServlet());
        verify(networks.response).setStatus(200);
        assertContains(networks.body(), "\"network\":[");
        assertContains(networks.body(), "net-1");
    }

    @Test
    public void testHandleMissingDataCenterReturnsNotFound() throws Exception {
        final DataCentersRouteHandler handler = new DataCentersRouteHandler();
        handler.serverAdapter = mock(org.apache.cloudstack.veeam.adapter.ServerAdapter.class);
        when(handler.serverAdapter.getDataCenter("missing")).thenThrow(new InvalidParameterValueException("missing dc"));

        final ResponseCapture response = newResponse();
        handler.handle(newRequest("GET"), response.response, "/api/datacenters/missing", Negotiation.OutFormat.JSON, newServlet());

        verify(response.response).setStatus(404);
        assertContains(response.body(), "missing dc");
    }
}

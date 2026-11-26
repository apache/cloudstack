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
import org.apache.cloudstack.veeam.api.dto.StorageDomain;
import org.apache.cloudstack.veeam.utils.Negotiation;
import org.junit.Test;

public class StorageDomainsRouteHandlerTest extends RouteHandlerTestSupport {

    @Test
    public void testHandleGetListAndById() throws Exception {
        final StorageDomainsRouteHandler handler = new StorageDomainsRouteHandler();
        handler.serverAdapter = mock(org.apache.cloudstack.veeam.adapter.ServerAdapter.class);
        when(handler.serverAdapter.listAllStorageDomains(null, null, null)).thenReturn(List.of(withId(new StorageDomain(), "sd-1")));
        when(handler.serverAdapter.getStorageDomain("sd-1")).thenReturn(withId(new StorageDomain(), "sd-1"));

        final ResponseCapture list = newResponse();
        handler.handle(newRequest("GET"), list.response, "/api/storagedomains", Negotiation.OutFormat.JSON, newServlet());
        verify(list.response).setStatus(200);
        assertContains(list.body(), "\"storage_domain\":[");

        final ResponseCapture item = newResponse();
        handler.handle(newRequest("GET"), item.response, "/api/storagedomains/sd-1", Negotiation.OutFormat.JSON, newServlet());
        verify(item.response).setStatus(200);
        assertContains(item.body(), "\"id\":\"sd-1\"");
    }

    @Test
    public void testHandleGetListPassesSearchNamePattern() throws Exception {
        final StorageDomainsRouteHandler handler = new StorageDomainsRouteHandler();
        handler.serverAdapter = mock(org.apache.cloudstack.veeam.adapter.ServerAdapter.class);
        when(handler.serverAdapter.listAllStorageDomains("data%", null, null)).thenReturn(List.of(withId(new StorageDomain(), "sd-1")));

        final ResponseCapture list = newResponse();
        handler.handle(newRequest("GET", Map.of("search", "name=data*"), null, null), list.response,
                "/api/storagedomains", Negotiation.OutFormat.JSON, newServlet());

        verify(list.response).setStatus(200);
        assertContains(list.body(), "sd-1");
    }

    @Test
    public void testHandleMissingStorageDomainReturnsNotFound() throws Exception {
        final StorageDomainsRouteHandler handler = new StorageDomainsRouteHandler();
        handler.serverAdapter = mock(org.apache.cloudstack.veeam.adapter.ServerAdapter.class);
        when(handler.serverAdapter.getStorageDomain("missing")).thenThrow(new InvalidParameterValueException("missing storage domain"));

        final ResponseCapture response = newResponse();
        handler.handle(newRequest("GET"), response.response, "/api/storagedomains/missing", Negotiation.OutFormat.JSON, newServlet());

        verify(response.response).setStatus(404);
        assertContains(response.body(), "missing storage domain");
    }

    @Test
    public void testHandleNonGetReturnsMethodNotAllowed() throws Exception {
        final StorageDomainsRouteHandler handler = new StorageDomainsRouteHandler();
        handler.serverAdapter = mock(org.apache.cloudstack.veeam.adapter.ServerAdapter.class);

        final ResponseCapture response = newResponse();
        handler.handle(newRequest("POST"), response.response, "/api/storagedomains", Negotiation.OutFormat.JSON, newServlet());

        verify(response.response).setStatus(405);
    }
}

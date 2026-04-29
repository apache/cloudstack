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

import com.cloud.exception.InvalidParameterValueException;
import org.apache.cloudstack.veeam.api.dto.Host;
import org.apache.cloudstack.veeam.utils.Negotiation;
import org.junit.Test;

public class HostsRouteHandlerTest extends RouteHandlerTestSupport {

    @Test
    public void testHandleGetListAndGetById() throws Exception {
        final HostsRouteHandler handler = new HostsRouteHandler();
        handler.serverAdapter = mock(org.apache.cloudstack.veeam.adapter.ServerAdapter.class);
        when(handler.serverAdapter.listAllHosts(null, null)).thenReturn(List.of(withId(new Host(), "host-1")));
        when(handler.serverAdapter.getHost("host-1")).thenReturn(withId(new Host(), "host-1"));

        final ResponseCapture list = newResponse();
        handler.handle(newRequest("GET"), list.response, "/api/hosts", Negotiation.OutFormat.JSON, newServlet());
        verify(list.response).setStatus(200);
        assertContains(list.body(), "\"host\":[");
        assertContains(list.body(), "host-1");

        final ResponseCapture item = newResponse();
        handler.handle(newRequest("GET"), item.response, "/api/hosts/host-1", Negotiation.OutFormat.JSON, newServlet());
        verify(item.response).setStatus(200);
        assertContains(item.body(), "\"id\":\"host-1\"");
    }

    @Test
    public void testHandleGetByIdNotFound() throws Exception {
        final HostsRouteHandler handler = new HostsRouteHandler();
        handler.serverAdapter = mock(org.apache.cloudstack.veeam.adapter.ServerAdapter.class);
        when(handler.serverAdapter.getHost("missing")).thenThrow(new InvalidParameterValueException("missing host"));

        final ResponseCapture response = newResponse();
        handler.handle(newRequest("GET"), response.response, "/api/hosts/missing", Negotiation.OutFormat.JSON, newServlet());

        verify(response.response).setStatus(404);
        assertContains(response.body(), "missing host");
    }
}

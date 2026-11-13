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

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import com.cloud.exception.InvalidParameterValueException;
import org.apache.cloudstack.veeam.api.dto.Host;
import org.apache.cloudstack.veeam.api.dto.Ref;
import org.apache.cloudstack.veeam.api.dto.Statistic;
import org.apache.cloudstack.veeam.utils.Negotiation;
import org.junit.Test;

public class HostsRouteHandlerTest extends RouteHandlerTestSupport {

    @Test
    public void testHandleGetListAndGetById() throws Exception {
        final HostsRouteHandler handler = new HostsRouteHandler();
        handler.serverAdapter = mock(org.apache.cloudstack.veeam.adapter.ServerAdapter.class);
        when(handler.serverAdapter.listAllHosts(false, null, null)).thenReturn(List.of(withId(new Host(), "host-1")));
        when(handler.serverAdapter.getHost("host-1", false)).thenReturn(withId(new Host(), "host-1"));

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
        when(handler.serverAdapter.getHost("missing", false)).thenThrow(new InvalidParameterValueException("missing host"));

        final ResponseCapture response = newResponse();
        handler.handle(newRequest("GET"), response.response, "/api/hosts/missing", Negotiation.OutFormat.JSON, newServlet());

        verify(response.response).setStatus(404);
        assertContains(response.body(), "missing host");
    }

    @Test
    public void testHandleGetListAndGetById_FollowStatistics() throws Exception {
        final HostsRouteHandler handler = new HostsRouteHandler();
        handler.serverAdapter = mock(org.apache.cloudstack.veeam.adapter.ServerAdapter.class);
        when(handler.serverAdapter.listAllHosts(true, 0L, 100L)).thenReturn(List.of(withId(new Host(), "host-1")));
        when(handler.serverAdapter.getHost("host-1", true)).thenReturn(withId(new Host(), "host-1"));

        final ResponseCapture list = newResponse();
        handler.handle(newRequest("GET", Map.of("follow", "statistics", "max", "100", "search", "page 1"), null, null),
                list.response, "/api/hosts", Negotiation.OutFormat.JSON, newServlet());
        verify(list.response).setStatus(200);
        verify(handler.serverAdapter).listAllHosts(true, 0L, 100L);

        final ResponseCapture item = newResponse();
        handler.handle(newRequest("GET", Map.of("follow", "statistics"), null, null),
                item.response, "/api/hosts/host-1", Negotiation.OutFormat.JSON, newServlet());
        verify(item.response).setStatus(200);
        verify(handler.serverAdapter).getHost("host-1", true);
    }

    @Test
    public void testHandleGetStatistics() throws Exception {
        final HostsRouteHandler handler = new HostsRouteHandler();
        handler.serverAdapter = mock(org.apache.cloudstack.veeam.adapter.ServerAdapter.class);
        final Statistic statistic = Statistic.hostGauge(Ref.of("/api/hosts/host-1", "host-1"), "memory.total", "Total memory",
                Statistic.TYPE_INTEGER, Statistic.UNIT_BYTES, BigDecimal.valueOf(1024));
        when(handler.serverAdapter.listHostStatistics("host-1")).thenReturn(List.of(statistic));

        final ResponseCapture list = newResponse();
        handler.handle(newRequest("GET"), list.response, "/api/hosts/host-1/statistics", Negotiation.OutFormat.JSON, newServlet());
        verify(list.response).setStatus(200);
        assertContains(list.body(), "\"statistic\":[");
        assertContains(list.body(), "\"datum\":1024");

        final ResponseCapture item = newResponse();
        handler.handle(newRequest("GET"), item.response, "/api/hosts/host-1/statistics/" + statistic.getId(),
                Negotiation.OutFormat.JSON, newServlet());
        verify(item.response).setStatus(200);
        assertContains(item.body(), "\"name\":\"memory.total\"");

        final ResponseCapture missing = newResponse();
        handler.handle(newRequest("GET"), missing.response, "/api/hosts/host-1/statistics/unknown",
                Negotiation.OutFormat.JSON, newServlet());
        verify(missing.response).setStatus(404);
    }

    @Test
    public void testHandleGetStatisticsHostNotFound() throws Exception {
        final HostsRouteHandler handler = new HostsRouteHandler();
        handler.serverAdapter = mock(org.apache.cloudstack.veeam.adapter.ServerAdapter.class);
        when(handler.serverAdapter.listHostStatistics("missing")).thenThrow(new InvalidParameterValueException("missing host"));

        final ResponseCapture response = newResponse();
        handler.handle(newRequest("GET"), response.response, "/api/hosts/missing/statistics", Negotiation.OutFormat.JSON, newServlet());

        verify(response.response).setStatus(404);
        assertContains(response.body(), "missing host");
    }
}

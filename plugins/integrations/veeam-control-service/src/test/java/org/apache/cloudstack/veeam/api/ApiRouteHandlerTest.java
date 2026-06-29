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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cloud.user.Account;
import com.cloud.user.User;
import com.cloud.utils.Pair;
import org.apache.cloudstack.veeam.VeeamControlService;
import org.apache.cloudstack.veeam.api.dto.Api;
import org.apache.cloudstack.veeam.utils.Negotiation;
import org.junit.Test;

public class ApiRouteHandlerTest extends RouteHandlerTestSupport {

    @Test
    public void testCanHandleAndHandleRootApiRequest() throws Exception {
        final ApiRouteHandler handler = new ApiRouteHandler();
        handler.serverAdapter = mock(org.apache.cloudstack.veeam.adapter.ServerAdapter.class);
        handler.veeamControlService = mock(VeeamControlService.class);

        final User user = mock(User.class);
        when(user.getUuid()).thenReturn("user-1");
        when(handler.serverAdapter.getServiceAccount()).thenReturn(new Pair<>(user, mock(Account.class)));
        when(handler.veeamControlService.getInstanceId()).thenReturn("instance-1");

        final ResponseCapture response = newResponse();
        handler.handle(newRequest("GET"), response.response, "/api", Negotiation.OutFormat.JSON, newServlet());

        assertTrue(handler.canHandle("GET", "/api?x=1"));
        verify(response.response).setStatus(200);
        assertContains(response.body(), "\"instance_id\":\"instance-1\"");
        assertContains(response.body(), "\"authenticated_user\"");
        assertContains(response.body(), "clusters/search");
    }

    @Test
    public void testCreateApiObjectBuildsLinksAndUserReferences() {
        final ApiRouteHandler handler = new ApiRouteHandler();
        handler.serverAdapter = mock(org.apache.cloudstack.veeam.adapter.ServerAdapter.class);
        handler.veeamControlService = mock(VeeamControlService.class);

        final User user = mock(User.class);
        when(user.getUuid()).thenReturn("service-user");
        when(handler.serverAdapter.getServiceAccount()).thenReturn(new Pair<>(user, mock(Account.class)));
        when(handler.veeamControlService.getInstanceId()).thenReturn("instance-2");

        final Api api = handler.createApiObject("/ctx/api");

        assertNotNull(api.getLink());
        assertTrue(!api.getLink().isEmpty());
        assertEquals("instance-2", api.getProductInfo().getInstanceId());
        assertEquals("service-user", api.getAuthenticatedUser().getId());
        assertEquals("service-user", api.getEffectiveUser().getId());
        assertNotNull(api.getTime());
    }

    @Test
    public void testHandleUnknownPathReturnsNotFound() throws Exception {
        final ApiRouteHandler handler = new ApiRouteHandler();
        handler.serverAdapter = mock(org.apache.cloudstack.veeam.adapter.ServerAdapter.class);
        handler.veeamControlService = mock(VeeamControlService.class);

        final ResponseCapture response = newResponse();
        handler.handle(newRequest("GET"), response.response, "/api/unknown", Negotiation.OutFormat.JSON, newServlet());

        verify(response.response).setStatus(404);
        assertContains(response.body(), "\"reason\":\"Not found\"");
    }
}

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
import org.apache.cloudstack.veeam.api.dto.Job;
import org.apache.cloudstack.veeam.utils.Negotiation;
import org.junit.Test;

public class JobsRouteHandlerTest extends RouteHandlerTestSupport {

    @Test
    public void testHandleGetListAndGetById() throws Exception {
        final JobsRouteHandler handler = new JobsRouteHandler();
        handler.serverAdapter = mock(org.apache.cloudstack.veeam.adapter.ServerAdapter.class);
        when(handler.serverAdapter.listPendingJobs()).thenReturn(List.of(withId(new Job(), "job-1")));
        when(handler.serverAdapter.getJob("job-1")).thenReturn(withId(new Job(), "job-1"));

        final ResponseCapture list = newResponse();
        handler.handle(newRequest("GET"), list.response, "/api/jobs", Negotiation.OutFormat.JSON, newServlet());
        verify(list.response).setStatus(200);
        assertContains(list.body(), "\"job\":[");

        final ResponseCapture item = newResponse();
        handler.handle(newRequest("GET"), item.response, "/api/jobs/job-1", Negotiation.OutFormat.JSON, newServlet());
        verify(item.response).setStatus(200);
        assertContains(item.body(), "\"id\":\"job-1\"");
    }

    @Test
    public void testHandleMissingJobAndUnsupportedMethod() throws Exception {
        final JobsRouteHandler handler = new JobsRouteHandler();
        handler.serverAdapter = mock(org.apache.cloudstack.veeam.adapter.ServerAdapter.class);
        when(handler.serverAdapter.getJob("missing")).thenThrow(new InvalidParameterValueException("missing job"));

        final ResponseCapture missing = newResponse();
        handler.handle(newRequest("GET"), missing.response, "/api/jobs/missing", Negotiation.OutFormat.JSON, newServlet());
        verify(missing.response).setStatus(404);
        assertContains(missing.body(), "missing job");

        final ResponseCapture wrongMethod = newResponse();
        handler.handle(newRequest("POST"), wrongMethod.response, "/api/jobs", Negotiation.OutFormat.JSON, newServlet());
        verify(wrongMethod.response).setStatus(405);
        assertContains(wrongMethod.body(), "Method Not Allowed");
    }
}

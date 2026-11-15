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

package org.apache.cloudstack.veeam;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;

import javax.servlet.http.HttpServletRequest;

import org.apache.cloudstack.veeam.utils.Negotiation;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

public class VeeamControlServletTest {

    @Test
    public void testServiceHandlesRootRequestForGet() throws Exception {
        final VeeamControlServlet servlet = new VeeamControlServlet(Collections.emptyList());
        final HttpServletRequest request = new MockHttpServletRequest("GET", "/");
        final MockHttpServletResponse response = new MockHttpServletResponse();

        servlet.service(request, response);

        assertEquals(200, response.getStatus());
        assertTrue(response.getContentAsString().contains("CloudStack Veeam Control Service"));
    }

    @Test
    public void testServiceReturnsBadRequestForUnsupportedRootMethod() throws Exception {
        final VeeamControlServlet servlet = new VeeamControlServlet(Collections.emptyList());
        final HttpServletRequest request = new MockHttpServletRequest("PUT", "/");
        final MockHttpServletResponse response = new MockHttpServletResponse();

        servlet.service(request, response);

        assertEquals(400, response.getStatus());
        assertTrue(response.getContentAsString().contains("Unsupported method for root"));
    }

    @Test
    public void testServiceDelegatesToMatchingRouteHandler() throws Exception {
        final RouteHandler handler = mock(RouteHandler.class);
        final String path = "/api/path";
        final VeeamControlServlet servlet = new VeeamControlServlet(Collections.singletonList(handler));
        final HttpServletRequest request = new MockHttpServletRequest("GET", path);
        final MockHttpServletResponse response = new MockHttpServletResponse();
        when(handler.canHandle("GET", path)).thenReturn(true);

        servlet.service(request, response);

        verify(handler).handle(request, response, path, Negotiation.OutFormat.XML, servlet);
    }

    @Test
    public void testServiceReturnsNotFoundWhenNoHandlerMatches() throws Exception {
        final RouteHandler handler = mock(RouteHandler.class);
        final String path = "/api/path";
        final VeeamControlServlet servlet = new VeeamControlServlet(Collections.singletonList(handler));
        final MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        request.setPathInfo(path);
        request.addHeader("Accept", "application/json");
        final MockHttpServletResponse response = new MockHttpServletResponse();
        when(handler.canHandle("GET", path)).thenReturn(false);

        servlet.service(request, response);

        assertEquals(404, response.getStatus());
        assertTrue(response.getContentAsString().contains("\"reason\":\"Not found\""));
        verify(handler).canHandle("GET", path);
    }

    @Test
    public void testServiceConvertsHandlerErrorToFaultResponse() throws Exception {
        final RouteHandler handler = mock(RouteHandler.class);
        final String path = "/api/faultpath";
        final VeeamControlServlet servlet = new VeeamControlServlet(Collections.singletonList(handler));
        final HttpServletRequest request = new MockHttpServletRequest("GET", path);
        final MockHttpServletResponse response = new MockHttpServletResponse();
        when(handler.canHandle("GET", path)).thenReturn(true);
        doThrow(VeeamControlServlet.Error.unauthorized("denied")).when(handler)
                .handle(request, response, path, Negotiation.OutFormat.XML, servlet);

        servlet.service(request, response);

        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("denied"));
    }

    @Test
    public void testMethodNotAllowedWritesAllowHeaderAndFault() throws Exception {
        final VeeamControlServlet servlet = new VeeamControlServlet(Collections.emptyList());
        final MockHttpServletResponse response = new MockHttpServletResponse();

        servlet.methodNotAllowed(response, "GET, POST", Negotiation.OutFormat.JSON);

        assertEquals(405, response.getStatus());
        assertEquals("GET, POST", response.getHeader("Allow"));
    }
}

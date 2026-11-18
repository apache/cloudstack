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

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Collections;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.cloudstack.veeam.VeeamControlServlet;
import org.apache.cloudstack.veeam.api.dto.BaseDto;

class RouteHandlerTestSupport {

    protected VeeamControlServlet newServlet() {
        return new VeeamControlServlet(Collections.emptyList());
    }

    protected HttpServletRequest newRequest(final String method) throws Exception {
        return newRequest(method, Collections.emptyMap(), null, null);
    }

    protected HttpServletRequest newRequest(final String method, final Map<String, String> params,
            final String contentType, final String body) throws Exception {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final Map<String, String> safeParams = params == null ? Collections.emptyMap() : params;

        when(request.getMethod()).thenReturn(method);
        when(request.getContentType()).thenReturn(contentType);
        when(request.getParameterMap()).thenReturn(toParameterMap(safeParams));
        when(request.getParameter(org.mockito.ArgumentMatchers.anyString())).thenAnswer(invocation -> safeParams.get(invocation.getArgument(0)));
        when(request.getReader()).thenReturn(new BufferedReader(new StringReader(body == null ? "" : body)));
        return request;
    }

    protected ResponseCapture newResponse() throws Exception {
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final StringWriter sink = new StringWriter();
        final PrintWriter writer = new PrintWriter(sink);
        when(response.getWriter()).thenReturn(writer);
        return new ResponseCapture(response, sink, writer);
    }

    protected static <T extends BaseDto> T withId(final T dto, final String id) {
        dto.setId(id);
        dto.setHref("/api/test/" + id);
        return dto;
    }

    protected static void assertContains(final String actual, final String expected) {
        assertTrue("Expected body to contain: " + expected + " but was: " + actual, actual.contains(expected));
    }

    private static Map<String, String[]> toParameterMap(final Map<String, String> params) {
        final java.util.LinkedHashMap<String, String[]> result = new java.util.LinkedHashMap<>();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            result.put(entry.getKey(), new String[]{entry.getValue()});
        }
        return result;
    }

    protected static class ResponseCapture {
        final HttpServletResponse response;
        private final StringWriter sink;
        private final PrintWriter writer;

        ResponseCapture(final HttpServletResponse response, final StringWriter sink, final PrintWriter writer) {
            this.response = response;
            this.sink = sink;
            this.writer = writer;
        }

        String body() {
            writer.flush();
            return sink.toString();
        }
    }
}

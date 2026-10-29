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

package org.apache.cloudstack.veeam.utils;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.PrintWriter;
import java.io.StringWriter;

import javax.servlet.http.HttpServletResponse;

import org.junit.Test;

public class ResponseWriterTest {

    @Test
    public void testWrite_NullBodySetsStatusAndZeroContentLength() throws Exception {
        final Mapper mapper = new Mapper();
        final ResponseWriter responseWriter = new ResponseWriter(mapper);
        final HttpServletResponse response = mock(HttpServletResponse.class);

        responseWriter.write(response, 204, null, Negotiation.OutFormat.XML);

        verify(response).setStatus(204);
        verify(response).setContentLength(0);
        verify(response, never()).getWriter();
    }

    @Test
    public void testWrite_JsonBodyWritesPayloadAndHeaders() throws Exception {
        final ResponseWriter responseWriter = new ResponseWriter(new Mapper());
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final StringWriter sink = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sink));

        responseWriter.write(response, 200, new Payload("item-1"), Negotiation.OutFormat.JSON);

        verify(response).setStatus(200);
        verify(response).setHeader("Content-Type", "application/json");
        assertTrue(sink.toString().contains("\"name\":\"item-1\""));
    }

    @Test
    public void testWrite_XmlBodyWritesPayloadAndHeaders() throws Exception {
        final ResponseWriter responseWriter = new ResponseWriter(new Mapper());
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final StringWriter sink = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sink));

        responseWriter.write(response, 200, new Payload("item-2"), Negotiation.OutFormat.XML);

        verify(response).setHeader("Content-Type", "application/xml");
        assertTrue(sink.toString().contains("<name>item-2</name>"));
    }

    @Test
    public void testWrite_WhenMappingFailsReturnsInternalServerError() throws Exception {
        final Mapper mapper = mock(Mapper.class);
        doThrow(new RuntimeException("boom")).when(mapper).toJson(org.mockito.ArgumentMatchers.any());

        final ResponseWriter responseWriter = new ResponseWriter(mapper);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final StringWriter sink = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sink));

        responseWriter.write(response, 200, new Payload("ignored"), Negotiation.OutFormat.JSON);

        verify(response).setStatus(500);
        verify(response).setHeader("Content-Type", "text/plain");
        assertTrue(sink.toString().contains("Internal Server Error"));
        verify(response, never()).setContentLength(anyInt());
    }

    @Test
    public void testWriteFault_JsonWritesFaultStructure() throws Exception {
        final ResponseWriter responseWriter = new ResponseWriter(new Mapper());
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final StringWriter sink = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sink));

        responseWriter.writeFault(response, 404, "Not Found", "missing vm", Negotiation.OutFormat.JSON);

        verify(response).setStatus(404);
        assertTrue(sink.toString().contains("\"reason\":\"Not Found\""));
        assertTrue(sink.toString().contains("\"detail\":\"missing vm\""));
    }

    static class Payload {
        public String name;

        Payload(final String name) {
            this.name = name;
        }
    }
}

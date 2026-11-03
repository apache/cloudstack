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

package org.apache.cloudstack.veeam.services;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

import org.apache.cloudstack.ca.CAManager;
import org.apache.cloudstack.veeam.VeeamControlServlet;
import org.apache.cloudstack.veeam.utils.Negotiation;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@RunWith(MockitoJUnitRunner.class)
public class PkiResourceRouteHandlerTest {

    @Test
    public void testCanHandleSanitizesQueryParameters() {
        final PkiResourceRouteHandler handler = new PkiResourceRouteHandler();

        assertTrue(handler.canHandle("GET", "/services/pki-resource?resource=ca-certificate"));
    }

    @Test
    public void testHandleReturnsCertificateDownload() throws Exception {
        final PkiResourceRouteHandler handler = new PkiResourceRouteHandler();
        final CAManager caManager = mock(CAManager.class);
        final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/services/pki-resource");
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final String certificate = "-----BEGIN CERTIFICATE-----\nveeam\n-----END CERTIFICATE-----\n";
        handler.caManager = caManager;
        when(caManager.getCaCertificate(null)).thenReturn(certificate);

        handler.handle(request, response, "/services/pki-resource", Negotiation.OutFormat.JSON,
                new VeeamControlServlet(Collections.emptyList()));

        assertEquals(200, response.getStatus());
        assertEquals("no-store", response.getHeader("Cache-Control"));
        assertEquals("attachment; filename=\"pki-resource.cer\"", response.getHeader("Content-Disposition"));
        assertEquals("application/x-x509-ca-cert; charset=ISO-8859-1", response.getContentType());
        assertArrayEquals(certificate.getBytes(StandardCharsets.ISO_8859_1), response.getContentAsByteArray());
    }

    @Test
    public void testHandleRejectsUnsupportedResource() throws Exception {
        final PkiResourceRouteHandler handler = new PkiResourceRouteHandler();
        handler.caManager = mock(CAManager.class);
        final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/services/pki-resource");
        final MockHttpServletResponse response = new MockHttpServletResponse();
        request.setParameter("resource", "unsupported");

        handler.handle(request, response, "/services/pki-resource", Negotiation.OutFormat.JSON,
                new VeeamControlServlet(Collections.emptyList()));

        assertEquals(400, response.getStatus());
        assertEquals("Unsupported resource", response.getErrorMessage());
    }

    @Test
    public void testHandleRejectsUnsupportedFormat() throws Exception {
        final PkiResourceRouteHandler handler = new PkiResourceRouteHandler();
        handler.caManager = mock(CAManager.class);
        final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/services/pki-resource");
        final MockHttpServletResponse response = new MockHttpServletResponse();
        request.setParameter("format", "PEM");

        handler.handle(request, response, "/services/pki-resource", Negotiation.OutFormat.JSON,
                new VeeamControlServlet(Collections.emptyList()));

        assertEquals(400, response.getStatus());
        assertEquals("Unsupported format", response.getErrorMessage());
    }

    @Test
    public void testHandleRejectsEmptyCertificateData() throws Exception {
        final PkiResourceRouteHandler handler = new PkiResourceRouteHandler();
        final CAManager caManager = mock(CAManager.class);
        final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/services/pki-resource");
        final MockHttpServletResponse response = new MockHttpServletResponse();
        handler.caManager = caManager;
        when(caManager.getCaCertificate(null)).thenReturn("");

        handler.handle(request, response, "/services/pki-resource", Negotiation.OutFormat.JSON,
                new VeeamControlServlet(Collections.emptyList()));

        assertEquals(500, response.getStatus());
        assertEquals("No certificate data available", response.getErrorMessage());
    }

    @Test
    public void testHandleReturnsNotFoundForUnknownPath() throws Exception {
        final PkiResourceRouteHandler handler = new PkiResourceRouteHandler();
        handler.caManager = mock(CAManager.class);
        final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/services/pki-resource/unknown");
        final MockHttpServletResponse response = new MockHttpServletResponse();

        handler.handle(request, response, "/services/pki-resource/unknown", Negotiation.OutFormat.JSON,
                new VeeamControlServlet(Collections.emptyList()));

        assertEquals(404, response.getStatus());
        assertTrue(response.getContentAsString().contains("\"reason\":\"Not found\""));
    }
}

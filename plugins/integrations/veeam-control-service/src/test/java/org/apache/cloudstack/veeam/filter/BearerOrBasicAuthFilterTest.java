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

package org.apache.cloudstack.veeam.filter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import javax.servlet.FilterChain;

import org.apache.cloudstack.veeam.VeeamControlService;
import org.apache.cloudstack.veeam.utils.JwtUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@RunWith(MockitoJUnitRunner.class)
public class BearerOrBasicAuthFilterTest {

    private static final String SECRET = "very-secret";

    @Mock
    private VeeamControlService veeamControlService;

    @Mock
    private FilterChain chain;

    @Test
    public void testDoFilterRejectsMissingAuthorizationWithJsonPayload() throws Exception {
        final BearerOrBasicAuthFilter filter = new BearerOrBasicAuthFilter(veeamControlService);
        final MockHttpServletRequest request = new MockHttpServletRequest();
        final MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader("Accept", "application/json");

        filter.doFilter(request, response, chain);

        assertEquals(401, response.getStatus());
        assertEquals("application/json; charset=UTF-8", response.getContentType());
        assertNotNull(response.getHeader("WWW-Authenticate"));
        assertTrue(response.getHeader("WWW-Authenticate").contains("error=\"invalid_token\""));
        assertTrue(response.getContentAsString().contains("\"error\":\"invalid_token\""));
        assertTrue(response.getContentAsString().contains("Missing Authorization"));
        verifyNoInteractions(chain);
    }

    @Test
    public void testDoFilterRejectsInvalidBearerToken() throws Exception {
        final BearerOrBasicAuthFilter filter = new BearerOrBasicAuthFilter(veeamControlService);
        final MockHttpServletRequest request = new MockHttpServletRequest();
        final MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader("Accept", "application/json");
        request.addHeader("Authorization", "Bearer not-a-jwt");

        filter.doFilter(request, response, chain);

        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("Invalid or expired token"));
        verifyNoInteractions(chain);
    }

    @Test
    public void testDoFilterAllowsValidBearerTokenWithRequiredScopes() throws Exception {
        final BearerOrBasicAuthFilter filter = new BearerOrBasicAuthFilter(veeamControlService);
        final MockHttpServletRequest request = new MockHttpServletRequest();
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final String token = JwtUtil.issueHs256Jwt("service-user", "ovirt-app-admin ovirt-app-portal", 60L, SECRET);
        request.addHeader("Authorization", "Bearer " + token);
        when(veeamControlService.getHmacSecret()).thenReturn(SECRET);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    public void testDoFilterAllowsValidBasicCredentials() throws Exception {
        final BearerOrBasicAuthFilter filter = new BearerOrBasicAuthFilter(veeamControlService);
        final MockHttpServletRequest request = new MockHttpServletRequest();
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final String credentials = Base64.getEncoder().encodeToString("veeam:secret".getBytes(StandardCharsets.UTF_8));
        request.addHeader("Authorization", "Basic " + credentials);
        when(veeamControlService.validateCredentials("veeam", "secret")).thenReturn(true);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    public void testDoFilterRejectsInvalidBasicCredentialsWithHtmlPayload() throws Exception {
        final BearerOrBasicAuthFilter filter = new BearerOrBasicAuthFilter(veeamControlService);
        final MockHttpServletRequest request = new MockHttpServletRequest();
        final MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader("Authorization", "Basic !!!not-base64!!!");

        filter.doFilter(request, response, chain);

        assertEquals(401, response.getStatus());
        assertEquals("text/html; charset=UTF-8", response.getContentType());
        assertTrue(response.getContentAsString().contains("Unauthorized"));
        assertNotNull(response.getHeader("WWW-Authenticate"));
        assertTrue(response.getHeader("WWW-Authenticate").contains("invalid_client"));
        verifyNoInteractions(chain);
    }
}

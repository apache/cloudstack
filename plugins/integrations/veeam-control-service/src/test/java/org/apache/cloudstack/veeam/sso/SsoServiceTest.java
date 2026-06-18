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

package org.apache.cloudstack.veeam.sso;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;

import org.apache.cloudstack.veeam.VeeamControlService;
import org.apache.cloudstack.veeam.VeeamControlServlet;
import org.apache.cloudstack.veeam.utils.Mapper;
import org.apache.cloudstack.veeam.utils.Negotiation;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.fasterxml.jackson.core.type.TypeReference;

@RunWith(MockitoJUnitRunner.class)
public class SsoServiceTest {

    private final Mapper mapper = new Mapper();

    @Test
    public void testCanHandleSanitizesQueryParameters() {
        final SsoService service = new SsoService();

        assertTrue(service.canHandle("POST", "/sso/oauth/token?scope=abc"));
    }

    @Test
    public void testHandleReturnsNotFoundForUnknownPath() throws Exception {
        final SsoService service = new SsoService();
        service.veeamControlService = mock(VeeamControlService.class);
        final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/sso/unknown");
        final MockHttpServletResponse response = new MockHttpServletResponse();

        service.handle(request, response, "/sso/unknown", Negotiation.OutFormat.JSON,
                new VeeamControlServlet(Collections.emptyList()));

        assertEquals(404, response.getStatus());
        assertTrue(response.getContentAsString().contains("\"reason\":\"Not found\""));
    }

    @Test
    public void testHandleTokenRejectsNonPostMethod() throws Exception {
        final SsoService service = new SsoService();
        service.veeamControlService = mock(VeeamControlService.class);
        final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/sso/oauth/token");
        final MockHttpServletResponse response = new MockHttpServletResponse();

        service.handle(request, response, "/sso/oauth/token", Negotiation.OutFormat.JSON,
                new VeeamControlServlet(Collections.emptyList()));

        assertEquals(405, response.getStatus());
        assertEquals("POST", response.getHeader("Allow"));
        assertTrue(response.getContentAsString().contains("method_not_allowed"));
    }

    @Test
    public void testHandleTokenRejectsMissingGrantType() throws Exception {
        final SsoService service = new SsoService();
        service.veeamControlService = mock(VeeamControlService.class);
        final MockHttpServletRequest request = new MockHttpServletRequest("POST", "/sso/oauth/token");
        final MockHttpServletResponse response = new MockHttpServletResponse();

        service.handle(request, response, "/sso/oauth/token", Negotiation.OutFormat.JSON,
                new VeeamControlServlet(Collections.emptyList()));

        assertEquals(400, response.getStatus());
        assertTrue(response.getContentAsString().contains("Missing parameter: grant_type"));
    }

    @Test
    public void testHandleTokenRejectsUnsupportedGrantType() throws Exception {
        final SsoService service = new SsoService();
        service.veeamControlService = mock(VeeamControlService.class);
        final MockHttpServletRequest request = new MockHttpServletRequest("POST", "/sso/oauth/token");
        final MockHttpServletResponse response = new MockHttpServletResponse();
        request.setParameter("grant_type", "client_credentials");

        service.handle(request, response, "/sso/oauth/token", Negotiation.OutFormat.JSON,
                new VeeamControlServlet(Collections.emptyList()));

        assertEquals(400, response.getStatus());
        assertTrue(response.getContentAsString().contains("unsupported_grant_type"));
    }

    @Test
    public void testHandleTokenRejectsMissingCredentials() throws Exception {
        final SsoService service = new SsoService();
        service.veeamControlService = mock(VeeamControlService.class);
        final MockHttpServletRequest request = new MockHttpServletRequest("POST", "/sso/oauth/token");
        final MockHttpServletResponse response = new MockHttpServletResponse();
        request.setParameter("grant_type", "password");
        request.setParameter("username", "veeam");

        service.handle(request, response, "/sso/oauth/token", Negotiation.OutFormat.JSON,
                new VeeamControlServlet(Collections.emptyList()));

        assertEquals(400, response.getStatus());
        assertTrue(response.getContentAsString().contains("Missing username/password"));
    }

    @Test
    public void testHandleTokenRejectsInvalidCredentials() throws Exception {
        final SsoService service = new SsoService();
        final VeeamControlService controlService = mock(VeeamControlService.class);
        service.veeamControlService = controlService;
        final MockHttpServletRequest request = new MockHttpServletRequest("POST", "/sso/oauth/token");
        final MockHttpServletResponse response = new MockHttpServletResponse();
        request.setParameter("grant_type", "password");
        request.setParameter("username", "veeam");
        request.setParameter("password", "wrong");
        when(controlService.validateCredentials("veeam", "wrong")).thenReturn(false);

        service.handle(request, response, "/sso/oauth/token", Negotiation.OutFormat.JSON,
                new VeeamControlServlet(Collections.emptyList()));

        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("invalid_grant"));
    }

    @Test
    public void testHandleTokenReturnsServerErrorWhenTokenIssuanceFails() throws Exception {
        final SsoService service = new SsoService();
        final VeeamControlService controlService = mock(VeeamControlService.class);
        service.veeamControlService = controlService;
        final MockHttpServletRequest request = new MockHttpServletRequest("POST", "/sso/oauth/token");
        final MockHttpServletResponse response = new MockHttpServletResponse();
        request.setParameter("grant_type", "password");
        request.setParameter("username", "veeam");
        request.setParameter("password", "secret");
        when(controlService.validateCredentials("veeam", "secret")).thenReturn(true);
        when(controlService.getHmacSecret()).thenReturn(null);

        service.handle(request, response, "/sso/oauth/token", Negotiation.OutFormat.JSON,
                new VeeamControlServlet(Collections.emptyList()));

        assertEquals(500, response.getStatus());
        assertTrue(response.getContentAsString().contains("Failed to issue token"));
    }

    @Test
    public void testHandleTokenIssuesTokenWithDefaultScopes() throws Exception {
        final SsoService service = new SsoService();
        final VeeamControlService controlService = mock(VeeamControlService.class);
        service.veeamControlService = controlService;
        final MockHttpServletRequest request = new MockHttpServletRequest("POST", "/sso/oauth/token");
        final MockHttpServletResponse response = new MockHttpServletResponse();
        request.setParameter("grant_type", "password");
        request.setParameter("username", "veeam");
        request.setParameter("password", "secret");
        when(controlService.validateCredentials("veeam", "secret")).thenReturn(true);
        when(controlService.getHmacSecret()).thenReturn("very-secret");

        service.handle(request, response, "/sso/oauth/token", Negotiation.OutFormat.JSON,
                new VeeamControlServlet(Collections.emptyList()));

        assertEquals(200, response.getStatus());

        final Map<String, Object> payload = mapper.jsonMapper().readValue(response.getContentAsByteArray(), new TypeReference<>() {
        });
        assertEquals("bearer", payload.get("token_type"));
        assertEquals(3600, ((Number) payload.get("expires_in")).intValue());
        assertEquals(String.join(" ", SsoService.REQUIRED_SCOPES), payload.get("scope"));

        final String accessToken = (String) payload.get("access_token");
        final String jwtPayload = new String(Base64.getUrlDecoder().decode(accessToken.split("\\.")[1]), StandardCharsets.UTF_8);
        final Map<String, Object> jwtClaims = mapper.jsonMapper().readValue(jwtPayload, new TypeReference<>() {
        });
        assertEquals("veeam", jwtClaims.get("sub"));
        assertEquals(String.join(" ", SsoService.REQUIRED_SCOPES), jwtClaims.get("scope"));
    }

    @Test
    public void testHandleTokenHonorsCustomScope() throws Exception {
        final SsoService service = new SsoService();
        final VeeamControlService controlService = mock(VeeamControlService.class);
        service.veeamControlService = controlService;
        final MockHttpServletRequest request = new MockHttpServletRequest("POST", "/sso/oauth/token");
        final MockHttpServletResponse response = new MockHttpServletResponse();
        request.setParameter("grant_type", "password");
        request.setParameter("username", "veeam");
        request.setParameter("password", "secret");
        request.setParameter("scope", "custom-scope");
        when(controlService.validateCredentials("veeam", "secret")).thenReturn(true);
        when(controlService.getHmacSecret()).thenReturn("very-secret");

        service.handle(request, response, "/sso/oauth/token", Negotiation.OutFormat.JSON,
                new VeeamControlServlet(Collections.emptyList()));

        assertEquals(200, response.getStatus());
        assertTrue(response.getContentAsString().contains("\"scope\":\"custom-scope\""));
    }
}

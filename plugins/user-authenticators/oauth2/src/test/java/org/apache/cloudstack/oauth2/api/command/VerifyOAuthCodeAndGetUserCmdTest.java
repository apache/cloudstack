/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cloudstack.oauth2.api.command;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.auth.PluggableAPIAuthenticator;
import org.apache.cloudstack.oauth2.OAuth2AuthManager;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class VerifyOAuthCodeAndGetUserCmdTest {

    private VerifyOAuthCodeAndGetUserCmd cmd;
    private OAuth2AuthManager oauth2mgr;
    private HttpSession session;
    private InetAddress remoteAddress;
    private StringBuilder auditTrailSb;
    private HttpServletRequest req;
    private HttpServletResponse resp;

    @Before
    public void setUp() {
        cmd = new VerifyOAuthCodeAndGetUserCmd();
        oauth2mgr = mock(OAuth2AuthManager.class);
        session = mock(HttpSession.class);
        remoteAddress = mock(InetAddress.class);
        auditTrailSb = new StringBuilder();
        req = mock(HttpServletRequest.class);
        resp = mock(HttpServletResponse.class);
        cmd._oauth2mgr = oauth2mgr;
    }

    @Test
    public void testAuthenticate() {
        final String[] secretcodeArray = new String[] { "secretcode" };
        final String[] providerArray = new String[] { "provider" };
        final String responseType = "json";

        Map<String, Object[]> params = new HashMap<>();
        params.put("secretcode", secretcodeArray);
        params.put("provider", providerArray);

        when(oauth2mgr.verifyCodeAndFetchEmail("secretcode", "provider")).thenReturn("test@example.com");

        String response = cmd.authenticate("command", params, session, remoteAddress, responseType, auditTrailSb, req, resp);

        Assert.assertNotNull(response);
        Assert.assertTrue(response.contains("test@example.com"));
    }

    @Test(expected = ServerApiException.class)
    public void testAuthenticateWithInvalidCode() throws Exception {
        final String[] secretcodeArray = new String[] { "invalidcode" };
        final String[] providerArray = new String[] { "provider" };
        final String responseType = "json";

        Map<String, Object[]> params = new HashMap<>();
        params.put("secretcode", secretcodeArray);
        params.put("provider", providerArray);

        when(oauth2mgr.verifyCodeAndFetchEmail("invalidcode", "provider")).thenReturn(null);

        cmd.authenticate("command", params, session, remoteAddress, responseType, auditTrailSb, req, resp);
    }

    @Test
    public void testSetAuthenticators() {
        VerifyOAuthCodeAndGetUserCmd cmd = new VerifyOAuthCodeAndGetUserCmd();
        OAuth2AuthManager oauth2mgr = mock(OAuth2AuthManager.class);
        List<PluggableAPIAuthenticator> authenticators = new ArrayList<>();
        authenticators.add(mock(PluggableAPIAuthenticator.class));
        authenticators.add(oauth2mgr);
        authenticators.add(null);
        cmd.setAuthenticators(authenticators);
        Assert.assertEquals(oauth2mgr, cmd._oauth2mgr);
    }
}

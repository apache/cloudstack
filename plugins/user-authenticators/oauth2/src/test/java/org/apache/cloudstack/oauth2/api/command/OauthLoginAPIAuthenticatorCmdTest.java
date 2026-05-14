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

package org.apache.cloudstack.oauth2.api.command;

import com.cloud.api.ApiServer;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.ServerApiException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OauthLoginAPIAuthenticatorCmdTest {
    @InjectMocks
    private OauthLoginAPIAuthenticatorCmd cmd;

    private AutoCloseable closeable;

    @Before
    public void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
    }

    @After
    public void tearDown() throws Exception {
        closeable.close();
    }

    @Test
    public void testGetDomainNameWhenDomainNameIsNull() {
        StringBuilder auditTrailSb = new StringBuilder();
        String[] domainName = null;
        String domain = cmd.getDomainName(auditTrailSb, domainName);
        assertNull(domain);
        assertEquals("", auditTrailSb.toString());
    }

    @Test
    public void testGetDomainNameWithStartingSlash() {
        StringBuilder auditTrailSb = new StringBuilder();
        String[] domainName = {"/example"};
        String domain = cmd.getDomainName(auditTrailSb, domainName);
        assertEquals("/example/", domain);
        assertEquals(" domain=/example", auditTrailSb.toString());
    }

    @Test
    public void testGetDomainNameWithEndingSlash() {
        StringBuilder auditTrailSb = new StringBuilder();
        String[] domainName = {"example/"};
        String domain = cmd.getDomainName(auditTrailSb, domainName);
        assertEquals("/example/", domain);
        assertEquals(" domain=example/", auditTrailSb.toString());
    }

    @Test
    public void testAuthenticateWithMissingParamsReturnsSerializedServerApiException() throws Exception {
        ApiServer apiServer = mock(ApiServer.class);
        cmd._apiServer = apiServer;
        String serializedError = "{\"oauthloginresponse\":{\"errorcode\":531,\"errortext\":\"...\"}}";
        when(apiServer.getSerializedApiError(anyInt(), anyString(), any(), anyString())).thenReturn(serializedError);

        Map<String, Object[]> params = new HashMap<>();
        // missing provider, email, secretCode — should trip the empty check
        params.put(ApiConstants.PROVIDER, new String[]{""});
        params.put(ApiConstants.EMAIL, new String[]{""});
        params.put(ApiConstants.SECRET_CODE, new String[]{""});

        try {
            cmd.authenticate("oauthlogin", params, null, InetAddress.getLoopbackAddress(),
                    "json", new StringBuilder(), null, null);
            fail("Expected ServerApiException to be thrown");
        } catch (ServerApiException ex) {
            assertEquals(ApiErrorCode.ACCOUNT_ERROR, ex.getErrorCode());
            assertEquals(serializedError, ex.getDescription());
        }
    }

    @Test
    public void testGetDomainIdFromParams() {
        StringBuilder auditTrailSb = new StringBuilder();
        String responseType = "json";
        Map<String, Object[]> params = new HashMap<>();
        params.put(ApiConstants.DOMAIN_ID, new String[]{"1234"});
        ApiServer apiServer = mock(ApiServer.class);
        cmd._apiServer = apiServer;
        when(apiServer.fetchDomainId("1234")).thenReturn(5678L);

        Long domainId = cmd.getDomainIdFromParams(params, auditTrailSb, responseType);

        assertEquals(Long.valueOf(5678), domainId);
        assertEquals(" domainid=5678", auditTrailSb.toString());
    }
}

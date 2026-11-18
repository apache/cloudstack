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
package org.apache.cloudstack.service;

import io.netris.ApiClient;
import io.netris.ApiResponse;
import io.netris.api.v1.AuthenticationApi;
import io.netris.api.v1.SitesApi;
import io.netris.api.v1.TenantsApi;
import io.netris.model.GetSiteBody;
import io.netris.model.SitesResponseOK;
import io.netris.model.response.AuthResponse;
import io.netris.model.response.TenantResponse;
import io.netris.model.response.TenantsResponse;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.mockito.Spy;

import java.math.BigDecimal;
import java.util.List;

public class NetrisApiClientImplTest {

    private static final String endpointUrl = "https://my-netris-controller.localdomain";
    private static final String username = "user";
    private static final String password = "password";
    private static final String siteName = "Datacenter-1";
    private static final String adminTenantName = "Admin";
    private static final int siteId = 1;
    private static final int adminTenantId = 1;

    private MockedConstruction<ApiClient> apiClientMockedConstruction;

    @Spy
    @InjectMocks
    private NetrisApiClientImpl client;

    @Before
    public void setUp() {
        GetSiteBody site = Mockito.mock(GetSiteBody.class);
        SitesApi sitesApiMock = Mockito.mock(SitesApi.class);
        Mockito.when(site.getName()).thenReturn(siteName);
        Mockito.when(site.getId()).thenReturn(siteId);
        TenantsApi tenantsApi = Mockito.mock(TenantsApi.class);
        TenantResponse tenant = Mockito.mock(TenantResponse.class);
        Mockito.when(tenant.getName()).thenReturn(adminTenantName);
        Mockito.when(tenant.getId()).thenReturn(new BigDecimal(adminTenantId));

        apiClientMockedConstruction = Mockito.mockConstruction(ApiClient.class, (mock, context) -> {
            SitesResponseOK sitesResponse = Mockito.mock(SitesResponseOK.class);
            Mockito.when(sitesResponse.getData()).thenReturn(List.of(site));
            Mockito.when(sitesApiMock.apiSitesGet()).thenReturn(sitesResponse);
            Mockito.when(mock.getApiStubForMethod(SitesApi.class)).thenReturn(sitesApiMock);
            Mockito.when(mock.getApiStubForMethod(TenantsApi.class)).thenReturn(tenantsApi);
            ApiResponse<TenantsResponse> tenantsResponse = Mockito.mock(ApiResponse.class);
            Mockito.when(tenantsApi.apiTenantsGet()).thenReturn(tenantsResponse);
            TenantsResponse tenantsResponseData = Mockito.mock(TenantsResponse.class);
            Mockito.when(tenantsResponseData.getData()).thenReturn(List.of(tenant));
            Mockito.when(tenantsResponse.getData()).thenReturn(tenantsResponseData);
            AuthenticationApi authenticationApi = Mockito.mock(AuthenticationApi.class);
            Mockito.when(mock.getApiStubForMethod(AuthenticationApi.class)).thenReturn(authenticationApi);
            ApiResponse<AuthResponse> authResponseApiResponse = Mockito.mock(ApiResponse.class);
            Mockito.when(authenticationApi.apiAuthGet()).thenReturn(authResponseApiResponse);
            Mockito.when(authResponseApiResponse.getStatusCode()).thenReturn(200);
        });
        client = new NetrisApiClientImpl(endpointUrl, username, password, siteName, adminTenantName);
    }

    @After
    public void tearDown() {
        apiClientMockedConstruction.close();
    }

    @Test
    public void testConstructor() {
        Assert.assertEquals(siteId, client.siteId);
        Assert.assertEquals(adminTenantId, client.tenantId);
    }

    @Test
    public void testNetrisAuthStatus() {
        Assert.assertTrue(client.isSessionAlive());
    }
}

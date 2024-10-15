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

import com.cloud.utils.exception.CloudRuntimeException;
import io.netris.ApiClient;
import io.netris.ApiException;
import io.netris.ApiResponse;
import io.netris.api.v1.AuthenticationApi;
import io.netris.api.v1.SitesApi;
import io.netris.api.v1.TenantsApi;
import io.netris.api.v2.VpcApi;
import io.netris.model.GetSiteBody;
import io.netris.model.SitesResponseOK;
import io.netris.model.VPCListing;
import io.netris.model.VPCResponseOK;
import io.netris.model.response.AuthResponse;
import io.netris.model.response.TenantResponse;
import io.netris.model.response.TenantsResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class NetrisApiClientImpl implements NetrisApiClient {

    private final Logger logger = LogManager.getLogger(getClass());

    private static ApiClient apiClient;

    public NetrisApiClientImpl(String endpointBaseUrl, String username, String password) {
        try {
            apiClient = new ApiClient(endpointBaseUrl, username, password, 1L);
        } catch (ApiException e) {
            logAndThrowException(String.format("Error creating the Netris API Client for %s", endpointBaseUrl), e);
        }
    }

    protected void logAndThrowException(String prefix, ApiException e) throws CloudRuntimeException {
        String msg = String.format("%s: (%s, %s, %s)", prefix, e.getCode(), e.getMessage(), e.getResponseBody());
        logger.error(msg);
        throw new CloudRuntimeException(msg);
    }

    @Override
    public boolean isSessionAlive() {
        ApiResponse<AuthResponse> response = null;
        try {
            AuthenticationApi api = apiClient.getApiStubForMethod(AuthenticationApi.class);
            response = api.apiAuthGet();
        } catch (ApiException e) {
            logAndThrowException("Error checking the Netris API session is alive", e);
        }
        return response != null && response.getStatusCode() == 200;
    }

    @Override
    public List<GetSiteBody> listSites() {
        SitesResponseOK response = null;
        try {
            SitesApi api = apiClient.getApiStubForMethod(SitesApi.class);
            response = api.apiSitesGet();
        } catch (ApiException e) {
            logAndThrowException("Error listing Netris Sites", e);
        }
        return response != null ? response.getData() : null;
    }

    @Override
    public List<VPCListing> listVPCs() {
        VPCResponseOK response = null;
        try {
            VpcApi api = apiClient.getApiStubForMethod(VpcApi.class);
            response = api.apiV2VpcGet();
        } catch (ApiException e) {
            logAndThrowException("Error listing Netris VPCs", e);
        }
        return response != null ? response.getData() : null;
    }

    @Override
    public List<TenantResponse> listTenants() {
        ApiResponse<TenantsResponse> response = null;
        try {
            TenantsApi api = apiClient.getApiStubForMethod(TenantsApi.class);
            response = api.apiTenantsGet();
        } catch (ApiException e) {
            logAndThrowException("Error listing Netris Tenants", e);
        }
        return (response != null && response.getData() != null) ? response.getData().getData() : null;
    }
}

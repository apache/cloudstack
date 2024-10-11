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
import io.netris.api.v2.VpcApi;
import io.netris.model.AuthSchema;
import io.netris.model.GetSiteBody;
import io.netris.model.SitesResponseOK;
import io.netris.model.VPCListing;
import io.netris.model.VPCResponseOK;
import io.netris.model.response.AuthResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.util.List;

public class NetrisApiClientImpl implements NetrisApiClient {

    private final Logger logger = LogManager.getLogger(getClass());

    private static final ApiClient apiClient = new ApiClient();

    public NetrisApiClientImpl(String endpointBaseUrl, String username, String password) {
        apiClient.setBasePath(endpointBaseUrl);
        authenticate(username, password);
    }

    private void authenticate(String username, String password) {
        AuthSchema authSchema = createAuthSchema(username, password);
        AuthenticationApi authenticationApi = new AuthenticationApi(apiClient);
        try {
            ApiResponse<AuthResponse> authResponse = authenticationApi.apiAuthPost(authSchema);
            if (authResponse.getStatusCode() == 200) {
                String cookie = authResponse.getHeaders().get("Set-Cookie").get(0).split(";")[0];
                apiClient.setApiKey(cookie);
                apiClient.addDefaultHeader("Cookie", cookie);
            } else {
                String msg = String.format("Authentication to the Netris Controller %s failed, please check the credentials provided", apiClient.getBasePath());
                logger.error(msg);
                throw new CloudRuntimeException(msg);
            }
        } catch (ApiException e) {
            String msg = String.format("Error authenticating to the Netris Controller %s: Code %s - Message: %s", apiClient.getBasePath(), e.getCode(), e.getResponseBody());
            logger.error(msg, e);
            throw new CloudRuntimeException(msg);
        }
    }

    private AuthSchema createAuthSchema(String username, String password) {
        AuthSchema authSchema = new AuthSchema();
        authSchema.setUser(username);
        authSchema.setPassword(password);
        authSchema.setAuthSchemeID(new BigDecimal(1));
        return authSchema;
    }

    @Override
    public boolean isSessionAlive() {
        AuthenticationApi api = new AuthenticationApi(apiClient);
        try {
            ApiResponse<AuthResponse> response = api.apiAuthGet();
            return response.getStatusCode() == 200;
        } catch (ApiException e) {
            throw new CloudRuntimeException(e);
        }
    }

    @Override
    public List<GetSiteBody> listSites() {
        SitesApi api = new SitesApi(apiClient);
        try {
            SitesResponseOK response = api.apiSitesGet();
            return response.getData();
        } catch (ApiException e) {
            throw new CloudRuntimeException(e);
        }
    }

    @Override
    public List<VPCListing> listVPCs() {
        VpcApi api = new VpcApi(apiClient);
        try {
            VPCResponseOK response = api.apiV2VpcGet();
            return response.getData();
        } catch (ApiException e) {
            throw new CloudRuntimeException(e);
        }
    }
}

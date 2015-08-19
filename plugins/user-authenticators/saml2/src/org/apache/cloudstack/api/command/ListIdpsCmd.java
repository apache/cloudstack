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
package org.apache.cloudstack.api.command;

import com.cloud.api.response.ApiResponseSerializer;
import com.cloud.user.Account;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.ApiServerService;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.auth.APIAuthenticationType;
import org.apache.cloudstack.api.auth.APIAuthenticator;
import org.apache.cloudstack.api.auth.PluggableAPIAuthenticator;
import org.apache.cloudstack.api.response.IdpResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.saml.SAML2AuthManager;
import org.apache.cloudstack.saml.SAMLProviderMetadata;
import org.apache.log4j.Logger;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@APICommand(name = "listIdps", description = "Returns list of discovered SAML Identity Providers", responseObject = IdpResponse.class, entityType = {})
public class ListIdpsCmd extends BaseCmd implements APIAuthenticator {
    public static final Logger s_logger = Logger.getLogger(ListIdpsCmd.class.getName());
    private static final String s_name = "listidpsresponse";

    @Inject
    ApiServerService _apiServer;

    SAML2AuthManager _samlAuthManager;

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_TYPE_NORMAL;
    }

    @Override
    public void execute() throws ServerApiException {
        // We should never reach here
        throw new ServerApiException(ApiErrorCode.METHOD_NOT_ALLOWED, "This is an authentication api, cannot be used directly");
    }

    @Override
    public String authenticate(String command, Map<String, Object[]> params, HttpSession session, InetAddress remoteAddress, String responseType, StringBuilder auditTrailSb, HttpServletRequest req, HttpServletResponse resp) throws ServerApiException {
        ListResponse<IdpResponse> response = new ListResponse<IdpResponse>();
        List<IdpResponse> idpResponseList = new ArrayList<IdpResponse>();
        for (SAMLProviderMetadata metadata: _samlAuthManager.getAllIdPMetadata()) {
            if (metadata == null) {
                continue;
            }
            IdpResponse idpResponse = new IdpResponse();
            idpResponse.setId(metadata.getEntityId());
            if (metadata.getOrganizationName() == null || metadata.getOrganizationName().isEmpty()) {
                idpResponse.setOrgName(metadata.getEntityId());
            } else {
                idpResponse.setOrgName(metadata.getOrganizationName());
            }
            idpResponse.setOrgUrl(metadata.getOrganizationUrl());
            idpResponse.setObjectName("idp");
            idpResponseList.add(idpResponse);
        }
        response.setResponses(idpResponseList, idpResponseList.size());
        response.setResponseName(getCommandName());
        return ApiResponseSerializer.toSerializedString(response, responseType);
    }

    @Override
    public APIAuthenticationType getAPIType() {
        return APIAuthenticationType.READONLY_API;
    }

    @Override
    public void setAuthenticators(List<PluggableAPIAuthenticator> authenticators) {
        for (PluggableAPIAuthenticator authManager: authenticators) {
            if (authManager != null && authManager instanceof SAML2AuthManager) {
                _samlAuthManager = (SAML2AuthManager) authManager;
            }
        }
        if (_samlAuthManager == null) {
            s_logger.error("No suitable Pluggable Authentication Manager found for SAML2 Login Cmd");
        }
    }
}

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

import java.net.InetAddress;
import java.util.List;
import java.util.Map;

import com.cloud.api.response.ApiResponseSerializer;
import com.cloud.user.Account;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.auth.APIAuthenticationType;
import org.apache.cloudstack.api.auth.APIAuthenticator;
import org.apache.cloudstack.api.auth.PluggableAPIAuthenticator;
import org.apache.cloudstack.api.response.UserResponse;
import org.apache.cloudstack.oauth2.OAuth2AuthManager;
import org.apache.cloudstack.oauth2.api.response.OauthProviderResponse;
import org.apache.commons.lang.ArrayUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@APICommand(name = "verifyOAuthCodeAndGetUser", description = "Verify the OAuth Code and fetch the corresponding user from provider", responseObject = OauthProviderResponse.class, entityType = {},
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false,
        authorized = {RoleType.Admin, RoleType.ResourceAdmin, RoleType.DomainAdmin, RoleType.User}, since = "4.19.0")
public class VerifyOAuthCodeAndGetUserCmd extends BaseListCmd implements APIAuthenticator {

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.PROVIDER, type = CommandType.STRING, description = "Name of the provider", required = true)
    private String provider;

    @Parameter(name = ApiConstants.SECRET_CODE, type = CommandType.STRING, description = "Code that is provided by OAuth provider (Eg. google, github) after successful login")
    private String secretCode;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getProvider() {
        return provider;
    }

    public String getSecretCode() {
        return secretCode;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    protected OAuth2AuthManager _oauth2mgr;

    @Override
    public long getEntityOwnerId() {
        return Account.Type.NORMAL.ordinal();
    }

    @Override
    public void execute() throws ServerApiException {
        // We should never reach here
        throw new ServerApiException(ApiErrorCode.METHOD_NOT_ALLOWED, "This is an authentication api, cannot be used directly");
    }

    @Override
    public String authenticate(String command, Map<String, Object[]> params, HttpSession session, InetAddress remoteAddress, String responseType, StringBuilder auditTrailSb, HttpServletRequest req, HttpServletResponse resp) throws ServerApiException {
        final String[] secretcodeArray = (String[])params.get(ApiConstants.SECRET_CODE);
        final String[] providerArray = (String[])params.get(ApiConstants.PROVIDER);
        if (ArrayUtils.isNotEmpty(secretcodeArray)) {
            secretCode = secretcodeArray[0];
        }
        if (ArrayUtils.isNotEmpty(providerArray)) {
            provider = providerArray[0];
        }

        String email = _oauth2mgr.verifyCodeAndFetchEmail(secretCode, provider);
        if (email != null) {
            UserResponse response = new UserResponse();
            response.setEmail(email);
            response.setResponseName(getCommandName());
            response.setObjectName("oauthemail");

            return ApiResponseSerializer.toSerializedString(response, responseType);
        }

        throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Unable to verify the code provided");
    }

    @Override
    public APIAuthenticationType getAPIType() {
        return null;
    }

    @Override
    public void setAuthenticators(List<PluggableAPIAuthenticator> authenticators) {
        for (PluggableAPIAuthenticator authManager: authenticators) {
            if (authManager != null && authManager instanceof OAuth2AuthManager) {
                _oauth2mgr = (OAuth2AuthManager) authManager;
            }
        }
        if (_oauth2mgr == null) {
            logger.error("No suitable Pluggable Authentication Manager found for listing OAuth providers");
        }
    }
}

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
import java.util.ArrayList;
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
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.auth.UserOAuth2Authenticator;
import org.apache.cloudstack.oauth2.OAuth2AuthManager;
import org.apache.cloudstack.oauth2.api.response.OauthProviderResponse;
import org.apache.cloudstack.oauth2.vo.OauthProviderVO;
import org.apache.commons.lang.ArrayUtils;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@APICommand(name = "listOauthProvider", description = "List OAuth providers registered", responseObject = OauthProviderResponse.class, entityType = {},
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false,
        authorized = {RoleType.Admin, RoleType.ResourceAdmin, RoleType.DomainAdmin, RoleType.User}, since = "4.19.0")
public class ListOAuthProvidersCmd extends BaseListCmd implements APIAuthenticator {
    public static final Logger s_logger = Logger.getLogger(ListOAuthProvidersCmd.class.getName());

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = OauthProviderResponse.class, description = "the ID of the OAuth provider")
    private String id;

    @Parameter(name = ApiConstants.PROVIDER, type = CommandType.STRING, description = "Name of the provider")
    private String provider;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////
    public String getId() {
        return id;
    }

    public String getProvider() {
        return provider;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    OAuth2AuthManager _oauth2mgr;

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
        final String[] idArray = (String[])params.get(ApiConstants.ID);
        final String[] providerArray = (String[])params.get(ApiConstants.PROVIDER);
        if (ArrayUtils.isNotEmpty(idArray)) {
            id = idArray[0];
        }
        if (ArrayUtils.isNotEmpty(providerArray)) {
            provider = providerArray[0];
        }

        List<OauthProviderVO> resultList = _oauth2mgr.listOauthProviders(provider, id);
        List<UserOAuth2Authenticator> userOAuth2AuthenticatorPlugins = _oauth2mgr.listUserOAuth2AuthenticationProviders();
        List<String> authenticatorPluginNames = new ArrayList<>();
        for (UserOAuth2Authenticator authenticator : userOAuth2AuthenticatorPlugins) {
            String name = authenticator.getName();
            authenticatorPluginNames.add(name);
        }
        List<OauthProviderResponse> responses = new ArrayList<>();
        for (OauthProviderVO result : resultList) {
            OauthProviderResponse r = new OauthProviderResponse(result.getUuid(), result.getProvider(),
                    result.getDescription(), result.getClientId(), result.getSecretKey(), result.getRedirectUri());
            if (OAuth2AuthManager.OAuth2IsPluginEnabled.value() && authenticatorPluginNames.contains(result.getProvider()) && result.isEnabled()) {
                r.setEnabled(true);
            } else {
                r.setEnabled(false);
            }
            r.setObjectName(ApiConstants.OAUTH_PROVIDER);
            responses.add(r);
        }

        ListResponse<OauthProviderResponse> response = new ListResponse<>();
        response.setResponses(responses, resultList.size());
        response.setResponseName(getCommandName());
        setResponseObject(response);

        return ApiResponseSerializer.toSerializedString(response, responseType);
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
            s_logger.error("No suitable Pluggable Authentication Manager found for listing OAuth providers");
        }
    }
}

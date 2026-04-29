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

import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.auth.UserOAuth2Authenticator;
import org.apache.cloudstack.oauth2.OAuth2AuthManager;
import org.apache.cloudstack.oauth2.api.response.OauthProviderResponse;
import org.apache.cloudstack.oauth2.vo.OauthProviderVO;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.context.CallContext;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

@APICommand(name = "updateOauthProvider", description = "Updates the registered OAuth provider details", responseObject = OauthProviderResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false, since = "4.19.0")
public final class UpdateOAuthProviderCmd extends BaseCmd {


    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = OauthProviderResponse.class, required = true, description = "id of the OAuth provider to be updated")
    private Long id;

    @Parameter(name = ApiConstants.DESCRIPTION, type = CommandType.STRING, description = "Description of the OAuth Provider")
    private String description;

    @Parameter(name = ApiConstants.CLIENT_ID, type = CommandType.STRING, description = "Client ID pre-registered in the specific OAuth provider")
    private String clientId;

    @Parameter(name = ApiConstants.OAUTH_SECRET_KEY, type = CommandType.STRING, description = "Secret Key pre-registered in the specific OAuth provider")
    private String secretKey;

    @Parameter(name = ApiConstants.REDIRECT_URI, type = CommandType.STRING, description = "Redirect URI pre-registered in the specific OAuth provider")
    private String redirectUri;

    @Parameter(name = ApiConstants.ENABLED, type = CommandType.BOOLEAN, description = "OAuth provider will be enabled or disabled based on this value")
    private Boolean enabled;

    @Inject
    OAuth2AuthManager _oauthMgr;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public String getClientId() {
        return clientId;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public long getEntityOwnerId() {
        return CallContext.current().getCallingAccount().getId();
    }

    @Override
    public Long getApiResourceId() {
        return id;
    }

    @Override
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.User;
    }

    @Override
    public void execute() {
        OauthProviderVO result = _oauthMgr.updateOauthProvider(this);
        if (result != null) {
            OauthProviderResponse r = new OauthProviderResponse(result.getUuid(), result.getProvider(),
                    result.getDescription(), result.getClientId(), result.getSecretKey(), result.getRedirectUri());

            List<UserOAuth2Authenticator> userOAuth2AuthenticatorPlugins = _oauthMgr.listUserOAuth2AuthenticationProviders();
            List<String> authenticatorPluginNames = new ArrayList<>();
            for (UserOAuth2Authenticator authenticator : userOAuth2AuthenticatorPlugins) {
                String name = authenticator.getName();
                authenticatorPluginNames.add(name);
            }
            if (OAuth2AuthManager.OAuth2IsPluginEnabled.value() && authenticatorPluginNames.contains(result.getProvider()) && result.isEnabled()) {
                r.setEnabled(true);
            } else {
                r.setEnabled(false);
            }

            r.setObjectName(ApiConstants.OAUTH_PROVIDER);
            r.setResponseName(getCommandName());
            this.setResponseObject(r);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to update OAuth provider");
        }
    }
}

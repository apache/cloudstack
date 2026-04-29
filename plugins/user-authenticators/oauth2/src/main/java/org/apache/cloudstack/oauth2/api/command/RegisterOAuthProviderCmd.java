//Licensed to the Apache Software Foundation (ASF) under one or more
// contributor license agreements.  See the NOTICE file distributed with
// this work for additional information regarding copyright ownership.
// The ASF licenses this file to You under the Apache License, Version 2.0
// (the "License"); you may not use this file except in compliance with
// the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package org.apache.cloudstack.oauth2.api.command;

import javax.inject.Inject;
import javax.persistence.EntityExistsException;

import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.oauth2.OAuth2AuthManager;
import org.apache.cloudstack.oauth2.api.response.OauthProviderResponse;
import org.apache.cloudstack.oauth2.vo.OauthProviderVO;
import org.apache.commons.collections.MapUtils;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.context.CallContext;

import com.cloud.exception.ConcurrentOperationException;

import java.util.Collection;
import java.util.Map;

@APICommand(name = "registerOauthProvider", responseObject = SuccessResponse.class, description = "Register the OAuth2 provider in CloudStack", since = "4.19.0")
public class RegisterOAuthProviderCmd extends BaseCmd {

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.DESCRIPTION, type = CommandType.STRING, required = true, description = "Description of the OAuth Provider")
    private String description;

    @Parameter(name = ApiConstants.PROVIDER, type = CommandType.STRING, description = "Name of the provider from the list of OAuth providers supported in CloudStack", required = true)
    private String provider;

    @Parameter(name = ApiConstants.CLIENT_ID, type = CommandType.STRING, description = "Client ID pre-registered in the specific OAuth provider", required = true)
    private String clientId;

    @Parameter(name = ApiConstants.OAUTH_SECRET_KEY, type = CommandType.STRING, description = "Secret Key pre-registered in the specific OAuth provider", required = true)
    private String secretKey;

    @Parameter(name = ApiConstants.REDIRECT_URI, type = CommandType.STRING, description = "Redirect URI pre-registered in the specific OAuth provider", required = true)
    private String redirectUri;

    @Parameter(name = ApiConstants.DETAILS, type = CommandType.MAP,
            description = "Any OAuth provider details in key/value pairs using format details[i].keyname=keyvalue. Example: details[0].clientsecret=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")
    protected Map details;

    @Override
    public long getEntityOwnerId() {
        return CallContext.current().getCallingAccount().getId();
    }

    public String getDescription() {
        return description;
    }

    public String getProvider() {
        return provider;
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

    public Map getDetails() {
        if (MapUtils.isEmpty(details)) {
            return null;
        }
        Collection paramsCollection = this.details.values();
        return (Map) (paramsCollection.toArray())[0];
    }

    @Inject
    OAuth2AuthManager _oauth2mgr;

    @Override
    public void execute() throws ServerApiException, ConcurrentOperationException, EntityExistsException {
        OauthProviderVO provider = _oauth2mgr.registerOauthProvider(this);

        OauthProviderResponse response = new OauthProviderResponse(provider.getUuid(), provider.getProvider(),
                provider.getDescription(), provider.getClientId(), provider.getSecretKey(), provider.getRedirectUri());
        response.setResponseName(getCommandName());
        response.setObjectName(ApiConstants.OAUTH_PROVIDER);
        setResponseObject(response);
    }
}

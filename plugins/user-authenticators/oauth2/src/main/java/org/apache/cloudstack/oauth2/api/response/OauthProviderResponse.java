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
package org.apache.cloudstack.oauth2.api.response;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;
import org.apache.cloudstack.oauth2.vo.OauthProviderVO;

@EntityReference(value = OauthProviderVO.class)
public class OauthProviderResponse extends BaseResponse {

    @SerializedName(ApiConstants.ID)
    @Param(description = "ID of the provider")
    private String id;

    @SerializedName(ApiConstants.PROVIDER)
    @Param(description = "Name of the provider")
    private String provider;

    @SerializedName(ApiConstants.NAME)
    @Param(description = "Name of the provider")
    private String name;

    @SerializedName(ApiConstants.DESCRIPTION)
    @Param(description = "Description of the provider registered")
    private String description;

    @SerializedName(ApiConstants.CLIENT_ID)
    @Param(description = "Client ID registered in the OAuth provider")
    private String clientId;

    @SerializedName(ApiConstants.OAUTH_SECRET_KEY)
    @Param(description = "Secret key registered in the OAuth provider")
    private String secretKey;

    @SerializedName(ApiConstants.REDIRECT_URI)
    @Param(description = "Redirect URI registered in the OAuth provider")
    private String redirectUri;

    @SerializedName(ApiConstants.ENABLED)
    @Param(description = "Whether the OAuth provider is enabled or not")
    private boolean enabled;

    public OauthProviderResponse(String id, String provider, String description, String clientId, String secretKey, String redirectUri) {
        this.id = id;
        this.provider = provider;
        this.name = provider;
        this.description = description;
        this.clientId = clientId;
        this.secretKey = secretKey;
        this.redirectUri =  redirectUri;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getDescription() {
        return description;
    }


    public void setDescription(String description) {
        this.description = description;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }
}

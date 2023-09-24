package org.apache.cloudstack.oauth2.api.response;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;
import org.apache.cloudstack.api.response.AuthenticationCmdResponse;

public class OauthProviderResponse extends AuthenticationCmdResponse {
    @SerializedName("id")
    @Param(description = "The Oauth Provider ID")
    private String id;

    @SerializedName("providerName")
    @Param(description = "Oauth Provider name")
    private String providerName;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }
}

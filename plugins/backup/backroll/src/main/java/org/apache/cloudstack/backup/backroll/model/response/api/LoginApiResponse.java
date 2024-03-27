package org.apache.cloudstack.backup.backroll.model.response.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public class LoginApiResponse {
    @JsonProperty("access_token")
    public String accessToken;

    @JsonProperty("expires_in")
    public int expiresIn;

    @JsonProperty("refresh_expires_in")
    public String refreshExpiresIn;

    @JsonProperty("token_type")
    public String tokenType;

    @JsonProperty("not-before-policy")
    public String notBeforePolicy;

    @JsonProperty("scope")
    public String scope;
}

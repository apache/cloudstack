package com.cloud.api.response;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class RegisterResponse extends BaseResponse {
    @SerializedName("apikey") @Param(description="the api key of the registered user")
    private String apiKey;

    @SerializedName("secretkey") @Param(description="the secret key of the registered user")
    private String secretKey;

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }
}

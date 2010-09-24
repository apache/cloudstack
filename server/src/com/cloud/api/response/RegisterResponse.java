package com.cloud.api.response;

import com.google.gson.annotations.SerializedName;

public class RegisterResponse extends BaseResponse {
    @SerializedName("apikey")
    private String apiKey;

    @SerializedName("secretkey")
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

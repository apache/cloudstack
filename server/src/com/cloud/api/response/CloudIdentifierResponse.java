package com.cloud.api.response;

import com.google.gson.annotations.SerializedName;

public class CloudIdentifierResponse extends BaseResponse {
	
    @SerializedName("userid")
    private Long userId;
    
    @SerializedName("cloudidentifier")
    private String cloudIdentifier;
    
    @SerializedName("signature")
    private String signature;
    
    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public String getCloudIdentifier() {
        return cloudIdentifier;
    }

    public void setCloudIdentifier(String cloudIdentifier) {
        this.cloudIdentifier = cloudIdentifier;
    }
    
    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

}

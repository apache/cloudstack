package com.cloud.api.response;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class CloudIdentifierResponse extends BaseResponse {
	
    @SerializedName("userid") @Param(description="the user ID for the cloud identifier")
    private Long userId;
    
    @SerializedName("cloudidentifier") @Param(description="the cloud identifier")
    private String cloudIdentifier;
    
    @SerializedName("signature") @Param(description="the signed response for the cloud identifier")
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

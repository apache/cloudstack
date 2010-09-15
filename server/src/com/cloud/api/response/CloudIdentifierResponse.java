package com.cloud.api.response;

import com.cloud.api.ResponseObject;
import com.cloud.serializer.Param;

public class CloudIdentifierResponse implements ResponseObject{
	
    @Param(name="userid")
    private Long userId;
    
    @Param(name="cloudidentifier")
    private String cloudIdentifier;
    
    @Param(name="signature")
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

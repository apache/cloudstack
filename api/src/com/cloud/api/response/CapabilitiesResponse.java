package com.cloud.api.response;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class CapabilitiesResponse extends BaseResponse {
    @SerializedName("securitygroupsenabled") @Param(description="true if security groups support is enabled, false otherwise")
    private boolean securityGroupsEnabled;

    @SerializedName("cloudstackversion") @Param(description="version of the cloud stack")
    private String cloudStackVersion;
    
    @SerializedName("userpublictemplateenabled") @Param(description="true if user and domain admins can set templates to be shared, false otherwise")
    private boolean userPublicTemplateEnabled;

    public boolean getSecurityGroupsEnabled() {
        return securityGroupsEnabled;
    }

    public void setSecurityGroupsEnabled(boolean securityGroupsEnabled) {
        this.securityGroupsEnabled = securityGroupsEnabled;
    }

    public String getCloudStackVersion() {
        return cloudStackVersion;
    }

    public void setCloudStackVersion(String cloudStackVersion) {
        this.cloudStackVersion = cloudStackVersion;
    }
    
    public boolean getUserPublicTemplateEnabled() {
        return securityGroupsEnabled;
    }

    public void setUserPublicTemplateEnabled(boolean userPublicTemplateEnabled) {
        this.userPublicTemplateEnabled = userPublicTemplateEnabled;
    }
}

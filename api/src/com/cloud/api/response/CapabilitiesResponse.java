package com.cloud.api.response;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class CapabilitiesResponse extends BaseResponse {
    @SerializedName("securitygroupsenabled") @Param(description="true if security groups support is enabled, false otherwise")
    private String securityGroupsEnabled;

    @SerializedName("cloudstackversion") @Param(description="version of the cloud stack")
    private String cloudStackVersion;

    public String getSecurityGroupsEnabled() {
        return securityGroupsEnabled;
    }

    public void setSecurityGroupsEnabled(String securityGroupsEnabled) {
        this.securityGroupsEnabled = securityGroupsEnabled;
    }

    public String getCloudStackVersion() {
        return cloudStackVersion;
    }

    public void setCloudStackVersion(String cloudStackVersion) {
        this.cloudStackVersion = cloudStackVersion;
    }
}

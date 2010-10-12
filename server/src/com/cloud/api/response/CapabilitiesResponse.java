package com.cloud.api.response;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class CapabilitiesResponse extends BaseResponse {
    @SerializedName("networkgroupsenabled") @Param(description="true if network groups support is enabled, false otherwise")
    private String networkGroupsEnabled;

    @SerializedName("cloudstackversion") @Param(description="version of the cloud stack")
    private String cloudStackVersion;

    public String getNetworkGroupsEnabled() {
        return networkGroupsEnabled;
    }

    public void setNetworkGroupsEnabled(String networkGroupsEnabled) {
        this.networkGroupsEnabled = networkGroupsEnabled;
    }

    public String getCloudStackVersion() {
        return cloudStackVersion;
    }

    public void setCloudStackVersion(String cloudStackVersion) {
        this.cloudStackVersion = cloudStackVersion;
    }
}

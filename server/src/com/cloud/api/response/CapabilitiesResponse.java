package com.cloud.api.response;

import com.google.gson.annotations.SerializedName;

public class CapabilitiesResponse extends BaseResponse {
    @SerializedName("networkgroupsenabled")
    private String networkGroupsEnabled;

    @SerializedName("cloudstackversion")
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

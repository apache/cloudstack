package com.cloud.ucs.manager;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class ListUcsProfileResponse extends BaseResponse {
    @SerializedName(ApiConstants.UCS_DN) @Param(description="the dn of ucs profile")
    private String dn;

    public String getDn() {
        return dn;
    }

    public void setDn(String dn) {
        this.dn = dn;
    }
}

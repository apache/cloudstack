package org.apache.cloudstack.api.response;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class VMUserDataResponse extends BaseResponse {
    @SerializedName(ApiConstants.VIRTUAL_MACHINE_ID)
    @Param(description = "the ID of the virtual machine")
    private String vmId;

    @SerializedName(ApiConstants.USER_DATA)
    @Param(description = "Base 64 encoded VM user data")
    private String userData;

    public void setUserData(String userData) {
        this.userData = userData;
    }

    public void setVmId(String vmId) {
        this.vmId = vmId;
    }

}

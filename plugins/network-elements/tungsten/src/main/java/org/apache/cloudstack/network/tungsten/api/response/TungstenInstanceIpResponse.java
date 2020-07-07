package org.apache.cloudstack.network.tungsten.api.response;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;

public class TungstenInstanceIpResponse extends BaseResponse {

    @SerializedName(ApiConstants.NAME)
    @Param(description = "name of the tungsten instance ip")
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}

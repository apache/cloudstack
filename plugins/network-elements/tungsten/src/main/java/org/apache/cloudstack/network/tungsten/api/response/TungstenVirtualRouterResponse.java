package org.apache.cloudstack.network.tungsten.api.response;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;

public class TungstenVirtualRouterResponse extends BaseResponse {

    @SerializedName(ApiConstants.UUID)
    @Param(description = "UUID of the tungsten virtual machine")
    private String uuid;
    @SerializedName(ApiConstants.NAME)
    @Param(description = "name of the tungsten virtual machine")
    private String name;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}

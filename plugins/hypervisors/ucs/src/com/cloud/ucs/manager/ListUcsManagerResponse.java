package com.cloud.ucs.manager;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class ListUcsManagerResponse extends BaseResponse {
    @SerializedName(ApiConstants.ID) @Param(description="id of ucs manager")
    private String id;
    
    @SerializedName(ApiConstants.NAME) @Param(description="name of ucs manager")
    private String name;
    
    @SerializedName(ApiConstants.ZONE_ID) @Param(description="zone id the ucs manager belongs to")
    private String zoneId;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getZoneId() {
        return zoneId;
    }

    public void setZoneId(String zoneId) {
        this.zoneId = zoneId;
    }
}

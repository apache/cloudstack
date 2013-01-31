package com.cloud.ucs.manager;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class AddUcsManagerResponse extends BaseResponse {
    @SerializedName(ApiConstants.ID) @Param(description="the ID of the ucs manager")
    private String id;
    
    @SerializedName(ApiConstants.NAME) @Param(description="the name of ucs manager")
    private String name;
    
    @SerializedName(ApiConstants.URL) @Param(description="the url of ucs manager")
    private String url;
    
    @SerializedName(ApiConstants.ZONE_ID) @Param(description="the zone ID of ucs manager")
    private String zoneId;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
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

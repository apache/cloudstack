package com.cloud.baremetal.networkservice;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;

/**
 * Created by frank on 5/8/14.
 */
public class BaremetalRctResponse extends BaseResponse {
    @SerializedName(ApiConstants.ID)
    @Param(description = "id of rct")
    private String id;

    @SerializedName(ApiConstants.URL)
    @Param(description = "url")
    private String url;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}

package com.cloud.api.response;

import com.google.gson.annotations.SerializedName;

public class StatusResponse extends BaseResponse {
    @SerializedName("status")
    private Boolean status;
    
    public Boolean getStatus() {
       return status;
    }

   public void setStatus(Boolean status) {
       this.status = status;
   }
}

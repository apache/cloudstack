package com.cloud.server.api.response.netapp;

import com.cloud.api.ApiConstants;
import com.cloud.api.response.BaseResponse;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class AssociateLunCmdResponse extends BaseResponse {
    
    @SerializedName(ApiConstants.ID) @Param(description="the LUN id")
    private String lun;
    
    @SerializedName(ApiConstants.IP_ADDRESS) @Param(description="the IP address of ")
    private String ipAddress;
    
    @SerializedName(ApiConstants.TARGET_IQN) @Param(description="the target IQN")
    private String targetIQN;
    
    public String getLun() {
    	return lun;
    }
    
    public String getIpAddress() {
    	return ipAddress;
    }
    
    public String getTargetIQN() {
    	return targetIQN;
    }
    
    
    public void setLun(String lun) {
    	this.lun = lun;
    }
    
    public void setIpAddress(String ipAddress) {
    	this.ipAddress = ipAddress;
    }
    
    public void setTargetIQN(String targetIQN) {
    	this.targetIQN = targetIQN;
    }
    
}

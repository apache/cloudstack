package com.cloud.server.api.response.netapp;

import com.cloud.api.ApiConstants;
import com.cloud.api.response.BaseResponse;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class CreateLunCmdResponse  extends BaseResponse {
	 
	 @SerializedName(ApiConstants.PATH) @Param(description="pool path")
	    private String path;
	 
	 @SerializedName(ApiConstants.IQN) @Param(description="iqn")
	    private String iqn;
	 
	 @SerializedName(ApiConstants.IP_ADDRESS) @Param(description="ip address")
	    private String ipAddress;
	 
	 
	 public String getPath() {
		 return path;
	 }
	 
	 public String getIqn() {
		 return iqn;
	 }
	 
	 public String getIpAddress() {
		 return ipAddress;
	 }
	 
	 public void setPath(String path) {
		 this.path = path;
	 }
	 
	 public void setIqn(String iqn) {
		 this.iqn = iqn;
	 }
	 
	 public void setIpAddress(String ipAddress) {
		 this.ipAddress = ipAddress;
	 }
}

/**
 *  Copyright (C) 2011 Cloud.com, Inc.  All rights reserved.
 */

package com.cloud.server.api.response;

import com.cloud.api.ApiConstants;
import com.cloud.api.response.BaseResponse;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class ExternalLoadBalancerResponse extends BaseResponse {

	@SerializedName(ApiConstants.ID) @Param(description="the ID of the external load balancer")
    private Long id;
    
    @SerializedName(ApiConstants.ZONE_ID) @Param(description="the zone ID of the external load balancer")
    private Long zoneId;
    
    @SerializedName(ApiConstants.IP_ADDRESS) @Param(description="the management IP address of the external load balancer")
    private String ipAddress;
    
    @SerializedName(ApiConstants.USERNAME) @Param(description="the username that's used to log in to the external load balancer")
    private String username;
    
    @SerializedName(ApiConstants.PUBLIC_INTERFACE) @Param(description="the public interface of the external load balancer")
    private String publicInterface;
    
    @SerializedName(ApiConstants.PRIVATE_INTERFACE) @Param(description="the private interface of the external load balancer")
    private String privateInterface;
    
    @SerializedName(ApiConstants.NUM_RETRIES) @Param(description="the number of times to retry requests to the external load balancer")
    private String numRetries;
	
	public Long getId() {
    	return id;
    }
    
    public void setId(Long id) {
    	this.id = id;
    }
    
    public Long getZoneId() {
    	return zoneId;
    }
    
    public void setZoneId(Long zoneId) {
    	this.zoneId = zoneId;
    }
    
    public String getIpAddress() {
    	return ipAddress;
    }
    
    public void setIpAddress(String ipAddress) {
    	this.ipAddress = ipAddress;
    }
    
    public String getUsername() {
    	return username;
    }
    
    public void setUsername(String username) {
    	this.username = username;
    }
    
    public String getPublicInterface() {
    	return publicInterface;
    }
    
    public void setPublicInterface(String publicInterface) {
    	this.publicInterface = publicInterface;
    }
    
    public String getPrivateInterface() {
    	return privateInterface;
    }
    
    public void setPrivateInterface(String privateInterface) {
    	this.privateInterface = privateInterface;
    }
    
    public String getNumRetries() {
    	return numRetries;
    }
    
    public void setNumRetries(String numRetries) {
    	this.numRetries = numRetries;
    }

}

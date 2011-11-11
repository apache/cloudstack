/**
 * *  Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved
*
 *
 * This software is licensed under the GNU General Public License v3 or later.
 *
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.cloud.server.api.response;

import com.cloud.api.ApiConstants;
import com.cloud.api.IdentityProxy;
import com.cloud.api.response.BaseResponse;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class ExternalLoadBalancerResponse extends NetworkDeviceResponse {

	@SerializedName(ApiConstants.ID) @Param(description="the ID of the external load balancer")
    private IdentityProxy id = new IdentityProxy("host");
    
    @SerializedName(ApiConstants.ZONE_ID) @Param(description="the zone ID of the external load balancer")
    private IdentityProxy zoneId = new IdentityProxy("data_center");
    
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
    	return id.getValue();
    }
    
    public void setId(Long id) {
    	this.id.setValue(id);
    }
    
    public Long getZoneId() {
    	return zoneId.getValue();
    }
    
    public void setZoneId(Long zoneId) {
    	this.zoneId.setValue(zoneId);
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

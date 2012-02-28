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
import com.cloud.utils.IdentityProxy;
import com.cloud.api.response.BaseResponse;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class ExternalFirewallResponse extends NetworkDeviceResponse {

    @SerializedName(ApiConstants.ID) @Param(description="the ID of the external firewall")
    private IdentityProxy id = new IdentityProxy("host");
    
    @SerializedName(ApiConstants.ZONE_ID) @Param(description="the zone ID of the external firewall")
    private IdentityProxy zoneId = new IdentityProxy("data_center");
    
    @SerializedName(ApiConstants.IP_ADDRESS) @Param(description="the management IP address of the external firewall")
    private String ipAddress;
    
    @SerializedName(ApiConstants.USERNAME) @Param(description="the username that's used to log in to the external firewall")
    private String username;
    
    @SerializedName(ApiConstants.PUBLIC_INTERFACE) @Param(description="the public interface of the external firewall")
    private String publicInterface;
    
    @SerializedName(ApiConstants.USAGE_INTERFACE) @Param(description="the usage interface of the external firewall")
    private String usageInterface;
    
    @SerializedName(ApiConstants.PRIVATE_INTERFACE) @Param(description="the private interface of the external firewall")
    private String privateInterface;
    
    @SerializedName(ApiConstants.PUBLIC_ZONE) @Param(description="the public security zone of the external firewall")
    private String publicZone;
    
    @SerializedName(ApiConstants.PRIVATE_ZONE) @Param(description="the private security zone of the external firewall")
    private String privateZone;
    
    @SerializedName(ApiConstants.NUM_RETRIES) @Param(description="the number of times to retry requests to the external firewall")
    private String numRetries;
    
    @SerializedName(ApiConstants.TIMEOUT) @Param(description="the timeout (in seconds) for requests to the external firewall")
    private String timeout;
    
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
    
    public String getUsageInterface() {
    	return usageInterface;
    }
    
    public void setUsageInterface(String usageInterface) {
    	this.usageInterface = usageInterface;
    }
    
    public String getPrivateInterface() {
    	return privateInterface;
    }
    
    public void setPrivateInterface(String privateInterface) {
    	this.privateInterface = privateInterface;
    }
    
    public String getPublicZone() {
    	return publicZone;
    }
    
    public void setPublicZone(String publicZone) {
    	this.publicZone = publicZone;
    }
    
    public String getPrivateZone() {
    	return privateZone;
    }
    
    public void setPrivateZone(String privateZone) {
    	this.privateZone = privateZone;
    }
    
    public String getNumRetries() {
    	return numRetries;
    }
    
    public void setNumRetries(String numRetries) {
    	this.numRetries = numRetries;
    }
    
    public String getTimeout() {
    	return timeout;
    }
    
    public void setTimeout(String timeout) {
    	this.timeout = timeout;
    }
}

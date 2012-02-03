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

public class TrafficMonitorResponse extends BaseResponse {

    @SerializedName(ApiConstants.ID) @Param(description="the ID of the external firewall")
    private IdentityProxy id = new IdentityProxy("host");
    
    @SerializedName(ApiConstants.ZONE_ID) @Param(description="the zone ID of the external firewall")
    private IdentityProxy zoneId = new IdentityProxy("data_center");
    
    @SerializedName(ApiConstants.IP_ADDRESS) @Param(description="the management IP address of the external firewall")
    private String ipAddress;
    
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

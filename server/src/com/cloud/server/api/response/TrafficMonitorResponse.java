/**
 *  Copyright (C) 2011 Cloud.com, Inc.  All rights reserved.
 */

package com.cloud.server.api.response;

import com.cloud.api.ApiConstants;
import com.cloud.api.response.BaseResponse;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class TrafficMonitorResponse extends BaseResponse {

    @SerializedName(ApiConstants.ID) @Param(description="the ID of the external firewall")
    private Long id;
    
    @SerializedName(ApiConstants.ZONE_ID) @Param(description="the zone ID of the external firewall")
    private Long zoneId;
    
    @SerializedName(ApiConstants.IP_ADDRESS) @Param(description="the management IP address of the external firewall")
    private String ipAddress;
    
    @SerializedName(ApiConstants.NUM_RETRIES) @Param(description="the number of times to retry requests to the external firewall")
    private String numRetries;
    
    @SerializedName(ApiConstants.TIMEOUT) @Param(description="the timeout (in seconds) for requests to the external firewall")
    private String timeout;
    
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

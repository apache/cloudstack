// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package org.apache.cloudstack.api.response;

import org.apache.cloudstack.api.ApiConstants;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class ExternalFirewallResponse extends NetworkDeviceResponse {

    @SerializedName(ApiConstants.ID) @Param(description="the ID of the external firewall")
    private String id;

    @SerializedName(ApiConstants.ZONE_ID) @Param(description="the zone ID of the external firewall")
    private String zoneId;

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

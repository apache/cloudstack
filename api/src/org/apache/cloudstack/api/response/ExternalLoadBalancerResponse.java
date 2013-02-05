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

public class ExternalLoadBalancerResponse extends NetworkDeviceResponse {

	@SerializedName(ApiConstants.ID) @Param(description="the ID of the external load balancer")
    private String id;

    @SerializedName(ApiConstants.ZONE_ID) @Param(description="the zone ID of the external load balancer")
    private String zoneId;

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

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
package com.cloud.api.response;

import com.google.gson.annotations.SerializedName;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;

import com.cloud.network.dao.ExternalFirewallDeviceVO;
import com.cloud.serializer.Param;

@EntityReference(value = ExternalFirewallDeviceVO.class)
@SuppressWarnings("unused")
public class PaloAltoFirewallResponse extends BaseResponse {

    @SerializedName(ApiConstants.FIREWALL_DEVICE_ID)
    @Param(description = "device id of the Palo Alto firewall")
    private String id;

    @SerializedName(ApiConstants.PHYSICAL_NETWORK_ID)
    @Param(description = "the physical network to which this Palo Alto firewall belongs to")
    private String physicalNetworkId;

    @SerializedName(ApiConstants.PROVIDER)
    @Param(description = "name of the provider")
    private String providerName;

    @SerializedName(ApiConstants.FIREWALL_DEVICE_NAME)
    @Param(description = "device name")
    private String deviceName;

    @SerializedName(ApiConstants.FIREWALL_DEVICE_STATE)
    @Param(description = "device state")
    private String deviceState;

    @SerializedName(ApiConstants.FIREWALL_DEVICE_CAPACITY)
    @Param(description = "device capacity")
    private Long deviceCapacity;

    @SerializedName(ApiConstants.ZONE_ID)
    @Param(description = "the zone ID of the external firewall")
    private String zoneId;

    @SerializedName(ApiConstants.IP_ADDRESS)
    @Param(description = "the management IP address of the external firewall")
    private String ipAddress;

    @SerializedName(ApiConstants.USERNAME)
    @Param(description = "the username that's used to log in to the external firewall")
    private String username;

    @SerializedName(ApiConstants.PUBLIC_INTERFACE)
    @Param(description = "the public interface of the external firewall")
    private String publicInterface;

    @SerializedName(ApiConstants.USAGE_INTERFACE)
    @Param(description = "the usage interface of the external firewall")
    private String usageInterface;

    @SerializedName(ApiConstants.PRIVATE_INTERFACE)
    @Param(description = "the private interface of the external firewall")
    private String privateInterface;

    @SerializedName(ApiConstants.PUBLIC_ZONE)
    @Param(description = "the public security zone of the external firewall")
    private String publicZone;

    @SerializedName(ApiConstants.PRIVATE_ZONE)
    @Param(description = "the private security zone of the external firewall")
    private String privateZone;

    @SerializedName(ApiConstants.NUM_RETRIES)
    @Param(description = "the number of times to retry requests to the external firewall")
    private String numRetries;

    @SerializedName(ApiConstants.TIMEOUT)
    @Param(description = "the timeout (in seconds) for requests to the external firewall")
    private String timeout;

    public void setId(String lbDeviceId) {
        this.id = lbDeviceId;
    }

    public void setPhysicalNetworkId(String physicalNetworkId) {
        this.physicalNetworkId = physicalNetworkId;
    }

    public void setProvider(String provider) {
        this.providerName = provider;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public void setDeviceCapacity(long deviceCapacity) {
        this.deviceCapacity = deviceCapacity;
    }

    public void setDeviceState(String deviceState) {
        this.deviceState = deviceState;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public void setPublicInterface(String publicInterface) {
        this.publicInterface = publicInterface;
    }

    public void setUsageInterface(String usageInterface) {
        this.usageInterface = usageInterface;
    }

    public void setPrivateInterface(String privateInterface) {
        this.privateInterface = privateInterface;
    }

    public void setPublicZone(String publicZone) {
        this.publicZone = publicZone;
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

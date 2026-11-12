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
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

@EntityReference(value = NicNetworkMapResponse.class)
@SuppressWarnings("unused")
public class NicNetworkMapResponse extends BaseResponse {

    @SerializedName(ApiConstants.NETWORK_ID)
    @Param(description = "The ID of the associated network")
    private String networkId;

    @SerializedName(ApiConstants.NETWORK_NAME)
    @Param(description = "The name of the associated network")
    private String networkName;

    @SerializedName(ApiConstants.BROADCAST_URI)
    @Param(description = "The broadcast URI of the associated network")
    private String broadcastUri;

    @SerializedName(ApiConstants.GATEWAY)
    @Param(description = "The gateway of the associated network")
    private String gateway;

    @SerializedName(ApiConstants.NETMASK)
    @Param(description = "The netmask of the associated network")
    private String netmask;

    @SerializedName(ApiConstants.IP6_GATEWAY)
    @Param(description = "The IPv6 gateway of the associated network")
    private String ip6Gateway;

    @SerializedName(ApiConstants.IP6_CIDR)
    @Param(description = "The IPv6 CIDR of the associated network")
    private String ip6Cidr;

    @SerializedName(ApiConstants.IP_ADDRESS)
    @Param(description = "The IPv4 address on the associated network")
    private String ipAddress;

    @SerializedName(ApiConstants.IP6_ADDRESS)
    @Param(description = "The IPv6 address on the associated network")
    private String ip6Address;

    public String getNetworkId() {
        return networkId;
    }

    public void setNetworkId(String networkId) {
        this.networkId = networkId;
    }

    public String getNetworkName() {
        return networkName;
    }

    public void setNetworkName(String networkName) {
        this.networkName = networkName;
    }

    public String getBroadcastUri() {
        return broadcastUri;
    }

    public void setBroadcastUri(String broadcastUri) {
        this.broadcastUri = broadcastUri;
    }

    public String getGateway() {
        return gateway;
    }

    public void setGateway(String gateway) {
        this.gateway = gateway;
    }

    public String getNetmask() {
        return netmask;
    }

    public void setNetmask(String netmask) {
        this.netmask = netmask;
    }

    public String getIp6Gateway() {
        return ip6Gateway;
    }

    public void setIp6Gateway(String ip6Gateway) {
        this.ip6Gateway = ip6Gateway;
    }

    public String getIp6Cidr() {
        return ip6Cidr;
    }

    public void setIp6Cidr(String ip6Cidr) {
        this.ip6Cidr = ip6Cidr;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getIp6Address() {
        return ip6Address;
    }

    public void setIp6Address(String ip6Address) {
        this.ip6Address = ip6Address;
    }
}

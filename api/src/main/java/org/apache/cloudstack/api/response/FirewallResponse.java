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

import java.util.List;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

@SuppressWarnings("unused")
public class FirewallResponse extends BaseResponse {
    @SerializedName(ApiConstants.ID)
    @Param(description = "the ID of the firewall rule")
    private String id;

    @SerializedName(ApiConstants.PROTOCOL)
    @Param(description = "the protocol of the firewall rule")
    private String protocol;

    @SerializedName(ApiConstants.START_PORT)
    @Param(description = "the starting port of firewall rule's port range")
    private Integer startPort;

    @SerializedName(ApiConstants.END_PORT)
    @Param(description = "the ending port of firewall rule's port range")
    private Integer endPort;

    @SerializedName(ApiConstants.IP_ADDRESS_ID)
    @Param(description = "the public ip address id for the firewall rule")
    private String publicIpAddressId;

    @SerializedName(ApiConstants.NETWORK_ID)
    @Param(description = "the network id of the firewall rule")
    private String networkId;

    @SerializedName(ApiConstants.IP_ADDRESS)
    @Param(description = "the public ip address for the firewall rule")
    private String publicIpAddress;

    @SerializedName(ApiConstants.STATE)
    @Param(description = "the state of the rule")
    private String state;

    @SerializedName(ApiConstants.CIDR_LIST)
    @Param(description = "the cidr list to forward traffic from. Multiple entries are separated by a single comma character (,).")
    private String cidrList;

    @SerializedName(ApiConstants.ICMP_TYPE)
    @Param(description = "type of the icmp message being sent")
    private Integer icmpType;

    @SerializedName(ApiConstants.ICMP_CODE)
    @Param(description = "error code for this icmp message")
    private Integer icmpCode;

    @SerializedName(ApiConstants.TAGS)
    @Param(description = "the list of resource tags associated with the rule", responseObject = ResourceTagResponse.class)
    private List<ResourceTagResponse> tags;

    @SerializedName(ApiConstants.FOR_DISPLAY)
    @Param(description = "is rule for display to the regular user", since = "4.4", authorized = {RoleType.Admin})
    private Boolean forDisplay;

    @SerializedName(ApiConstants.DEST_CIDR_LIST)
    @Param(description = "the cidr list to forward traffic to. Multiple entries are separated by a single comma character (,).")
    private String destCidr;

    @SerializedName(ApiConstants.TRAFFIC_TYPE)
    @Param(description = "the traffic type for the firewall rule", since = "4.17.0")
    private String trafficType;

    public void setId(String id) {
        this.id = id;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public void setStartPort(Integer startPort) {
        this.startPort = startPort;
    }

    public void setEndPort(Integer endPort) {
        this.endPort = endPort;
    }

    public void setPublicIpAddressId(String publicIpAddressId) {
        this.publicIpAddressId = publicIpAddressId;
    }

    public void setPublicIpAddress(String publicIpAddress) {
        this.publicIpAddress = publicIpAddress;
    }

    public void setNetworkId(String networkId) {
        this.networkId = networkId;
    }

    public void setState(String state) {
        this.state = state;
    }

    public void setCidrList(String cidrList) {
        this.cidrList = cidrList;
    }

    public void setIcmpType(Integer icmpType) {
        this.icmpType = icmpType;
    }

    public void setIcmpCode(Integer icmpCode) {
        this.icmpCode = icmpCode;
    }

    public void setTags(List<ResourceTagResponse> tags) {
        this.tags = tags;
    }

    public void setForDisplay(Boolean forDisplay) {
        this.forDisplay = forDisplay;
    }

    public void setDestCidr(String cidrList){
        this.destCidr = cidrList;
    }

    public void setTrafficType(String trafficType) {
        this.trafficType = trafficType;
    }
}

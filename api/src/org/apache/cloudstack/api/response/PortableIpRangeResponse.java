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

import java.util.Date;
import java.util.List;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;

import com.cloud.network.IpAddress;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;
import org.apache.cloudstack.region.PortableIpRange;

@EntityReference(value=PortableIpRange.class)
public class PortableIpRangeResponse extends BaseResponse {

    @SerializedName(ApiConstants.ID)
    @Param(description = "portable IP range ID")
    private String id;

    @SerializedName(ApiConstants.REGION_ID)
    @Param(description = "Region Id in which portable ip range is provisioned")
    private Integer regionId;

    @SerializedName(ApiConstants.GATEWAY) @Param(description="the gateway of the VLAN IP range")
    private String gateway;

    @SerializedName(ApiConstants.NETMASK) @Param(description="the netmask of the VLAN IP range")
    private String netmask;

    @SerializedName(ApiConstants.VLAN) @Param(description="the ID or VID of the VLAN.")
    private String vlan;

    @SerializedName(ApiConstants.START_IP) @Param(description="the start ip of the portable IP range")
    private String startIp;

    @SerializedName(ApiConstants.END_IP) @Param(description="the end ip of the portable IP range")
    private String endIp;

    @SerializedName(ApiConstants.PORTABLE_IP_ADDRESS)
    @Param(description="List of portable IP and association with zone/network/vpc details that are part of GSLB rule", responseObject = PortableIpResponse.class)
    private List<PortableIpResponse> portableIpResponses;

    public void setId(String id) {
        this.id = id;
    }

    public void setRegionId(int regionId) {
        this.regionId = regionId;
    }

    public void setStartIp(String startIp) {
        this.startIp = startIp;
    }

    public void setEndIp(String endIp) {
        this.endIp = endIp;
    }

    public void setNetmask(String netmask) {
        this.netmask = netmask;
    }

    public void setGateway(String gateway) {
        this.gateway = gateway;
    }

    public void setVlan(String vlan) {
        this.vlan = vlan;
    }

    public void setPortableIpResponses(List<PortableIpResponse> portableIpResponses) {
        this.portableIpResponses = portableIpResponses;
    }
}

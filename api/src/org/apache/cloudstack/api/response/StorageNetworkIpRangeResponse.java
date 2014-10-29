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

import com.google.gson.annotations.SerializedName;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;

import com.cloud.dc.StorageNetworkIpRange;
import com.cloud.serializer.Param;

@EntityReference(value = StorageNetworkIpRange.class)
public class StorageNetworkIpRangeResponse extends BaseResponse {
    @SerializedName(ApiConstants.ID)
    @Param(description = "the uuid of storage network IP range.")
    private String uuid;

    @SerializedName(ApiConstants.VLAN)
    @Param(description = "the ID or VID of the VLAN.")
    private Integer vlan;

    @SerializedName(ApiConstants.POD_ID)
    @Param(description = "the Pod uuid for the storage network IP range")
    private String podUuid;

    @SerializedName(ApiConstants.START_IP)
    @Param(description = "the start ip of the storage network IP range")
    private String startIp;

    @SerializedName(ApiConstants.END_IP)
    @Param(description = "the end ip of the storage network IP range")
    private String endIp;

    @SerializedName(ApiConstants.GATEWAY)
    @Param(description = "the gateway of the storage network IP range")
    private String gateway;

    @SerializedName(ApiConstants.NETWORK_ID)
    @Param(description = "the network uuid of storage network IP range")
    private String networkUuid;

    @SerializedName(ApiConstants.ZONE_ID)
    @Param(description = "the Zone uuid of the storage network IP range")
    private String zoneUuid;

    @SerializedName(ApiConstants.NETMASK)
    @Param(description = "the netmask of the storage network IP range")
    private String netmask;

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public void setZoneUuid(String zoneUuid) {
        this.zoneUuid = zoneUuid;
    }

    public void setVlan(Integer vlan) {
        this.vlan = vlan;
    }

    public void setPodUuid(String podUuid) {
        this.podUuid = podUuid;
    }

    public void setStartIp(String startIp) {
        this.startIp = startIp;
    }

    public void setEndIp(String endIp) {
        this.endIp = endIp;
    }

    public void setNetworkUuid(String networkUuid) {
        this.networkUuid = networkUuid;
    }

    public void setNetmask(String netmask) {
        this.netmask = netmask;
    }

    public void setGateway(String gateway) {
        this.gateway = gateway;
    }
}

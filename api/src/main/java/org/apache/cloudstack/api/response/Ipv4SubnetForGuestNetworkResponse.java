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

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;
import org.apache.cloudstack.network.Ipv4GuestSubnetNetworkMap;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

@EntityReference(value = Ipv4GuestSubnetNetworkMap.class)
public class Ipv4SubnetForGuestNetworkResponse extends BaseResponse {
    @SerializedName(ApiConstants.ID)
    @Param(description = "id of the IPv4 subnet for guest network")
    private String id;

    @SerializedName(ApiConstants.PARENT_ID)
    @Param(description = "id of the data center IPv4 subnet")
    private String parentId;

    @SerializedName(ApiConstants.PARENT_SUBNET)
    @Param(description = "subnet of the data center IPv4 subnet")
    private String parentSubnet;

    @SerializedName(ApiConstants.SUBNET)
    @Param(description = "subnet of the IPv4 network")
    private String subnet;

    @SerializedName(ApiConstants.STATE)
    @Param(description = "state of subnet of the IPv4 network")
    private String state;

    @SerializedName(ApiConstants.ZONE_ID)
    @Param(description = "id of zone to which the IPv4 subnet belongs to." )
    private String zoneId;

    @SerializedName(ApiConstants.ZONE_NAME)
    @Param(description = "id of zone to which the IPv4 subnet belongs to." )
    private String zoneName;

    @SerializedName(ApiConstants.NETWORK_ID)
    @Param(description = "id of network which the IPv4 subnet is associated with." )
    private String networkId;

    @SerializedName(ApiConstants.NETWORK_NAME)
    @Param(description = "name of network which the IPv4 subnet is associated with." )
    private String networkName;

    @SerializedName(ApiConstants.VPC_ID)
    @Param(description = "Id of the VPC which the IPv4 subnet is associated with.")
    private String vpcId;

    @SerializedName(ApiConstants.VPC_NAME)
    @Param(description = "Name of the VPC which the IPv4 subnet is associated with.")
    private String vpcName;

    @SerializedName(ApiConstants.CREATED)
    @Param(description = "date when this IPv4 subnet was created." )
    private Date created;

    @SerializedName(ApiConstants.REMOVED)
    @Param(description = "date when this IPv4 subnet was removed." )
    private Date removed;

    @SerializedName(ApiConstants.ALLOCATED_TIME)
    @Param(description = "date when this IPv4 subnet was allocated." )
    private Date allocatedTime;

    public void setId(String id) {
        this.id = id;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public void setParentSubnet(String parentSubnet) {
        this.parentSubnet = parentSubnet;
    }

    public void setSubnet(String subnet) {
        this.subnet = subnet;
    }

    public void setState(String state) {
        this.state = state;
    }

    public void setNetworkId(String networkId) {
        this.networkId = networkId;
    }

    public void setNetworkName(String networkName) {
        this.networkName = networkName;
    }

    public void setVpcId(String vpcId) {
        this.vpcId = vpcId;
    }

    public void setVpcName(String vpcName) {
        this.vpcName = vpcName;
    }

    public void setZoneId(String zoneId) {
        this.zoneId = zoneId;
    }

    public void setZoneName(String zoneName) {
        this.zoneName = zoneName;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public void setRemoved(Date removed) {
        this.removed = removed;
    }

    public void setAllocatedTime(Date allocatedTime) {
        this.allocatedTime = allocatedTime;
    }

    public String getId() {
        return id;
    }

    public String getParentId() {
        return parentId;
    }

    public String getParentSubnet() {
        return parentSubnet;
    }

    public String getSubnet() {
        return subnet;
    }

    public String getState() {
        return state;
    }

    public String getZoneId() {
        return zoneId;
    }

    public String getZoneName() {
        return zoneName;
    }

    public String getNetworkId() {
        return networkId;
    }

    public String getNetworkName() {
        return networkName;
    }

    public String getVpcId() {
        return vpcId;
    }

    public String getVpcName() {
        return vpcName;
    }

    public Date getCreated() {
        return created;
    }

    public Date getRemoved() {
        return removed;
    }

    public Date getAllocatedTime() {
        return allocatedTime;
    }
}

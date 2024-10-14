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

import com.cloud.serializer.Param;

import java.util.Date;
import java.util.List;

@SuppressWarnings("unused")
public class GuestVlanResponse extends BaseResponse implements ControlledEntityResponse {

    @SerializedName(ApiConstants.ID)
    @Param(description = "the guest VLAN id")
    private long id;

    @SerializedName(ApiConstants.VLAN)
    @Param(description = "the guest VLAN")
    private String guestVlan;

    @SerializedName(ApiConstants.ACCOUNT)
    @Param(description = "the account of the guest VLAN range")
    private String accountName;

    @SerializedName(ApiConstants.DOMAIN_ID)
    @Param(description = "the domain ID of the guest VLAN range")
    private String domainId;

    @SerializedName(ApiConstants.DOMAIN)
    @Param(description = "the domain name of the guest VLAN range")
    private String domainName;

    @SerializedName(ApiConstants.DOMAIN_PATH)
    @Param(description = "path of the domain to which the guest VLAN range belongs", since = "4.19.2.0")
    private String domainPath;

    @SerializedName(ApiConstants.PROJECT_ID)
    @Param(description = "the project id of the guest VLAN range")
    private String projectId;

    @SerializedName(ApiConstants.PROJECT)
    @Param(description = "the project name of the guest VLAN range")
    private String projectName;

    @SerializedName(ApiConstants.ZONE_ID)
    @Param(description = "the zone ID of the guest VLAN range")
    private String zoneId;

    @SerializedName(ApiConstants.ZONE_NAME)
    @Param(description = "the zone name of the guest VLAN range")
    private String zoneName;

    @SerializedName(ApiConstants.PHYSICAL_NETWORK_ID)
    @Param(description = "the physical network ID of the guest VLAN range")
    private String physicalNetworkId;

    @SerializedName(ApiConstants.PHYSICAL_NETWORK_NAME)
    @Param(description = "the physical network name of the guest VLAN range")
    private String physicalNetworkName;

    @SerializedName(ApiConstants.IS_DEDICATED)
    @Param(description = "true if the guest VLAN is dedicated to the account")
    private Boolean isDedicated;

    @SerializedName(ApiConstants.ALLOCATION_STATE)
    @Param(description = "the allocation state of the guest VLAN")
    private String allocationState;

    @SerializedName(ApiConstants.TAKEN)
    @Param(description = "date the guest VLAN was taken")
    private Date taken;

    @SerializedName(ApiConstants.NETWORK)
    @Param(description = "the list of networks who use this guest VLAN", responseObject = NetworkResponse.class)
    private List<NetworkResponse> networks;

    public void setId(long id) {
        this.id = id;
    }

    @Override
    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    @Override
    public void setDomainId(String domainId) {
        this.domainId = domainId;
    }

    @Override
    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    @Override
    public void setDomainPath(String domainPath) {
        this.domainPath = domainPath;
    }
    @Override
    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    @Override
    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public void setZoneId(String zoneId) {
        this.zoneId = zoneId;
    }

    public void setZoneName(String zoneName) {
        this.zoneName = zoneName;
    }

    public void setPhysicalNetworkId(String physicalNetworkId) {
        this.physicalNetworkId = physicalNetworkId;
    }

    public void setPhysicalNetworkName(String physicalNetworkName) {
        this.physicalNetworkName = physicalNetworkName;
    }

    public void setGuestVlan(String guestVlan) {
        this.guestVlan = guestVlan;
    }

    public void setDedicated(Boolean dedicated) {
        isDedicated = dedicated;
    }

    public void setAllocationState(String allocationState) {
        this.allocationState = allocationState;
    }

    public void setTaken(Date taken) {
        this.taken = taken;
    }

    public void setNetworks(List<NetworkResponse> networks) {
        this.networks = networks;
    }
}

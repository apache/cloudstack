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

import com.cloud.network.GuestVlanRange;
import com.cloud.serializer.Param;

@EntityReference(value = GuestVlanRange.class)
@SuppressWarnings("unused")
public class GuestVlanRangeResponse extends BaseResponse implements ControlledEntityResponse {
    @SerializedName(ApiConstants.ID)
    @Param(description = "the ID of the guest VLAN range")
    private String id;

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

    @SerializedName(ApiConstants.GUEST_VLAN_RANGE)
    @Param(description = "the guest VLAN range")
    private String guestVlanRange;

    @SerializedName(ApiConstants.PROJECT_ID)
    @Param(description = "the project id of the guest vlan range")
    private String projectId;

    @SerializedName(ApiConstants.PROJECT)
    @Param(description = "the project name of the guest vlan range")
    private String projectName;

    @SerializedName(ApiConstants.PHYSICAL_NETWORK_ID)
    @Param(description = "the physical network of the guest vlan range")
    private Long physicalNetworkId;

    @SerializedName(ApiConstants.ZONE_ID)
    @Param(description = "the zone of the guest vlan range")
    private Long zoneId;

    public void setId(String id) {
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
    public void setGuestVlanRange(String guestVlanRange) {
        this.guestVlanRange = guestVlanRange;
    }

    @Override
    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    @Override
    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public void setPhysicalNetworkId(Long physicalNetworkId) {
        this.physicalNetworkId = physicalNetworkId;
    }

    public void setZoneId(Long zoneId) {
        this.zoneId = zoneId;
    }

}

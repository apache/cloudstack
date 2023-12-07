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

import com.google.gson.annotations.SerializedName;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;
import org.apache.cloudstack.region.PortableIp;

import com.cloud.serializer.Param;

@EntityReference(value = PortableIp.class)
public class PortableIpResponse extends BaseResponse {

    @SerializedName(ApiConstants.REGION_ID)
    @Param(description = "Region Id in which global load balancer is created")
    private Integer regionId;

    @SerializedName(ApiConstants.IP_ADDRESS)
    @Param(description = "public IP address")
    private String ipAddress;

    @SerializedName(ApiConstants.ZONE_ID)
    @Param(description = "the ID of the zone the public IP address belongs to")
    private String zoneId;

    @SerializedName(ApiConstants.NETWORK_ID)
    @Param(description = "the ID of the Network where ip belongs to")
    private String networkId;

    @SerializedName(ApiConstants.VPC_ID)
    @Param(description = "VPC the ip belongs to")
    private String vpcId;

    @SerializedName(ApiConstants.PHYSICAL_NETWORK_ID)
    @Param(description = "the physical network this belongs to")
    private String physicalNetworkId;

    @SerializedName(ApiConstants.ACCOUNT_ID)
    @Param(description = "the account ID the portable IP address is associated with")
    private String accountId;

    @SerializedName(ApiConstants.DOMAIN_ID)
    @Param(description = "the domain ID the portable IP address is associated with")
    private String domainId;

    @SerializedName("allocated")
    @Param(description = "date the portal IP address was acquired")
    private Date allocated;

    @SerializedName(ApiConstants.STATE)
    @Param(description = "State of the ip address. Can be: Allocating, Allocated, Releasing and Free")
    private String state;

    public void setRegionId(Integer regionId) {
        this.regionId = regionId;
    }

    public void setAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public void setAssociatedDataCenterId(String zoneId) {
        this.zoneId = zoneId;
    }

    public void setAssociatedWithNetworkId(String networkId) {
        this.networkId = networkId;
    }

    public void setAssociatedWithVpcId(String vpcId) {
        this.vpcId = vpcId;
    }

    public void setPhysicalNetworkId(String physicalNetworkId) {
        this.physicalNetworkId = physicalNetworkId;
    }

    public void setAllocatedToAccountId(String accountId) {
        this.accountId = accountId;
    }

    public void setAllocatedInDomainId(String domainId) {
        this.domainId = domainId;
    }

    public void setAllocatedTime(Date allocatedTimetime) {
        this.allocated = allocatedTimetime;
    }

    public void setState(String state) {
        this.state = state;
    }
}

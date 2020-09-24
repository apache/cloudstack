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

import com.cloud.network.vpc.VpcGateway;
import com.cloud.serializer.Param;

@EntityReference(value = VpcGateway.class)
@SuppressWarnings("unused")
public class PrivateGatewayResponse extends BaseResponse implements ControlledEntityResponse {

    @SerializedName(ApiConstants.ID)
    @Param(description = "the id of the private gateway")
    private String id;

    @SerializedName(ApiConstants.GATEWAY)
    @Param(description = "the gateway")
    private String gateway;

    @SerializedName(ApiConstants.NETMASK)
    @Param(description = "the private gateway's netmask")
    private String netmask;

    @SerializedName(ApiConstants.IP_ADDRESS)
    @Param(description = "the private gateway's ip address")
    private String address;

    @SerializedName(ApiConstants.ZONE_ID)
    @Param(description = "zone id of the private gateway")
    private String zoneId;

    @SerializedName(ApiConstants.ZONE_NAME)
    @Param(description = "the name of the zone the private gateway belongs to")
    private String zoneName;

    @SerializedName(ApiConstants.VLAN)
    @Param(description = "the network implementation uri for the private gateway")
    private String broadcastUri;

    @SerializedName(ApiConstants.VPC_ID)
    @Param(description = "VPC id the private gateway belongs to")
    private String vpcId;

    @SerializedName(ApiConstants.VPC_NAME)
    @Param(description = "VPC name the private gateway belongs to", since = "4.13.2")
    private String vpcName;

    @SerializedName(ApiConstants.PHYSICAL_NETWORK_ID)
    @Param(description = "the physical network id")
    private String physicalNetworkId;

    @SerializedName(ApiConstants.ACCOUNT)
    @Param(description = "the account associated with the private gateway")
    private String accountName;

    @SerializedName(ApiConstants.PROJECT_ID)
    @Param(description = "the project id of the private gateway")
    private String projectId;

    @SerializedName(ApiConstants.PROJECT)
    @Param(description = "the project name of the private gateway")
    private String projectName;

    @SerializedName(ApiConstants.DOMAIN_ID)
    @Param(description = "the ID of the domain associated with the private gateway")
    private String domainId;

    @SerializedName(ApiConstants.DOMAIN)
    @Param(description = "the domain associated with the private gateway")
    private String domainName;

    @SerializedName(ApiConstants.STATE)
    @Param(description = "State of the gateway, can be Creating, Ready, Deleting")
    private String state;

    @SerializedName(ApiConstants.SOURCE_NAT_SUPPORTED)
    @Param(description = "Souce Nat enable status")
    private Boolean sourceNat;

    @SerializedName(ApiConstants.ACL_ID)
    @Param(description = "ACL Id set for private gateway")
    private String aclId;

    @SerializedName(ApiConstants.ACL_NAME)
    @Param(description = "ACL name set for private gateway")
    private String aclName;

    @Override
    public String getObjectId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setGateway(String gateway) {
        this.gateway = gateway;
    }

    public void setNetmask(String netmask) {
        this.netmask = netmask;
    }

    public void setZoneId(String zoneId) {
        this.zoneId = zoneId;
    }

    public void setBroadcastUri(String broadcastUri) {
        this.broadcastUri = broadcastUri;
    }

    public void setZoneName(String zoneName) {
        this.zoneName = zoneName;
    }

    public void setVpcId(String vpcId) {
        this.vpcId = vpcId;
    }

    public void setVpcName(String vpcName) {
        this.vpcName = vpcName;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setPhysicalNetworkId(String physicalNetworkId) {
        this.physicalNetworkId = physicalNetworkId;
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
    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    @Override
    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public void setState(String state) {
        this.state = state;
    }

    public void setSourceNat(Boolean sourceNat) {
        this.sourceNat = sourceNat;
    }

    public void setAclId(String aclId) {
        this.aclId = aclId;
    }

    public void setAclName(String aclName) {
        this.aclName = aclName;
    }

}

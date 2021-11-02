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

import com.cloud.network.Ipv6Address;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponseWithAnnotations;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;
import org.apache.cloudstack.api.EntityReference;

@EntityReference(value = Ipv6Address.class)
public class Ipv6RangeResponse extends BaseResponseWithAnnotations implements ControlledEntityResponse {
    @SerializedName(ApiConstants.ID)
    @Param(description = "public ipv6 address id")
    private String id;

    @SerializedName(ApiConstants.ZONE_ID)
    @Param(description = "the ID of the zone the public ipv6 address belongs to")
    private String zoneId;

    @SerializedName(ApiConstants.ZONE_NAME)
    @Param(description = "the name of the zone the public ipv6 address belongs to")
    private String zoneName;

    @SerializedName(ApiConstants.PHYSICAL_NETWORK_ID)
    @Param(description = "the physical network this belongs to")
    private String physicalNetworkId;

    @SerializedName(ApiConstants.IP6_GATEWAY)
    @Param(description = "IPv6 gateway")
    private String ip6Gateway;

    @SerializedName(ApiConstants.IP6_CIDR)
    @Param(description = "IPv6 cidr")
    private String ip6Cidr;

    @SerializedName(ApiConstants.ROUTER_IPV6)
    @Param(description = "Outbound IPv6 address in virtual router")
    private String routerIpv6;

    @SerializedName(ApiConstants.ROUTER_IPV6_GATEWAY)
    @Param(description = "Gateway of Outbound IPv6 network")
    private String routerIpv6Gateway;

    @SerializedName(ApiConstants.ROUTER_IPV6_VLAN)
    @Param(description = "Vlan of Outbound IPv6 network")
    private String routerIpv6Vlan;

    @SerializedName(ApiConstants.DOMAIN_ID)
    @Param(description = "the domain ID the public ipv6 address is associated with")
    private String domainId;

    @SerializedName(ApiConstants.DOMAIN)
    @Param(description = "the domain the public ipv6 address is associated with")
    private String domainName;

    @SerializedName(ApiConstants.ACCOUNT)
    @Param(description = "the account the public ipv6 address is associated with")
    private String accountName;

    @SerializedName(ApiConstants.PROJECT_ID)
    @Param(description = "the project id of the ipv6 address")
    private String projectId;

    @SerializedName(ApiConstants.PROJECT)
    @Param(description = "the project name of the ipv6 address")
    private String projectName;

    @SerializedName(ApiConstants.NETWORK_ID)
    @Param(description = "the ID of the Network associated with the ipv6 address")
    private String networkId;

    @SerializedName(ApiConstants.NETWORK_NAME)
    @Param(description = "the name of the Network associated with the ipv6 address")
    private String networkName;

    @SerializedName(ApiConstants.VPC_ID)
    @Param(description = "VPC id the ipv6 belongs to")
    private String vpcId;

    @SerializedName(ApiConstants.VPC_NAME)
    @Param(description = "VPC name the ipv6 belongs to", since = "4.13.2")
    private String vpcName;

    @SerializedName(ApiConstants.TAGS)
    @Param(description = "the list of resource tags associated with ipv6 address", responseObject = ResourceTagResponse.class)
    private List<ResourceTagResponse> tags;

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

    public void setId(String id) {
        this.id = id;
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

    public void setIp6Gateway(String ip6Gateway) {
        this.ip6Gateway = ip6Gateway;
    }

    public void setIp6Cidr(String ip6Cidr) {
        this.ip6Cidr = ip6Cidr;
    }

    public void setRouterIpv6(String routerIpv6) {
        this.routerIpv6 = routerIpv6;
    }

    public void setRouterIpv6Gateway(String routerIpv6Gateway) {
        this.routerIpv6Gateway = routerIpv6Gateway;
    }

    public void setRouterIpv6Vlan(String routerIpv6Vlan) {
        this.routerIpv6Vlan = routerIpv6Vlan;
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

    public void setTags(List<ResourceTagResponse> tags) {
        this.tags = tags;
    }
}

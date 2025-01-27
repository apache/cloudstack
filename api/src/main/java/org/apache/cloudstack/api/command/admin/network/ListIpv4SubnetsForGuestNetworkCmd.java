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
package org.apache.cloudstack.api.command.admin.network;

import java.util.ArrayList;
import java.util.List;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.DataCenterIpv4SubnetResponse;
import org.apache.cloudstack.api.response.Ipv4SubnetForGuestNetworkResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.NetworkResponse;
import org.apache.cloudstack.api.response.VpcResponse;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.cloudstack.network.Ipv4GuestSubnetNetworkMap;

@APICommand(name = "listIpv4SubnetsForGuestNetwork",
        description = "Lists IPv4 subnets for guest networks.",
        responseObject = Ipv4SubnetForGuestNetworkResponse.class,
        since = "4.20.0",
        requestHasSensitiveInfo = false,
        responseHasSensitiveInfo = false,
        authorized = {RoleType.Admin})
public class ListIpv4SubnetsForGuestNetworkCmd extends BaseListCmd {

    @Parameter(name = ApiConstants.ID,
            type = CommandType.UUID,
            entityType = Ipv4SubnetForGuestNetworkResponse.class,
            description = "UUID of the IPv4 subnet for guest network.")
    private Long id;

    @Parameter(name = ApiConstants.PARENT_ID,
            type = CommandType.UUID,
            entityType = DataCenterIpv4SubnetResponse.class,
            description = "UUID of zone Ipv4 subnet which the IPv4 subnet belongs to.")
    private Long parentId;

    @Parameter(name = ApiConstants.SUBNET,
            type = CommandType.STRING,
            description = "The CIDR of the Ipv4 subnet.")
    private String subnet;

    @Parameter(name = ApiConstants.ZONE_ID,
            type = CommandType.UUID,
            entityType = ZoneResponse.class,
            description = "UUID of zone to which the IPv4 subnet belongs to.")
    private Long zoneId;

    @Parameter(name = ApiConstants.NETWORK_ID,
            type = CommandType.UUID,
            entityType = NetworkResponse.class,
            description = "UUID of network to which the IPv4 subnet is associated to.")
    private Long networkId;

    @Parameter(name = ApiConstants.VPC_ID,
            type = CommandType.UUID,
            entityType = VpcResponse.class,
            description = "UUID of VPC to which the IPv4 subnet is associated to.")
    private Long vpcId;


    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public Long getParentId() {
        return parentId;
    }

    public Long getZoneId() {
        return zoneId;
    }

    public String getSubnet() {
        return subnet;
    }

    public Long getNetworkId() {
        return networkId;
    }

    public Long getVpcId() {
        return vpcId;
    }

    @Override
    public void execute() {
        List<? extends Ipv4GuestSubnetNetworkMap> subnets = routedIpv4Manager.listIpv4GuestSubnetsForGuestNetwork(this);
        ListResponse<Ipv4SubnetForGuestNetworkResponse> response = new ListResponse<>();
        List<Ipv4SubnetForGuestNetworkResponse> subnetResponses = new ArrayList<>();
        for (Ipv4GuestSubnetNetworkMap subnet : subnets) {
            Ipv4SubnetForGuestNetworkResponse subnetResponse = routedIpv4Manager.createIpv4SubnetForGuestNetworkResponse(subnet);
            subnetResponses.add(subnetResponse);
        }

        response.setResponses(subnetResponses, subnets.size());
        response.setResponseName(getCommandName());
        this.setResponseObject(response);
    }

}

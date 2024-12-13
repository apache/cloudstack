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
import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.ProjectResponse;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.cloudstack.datacenter.DataCenterIpv4GuestSubnet;

@APICommand(name = "listIpv4SubnetsForZone",
        description = "Lists IPv4 subnets for zone.",
        responseObject = DataCenterIpv4SubnetResponse.class,
        since = "4.20.0",
        requestHasSensitiveInfo = false,
        responseHasSensitiveInfo = false,
        authorized = {RoleType.Admin})
public class ListIpv4SubnetsForZoneCmd extends BaseListCmd {

    @Parameter(name = ApiConstants.ID,
            type = CommandType.UUID,
            entityType = DataCenterIpv4SubnetResponse.class,
            description = "UUID of the IPv4 subnet.")
    private Long id;

    @Parameter(name = ApiConstants.ZONE_ID,
            type = CommandType.UUID,
            entityType = ZoneResponse.class,
            description = "UUID of zone to which the IPv4 subnet belongs to.")
    private Long zoneId;

    @Parameter(name = ApiConstants.SUBNET,
            type = CommandType.STRING,
            description = "CIDR of the IPv4 subnet.")
    private String subnet;

    @Parameter(name = ApiConstants.ACCOUNT,
            type = CommandType.STRING,
            description = "the account which the IPv4 subnet is dedicated to. Must be used with the domainId parameter.")
    private String accountName;

    @Parameter(name = ApiConstants.PROJECT_ID,
            type = CommandType.UUID,
            entityType = ProjectResponse.class,
            description = "project who which the IPv4 subnet is dedicated to")
    private Long projectId;

    @Parameter(name = ApiConstants.DOMAIN_ID,
            type = CommandType.UUID,
            entityType = DomainResponse.class,
            description = "the domain ID which the IPv4 subnet is dedicated to.")
    private Long domainId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public Long getZoneId() {
        return zoneId;
    }

    public String getSubnet() {
        return subnet;
    }

    public String getAccountName() {
        return accountName;
    }

    public Long getProjectId() {
        return projectId;
    }

    public Long getDomainId() {
        return domainId;
    }

    @Override
    public void execute() {
        List<? extends DataCenterIpv4GuestSubnet> subnets = routedIpv4Manager.listDataCenterIpv4GuestSubnets(this);
        ListResponse<DataCenterIpv4SubnetResponse> response = new ListResponse<>();
        List<DataCenterIpv4SubnetResponse> subnetResponses = new ArrayList<>();
        for (DataCenterIpv4GuestSubnet subnet : subnets) {
            DataCenterIpv4SubnetResponse subnetResponse = routedIpv4Manager.createDataCenterIpv4SubnetResponse(subnet);
            subnetResponses.add(subnetResponse);
        }

        response.setResponses(subnetResponses, subnets.size());
        response.setResponseName(getCommandName());
        this.setResponseObject(response);
    }

}

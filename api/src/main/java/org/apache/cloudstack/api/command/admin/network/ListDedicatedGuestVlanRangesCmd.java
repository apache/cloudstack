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

import org.apache.log4j.Logger;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.api.response.GuestVlanRangeResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.PhysicalNetworkResponse;
import org.apache.cloudstack.api.response.ProjectResponse;
import org.apache.cloudstack.api.response.ZoneResponse;

import com.cloud.network.GuestVlanRange;
import com.cloud.user.Account;
import com.cloud.utils.Pair;

@APICommand(name = "listDedicatedGuestVlanRanges", description = "Lists dedicated guest vlan ranges", responseObject = GuestVlanRangeResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class ListDedicatedGuestVlanRangesCmd extends BaseListCmd {
    public static final Logger s_logger = Logger.getLogger(ListDedicatedGuestVlanRangesCmd.class.getName());


    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = GuestVlanRangeResponse.class, description = "list dedicated guest vlan ranges by id")
    private Long id;

    @Parameter(name = ApiConstants.ACCOUNT,
               type = CommandType.STRING,
               description = "the account with which the guest VLAN range is associated. Must be used with the domainId parameter.")
    private String accountName;

    @Parameter(name = ApiConstants.PROJECT_ID, type = CommandType.UUID, entityType = ProjectResponse.class, description = "project who will own the guest VLAN range")
    private Long projectId;

    @Parameter(name = ApiConstants.DOMAIN_ID,
               type = CommandType.UUID,
               entityType = DomainResponse.class,
               description = "the domain ID with which the guest VLAN range is associated.  If used with the account parameter, returns all guest VLAN ranges for that account in the specified domain.")
    private Long domainId;

    @Parameter(name = ApiConstants.GUEST_VLAN_RANGE, type = CommandType.STRING, description = "the dedicated guest vlan range")
    private String guestVlanRange;

    @Parameter(name = ApiConstants.PHYSICAL_NETWORK_ID,
               type = CommandType.UUID,
               entityType = PhysicalNetworkResponse.class,
               description = "physical network id of the guest VLAN range")
    private Long physicalNetworkId;

    @Parameter(name = ApiConstants.ZONE_ID, type = CommandType.UUID, entityType = ZoneResponse.class, description = "zone of the guest VLAN range")
    private Long zoneId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public String getAccountName() {
        return accountName;
    }

    public Long getDomainId() {
        return domainId;
    }

    public Long getProjectId() {
        return projectId;
    }

    public String getGuestVlanRange() {
        return guestVlanRange;
    }

    public Long getPhysicalNetworkId() {
        return physicalNetworkId;
    }

    public Long getZoneId() {
        return zoneId;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public void execute() {
        Pair<List<? extends GuestVlanRange>, Integer> vlans = _networkService.listDedicatedGuestVlanRanges(this);
        ListResponse<GuestVlanRangeResponse> response = new ListResponse<GuestVlanRangeResponse>();
        List<GuestVlanRangeResponse> guestVlanResponses = new ArrayList<GuestVlanRangeResponse>();
        for (GuestVlanRange vlan : vlans.first()) {
            GuestVlanRangeResponse guestVlanResponse = _responseGenerator.createDedicatedGuestVlanRangeResponse(vlan);
            guestVlanResponse.setObjectName("dedicatedguestvlanrange");
            guestVlanResponses.add(guestVlanResponse);
        }

        response.setResponses(guestVlanResponses, vlans.second());
        response.setResponseName(getCommandName());
        this.setResponseObject(response);
    }

}

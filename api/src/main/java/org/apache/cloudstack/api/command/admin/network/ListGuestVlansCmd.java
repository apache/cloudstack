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
import org.apache.cloudstack.api.response.PhysicalNetworkResponse;
import org.apache.cloudstack.api.response.ZoneResponse;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.GuestVlanResponse;
import org.apache.cloudstack.api.response.ListResponse;

import com.cloud.network.GuestVlan;
import com.cloud.utils.Pair;

@APICommand(name = "listGuestVlans", description = "Lists all guest vlans", responseObject = GuestVlanResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false,
        since = "4.17.0",
        authorized = {RoleType.Admin})
public class ListGuestVlansCmd extends BaseListCmd {


    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ID, type = CommandType.LONG, required = false, description = "list guest vlan by id")
    private Long id;

    @Parameter(name = ApiConstants.ZONE_ID, type = CommandType.UUID, entityType = ZoneResponse.class, required = false, description = "list guest vlan by zone")
    private Long zoneId;

    @Parameter(name = ApiConstants.PHYSICAL_NETWORK_ID, type = CommandType.UUID, entityType = PhysicalNetworkResponse.class, required = false, description = "list guest vlan by physical network")
    private Long physicalNetworkId;

    @Parameter(name = ApiConstants.VNET, type = CommandType.STRING, required = false, description = "list guest vlan by vnet")
    private String vnet;

    @Parameter(name = ApiConstants.ALLOCATED_ONLY, type = CommandType.BOOLEAN, required = false, description = "limits search results to allocated guest vlan. false by default.")
    private Boolean allocatedOnly;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public Long getZoneId() {
        return zoneId;
    }

    public Long getPhysicalNetworkId() {
        return physicalNetworkId;
    }

    public String getVnet() {
        return vnet;
    }

    public Boolean getAllocatedOnly() {
        return allocatedOnly;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() {
        Pair<List<? extends GuestVlan>, Integer> vlans = _networkService.listGuestVlans(this);
        ListResponse<GuestVlanResponse> response = new ListResponse<GuestVlanResponse>();
        List<GuestVlanResponse> guestVlanResponses = new ArrayList<GuestVlanResponse>();
        for (GuestVlan vlan : vlans.first()) {
            GuestVlanResponse guestVlanResponse = _responseGenerator.createGuestVlanResponse(vlan);
            guestVlanResponse.setObjectName("guestvlan");
            guestVlanResponses.add(guestVlanResponse);
        }

        response.setResponses(guestVlanResponses, vlans.second());
        response.setResponseName(getCommandName());
        this.setResponseObject(response);
    }
}

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
package org.apache.cloudstack.api.command.admin.vlan;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.NetworkResponse;
import org.apache.cloudstack.api.response.PhysicalNetworkResponse;
import org.apache.cloudstack.api.response.PodResponse;
import org.apache.cloudstack.api.response.ProjectResponse;
import org.apache.cloudstack.api.response.VlanIpRangeResponse;
import org.apache.cloudstack.api.response.ZoneResponse;

import com.cloud.dc.Vlan;
import com.cloud.utils.Pair;

@APICommand(name = "listVlanIpRanges", description = "Lists all VLAN IP ranges.", responseObject = VlanIpRangeResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class ListVlanIpRangesCmd extends BaseListCmd {
    public static final Logger s_logger = Logger.getLogger(ListVlanIpRangesCmd.class.getName());


    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ACCOUNT,
               type = CommandType.STRING,
               description = "the account with which the VLAN IP range is associated. Must be used with the domainId parameter.")
    private String accountName;

    @Parameter(name = ApiConstants.PROJECT_ID, type = CommandType.UUID, entityType = ProjectResponse.class, description = "project who will own the VLAN")
    private Long projectId;

    @Parameter(name = ApiConstants.DOMAIN_ID,
               type = CommandType.UUID,
               entityType = DomainResponse.class,
               description = "the domain ID with which the VLAN IP range is associated.  If used with the account parameter, " +
                   "returns all VLAN IP ranges for that account in the specified domain.")
    private Long domainId;

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = VlanIpRangeResponse.class, required = false, description = "the ID of the VLAN IP range")
    private Long id;

    @Parameter(name = ApiConstants.POD_ID, type = CommandType.UUID, entityType = PodResponse.class, description = "the Pod ID of the VLAN IP range")
    private Long podId;

    @Parameter(name = ApiConstants.VLAN, type = CommandType.STRING, description = "the ID or VID of the VLAN. Default is an \"untagged\" VLAN.")
    private String vlan;

    @Parameter(name = ApiConstants.ZONE_ID, type = CommandType.UUID, entityType = ZoneResponse.class, description = "the Zone ID of the VLAN IP range")
    private Long zoneId;

    @Parameter(name = ApiConstants.NETWORK_ID, type = CommandType.UUID, entityType = NetworkResponse.class, description = "network id of the VLAN IP range")
    private Long networkId;

    @Parameter(name = ApiConstants.FOR_VIRTUAL_NETWORK, type = CommandType.BOOLEAN, description = "true if VLAN is of Virtual type, false if Direct")
    private Boolean forVirtualNetwork;

    @Parameter(name = ApiConstants.PHYSICAL_NETWORK_ID,
               type = CommandType.UUID,
               entityType = PhysicalNetworkResponse.class,
               description = "physical network id of the VLAN IP range")
    private Long physicalNetworkId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getAccountName() {
        return accountName;
    }

    public Long getDomainId() {
        return domainId;
    }

    public Long getId() {
        return id;
    }

    public Long getPodId() {
        return podId;
    }

    public String getVlan() {
        return vlan;
    }

    public Long getZoneId() {
        return zoneId;
    }

    public Long getNetworkId() {
        return networkId;
    }

    public Boolean isForVirtualNetwork() {
        return forVirtualNetwork;
    }

    public Long getProjectId() {
        return projectId;
    }

    public Long getPhysicalNetworkId() {
        return physicalNetworkId;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() {
        Pair<List<? extends Vlan>, Integer> vlans = _mgr.searchForVlans(this);
        ListResponse<VlanIpRangeResponse> response = new ListResponse<VlanIpRangeResponse>();
        List<VlanIpRangeResponse> vlanResponses = new ArrayList<VlanIpRangeResponse>();
        for (Vlan vlan : vlans.first()) {
            VlanIpRangeResponse vlanResponse = _responseGenerator.createVlanIpRangeResponse(vlan);
            vlanResponse.setObjectName("vlaniprange");
            vlanResponses.add(vlanResponse);
        }

        response.setResponses(vlanResponses, vlans.second());
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }
}

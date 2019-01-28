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
package org.apache.cloudstack.api.command.user.network;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListTaggedResourcesCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject.ResponseView;
import org.apache.cloudstack.api.command.user.UserCmd;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.NetworkResponse;
import org.apache.cloudstack.api.response.PhysicalNetworkResponse;
import org.apache.cloudstack.api.response.VpcResponse;
import org.apache.cloudstack.api.response.ZoneResponse;

import com.cloud.network.Network;
import com.cloud.utils.Pair;

@APICommand(name = "listNetworks", description = "Lists all available networks.", responseObject = NetworkResponse.class, responseView = ResponseView.Restricted, entityType = {Network.class},
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class ListNetworksCmd extends BaseListTaggedResourcesCmd implements UserCmd {
    public static final Logger s_logger = Logger.getLogger(ListNetworksCmd.class.getName());
    private static final String s_name = "listnetworksresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = NetworkResponse.class, description = "list networks by ID")
    private Long id;

    @Parameter(name = ApiConstants.ZONE_ID, type = CommandType.UUID, entityType = ZoneResponse.class, description = "the zone ID of the network")
    private Long zoneId;

    @Parameter(name = ApiConstants.TYPE, type = CommandType.STRING, description = "the type of the network. Supported values are: isolated and shared")
    private String guestIpType;

    @Parameter(name = ApiConstants.IS_SYSTEM, type = CommandType.BOOLEAN, description = "true if network is system, false otherwise")
    private Boolean isSystem;

    @Parameter(name = ApiConstants.ACL_TYPE, type = CommandType.STRING, description = "list networks by ACL (access control list) type. Supported values are account and domain")
    private String aclType;

    @Parameter(name = ApiConstants.TRAFFIC_TYPE, type = CommandType.STRING, description = "type of the traffic")
    private String trafficType;

    @Parameter(name = ApiConstants.PHYSICAL_NETWORK_ID, type = CommandType.UUID, entityType = PhysicalNetworkResponse.class, description = "list networks by physical network id")
    private Long physicalNetworkId;

    @Parameter(name = ApiConstants.SUPPORTED_SERVICES, type = CommandType.LIST, collectionType = CommandType.STRING, description = "list networks supporting certain services")
    private List<String> supportedServices;

    @Parameter(name = ApiConstants.RESTART_REQUIRED, type = CommandType.BOOLEAN, description = "list networks by restartRequired")
    private Boolean restartRequired;

    @Parameter(name = ApiConstants.SPECIFY_IP_RANGES, type = CommandType.BOOLEAN, description = "true if need to list only networks which support specifying IP ranges")
    private Boolean specifyIpRanges;

    @Parameter(name = ApiConstants.VPC_ID, type = CommandType.UUID, entityType = VpcResponse.class, description = "List networks by VPC")
    private Long vpcId;

    @Parameter(name = ApiConstants.CAN_USE_FOR_DEPLOY, type = CommandType.BOOLEAN, description = "list networks available for VM deployment")
    private Boolean canUseForDeploy;

    @Parameter(name = ApiConstants.FOR_VPC, type = CommandType.BOOLEAN, description = "the network belongs to VPC")
    private Boolean forVpc;

    @Parameter(name = ApiConstants.DISPLAY_NETWORK, type = CommandType.BOOLEAN, description = "list resources by display flag; only ROOT admin is eligible to pass this parameter", since = "4.4", authorized = {RoleType.Admin})
    private Boolean display;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public Long getZoneId() {
        return zoneId;
    }

    public String getGuestIpType() {
        return guestIpType;
    }

    public Boolean getIsSystem() {
        return isSystem;
    }

    public String getAclType() {
        return aclType;
    }

    public String getTrafficType() {
        return trafficType;
    }

    public Long getPhysicalNetworkId() {
        return physicalNetworkId;
    }

    public List<String> getSupportedServices() {
        return supportedServices;
    }

    public Boolean isRestartRequired() {
        return restartRequired;
    }

    public Boolean isSpecifyIpRanges() {
        return specifyIpRanges;
    }

    public Long getVpcId() {
        return vpcId;
    }

    public Boolean canUseForDeploy() {
        return canUseForDeploy;
    }

    public Boolean getForVpc() {
        return forVpc;
    }

    @Override
    public Boolean getDisplay() {
        if (display != null) {
            return display;
        }
        return super.getDisplay();
    }
    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////
    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public void execute() {
        Pair<List<? extends Network>, Integer> networks = _networkService.searchForNetworks(this);
        ListResponse<NetworkResponse> response = new ListResponse<NetworkResponse>();
        List<NetworkResponse> networkResponses = new ArrayList<NetworkResponse>();
        for (Network network : networks.first()) {
            NetworkResponse networkResponse = _responseGenerator.createNetworkResponse(getResponseView(), network);
            networkResponses.add(networkResponse);
        }
        response.setResponses(networkResponses, networks.second());
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }
}

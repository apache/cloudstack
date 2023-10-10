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
package org.apache.cloudstack.api.command.user.vpc;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListProjectAndAccountResourcesCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject;
import org.apache.cloudstack.api.command.user.UserCmd;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.PrivateGatewayResponse;
import org.apache.cloudstack.api.response.VpcResponse;

import com.cloud.network.vpc.PrivateGateway;
import com.cloud.network.vpc.VpcGateway;
import com.cloud.utils.Pair;

@APICommand(name = "listPrivateGateways", description = "List private gateways", responseObject = PrivateGatewayResponse.class, entityType = {VpcGateway.class},
        responseView = ResponseObject.ResponseView.Restricted,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class ListPrivateGatewaysCmd extends BaseListProjectAndAccountResourcesCmd implements UserCmd {
    public static final Logger s_logger = Logger.getLogger(ListPrivateGatewaysCmd.class.getName());


    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = PrivateGatewayResponse.class, description = "list private gateway by id")
    private Long id;

    @Parameter(name = ApiConstants.IP_ADDRESS, type = CommandType.STRING, description = "list gateways by ip address")
    private String ipAddress;

    @Parameter(name = ApiConstants.VLAN, type = CommandType.STRING, description = "list gateways by vlan")
    private String vlan;

    @Parameter(name = ApiConstants.VPC_ID, type = CommandType.UUID, entityType = VpcResponse.class, description = "list gateways by vpc")
    private Long vpcId;

    @Parameter(name = ApiConstants.STATE, type = CommandType.STRING, description = "list gateways by state")
    private String state;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getVlan() {
        return vlan;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public Long getVpcId() {
        return vpcId;
    }

    public Long getId() {
        return id;
    }

    public String getState() {
        return state;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////
    @Override
    public void execute() {
        Pair<List<PrivateGateway>, Integer> gateways = _vpcService.listPrivateGateway(this);
        ListResponse<PrivateGatewayResponse> response = new ListResponse<PrivateGatewayResponse>();
        List<PrivateGatewayResponse> projectResponses = new ArrayList<PrivateGatewayResponse>();
        for (PrivateGateway gateway : gateways.first()) {
            PrivateGatewayResponse gatewayResponse = _responseGenerator.createPrivateGatewayResponse(getResponseView(), gateway);
            projectResponses.add(gatewayResponse);
        }
        response.setResponses(projectResponses, gateways.second());
        response.setResponseName(getCommandName());

        setResponseObject(response);
    }
}

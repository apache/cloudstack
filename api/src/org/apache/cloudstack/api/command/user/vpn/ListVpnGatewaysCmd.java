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
package org.apache.cloudstack.api.command.user.vpn;

import java.util.ArrayList;
import java.util.List;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListProjectAndAccountResourcesCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.Site2SiteVpnGatewayResponse;
import org.apache.cloudstack.api.response.VpcResponse;
import org.apache.log4j.Logger;

import com.cloud.network.Site2SiteVpnGateway;
import com.cloud.utils.Pair;

@APICommand(name = "listVpnGateways", description="Lists site 2 site vpn gateways", responseObject=Site2SiteVpnGatewayResponse.class)
public class ListVpnGatewaysCmd extends BaseListProjectAndAccountResourcesCmd {
    public static final Logger s_logger = Logger.getLogger (ListVpnGatewaysCmd.class.getName());

    private static final String s_name = "listvpngatewaysresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.ID, type=CommandType.UUID, entityType=Site2SiteVpnGatewayResponse.class,
            description="id of the vpn gateway")
    private Long id;

    @Parameter(name=ApiConstants.VPC_ID, type=CommandType.UUID, entityType=VpcResponse.class, description="id of vpc")
    private Long vpcId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public Long getVpcId() {
        return vpcId;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public void execute(){
        Pair<List<? extends Site2SiteVpnGateway>, Integer> gws = _s2sVpnService.searchForVpnGateways(this);
        ListResponse<Site2SiteVpnGatewayResponse> response = new ListResponse<Site2SiteVpnGatewayResponse>();
        List<Site2SiteVpnGatewayResponse> gwResponses = new ArrayList<Site2SiteVpnGatewayResponse>();
        for (Site2SiteVpnGateway gw : gws.first()) {
            if (gw == null) {
                continue;
            }
            Site2SiteVpnGatewayResponse site2SiteVpnGatewayRes = _responseGenerator.createSite2SiteVpnGatewayResponse(gw);
            site2SiteVpnGatewayRes.setObjectName("vpngateway");
            gwResponses.add(site2SiteVpnGatewayRes);
        }

        response.setResponses(gwResponses, gws.second());
        response.setResponseName(getCommandName());
        this.setResponseObject(response);
    }
}

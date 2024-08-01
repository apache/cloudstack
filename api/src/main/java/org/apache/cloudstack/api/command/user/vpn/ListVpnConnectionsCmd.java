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


import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListProjectAndAccountResourcesCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.Site2SiteVpnConnectionResponse;
import org.apache.cloudstack.api.response.VpcResponse;

import com.cloud.network.Site2SiteVpnConnection;
import com.cloud.utils.Pair;

@APICommand(name = "listVpnConnections", description = "Lists site to site vpn connection gateways", responseObject = Site2SiteVpnConnectionResponse.class, entityType = {Site2SiteVpnConnection.class},
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class ListVpnConnectionsCmd extends BaseListProjectAndAccountResourcesCmd {


    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = Site2SiteVpnConnectionResponse.class, description = "id of the vpn connection")
    private Long id;

    @Parameter(name = ApiConstants.VPC_ID, type = CommandType.UUID, entityType = VpcResponse.class, description = "id of vpc")
    private Long vpcId;

    @Parameter(name = ApiConstants.FOR_DISPLAY, type = CommandType.BOOLEAN, description = "list resources by display flag; only ROOT admin is eligible to pass this parameter", since = "4.4", authorized = {RoleType.Admin})
    private Boolean display;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public Long getVpcId() {
        return vpcId;
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
    public void execute() {
        Pair<List<? extends Site2SiteVpnConnection>, Integer> conns = _s2sVpnService.searchForVpnConnections(this);
        ListResponse<Site2SiteVpnConnectionResponse> response = new ListResponse<Site2SiteVpnConnectionResponse>();
        List<Site2SiteVpnConnectionResponse> connResponses = new ArrayList<Site2SiteVpnConnectionResponse>();
        for (Site2SiteVpnConnection conn : conns.first()) {
            if (conn == null) {
                continue;
            }
            Site2SiteVpnConnectionResponse site2SiteVpnConnectonRes = _responseGenerator.createSite2SiteVpnConnectionResponse(conn);
            site2SiteVpnConnectonRes.setObjectName("vpnconnection");
            connResponses.add(site2SiteVpnConnectonRes);
        }

        response.setResponses(connResponses, conns.second());
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }
}

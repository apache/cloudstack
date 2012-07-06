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
package com.cloud.api.commands;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.cloud.api.ApiConstants;
import com.cloud.api.BaseListProjectAndAccountResourcesCmd;
import com.cloud.api.IdentityMapper;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.response.ListResponse;
import com.cloud.api.response.Site2SiteVpnConnectionResponse;
import com.cloud.network.Site2SiteVpnConnection;

@Implementation(description="Lists site to site vpn connection gateways", responseObject=Site2SiteVpnConnectionResponse.class)
public class ListVpnConnectionsCmd extends BaseListProjectAndAccountResourcesCmd {
    public static final Logger s_logger = Logger.getLogger (ListVpnConnectionsCmd.class.getName());

    private static final String s_name = "listvpnconnectionsresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @IdentityMapper(entityTableName="s2s_vpn_connection")
    @Parameter(name=ApiConstants.ID, type=CommandType.LONG, description="id of the vpn connection")
    private Long id;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    
    public Long getId() {
        return id;
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
        List<Site2SiteVpnConnection> conns = _s2sVpnService.searchForVpnConnections(this);
        ListResponse<Site2SiteVpnConnectionResponse> response = new ListResponse<Site2SiteVpnConnectionResponse>();
        List<Site2SiteVpnConnectionResponse> connResponses = new ArrayList<Site2SiteVpnConnectionResponse>();
        if (conns != null && !conns.isEmpty()) {
        	for (Site2SiteVpnConnection conn : conns) {
                if (conn == null) {
                    continue;
                }
            	Site2SiteVpnConnectionResponse site2SiteVpnConnectonRes = _responseGenerator.createSite2SiteVpnConnectionResponse(conn);
            	site2SiteVpnConnectonRes.setObjectName("vpnconnection");
                connResponses.add(site2SiteVpnConnectonRes);
            }
        }
        response.setResponses(connResponses);
        response.setResponseName(getCommandName());
        this.setResponseObject(response);
    }
}

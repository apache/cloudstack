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

import org.apache.log4j.Logger;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListProjectAndAccountResourcesCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.Site2SiteCustomerGatewayResponse;

import com.cloud.network.Site2SiteCustomerGateway;
import com.cloud.utils.Pair;

@APICommand(name = "listVpnCustomerGateways", description = "Lists site to site vpn customer gateways", responseObject = Site2SiteCustomerGatewayResponse.class, entityType = {Site2SiteCustomerGateway.class},
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class ListVpnCustomerGatewaysCmd extends BaseListProjectAndAccountResourcesCmd {
    public static final Logger s_logger = Logger.getLogger(ListVpnCustomerGatewaysCmd.class.getName());


    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = Site2SiteCustomerGatewayResponse.class, description = "id of the customer gateway")
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
    public void execute() {
        Pair<List<? extends Site2SiteCustomerGateway>, Integer> gws = _s2sVpnService.searchForCustomerGateways(this);
        ListResponse<Site2SiteCustomerGatewayResponse> response = new ListResponse<Site2SiteCustomerGatewayResponse>();
        List<Site2SiteCustomerGatewayResponse> gwResponses = new ArrayList<Site2SiteCustomerGatewayResponse>();
        for (Site2SiteCustomerGateway gw : gws.first()) {
            if (gw == null) {
                continue;
            }
            Site2SiteCustomerGatewayResponse site2SiteCustomerGatewayRes = _responseGenerator.createSite2SiteCustomerGatewayResponse(gw);
            site2SiteCustomerGatewayRes.setObjectName("vpncustomergateway");
            gwResponses.add(site2SiteCustomerGatewayRes);
        }

        response.setResponses(gwResponses, gws.second());
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }
}

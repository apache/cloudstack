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

import javax.inject.Inject;

import org.apache.log4j.Logger;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.ExternalFirewallResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.ZoneResponse;

import com.cloud.host.Host;
import com.cloud.network.element.JuniperSRXFirewallElementService;

@APICommand(name = "listExternalFirewalls", description = "List external firewall appliances.", responseObject = ExternalFirewallResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class ListExternalFirewallsCmd extends BaseListCmd {
    public static final Logger s_logger = Logger.getLogger(ListExternalFirewallsCmd.class.getName());

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ZONE_ID, type = CommandType.UUID, entityType = ZoneResponse.class, required = true, description = "zone Id")
    private long zoneId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public long getZoneId() {
        return zoneId;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Inject
    JuniperSRXFirewallElementService _srxElementService;

    @SuppressWarnings("deprecation")
    @Override
    public void execute() {

        List<? extends Host> externalFirewalls = _srxElementService.listExternalFirewalls(this);

        ListResponse<ExternalFirewallResponse> listResponse = new ListResponse<ExternalFirewallResponse>();
        List<ExternalFirewallResponse> responses = new ArrayList<ExternalFirewallResponse>();
        for (Host externalFirewall : externalFirewalls) {
            ExternalFirewallResponse response = _srxElementService.createExternalFirewallResponse(externalFirewall);
            response.setObjectName("externalfirewall");
            response.setResponseName(getCommandName());
            responses.add(response);
        }

        listResponse.setResponses(responses);
        listResponse.setResponseName(getCommandName());
        this.setResponseObject(listResponse);
    }
}

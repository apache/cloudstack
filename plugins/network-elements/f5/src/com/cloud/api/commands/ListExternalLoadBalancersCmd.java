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
import org.apache.cloudstack.api.response.ExternalLoadBalancerResponse;
import org.apache.cloudstack.api.response.HostResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.ZoneResponse;

import com.cloud.host.Host;
import com.cloud.network.element.F5ExternalLoadBalancerElementService;

@APICommand(name = "listExternalLoadBalancers", description = "Lists F5 external load balancer appliances added in a zone.", responseObject = HostResponse.class)
@Deprecated
// API supported for backward compatibility.
public class ListExternalLoadBalancersCmd extends BaseListCmd {
    public static final Logger s_logger = Logger.getLogger(ListExternalLoadBalancersCmd.class.getName());
    private static final String s_name = "listexternalloadbalancersresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ZONE_ID, type = CommandType.UUID, entityType = ZoneResponse.class, description = "zone Id")
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
    F5ExternalLoadBalancerElementService _f5DeviceManagerService;

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public void execute() {
        List<? extends Host> externalLoadBalancers = _f5DeviceManagerService.listExternalLoadBalancers(this);
        ListResponse<ExternalLoadBalancerResponse> listResponse = new ListResponse<ExternalLoadBalancerResponse>();
        List<ExternalLoadBalancerResponse> responses = new ArrayList<ExternalLoadBalancerResponse>();
        for (Host externalLoadBalancer : externalLoadBalancers) {
            ExternalLoadBalancerResponse response = _f5DeviceManagerService.createExternalLoadBalancerResponse(externalLoadBalancer);
            response.setObjectName("externalloadbalancer");
            response.setResponseName(getCommandName());
            responses.add(response);
        }

        listResponse.setResponses(responses);
        listResponse.setResponseName(getCommandName());
        this.setResponseObject(listResponse);
    }
}

/**
 * *  Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved
*
 *
 * This software is licensed under the GNU General Public License v3 or later.
 *
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.cloud.api.commands;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.cloud.api.ApiConstants;
import com.cloud.api.BaseListCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.BaseCmd.CommandType;
import com.cloud.api.response.HostResponse;
import com.cloud.api.response.ListResponse;
import com.cloud.host.Host;
import com.cloud.network.ExternalNetworkDeviceManager;
import com.cloud.server.ManagementService;
import com.cloud.server.api.response.ExternalLoadBalancerResponse;
import com.cloud.utils.component.ComponentLocator;

@Implementation(description="List external load balancer appliances.", responseObject = HostResponse.class)
public class ListExternalLoadBalancersCmd extends BaseListCmd {
	public static final Logger s_logger = Logger.getLogger(ListExternalLoadBalancersCmd.class.getName());
    private static final String s_name = "listexternalloadbalancersresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.ZONE_ID, type=CommandType.LONG, description="zone Id")
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

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public void execute(){
        ComponentLocator locator = ComponentLocator.getLocator(ManagementService.Name);
        ExternalNetworkDeviceManager externalNetworkMgr = locator.getManager(ExternalNetworkDeviceManager.class);
    	List<? extends Host> externalLoadBalancers = externalNetworkMgr.listExternalLoadBalancers(this);

        ListResponse<ExternalLoadBalancerResponse> listResponse = new ListResponse<ExternalLoadBalancerResponse>();
        List<ExternalLoadBalancerResponse> responses = new ArrayList<ExternalLoadBalancerResponse>();
        for (Host externalLoadBalancer : externalLoadBalancers) {
        	ExternalLoadBalancerResponse response = externalNetworkMgr.createExternalLoadBalancerResponse(externalLoadBalancer);
        	response.setObjectName("externalloadbalancer");
        	response.setResponseName(getCommandName());
        	responses.add(response);
        }

        listResponse.setResponses(responses);
        listResponse.setResponseName(getCommandName());
        this.setResponseObject(listResponse);
    }
}

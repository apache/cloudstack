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
import com.cloud.api.IdentityMapper;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.PlugService;
import com.cloud.api.response.HostResponse;
import com.cloud.api.response.ListResponse;
import com.cloud.host.Host;
import com.cloud.network.element.F5ExternalLoadBalancerElementService;
import com.cloud.server.api.response.ExternalLoadBalancerResponse;

@Implementation(description="Lists F5 external load balancer appliances added in a zone.", responseObject = HostResponse.class)
@Deprecated // API supported for backward compatibility.
public class ListExternalLoadBalancersCmd extends BaseListCmd {
	public static final Logger s_logger = Logger.getLogger(ListExternalLoadBalancersCmd.class.getName());
    private static final String s_name = "listexternalloadbalancersresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @IdentityMapper(entityTableName="data_center")
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

    @PlugService
    F5ExternalLoadBalancerElementService _f5DeviceManagerService;

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public void execute(){
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

/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
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
import com.cloud.api.response.DomainRouterResponse;
import com.cloud.api.response.ListResponse;
import com.cloud.async.AsyncJob;
import com.cloud.network.router.VirtualRouter;

@Implementation(description="List routers.", responseObject=DomainRouterResponse.class)
public class ListRoutersCmd extends BaseListCmd {
    public static final Logger s_logger = Logger.getLogger(ListRoutersCmd.class.getName());

    private static final String s_name = "listroutersresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.ACCOUNT, type=CommandType.STRING, description="the name of the account associated with the router. Must be used with the domainId parameter.")
    private String accountName;

    @Parameter(name=ApiConstants.DOMAIN_ID, type=CommandType.LONG, description="the domain ID associated with the router. If used with the account parameter, lists all routers associated with an account in the specified domain.")
    private Long domainId;

    @Parameter(name=ApiConstants.HOST_ID, type=CommandType.LONG, description="the host ID of the router")
    private Long hostId;

    @Parameter(name=ApiConstants.ID, type=CommandType.LONG, description="the ID of the disk router")
    private Long id;
    
    @Parameter(name=ApiConstants.NAME, type=CommandType.STRING, description="the name of the router")
    private String routerName;

    @Parameter(name=ApiConstants.POD_ID, type=CommandType.LONG, description="the Pod ID of the router")
    private Long podId;

    @Parameter(name=ApiConstants.STATE, type=CommandType.STRING, description="the state of the router")
    private String state;

    @Parameter(name=ApiConstants.ZONE_ID, type=CommandType.LONG, description="the Zone ID of the router")
    private Long zoneId;
    
    @Parameter(name=ApiConstants.NETWORK_ID, type=CommandType.LONG, description="list by network id")
    private Long networkId;
    
    @Parameter(name=ApiConstants.PROJECT_ID, type=CommandType.LONG, description="list firewall rules by project")
    private Long projectId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getAccountName() {
        return accountName;
    }

    public Long getDomainId() {
        return domainId;
    }

    public Long getHostId() {
        return hostId;
    }

    public Long getId() {
        return id;
    }
    
    public String getRouterName() {
        return routerName;
    }

    public Long getPodId() {
        return podId;
    }

    public String getState() {
        return state;
    }

    public Long getZoneId() {
        return zoneId;
    }
    
    public Long getNetworkId() {
        return networkId;
    }
    
    public Long getProjectId() {
        return projectId;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }
    
    public AsyncJob.Type getInstanceType() {
    	return AsyncJob.Type.DomainRouter;
    }

    @Override
    public void execute(){
        List<? extends VirtualRouter> result = _mgr.searchForRouters(this);
        ListResponse<DomainRouterResponse> response = new ListResponse<DomainRouterResponse>();
        List<DomainRouterResponse> routerResponses = new ArrayList<DomainRouterResponse>();
        for (VirtualRouter router : result) {
            DomainRouterResponse routerResponse = _responseGenerator.createDomainRouterResponse(router);
            routerResponse.setObjectName("router");
            routerResponses.add(routerResponse);
        }

        response.setResponses(routerResponses);
        response.setResponseName(getCommandName());
        this.setResponseObject(response);
    }
}

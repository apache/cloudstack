/**
 *  Copyright (C) 2011 Cloud.com, Inc.  All rights reserved.
 */

package com.cloud.api.commands;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.cloud.api.ApiConstants;
import com.cloud.api.BaseListCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.response.HostResponse;
import com.cloud.api.response.ListResponse;
import com.cloud.host.Host;
import com.cloud.network.ExternalLoadBalancerManager;
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
        ExternalLoadBalancerManager externalLoadBalancerMgr = locator.getManager(ExternalLoadBalancerManager.class);
    	List<? extends Host> externalLoadBalancers = externalLoadBalancerMgr.listExternalLoadBalancers(this);

        ListResponse<ExternalLoadBalancerResponse> listResponse = new ListResponse<ExternalLoadBalancerResponse>();
        List<ExternalLoadBalancerResponse> responses = new ArrayList<ExternalLoadBalancerResponse>();
        for (Host externalLoadBalancer : externalLoadBalancers) {
        	ExternalLoadBalancerResponse response = externalLoadBalancerMgr.getApiResponse(externalLoadBalancer);
        	response.setObjectName("externalloadbalancer");
        	response.setResponseName(getCommandName());
        	responses.add(response);
        }

        listResponse.setResponses(responses);
        listResponse.setResponseName(getCommandName());
        this.setResponseObject(listResponse);
    }
}

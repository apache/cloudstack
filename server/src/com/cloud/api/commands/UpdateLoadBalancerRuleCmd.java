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

import org.apache.log4j.Logger;

import com.cloud.api.ApiConstants;
import com.cloud.api.ApiDBUtils;
import com.cloud.api.ApiResponseHelper;
import com.cloud.api.BaseAsyncCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.response.LoadBalancerResponse;
import com.cloud.event.EventTypes;
import com.cloud.network.LoadBalancerVO;
import com.cloud.network.NetworkManager;
import com.cloud.user.Account;

@Implementation(method="updateLoadBalancerRule", manager=NetworkManager.class)
public class UpdateLoadBalancerRuleCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(UpdateLoadBalancerRuleCmd.class.getName());
    private static final String s_name = "updateloadbalancerruleresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.ALGORITHM, type=CommandType.STRING, description="load balancer algorithm (source, roundrobin, leastconn)")
    private String algorithm;

    @Parameter(name=ApiConstants.DESCRIPTION, type=CommandType.STRING, description="the description of the load balancer rule")
    private String description;

    @Parameter(name=ApiConstants.ID, type=CommandType.LONG, required=true, description="the id of the load balancer rule to update")
    private Long id;

    @Parameter(name=ApiConstants.NAME, type=CommandType.STRING, description="the name of the load balancer rule")
    private String loadBalancerName;

    @Parameter(name=ApiConstants.PRIVATE_PORT, type=CommandType.STRING, description="the private port of the private ip address/virtual machine where the network traffic will be load balanced to")
    private String privatePort;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getAlgorithm() {
        return algorithm;
    }

    public String getDescription() {
        return description;
    }

    public Long getId() {
        return id;
    }

    public String getLoadBalancerName() {
        return loadBalancerName;
    }

    public String getPrivatePort() {
        return privatePort;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getName() {
        return s_name;
    }

    @Override
    public long getAccountId() {
        LoadBalancerVO lb = ApiDBUtils.findLoadBalancerById(getId());
        if (lb == null) {
            return Account.ACCOUNT_ID_SYSTEM; // bad id given, parent this command to SYSTEM so ERROR events are tracked
        }
        return lb.getAccountId();
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_LOAD_BALANCER_UPDATE;
    }

    @Override
    public String getEventDescription() {
        return  "updating load balancer rule";
    }

	@Override @SuppressWarnings("unchecked")
	public LoadBalancerResponse getResponse() {
	    LoadBalancerVO loadBalancer = (LoadBalancerVO)getResponseObject();
	    LoadBalancerResponse response = ApiResponseHelper.createLoadBalancerResponse(loadBalancer);
        response.setResponseName("loadbalancerrule");
        return response;
	}
}

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

import java.util.List;

import org.apache.log4j.Logger;

import com.cloud.api.ApiConstants;
import com.cloud.api.ApiDBUtils;
import com.cloud.api.BaseAsyncCmd;
import com.cloud.api.BaseCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.SuccessResponse;
import com.cloud.event.EventTypes;
import com.cloud.network.LoadBalancerVO;
import com.cloud.network.NetworkManager;
import com.cloud.user.Account;

@Implementation(method="assignToLoadBalancer", manager=NetworkManager.class, description="Assigns virtual machine or a list of virtual machines to a load balancer rule.")
public class AssignToLoadBalancerRuleCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(AssignToLoadBalancerRuleCmd.class.getName());

    private static final String s_name = "assigntoloadbalancerruleresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.ID, type=CommandType.LONG, required=true, description="the ID of the load balancer rule")
    private Long id;

    @Parameter(name=ApiConstants.VIRTUAL_MACHINE_ID, type=CommandType.LONG, required=false, description="the ID of the virtual machine that is being assigned to the load balancer rule")
    private Long virtualMachineId;

    @Parameter(name=ApiConstants.VIRTUAL_MACHINE_IDS, type=CommandType.LIST, collectionType=CommandType.LONG, required=false, description="the list of IDs of the virtual machine that are being assigned to the load balancer rule(i.e. virtualMachineIds=1,2,3)")
    private List<Long> virtualMachineIds;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getLoadBalancerId() {
        return id;
    }

    public Long getVirtualMachineId() {
        return virtualMachineId;
    }

    public List<Long> getVirtualMachineIds() {
        return virtualMachineIds;
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
        LoadBalancerVO lb = ApiDBUtils.findLoadBalancerById(getLoadBalancerId());
        if (lb == null) {
            return Account.ACCOUNT_ID_SYSTEM; // bad id given, parent this command to SYSTEM so ERROR events are tracked
        }
        return lb.getAccountId();
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_ASSIGN_TO_LOAD_BALANCER_RULE;
    }

    @Override
    public String getEventDescription() {
        return "applying port forwarding service for vm with id: " + getVirtualMachineId();
    }

    @Override @SuppressWarnings("unchecked")
    public SuccessResponse getResponse() {
    	if (getResponseObject() == null || (Boolean)getResponseObject()) {
	    	return new SuccessResponse(getName());
	    } else {
	    	throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to assign to load balancer");
	    }
    }
}

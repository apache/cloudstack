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
import com.cloud.api.BaseAsyncCmd;
import com.cloud.api.BaseCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.SuccessResponse;
import com.cloud.event.EventTypes;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.network.rules.LoadBalancer;
import com.cloud.user.Account;
import com.cloud.utils.StringUtils;

@Implementation(description="Removes a virtual machine or a list of virtual machines from a load balancer rule.", responseObject=SuccessResponse.class)
public class RemoveFromLoadBalancerRuleCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(RemoveFromLoadBalancerRuleCmd.class.getName());

    private static final String s_name = "removefromloadbalancerruleresponse";
 
    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.ID, type=CommandType.LONG, required=true, description="The ID of the load balancer rule")
    private Long id;

    @Parameter(name=ApiConstants.VIRTUAL_MACHINE_ID, type=CommandType.LONG, description="the ID of the virtual machine that is being removed from the load balancer rule")
    private Long virtualMachineId;

    @Parameter(name=ApiConstants.VIRTUAL_MACHINE_IDS, type=CommandType.LIST, collectionType=CommandType.LONG, description="the list of IDs of the virtual machines that are being removed from the load balancer rule (i.e. virtualMachineIds=1,2,3)")
    private List<Long> virtualMachineIds;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
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
    public String getCommandName() {
        return s_name;
    }

    @Override
    public long getEntityOwnerId() {
        LoadBalancer lb = _entityMgr.findById(LoadBalancer.class, getId());
        if (lb == null) {
            return Account.ACCOUNT_ID_SYSTEM; // bad id given, parent this command to SYSTEM so ERROR events are tracked
        }
        return lb.getAccountId();
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_REMOVE_FROM_LOAD_BALANCER_RULE;
    }

    @Override
    public String getEventDescription() {
        List<Long> vmIds = getVirtualMachineIds();
        if ((vmIds == null) || vmIds.isEmpty()) {
            vmIds = new ArrayList<Long>();
            vmIds.add(getVirtualMachineId());
        }

        return  "removing instances from load balancer: " + getId() + " (ids: " + StringUtils.join(vmIds, ",") + ")";
    }

    @Override
    public void execute(){
        if (virtualMachineIds == null && virtualMachineId == null) {
            throw new InvalidParameterValueException("Must specify virtual machine id");
        } 
        if (virtualMachineIds == null) {
            virtualMachineIds = new ArrayList<Long>();
        }
        
        if (virtualMachineId != null) {
            virtualMachineIds.add(virtualMachineId);
        }
        
        boolean result = _lbService.removeFromLoadBalancer(id, virtualMachineIds);
        if (result) {
            SuccessResponse response = new SuccessResponse(getCommandName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to remove instance from load balancer rule");
        }
    }
}

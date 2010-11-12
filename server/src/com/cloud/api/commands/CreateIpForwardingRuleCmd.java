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
import com.cloud.api.ApiResponseHelper;
import com.cloud.api.BaseAsyncCreateCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.FirewallRuleResponse;
import com.cloud.event.EventTypes;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.FirewallRuleVO;
import com.cloud.user.Account;

@Implementation(description="Creates an ip forwarding rule")
public class CreateIpForwardingRuleCmd extends BaseAsyncCreateCmd {
    public static final Logger s_logger = Logger.getLogger(CreateIpForwardingRuleCmd.class.getName());

    private static final String s_name = "createipforwardingruleresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.IP_ADDRESS, type=CommandType.STRING, required=true, description="the public IP address of the forwarding rule, already associated via associateIp")
    private String ipAddress;

    @Parameter(name=ApiConstants.VIRTUAL_MACHINE_ID, type=CommandType.LONG, required=true, description="the ID of the virtual machine for the forwarding rule")
    private Long virtualMachineId;


    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getIpAddress() {
        return ipAddress;
    }

    public Long getVirtualMachineId() {
        return virtualMachineId;
    }


    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getName() {
        return s_name;
    }

    @Override
    public void execute() throws ServerApiException, InvalidParameterValueException, PermissionDeniedException, InsufficientAddressCapacityException, InsufficientCapacityException, ConcurrentOperationException{ 
        FirewallRuleVO result = _networkMgr.createIpForwardingRuleOnDomr(this.getId());
        if (result != null) {
            FirewallRuleResponse fwResponse = ApiResponseHelper.createFirewallRuleResponse(result);
            fwResponse.setResponseName(getName());
            this.setResponseObject(fwResponse);
        } else {
            throw new ServerApiException(NET_CREATE_IPFW_RULE_ERROR, "Error in creating ip forwarding rule on the domr");
        }
       
    }

	@Override
	public void callCreate() throws ServerApiException,InvalidParameterValueException, PermissionDeniedException,InsufficientAddressCapacityException,InsufficientCapacityException, ResourceUnavailableException,ConcurrentOperationException, ResourceAllocationException{
		FirewallRuleVO rule = _networkMgr.createIpForwardingRuleInDb(ipAddress,virtualMachineId);
        this.setId(rule.getId());
	}

    @Override
    public long getAccountId() {
        return Account.ACCOUNT_ID_SYSTEM; // no account info given, parent this command to SYSTEM so ERROR events are tracked
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_NET_RULE_ADD;
    }

    @Override
    public String getEventDescription() {
        return  ("Creating an ipforwarding 1:1 NAT rule for "+ipAddress+" with virtual machine:"+virtualMachineId);
    }

}

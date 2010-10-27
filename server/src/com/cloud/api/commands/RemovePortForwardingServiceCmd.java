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

import com.cloud.api.ApiDBUtils;
import com.cloud.api.BaseAsyncCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.response.SuccessResponse;
import com.cloud.event.EventTypes;
import com.cloud.network.SecurityGroupVO;
import com.cloud.server.ManagementServer;
import com.cloud.user.Account;

@Implementation(method="removeSecurityGroup", manager=ManagementServer.class, description="Removes a port forwarding service from a virtual machine")
public class RemovePortForwardingServiceCmd extends BaseAsyncCmd {
	public static final Logger s_logger = Logger.getLogger(RemovePortForwardingServiceCmd.class.getName());

    private static final String s_name = "removeportforwardingserviceresponse";
 
    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name="id", type=CommandType.LONG, required=true, description="the ID of the port forwarding service to remove from the virtual machine/publicIp")
    private Long id;

    @Parameter(name="publicip", type=CommandType.STRING, required=true, description="the public IP address associated with the port forwarding service")
    private String publicIp;

    @Parameter(name="virtualmachineid", type=CommandType.LONG, required=true, description="the virtual machine currently assigned to the port forwarding service")
    private Long virtualMachineId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public String getPublicIp() {
        return publicIp;
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
    public long getAccountId() {
        SecurityGroupVO sg = ApiDBUtils.findPortForwardingServiceById(getId());
        if (sg != null) {
            return sg.getAccountId();
        }

        return Account.ACCOUNT_ID_SYSTEM; // no account info given, parent this command to SYSTEM so ERROR events are tracked
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_PORT_FORWARDING_SERVICE_REMOVE;
    }

    @Override
    public String getEventDescription() {
        return  "removing port forwarding service: " + getId() + " from vm: " + getVirtualMachineId() + " on IP: " + getPublicIp();
    }

	@Override @SuppressWarnings("unchecked")
	public SuccessResponse getResponse() {
        Boolean success = (Boolean)getResponseObject();
        SuccessResponse response = new SuccessResponse();
        response.setSuccess(success);
        response.setResponseName(getName());
        return response;
	}
}

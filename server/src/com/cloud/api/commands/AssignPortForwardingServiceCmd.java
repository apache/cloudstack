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

import com.cloud.api.ApiDBUtils;
import com.cloud.api.BaseAsyncCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.response.SuccessResponse;
import com.cloud.event.EventTypes;
import com.cloud.network.SecurityGroupVO;
import com.cloud.server.ManagementServer;
import com.cloud.user.Account;

@Implementation(method="assignSecurityGroup", manager=ManagementServer.class, description="Assigns a single or a list of port forwarding services to a virtual machine. If a list of port forwarding services is given, it will overwrite the previous assignment of port forwarding services. For example, on the first call, if you assigned port forwarding service A to virtual machine 1 and on the next call, you assign port forwarding services B and C to virtual machine 1, the ultimate result of these two commands would be that virtual machine 1 would only have port forwarding services B and C assigned to it. Individual port forwarding services can be assigned to the virtual machine by specifying a single port forwarding service group.")
public class AssignPortForwardingServiceCmd extends BaseAsyncCmd {
	public static final Logger s_logger = Logger.getLogger(AssignPortForwardingServiceCmd.class.getName());
	
    private static final String s_name = "assignportforwardingserviceresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name="id", type=CommandType.LONG, description="the ID of the port forwarding service to assign to the virtual machine/public IP")
    private Long id;

    @Parameter(name="ids", type=CommandType.LIST, collectionType=CommandType.LONG, description="a comma delimited list of port forwarding service IDs to assign to the virtual machine/public IP")
    private List<Long> ids;

    @Parameter(name="publicip", type=CommandType.STRING, required=true, description="the public IP address to associate to the port forwarding service")
    private String publicIp;

    @Parameter(name="virtualmachineid", type=CommandType.LONG, required=true, description="the ID of the virtual machine to assign to the port forwarding service")
    private Long virtualMachineId;


    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public List<Long> getIds() {
        return ids;
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
        if (sg == null) {
            return Account.ACCOUNT_ID_SYSTEM; // bad id given, parent this command to SYSTEM so ERROR events are tracked
        }
        return sg.getAccountId();
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_PORT_FORWARDING_SERVICE_APPLY;
    }

    @Override
    public String getEventDescription() {
        return "applying port forwarding service for vm with id: " + getVirtualMachineId();
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

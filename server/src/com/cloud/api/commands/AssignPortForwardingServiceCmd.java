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

import com.cloud.api.BaseAsyncCmd;
import com.cloud.api.BaseCmd.Manager;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ResponseObject;
import com.cloud.api.response.SuccessResponse;

@Implementation(method="assignSecurityGroup", manager=Manager.ManagementServer)
public class AssignPortForwardingServiceCmd extends BaseAsyncCmd {
	public static final Logger s_logger = Logger.getLogger(AssignPortForwardingServiceCmd.class.getName());
	
    private static final String s_name = "assignportforwardingserviceresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name="id", type=CommandType.STRING)
    private Long id;

    @Parameter(name="ids", type=CommandType.LIST, collectionType=CommandType.LONG)
    private List<Long> ids;

    @Parameter(name="publicip", type=CommandType.STRING, required=true)
    private String publicIp;

    @Parameter(name="virtualmachineid", type=CommandType.LONG, required=true)
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
	public ResponseObject getResponse() {
		Boolean success = (Boolean)getResponseObject();
		SuccessResponse response = new SuccessResponse();
		response.setSuccess(success);
		response.setResponseName(getName());
		return response;
	}
}

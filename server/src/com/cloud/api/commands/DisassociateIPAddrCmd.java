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
import com.cloud.api.BaseCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.SuccessResponse;
import com.cloud.network.NetworkManager;

@Implementation(method="disassociateIpAddress", manager=NetworkManager.class, description="Disassociates an ip address from the account.")
public class DisassociateIPAddrCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(DisassociateIPAddrCmd.class.getName());

    private static final String s_name = "disassociateipaddressresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.IP_ADDRESS, type=CommandType.STRING, required=true, description="the public ip address to disassociate")
    private String ipAddress;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getIpAddress() {
        return ipAddress;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getName() {
        return s_name;
    }

	@Override @SuppressWarnings("unchecked")
	public SuccessResponse getResponse() {
		if (getResponseObject() == null || (Boolean)getResponseObject()) {
	    	return new SuccessResponse(getName());
	    } else {
	    	throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to disassociate ip address");
	    }
	}
}

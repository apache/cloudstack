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

import java.util.Map;

import org.apache.log4j.Logger;

import com.cloud.api.BaseCmd;
import com.cloud.api.Implementation;
import com.cloud.api.response.CapabilitiesResponse;

@Implementation(description="Lists capabilities", responseObject=CapabilitiesResponse.class)
public class ListCapabilitiesCmd extends BaseCmd {
	public static final Logger s_logger = Logger.getLogger(ListCapabilitiesCmd.class.getName());

    private static final String s_name = "listcapabilitiesresponse";

    @Override
    public String getName() {
        return s_name;
    }
    
    @Override
    public void execute(){
        Map<String, String> capabilities = _mgr.listCapabilities(this);
        CapabilitiesResponse response = new CapabilitiesResponse();
        response.setNetworkGroupsEnabled(capabilities.get("networkGroupsEnabled"));
        response.setCloudStackVersion(capabilities.get("cloudStackVersion"));
        response.setObjectName("capability");
        response.setResponseName(getName());
        this.setResponseObject(response);
    }
}

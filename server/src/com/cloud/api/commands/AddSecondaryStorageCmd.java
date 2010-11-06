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

import com.cloud.agent.AgentManager;
import com.cloud.api.ApiConstants;
import com.cloud.api.ApiResponseHelper;
import com.cloud.api.BaseCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.HostResponse;
import com.cloud.api.response.ListResponse;
import com.cloud.host.HostVO;

@Implementation(method="discoverHosts", manager=AgentManager.class, description="Adds secondary storage.")
public class AddSecondaryStorageCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(AddSecondaryStorageCmd.class.getName());
    private static final String s_name = "addsecondarystorageresponse";
     
    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.URL, type=CommandType.STRING, required=true, description="the URL for the secondary storage")
    private String url;

    @Parameter(name=ApiConstants.ZONE_ID, type=CommandType.LONG, description="the Zone ID for the secondary storage")
    private Long zoneId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getUrl() {
        return url;
    }

    public Long getZoneId() {
        return zoneId;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getName() {
    	return s_name;
    }
    
    @Override @SuppressWarnings("unchecked")
    public HostResponse getResponse() {
		List<HostVO> hosts = (List<HostVO>)getResponseObject();
		HostResponse hostResponse = null;
	    if (hosts != null && hosts.size() > 0) {
	        for (HostVO host : hosts) {
	        	// There should only be one secondary storage host per add
	        	hostResponse = ApiResponseHelper.createHostResponse(host);
	            hostResponse.setResponseName(getName());
	            hostResponse.setObjectName("secondarystorage");
	            return hostResponse;
	        }
	    } else {
	        throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to add secondary storage");
	    }
	    return hostResponse;
    }
}

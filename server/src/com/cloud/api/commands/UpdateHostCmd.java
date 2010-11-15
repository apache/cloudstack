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
import com.cloud.api.BaseCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.HostResponse;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.host.HostVO;

@Implementation(description="Updates a host.", responseObject=HostResponse.class)
public class UpdateHostCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(UpdateHostCmd.class.getName());
    private static final String s_name = "updatehostresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.ID, type=CommandType.LONG, required=true, description="the ID of the host to update")
    private Long id;

    @Parameter(name=ApiConstants.OS_CATEGORY_ID, type=CommandType.LONG, description="the id of Os category to update the host with")
    private Long osCategoryId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public Long getOsCategoryId() {
        return osCategoryId;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////
    
    @Override
    public String getName() {
        return s_name;
    }
   
    public static String getResultObjectName() {
    	return "updatehost";
    }
    
    @Override
    public void execute() throws ServerApiException, InvalidParameterValueException, PermissionDeniedException, InsufficientAddressCapacityException, InsufficientCapacityException, ConcurrentOperationException{
        HostVO result = _agentMgr.updateHost(this);
        if (result != null) {
            HostResponse hostResponse = ApiResponseHelper.createHostResponse(result);
            hostResponse.setResponseName(getName());
            this.setResponseObject(hostResponse);
        } else {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to update host");
        }   
    }
}

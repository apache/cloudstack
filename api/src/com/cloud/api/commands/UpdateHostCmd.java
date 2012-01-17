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
import com.cloud.api.BaseCmd;
import com.cloud.api.IdentityMapper;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.HostResponse;
import com.cloud.host.Host;
import com.cloud.user.Account;
import com.cloud.utils.fsm.NoTransitionException;

@Implementation(description="Updates a host.", responseObject=HostResponse.class)
public class UpdateHostCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(UpdateHostCmd.class.getName());
    private static final String s_name = "updatehostresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @IdentityMapper(entityTableName="host")
    @Parameter(name=ApiConstants.ID, type=CommandType.LONG, required=true, description="the ID of the host to update")
    private Long id;

    @IdentityMapper(entityTableName="guest_os_category")
    @Parameter(name=ApiConstants.OS_CATEGORY_ID, type=CommandType.LONG, description="the id of Os category to update the host with")
    private Long osCategoryId;

    @Parameter(name=ApiConstants.ALLOCATION_STATE, type=CommandType.STRING, description="Change resource state of host, valid values are [Enable, Disable]. Operation may failed if host in states not allowing Enable/Disable")
    private String allocationState;

    @Parameter(name=ApiConstants.HOST_TAGS, type=CommandType.LIST, collectionType=CommandType.STRING, description="list of tags to be added to the host")
    private List<String> hostTags;
    
    @Parameter(name=ApiConstants.URL, type=CommandType.STRING, description="the new uri for the secondary storage: nfs://host/path")
    private String url;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public Long getOsCategoryId() {
        return osCategoryId;
    }

    public String getAllocationState() {
        return allocationState;
    }

    public List<String> getHostTags() {
        return hostTags;
    }    
    
    public String getUrl() {
    	return url;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }

    public static String getResultObjectName() {
        return "updatehost";
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public void execute(){
        Host result;
        try {
	        result = _resourceService.updateHost(this);
            HostResponse hostResponse = _responseGenerator.createHostResponse(result);
            hostResponse.setResponseName(getCommandName());
            this.setResponseObject(hostResponse);
        } catch (Exception e) {
        	s_logger.debug("Failed to update host:" + getId(), e);
        	throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to update host:" + getId() + "," + e.getMessage());
        }
    }
}

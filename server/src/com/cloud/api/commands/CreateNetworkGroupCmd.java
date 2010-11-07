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
import com.cloud.api.ApiDBUtils;
import com.cloud.api.BaseCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.NetworkGroupResponse;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.network.security.NetworkGroupManager;
import com.cloud.network.security.NetworkGroupVO;

@Implementation(method="createNetworkGroup", manager=NetworkGroupManager.class)
public class CreateNetworkGroupCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(CreateNetworkGroupCmd.class.getName());

    private static final String s_name = "createsecuritygroupresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.ACCOUNT, type=CommandType.STRING, description="an optional account for the network group. Must be used with domainId.")
    private String accountName;

    @Parameter(name=ApiConstants.DESCRIPTION, type=CommandType.STRING, description="the description of the network group")
    private String description;

    @Parameter(name=ApiConstants.DOMAIN_ID, type=CommandType.LONG, description="an optional domainId for the network group. If the account parameter is used, domainId must also be used.")
    private Long domainId;

    @Parameter(name=ApiConstants.NAME, type=CommandType.STRING, required=true, description="name of the network group")
    private String networkGroupName;


    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getAccountName() {
        return accountName;
    }

    public String getDescription() {
        return description;
    }

    public Long getDomainId() {
        return domainId;
    }

    public String getNetworkGroupName() {
        return networkGroupName;
    }


    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    public String getName() {
        return s_name;
    }

    @Override @SuppressWarnings("unchecked")
    public NetworkGroupResponse getResponse() {
        NetworkGroupVO group = (NetworkGroupVO)getResponseObject();

        NetworkGroupResponse response = new NetworkGroupResponse();
        response.setAccountName(group.getAccountName());
        response.setDescription(group.getDescription());
        response.setDomainId(group.getDomainId());
        response.setDomainName(ApiDBUtils.findDomainById(group.getDomainId()).getName());
        response.setId(group.getId());
        response.setName(group.getName());

        response.setResponseName(getName());
        response.setObjectName("securitygroup");
        return response;
    }
    
    @Override
    public Object execute() throws ServerApiException, InvalidParameterValueException, PermissionDeniedException, InsufficientAddressCapacityException, InsufficientCapacityException, ConcurrentOperationException{
        NetworkGroupVO result = _networkGroupMgr.createNetworkGroup(this);
        return result;
    }
}

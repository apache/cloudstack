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

import com.cloud.api.ApiConstants;
import com.cloud.api.ApiResponseHelper;
import com.cloud.api.BaseListCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.ListResponse;
import com.cloud.api.response.UserVmResponse;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.uservm.UserVm;
import com.cloud.vm.UserVmVO;

@Implementation(description="List the virtual machines owned by the account.")
public class ListVMsCmd extends BaseListCmd {
    public static final Logger s_logger = Logger.getLogger(ListVMsCmd.class.getName());

    private static final String s_name = "listvirtualmachinesresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.ACCOUNT, type=CommandType.STRING, description="account. Must be used with the domainId parameter.")
    private String accountName;

    @Parameter(name=ApiConstants.DOMAIN_ID, type=CommandType.LONG, description="the domain ID. If used with the account parameter, lists virtual machines for the specified account in this domain.")
    private Long domainId;

    @Parameter(name=ApiConstants.GROUP_ID, type=CommandType.LONG, description="the group ID")
    private Long groupId;

    @Parameter(name=ApiConstants.HOST_ID, type=CommandType.LONG, description="the host ID")
    private Long hostId;

    @Parameter(name=ApiConstants.ID, type=CommandType.LONG, description="the ID of the virtual machine")
    private Long id;

    @Parameter(name=ApiConstants.NAME, type=CommandType.STRING, description="name of the virtual machine")
    private String instanceName;

    @Parameter(name=ApiConstants.POD_ID, type=CommandType.LONG, description="the pod ID")
    private Long podId;

    @Parameter(name=ApiConstants.STATE, type=CommandType.STRING, description="state of the virtual machine")
    private String state;

    @Parameter(name=ApiConstants.ZONE_ID, type=CommandType.LONG, description="the availability zone ID")
    private Long zoneId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getAccountName() {
        return accountName;
    }

    public Long getDomainId() {
        return domainId;
    }

    public Long getGroupId() {
        return groupId;
    }

    public Long getHostId() {
        return hostId;
    }

    public Long getId() {
        return id;
    }

    public String getInstanceName() {
        return instanceName;
    }

    public Long getPodId() {
        return podId;
    }

    public String getState() {
        return state;
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
    
    @Override
    public void execute() throws ServerApiException, InvalidParameterValueException, PermissionDeniedException, InsufficientAddressCapacityException, InsufficientCapacityException, ConcurrentOperationException{
        List<UserVmVO> result = _mgr.searchForUserVMs(this);
        ListResponse<UserVmResponse> response = new ListResponse<UserVmResponse>();
        List<UserVmResponse> vmResponses = new ArrayList<UserVmResponse>();
        for (UserVm userVm : result) {
            UserVmResponse userVmResponse = ApiResponseHelper.createUserVmResponse(userVm);
            if (userVmResponse != null) {
                userVmResponse.setObjectName("virtualmachine");
                vmResponses.add(userVmResponse);
            }
        }
        response.setResponses(vmResponses);
        response.setResponseName(getName());
        this.setResponseObject(response);
    }
    
}

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
import com.cloud.api.BaseAsyncCmd;
import com.cloud.api.BaseCmd;
import com.cloud.api.IdentityMapper;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.api.BaseCmd.CommandType;
import com.cloud.api.response.UserVmResponse;
import com.cloud.api.response.ZoneResponse;
import com.cloud.async.AsyncJob;
import com.cloud.event.EventTypes;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.user.Account;
import com.cloud.uservm.UserVm;

@Implementation(description="Move a user VM to another user under same domain.", responseObject=UserVmResponse.class)
public class AssignVMCmd extends BaseCmd  {
    public static final Logger s_logger = Logger.getLogger(AssignVMCmd.class.getName());

    private static final String s_name = "moveuservmresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.VIRTUAL_MACHINE_ID, type=CommandType.LONG, required=true, description="the vm ID of the user VM to be moved")
    private Long virtualMachineId;
    
    @Parameter(name=ApiConstants.ACCOUNT, type=CommandType.STRING, required=true, description="an optional account for the vpn user. Must be used with domainId.")
    private String accountName;
    
    @Parameter(name=ApiConstants.DOMAIN_ID, type=CommandType.LONG, required=true, description="an optional domainId for the vpn user. If the account parameter is used, domainId must also be used.")
    private Long domainId;

    //Network information
    @IdentityMapper(entityTableName="networks")
    @Parameter(name=ApiConstants.NETWORK_IDS, type=CommandType.LIST, collectionType=CommandType.LONG, description="list of network ids that will be part of VM network after move in advanced network setting.")
    private List<Long> networkIds;

    @IdentityMapper(entityTableName="security_group")
    @Parameter(name=ApiConstants.SECURITY_GROUP_IDS, type=CommandType.LIST, collectionType=CommandType.LONG, description="comma separated list of security groups id that going to be applied to the virtual machine. Should be passed only when vm is moved in a zone with Basic Network support.")
    private List<Long> securityGroupIdList;
    
    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getVmId() {
        return virtualMachineId;
    }

    public String getAccountName() {
        return accountName;
    }

	public Long getDomainId() {
		return domainId;
	}
	
	public List<Long> getNetworkIds() {
		return networkIds;
	}

	public List<Long> getSecurityGroupIdList() {
		return securityGroupIdList;
	}


    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }
    
    @Override
    public void execute(){
        try {
           UserVm userVm = _userVmService.moveVMToUser(this);
           if (userVm == null){
               throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to move vm");
           }  
           UserVmResponse response = _responseGenerator.createUserVmResponse("virtualmachine", userVm).get(0);            
           response.setResponseName(DeployVMCmd.getResultObjectName());           
           this.setResponseObject(response);
        }catch (Exception e){
            e.printStackTrace();
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to move vm " + e.getMessage());
        }
        
    }

    @Override
    public long getEntityOwnerId() {
        UserVm vm = _responseGenerator.findUserVmById(getVmId());
        if (vm != null) {
            return vm.getAccountId();
        }

        return Account.ACCOUNT_ID_SYSTEM; // no account info given, parent this command to SYSTEM so ERROR events are tracked
    }

}

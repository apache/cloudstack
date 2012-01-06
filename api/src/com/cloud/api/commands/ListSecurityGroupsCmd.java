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
import com.cloud.api.BaseCmd.CommandType;
import com.cloud.api.BaseListProjectAndAccountResourcesCmd;
import com.cloud.api.IdentityMapper;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.response.ListResponse;
import com.cloud.api.response.SecurityGroupResponse;
import com.cloud.async.AsyncJob;
import com.cloud.network.security.SecurityGroupRules;

@Implementation(description="Lists security groups", responseObject=SecurityGroupResponse.class)
public class ListSecurityGroupsCmd extends BaseListProjectAndAccountResourcesCmd {
	public static final Logger s_logger = Logger.getLogger(ListSecurityGroupsCmd.class.getName());

    private static final String s_name = "listsecuritygroupsresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.SECURITY_GROUP_NAME, type=CommandType.STRING, description="lists security groups by name")
    private String securityGroupName;

    @IdentityMapper(entityTableName="vm_instance")
    @Parameter(name=ApiConstants.VIRTUAL_MACHINE_ID, type=CommandType.LONG, description="lists security groups by virtual machine id")
    private Long virtualMachineId;

    @IdentityMapper(entityTableName="security_group")
    @Parameter(name=ApiConstants.ID, type=CommandType.LONG, description="list the security group by the id provided")
    private Long id;
    
    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////
    public String getSecurityGroupName() {
        return securityGroupName;
    }

    public Long getVirtualMachineId() {
        return virtualMachineId;
    }
    
    public Long getId(){
    	return id;
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
        List<? extends SecurityGroupRules> securityGroups = _securityGroupService.searchForSecurityGroupRules(this);

        ListResponse<SecurityGroupResponse> response = _responseGenerator.createSecurityGroupResponses(securityGroups);
        response.setResponseName(getCommandName());
        this.setResponseObject(response);
    }
    
    @Override
    public AsyncJob.Type getInstanceType() {
        return AsyncJob.Type.SecurityGroup;
    }
}

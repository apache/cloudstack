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
import com.cloud.api.BaseListCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.BaseCmd.CommandType;
import com.cloud.api.response.ListResponse;
import com.cloud.api.response.SecurityGroupResponse;
import com.cloud.async.AsyncJob;
import com.cloud.network.security.SecurityGroupRules;

@Implementation(description="Lists security groups", responseObject=SecurityGroupResponse.class)
public class ListSecurityGroupsCmd extends BaseListCmd {
	public static final Logger s_logger = Logger.getLogger(ListSecurityGroupsCmd.class.getName());

    private static final String s_name = "listsecuritygroupsresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.ACCOUNT, type=CommandType.STRING, description="lists all available port security groups for the account. Must be used with domainID parameter")
    private String accountName;

    @Parameter(name=ApiConstants.DOMAIN_ID, type=CommandType.LONG, description="lists all available security groups for the domain ID. If used with the account parameter, lists all available security groups for the account in the specified domain ID.")
    private Long domainId;

    @Parameter(name=ApiConstants.SECURITY_GROUP_NAME, type=CommandType.STRING, description="lists security groups by name")
    private String securityGroupName;

    @Parameter(name=ApiConstants.VIRTUAL_MACHINE_ID, type=CommandType.LONG, description="lists security groups by virtual machine id")
    private Long virtualMachineId;

    @Parameter(name=ApiConstants.ID, type=CommandType.LONG, description="list the security group by the id provided")
    private Long id;
    
    @Parameter(name=ApiConstants.PROJECT_ID, type=CommandType.LONG, description="list security groups by project")
    private Long projectId;
    
    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getAccountName() {
        return accountName;
    }

    public Long getDomainId() {
        return domainId;
    }

    public String getSecurityGroupName() {
        return securityGroupName;
    }

    public Long getVirtualMachineId() {
        return virtualMachineId;
    }
    
    public Long getId(){
    	return id;
    }
    
    public Long getProjectId() {
        return projectId;
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

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
import com.cloud.api.response.ListResponse;
import com.cloud.api.response.NetworkGroupResponse;
import com.cloud.network.security.NetworkGroupRules;

@Implementation(description="Lists network groups", responseObject=NetworkGroupResponse.class)
public class ListNetworkGroupsCmd extends BaseListCmd {
	public static final Logger s_logger = Logger.getLogger(ListNetworkGroupsCmd.class.getName());

    private static final String s_name = "listnetworkgroupsresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.ACCOUNT, type=CommandType.STRING, description="lists all available port network groups for the account. Must be used with domainID parameter")
    private String accountName;

    @Parameter(name=ApiConstants.DOMAIN_ID, type=CommandType.LONG, description="lists all available network groups for the domain ID. If used with the account parameter, lists all available network groups for the account in the specified domain ID.")
    private Long domainId;

    @Parameter(name=ApiConstants.NETWORK_GROUP_NAME, type=CommandType.STRING, description="lists network groups by name")
    private String networkGroupName;

    @Parameter(name=ApiConstants.VIRTUAL_MACHINE_ID, type=CommandType.LONG, description="lists network groups by virtual machine id")
    private Long virtualMachineId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getAccountName() {
        return accountName;
    }

    public Long getDomainId() {
        return domainId;
    }

    public String getNetworkGroupName() {
        return networkGroupName;
    }

    public Long getVirtualMachineId() {
        return virtualMachineId;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getName() {
        return s_name;
    }

    @Override
    public void execute(){
        List<? extends NetworkGroupRules> networkGroups = _networkGroupMgr.searchForNetworkGroupRules(this);

        ListResponse<NetworkGroupResponse> response = _responseGenerator.createNetworkGroupResponses(networkGroups);
        response.setResponseName(getName());
        this.setResponseObject(response);
    }
}

/**
 *  Copyright (C) 2011 Citrix, Inc.  All rights reserved.
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
import com.cloud.api.response.UserVmResponse;
import com.cloud.async.AsyncJob;
import com.cloud.uservm.UserVm;

@Implementation(description="List summary of the virtual machines owned by the account.", responseObject=UserVmResponse.class)
public class VMsSummaryCmd extends BaseListCmd {
    public static final Logger s_logger = Logger.getLogger(VMsSummaryCmd.class.getName());

    private static final String s_name = "listvirtualmachinessummaryresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.ACCOUNT, type=CommandType.STRING, description="account. Must be used with the domainId parameter.")
    private String accountName;

    @Parameter(name=ApiConstants.DOMAIN_ID, type=CommandType.LONG, description="the domain ID. If used with the account parameter, lists virtual machines for the specified account in this domain.")
    private Long domainId;

    @Parameter(name=ApiConstants.IS_RECURSIVE, type=CommandType.BOOLEAN, description="Must be used with domainId parameter. Defaults to false, but if true, lists all vms from the parent specified by the domain id till leaves.")
    private Boolean recursive;

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
    
    @Parameter(name=ApiConstants.FOR_VIRTUAL_NETWORK, type=CommandType.BOOLEAN, description="list by network type; true if need to list vms using Virtual Network, false otherwise")
    private Boolean forVirtualNetwork;
    
    @Parameter(name=ApiConstants.NETWORK_ID, type=CommandType.LONG, description="list by network id")
    private Long networkId;

    @Parameter(name=ApiConstants.HYPERVISOR, type=CommandType.STRING, description="the target hypervisor for the template")
    private String hypervisor;
    
    @Parameter(name=ApiConstants.STORAGE_ID, type=CommandType.LONG, description="the storage ID where vm's volumes belong to")
    private Long storageId;
    
    @Parameter(name=ApiConstants.PROJECT_ID, type=CommandType.LONG, description="list vms by project")
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
    
    public Boolean getForVirtualNetwork() {
        return forVirtualNetwork;
    }

    public void setForVirtualNetwork(Boolean forVirtualNetwork) {
        this.forVirtualNetwork = forVirtualNetwork;
    }
    
    public Long getNetworkId() {
        return networkId;
    }

    public Boolean isRecursive() {
        return recursive;
    }
        
    public String getHypervisor() {
        return hypervisor;
    }
    
    public Long getStorageId() {
        return storageId;
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
    
    public AsyncJob.Type getInstanceType() {
        return AsyncJob.Type.VirtualMachine;
    }

    @Override
    public void execute(){
        List<? extends UserVm> result = _userVmService.searchForUserVMs(this);
        ListResponse<UserVmResponse> response = new ListResponse<UserVmResponse>();
        List<UserVmResponse> vmResponses = _responseGenerator.createUserVmSummaryResponse("virtualmachine", result.toArray(new UserVm[result.size()]));
        response.setResponses(vmResponses);
        response.setResponseName(getCommandName());
        this.setResponseObject(response);
    }
    
}

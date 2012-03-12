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
import com.cloud.api.BaseCmd;
import com.cloud.api.IdentityMapper;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.VlanIpRangeResponse;
import com.cloud.dc.Vlan;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.user.Account;

@Implementation(description="Creates a VLAN IP range.", responseObject=VlanIpRangeResponse.class)
public class CreateVlanIpRangeCmd extends BaseCmd {
	public static final Logger s_logger = Logger.getLogger(CreateVlanIpRangeCmd.class.getName());

    private static final String s_name = "createvlaniprangeresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.ACCOUNT, type=CommandType.STRING, description="account who will own the VLAN. If VLAN is Zone wide, this parameter should be ommited")
    private String accountName;
    
    @IdentityMapper(entityTableName="projects")
    @Parameter(name=ApiConstants.PROJECT_ID, type=CommandType.LONG, description="project who will own the VLAN. If VLAN is Zone wide, this parameter should be ommited")
    private Long projectId;

    @IdentityMapper(entityTableName="domain")
    @Parameter(name=ApiConstants.DOMAIN_ID, type=CommandType.LONG, description="domain ID of the account owning a VLAN")
    private Long domainId;

    @Parameter(name=ApiConstants.END_IP, type=CommandType.STRING, description="the ending IP address in the VLAN IP range")
    private String endIp;

    @Parameter(name=ApiConstants.FOR_VIRTUAL_NETWORK, type=CommandType.BOOLEAN, description="true if VLAN is of Virtual type, false if Direct")
    private Boolean forVirtualNetwork;

    @Parameter(name=ApiConstants.GATEWAY, type=CommandType.STRING, description="the gateway of the VLAN IP range")
    private String gateway;

    @Parameter(name=ApiConstants.NETMASK, type=CommandType.STRING, description="the netmask of the VLAN IP range")
    private String netmask;

    @IdentityMapper(entityTableName="host_pod_ref")
    @Parameter(name=ApiConstants.POD_ID, type=CommandType.LONG, description="optional parameter. Have to be specified for Direct Untagged vlan only.")
    private Long podId;

    @Parameter(name=ApiConstants.START_IP, type=CommandType.STRING, required=true, description="the beginning IP address in the VLAN IP range")
    private String startIp;

    @Parameter(name=ApiConstants.VLAN, type=CommandType.STRING, description="the ID or VID of the VLAN. Default is an \"untagged\" VLAN.")
    private String vlan;

    @IdentityMapper(entityTableName="data_center")
    @Parameter(name=ApiConstants.ZONE_ID, type=CommandType.LONG, description="the Zone ID of the VLAN IP range")
    private Long zoneId;
    
    @IdentityMapper(entityTableName="networks")
    @Parameter(name=ApiConstants.NETWORK_ID, type=CommandType.LONG, description="the network id")
    private Long networkID;

    @IdentityMapper(entityTableName="physical_network")
    @Parameter(name=ApiConstants.PHYSICAL_NETWORK_ID, type=CommandType.LONG, description="the physical network id")
    private Long physicalNetworkId;
    
    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getAccountName() {
        return accountName;
    }

    public Long getDomainId() {
        return domainId;
    }

    public String getEndIp() {
        return endIp;
    }

    public Boolean isForVirtualNetwork() {
        return forVirtualNetwork == null ? true : forVirtualNetwork;
    }

    public String getGateway() {
        return gateway;
    }

    public String getNetmask() {
        return netmask;
    }

    public Long getPodId() {
        return podId;
    }

    public String getStartIp() {
        return startIp;
    }

    public String getVlan() {
        return vlan;
    }

    public Long getZoneId() {
        return zoneId;
    }

    public Long getProjectId() {
        return projectId;
    }
    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    public Long getNetworkID() {
        return networkID;
    }

    public Long getPhysicalNetworkId() {
        return physicalNetworkId;
    }

    
    @Override
    public String getCommandName() {
        return s_name;
    }
    
    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }
    
    @Override
    public void execute() throws ResourceUnavailableException, ResourceAllocationException{
        try {
            Vlan result = _configService.createVlanAndPublicIpRange(this);
            if (result != null) {
                VlanIpRangeResponse response = _responseGenerator.createVlanIpRangeResponse(result);
                response.setResponseName(getCommandName());
                this.setResponseObject(response);
            }else {
                throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to create vlan ip range");
            }
        } catch (ConcurrentOperationException ex) {
            s_logger.warn("Exception: ", ex);
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, ex.getMessage());
        } catch (InsufficientCapacityException ex) {
            s_logger.info(ex);
            throw new ServerApiException(BaseCmd.INSUFFICIENT_CAPACITY_ERROR, ex.getMessage());
        } 
    }
}

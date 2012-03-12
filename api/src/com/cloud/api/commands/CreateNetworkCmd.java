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
import com.cloud.api.response.NetworkResponse;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.network.Network;
import com.cloud.network.Network.GuestType;
import com.cloud.offering.NetworkOffering;
import com.cloud.user.UserContext;

@Implementation(description="Creates a network", responseObject=NetworkResponse.class)
public class CreateNetworkCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(CreateNetworkCmd.class.getName());

    private static final String s_name = "createnetworkresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    
    @Parameter(name=ApiConstants.NAME, type=CommandType.STRING, required=true, description="the name of the network")
    private String name;
    
    @Parameter(name=ApiConstants.DISPLAY_TEXT, type=CommandType.STRING, required=true, description="the display text of the network")
    private String displayText;
    
    @IdentityMapper(entityTableName="network_offerings")
    @Parameter(name=ApiConstants.NETWORK_OFFERING_ID, type=CommandType.LONG, required=true, description="the network offering id")
    private Long networkOfferingId;
    
    @IdentityMapper(entityTableName="data_center")
    @Parameter(name=ApiConstants.ZONE_ID, type=CommandType.LONG, required=true, description="the Zone ID for the network")
    private Long zoneId;
    
    @IdentityMapper(entityTableName="physical_network")
    @Parameter(name=ApiConstants.PHYSICAL_NETWORK_ID, type=CommandType.LONG, description="the Physical Network ID the network belongs to")
    private Long physicalNetworkId;

    @Parameter(name=ApiConstants.GATEWAY, type=CommandType.STRING, description="the gateway of the network")
    private String gateway;
    
    @Parameter(name=ApiConstants.NETMASK, type=CommandType.STRING, description="the netmask of the network")
    private String netmask;
    
    @Parameter(name=ApiConstants.START_IP, type=CommandType.STRING, description="the beginning IP address in the network IP range")
    private String startIp;
    
    @Parameter(name=ApiConstants.END_IP, type=CommandType.STRING, description="the ending IP address in the network IP range. If not specified, will be defaulted to startIP")
    private String endIp;

    @Parameter(name=ApiConstants.VLAN, type=CommandType.STRING, description="the ID or VID of the network")
    private String vlan;
    
    @Parameter(name=ApiConstants.NETWORK_DOMAIN, type=CommandType.STRING, description="network domain")
    private String networkDomain;
    
    @Parameter(name=ApiConstants.ACL_TYPE, type=CommandType.STRING, description="Access control type; supported values are account and domain. In 3.0 all shared networks should have aclType=Domain, and all Isolated networks - Account. Account means that only the account owner can use the network, domain - all accouns in the domain can use the network")
    private String aclType;

    @Parameter(name=ApiConstants.ACCOUNT, type=CommandType.STRING, description="account who will own the network")
    private String accountName;
    
    @IdentityMapper(entityTableName="projects")
    @Parameter(name=ApiConstants.PROJECT_ID, type=CommandType.LONG, description="an optional project for the ssh key")
    private Long projectId;

    @IdentityMapper(entityTableName="domain")
    @Parameter(name=ApiConstants.DOMAIN_ID, type=CommandType.LONG, description="domain ID of the account owning a network")
    private Long domainId;
   
    @Parameter(name=ApiConstants.SUBDOMAIN_ACCESS, type=CommandType.BOOLEAN, description="Defines whether to allow subdomains to use networks dedicated to their parent domain(s). Should be used with aclType=Domain, defaulted to allow.subdomain.network.access global config if not specified")
    private Boolean subdomainAccess;
    


    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////
    public Long getNetworkOfferingId() {
        return networkOfferingId;
    }

    public String getGateway() {
        return gateway;
    }

    public String getVlan() {
        return vlan;
    }

    public String getAccountName() {
        return accountName;
    }

    public Long getDomainId() {
        return domainId;
    }
    
    public String getNetmask() {
        return netmask;
    }

    public String getStartIp() {
        return startIp;
    }

    public String getEndIp() {
        return endIp;
    }
    
    public String getNetworkName() {
        return name;
    }
    
    public String getDisplayText() {
        return displayText;
    }

    public String getNetworkDomain() {
        return networkDomain;
    }
    
    public Long getProjectId() {
        return projectId;
    }

    public String getAclType() {
		return aclType;
	}

	public Boolean getSubdomainAccess() {
		return subdomainAccess;
	}

	public Long getZoneId() {
        Long physicalNetworkId = getPhysicalNetworkId();
        
        if (physicalNetworkId == null && zoneId == null) {
            throw new InvalidParameterValueException("Zone id is required");
        }
        
        return zoneId;
    }
    
    public Long getPhysicalNetworkId() {
        NetworkOffering offering = _configService.getNetworkOffering(networkOfferingId);
        if (offering == null) {
            throw new InvalidParameterValueException("Unable to find network offering by id " + networkOfferingId);
        }
        
        if (physicalNetworkId != null) {
            if (offering.getGuestType() == GuestType.Shared) {
                return physicalNetworkId;
            } else {
                throw new InvalidParameterValueException("Physical network id can be specified for networks of guest ip type " + GuestType.Shared + " only.");
            }
        } else {
            if (zoneId == null) {
                throw new InvalidParameterValueException("ZoneId is required as physicalNetworkId is null");
            }
            return _networkService.findPhysicalNetworkId(zoneId, offering.getTags(), offering.getTrafficType());
        }
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////
    @Override
    public String getCommandName() {
        return s_name;
    }
    
    @Override
    public long getEntityOwnerId() {
        Long accountId = finalyzeAccountId(accountName, domainId, projectId, true);
        if (accountId == null) {
            return UserContext.current().getCaller().getId();
        }
        
        return accountId;
    }
    
    @Override
	// an exception thrown by createNetwork() will be caught by the dispatcher. 
    public void execute() throws InsufficientCapacityException, ConcurrentOperationException, ResourceAllocationException{
        Network result = _networkService.createNetwork(this);
        if (result != null) {
            NetworkResponse response = _responseGenerator.createNetworkResponse(result);
            response.setResponseName(getCommandName());
            this.setResponseObject(response);
        }else {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to create network");
        }
    }
}

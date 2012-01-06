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
import com.cloud.api.BaseListProjectAndAccountResourcesCmd;
import com.cloud.api.IdentityMapper;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.response.IPAddressResponse;
import com.cloud.api.response.ListResponse;
import com.cloud.async.AsyncJob;
import com.cloud.network.IpAddress;

@Implementation(description="Lists all public ip addresses", responseObject=IPAddressResponse.class)
public class ListPublicIpAddressesCmd extends BaseListProjectAndAccountResourcesCmd {
    public static final Logger s_logger = Logger.getLogger(ListPublicIpAddressesCmd.class.getName());

    private static final String s_name = "listpublicipaddressesresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.ALLOCATED_ONLY, type=CommandType.BOOLEAN, description="limits search results to allocated public IP addresses")
    private Boolean allocatedOnly;

    @Parameter(name=ApiConstants.FOR_VIRTUAL_NETWORK, type=CommandType.BOOLEAN, description="the virtual network for the IP address")
    private Boolean forVirtualNetwork;
    
    @IdentityMapper(entityTableName="user_ip_address")
    @Parameter(name=ApiConstants.ID, type=CommandType.LONG, description="lists ip address by id")
    private Long id;

    @Parameter(name=ApiConstants.IP_ADDRESS, type=CommandType.STRING, description="lists the specified IP address")
    private String ipAddress;

    @IdentityMapper(entityTableName="vlan")
    @Parameter(name=ApiConstants.VLAN_ID, type=CommandType.LONG, description="lists all public IP addresses by VLAN ID")
    private Long vlanId;

    @IdentityMapper(entityTableName="data_center")
    @Parameter(name=ApiConstants.ZONE_ID, type=CommandType.LONG, description="lists all public IP addresses by Zone ID")
    private Long zoneId;
    
    @Parameter(name=ApiConstants.FOR_LOAD_BALANCING, type=CommandType.BOOLEAN, description="list only ips used for load balancing")
    private Boolean forLoadBalancing;
    
    @IdentityMapper(entityTableName="physical_network")
    @Parameter(name=ApiConstants.PHYSICAL_NETWORK_ID, type=CommandType.LONG, description="lists all public IP addresses by physical network id")
    private Long physicalNetworkId;
    
    @IdentityMapper(entityTableName="networks")
    @Parameter(name=ApiConstants.ASSOCIATED_NETWORK_ID, type=CommandType.LONG, description="lists all public IP addresses associated to the network specified")
    private Long associatedNetworkId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////
    public Long getId() {
        return id;
    }

    public Boolean isAllocatedOnly() {
        return allocatedOnly;
    }

    public Boolean isForVirtualNetwork() {
        return forVirtualNetwork;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public Long getVlanId() {
        return vlanId;
    }

    public Long getZoneId() {
        return zoneId;
    }
    
    public Long getPhysicalNetworkId() {
        return physicalNetworkId;
    }
    
    public Long getAssociatedNetworkId() {
		return associatedNetworkId;
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
        List<? extends IpAddress> result = _mgr.searchForIPAddresses(this);
        ListResponse<IPAddressResponse> response = new ListResponse<IPAddressResponse>();
        List<IPAddressResponse> ipAddrResponses = new ArrayList<IPAddressResponse>();
        for (IpAddress ipAddress : result) {
            IPAddressResponse ipResponse = _responseGenerator.createIPAddressResponse(ipAddress);
            ipResponse.setObjectName("publicipaddress");
            ipAddrResponses.add(ipResponse);
        }

        response.setResponses(ipAddrResponses);
        response.setResponseName(getCommandName());
        this.setResponseObject(response);
    }
    
    public AsyncJob.Type getInstanceType() {
        return AsyncJob.Type.IpAddress;
    }


    public Boolean isForLoadBalancing() {
        return forLoadBalancing;
    }

    public Boolean getAllocatedOnly() {
        return allocatedOnly;
    }

    public Boolean getForVirtualNetwork() {
        return forVirtualNetwork;
    }

    public Boolean getForLoadBalancing() {
        return forLoadBalancing;
    }
}

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
import com.cloud.api.response.IPAddressResponse;
import com.cloud.api.response.ListResponse;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.network.IPAddressVO;

@Implementation(method="searchForIPAddresses", description="Lists all public ip addresses")
public class ListPublicIpAddressesCmd extends BaseListCmd {
    public static final Logger s_logger = Logger.getLogger(ListPublicIpAddressesCmd.class.getName());

    private static final String s_name = "listpublicipaddressesresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.ACCOUNT, type=CommandType.STRING, description="lists all public IP addresses by account. Must be used with the domainId parameter.")
    private String accountName;

    @Parameter(name=ApiConstants.ALLOCATED_ONLY, type=CommandType.BOOLEAN, description="limits search results to allocated public IP addresses")
    private Boolean allocatedOnly;

    @Parameter(name=ApiConstants.DOMAIN_ID, type=CommandType.LONG, description="lists all public IP addresses by domain ID. If used with the account parameter, lists all public IP addresses by account for specified domain.")
    private Long domainId;

    @Parameter(name=ApiConstants.FOR_VIRTUAL_NETWORK, type=CommandType.BOOLEAN, description="the virtual network for the IP address")
    private Boolean forVirtualNetwork;

    @Parameter(name=ApiConstants.IP_ADDRESS, type=CommandType.STRING, description="lists the specified IP address")
    private String ipAddress;

    @Parameter(name=ApiConstants.VLAN_ID, type=CommandType.LONG, description="lists all public IP addresses by VLAN ID")
    private Long vlanId;

    @Parameter(name=ApiConstants.ZONE_ID, type=CommandType.LONG, description="lists all public IP addresses by Zone ID")
    private Long zoneId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getAccountName() {
        return accountName;
    }

    public Boolean isAllocatedOnly() {
        return allocatedOnly;
    }

    public Long getDomainId() {
        return domainId;
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

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getName() {
        return s_name;
    }

    @Override @SuppressWarnings("unchecked")
    public ListResponse<IPAddressResponse> getResponse() {
        List<IPAddressVO> ipAddresses = (List<IPAddressVO>)getResponseObject();

        ListResponse<IPAddressResponse> response = new ListResponse<IPAddressResponse>();
        List<IPAddressResponse> ipAddrResponses = new ArrayList<IPAddressResponse>();
        for (IPAddressVO ipAddress : ipAddresses) {
            IPAddressResponse ipResponse = ApiResponseHelper.createIPAddressResponse(ipAddress);
            ipResponse.setObjectName("publicipaddress");
            ipAddrResponses.add(ipResponse);
        }

        response.setResponses(ipAddrResponses);
        response.setResponseName(getName());
        return response;
    }
    
    @Override
    public Object execute() throws ServerApiException, InvalidParameterValueException, PermissionDeniedException, InsufficientAddressCapacityException, InsufficientCapacityException, ConcurrentOperationException{
        List<IPAddressVO> result = _mgr.searchForIPAddresses(this);
        return result;
    }
}

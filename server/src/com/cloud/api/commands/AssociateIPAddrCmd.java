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
import com.cloud.api.ApiResponseHelper;
import com.cloud.api.BaseCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.IPAddressResponse;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.network.IPAddressVO;
import com.cloud.network.NetworkManager;

@Implementation(method="associateIP", manager=NetworkManager.class, description="Acquires and associates a public IP to an account.")
public class AssociateIPAddrCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(AssociateIPAddrCmd.class.getName());
    private static final String s_name = "associateipaddressresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.ACCOUNT, type=CommandType.STRING, description="the account to associate with this IP address")
    private String accountName;

    @Parameter(name=ApiConstants.DOMAIN_ID, type=CommandType.LONG, description="the ID of the domain to associate with this IP address")
    private Long domainId;

    @Parameter(name=ApiConstants.ZONE_ID, type=CommandType.LONG, required=true, description="the ID of the availability zone you want to acquire an public IP address from")
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

    public Long getZoneId() {
        return zoneId;
    }


    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////


    public String getName() {
        return s_name;
    }

    public static String getResultObjectName() {
    	return "addressinfo";
    }
    
    @SuppressWarnings("unchecked")
    public IPAddressResponse getResponse() {
    	IPAddressVO ipAddress = (IPAddressVO)getResponseObject();
        IPAddressResponse ipResponse = ApiResponseHelper.createIPAddressResponse(ipAddress);
        ipResponse.setResponseName(getName());
        return ipResponse;
    }
    
    @Override
    public Object execute() throws ServerApiException, InvalidParameterValueException, PermissionDeniedException, InsufficientAddressCapacityException, InsufficientCapacityException, ConcurrentOperationException{
        try {
            IPAddressVO result = _networkMgr.associateIP(this);
            return result;
        } catch (ResourceAllocationException ex) {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, ex.getMessage());
        }

    }
}

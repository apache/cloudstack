/**
 *  Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved.
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
import com.cloud.api.BaseAsyncCmd;
import com.cloud.api.BaseCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.PlugService;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.SuccessResponse;
import com.cloud.async.AsyncJob;
import com.cloud.event.EventTypes;
import com.cloud.network.element.DhcpElementService;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.user.Account;
import com.cloud.user.UserContext;

@Implementation(responseObject=SuccessResponse.class, description="Configures a dhcp element.")
public class ConfigureDhcpElementCmd extends BaseAsyncCmd {
	public static final Logger s_logger = Logger.getLogger(ConfigureDhcpElementCmd.class.getName());
    private static final String s_name = "configuredhcpelementresponse";
    
    @PlugService
    private DhcpElementService _service;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.UUID, type=CommandType.STRING, required=true, description="the UUID of the virtual router element")
    private String uuid;

    @Parameter(name=ApiConstants.DHCP_SERVICE, type=CommandType.BOOLEAN, required=true, description="true is dhcp service would be enabled")
    private Boolean dhcpService; 
    
    @Parameter(name=ApiConstants.DNS_SERVICE, type=CommandType.BOOLEAN, required=true, description="true is dns service would be enabled")
    private Boolean dnsService; 
    
    @Parameter(name=ApiConstants.USERDATA_SERVICE, type=CommandType.BOOLEAN, required=true, description="true is user data service would be enabled")
    private Boolean userdataService;
    
    @Parameter(name=ApiConstants.DHCP_RANGE, type=CommandType.STRING, description="the dhcp range for the DHCP service ")
    private String dhcpRange;

    @Parameter(name=ApiConstants.DNS1, type=CommandType.STRING, description="the first DNS")
    private String dns1;

    @Parameter(name=ApiConstants.DNS2, type=CommandType.STRING, description="the second DNS")
    private String dns2;

    @Parameter(name=ApiConstants.INTERNAL_DNS1, type=CommandType.STRING, description="the first internal DNS")
    private String internalDns1;

    @Parameter(name=ApiConstants.INTERNAL_DNS2, type=CommandType.STRING, description="the second internal DNS")
    private String internalDns2;

    @Parameter(name=ApiConstants.DOMAIN, type=CommandType.STRING, description="the gateway ip")
    private String domainName;
    
    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getUUID() {
        return uuid;
    }

    public Boolean getDhcpService() {
        return dhcpService;
    }

    public Boolean getDnsService() {
        return dnsService;
    }

    public Boolean getUserdataService() {
        return userdataService;
    }

    public String getDomainName() {
        return domainName;
    }
    
    public String getDhcpRange() {
        return dhcpRange;
    }

    public String getDns1() {
        return dns1;
    }

    public String getDns2() {
        return dns2;
    }

    public String getInternalDns1() {
        return internalDns1;
    }

    public String getInternalDns2() {
        return internalDns2;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }
    
    public static String getResultObjectName() {
    	return "boolean";
    }
    
    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_NETWORK_ELEMENT_CONFIGURE;
    }

    @Override
    public String getEventDescription() {
        return  "configuring dhcp element: " + getUUID();
    }
    
    public AsyncJob.Type getInstanceType() {
    	return AsyncJob.Type.None;
    }
    
    public Long getInstanceId() {
        return _service.getIdByUUID(uuid);
    }
	
    @Override
    public void execute() throws ConcurrentOperationException, ResourceUnavailableException, InsufficientCapacityException{
        UserContext.current().setEventDetails("Dhcp element: " + getUUID());
        Boolean result = _service.configure(this);
        if (result){
            SuccessResponse response = new SuccessResponse();
            response.setResponseName(getCommandName());
            response.setSuccess(result);
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to configure the dhcp element");
        }
    }
}

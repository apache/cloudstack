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
import com.cloud.network.element.VirtualRouterElementService;
import com.cloud.async.AsyncJob;
import com.cloud.event.EventTypes;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.user.Account;
import com.cloud.user.UserContext;

@Implementation(responseObject=SuccessResponse.class, description="Configures a virtual router element.")
public class ConfigureVirtualRouterElementCmd extends BaseAsyncCmd {
	public static final Logger s_logger = Logger.getLogger(ConfigureVirtualRouterElementCmd.class.getName());
    private static final String s_name = "configurevirtualrouterelementresponse";
    
    @PlugService
    private VirtualRouterElementService _service;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.UUID, type=CommandType.STRING, required=true, description="the UUID of the virtual router element")
    private String uuid;

    @Parameter(name=ApiConstants.DHCP_SERVICE, type=CommandType.BOOLEAN, required=true, description="true is dhcp service would be enabled")
    private Boolean dhcpService; 
    
    @Parameter(name=ApiConstants.DNS_SERVICE, type=CommandType.BOOLEAN, required=true, description="true is dns service would be enabled")
    private Boolean dnsService; 
    
    @Parameter(name=ApiConstants.GATEWAY_SERVICE, type=CommandType.BOOLEAN, required=true, description="true is gateway service would be enabled")
    private Boolean gatewayService; 
    
    @Parameter(name=ApiConstants.FIREWALL_SERVICE, type=CommandType.BOOLEAN, required=true, description="true is firewall service would be enabled")
    private Boolean firewallService; 
    
    @Parameter(name=ApiConstants.LB_SERVICE, type=CommandType.BOOLEAN, required=true, description="true is lb service would be enabled")
    private Boolean lbService; 
    
    @Parameter(name=ApiConstants.USERDATA_SERVICE, type=CommandType.BOOLEAN, required=true, description="true is user data service would be enabled")
    private Boolean userdataService;
    
    @Parameter(name=ApiConstants.SOURCE_NAT_SERVICE, type=CommandType.BOOLEAN, required=true, description="true is source nat service would be enabled")
    private Boolean sourceNatService;
    
    @Parameter(name=ApiConstants.VPN_SERVICE, type=CommandType.BOOLEAN, required=true, description="true is vpn service would be enabled")
    private Boolean vpnService;
    
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

    public Boolean getGatewayService() {
        return gatewayService;
    }

    public Boolean getFirewallService() {
        return firewallService;
    }

    public Boolean getLbService() {
        return lbService;
    }

    public Boolean getUserdataService() {
        return userdataService;
    }

    public Boolean getSourceNatService() {
        return sourceNatService;
    }

    public Boolean getVpnService() {
        return vpnService;
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
        return  "configuring virtual router element: " + getUUID();
    }
    
    public AsyncJob.Type getInstanceType() {
    	return AsyncJob.Type.None;
    }
    
    public Long getInstanceId() {
        return _service.getIdByUUID(uuid);
    }
	
    @Override
    public void execute() throws ConcurrentOperationException, ResourceUnavailableException, InsufficientCapacityException{
        UserContext.current().setEventDetails("Virtual router element: " + getUUID());
        Boolean result = _service.configure(this);
        if (result){
            SuccessResponse response = new SuccessResponse();
            response.setResponseName(getCommandName());
            response.setSuccess(result);
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to configure the virtual router element");
        }
    }
}

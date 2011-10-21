/**
 * *  Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved
*
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
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.host.Host;
import com.cloud.network.ExternalNetworkDeviceManager;
import com.cloud.server.ManagementService;
import com.cloud.server.api.response.ExternalLoadBalancerResponse;
import com.cloud.user.Account;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.utils.exception.CloudRuntimeException;

@Implementation(description="Adds an external load balancer appliance.", responseObject = ExternalLoadBalancerResponse.class)
public class AddExternalLoadBalancerCmd extends BaseCmd {
	public static final Logger s_logger = Logger.getLogger(AddExternalLoadBalancerCmd.class.getName());
	private static final String s_name = "addexternalloadbalancerresponse";
	
	/////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
	
	@Parameter(name=ApiConstants.ZONE_ID, type=CommandType.LONG, required = true, description="Zone in which to add the external load balancer appliance.")
	private Long zoneId;

	@Parameter(name=ApiConstants.NETWORK_ID, type=CommandType.LONG, required = false, description="Pyshical network in the zone to which external load balancer appliance will be added.")
	private Long networkId;
	
	@Parameter(name=ApiConstants.URL, type=CommandType.STRING, required = true, description="URL of the external load balancer appliance.")
	private String url;	 
	
	@Parameter(name=ApiConstants.USERNAME, type=CommandType.STRING, required = true, description="Username of the external load balancer appliance.")
	private String username;	 
	
	@Parameter(name=ApiConstants.PASSWORD, type=CommandType.STRING, required = true, description="Password of the external load balancer appliance.")
	private String password;	 

	@Parameter(name=ApiConstants.NETWORK_DEVICE_TYPE, type=CommandType.STRING, required = false, description="External load balancer type. Now supports NetscalerLoadBalancer, F5BigIpLoadBalancer.")
	private String type;

	///////////////////////////////////////////////////
	/////////////////// Accessors ///////////////////////
	/////////////////////////////////////////////////////
	 
	public Long getZoneId() {
		return zoneId;
	}
	
	public Long getNetworkId() {
		return networkId;
	}
	
	public String getUrl() {
		return url;
	}
	
	public String getUsername() {
		return username;
	}
	
	public String getPassword() {
		return password;
	}
	
	public String getDeviceType() {
		return type;
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
        return Account.ACCOUNT_ID_SYSTEM;
    }
	 
	@Override
    public void execute(){
		try {
		    ComponentLocator locator = ComponentLocator.getLocator(ManagementService.Name);
		    ExternalNetworkDeviceManager externalNetworkMgr = locator.getManager(ExternalNetworkDeviceManager.class);
			Host externalLoadBalancer = externalNetworkMgr.addExternalLoadBalancer(this);
			ExternalLoadBalancerResponse response = externalNetworkMgr.createExternalLoadBalancerResponse(externalLoadBalancer);
			response.setObjectName("externalloadbalancer");
			response.setResponseName(getCommandName());
			this.setResponseObject(response);
		} catch (InvalidParameterValueException ipve) {
			throw new ServerApiException(BaseCmd.PARAM_ERROR, ipve.getMessage());
		} catch (CloudRuntimeException cre) {
			throw new ServerApiException(BaseCmd.INTERNAL_ERROR, cre.getMessage());
		}
    }

}


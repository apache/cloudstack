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
import com.cloud.api.IdentityMapper;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.PlugService;
import com.cloud.api.ServerApiException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.host.Host;
import com.cloud.network.element.F5ExternalLoadBalancerElementService;
import com.cloud.server.api.response.ExternalLoadBalancerResponse;
import com.cloud.user.Account;
import com.cloud.utils.exception.CloudRuntimeException;

@Implementation(description="Adds F5 external load balancer appliance.", responseObject = ExternalLoadBalancerResponse.class)
@Deprecated // API supported only for backward compatibility.
public class AddExternalLoadBalancerCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(AddExternalLoadBalancerCmd.class.getName());
    private static final String s_name = "addexternalloadbalancerresponse";
    
    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
	
    @IdentityMapper(entityTableName="data_center")
	@Parameter(name=ApiConstants.ZONE_ID, type=CommandType.LONG, required = true, description="Zone in which to add the external load balancer appliance.")
	private Long zoneId;

    @Parameter(name=ApiConstants.URL, type=CommandType.STRING, required = true, description="URL of the external load balancer appliance.")
    private String url;

    @Parameter(name=ApiConstants.USERNAME, type=CommandType.STRING, required = true, description="Username of the external load balancer appliance.")
    private String username;

    @Parameter(name=ApiConstants.PASSWORD, type=CommandType.STRING, required = true, description="Password of the external load balancer appliance.")
    private String password;

    ///////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////
     
    public Long getZoneId() {
        return zoneId;
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

    @PlugService
    F5ExternalLoadBalancerElementService _f5DeviceManagerService;

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
            Host externalLoadBalancer = _f5DeviceManagerService.addExternalLoadBalancer(this);
            ExternalLoadBalancerResponse response = _f5DeviceManagerService.createExternalLoadBalancerResponse(externalLoadBalancer);
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
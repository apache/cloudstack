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
import com.cloud.api.IdentityMapper;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.PlugService;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.F5LoadBalancerResponse;
import com.cloud.event.EventTypes;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.ExternalLoadBalancerDeviceVO;
import com.cloud.network.element.F5ExternalLoadBalancerElementService;
import com.cloud.user.UserContext;
import com.cloud.utils.exception.CloudRuntimeException;

@Implementation(responseObject=F5LoadBalancerResponse.class, description="Adds a F5 BigIP load balancer device")
public class AddF5LoadBalancerCmd extends BaseAsyncCmd {

    public static final Logger s_logger = Logger.getLogger(AddF5LoadBalancerCmd.class.getName());
    private static final String s_name = "addf5bigiploadbalancerresponse";
    @PlugService F5ExternalLoadBalancerElementService _f5DeviceManagerService;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @IdentityMapper(entityTableName="physical_network")
    @Parameter(name=ApiConstants.PHYSICAL_NETWORK_ID, type=CommandType.LONG, required=true, description="the Physical Network ID")
    private Long physicalNetworkId;

    @Parameter(name=ApiConstants.URL, type=CommandType.STRING, required = true, description="URL of the F5 load balancer appliance.")
    private String url;

    @Parameter(name=ApiConstants.USERNAME, type=CommandType.STRING, required = true, description="Credentials to reach F5 BigIP load balancer device")
    private String username;
    
    @Parameter(name=ApiConstants.PASSWORD, type=CommandType.STRING, required = true, description="Credentials to reach F5 BigIP load balancer device")
    private String password;

    @Parameter(name = ApiConstants.NETWORK_DEVICE_TYPE, type = CommandType.STRING, required = true, description = "supports only F5BigIpLoadBalancer")
    private String deviceType;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getPhysicalNetworkId() {
        return physicalNetworkId;
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
        return deviceType;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException {
        try {
            ExternalLoadBalancerDeviceVO lbDeviceVO = _f5DeviceManagerService.addF5LoadBalancer(this);
            if (lbDeviceVO != null) {
                F5LoadBalancerResponse response = _f5DeviceManagerService.createF5LoadBalancerResponse(lbDeviceVO);
                response.setObjectName("f5loadbalancer");
                response.setResponseName(getCommandName());
                this.setResponseObject(response);
            } else {
                throw new ServerApiException(BaseAsyncCmd.INTERNAL_ERROR, "Failed to add F5 Big IP load balancer due to internal error.");
            }
        }  catch (InvalidParameterValueException invalidParamExcp) {
            throw new ServerApiException(BaseCmd.PARAM_ERROR, invalidParamExcp.getMessage());
        } catch (CloudRuntimeException runtimeExcp) {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, runtimeExcp.getMessage());
        }
    }

    @Override
    public String getEventDescription() {
        return "Adding a F5 Big Ip load balancer device";
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_EXTERAL_LB_DEVICE_ADD;
    }
 
    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public long getEntityOwnerId() {
        return UserContext.current().getCaller().getId();
    }
}
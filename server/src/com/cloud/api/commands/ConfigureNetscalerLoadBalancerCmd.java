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
import com.cloud.api.response.NetscalerLoadBalancerResponse;
import com.cloud.event.EventTypes;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.ExternalLoadBalancerDeviceVO;
import com.cloud.network.element.NetscalerLoadBalancerElementService;
import com.cloud.user.UserContext;
import com.cloud.utils.exception.CloudRuntimeException;

@Implementation(responseObject=NetscalerLoadBalancerResponse.class, description="configures a netscaler load balancer device")
public class ConfigureNetscalerLoadBalancerCmd extends BaseAsyncCmd {

    public static final Logger s_logger = Logger.getLogger(ConfigureNetscalerLoadBalancerCmd.class.getName());
    private static final String s_name = "configurenetscalerloadbalancerresponse";
    @PlugService NetscalerLoadBalancerElementService _netsclarLbService;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @IdentityMapper(entityTableName="external_load_balancer_devices")
    @Parameter(name=ApiConstants.LOAD_BALANCER_DEVICE_ID, type=CommandType.LONG, required=true, description="the Physical Network ID")
    private Long lbDeviceId;

    @Parameter(name=ApiConstants.LOAD_BALANCER_DEVICE_CAPACITY, type=CommandType.LONG, required=false, description="capacity of the device, Capacity will be interpreted as number of networks device can handle")
    private Long capacity;

    @Parameter(name=ApiConstants.LOAD_BALANCER_DEVICE_DEDICATED, type=CommandType.BOOLEAN, required=false, description="true if this netscaler device to dedicated for a account")
    private Boolean dedicatedUse;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getLoadBalancerDeviceId() {
        return lbDeviceId;
    }

    public Long getLoadBalancerCapacity() {
        return capacity;
    }

    public Boolean getLoadBalancerDedicated() {
        return dedicatedUse;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException {
        try {
            ExternalLoadBalancerDeviceVO lbDeviceVO = _netsclarLbService.configureNetscalerLoadBalancer(this);
            if (lbDeviceVO != null) {
                NetscalerLoadBalancerResponse response = _netsclarLbService.createNetscalerLoadBalancerResponse(lbDeviceVO);
                response.setObjectName("netscalerloadbalancer");
                response.setResponseName(getCommandName());
                this.setResponseObject(response);
            } else {
                throw new ServerApiException(BaseAsyncCmd.INTERNAL_ERROR, "Failed to configure netscaler load balancer due to internal error.");
            }
        }  catch (InvalidParameterValueException invalidParamExcp) {
            throw new ServerApiException(BaseCmd.PARAM_ERROR, invalidParamExcp.getMessage());
        } catch (CloudRuntimeException runtimeExcp) {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, runtimeExcp.getMessage());
        }
    }

    @Override
    public String getEventDescription() {
        return "Configuring a netscaler load balancer device";
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_EXTERAL_LB_DEVICE_CONFIGURE;
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

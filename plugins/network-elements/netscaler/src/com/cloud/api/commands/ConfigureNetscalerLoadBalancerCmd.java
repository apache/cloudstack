// Licensed to the Apache Software Foundation (ASF) under one or more
// contributor license agreements.  See the NOTICE file distributed with
// this work for additional information regarding copyright ownership.
// The ASF licenses this file to You under the Apache License, Version 2.0
// (the "License"); you may not use this file except in compliance with
// the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.cloud.api.commands;

import java.util.List;

import javax.inject.Inject;

import org.apache.cloudstack.api.*;
import org.apache.cloudstack.api.response.PodResponse;

import org.apache.log4j.Logger;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.context.CallContext;

import com.cloud.api.response.NetscalerLoadBalancerResponse;
import com.cloud.event.EventTypes;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.dao.ExternalLoadBalancerDeviceVO;
import com.cloud.network.element.NetscalerLoadBalancerElementService;
import com.cloud.utils.exception.CloudRuntimeException;

@APICommand(name = "configureNetscalerLoadBalancer", responseObject=NetscalerLoadBalancerResponse.class, description="configures a netscaler load balancer device")
public class ConfigureNetscalerLoadBalancerCmd extends BaseAsyncCmd {

    public static final Logger s_logger = Logger.getLogger(ConfigureNetscalerLoadBalancerCmd.class.getName());
    private static final String s_name = "configurenetscalerloadbalancerresponse";
    @Inject NetscalerLoadBalancerElementService _netsclarLbService;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.LOAD_BALANCER_DEVICE_ID, type=CommandType.UUID, entityType = NetscalerLoadBalancerResponse.class,
            required=true, description="Netscaler load balancer device ID")
    private Long lbDeviceId;

    @Parameter(name=ApiConstants.LOAD_BALANCER_DEVICE_CAPACITY, type=CommandType.LONG, required=false, description="capacity of the device, Capacity will be interpreted as number of networks device can handle")
    private Long capacity;

    @Parameter(name=ApiConstants.LOAD_BALANCER_DEVICE_DEDICATED, type=CommandType.BOOLEAN, required=false, description="true if this netscaler device to dedicated for a account, false if the netscaler device will be shared by multiple accounts")
    private Boolean dedicatedUse;

    @Parameter (name=ApiConstants.INLINE, type=CommandType.BOOLEAN, required=false, description="true if netscaler load balancer is intended to be used in in-line with firewall, false if netscaler load balancer will side-by-side with firewall")
    private Boolean inline;

    @Parameter(name=ApiConstants.POD_IDS, type=CommandType.LIST, collectionType = CommandType.UUID, entityType = PodResponse.class,
            required=false, description="Used when NetScaler device is provider of EIP service." +
            " This parameter represents the list of pod's, for which there exists a policy based route on datacenter L3 router to " +
            "route pod's subnet IP to a NetScaler device.")
    private List<Long> podIds;

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

    public Boolean getLoadBalancerInline() {
        return inline;
    }

    public List<Long> getPodIds() {
        return podIds;
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
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to configure netscaler load balancer due to internal error.");
            }
        }  catch (InvalidParameterValueException invalidParamExcp) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, invalidParamExcp.getMessage());
        } catch (CloudRuntimeException runtimeExcp) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, runtimeExcp.getMessage());
        }
    }

    @Override
    public String getEventDescription() {
        return "Configuring a netscaler load balancer device";
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_EXTERNAL_LB_DEVICE_CONFIGURE;
    }

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public long getEntityOwnerId() {
        return CallContext.current().getCallingAccount().getId();
    }
}

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

import com.cloud.api.response.NetscalerLoadBalancerResponse;
import com.cloud.event.EventTypes;
import com.cloud.exception.*;
import com.cloud.network.dao.ExternalLoadBalancerDeviceVO;
import com.cloud.network.element.NetscalerLoadBalancerElementService;
import com.cloud.utils.exception.CloudRuntimeException;

import org.apache.cloudstack.api.*;
import org.apache.cloudstack.api.response.PhysicalNetworkResponse;
import org.apache.cloudstack.context.CallContext;

import org.apache.log4j.Logger;

import javax.inject.Inject;

@APICommand(name = "addNetscalerLoadBalancer", responseObject=NetscalerLoadBalancerResponse.class, description="Adds a netscaler load balancer device")
public class AddNetscalerLoadBalancerCmd extends BaseAsyncCmd {

    public static final Logger s_logger = Logger.getLogger(AddNetscalerLoadBalancerCmd.class.getName());
    private static final String s_name = "addnetscalerloadbalancerresponse";
    @Inject NetscalerLoadBalancerElementService _netsclarLbService;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.PHYSICAL_NETWORK_ID, type=CommandType.UUID, entityType = PhysicalNetworkResponse.class,
            required=true, description="the Physical Network ID")
    private Long physicalNetworkId;

    @Parameter(name=ApiConstants.URL, type=CommandType.STRING, required = true, description="URL of the netscaler load balancer appliance.")
    private String url;

    @Parameter(name=ApiConstants.USERNAME, type=CommandType.STRING, required = true, description="Credentials to reach netscaler load balancer device")
    private String username;
    
    @Parameter(name=ApiConstants.PASSWORD, type=CommandType.STRING, required = true, description="Credentials to reach netscaler load balancer device")
    private String password;

    @Parameter(name = ApiConstants.NETWORK_DEVICE_TYPE, type = CommandType.STRING, required = true, description = "Netscaler device type supports NetscalerMPXLoadBalancer, NetscalerVPXLoadBalancer, NetscalerSDXLoadBalancer")
    private String deviceType;

    @Parameter(name = ApiConstants.GSLB_PROVIDER, type = CommandType.BOOLEAN, required = false,
            description = "true if NetScaler device being added is for providing GSLB service")
    private boolean  isGslbProvider;

    @Parameter(name = ApiConstants.GSLB_PROVIDER_PUBLIC_IP, type = CommandType.STRING, required = false,
            description = "public IP of the site")
    private String gslbSitePublicIp;

    @Parameter(name = ApiConstants.GSLB_PROVIDER_PRIVATE_IP, type = CommandType.STRING, required = false,
            description = "public IP of the site")
    private String gslbSitePrivateIp;

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

    public boolean isGslbProvider() {
        return isGslbProvider;
    }

    public String getSitePublicIp() {
        return gslbSitePublicIp;
    }

    public String getSitePrivateIp() {
        return gslbSitePrivateIp;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException {
        try {
            ExternalLoadBalancerDeviceVO lbDeviceVO = _netsclarLbService.addNetscalerLoadBalancer(this);
            if (lbDeviceVO != null) {
                NetscalerLoadBalancerResponse response = _netsclarLbService.createNetscalerLoadBalancerResponse(lbDeviceVO);
                response.setObjectName("netscalerloadbalancer");
                response.setResponseName(getCommandName());
                this.setResponseObject(response);
            } else {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to add netscaler load balancer due to internal error.");
            }
        }  catch (InvalidParameterValueException invalidParamExcp) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, invalidParamExcp.getMessage());
        } catch (CloudRuntimeException runtimeExcp) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, runtimeExcp.getMessage());
        }
    }

    @Override
    public String getEventDescription() {
        return "Adding a netscaler load balancer device";
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_EXTERNAL_LB_DEVICE_ADD;
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

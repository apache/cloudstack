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

import javax.inject.Inject;


import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.context.CallContext;

import com.cloud.api.response.NetscalerControlCenterResponse;
import com.cloud.api.response.NetscalerLoadBalancerResponse;
import com.cloud.event.EventTypes;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.NetScalerControlCenterVO;
import com.cloud.network.element.NetscalerLoadBalancerElementService;
import com.cloud.utils.exception.CloudRuntimeException;

@APICommand(name = "registerNetscalerControlCenter", responseObject = NetscalerLoadBalancerResponse.class, description = "Adds a netscaler control center device",
        requestHasSensitiveInfo = true, responseHasSensitiveInfo = false)
public class RegisterNetscalerControlCenterCmd extends BaseAsyncCmd {

    @Inject
    NetscalerLoadBalancerElementService _netsclarLbService;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.IP_ADDRESS , type = CommandType.STRING, required = true, description = "URL of the netscaler controlcenter appliance.")
    private String ipaddress;

    @Parameter(name = ApiConstants.USERNAME, type = CommandType.STRING, required = true, description = "Credentials to reach netscaler controlcenter device")
    private String username;

    @Parameter(name = ApiConstants.PASSWORD, type = CommandType.STRING, required = true, description = "Credentials to reach netscaler controlcenter  device")
    private String password;

    @Parameter(name = ApiConstants.NUM_RETRIES , type = CommandType.INTEGER, required = true, description = "Credentials to reach netscaler controlcenter device")
    private int numretries;


    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////


    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }



    public NetscalerLoadBalancerElementService get_netsclarLbService() {
        return _netsclarLbService;
    }

    public String getIpaddress() {
        return ipaddress;
    }

    public int getNumretries() {
        return numretries;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException,
        ResourceAllocationException {
        try {
            NetScalerControlCenterVO nccVO = _netsclarLbService.registerNetscalerControlCenter(this);
            if (nccVO != null) {
                NetscalerControlCenterResponse  response = _netsclarLbService.createNetscalerControlCenterResponse(nccVO);
                response.setObjectName("netscalerloadbalancer");
                response.setResponseName(getCommandName());
                this.setResponseObject(response);
            } else {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to add netscaler load balancer due to internal error.");
            }
        } catch (InvalidParameterValueException invalidParamExcp) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, invalidParamExcp.getMessage());
        } catch (CloudRuntimeException runtimeExcp) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, runtimeExcp.getMessage());
        }
    }

    @Override
    public String getEventDescription() {
        return "Adding a Netscaler Control Center Device";
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_EXTERNAL_NCC_DEVICE_ADD;
    }

    @Override
    public long getEntityOwnerId() {
        return CallContext.current().getCallingAccount().getId();
    }
}

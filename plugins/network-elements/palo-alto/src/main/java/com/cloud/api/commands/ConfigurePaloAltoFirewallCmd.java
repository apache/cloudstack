// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.api.commands;

import javax.inject.Inject;


import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.context.CallContext;

import com.cloud.api.response.PaloAltoFirewallResponse;
import com.cloud.event.EventTypes;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.dao.ExternalFirewallDeviceVO;
import com.cloud.network.element.PaloAltoFirewallElementService;
import com.cloud.utils.exception.CloudRuntimeException;

@APICommand(name = "configurePaloAltoFirewall", responseObject = PaloAltoFirewallResponse.class, description = "Configures a Palo Alto firewall device",
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class ConfigurePaloAltoFirewallCmd extends BaseAsyncCmd {

    @Inject
    PaloAltoFirewallElementService _paFwService;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.FIREWALL_DEVICE_ID,
               type = CommandType.UUID,
               entityType = PaloAltoFirewallResponse.class,
               required = true,
               description = "Palo Alto firewall device ID")
    private Long fwDeviceId;

    @Parameter(name = ApiConstants.FIREWALL_DEVICE_CAPACITY,
               type = CommandType.LONG,
               required = false,
               description = "capacity of the firewall device, Capacity will be interpreted as number of networks device can handle")
    private Long capacity;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getFirewallDeviceId() {
        return fwDeviceId;
    }

    public Long getFirewallCapacity() {
        return capacity;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException,
        ResourceAllocationException {
        try {
            ExternalFirewallDeviceVO fwDeviceVO = _paFwService.configurePaloAltoFirewall(this);
            if (fwDeviceVO != null) {
                PaloAltoFirewallResponse response = _paFwService.createPaloAltoFirewallResponse(fwDeviceVO);
                response.setObjectName("pafirewall");
                response.setResponseName(getCommandName());
                this.setResponseObject(response);
            } else {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to configure Palo Alto firewall device due to internal error.");
            }
        } catch (InvalidParameterValueException invalidParamExcp) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, invalidParamExcp.getMessage());
        } catch (CloudRuntimeException runtimeExcp) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, runtimeExcp.getMessage());
        }
    }

    @Override
    public String getEventDescription() {
        return "Configuring a Palo Alto firewall device";
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_EXTERNAL_FIREWALL_DEVICE_CONFIGURE;
    }

    @Override
    public long getEntityOwnerId() {
        return CallContext.current().getCallingAccount().getId();
    }
}

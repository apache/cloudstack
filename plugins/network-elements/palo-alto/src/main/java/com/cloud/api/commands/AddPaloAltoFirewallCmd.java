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

import org.apache.log4j.Logger;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.PhysicalNetworkResponse;
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

@APICommand(name = "addPaloAltoFirewall", responseObject = PaloAltoFirewallResponse.class, description = "Adds a Palo Alto firewall device",
        requestHasSensitiveInfo = true, responseHasSensitiveInfo = false)
public class AddPaloAltoFirewallCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(AddPaloAltoFirewallCmd.class.getName());
    @Inject
    PaloAltoFirewallElementService _paFwService;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.PHYSICAL_NETWORK_ID,
               type = CommandType.UUID,
               entityType = PhysicalNetworkResponse.class,
               required = true,
               description = "the Physical Network ID")
    private Long physicalNetworkId;

    @Parameter(name = ApiConstants.URL, type = CommandType.STRING, required = true, description = "URL of the Palo Alto appliance.")
    private String url;

    @Parameter(name = ApiConstants.USERNAME, type = CommandType.STRING, required = true, description = "Credentials to reach Palo Alto firewall device")
    private String username;

    @Parameter(name = ApiConstants.PASSWORD, type = CommandType.STRING, required = true, description = "Credentials to reach Palo Alto firewall device")
    private String password;

    @Parameter(name = ApiConstants.NETWORK_DEVICE_TYPE, type = CommandType.STRING, required = true, description = "supports only PaloAltoFirewall")
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
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException,
        ResourceAllocationException {
        try {
            ExternalFirewallDeviceVO fwDeviceVO = _paFwService.addPaloAltoFirewall(this);
            if (fwDeviceVO != null) {
                PaloAltoFirewallResponse response = _paFwService.createPaloAltoFirewallResponse(fwDeviceVO);
                response.setObjectName("pafirewall");
                response.setResponseName(getCommandName());
                this.setResponseObject(response);
            } else {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to add Palo Alto firewall due to internal error.");
            }
        } catch (InvalidParameterValueException invalidParamExcp) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, invalidParamExcp.getMessage());
        } catch (CloudRuntimeException runtimeExcp) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, runtimeExcp.getMessage());
        }
    }

    @Override
    public String getEventDescription() {
        return "Adding a Palo Alto firewall device";
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_EXTERNAL_FIREWALL_DEVICE_ADD;
    }

    @Override
    public long getEntityOwnerId() {
        return CallContext.current().getCallingAccount().getId();
    }
}

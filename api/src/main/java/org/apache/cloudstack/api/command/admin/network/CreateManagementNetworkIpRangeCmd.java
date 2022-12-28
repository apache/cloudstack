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
package org.apache.cloudstack.api.command.admin.network;

import org.apache.log4j.Logger;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.ApiArgValidator;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.PodResponse;

import com.cloud.dc.Pod;
import com.cloud.event.EventTypes;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.user.Account;

@APICommand(name = CreateManagementNetworkIpRangeCmd.APINAME,
        description = "Creates a Management network IP range.",
        responseObject = PodResponse.class,
        since = "4.11.0.0",
        requestHasSensitiveInfo = false,
        responseHasSensitiveInfo = false,
        authorized = {RoleType.Admin})
public class CreateManagementNetworkIpRangeCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(CreateManagementNetworkIpRangeCmd.class);

    public static final String APINAME = "createManagementNetworkIpRange";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    @Parameter(name = ApiConstants.POD_ID,
            type = CommandType.UUID,
            entityType = PodResponse.class,
            required = true,
            description = "UUID of POD, where the IP range belongs to.",
            validations = {ApiArgValidator.PositiveNumber})
    private Long podId;

    @Parameter(name = ApiConstants.GATEWAY,
            type = CommandType.STRING,
            required = true,
            description = "The gateway for the management network.")
    private String gateway;

    @Parameter(name = ApiConstants.NETMASK,
            type = CommandType.STRING,
            required = true,
            description = "The netmask for the management network.")
    private String netmask;

    @Parameter(name = ApiConstants.START_IP,
            type = CommandType.STRING,
            required = true,
            description = "The starting IP address.")
    private String startIp;

    @Parameter(name = ApiConstants.END_IP,
            type = CommandType.STRING,
            description = "The ending IP address.")
    private String endIp;

    @Parameter(name = ApiConstants.FOR_SYSTEM_VMS,
            type = CommandType.BOOLEAN,
            description = "Specify if range is dedicated for CPVM and SSVM.")
    private Boolean forSystemVms;

    @Parameter(name = ApiConstants.VLAN,
            type = CommandType.STRING,
            description = "Optional. The vlan id the ip range sits on, default to Null when it is not specified which means your network is not on any Vlan")
    private String vlan;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getPodId() {
        return podId;
    }

    public String getGateWay() {
        return gateway;
    }

    public String getNetmask() {
        return netmask;
    }

    public String getStartIp() {
        return startIp;
    }

    public String getEndIp() {
        return endIp;
    }

    public Boolean isForSystemVms() {
        return forSystemVms == null ? Boolean.FALSE : forSystemVms;
    }

    public String getVlan() {
        if (vlan == null || vlan.isEmpty()) {
            vlan = "untagged";
        }
        return vlan;
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_MANAGEMENT_IP_RANGE_CREATE;
    }

    @Override
    public String getEventDescription() {
        return "Creating management ip range from " + getStartIp() + " to " + getEndIp() + " and gateway=" + getGateWay() + ", netmask=" + getNetmask() + " of pod=" + getPodId();
    }

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException,
            ResourceAllocationException {
        Pod result = _configService.createPodIpRange(this);
        if (result != null) {
            PodResponse response = _responseGenerator.createPodResponse(result, false);
            response.setResponseName(getCommandName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to create Pod IP Range.");
        }
    }

    @Override
    public String getCommandName() {
        return APINAME.toLowerCase() + BaseAsyncCmd.RESPONSE_SUFFIX;
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

}

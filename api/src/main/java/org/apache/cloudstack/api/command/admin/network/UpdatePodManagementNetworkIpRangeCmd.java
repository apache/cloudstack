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

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiArgValidator;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.PodResponse;
import org.apache.cloudstack.api.response.SuccessResponse;

import com.cloud.event.EventTypes;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.user.Account;

@APICommand(name = "updatePodManagementNetworkIpRange",
        description = "Updates a management network IP range. Only allowed when no IPs are allocated.",
        responseObject = SuccessResponse.class,
        since = "4.16.0.0",
        requestHasSensitiveInfo = false,
        responseHasSensitiveInfo = false,
        authorized = {RoleType.Admin})
public class UpdatePodManagementNetworkIpRangeCmd extends BaseAsyncCmd {



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

    @Parameter(name = ApiConstants.CURRENT_START_IP,
            type = CommandType.STRING,
            entityType = PodResponse.class,
            required = true,
            description = "The current starting IP address.",
            validations = {ApiArgValidator.NotNullOrEmpty})
    private String currentStartIp;

    @Parameter(name = ApiConstants.CURRENT_END_IP,
            type = CommandType.STRING,
            entityType = PodResponse.class,
            required = true,
            description = "The current ending IP address.",
            validations = {ApiArgValidator.NotNullOrEmpty})
    private String currentEndIp;

    @Parameter(name = ApiConstants.NEW_START_IP,
            type = CommandType.STRING,
            description = "The new starting IP address.",
            validations = {ApiArgValidator.NotNullOrEmpty})
    private String newStartIp;

    @Parameter(name = ApiConstants.NEW_END_IP,
            type = CommandType.STRING,
            description = "The new ending IP address.",
            validations = {ApiArgValidator.NotNullOrEmpty})
    private String newEndIp;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getPodId() {
        return podId;
    }

    public String getCurrentStartIP() {
        return currentStartIp;
    }

    public String getCurrentEndIP() {
        return currentEndIp;
    }

    public String getNewStartIP() {
        return newStartIp;
    }

    public String getNewEndIP() {
        return newEndIp;
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_MANAGEMENT_IP_RANGE_UPDATE;
    }

    @Override
    public String getEventDescription() {
        return "Updating pod management IP range " + getNewStartIP() + "-" + getNewEndIP() + " of Pod: " + getPodId();
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() {
        if (getNewStartIP() == null && getNewEndIP() == null) {
            throw new InvalidParameterValueException("Either new starting IP address or new ending IP address must be specified");
        }

        try {
            _configService.updatePodIpRange(this);
            SuccessResponse response = new SuccessResponse(getCommandName());
            this.setResponseObject(response);
        } catch (ConcurrentOperationException ex) {
            logger.warn("Exception: ", ex);
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, ex.getMessage());
        } catch (Exception e) {
            logger.warn("Failed to update pod management IP range " + getNewStartIP() + "-" + getNewEndIP() + " of Pod: " + getPodId(), e);
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, e.getMessage());
        }
    }
}

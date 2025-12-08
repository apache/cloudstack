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
package org.apache.cloudstack.api.command.admin.outofbandmanagement;

import com.cloud.event.EventTypes;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.host.Host;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiArgValidator;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.HostResponse;
import org.apache.cloudstack.api.response.OutOfBandManagementResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.outofbandmanagement.OutOfBandManagementService;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;

@APICommand(name = "changeOutOfBandManagementPassword", description = "Changes out-of-band management interface password on the host and updates the interface configuration in CloudStack if the operation succeeds, else reverts the old password",
        responseObject = OutOfBandManagementResponse.class, requestHasSensitiveInfo = true, responseHasSensitiveInfo = false,
        since = "4.9.0", authorized = {RoleType.Admin})
public class ChangeOutOfBandManagementPasswordCmd extends BaseAsyncCmd {

    @Inject
    private OutOfBandManagementService outOfBandManagementService;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.HOST_ID, type = CommandType.UUID, entityType = HostResponse.class, required = true,
            validations = {ApiArgValidator.PositiveNumber}, description = "the ID of the host")
    private Long hostId;

    @Parameter(name = ApiConstants.PASSWORD, type = CommandType.STRING, description = "the new host management interface password of maximum length 16, if none is provided a random password would be used")
    private String password;

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {
        final Host host = _resourceService.getHost(getHostId());
        if (host == null) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, "Unable to find host by ID: " + getHostId());
        }

        CallContext.current().setEventDetails("Host Id: " + host.getId() + " Password: " + getPassword().charAt(0) + "****");
        CallContext.current().putContextParameter(Host.class, host.getUuid());

        final OutOfBandManagementResponse response = outOfBandManagementService.changePassword(host, getPassword());
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }

    @Override
    public long getEntityOwnerId() {
        return CallContext.current().getCallingAccountId();
    }

    public Long getHostId() {
        return hostId;
    }

    public String getPassword() {
        if (StringUtils.isEmpty(password)) {
            password = _mgr.generateRandomPassword();
        }
        return password;
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_HOST_OUTOFBAND_MANAGEMENT_CHANGE_PASSWORD;
    }

    @Override
    public String getEventDescription() {
        return "change out-of-band management password for host: " + getHostId();
    }

    @Override
    public Long getApiResourceId() {
        return getHostId();
    }

    @Override
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.Host;
    }
}

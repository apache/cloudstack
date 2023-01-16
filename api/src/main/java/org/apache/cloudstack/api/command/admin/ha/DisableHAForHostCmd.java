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

package org.apache.cloudstack.api.command.admin.ha;

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
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.HostHAResponse;
import org.apache.cloudstack.api.response.HostResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.ha.HAConfigManager;
import org.apache.cloudstack.ha.HAResource;

import javax.inject.Inject;

@APICommand(name = "disableHAForHost", description = "Disables HA for a host",
        responseObject = HostHAResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false,
        since = "4.11", authorized = {RoleType.Admin})
public final class DisableHAForHostCmd extends BaseAsyncCmd {

    @Inject
    private HAConfigManager haConfigManager;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.HOST_ID, type = CommandType.UUID, entityType = HostResponse.class,
            description = "ID of the host", required = true, validations = {ApiArgValidator.PositiveNumber})
    private Long hostId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getHostId() {
        return hostId;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public long getEntityOwnerId() {
        return CallContext.current().getCallingAccountId();
    }

    private void setupResponse(final boolean result, final String resourceUuid) {
        final HostHAResponse response = new HostHAResponse();
        response.setId(resourceUuid);
        response.setEnabled(false);
        response.setStatus(result);
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {
        final Host host = _resourceService.getHost(getHostId());
        if (host == null) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, "Unable to find host by ID: " + getHostId());
        }

        final boolean result = haConfigManager.disableHA(host.getId(), HAResource.ResourceType.Host);
        CallContext.current().setEventDetails("Host Id:" + host.getId() + " HA enabled: false");
        CallContext.current().putContextParameter(Host.class, host.getUuid());

        setupResponse(result, host.getUuid());
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_HA_RESOURCE_DISABLE;
    }

    @Override
    public String getEventDescription() {
        return "disable HA for host: " + getHostId();
    }
}

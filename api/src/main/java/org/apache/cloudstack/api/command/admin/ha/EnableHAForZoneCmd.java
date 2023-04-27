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

import com.cloud.dc.DataCenter;
import com.cloud.event.EventTypes;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiArgValidator;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.ha.HAConfigManager;

import javax.inject.Inject;

@APICommand(name = "enableHAForZone", description = "Enables HA for a zone",
        responseObject = SuccessResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false,
        since = "4.11", authorized = {RoleType.Admin})
public final class EnableHAForZoneCmd extends BaseAsyncCmd {

    @Inject
    private HAConfigManager haConfigManager;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ZONE_ID, type = BaseCmd.CommandType.UUID, entityType = ZoneResponse.class,
            description = "ID of the zone", required = true, validations = {ApiArgValidator.PositiveNumber})
    private Long zoneId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getZoneId() {
        return zoneId;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public long getEntityOwnerId() {
        return CallContext.current().getCallingAccountId();
    }

    private void setupResponse(final boolean result) {
        final SuccessResponse response = new SuccessResponse();
        response.setSuccess(result);
        response.setResponseName(getCommandName());
        response.setObjectName("ha");
        setResponseObject(response);
    }

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {
        final DataCenter dataCenter = _resourceService.getZone(getZoneId());
        if (dataCenter == null) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, "Unable to find zone by ID: " + getZoneId());
        }

        final boolean result = haConfigManager.enableHA(dataCenter);
        CallContext.current().setEventDetails("Zone Id:" + dataCenter.getId() + " HA enabled: true");
        CallContext.current().putContextParameter(DataCenter.class, dataCenter.getUuid());

        setupResponse(result);
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_HA_RESOURCE_ENABLE;
    }

    @Override
    public String getEventDescription() {
        return "enable HA for zone: " + getZoneId();
    }

}

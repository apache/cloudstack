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
//
package org.apache.cloudstack.api;

import javax.inject.Inject;

import org.apache.log4j.Logger;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.context.CallContext;

import com.cloud.baremetal.manager.BaremetalVlanManager;
import com.cloud.baremetal.networkservice.BaremetalSwitchResponse;
import com.cloud.event.EventTypes;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;

/**
 * @author fridvin
 */
@APICommand(name = "deleteBaremetalSwitch", description = "deletes baremetal switch", responseObject = SuccessResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false, authorized = {RoleType.Admin})
public class DeleteBaremetalSwitchCmd extends BaseAsyncCmd {
    private static final String s_name = "deletebaremetalswitchresponse";
    public static final Logger s_logger = Logger.getLogger(DeleteBaremetalSwitchCmd.class);

    @Inject
    private BaremetalVlanManager vlanMgr;

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = BaremetalSwitchResponse.class, required = true, description = "baremetal switch ID")
    private Long id;

    public Long getId() {
        return id;
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_BAREMETAL_SWITCH_DELETE;
    }

    @Override
    public String getEventDescription() {
        return "Deleting baremetal switch";
    }

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException,
            NetworkRuleConflictException {
        try {
            vlanMgr.deleteSwitch(this);
            SuccessResponse response = new SuccessResponse(getCommandName());
            setResponseObject(response);
        } catch (Exception e) {
            s_logger.warn(String.format("unable to delete baremetal switch [%s]", getId()), e);
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, e.getMessage());
        }
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

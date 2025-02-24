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
package org.apache.cloudstack.api.command.admin.management;

import com.cloud.event.EventTypes;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.response.ManagementServerResponse;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.context.CallContext;

@APICommand(name = "removeManagementServer", description = "Removes a Management Server.", responseObject = SuccessResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false, authorized = RoleType.Admin)
public class RemoveManagementServerCmd extends BaseCmd {

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = ManagementServerResponse.class, required = true, description = "the ID of the Management Server")
    private Long id;

    public Long getId() {
        return id;
    }

    @Override
    public void execute() {
        boolean result = _mgr.removeManagementServer(this);
        if (result) {
            SuccessResponse response = new SuccessResponse(getCommandName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to remove Management Server.");
        }
    }

    @Override
    public long getEntityOwnerId() {
        return CallContext.current().getCallingAccountId();
    }

    public String getEventType() {
        return EventTypes.EVENT_MANAGEMENT_SERVER_REMOVE;
    }
}

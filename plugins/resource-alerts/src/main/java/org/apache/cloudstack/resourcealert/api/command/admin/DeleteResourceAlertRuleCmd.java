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

package org.apache.cloudstack.resourcealert.api.command.admin;

import javax.inject.Inject;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.resourcealert.ResourceAlertRule;
import org.apache.cloudstack.resourcealert.ResourceAlertService;
import org.apache.cloudstack.resourcealert.api.response.ResourceAlertRuleResponse;

import com.cloud.utils.exception.CloudRuntimeException;

@APICommand(name = "deleteResourceAlertRule",
        description = "Deletes a resource alert rule",
        responseObject = SuccessResponse.class,
        entityType = {ResourceAlertRule.class},
        authorized = {RoleType.Admin},
        since = "4.23.0")
public class DeleteResourceAlertRuleCmd extends BaseCmd {

    @Inject
    ResourceAlertService resourceAlertService;

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID,
            entityType = ResourceAlertRuleResponse.class,
            required = true,
            description = "the ID of the alert rule to delete")
    private Long id;

    public Long getId() { return id; }

    @Override
    public long getEntityOwnerId() {
        return CallContext.current().getCallingAccountId();
    }

    @Override
    public void execute() throws ServerApiException {
        try {
            boolean result = resourceAlertService.deleteResourceAlertRule(this);
            if (!result) {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to delete resource alert rule");
            }
            SuccessResponse response = new SuccessResponse(getCommandName());
            setResponseObject(response);
        } catch (CloudRuntimeException e) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, e.getMessage());
        }
    }
}

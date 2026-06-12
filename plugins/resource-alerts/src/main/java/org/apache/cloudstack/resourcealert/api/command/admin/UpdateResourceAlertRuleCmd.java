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
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.resourcealert.ResourceAlertRule;
import org.apache.cloudstack.resourcealert.ResourceAlertService;
import org.apache.cloudstack.resourcealert.api.response.ResourceAlertRuleResponse;

import com.cloud.utils.exception.CloudRuntimeException;

@APICommand(name = "updateResourceAlertRule",
        description = "Updates a resource alert rule",
        responseObject = ResourceAlertRuleResponse.class,
        entityType = {ResourceAlertRule.class},
        authorized = {RoleType.Admin},
        since = "4.23.0")
public class UpdateResourceAlertRuleCmd extends BaseCmd {

    @Inject
    ResourceAlertService resourceAlertService;

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID,
            entityType = ResourceAlertRuleResponse.class,
            required = true,
            description = "the ID of the alert rule to update")
    private Long id;

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING,
            description = "new name for the rule")
    private String name;

    @Parameter(name = "condition", type = CommandType.STRING,
            description = "new comparison operator: GT, GTE, LT, LTE, EQ")
    private String condition;

    @Parameter(name = "threshold", type = CommandType.DOUBLE,
            description = "new threshold value")
    private Double threshold;

    @Parameter(name = "severity", type = CommandType.STRING,
            description = "new severity: CRITICAL, HIGH, MEDIUM, LOW")
    private String severity;

    @Parameter(name = ApiConstants.MESSAGE, type = CommandType.STRING,
            description = "new alert message")
    private String message;

    @Parameter(name = "email", type = CommandType.BOOLEAN,
            description = "enable or disable email notification")
    private Boolean email;

    @Parameter(name = "resetinterval", type = CommandType.INTEGER,
            description = "new minimum seconds between repeat firings")
    private Integer resetInterval;

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getCondition() { return condition; }
    public Double getThreshold() { return threshold; }
    public String getSeverity() { return severity; }
    public String getMessage() { return message; }
    public Boolean getEmail() { return email; }
    public Integer getResetInterval() { return resetInterval; }

    @Override
    public long getEntityOwnerId() {
        return CallContext.current().getCallingAccountId();
    }

    @Override
    public void execute() throws ServerApiException {
        try {
            ResourceAlertRuleResponse response = resourceAlertService.updateResourceAlertRule(this);
            if (response == null) {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to update resource alert rule");
            }
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } catch (CloudRuntimeException e) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, e.getMessage());
        }
    }
}

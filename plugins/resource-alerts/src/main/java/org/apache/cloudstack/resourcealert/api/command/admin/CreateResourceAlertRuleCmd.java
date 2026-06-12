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

@APICommand(name = "createResourceAlertRule",
        description = "Creates a resource alert rule",
        responseObject = ResourceAlertRuleResponse.class,
        entityType = {ResourceAlertRule.class},
        authorized = {RoleType.Admin},
        since = "4.23.0")
public class CreateResourceAlertRuleCmd extends BaseCmd {

    @Inject
    ResourceAlertService resourceAlertService;

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, required = true,
            description = "name of the alert rule")
    private String name;

    @Parameter(name = "resourcetype", type = CommandType.STRING, required = true,
            description = "type of resource to monitor: VirtualMachine, Volume, Host, StoragePool")
    private String resourceType;

    @Parameter(name = "resourceid", type = CommandType.LONG,
            description = "ID of the specific resource to monitor; omit for a generic rule covering all resources of this type")
    private Long resourceId;

    @Parameter(name = "metric", type = CommandType.STRING, required = true,
            description = "metric to monitor (e.g. CPU_UTILIZATION, MEMORY_UTILIZATION)")
    private String metric;

    @Parameter(name = "condition", type = CommandType.STRING, required = true,
            description = "comparison operator: GT, GTE, LT, LTE, EQ")
    private String condition;

    @Parameter(name = "threshold", type = CommandType.DOUBLE, required = true,
            description = "threshold value that triggers the alert")
    private Double threshold;

    @Parameter(name = "severity", type = CommandType.STRING, required = true,
            description = "alert severity: CRITICAL, HIGH, MEDIUM, LOW")
    private String severity;

    @Parameter(name = ApiConstants.MESSAGE, type = CommandType.STRING,
            description = "custom message to include in the alert")
    private String message;

    @Parameter(name = "email", type = CommandType.BOOLEAN,
            description = "true to send email notification when this rule fires (admin SMTP must be configured)")
    private Boolean email;

    @Parameter(name = "resetinterval", type = CommandType.INTEGER,
            description = "minimum seconds between repeat firings of this rule (default: 600)")
    private Integer resetInterval;

    @Parameter(name = ApiConstants.ACCOUNT, type = CommandType.STRING,
            description = "account to associate this rule with (defaults to caller)")
    private String accountName;

    @Parameter(name = ApiConstants.DOMAIN_ID, type = CommandType.UUID,
            entityType = org.apache.cloudstack.api.response.DomainResponse.class,
            description = "domain to associate this rule with")
    private Long domainId;

    public String getName() { return name; }
    public String getResourceType() { return resourceType; }
    public Long getResourceId() { return resourceId; }
    public String getMetric() { return metric; }
    public String getCondition() { return condition; }
    public Double getThreshold() { return threshold; }
    public String getSeverity() { return severity; }
    public String getMessage() { return message; }
    public Boolean getEmail() { return email; }
    public Integer getResetInterval() { return resetInterval; }
    public String getAccountName() { return accountName; }
    public Long getDomainId() { return domainId; }

    @Override
    public long getEntityOwnerId() {
        return CallContext.current().getCallingAccountId();
    }

    @Override
    public void execute() throws ServerApiException {
        try {
            ResourceAlertRuleResponse response = resourceAlertService.createResourceAlertRule(this);
            if (response == null) {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to create resource alert rule");
            }
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } catch (CloudRuntimeException e) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, e.getMessage());
        }
    }
}

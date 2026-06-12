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

import java.util.Date;

import javax.inject.Inject;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.resourcealert.ResourceAlert;
import org.apache.cloudstack.resourcealert.ResourceAlertService;
import org.apache.cloudstack.resourcealert.api.response.ResourceAlertResponse;
import org.apache.cloudstack.resourcealert.api.response.ResourceAlertRuleResponse;

@APICommand(name = "listResourceAlerts",
        description = "Lists fired resource alerts",
        responseObject = ResourceAlertResponse.class,
        entityType = {ResourceAlert.class},
        authorized = {RoleType.Admin},
        since = "4.23.0")
public class ListResourceAlertsCmd extends BaseListCmd {

    @Inject
    ResourceAlertService resourceAlertService;

    @Parameter(name = "alertruleid", type = CommandType.UUID,
            entityType = ResourceAlertRuleResponse.class,
            description = "filter by the rule that fired the alert")
    private Long alertRuleId;

    @Parameter(name = "resourceid", type = CommandType.LONG,
            description = "filter by the resource that triggered the alert")
    private Long resourceId;

    @Parameter(name = "severity", type = CommandType.STRING,
            description = "filter by severity: CRITICAL, HIGH, MEDIUM, LOW")
    private String severity;

    @Parameter(name = ApiConstants.START_DATE, type = CommandType.DATE,
            description = "filter alerts fired on or after this date")
    private Date startDate;

    @Parameter(name = ApiConstants.END_DATE, type = CommandType.DATE,
            description = "filter alerts fired on or before this date")
    private Date endDate;

    public Long getAlertRuleId() { return alertRuleId; }
    public Long getResourceId() { return resourceId; }
    public String getSeverity() { return severity; }
    public Date getStartDate() { return startDate; }
    public Date getEndDate() { return endDate; }

    @Override
    public void execute() throws ServerApiException {
        ListResponse<ResourceAlertResponse> response = resourceAlertService.listResourceAlerts(this);
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }
}

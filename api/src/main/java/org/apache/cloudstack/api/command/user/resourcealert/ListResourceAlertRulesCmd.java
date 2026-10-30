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

package org.apache.cloudstack.api.command.user.resourcealert;

import javax.inject.Inject;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListProjectAndAccountResourcesCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ListResponse;
import com.cloud.alert.ResourceAlertRule;
import org.apache.cloudstack.alert.ResourceAlertService;
import org.apache.cloudstack.api.response.ResourceAlertRuleResponse;

@APICommand(name = "listResourceAlertRules",
        description = "Lists resource alert rules",
        responseObject = ResourceAlertRuleResponse.class,
        entityType = {ResourceAlertRule.class},
        authorized = {RoleType.Admin, RoleType.ResourceAdmin, RoleType.DomainAdmin, RoleType.User},
        since = "24.0.0")
public class ListResourceAlertRulesCmd extends BaseListProjectAndAccountResourcesCmd {

    @Inject
    ResourceAlertService resourceAlertService;

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID,
            entityType = ResourceAlertRuleResponse.class,
            description = "the ID of the alert rule")
    private Long id;

    @Parameter(name = ApiConstants.RESOURCE_TYPE, type = CommandType.STRING,
            description = "filter by resource type: VirtualMachine, Volume, Host, StoragePool")
    private String resourceType;

    @Parameter(name = ApiConstants.RESOURCE_ID, type = CommandType.STRING,
            description = "filter by UUID of a specific resource")
    private String resourceId;

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING,
            description = "filter by rule name")
    private String name;

    @Parameter(name = ApiConstants.METRIC, type = CommandType.STRING,
            description = "filter by metric, for example CpuUtilization")
    private String metric;

    @Parameter(name = ApiConstants.SEVERITY, type = CommandType.STRING,
            description = "filter by severity: Low, Medium, High, Critical")
    private String severity;

    public Long getId() { return id; }
    public String getResourceType() { return resourceType; }
    public String getResourceId() { return resourceId; }
    public String getRuleName() { return name; }
    public String getMetric() { return metric; }
    public String getSeverity() { return severity; }

    @Override
    public void execute() throws ServerApiException {
        ListResponse<ResourceAlertRuleResponse> response = resourceAlertService.listResourceAlertRules(this);
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }
}

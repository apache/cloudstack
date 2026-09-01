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

package org.apache.cloudstack.api.command.user.bootgroup;

import javax.inject.Inject;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.command.user.UserCmd;
import org.apache.cloudstack.api.response.InstanceBootGroupReadinessRuleResponse;
import org.apache.cloudstack.api.response.InstanceBootGroupResponse;
import org.apache.cloudstack.api.response.InstanceGroupResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.UserVmResponse;
import org.apache.cloudstack.vm.bootgroup.readiness.InstanceBootGroupReadinessRule;
import org.apache.cloudstack.vm.bootgroup.InstanceBootGroupService;

@APICommand(name = "listInstanceBootGroupReadinessRules",
        description = "Lists readiness rules for an Instance Boot Group",
        responseObject = InstanceBootGroupReadinessRuleResponse.class,
        entityType = {InstanceBootGroupReadinessRule.class},
        requestHasSensitiveInfo = false,
        responseHasSensitiveInfo = false,
        authorized = {RoleType.Admin, RoleType.ResourceAdmin, RoleType.DomainAdmin, RoleType.User})
public class ListInstanceBootGroupReadinessRulesCmd extends BaseListCmd implements UserCmd {

    @Inject
    InstanceBootGroupService instanceBootGroupService;

    @Parameter(name = ApiConstants.BOOT_GROUP_ID, type = CommandType.UUID, entityType = InstanceBootGroupResponse.class, required = true,
            description = "The ID of the Instance Boot Group; listing is always scoped to one boot group")
    private Long bootGroupId;

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = InstanceBootGroupReadinessRuleResponse.class, description = "List by readiness rule ID")
    private Long id;

    @Parameter(name = ApiConstants.VIRTUAL_MACHINE_ID, type = CommandType.UUID, entityType = UserVmResponse.class, description = "List rules for the Instance")
    private Long virtualMachineId;

    @Parameter(name = ApiConstants.INSTANCE_GROUP_ID, type = CommandType.UUID, entityType = InstanceGroupResponse.class, description = "List rules for the Instance Group")
    private Long instanceGroupId;

    @Parameter(name = ApiConstants.RULE_TYPE, type = CommandType.STRING, description = "Filter by readiness rule type")
    private String ruleType;

    public Long getBootGroupId() {
        return bootGroupId;
    }

    public Long getId() {
        return id;
    }

    public Long getVirtualMachineId() {
        return virtualMachineId;
    }

    public Long getInstanceGroupId() {
        return instanceGroupId;
    }

    public String getRuleType() {
        return ruleType;
    }

    @Override
    public void execute() {
        ListResponse<InstanceBootGroupReadinessRuleResponse> response = instanceBootGroupService.listInstanceBootGroupReadinessRules(this);
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }
}

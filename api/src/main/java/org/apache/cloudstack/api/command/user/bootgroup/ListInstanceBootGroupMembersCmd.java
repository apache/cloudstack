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

import java.util.List;

import javax.inject.Inject;

import org.apache.commons.lang3.BooleanUtils;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.command.user.UserCmd;
import org.apache.cloudstack.api.response.InstanceBootGroupMemberResponse;
import org.apache.cloudstack.api.response.InstanceBootGroupResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.vm.bootgroup.InstanceBootGroupMember;
import org.apache.cloudstack.vm.bootgroup.InstanceBootGroupService;

@APICommand(name = "listInstanceBootGroupMembers",
        description = "Lists members of an Instance Boot Group, sorted by boot order",
        since = "4.24.0",
        responseObject = InstanceBootGroupMemberResponse.class,
        entityType = {InstanceBootGroupMember.class},
        requestHasSensitiveInfo = false,
        responseHasSensitiveInfo = false,
        authorized = {RoleType.Admin, RoleType.ResourceAdmin, RoleType.DomainAdmin, RoleType.User})
public class ListInstanceBootGroupMembersCmd extends BaseListCmd implements UserCmd {

    @Inject
    InstanceBootGroupService instanceBootGroupService;

    @Parameter(name = ApiConstants.BOOT_GROUP_ID, type = CommandType.UUID, required = true, entityType = InstanceBootGroupResponse.class, description = "The ID of the Instance Boot Group")
    private Long bootGroupId;

    @Parameter(name = ApiConstants.MEMBER_TYPE, type = CommandType.STRING, description = "Filter by member type: VirtualMachine or InstanceGroup")
    private String memberType;

    @Parameter(name = ApiConstants.DETAILS,
               type = CommandType.LIST,
               collectionType = CommandType.STRING,
               description = "Comma separated list of additional details requested, value can be a list of [all, readiness, children]. "
                       + "Readiness fields are computed from cached check results (not a live re-check) and are omitted unless requested, since computing them is not free. "
                       + "Children returns the member Instances of InstanceGroup-type members (omitted for VirtualMachine-type members); combine with readiness to also include per-child readiness")
    private List<String> viewDetails;

    @Parameter(name = ApiConstants.IGNORE_INSTANCE_STATE, type = CommandType.BOOLEAN,
               description = "If true, readiness status/message reflect the last cached rule check regardless of the member's current instance state. "
                       + "If false (default), an Instance that isn't Running is always reported NotReady, even if its rules were last cached Ready")
    private Boolean ignoreInstanceState;

    public Long getBootGroupId() {
        return bootGroupId;
    }

    public String getMemberType() {
        return memberType;
    }

    public boolean isIgnoreInstanceState() {
        return BooleanUtils.toBoolean(ignoreInstanceState);
    }

    public boolean isReadinessDetailRequested() {
        return viewDetails != null && (viewDetails.contains("readiness") || viewDetails.contains("all"));
    }

    public boolean isChildrenDetailRequested() {
        return viewDetails != null && (viewDetails.contains("children") || viewDetails.contains("all"));
    }

    @Override
    public void execute() {
        ListResponse<InstanceBootGroupMemberResponse> response = instanceBootGroupService.listInstanceBootGroupMembers(this);
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }
}

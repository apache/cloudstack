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

package org.apache.cloudstack.vm.bootgroup;

import org.apache.cloudstack.api.command.user.bootgroup.AddMemberToInstanceBootGroupCmd;
import org.apache.cloudstack.api.command.user.bootgroup.CreateInstanceBootGroupCmd;
import org.apache.cloudstack.api.command.user.bootgroup.CreateInstanceBootGroupReadinessRuleCmd;
import org.apache.cloudstack.api.command.user.bootgroup.DeleteInstanceBootGroupCmd;
import org.apache.cloudstack.api.command.user.bootgroup.DeleteInstanceBootGroupReadinessRuleCmd;
import org.apache.cloudstack.api.command.user.bootgroup.ListInstanceBootGroupMembersCmd;
import org.apache.cloudstack.api.command.user.bootgroup.ListInstanceBootGroupReadinessRulesCmd;
import org.apache.cloudstack.api.command.user.bootgroup.ListInstanceBootGroupsCmd;
import org.apache.cloudstack.api.command.user.bootgroup.RebootInstanceBootGroupCmd;
import org.apache.cloudstack.api.command.user.bootgroup.RemoveInstanceBootGroupMemberCmd;
import org.apache.cloudstack.api.command.user.bootgroup.StartInstanceBootGroupCmd;
import org.apache.cloudstack.api.command.user.bootgroup.StopInstanceBootGroupCmd;
import org.apache.cloudstack.api.command.user.bootgroup.UpdateInstanceBootGroupCmd;
import org.apache.cloudstack.api.command.user.bootgroup.UpdateInstanceBootGroupMemberCmd;
import org.apache.cloudstack.api.command.user.bootgroup.UpdateInstanceBootGroupReadinessRuleCmd;
import org.apache.cloudstack.api.response.InstanceBootGroupMemberResponse;
import org.apache.cloudstack.api.response.InstanceBootGroupReadinessRuleResponse;
import org.apache.cloudstack.api.response.InstanceBootGroupResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.vm.bootgroup.readiness.InstanceBootGroupReadinessRule;

public interface InstanceBootGroupService {

    InstanceBootGroup createInstanceBootGroup(CreateInstanceBootGroupCmd cmd);

    boolean deleteInstanceBootGroup(DeleteInstanceBootGroupCmd cmd);

    InstanceBootGroup updateInstanceBootGroup(UpdateInstanceBootGroupCmd cmd);

    ListResponse<InstanceBootGroupResponse> listInstanceBootGroups(ListInstanceBootGroupsCmd cmd);

    InstanceBootGroupMember addMemberToInstanceBootGroup(AddMemberToInstanceBootGroupCmd cmd);

    boolean removeInstanceBootGroupMember(RemoveInstanceBootGroupMemberCmd cmd);

    InstanceBootGroupMember updateInstanceBootGroupMember(UpdateInstanceBootGroupMemberCmd cmd);

    ListResponse<InstanceBootGroupMemberResponse> listInstanceBootGroupMembers(ListInstanceBootGroupMembersCmd cmd);

    InstanceBootGroup startInstanceBootGroup(StartInstanceBootGroupCmd cmd);

    InstanceBootGroup stopInstanceBootGroup(StopInstanceBootGroupCmd cmd);

    InstanceBootGroup rebootInstanceBootGroup(RebootInstanceBootGroupCmd cmd);

    InstanceBootGroupResponse createInstanceBootGroupResponse(long id);

    InstanceBootGroupMemberResponse createInstanceBootGroupMemberResponse(InstanceBootGroupMember member);

    InstanceBootGroupReadinessRule createInstanceBootGroupReadinessRule(CreateInstanceBootGroupReadinessRuleCmd cmd);

    InstanceBootGroupReadinessRule updateInstanceBootGroupReadinessRule(UpdateInstanceBootGroupReadinessRuleCmd cmd);

    boolean deleteInstanceBootGroupReadinessRule(DeleteInstanceBootGroupReadinessRuleCmd cmd);

    ListResponse<InstanceBootGroupReadinessRuleResponse> listInstanceBootGroupReadinessRules(ListInstanceBootGroupReadinessRulesCmd cmd);

    InstanceBootGroupReadinessRuleResponse createInstanceBootGroupReadinessRuleResponse(InstanceBootGroupReadinessRule rule);

    Long getInstanceBootGroupIdForMember(long memberId);

}

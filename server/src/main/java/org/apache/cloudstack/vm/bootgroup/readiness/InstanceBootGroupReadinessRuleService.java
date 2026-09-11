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

package org.apache.cloudstack.vm.bootgroup.readiness;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.cloudstack.vm.bootgroup.InstanceBootGroupMember;

/**
 * Backend counterpart consumed by {@code InstanceBootGroupApiServiceImpl} for mutating readiness-rule
 * operations and evaluation. Listing goes straight through the DAO from the API layer, same as
 * {@code InstanceBootGroupMember} listing does — this interface only covers create/update/delete and
 * evaluation, which need the domain validation in {@code InstanceBootGroupReadinessRuleManagerImpl}.
 */
public interface InstanceBootGroupReadinessRuleService {

    InstanceBootGroupReadinessRule createReadinessRule(long bootGroupId, InstanceBootGroupMember.MemberType itemType, long itemId,
                                                       InstanceBootGroupReadinessRule.RuleType ruleType, String name, boolean enabled, Map<String, String> details);

    InstanceBootGroupReadinessRule updateReadinessRule(long ruleId, String name, Boolean enabled, Map<String, String> details);

    boolean deleteReadinessRule(long ruleId);

    InstanceBootGroupReadinessRule findById(long ruleId);

    Map<String, String> getRuleDetails(long ruleId);

    /**
     * AND across all enabled rules for this VM within the boot group; Ready if none are attached
     * and the VM is Running.
     * @param remainingMs dispatch time budget left for this VM's current attempt; pass a generous
     *        value (e.g. {@code Long.MAX_VALUE}) outside budget-tracking contexts.
     * @param attemptLabel appended to each persisted rule message (e.g. {@code "2/5"}); pass
     *        {@code null} to skip.
     */
    InstanceBootGroupReadinessRule.Status evaluateVmReadiness(long bootGroupId, long vmId, long remainingMs, String attemptLabel);

    /**
     * AND of the group's own enabled rules (e.g. MemberQuorum) and every member VM's own readiness.
     * @param permanentlyFailedVmIds members the caller has given up retrying — lets a MemberQuorum
     *        rule distinguish "not met yet" from "mathematically impossible"; pass an empty set
     *        outside that orchestration context.
     */
    InstanceBootGroupReadinessRule.Status evaluateInstanceGroupReadiness(long bootGroupId, long instanceGroupId, Set<Long> permanentlyFailedVmIds);

    /**
     * The Ping/PortCheck/GuestAgentLiveness rules {@code vmId} inherits from its owning
     * InstanceGroup, if any — empty if none apply.
     */
    List<InstanceBootGroupReadinessRule> findInheritedGroupRules(long bootGroupId, long vmId);

    /**
     * Resets {@code vmId}'s own cached rule results (direct and inherited) to Unknown — called when
     * the VM starts, so a restart outside boot group orchestration can't keep reporting a stale Ready
     * from before it stopped. A no-op if the VM has no boot-group involvement.
     */
    void invalidateCachedReadinessOnRestart(long vmId);
}

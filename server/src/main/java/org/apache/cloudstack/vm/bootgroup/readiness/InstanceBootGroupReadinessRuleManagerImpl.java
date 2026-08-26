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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.cloudstack.vm.bootgroup.InstanceBootGroupMember;
import org.apache.cloudstack.vm.bootgroup.InstanceBootGroupMemberVO;
import org.apache.cloudstack.vm.bootgroup.InstanceBootGroupReadinessCheckResultVO;
import org.apache.cloudstack.vm.bootgroup.InstanceBootGroupReadinessRuleVO;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallback;
import com.cloud.vm.InstanceGroupVMMapVO;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.InstanceBootGroupMemberDao;
import com.cloud.vm.dao.InstanceBootGroupReadinessCheckResultDao;
import com.cloud.vm.dao.InstanceBootGroupReadinessRuleDao;
import com.cloud.vm.dao.InstanceBootGroupReadinessRuleDetailsDao;
import com.cloud.vm.dao.InstanceGroupDao;
import com.cloud.vm.dao.InstanceGroupVMMapDao;
import com.cloud.vm.dao.UserVmDao;

@Component
public class InstanceBootGroupReadinessRuleManagerImpl extends ManagerBase implements InstanceBootGroupReadinessRuleService {

    private static final String THRESHOLD_TYPE_KEY = "threshold_type";
    private static final String THRESHOLD_VALUE_KEY = "threshold_value";
    private static final String PORT_KEY = "port";
    private static final String PROTOCOL_KEY = "protocol";

    private static final Map<InstanceBootGroupMember.MemberType, Set<InstanceBootGroupReadinessRule.RuleType>> VALID_RULE_TYPES_BY_ITEM_TYPE = Map.of(
            InstanceBootGroupMember.MemberType.VirtualMachine, EnumSet.of(
                    InstanceBootGroupReadinessRule.RuleType.GuestAgentLiveness,
                    InstanceBootGroupReadinessRule.RuleType.Ping,
                    InstanceBootGroupReadinessRule.RuleType.PortCheck,
                    InstanceBootGroupReadinessRule.RuleType.CustomScript),
            InstanceBootGroupMember.MemberType.InstanceGroup, EnumSet.of(
                    InstanceBootGroupReadinessRule.RuleType.GuestAgentLiveness,
                    InstanceBootGroupReadinessRule.RuleType.Ping,
                    InstanceBootGroupReadinessRule.RuleType.PortCheck,
                    InstanceBootGroupReadinessRule.RuleType.CustomScript,
                    InstanceBootGroupReadinessRule.RuleType.MemberQuorum));

    /**
     * Rule types an item may have at most one of — Ping/GuestAgentLiveness each check a single fixed
     * target on the VM, and MemberQuorum aggregates the whole InstanceGroup, so a second one would
     * just be redundant. PortCheck (different ports) and CustomScript are not singletons.
     */
    private static final Set<InstanceBootGroupReadinessRule.RuleType> SINGLETON_RULE_TYPES = EnumSet.of(
            InstanceBootGroupReadinessRule.RuleType.Ping,
            InstanceBootGroupReadinessRule.RuleType.GuestAgentLiveness,
            InstanceBootGroupReadinessRule.RuleType.MemberQuorum);

    @Inject
    private InstanceBootGroupReadinessRuleDao instanceBootGroupReadinessRuleDao;

    @Inject
    private InstanceBootGroupReadinessRuleDetailsDao instanceBootGroupReadinessRuleDetailsDao;

    @Inject
    private InstanceBootGroupReadinessCheckResultDao instanceBootGroupReadinessCheckResultDao;

    @Inject
    private InstanceBootGroupMemberDao instanceBootGroupMemberDao;

    @Inject
    private InstanceGroupVMMapDao instanceGroupVMMapDao;

    @Inject
    private InstanceGroupDao instanceGroupDao;

    @Inject
    private UserVmDao userVmDao;

    private List<ReadinessChecker> readinessCheckers;

    private Map<InstanceBootGroupReadinessRule.RuleType, ReadinessChecker> checkersByRuleType;

    protected void updateCheckersByRuleType(boolean forced) {
        if (MapUtils.isNotEmpty(checkersByRuleType) && !forced) {
            return;
        }
        checkersByRuleType = new HashMap<>();
        for (ReadinessChecker checker : readinessCheckers) {
            checkersByRuleType.put(checker.getRuleType(), checker);
        }
    }

    protected ReadinessChecker getCheckerByRuleType(InstanceBootGroupReadinessRule.RuleType ruleType) {
        updateCheckersByRuleType(false);
        return checkersByRuleType.get(ruleType);
    }

    public List<ReadinessChecker> getReadinessCheckers() {
        return readinessCheckers;
    }

    public void setReadinessCheckers(List<ReadinessChecker> readinessCheckers) {
        this.readinessCheckers = readinessCheckers;
        updateCheckersByRuleType(true);
    }

    @Override
    public InstanceBootGroupReadinessRule createReadinessRule(long bootGroupId, InstanceBootGroupMember.MemberType itemType, long itemId,
            InstanceBootGroupReadinessRule.RuleType ruleType, String name, boolean enabled, Map<String, String> details) {
        validateRuleTypeForItemType(itemType, ruleType);
        validateItemBelongsToBootGroup(bootGroupId, itemType, itemId);
        validateSingletonRuleType(bootGroupId, itemType, itemId, ruleType);
        validateGuestAgentLivenessSupported(itemType, itemId, ruleType);
        validateRuleTypeSpecificDetails(ruleType, details);

        String effectiveName = StringUtils.isNotBlank(name) ? name : String.format("%s-%s-%d", ruleType.name(), itemType.name(), itemId);
        InstanceBootGroupReadinessRuleVO rule = new InstanceBootGroupReadinessRuleVO(effectiveName, bootGroupId, itemType, itemId, ruleType, enabled);
        rule = instanceBootGroupReadinessRuleDao.persist(rule);

        if (details != null) {
            for (Map.Entry<String, String> entry : details.entrySet()) {
                instanceBootGroupReadinessRuleDetailsDao.addDetail(rule.getId(), entry.getKey(), entry.getValue(), true);
            }
        }
        return rule;
    }

    @Override
    public InstanceBootGroupReadinessRule updateReadinessRule(long ruleId, String name, Boolean enabled, Map<String, String> details) {
        InstanceBootGroupReadinessRuleVO rule = instanceBootGroupReadinessRuleDao.findById(ruleId);
        if (rule == null) {
            throw new InvalidParameterValueException("Unable to find a readiness rule with ID: " + ruleId);
        }

        if (StringUtils.isNotBlank(name)) {
            rule.setName(name);
        }
        if (enabled != null) {
            rule.setEnabled(enabled);
        }
        if (details != null) {
            Map<String, String> mergedDetails = new HashMap<>(instanceBootGroupReadinessRuleDetailsDao.getDetails(rule.getId()));
            mergedDetails.putAll(details);
            validateRuleTypeSpecificDetails(rule.getRuleType(), mergedDetails);
        }
        instanceBootGroupReadinessRuleDao.update(rule.getId(), rule);

        if (details != null) {
            for (Map.Entry<String, String> entry : details.entrySet()) {
                instanceBootGroupReadinessRuleDetailsDao.addDetail(rule.getId(), entry.getKey(), entry.getValue(), true);
            }
        }
        return instanceBootGroupReadinessRuleDao.findById(rule.getId());
    }

    @Override
    public boolean deleteReadinessRule(long ruleId) {
        InstanceBootGroupReadinessRuleVO rule = instanceBootGroupReadinessRuleDao.findById(ruleId);
        if (rule == null) {
            throw new InvalidParameterValueException("Unable to find a readiness rule with ID: " + ruleId);
        }
        return Transaction.execute((TransactionCallback<Boolean>) status -> {
            instanceBootGroupReadinessRuleDetailsDao.removeDetails(rule.getId());
            instanceBootGroupReadinessCheckResultDao.deleteByRuleId(rule.getId());
            instanceBootGroupReadinessRuleDao.remove(rule.getId());
            return true;
        });
    }

    @Override
    public InstanceBootGroupReadinessRule findById(long ruleId) {
        return instanceBootGroupReadinessRuleDao.findById(ruleId);
    }

    @Override
    public Map<String, String> getRuleDetails(long ruleId) {
        return instanceBootGroupReadinessRuleDetailsDao.getDetails(ruleId);
    }

    @Override
    public InstanceBootGroupReadinessRule.Status evaluateVmReadiness(long bootGroupId, long vmId, long remainingMs, String attemptLabel) {
        return resolveVmReadiness(bootGroupId, vmId, true, remainingMs, attemptLabel);
    }

    /**
     * Same aggregation as {@link #evaluateVmReadiness}, but reads each rule's last-cached result
     * instead of dispatching a fresh check — for callers that run after the per-VM loop has already
     * dispatched this poll, so a live re-check would just repeat the same remote command.
     */
    private InstanceBootGroupReadinessRule.Status getCachedVmReadiness(long bootGroupId, long vmId) {
        return resolveVmReadiness(bootGroupId, vmId, false, Long.MAX_VALUE, null);
    }

    /**
     * Shared by the dispatching and cache-reading paths. A non-Running VM is always NotReady regardless
     * of any cached rule result. Direct rules cache at vmId 0, inherited group rules per member vmId;
     * when dispatching, the remaining budget shrinks across a VM's rules so N slow rules can't each burn
     * the full per-attempt timeout.
     */
    private InstanceBootGroupReadinessRule.Status resolveVmReadiness(long bootGroupId, long vmId, boolean dispatch, long remainingMs, String attemptLabel) {
        UserVmVO vm = userVmDao.findById(vmId);
        if (vm == null) {
            logger.debug("VM id {} not found while evaluating readiness for boot group id {}; treating as NotReady", vmId, bootGroupId);
            return InstanceBootGroupReadinessRule.Status.NotReady;
        }
        if (!VirtualMachine.State.Running.equals(vm.getState())) {
            logger.debug("{} is not Running (state={}) for boot group id {}; treating as NotReady without evaluating or dispatching its readiness rules", vm, vm.getState(), bootGroupId);
            return InstanceBootGroupReadinessRule.Status.NotReady;
        }
        List<InstanceBootGroupReadinessRuleVO> directRules = instanceBootGroupReadinessRuleDao.listEnabledByItem(bootGroupId, InstanceBootGroupMember.MemberType.VirtualMachine, vmId);
        List<InstanceBootGroupReadinessRuleVO> inheritedRules = findInheritedGroupRuleVOs(bootGroupId, vmId);

        if (directRules.isEmpty() && inheritedRules.isEmpty()) {
            logger.debug("{} has no readiness rules for boot group id {}; derived readiness Ready from its current state ({})", vm, bootGroupId, vm.getState());
            return InstanceBootGroupReadinessRule.Status.Ready;
        }

        if (dispatch) {
            logger.debug("Evaluating readiness of {} for boot group id {}: {} direct rule(s), {} inherited rule(s), {}ms remaining budget",
                    vm, bootGroupId, directRules.size(), inheritedRules.size(), remainingMs);
        }

        boolean anyError = false;
        boolean anyNotReady = false;
        long remainingBudgetMs = remainingMs;
        for (InstanceBootGroupReadinessRuleVO rule : directRules) {
            long startedAtMs = System.currentTimeMillis();
            InstanceBootGroupReadinessRule.Status status = dispatch ? evaluateAndCacheRule(rule, vmId, 0, remainingBudgetMs, attemptLabel) : readCachedRuleStatus(rule.getId(), 0);
            if (dispatch) {
                remainingBudgetMs = Math.max(0, remainingBudgetMs - (System.currentTimeMillis() - startedAtMs));
            }
            if (status == InstanceBootGroupReadinessRule.Status.Error) {
                anyError = true;
            } else if (status != InstanceBootGroupReadinessRule.Status.Ready) {
                anyNotReady = true;
            }
        }
        for (InstanceBootGroupReadinessRuleVO rule : inheritedRules) {
            long startedAtMs = System.currentTimeMillis();
            InstanceBootGroupReadinessRule.Status status = dispatch ? evaluateAndCacheRule(rule, vmId, vmId, remainingBudgetMs, attemptLabel) : readCachedRuleStatus(rule.getId(), vmId);
            if (dispatch) {
                remainingBudgetMs = Math.max(0, remainingBudgetMs - (System.currentTimeMillis() - startedAtMs));
            }
            if (status == InstanceBootGroupReadinessRule.Status.Error) {
                anyError = true;
            } else if (status != InstanceBootGroupReadinessRule.Status.Ready) {
                anyNotReady = true;
            }
        }

        InstanceBootGroupReadinessRule.Status overallStatus = anyError ? InstanceBootGroupReadinessRule.Status.Error
                : (anyNotReady ? InstanceBootGroupReadinessRule.Status.NotReady : InstanceBootGroupReadinessRule.Status.Ready);
        if (dispatch) {
            logger.debug("{} readiness for boot group id {} evaluated as {}", vm, bootGroupId, overallStatus);
        }
        return overallStatus;
    }

    private InstanceBootGroupReadinessRule.Status readCachedRuleStatus(long ruleId, long cacheVmId) {
        InstanceBootGroupReadinessCheckResultVO cached = instanceBootGroupReadinessCheckResultDao.findByRuleAndVm(ruleId, cacheVmId);
        return (cached != null && cached.getStatus() != null) ? cached.getStatus() : InstanceBootGroupReadinessRule.Status.Unknown;
    }

    /**
     * Dispatches (or, for a missing checker, synthesizes) one rule's check and persists the result —
     * the single place a check actually happens, so every caller shares this one log/cache path.
     * @param attemptLabel e.g. {@code "2/5"}, appended to the persisted message; pass {@code null} to skip.
     */
    private InstanceBootGroupReadinessRule.Status evaluateAndCacheRule(InstanceBootGroupReadinessRuleVO rule, long vmId, long cacheVmId, long remainingMs, String attemptLabel) {
        logger.debug("Evaluating rule {} against VM id {} with {}ms remaining budget", () -> rule, () -> userVmDao.findById(vmId), () -> remainingMs);
        ReadinessChecker checker = getCheckerByRuleType(rule.getRuleType());
        InstanceBootGroupReadinessRule.Status status;
        String message;
        if (checker == null) {
            status = InstanceBootGroupReadinessRule.Status.Error;
            message = "No checker implemented yet for rule type " + rule.getRuleType();
        } else {
            Map<String, String> details = instanceBootGroupReadinessRuleDetailsDao.getDetails(rule.getId());
            ReadinessChecker.Result result = checker.check(rule, details, vmId, remainingMs);
            status = result.getStatus();
            message = result.getMessage();
        }
        String finalMessage = StringUtils.isNotBlank(attemptLabel) ? message + " (attempt " + attemptLabel + ")" : message;
        logger.debug("Rule {} evaluated against VM id {}: status={}, message={}", () -> rule, () -> userVmDao.findById(vmId), () -> status, () -> finalMessage);
        instanceBootGroupReadinessCheckResultDao.upsert(rule.getId(), cacheVmId, status, finalMessage, new Date());
        return status;
    }

    @Override
    public List<InstanceBootGroupReadinessRule> findInheritedGroupRules(long bootGroupId, long vmId) {
        return new ArrayList<>(findInheritedGroupRuleVOs(bootGroupId, vmId));
    }

    /**
     * A VM inherits its owning InstanceGroup's Ping/PortCheck/GuestAgentLiveness rules (not
     * MemberQuorum/CustomScript, which only ever make sense at group scope) — resolved by finding
     * the InstanceGroup, among any this VM belongs to, that is itself a member of this boot group.
     */
    private List<InstanceBootGroupReadinessRuleVO> findInheritedGroupRuleVOs(long bootGroupId, long vmId) {
        for (InstanceGroupVMMapVO mapping : instanceGroupVMMapDao.listByInstanceId(vmId)) {
            InstanceBootGroupMemberVO groupMember = instanceBootGroupMemberDao.findByMember(InstanceBootGroupMember.MemberType.InstanceGroup, mapping.getGroupId());
            if (groupMember != null && groupMember.getBootGroupId() == bootGroupId) {
                return instanceBootGroupReadinessRuleDao.listEnabledByItem(bootGroupId, InstanceBootGroupMember.MemberType.InstanceGroup, mapping.getGroupId()).stream()
                        .filter(rule -> rule.getRuleType().isMemberTargeted())
                        .collect(Collectors.toList());
            }
        }
        return Collections.emptyList();
    }

    /**
     * AND of the group's own rules and every member's cached readiness (read-only — the per-VM loop
     * already dispatched this poll). A MemberQuorum rule's own tolerance-aware verdict decides
     * Ready/Error on its own; without one, a member still mid-retry only counts as NotReady, never Error.
     * Once a MemberQuorum rule governs the group, every other member-targeted rule's own all-members
     * aggregate becomes informational only — it's still evaluated and shown, but no longer gates the
     * overall verdict, since that's exactly what attaching a quorum rule is meant to relax.
     */
    @Override
    public InstanceBootGroupReadinessRule.Status evaluateInstanceGroupReadiness(long bootGroupId, long instanceGroupId, Set<Long> permanentlyFailedVmIds) {
        List<InstanceBootGroupReadinessRuleVO> groupRules = instanceBootGroupReadinessRuleDao.listEnabledByItem(bootGroupId, InstanceBootGroupMember.MemberType.InstanceGroup, instanceGroupId);
        List<InstanceGroupVMMapVO> members = instanceGroupVMMapDao.listByGroupId(instanceGroupId);
        boolean hasMemberQuorumRule = groupRules.stream().anyMatch(rule -> rule.getRuleType() == InstanceBootGroupReadinessRule.RuleType.MemberQuorum);

        logger.debug("Evaluating readiness of instance group id {} for boot group id {}: {} member VM(s), {} own rule(s), quorum-governed={}",
                () -> instanceGroupDao.findById(instanceGroupId), () -> bootGroupId, () -> members.size(), () -> groupRules.size(), () -> hasMemberQuorumRule);

        boolean anyError = false;
        boolean anyNotReady = false;

        for (InstanceGroupVMMapVO member : members) {
            InstanceBootGroupReadinessRule.Status vmStatus = getCachedVmReadiness(bootGroupId, member.getInstanceId());
            if (hasMemberQuorumRule || vmStatus == InstanceBootGroupReadinessRule.Status.Ready) {
                continue;
            }
            if (permanentlyFailedVmIds.contains(member.getInstanceId())) {
                anyError = true;
            } else {
                anyNotReady = true;
            }
        }

        for (InstanceBootGroupReadinessRuleVO rule : groupRules) {
            ReadinessChecker.Result result;
            boolean memberTargeted = rule.getRuleType().isMemberTargeted();
            if (rule.getRuleType() == InstanceBootGroupReadinessRule.RuleType.MemberQuorum) {
                logger.debug("Evaluating group-scoped rule {} for instance group id {} via member quorum", rule, instanceGroupId);
                Map<String, String> details = instanceBootGroupReadinessRuleDetailsDao.getDetails(rule.getId());
                result = evaluateInstanceQuorum(bootGroupId, instanceGroupId, details, permanentlyFailedVmIds);
            } else if (memberTargeted) {
                logger.debug("Evaluating group-scoped rule {} for instance group id {} by aggregating its {} member(s)' own cached results", rule, instanceGroupId, members.size());
                result = aggregateMemberTargetedGroupRule(rule, members);
            } else {
                result = new ReadinessChecker.Result(InstanceBootGroupReadinessRule.Status.Error,
                        "No evaluator implemented yet for rule type " + rule.getRuleType());
            }
            logger.debug("Group-scoped rule {} evaluated for instance group id {}: status={}, message={}", rule, instanceGroupId, result.getStatus(), result.getMessage());
            instanceBootGroupReadinessCheckResultDao.upsert(rule.getId(), 0, result.getStatus(), result.getMessage(), new Date());

            if (memberTargeted && hasMemberQuorumRule) {
                continue;
            }
            if (result.getStatus() == InstanceBootGroupReadinessRule.Status.Error) {
                anyError = true;
            } else if (result.getStatus() != InstanceBootGroupReadinessRule.Status.Ready) {
                anyNotReady = true;
            }
        }

        InstanceBootGroupReadinessRule.Status overallStatus = anyError ? InstanceBootGroupReadinessRule.Status.Error
                : (anyNotReady ? InstanceBootGroupReadinessRule.Status.NotReady : InstanceBootGroupReadinessRule.Status.Ready);
        logger.debug("Instance group id {} readiness for boot group id {} evaluated as {}", instanceGroupId, bootGroupId, overallStatus);
        return overallStatus;
    }

    /**
     * Reads the per-member cached results {@link #evaluateVmReadiness} already wrote for this rule,
     * rather than dispatching it again.
     */
    private ReadinessChecker.Result aggregateMemberTargetedGroupRule(InstanceBootGroupReadinessRuleVO rule, List<InstanceGroupVMMapVO> members) {
        if (members.isEmpty()) {
            return new ReadinessChecker.Result(InstanceBootGroupReadinessRule.Status.NotReady, "Instance group has no members");
        }
        boolean anyError = false;
        int readyCount = 0;
        for (InstanceGroupVMMapVO member : members) {
            InstanceBootGroupReadinessCheckResultVO cached = instanceBootGroupReadinessCheckResultDao.findByRuleAndVm(rule.getId(), member.getInstanceId());
            InstanceBootGroupReadinessRule.Status status = (cached != null && cached.getStatus() != null) ? cached.getStatus() : InstanceBootGroupReadinessRule.Status.Unknown;
            if (status == InstanceBootGroupReadinessRule.Status.Ready) {
                readyCount++;
            } else if (status == InstanceBootGroupReadinessRule.Status.Error) {
                anyError = true;
            }
        }
        int total = members.size();
        String message = String.format("%d of %d member(s) ready via %s", readyCount, total, rule.getRuleType().name());
        if (anyError) {
            return new ReadinessChecker.Result(InstanceBootGroupReadinessRule.Status.Error, message);
        }
        return new ReadinessChecker.Result(readyCount == total ? InstanceBootGroupReadinessRule.Status.Ready : InstanceBootGroupReadinessRule.Status.NotReady, message);
    }

    /**
     * Pure computation, no dispatch: counts members currently READY against the configured threshold.
     * @param permanentlyFailedVmIds excluded from "achievable" so a hopeless quorum reports Error
     *        instead of NotReady once it can never be met, even with every remaining member succeeding.
     */
    private ReadinessChecker.Result evaluateInstanceQuorum(long bootGroupId, long instanceGroupId, Map<String, String> details, Set<Long> permanentlyFailedVmIds) {
        List<InstanceGroupVMMapVO> members = instanceGroupVMMapDao.listByGroupId(instanceGroupId);
        int total = members.size();
        if (total == 0) {
            return new ReadinessChecker.Result(InstanceBootGroupReadinessRule.Status.NotReady, "Instance group has no members");
        }

        long readyCount = members.stream()
                .filter(member -> getCachedVmReadiness(bootGroupId, member.getInstanceId()) == InstanceBootGroupReadinessRule.Status.Ready)
                .count();
        long permanentlyFailedCount = members.stream()
                .filter(member -> permanentlyFailedVmIds.contains(member.getInstanceId()))
                .count();
        long achievableCount = total - permanentlyFailedCount;

        String thresholdType = details == null ? null : details.get(THRESHOLD_TYPE_KEY);
        String thresholdValue = details == null ? null : details.get(THRESHOLD_VALUE_KEY);

        boolean met;
        boolean achievable;
        try {
            if ("PERCENTAGE".equalsIgnoreCase(thresholdType)) {
                double thresholdPercentage = Double.parseDouble(thresholdValue);
                met = (readyCount * 100.0 / total) >= thresholdPercentage;
                achievable = (achievableCount * 100.0 / total) >= thresholdPercentage;
            } else {
                long thresholdCount = Long.parseLong(thresholdValue);
                met = readyCount >= thresholdCount;
                achievable = achievableCount >= thresholdCount;
            }
        } catch (NumberFormatException e) {
            return new ReadinessChecker.Result(InstanceBootGroupReadinessRule.Status.Error, "Invalid threshold configuration: " + thresholdType + "=" + thresholdValue);
        }

        String message = String.format("%d/%d members ready (%s threshold %s)", readyCount, total, thresholdType, thresholdValue);
        if (!met && !achievable) {
            String reason = String.format("%s; unreachable — %d/%d member(s) have permanently failed readiness", message, permanentlyFailedCount, total);
            logger.debug("Instance group id {} quorum check: {}", instanceGroupId, reason);
            return new ReadinessChecker.Result(InstanceBootGroupReadinessRule.Status.Error, reason);
        }
        logger.debug("Instance group id {} quorum check: {}, met={}", instanceGroupId, message, met);
        return new ReadinessChecker.Result(met ? InstanceBootGroupReadinessRule.Status.Ready : InstanceBootGroupReadinessRule.Status.NotReady, message);
    }

    private void validateRuleTypeSpecificDetails(InstanceBootGroupReadinessRule.RuleType ruleType, Map<String, String> details) {
        if (ruleType == InstanceBootGroupReadinessRule.RuleType.MemberQuorum) {
            validateInstanceQuorumDetails(details);
        } else if (ruleType == InstanceBootGroupReadinessRule.RuleType.PortCheck) {
            validatePortCheckDetails(details);
        }
    }

    private void validateInstanceQuorumDetails(Map<String, String> details) {
        String thresholdType = details == null ? null : details.get(THRESHOLD_TYPE_KEY);
        String thresholdValue = details == null ? null : details.get(THRESHOLD_VALUE_KEY);
        if (StringUtils.isBlank(thresholdType) || StringUtils.isBlank(thresholdValue)) {
            throw new InvalidParameterValueException(String.format("%s rules require '%s' (COUNT or PERCENTAGE) and '%s' details", InstanceBootGroupReadinessRule.RuleType.MemberQuorum.name(), THRESHOLD_TYPE_KEY, THRESHOLD_VALUE_KEY));
        }
        if (!"COUNT".equalsIgnoreCase(thresholdType) && !"PERCENTAGE".equalsIgnoreCase(thresholdType)) {
            throw new InvalidParameterValueException(THRESHOLD_TYPE_KEY + " must be COUNT or PERCENTAGE");
        }
        try {
            if ("PERCENTAGE".equalsIgnoreCase(thresholdType)) {
                Double.parseDouble(thresholdValue);
            } else {
                Long.parseLong(thresholdValue);
            }
        } catch (NumberFormatException e) {
            throw new InvalidParameterValueException("Invalid " + THRESHOLD_VALUE_KEY + ": " + thresholdValue);
        }
    }

    private void validatePortCheckDetails(Map<String, String> details) {
        String protocol = details == null ? null : details.get(PROTOCOL_KEY);
        if (StringUtils.isNotBlank(protocol) && !"tcp".equalsIgnoreCase(protocol)) {
            throw new InvalidParameterValueException(InstanceBootGroupReadinessRule.RuleType.PortCheck.name() + " rules only support the tcp protocol currently");
        }
        String port = details == null ? null : details.get(PORT_KEY);
        if (StringUtils.isBlank(port)) {
            throw new InvalidParameterValueException(InstanceBootGroupReadinessRule.RuleType.PortCheck.name() + " rules require a '" + PORT_KEY + "' detail");
        }
        try {
            int portValue = Integer.parseInt(port);
            if (portValue < 1 || portValue > 65535) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            throw new InvalidParameterValueException("Invalid " + PORT_KEY + ": " + port);
        }
    }

    private void validateSingletonRuleType(long bootGroupId, InstanceBootGroupMember.MemberType itemType, long itemId, InstanceBootGroupReadinessRule.RuleType ruleType) {
        if (!SINGLETON_RULE_TYPES.contains(ruleType)) {
            return;
        }
        boolean alreadyExists = instanceBootGroupReadinessRuleDao.listByItem(bootGroupId, itemType, itemId).stream()
                .anyMatch(rule -> rule.getRuleType() == ruleType);
        if (alreadyExists) {
            throw new InvalidParameterValueException(String.format("A %s rule already exists for this %s", ruleType.name(), itemType.name()));
        }
    }

    /**
     * GuestAgentLiveness is KVM/libvirt-specific — reject it up front on other hypervisors. Only
     * checked for a direct VM-scoped rule; a group-scoped rule can't be validated once at creation
     * since membership is dynamic, so a non-KVM member just evaluates to Error per-member instead.
     */
    private void validateGuestAgentLivenessSupported(InstanceBootGroupMember.MemberType itemType, long vmId, InstanceBootGroupReadinessRule.RuleType ruleType) {
        if (ruleType != InstanceBootGroupReadinessRule.RuleType.GuestAgentLiveness || itemType != InstanceBootGroupMember.MemberType.VirtualMachine) {
            return;
        }
        UserVmVO vm = userVmDao.findById(vmId);
        if (vm == null) {
            throw new InvalidParameterValueException("Unable to find a VM with ID: " + vmId);
        }
        if (vm.getHypervisorType() != HypervisorType.KVM) {
            throw new InvalidParameterValueException(String.format(
                    "%s rules are only supported on KVM VMs; this VM's hypervisor is %s", ruleType.name(), vm.getHypervisorType()));
        }
    }

    private void validateRuleTypeForItemType(InstanceBootGroupMember.MemberType itemType, InstanceBootGroupReadinessRule.RuleType ruleType) {
        if (!VALID_RULE_TYPES_BY_ITEM_TYPE.getOrDefault(itemType, Collections.emptySet()).contains(ruleType)) {
            throw new InvalidParameterValueException(String.format("Rule type %s is not valid for item type %s", ruleType, itemType));
        }
    }

    private void validateItemBelongsToBootGroup(long bootGroupId, InstanceBootGroupMember.MemberType itemType, long itemId) {
        if (itemType == InstanceBootGroupMember.MemberType.InstanceGroup) {
            InstanceBootGroupMemberVO member = instanceBootGroupMemberDao.findByMember(InstanceBootGroupMember.MemberType.InstanceGroup, itemId);
            if (member == null || member.getBootGroupId() != bootGroupId) {
                throw new InvalidParameterValueException(String.format("Instance group %d is not a member of boot group %d", itemId, bootGroupId));
            }
            return;
        }

        InstanceBootGroupMemberVO directMember = instanceBootGroupMemberDao.findByMember(InstanceBootGroupMember.MemberType.VirtualMachine, itemId);
        if (directMember != null && directMember.getBootGroupId() == bootGroupId) {
            return;
        }

        List<InstanceGroupVMMapVO> mappings = instanceGroupVMMapDao.listByInstanceId(itemId);
        for (InstanceGroupVMMapVO mapping : mappings) {
            InstanceBootGroupMemberVO groupMember = instanceBootGroupMemberDao.findByMember(InstanceBootGroupMember.MemberType.InstanceGroup, mapping.getGroupId());
            if (groupMember != null && groupMember.getBootGroupId() == bootGroupId) {
                return;
            }
        }

        throw new InvalidParameterValueException(String.format(
                "VM %d is not part of boot group %d, neither directly nor via its instance group", itemId, bootGroupId));
    }
}

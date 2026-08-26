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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.managed.context.ManagedContextRunnable;
import org.apache.cloudstack.vm.bootgroup.readiness.InstanceBootGroupReadinessRule;
import org.apache.cloudstack.vm.bootgroup.readiness.InstanceBootGroupReadinessRuleService;
import org.springframework.stereotype.Component;

import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.InstanceGroup;
import com.cloud.vm.UserVmService;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.dao.InstanceBootGroupDetailsDao;
import com.cloud.vm.dao.InstanceBootGroupMemberDao;
import com.cloud.vm.dao.InstanceGroupDao;
import com.cloud.vm.dao.InstanceGroupVMMapDao;
import com.cloud.vm.dao.UserVmDao;

/**
 * Backend/orchestration half of the Instance Boot Group feature — tier concurrency, hypervisor
 * start/stop/reboot calls, and readiness-gated tier progression. API-cmd handling (ACL, validation,
 * response building, command registration) lives in {@code InstanceBootGroupApiServiceImpl}, which
 * delegates here with resolved domain objects.
 *
 * <p>Per-VM timeout/reboot-attempt bookkeeping during a start is kept purely in-memory, scoped to the
 * async job thread executing the start — it is not persisted. Surviving a management-server restart
 * mid-run is explicitly not a goal here; if the process restarts, the job (and this bookkeeping) is
 * simply lost, same as any other in-flight async job. Current readiness is queryable at any time via
 * {@code listInstanceBootGroupMembers?details=readiness}, not via a separate run-history API.</p>
 */
@Component
public class InstanceBootGroupManagerImpl extends ManagerBase implements InstanceBootGroupManager, Configurable {

    public static final ConfigKey<Long> ReadinessAttemptTimeoutSeconds = new ConfigKey<>("Advanced", Long.class,
            "instance.boot.group.readiness.timeout.seconds", "300",
            "How long to wait (in seconds) for an instance to become ready during boot group orchestration before starting a new readiness retry attempt. Overridable per boot group.", true);

    public static final ConfigKey<Long> ReadinessMaxRetryAttempts = new ConfigKey<>("Advanced", Long.class,
            "instance.boot.group.readiness.max.retry.attempts", "5",
            "Maximum number of readiness retry attempts for an instance that fails to become ready during boot group orchestration before the boot group start is halted. Overridable per boot group.", true);

    public static final ConfigKey<Long> ReadinessPollIntervalSeconds = new ConfigKey<>("Advanced", Long.class,
            "instance.boot.group.readiness.poll.interval.seconds", "10",
            "How often (in seconds) to re-check instance/instance-group readiness during boot group orchestration, including the minimum pause after a readiness retry attempt that did not reboot the instance before repeating the check that just failed. A very low value can cause rapid repeated (\"hammering\") readiness retries against an instance/VR/host. Global only, not overridable per boot group.", true);

    public static final ConfigKey<Long> ReadinessInitialDelaySeconds = new ConfigKey<>("Advanced", Long.class,
            "instance.boot.group.readiness.initial.delay.seconds", "30",
            "How long to wait (in seconds) after starting or rebooting an instance before its first readiness check of that attempt, giving the guest OS/agent/network time to come up. Overridable per boot group.", true);

    public static final ConfigKey<Boolean> ReadinessRebootOnRetry = new ConfigKey<>("Advanced", Boolean.class,
            "instance.boot.group.readiness.reboot.on.retry", "false",
            "Whether to reboot an instance between readiness retry attempts during boot group orchestration, instead of just waiting longer. Overridable per boot group.", true);

    public static final ConfigKey<Long> ReadinessCheckConcurrency = new ConfigKey<>("Advanced", Long.class,
            "instance.boot.group.readiness.check.concurrency", "10",
            "Maximum number of instances within a single boot-order tier whose readiness is checked concurrently during boot group orchestration, so one slow check cannot delay every other instance's check in the same poll. Global only, not overridable per boot group.", true);

    @Inject
    private InstanceBootGroupMemberDao instanceBootGroupMemberDao;

    @Inject
    private UserVmService userVmService;

    @Inject
    private UserVmDao userVmDao;

    @Inject
    private InstanceGroupDao instanceGroupDao;

    @Inject
    private InstanceGroupVMMapDao instanceGroupVMMapDao;

    @Inject
    private VirtualMachineManager virtualMachineManager;

    @Inject
    private InstanceBootGroupReadinessRuleService instanceBootGroupReadinessRuleService;

    @Inject
    private InstanceBootGroupDetailsDao instanceBootGroupDetailsDao;

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        return true;
    }

    @Override
    public String getConfigComponentName() {
        return InstanceBootGroupManagerImpl.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[]{ReadinessAttemptTimeoutSeconds, ReadinessMaxRetryAttempts, ReadinessPollIntervalSeconds, ReadinessInitialDelaySeconds, ReadinessRebootOnRetry, ReadinessCheckConcurrency};
    }

    /** In-memory-only per-VM progress for a single start attempt — never persisted. */
    private static final class VmProgress {
        private final long vmId;
        private final Long bootGroupMemberId;
        private boolean ready;
        /** Set when this VM has exhausted its retry attempts but belongs to an InstanceGroup
         *  member, so the group's own readiness rule (e.g. a quorum rule) gets the final say
         *  instead of this one VM halting the whole boot group. */
        private boolean gaveUp;
        private int retryAttempts;
        /** Anchor for the per-attempt timeout window; reset on every retry, rebooted or not. */
        private long enteredWaitAtMs;
        /** Anchor for the initial-delay grace period; only reset on the initial start and on an
         *  actual reboot — a no-op retry (reboot-on-retry disabled) leaves this alone, since there's
         *  no fresh boot to wait out. */
        private long lastBootedAtMs;

        private VmProgress(long vmId, Long bootGroupMemberId) {
            this.vmId = vmId;
            this.bootGroupMemberId = bootGroupMemberId;
        }

        private String getAttemptsLog(long maxAttempts) {
            return String.format("%d/%d", retryAttempts + 1, maxAttempts);
        }
    }

    @Override
    public void startInstanceBootGroup(InstanceBootGroupVO group) {
        List<InstanceBootGroupMemberVO> members = instanceBootGroupMemberDao.listByBootGroupId(group.getId());
        Map<Integer, List<InstanceBootGroupMemberVO>> tiers = groupByOrder(members);
        logger.info("Starting {}: {} tier(s), {} member(s) total", group, tiers.size(), members.size());
        long groupStartedAtMs = System.currentTimeMillis();

        for (Map.Entry<Integer, List<InstanceBootGroupMemberVO>> tierEntry : tiers.entrySet()) {
            int tierOrder = tierEntry.getKey();
            List<InstanceBootGroupMemberVO> tierMembers = tierEntry.getValue();

            Map<Long, VmProgress> progressByVmId = new LinkedHashMap<>();
            for (InstanceBootGroupMemberVO member : tierMembers) {
                for (Long vmId : resolveVmIds(List.of(member))) {
                    progressByVmId.put(vmId, new VmProgress(vmId, member.getId()));
                }
            }
            List<Long> tierVmIds = new ArrayList<>(progressByVmId.keySet());
            logger.info("Starting tier {} of {}: {} member(s), {} VM(s)", tierOrder, group, tierMembers.size(), tierVmIds.size());
            long tierStartedAtMs = System.currentTimeMillis();

            try {
                runTierConcurrently(tierVmIds, group, "start", vmId -> {
                    UserVmVO vm = userVmDao.findById(vmId);
                    boolean alreadyRunning = vm != null && VirtualMachine.State.Running.equals(vm.getState());
                    if (vm != null && !alreadyRunning) {
                        userVmService.startVirtualMachine(vm, null);
                    }
                    anchorInitialDelay(group, progressByVmId.get(vmId), vm, alreadyRunning);
                });
            } catch (CloudRuntimeException e) {
                halt(group, "Failed to start a VM in tier " + tierOrder + ": " + e.getMessage());
                throw e;
            }

            waitForTierReady(group, tierOrder, tierMembers, progressByVmId);
            logger.info("Tier {} of {} is ready ({}ms)", tierOrder, group, System.currentTimeMillis() - tierStartedAtMs);
        }

        logger.info("{} start completed ({}ms)", group, System.currentTimeMillis() - groupStartedAtMs);
    }

    /**
     * If the VM was already running, anchors the initial-delay grace period to when CloudStack last
     * confirmed its power state rather than to "now" — so it waits out only what's left of the
     * delay (or none) instead of a full fresh wait it doesn't need.
     */
    private void anchorInitialDelay(InstanceBootGroupVO group, VmProgress progress, UserVmVO vm, boolean alreadyRunning) {
        long now = System.currentTimeMillis();
        progress.enteredWaitAtMs = now;
        if (alreadyRunning && vm.getPowerStateUpdateTime() != null) {
            progress.lastBootedAtMs = vm.getPowerStateUpdateTime().getTime();
            logger.debug("{} was already running (power state last confirmed {}); readiness checks begin after any remaining portion of the {}s initial delay",
                    vm, vm.getPowerStateUpdateTime(), effectiveInitialDelaySeconds(group));
        } else {
            progress.lastBootedAtMs = now;
            logger.debug("{} start action completed; readiness checks begin after the {}s initial delay", vm, effectiveInitialDelaySeconds(group));
        }
    }

    /**
     * Polls every not-yet-settled VM in the tier concurrently (bounded by
     * {@code ReadinessCheckConcurrency}) until the whole tier — VMs and any InstanceGroup
     * members — reports ready, or a halt is triggered.
     */
    private void waitForTierReady(InstanceBootGroupVO group, int tierOrder, List<InstanceBootGroupMemberVO> tierMembers, Map<Long, VmProgress> progressByVmId) {
        Map<Long, InstanceBootGroupMemberVO> memberById = new HashMap<>();
        Map<Long, Boolean> membersReadyStatus = new ConcurrentHashMap<>();
        for (InstanceBootGroupMemberVO member : tierMembers) {
            memberById.put(member.getId(), member);
            membersReadyStatus.put(member.getId(), false);
        }
        final long effectiveMaxRetryAttempts = effectiveMaxRetryAttempts(group);
        final long effectiveTimeoutSeconds = effectiveTimeoutSeconds(group);
        final long effectivePollIntervalSeconds = effectivePollIntervalSeconds();
        final long pollIntervalMs = effectivePollIntervalSeconds * 1000L;
        final boolean effectiveRebootOnRetry = effectiveRebootOnRetry(group);
        int concurrency = (int) Math.max(1, Math.min(progressByVmId.size(), effectiveReadinessCheckConcurrency()));
        // Bound the polling loop: initial delay + (maxRetries + 1) full timeout windows + inter-poll sleeps.
        long maxWaitMs = (effectiveInitialDelaySeconds(group) + (effectiveMaxRetryAttempts + 1) * effectiveTimeoutSeconds
                + effectiveMaxRetryAttempts * effectivePollIntervalSeconds) * 1000L;
        long deadline = System.currentTimeMillis() + maxWaitMs;
        logger.debug("Waiting for tier {} of {} to become ready: {} VM(s) tracked, timeout={}s, pollInterval={}s, checkConcurrency={}, maxWait={}ms",
                tierOrder, group, progressByVmId.size(), effectiveTimeoutSeconds, effectivePollIntervalSeconds, concurrency, maxWaitMs);

        CallContext callerContext = CallContext.current();
        ExecutorService readinessExecutor = Executors.newFixedThreadPool(concurrency, new NamedThreadFactory("InstanceBootGroup-readiness-" + tierOrder));
        try {
            while (System.currentTimeMillis() < deadline) {
                List<Future<Void>> futures = new ArrayList<>();
                for (VmProgress progress : progressByVmId.values()) {
                    if (progress.ready || progress.gaveUp) {
                        continue;
                    }
                    futures.add(readinessExecutor.submit(() -> {
                        CallContext.register(callerContext, ApiCommandResourceType.VirtualMachine);
                        try {
                            checkVmReadiness(group, progress, memberById, membersReadyStatus,
                                    effectiveMaxRetryAttempts, effectiveTimeoutSeconds, effectiveRebootOnRetry);
                        } finally {
                            CallContext.unregister();
                        }
                        return null;
                    }));
                }
                for (Future<Void> future : futures) {
                    try {
                        future.get();
                    } catch (ExecutionException e) {
                        Throwable cause = e.getCause() != null ? e.getCause() : e;
                        if (cause instanceof CloudRuntimeException) {
                            throw (CloudRuntimeException) cause;
                        }
                        throw new CloudRuntimeException("Failed to evaluate readiness for a VM in tier " + tierOrder + " of " + group.getName() + ": " + cause.getMessage(), cause);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new CloudRuntimeException("Interrupted while evaluating readiness for tier " + tierOrder + " of " + group.getName(), e);
                    }
                }

                checkInstanceGroupMembersReady(group, tierMembers, progressByVmId, membersReadyStatus);

                if (membersReadyStatus.values().stream().allMatch(Boolean::booleanValue)) {
                    return;
                }

                sleep(pollIntervalMs);
            }
            String reason = String.format("Tier %d of boot group '%s' did not become ready within the maximum wait of %dms", tierOrder, group.getName(), maxWaitMs);
            logger.error(reason);
            halt(group, reason);
            throw new CloudRuntimeException(reason);
        } finally {
            readinessExecutor.shutdown();
        }
    }

    /**
     * Runs on one of {@code waitForTierReady}'s pooled threads for a single VM: gates on the
     * initial-delay window, dispatches this poll's check with the remaining time budget, and treats
     * Error the same as NotReady — both get a retry before anything halts.
     */
    private void checkVmReadiness(InstanceBootGroupVO group, VmProgress progress, Map<Long, InstanceBootGroupMemberVO> memberById,
            Map<Long, Boolean> membersReadyStatus, long effectiveMaxRetryAttempts, long effectiveTimeoutSeconds,
            boolean effectiveRebootOnRetry) {
        UserVmVO vm = userVmDao.findById(progress.vmId);
        long elapsedMs = System.currentTimeMillis() - progress.enteredWaitAtMs;
        long elapsedSinceBootMs = System.currentTimeMillis() - progress.lastBootedAtMs;
        long initialDelayMs = effectiveInitialDelaySeconds(group) * 1000L;
        if (elapsedSinceBootMs < initialDelayMs) {
            logger.debug("{} still within the initial delay window ({}ms elapsed of {}ms since last boot) — skipping readiness check this poll. Attempt: {}",
                    vm, elapsedSinceBootMs, initialDelayMs, progress.getAttemptsLog(effectiveMaxRetryAttempts));
            return;
        }

        long remainingMs = Math.max(0, effectiveTimeoutSeconds * 1000L - elapsedMs);
        String attemptLabel = progress.getAttemptsLog(effectiveMaxRetryAttempts);
        logger.debug("Evaluating readiness of {} for {} ({}ms since this attempt started, {}ms remaining budget). Attempt: {}",
                vm, group, elapsedMs, remainingMs, attemptLabel);
        InstanceBootGroupReadinessRule.Status readiness = instanceBootGroupReadinessRuleService.evaluateVmReadiness(group.getId(), progress.vmId, remainingMs, attemptLabel);
        if (readiness == InstanceBootGroupReadinessRule.Status.Ready) {
            progress.ready = true;
            logger.debug("{} is ready for {}", vm, group);
            InstanceBootGroupMemberVO member = memberById.get(progress.bootGroupMemberId);
            if (member != null && InstanceBootGroupMember.MemberType.VirtualMachine.equals(member.getMemberType())) {
                membersReadyStatus.put(progress.bootGroupMemberId, true);
            }
            return;
        }

        if (progress.retryAttempts < effectiveMaxRetryAttempts) {
            long now = System.currentTimeMillis();
            if (effectiveRebootOnRetry) {
                rebootVm(progress.vmId);
                progress.lastBootedAtMs = now;
            }
            logger.debug("{} readiness retry attempt {} of {} with a reboot={}",
                    vm, progress.getAttemptsLog(effectiveMaxRetryAttempts), group, effectiveRebootOnRetry);
            progress.retryAttempts++;
            progress.enteredWaitAtMs = now;
        } else {
            progress.gaveUp = true;
            InstanceBootGroupMemberVO member = memberById.get(progress.bootGroupMemberId);
            if (member != null && InstanceBootGroupMember.MemberType.InstanceGroup.equals(member.getMemberType())) {
                logger.warn("{} failed readiness after {} retry attempts; giving up on it and deferring to {}'s own readiness rule",
                        vm, progress.getAttemptsLog(effectiveMaxRetryAttempts), member);
            } else {
                String reason = String.format("Instance '%s' failed readiness after %s retry attempts",
                        vm.getName(), progress.getAttemptsLog(effectiveMaxRetryAttempts));
                logger.warn("{} failed readiness after {} retry attempts; halting {}",
                        vm, progress.getAttemptsLog(effectiveMaxRetryAttempts), group);
                halt(group, reason);
                throw new CloudRuntimeException(reason);
            }
        }
    }

    /**
     * Sequential pass over the tier's InstanceGroup members, run once all of this poll's per-VM
     * tasks finish. An empty member list is treated as settled — {@code allMatch()} on an empty
     * stream is vacuously true either way, so there's nothing left to wait for.
     */
    private void checkInstanceGroupMembersReady(InstanceBootGroupVO group, List<InstanceBootGroupMemberVO> tierMembers,
            Map<Long, VmProgress> progressByVmId, Map<Long, Boolean> membersReadyStatus) {
        for (InstanceBootGroupMemberVO member : tierMembers) {
            if (!InstanceBootGroupMember.MemberType.InstanceGroup.equals(member.getMemberType()) || membersReadyStatus.getOrDefault(member.getId(), false)) {
                continue;
            }
            InstanceGroup instanceGroup = instanceGroupDao.findById(member.getMemberId());
            Collection<VmProgress> memberProgresses = progressByVmId.values().stream()
                    .filter(p -> member.getId() == (p.bootGroupMemberId == null ? -1 : p.bootGroupMemberId))
                    .collect(Collectors.toList());
            if (!memberProgresses.isEmpty() && memberProgresses.stream().allMatch(p -> !p.ready && !p.gaveUp)) {
                logger.debug("{} part of {} has no VMs that are ready or have exhausted their retries yet", instanceGroup, group);
                continue;
            }

            Set<Long> permanentlyFailedVmIds = memberProgresses.stream()
                    .filter(p -> p.gaveUp)
                    .map(p -> p.vmId)
                    .collect(Collectors.toSet());
            InstanceBootGroupReadinessRule.Status groupStatus =
                    instanceBootGroupReadinessRuleService.evaluateInstanceGroupReadiness(group.getId(), member.getMemberId(), permanentlyFailedVmIds);
            if (groupStatus == InstanceBootGroupReadinessRule.Status.Ready) {
                membersReadyStatus.put(member.getId(), true);
                logger.info("{} part of {} reached readiness state Ready", instanceGroup, group);
                continue;
            }
            if (InstanceBootGroupReadinessRule.Status.Error.equals(groupStatus) ||
                    (InstanceBootGroupReadinessRule.Status.NotReady.equals(groupStatus) &&
                            memberProgresses.stream().allMatch(p -> p.ready || p.gaveUp))) {
                String reason = String.format("Instance group '%s' failed its own readiness rules", instanceGroup.getName());
                logger.error("{} failed its own readiness rules; halting {}", instanceGroup, group);
                halt(group, reason);
                throw new CloudRuntimeException(reason);
            }
        }
    }

    /**
     * Only stops the orchestration loop — never stops a VM, since every VM touched by this point may
     * already be running and tearing it down would be destructive, not recoverable.
     */
    private void halt(InstanceBootGroupVO group, String reason) {
        logger.warn("Halting {} start: {}", group, reason);
    }

    private void rebootVm(long vmId) {
        UserVmVO vm = userVmDao.findById(vmId);
        if (vm == null) {
            logger.warn("Cannot reboot instance id {} for a boot group readiness retry: VM not found", vmId);
            return;
        }
        logger.debug("Rebooting {} for a boot group readiness retry attempt", vm);
        try {
            virtualMachineManager.reboot(vm.getUuid(), null);
        } catch (Exception e) {
            throw new CloudRuntimeException("Failed to reboot VM " + vm + " during boot group readiness retry: " + e.getMessage(), e);
        }
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CloudRuntimeException("Interrupted while waiting for boot group tier readiness", e);
        }
    }

    private long effectiveTimeoutSeconds(InstanceBootGroupVO group) {
        String override = instanceBootGroupDetailsDao.getDetail(group.getId(), ReadinessAttemptTimeoutSeconds.key());
        return override != null ? Long.parseLong(override) : ReadinessAttemptTimeoutSeconds.value();
    }

    private long effectiveMaxRetryAttempts(InstanceBootGroupVO group) {
        String override = instanceBootGroupDetailsDao.getDetail(group.getId(), ReadinessMaxRetryAttempts.key());
        return override != null ? Long.parseLong(override) : ReadinessMaxRetryAttempts.value();
    }

    private long effectivePollIntervalSeconds() {
        return ReadinessPollIntervalSeconds.value();
    }

    private long effectiveReadinessCheckConcurrency() {
        return ReadinessCheckConcurrency.value();
    }

    private long effectiveInitialDelaySeconds(InstanceBootGroupVO group) {
        String override = instanceBootGroupDetailsDao.getDetail(group.getId(), ReadinessInitialDelaySeconds.key());
        return override != null ? Long.parseLong(override) : ReadinessInitialDelaySeconds.value();
    }

    private boolean effectiveRebootOnRetry(InstanceBootGroupVO group) {
        String override = instanceBootGroupDetailsDao.getDetail(group.getId(), ReadinessRebootOnRetry.key());
        return override != null ? Boolean.parseBoolean(override) : ReadinessRebootOnRetry.value();
    }

    @Override
    public void stopInstanceBootGroup(InstanceBootGroupVO group) {
        List<InstanceBootGroupMemberVO> members = instanceBootGroupMemberDao.listByBootGroupId(group.getId());
        Map<Integer, List<InstanceBootGroupMemberVO>> tiers = groupByOrderDescending(members);
        logger.info("Stopping {}: {} tier(s), {} member(s) total", group, tiers.size(), members.size());
        long groupStoppedAtMs = System.currentTimeMillis();

        for (Map.Entry<Integer, List<InstanceBootGroupMemberVO>> tier : tiers.entrySet()) {
            List<Long> vmIds = resolveVmIds(tier.getValue());
            runTierConcurrently(vmIds, group, "stop", vmId -> {
                UserVmVO vm = userVmDao.findById(vmId);
                if (vm != null && vm.getState() != com.cloud.vm.VirtualMachine.State.Stopped) {
                    userVmService.stopVirtualMachine(vmId, false);
                }
            });
        }

        logger.info("{} stop completed ({}ms)", group, System.currentTimeMillis() - groupStoppedAtMs);
    }

    @Override
    public void rebootInstanceBootGroup(InstanceBootGroupVO group) {
        logger.info("Rebooting {}: stopping, then starting", group);
        stopInstanceBootGroup(group);
        startInstanceBootGroup(group);
    }

    /**
     * Runs {@code action} for every VM in a tier concurrently and aborts on the first failure. Each
     * thread gets a copied {@link CallContext} — without one, a VM lifecycle action routed through
     * the job-queue path fails to submit its sub-job ("no lock found").
     */
    private void runTierConcurrently(List<Long> vmIds, InstanceBootGroupVO group,
                                     String actionName, VmAction action) {
        if (vmIds.isEmpty()) {
            return;
        }

        logger.debug("Running '{}' action for a tier of {}: {} VM id(s) {}",
                actionName, group, vmIds.size(), vmIds);
        long actionStartedAtMs = System.currentTimeMillis();
        CallContext callerContext = CallContext.current();
        int threadCount = Math.min(vmIds.size(), ReadinessCheckConcurrency.value().intValue());
        ExecutorService executor = Executors.newFixedThreadPool(
                threadCount, new NamedThreadFactory("InstanceBootGroup-" + actionName));

        try {
            List<Future<?>> futures = new ArrayList<>(vmIds.size());

            for (Long vmId : vmIds) {
                futures.add(executor.submit(new ManagedContextRunnable() {
                    @Override
                    protected void runInContext() {
                        CallContext.register(callerContext, ApiCommandResourceType.VirtualMachine);
                        CallContext.current().setEventResourceId(vmId);
                        try {
                            action.run(vmId);
                        } catch (Exception e) {
                            throw new CloudRuntimeException(String.format("Failed to %s VM %d", actionName, vmId), e);
                        } finally {
                            CallContext.unregister();
                        }
                    }
                }));
            }

            for (Future<?> future : futures) {
                try {
                    future.get();
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause() != null ? e.getCause() : e;
                    throw new CloudRuntimeException(
                            String.format("Failed to %s a VM in boot group %s: %s",
                                    actionName, group.getName(), cause.getMessage()),
                            cause);

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new CloudRuntimeException(
                            String.format("Interrupted while waiting to %s VMs in boot group %s",
                                    actionName, group.getName()),
                            e);
                }
            }
        } finally {
            executor.shutdown();
        }

        logger.debug(
                "'{}' action for a tier of {} completed for {} VM(s) in {}ms",
                actionName, group, vmIds.size(), System.currentTimeMillis() - actionStartedAtMs);
    }

    private Map<Integer, List<InstanceBootGroupMemberVO>> groupByOrder(List<InstanceBootGroupMemberVO> members) {
        Map<Integer, List<InstanceBootGroupMemberVO>> tiers = new TreeMap<>();
        for (InstanceBootGroupMemberVO m : members) {
            tiers.computeIfAbsent(m.getOrder(), k -> new ArrayList<>()).add(m);
        }
        return tiers;
    }

    private Map<Integer, List<InstanceBootGroupMemberVO>> groupByOrderDescending(List<InstanceBootGroupMemberVO> members) {
        Map<Integer, List<InstanceBootGroupMemberVO>> tiers = new TreeMap<>(Comparator.reverseOrder());
        for (InstanceBootGroupMemberVO m : members) {
            tiers.computeIfAbsent(m.getOrder(), k -> new ArrayList<>()).add(m);
        }
        return tiers;
    }

    private List<Long> resolveVmIds(List<InstanceBootGroupMemberVO> tierMembers) {
        List<Long> vmIds = new ArrayList<>();
        for (InstanceBootGroupMemberVO member : tierMembers) {
            if (member.getMemberType() == InstanceBootGroupMember.MemberType.VirtualMachine) {
                vmIds.add(member.getMemberId());
            } else {
                instanceGroupVMMapDao.listByGroupId(member.getMemberId())
                        .forEach(map -> vmIds.add(map.getInstanceId()));
            }
        }
        return vmIds;
    }

    @FunctionalInterface
    private interface VmAction {
        void run(long vmId) throws Exception;
    }
}

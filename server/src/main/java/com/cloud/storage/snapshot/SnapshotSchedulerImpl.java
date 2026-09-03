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
package com.cloud.storage.snapshot;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.command.user.snapshot.CreateSnapshotCmd;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.framework.jobs.AsyncJobDispatcher;
import org.apache.cloudstack.framework.jobs.AsyncJobManager;
import org.apache.cloudstack.framework.jobs.dao.AsyncJobDao;
import org.apache.cloudstack.framework.jobs.impl.AsyncJobVO;
import org.apache.cloudstack.jobs.JobInfo;
import org.apache.cloudstack.managed.context.ManagedContextTimerTask;
import org.springframework.stereotype.Component;

import com.cloud.api.ApiDispatcher;
import com.cloud.api.ApiGsonHelper;
import com.cloud.event.ActionEventUtils;
import com.cloud.event.EventTypes;
import com.cloud.event.EventVO;
import com.cloud.event.dao.EventDao;
import com.cloud.server.ResourceTag;
import com.cloud.server.TaggedResourceService;
import com.cloud.storage.Snapshot;
import com.cloud.storage.SnapshotPolicyVO;
import com.cloud.storage.SnapshotScheduleVO;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.storage.dao.SnapshotPolicyDao;
import com.cloud.storage.dao.SnapshotScheduleDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.user.Account;
import com.cloud.user.User;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.DateUtil;
import com.cloud.utils.DateUtil.IntervalType;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.concurrency.TestClock;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.db.TransactionLegacy;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.dao.VMInstanceDao;
import com.cloud.vm.snapshot.VMSnapshotManager;
import com.cloud.vm.snapshot.VMSnapshotVO;
import com.cloud.vm.snapshot.dao.VMSnapshotDao;

@Component
public class SnapshotSchedulerImpl extends ManagerBase implements SnapshotScheduler {

    @Inject
    protected AsyncJobDao _asyncJobDao;
    @Inject
    protected SnapshotDao _snapshotDao;
    @Inject
    protected SnapshotScheduleDao _snapshotScheduleDao;
    @Inject
    protected SnapshotPolicyDao _snapshotPolicyDao;
    @Inject
    protected AsyncJobManager _asyncMgr;
    @Inject
    protected VolumeDao _volsDao;
    @Inject
    protected ConfigurationDao _configDao;
    @Inject
    protected ApiDispatcher _dispatcher;
    @Inject
    protected AccountDao _acctDao;
    @Inject
    protected SnapshotApiService _snapshotService;
    @Inject
    protected VMSnapshotDao _vmSnapshotDao;
    @Inject
    protected VMSnapshotManager _vmSnaphostManager;
    @Inject
    public TaggedResourceService taggedResourceService;
    @Inject
    protected EventDao eventDao;
    @Inject
    protected VMInstanceDao vmInstanceDao;

    protected AsyncJobDispatcher _asyncDispatcher;

    private static final int ACQUIRE_GLOBAL_LOCK_TIMEOUT_FOR_COOPERATION = 5;    // 5 seconds
    private int _snapshotPollInterval;
    private Timer _testClockTimer;
    private Date _currentTimestamp;
    private TestClock _testTimerTask;

    public AsyncJobDispatcher getAsyncJobDispatcher() {
        return _asyncDispatcher;
    }

    public void setAsyncJobDispatcher(final AsyncJobDispatcher dispatcher) {
        _asyncDispatcher = dispatcher;
    }

    private Date getNextScheduledTime(final long policyId, final Date currentTimestamp) {
        final SnapshotPolicyVO policy = _snapshotPolicyDao.findById(policyId);
        Date nextTimestamp = null;
        if (policy != null) {
            final short intervalType = policy.getInterval();
            final IntervalType type = DateUtil.getIntervalType(intervalType);
            final String schedule = policy.getSchedule();
            final String timezone = policy.getTimezone();
            nextTimestamp = DateUtil.getNextRunTime(type, schedule, timezone, currentTimestamp);
            final String currentTime = DateUtil.displayDateInTimezone(DateUtil.GMT_TIMEZONE, currentTimestamp);
            final String nextScheduledTime = DateUtil.displayDateInTimezone(DateUtil.GMT_TIMEZONE, nextTimestamp);
            logger.debug("Current time is {}. NextScheduledTime of policy {} is {}", currentTime, policy, nextScheduledTime);
        }
        return nextTimestamp;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void poll(final Date currentTimestamp) {
        // We don't maintain the time. The timer task does.
        _currentTimestamp = currentTimestamp;

        GlobalLock scanLock = GlobalLock.getInternLock("snapshot.poll");
        try {
            if (scanLock.lock(ACQUIRE_GLOBAL_LOCK_TIMEOUT_FOR_COOPERATION)) {
                try {
                    scheduleNextSnapshotJobsIfNecessary();
                } finally {
                    scanLock.unlock();
                }
            }
        } finally {
            scanLock.releaseRef();
        }

        scanLock = GlobalLock.getInternLock("snapshot.poll");
        try {
            if (scanLock.lock(ACQUIRE_GLOBAL_LOCK_TIMEOUT_FOR_COOPERATION)) {
                try {
                    scheduleSnapshots();
                } finally {
                    scanLock.unlock();
                }
            }
        } finally {
            scanLock.releaseRef();
        }

        try {
            deleteExpiredVMSnapshots();
        }
        catch (Exception e) {
            logger.warn("Error in expiring Instance Snapshots", e);
        }
    }

    private void scheduleNextSnapshotJobsIfNecessary() {
        List<SnapshotScheduleVO> snapshotSchedules = _snapshotScheduleDao.getSchedulesAssignedWithAsyncJob();
        logger.info("Verifying the current state of [{}] Snapshot schedules and scheduling next jobs, if necessary.", snapshotSchedules.size());
        for (SnapshotScheduleVO snapshotSchedule : snapshotSchedules) {
            scheduleNextSnapshotJobIfNecessary(snapshotSchedule);
        }
    }

    protected void scheduleNextSnapshotJobIfNecessary(SnapshotScheduleVO snapshotSchedule) {
        Long asyncJobId = snapshotSchedule.getAsyncJobId();
        AsyncJobVO asyncJob = _asyncJobDao.findByIdIncludingRemoved(asyncJobId);

        if (asyncJob == null) {
            logger.debug("The async job [{}] of snapshot schedule [{}] does not exist anymore. Considering it as finished and scheduling the next snapshot job.",
                    asyncJobId, snapshotSchedule);
            scheduleNextSnapshotJob(snapshotSchedule);
            return;
        }

        JobInfo.Status status = asyncJob.getStatus();

        if (JobInfo.Status.SUCCEEDED.equals(status)) {
            logger.debug("Last job of schedule [{}] succeeded; scheduling the next snapshot job.", snapshotSchedule);
            recordSnapshotAttemptOutcome(snapshotSchedule, true, null);
        } else if (JobInfo.Status.FAILED.equals(status)) {
            logger.debug("Last job of schedule [{}] failed with [{}]; scheduling a new snapshot job.", snapshotSchedule, asyncJob.getResult());
            recordSnapshotAttemptOutcome(snapshotSchedule, false, asyncJob.getResult());
        } else {
            logger.debug("Schedule [{}] is still in progress, skipping next job scheduling.", snapshotSchedule);
            return;
        }

        scheduleNextSnapshotJob(snapshotSchedule);
    }

    /**
     * Logs an event for the outcome of a recurring snapshot job (keyed by the volume, since a fresh snapshot entity
     * ID is minted on every attempt) so that consecutive failures can be counted from event history, and raises a
     * WARN notification once {@link SnapshotManager#SnapshotRecurringMaxFailures} consecutive failures are reached.
     */
    protected void recordSnapshotAttemptOutcome(final SnapshotScheduleVO snapshotSchedule, final boolean succeeded, final String failureResult) {
        final VolumeVO volume = _volsDao.findByIdIncludingRemoved(snapshotSchedule.getVolumeId());
        if (volume == null) {
            return;
        }

        if (succeeded) {
            ActionEventUtils.onCreatedActionEvent(User.UID_SYSTEM, volume.getAccountId(), EventVO.LEVEL_INFO, EventTypes.EVENT_SNAPSHOT_CREATE, true,
                    String.format("Scheduled snapshot creation job for volume [%s] succeeded.", volume),
                    volume.getId(), ApiCommandResourceType.Volume.toString());
            return;
        }

        final Account account = _acctDao.findById(volume.getAccountId());
        final int maxFailures = getScopedConfigValue(SnapshotManager.SnapshotRecurringMaxFailures, volume, account);
        final int totalFailures = countConsecutiveFailedAttempts(volume.getId(), maxFailures) + 1;

        ActionEventUtils.onCreatedActionEvent(User.UID_SYSTEM, volume.getAccountId(), EventVO.LEVEL_ERROR, EventTypes.EVENT_SNAPSHOT_CREATE, true,
                String.format("Scheduled snapshot creation job for volume [%s] failed: %s", volume, failureResult),
                volume.getId(), ApiCommandResourceType.Volume.toString());

        if (maxFailures > 0 && totalFailures >= maxFailures) {
            logger.warn("Snapshot schedule [{}] for volume [{}] has failed [{}] consecutive times.", snapshotSchedule, volume, totalFailures);
            raiseFailureLimitReachedEvent(volume, String.format("Recurring snapshot for volume [%s] has failed %d consecutive times.", volume, totalFailures));
        }
    }

    /**
     * Raises the WARN notification event for a volume's recurring snapshot having reached
     * {@link SnapshotManager#SnapshotRecurringMaxFailures} consecutive failures. Kept as a single call site (used by
     * both {@link #recordSnapshotAttemptOutcome} and {@link #handleFailedSnapshotDispatch}) so it can't drift onto
     * {@link EventTypes#EVENT_SNAPSHOT_CREATE} again, which would make it part of the event stream that
     * {@link #countConsecutiveFailedAttempts} scans and silently reset the count.
     */
    private void raiseFailureLimitReachedEvent(final VolumeVO volume, final String message) {
        ActionEventUtils.onCreatedActionEvent(User.UID_SYSTEM, volume.getAccountId(), EventVO.LEVEL_WARN, EventTypes.EVENT_SNAPSHOT_RECURRING_FAILURE_LIMIT_REACHED, true,
                message, volume.getId(), ApiCommandResourceType.Volume.toString());
    }

    @DB
    protected void deleteExpiredVMSnapshots() {
        Date now = new Date();
        List<VMSnapshotVO> vmSnapshots = _vmSnapshotDao.listAll();
        for (VMSnapshotVO vmSnapshot : vmSnapshots) {
            long accountId = vmSnapshot.getAccountId();
            int expiration_interval_hours = VMSnapshotManager.VMSnapshotExpireInterval.valueIn(accountId);
            if (expiration_interval_hours < 0 ) {
                continue;
            }
            Date creationTime = vmSnapshot.getCreated();
            long diffInHours = TimeUnit.MILLISECONDS.toHours(now.getTime() - creationTime.getTime());
            if (diffInHours >= expiration_interval_hours) {
                if (logger.isDebugEnabled()){
                    logger.debug("Deleting expired Instance Snapshot: {}", vmSnapshot);
                }
                _vmSnaphostManager.deleteVMSnapshot(vmSnapshot.getId());
            }
        }
    }

    @DB
    protected void scheduleSnapshots() {
        String displayTime = DateUtil.displayDateInTimezone(DateUtil.GMT_TIMEZONE, _currentTimestamp);
        logger.debug(String.format("Snapshot scheduler is being called at [%s].", displayTime));

        final List<SnapshotScheduleVO> snapshotsToBeExecuted = _snapshotScheduleDao.getSchedulesToExecute(_currentTimestamp);
        logger.debug(String.format("There are [%s] scheduled snapshots to be executed at [%s].", snapshotsToBeExecuted.size(), displayTime));

        for (final SnapshotScheduleVO snapshotToBeExecuted : snapshotsToBeExecuted) {
            SnapshotScheduleVO tmpSnapshotScheduleVO = null;
            Long eventId = null;
            final long snapshotScheId = snapshotToBeExecuted.getId();
            final VolumeVO volume = _volsDao.findByIdIncludingRemoved(snapshotToBeExecuted.getVolumeId());
            try {
                if (shouldSkipSchedule(snapshotToBeExecuted, volume)) {
                    continue;
                }

                tmpSnapshotScheduleVO = _snapshotScheduleDao.acquireInLockTable(snapshotScheId);
                eventId = dispatchSnapshotCreateJob(snapshotToBeExecuted, volume, tmpSnapshotScheduleVO);
            } catch (final Exception e) {
                logger.error("The scheduling of snapshot [{}] for volume [{}] failed due to [{}].", snapshotToBeExecuted, volume, e.toString(), e);
                if (tmpSnapshotScheduleVO != null) {
                    handleFailedSnapshotDispatch(snapshotToBeExecuted, volume, tmpSnapshotScheduleVO, eventId, e);
                }
            } finally {
                if (tmpSnapshotScheduleVO != null) {
                    _snapshotScheduleDao.releaseFromLockTable(snapshotScheId);
                }
            }
        }
    }

    /**
     * Returns true (after rescheduling as needed) when this iteration's snapshot should not be dispatched: either
     * because it can't be scheduled at all, or because it's a redundant snapshot of an unchanged volume that gets
     * skipped and rescheduled to its next regular run instead. Kept as a single decision point so the caller only
     * needs one {@code continue}.
     */
    private boolean shouldSkipSchedule(final SnapshotScheduleVO snapshotToBeExecuted, final VolumeVO volume) {
        if (!canSnapshotBeScheduled(snapshotToBeExecuted, volume)) {
            return true;
        }
        if (shouldSkipUnchangedVolumeSnapshot(volume)) {
            skipAndRescheduleSnapshot(snapshotToBeExecuted, volume);
            return true;
        }
        return false;
    }

    /**
     * Builds and dispatches the CreateSnapshotCmd async job for a scheduled snapshot, returning the "scheduled"
     * action event id so the caller can complete it if dispatch subsequently fails.
     */
    private Long dispatchSnapshotCreateJob(final SnapshotScheduleVO snapshotToBeExecuted, final VolumeVO volume, final SnapshotScheduleVO tmpSnapshotScheduleVO) throws Exception {
        final long snapshotScheId = snapshotToBeExecuted.getId();
        final long policyId = snapshotToBeExecuted.getPolicyId();
        final long volumeId = snapshotToBeExecuted.getVolumeId();

        final Long eventId =
            ActionEventUtils.onScheduledActionEvent(User.UID_SYSTEM, volume.getAccountId(), EventTypes.EVENT_SNAPSHOT_CREATE, "creating snapshot for volume Id:" +
                volume.getUuid(), volumeId, ApiCommandResourceType.Volume.toString(), true, 0);

        logger.trace("Mapping parameters required to generate a CreateSnapshotCmd for snapshot [{}].", snapshotToBeExecuted);
        final Map<String, String> params = new HashMap<String, String>();
        params.put(ApiConstants.VOLUME_ID, "" + volumeId);
        params.put(ApiConstants.POLICY_ID, "" + policyId);
        params.put("ctxUserId", "1");
        params.put("ctxAccountId", "" + volume.getAccountId());
        params.put("ctxStartEventId", String.valueOf(eventId));
        List<? extends ResourceTag> resourceTags = taggedResourceService.listByResourceTypeAndId(ResourceTag.ResourceObjectType.SnapshotPolicy, policyId);
        if (resourceTags != null && !resourceTags.isEmpty()) {
            int tagNumber = 0;
            for (ResourceTag resourceTag : resourceTags) {
                params.put("tags[" + tagNumber + "].key", resourceTag.getKey());
                params.put("tags[" + tagNumber + "].value", resourceTag.getValue());
                tagNumber++;
            }
        }

        logger.trace("Generating a CreateSnapshotCmd for snapshot [{}] with parameters: [{}].", snapshotToBeExecuted, params.toString());
        final CreateSnapshotCmd cmd = new CreateSnapshotCmd();
        ComponentContext.inject(cmd);
        _dispatcher.dispatchCreateCmd(cmd, params);
        params.put("id", "" + cmd.getEntityId());
        params.put("ctxStartEventId", "1");

        final Date scheduledTimestamp = snapshotToBeExecuted.getScheduledTimestamp();
        final String displayTime = DateUtil.displayDateInTimezone(DateUtil.GMT_TIMEZONE, scheduledTimestamp);
        logger.debug("Scheduling snapshot [{}] for volume [{}] at [{}].", snapshotToBeExecuted, volume, displayTime);
        AsyncJobVO job = new AsyncJobVO("", User.UID_SYSTEM, volume.getAccountId(), CreateSnapshotCmd.class.getName(),
                ApiGsonHelper.getBuilder().create().toJson(params), cmd.getEntityId(),
                cmd.getApiResourceType() != null ? cmd.getApiResourceType().toString() : null, null);
        job.setDispatcher(_asyncDispatcher.getName());
        final long jobId = _asyncMgr.submitAsyncJob(job);
        logger.debug("Scheduled snapshot [{}] for volume [{}] as job [{}].", snapshotToBeExecuted, volume, job);

        tmpSnapshotScheduleVO.setAsyncJobId(jobId);
        _snapshotScheduleDao.update(snapshotScheId, tmpSnapshotScheduleVO);

        return eventId;
    }

    /**
     * Handles a synchronous failure to dispatch the CreateSnapshotCmd (e.g. an allocation error) for a recurring
     * snapshot. Without this, the schedule's {@code scheduledTimestamp} is never advanced, so it gets retried on
     * every poll (every {@code snapshot.poll.interval} seconds) forever. Instead: log a failure event keyed by the
     * volume (so consecutive failures can be counted from event history), and either back off by the configured
     * retry interval, or - once the configured maximum consecutive failures is reached - give up until the next
     * regularly scheduled run and raise a WARN notification event.
     */
    protected void handleFailedSnapshotDispatch(final SnapshotScheduleVO snapshotToBeExecuted, final VolumeVO volume,
            final SnapshotScheduleVO lockedSchedule, final Long eventId, final Exception cause) {
        final long volumeId = volume.getId();
        final Account account = _acctDao.findById(volume.getAccountId());
        final int maxFailures = getScopedConfigValue(SnapshotManager.SnapshotRecurringMaxFailures, volume, account);
        final int retryInterval = getScopedConfigValue(SnapshotManager.SnapshotRecurringRetryInterval, volume, account);
        final int totalFailures = countConsecutiveFailedAttempts(volumeId, maxFailures) + 1;

        final String failureMessage = String.format("Failed to create scheduled snapshot for volume [%s]: %s", volume, cause.getMessage());
        if (eventId != null) {
            ActionEventUtils.onCompletedActionEvent(User.UID_SYSTEM, volume.getAccountId(), EventVO.LEVEL_ERROR,
                    EventTypes.EVENT_SNAPSHOT_CREATE, failureMessage, volumeId, ApiCommandResourceType.Volume.toString(), eventId);
        } else {
            ActionEventUtils.onCreatedActionEvent(User.UID_SYSTEM, volume.getAccountId(), EventVO.LEVEL_ERROR,
                    EventTypes.EVENT_SNAPSHOT_CREATE, true, failureMessage, volumeId, ApiCommandResourceType.Volume.toString());
        }

        if (maxFailures > 0 && totalFailures >= maxFailures) {
            final Date nextRegularRun = getNextScheduledTime(snapshotToBeExecuted.getPolicyId(), _currentTimestamp);
            lockedSchedule.setScheduledTimestamp(nextRegularRun);
            logger.warn("Snapshot schedule [{}] for volume [{}] has failed [{}] consecutive times; it will not be retried until its next regularly scheduled run at [{}].",
                    snapshotToBeExecuted, volume, totalFailures, nextRegularRun);
            raiseFailureLimitReachedEvent(volume, String.format(
                    "Recurring snapshot for volume [%s] has failed %d consecutive times and will not be retried until its next regularly scheduled run.", volume, totalFailures));
        } else {
            final Date nextRetry = new Date(_currentTimestamp.getTime() + retryInterval * 1000L);
            lockedSchedule.setScheduledTimestamp(nextRetry);
            logger.debug("Snapshot schedule [{}] for volume [{}] failed [{}] time(s); retrying at [{}].",
                    snapshotToBeExecuted, volume, totalFailures, nextRetry);
        }
        _snapshotScheduleDao.update(lockedSchedule.getId(), lockedSchedule);
    }

    /**
     * Counts how many of the most recent {@code EVENT_SNAPSHOT_CREATE} events logged for this volume are
     * consecutive failures (level ERROR), starting from the most recent event and stopping at the first
     * non-failure (or absent) event. This derives the "number of failed attempts" from event history instead of a
     * dedicated counter column.
     */
    protected int countConsecutiveFailedAttempts(final long volumeId, final int limit) {
        if (limit <= 0) {
            return 0;
        }
        final List<EventVO> recentEvents = eventDao.listLatestEventsByResource(volumeId, ApiCommandResourceType.Volume.toString(),
                EventTypes.EVENT_SNAPSHOT_CREATE, limit);
        int count = 0;
        for (final EventVO event : recentEvents) {
            if (!EventVO.LEVEL_ERROR.equals(event.getLevel())) {
                break;
            }
            count++;
        }
        return count;
    }

    /**
     * Resolves a config value in order of most to least specific scope: account, domain, zone, then global. A
     * {@link ConfigKey} can only walk a single scope-parent chain automatically (Account-&gt;Domain-&gt;Global, or
     * Zone-&gt;Global), so the four scopes are resolved manually here.
     */
    protected <T> T getScopedConfigValue(final ConfigKey<T> key, final VolumeVO volume, final Account account) {
        T value = key.valueInScope(ConfigKey.Scope.Account, volume.getAccountId(), true);
        if (value == null && account != null) {
            value = key.valueInScope(ConfigKey.Scope.Domain, account.getDomainId(), true);
        }
        if (value == null) {
            value = key.valueInScope(ConfigKey.Scope.Zone, volume.getDataCenterId(), true);
        }
        if (value == null) {
            value = key.value();
        }
        return value;
    }

    /**
     * Implements https://github.com/apache/cloudstack/issues/6827: a recurring snapshot is redundant when nothing
     * could have changed on the volume since the last one was taken, i.e. when the attached VM has not been running
     * at any point since then. Volumes with no snapshot yet, or that are not attached to a VM, are never skipped.
     */
    protected boolean shouldSkipUnchangedVolumeSnapshot(final VolumeVO volume) {
        final Account account = _acctDao.findById(volume.getAccountId());
        if (!Boolean.TRUE.equals(getScopedConfigValue(SnapshotManager.SnapshotSkipIfVmNotRunning, volume, account))) {
            return false;
        }

        final Long instanceId = volume.getInstanceId();
        if (instanceId == null) {
            return false;
        }

        final SnapshotVO lastSnapshot = findLastSnapshot(volume.getId());
        if (lastSnapshot == null) {
            return false;
        }

        final VMInstanceVO vm = vmInstanceDao.findById(instanceId);
        if (vm == null || vm.getPowerState() == VirtualMachine.PowerState.PowerOn) {
            return false;
        }

        final Date poweredOffSince = vm.getPowerStateUpdateTime();
        // If the VM's power state changed at or after the last snapshot, it may have been running (and the volume
        // may have changed) at some point since; only skip when it has been off since strictly before that snapshot.
        return poweredOffSince != null && poweredOffSince.before(lastSnapshot.getCreated());
    }

    protected SnapshotVO findLastSnapshot(final long volumeId) {
        final Filter filter = new Filter(SnapshotVO.class, "created", false, 0L, 1L);
        final List<SnapshotVO> snapshots = _snapshotDao.listByVolumeId(filter, volumeId);
        return (snapshots == null || snapshots.isEmpty()) ? null : snapshots.get(0);
    }

    /**
     * Advances a schedule that was skipped (see {@link #shouldSkipUnchangedVolumeSnapshot}) to its next regularly
     * scheduled run, and logs an informational event so the skip is visible and not mistaken for a missed snapshot.
     */
    @DB
    protected void skipAndRescheduleSnapshot(final SnapshotScheduleVO snapshotToBeExecuted, final VolumeVO volume) {
        SnapshotScheduleVO lockedSchedule = null;
        final long snapshotScheId = snapshotToBeExecuted.getId();
        try {
            lockedSchedule = _snapshotScheduleDao.acquireInLockTable(snapshotScheId);
            if (lockedSchedule == null) {
                return;
            }
            final Date nextRegularRun = getNextScheduledTime(snapshotToBeExecuted.getPolicyId(), _currentTimestamp);
            lockedSchedule.setScheduledTimestamp(nextRegularRun);
            _snapshotScheduleDao.update(snapshotScheId, lockedSchedule);
            logger.info("Skipped scheduled snapshot [{}] for volume [{}] because its instance has not been running since the last snapshot; next run at [{}].",
                    snapshotToBeExecuted, volume, nextRegularRun);
            ActionEventUtils.onCreatedActionEvent(User.UID_SYSTEM, volume.getAccountId(), EventVO.LEVEL_INFO, EventTypes.EVENT_SNAPSHOT_SKIPPED, true,
                    String.format("Skipped scheduled snapshot for volume [%s] because its instance has not been running since the last snapshot.", volume),
                    volume.getId(), ApiCommandResourceType.Volume.toString());
        } finally {
            if (lockedSchedule != null) {
                _snapshotScheduleDao.releaseFromLockTable(snapshotScheId);
            }
        }
    }

    /**
     * Verifies if a snapshot for a volume can be scheduled or not based on volume and account status, and removes it from the snapshot scheduler if its policy was removed.
     *
     * @param snapshotToBeScheduled the snapshot to be scheduled
     * @param volume the volume associated with the snapshot to be scheduled
     * @return <code>true</code> if the snapshot can be scheduled, and <code>false</code> otherwise.
     */
    protected boolean canSnapshotBeScheduled(final SnapshotScheduleVO snapshotToBeScheduled, final VolumeVO volume) {
        if (volume.getRemoved() != null) {
            logger.warn("Skipping snapshot [{}] for volume [{}] because it has been removed. Having a snapshot scheduled for a volume that has been "
                    + "removed is an inconsistency; please, check your database.", snapshotToBeScheduled, volume);
            return false;
        }

        if (volume.getPoolId() == null) {
            logger.debug("Skipping snapshot [{}] for volume [{}] because it is not attached to any storage pool.", snapshotToBeScheduled, volume);
            return false;
        }

        if (isAccountRemovedOrDisabled(snapshotToBeScheduled, volume)) {
            return false;
        }

        if (_snapshotPolicyDao.findById(snapshotToBeScheduled.getPolicyId()) == null) {
            logger.debug("Snapshot's policy [{}] for volume [{}] has been removed; " +
                    "therefore, this snapshot will be removed from the snapshot scheduler.",
                    snapshotToBeScheduled.getPolicyId(), volume);
            _snapshotScheduleDao.remove(snapshotToBeScheduled.getId());
        }

        logger.debug("Snapshot [{}] for volume [{}] can be executed.", snapshotToBeScheduled, volume);
        return true;
    }

    protected boolean isAccountRemovedOrDisabled(final SnapshotScheduleVO snapshotToBeExecuted, final VolumeVO volume) {
        Account volAcct = _acctDao.findById(volume.getAccountId());

        if (volAcct == null) {
            logger.debug(String.format("Skipping snapshot [%s] for volume [%s] because its account [%s] has been removed.",
                    snapshotToBeExecuted, volume, volume.getAccountId()));
            return true;
        }

        if (volAcct.getState() == Account.State.DISABLED) {
            logger.debug("Skipping snapshot [{}] for volume [{}] because its account [{}] is disabled.", snapshotToBeExecuted, volume, volAcct);
            return true;
        }

        return false;
    }

    protected Date scheduleNextSnapshotJob(final SnapshotScheduleVO snapshotSchedule) {
        if (snapshotSchedule == null) {
            return null;
        }
        final Long policyId = snapshotSchedule.getPolicyId();
        if (policyId.longValue() == Snapshot.MANUAL_POLICY_ID) {
            // Don't need to schedule the next job for this.
            return null;
        }
        final SnapshotPolicyVO snapshotPolicy = _snapshotPolicyDao.findById(policyId);
        if (snapshotPolicy == null) {
            _snapshotScheduleDao.expunge(snapshotSchedule.getId());
        }
        return scheduleNextSnapshotJob(snapshotPolicy);
    }

    @Override
    @DB
    public Date scheduleNextSnapshotJob(final SnapshotPolicyVO policy) {
        if (policy == null) {
            return null;
        }

        // If display attribute is false then remove schedules if any and return.
        if(!policy.isDisplay()){
            removeSchedule(policy.getVolumeId(), policy.getId());
            return null;
        }

        final long policyId = policy.getId();
        if (policyId == Snapshot.MANUAL_POLICY_ID) {
            return null;
        }

        if (_volsDao.findById(policy.getVolumeId()) == null) {
            logger.warn("Found snapshot policy: {} for volume ID: {} that does not exist or has been removed", policy, policy.getVolumeId());
            removeSchedule(policy.getVolumeId(), policy.getId());
            return null;
        }

        final Date nextSnapshotTimestamp = getNextScheduledTime(policyId, _currentTimestamp);
        SnapshotScheduleVO spstSchedVO = _snapshotScheduleDao.findOneByVolumePolicy(policy.getVolumeId(), policy.getId());
        if (spstSchedVO == null) {
            spstSchedVO = new SnapshotScheduleVO(policy.getVolumeId(), policyId, nextSnapshotTimestamp);
            _snapshotScheduleDao.persist(spstSchedVO);
        } else {
            TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.CLOUD_DB);

            try {
                spstSchedVO = _snapshotScheduleDao.acquireInLockTable(spstSchedVO.getId());
                spstSchedVO.setPolicyId(policyId);
                spstSchedVO.setScheduledTimestamp(nextSnapshotTimestamp);
                spstSchedVO.setAsyncJobId(null);
                spstSchedVO.setSnapshotId(null);
                _snapshotScheduleDao.update(spstSchedVO.getId(), spstSchedVO);
                txn.commit();
            } finally {
                if (spstSchedVO != null) {
                    _snapshotScheduleDao.releaseFromLockTable(spstSchedVO.getId());
                }
                txn.close();
            }
        }
        return nextSnapshotTimestamp;
    }

    @Override
    public void scheduleOrCancelNextSnapshotJobOnDisplayChange(final SnapshotPolicyVO policy, boolean previousDisplay) {

        // Take action only if display changed
        if(policy.isDisplay() != previousDisplay ){
            if(policy.isDisplay()){
                scheduleNextSnapshotJob(policy);
            }else{
                removeSchedule(policy.getVolumeId(), policy.getId());
            }
        }
    }


    @Override
    @DB
    public boolean removeSchedule(final Long volumeId, final Long policyId) {
        // We can only remove schedules which are in the future. Not which are already executed in the past.
        final SnapshotScheduleVO schedule = _snapshotScheduleDao.getCurrentSchedule(volumeId, policyId, false);
        boolean success = true;
        if (schedule != null) {
            success = _snapshotScheduleDao.remove(schedule.getId());
        }
        if (!success) {
            logger.debug("Error while deleting Snapshot schedule: " + schedule);
        }
        return success;
    }

    @Override
    public boolean configure(final String name, final Map<String, Object> params) throws ConfigurationException {

        _snapshotPollInterval = NumbersUtil.parseInt(_configDao.getValue("snapshot.poll.interval"), 300);
        final boolean snapshotsRecurringTest = Boolean.parseBoolean(_configDao.getValue("snapshot.recurring.test"));
        if (snapshotsRecurringTest) {
            // look for some test values in the configuration table so that snapshots can be taken more frequently (QA test code)
            final int minutesPerHour = NumbersUtil.parseInt(_configDao.getValue("snapshot.test.minutes.per.hour"), 60);
            final int hoursPerDay = NumbersUtil.parseInt(_configDao.getValue("snapshot.test.hours.per.day"), 24);
            final int daysPerWeek = NumbersUtil.parseInt(_configDao.getValue("snapshot.test.days.per.week"), 7);
            final int daysPerMonth = NumbersUtil.parseInt(_configDao.getValue("snapshot.test.days.per.month"), 30);
            final int weeksPerMonth = NumbersUtil.parseInt(_configDao.getValue("snapshot.test.weeks.per.month"), 4);
            final int monthsPerYear = NumbersUtil.parseInt(_configDao.getValue("snapshot.test.months.per.year"), 12);

            _testTimerTask = new TestClock(this, minutesPerHour, hoursPerDay, daysPerWeek, daysPerMonth, weeksPerMonth, monthsPerYear);
        }
        _currentTimestamp = new Date();

        logger.info("Snapshot Scheduler is configured.");

        return true;
    }

    @Override
    @DB
    public boolean start() {
        // reschedule all policies after management restart
        final List<SnapshotPolicyVO> policyInstances = _snapshotPolicyDao.listAll();
        for (final SnapshotPolicyVO policyInstance : policyInstances) {
            if (policyInstance.getId() != Snapshot.MANUAL_POLICY_ID) {
                scheduleNextSnapshotJob(policyInstance);
            }
        }
        if (_testTimerTask != null) {
            _testClockTimer = new Timer("TestClock");
            // Run the test clock every 60s. Because every tick is counted as 1 minute.
            // Else it becomes too confusing.
            _testClockTimer.schedule(_testTimerTask, 100 * 1000L, 60 * 1000L);
        } else {
            final TimerTask timerTask = new ManagedContextTimerTask() {
                @Override
                protected void runInContext() {
                    try {
                        final Date currentTimestamp = new Date();
                        poll(currentTimestamp);
                    } catch (final Throwable t) {
                        logger.warn("Catch throwable in snapshot scheduler ", t);
                    }
                }
            };
            _testClockTimer = new Timer("SnapshotPollTask");
            _testClockTimer.schedule(timerTask, _snapshotPollInterval * 1000L, _snapshotPollInterval * 1000L);
        }

        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }
}

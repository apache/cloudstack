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

import javax.ejb.Local;
import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.command.user.snapshot.CreateSnapshotCmd;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.framework.jobs.AsyncJobDispatcher;
import org.apache.cloudstack.framework.jobs.AsyncJobManager;
import org.apache.cloudstack.framework.jobs.dao.AsyncJobDao;
import org.apache.cloudstack.framework.jobs.impl.AsyncJobVO;
import org.apache.cloudstack.managed.context.ManagedContextTimerTask;

import com.cloud.api.ApiDispatcher;
import com.cloud.api.ApiGsonHelper;
import com.cloud.event.ActionEventUtils;
import com.cloud.event.EventTypes;
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
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.TransactionLegacy;

@Component
@Local(value = {SnapshotScheduler.class})
public class SnapshotSchedulerImpl extends ManagerBase implements SnapshotScheduler {
    private static final Logger s_logger = Logger.getLogger(SnapshotSchedulerImpl.class);

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
            s_logger.debug("Current time is " + currentTime + ". NextScheduledTime of policyId " + policyId + " is " + nextScheduledTime);
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
                    checkStatusOfCurrentlyExecutingSnapshots();
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
    }

    private void checkStatusOfCurrentlyExecutingSnapshots() {
        final SearchCriteria<SnapshotScheduleVO> sc = _snapshotScheduleDao.createSearchCriteria();
        sc.addAnd("asyncJobId", SearchCriteria.Op.NNULL);
        final List<SnapshotScheduleVO> snapshotSchedules = _snapshotScheduleDao.search(sc, null);
        for (final SnapshotScheduleVO snapshotSchedule : snapshotSchedules) {
            final Long asyncJobId = snapshotSchedule.getAsyncJobId();
            final AsyncJobVO asyncJob = _asyncJobDao.findById(asyncJobId);
            switch (asyncJob.getStatus()) {
                case SUCCEEDED:
                    // The snapshot has been successfully backed up.
                    // The snapshot state has also been cleaned up.
                    // We can schedule the next job for this snapshot.
                    // Remove the existing entry in the snapshot_schedule table.
                    scheduleNextSnapshotJob(snapshotSchedule);
                    break;
                case FAILED:
                    // Check the snapshot status.
                    final Long snapshotId = snapshotSchedule.getSnapshotId();
                    if (snapshotId == null) {
                        // createSnapshotAsync exited, successfully or unsuccessfully,
                        // even before creating a snapshot record
                        // No cleanup needs to be done.
                        // Schedule the next snapshot.
                        scheduleNextSnapshotJob(snapshotSchedule);
                    } else {
                        final SnapshotVO snapshot = _snapshotDao.findById(snapshotId);
                        if (snapshot == null || snapshot.getRemoved() != null) {
                            // This snapshot has been deleted successfully from the primary storage
                            // Again no cleanup needs to be done.
                            // Schedule the next snapshot.
                            // There's very little probability that the code reaches this point.
                            // The snapshotId is a foreign key for the snapshot_schedule table
                            // set to ON DELETE CASCADE. So if the snapshot entry is deleted, the snapshot_schedule entry will be too.
                            // But what if it has only been marked as removed?
                            scheduleNextSnapshotJob(snapshotSchedule);
                        } else {
                            // The management server executing this snapshot job appears to have crashed
                            // while creating the snapshot on primary storage/or backing it up.
                            // We have no idea whether the snapshot was successfully taken on the primary or not.
                            // Schedule the next snapshot job.
                            // The ValidatePreviousSnapshotCommand will take appropriate action on this snapshot
                            // If the snapshot was taken successfully on primary, it will retry backing it up.
                            // and cleanup the previous snapshot
                            // Set the userId to that of system.
                            //_snapshotManager.validateSnapshot(1L, snapshot);
                            // In all cases, schedule the next snapshot job
                            scheduleNextSnapshotJob(snapshotSchedule);
                        }
                    }

                    break;
                case IN_PROGRESS:
                    // There is no way of knowing from here whether
                    // 1) Another management server is processing this snapshot job
                    // 2) The management server has crashed and this snapshot is lying
                    // around in an inconsistent state.
                    // Hopefully, this can be resolved at the backend when the current snapshot gets executed.
                    // But if it remains in this state, the current snapshot will not get executed.
                    // And it will remain in stasis.
                    break;
            }
        }
    }

    @DB
    protected void scheduleSnapshots() {
        String displayTime = DateUtil.displayDateInTimezone(DateUtil.GMT_TIMEZONE, _currentTimestamp);
        s_logger.debug("Snapshot scheduler.poll is being called at " + displayTime);

        final List<SnapshotScheduleVO> snapshotsToBeExecuted = _snapshotScheduleDao.getSchedulesToExecute(_currentTimestamp);
        s_logger.debug("Got " + snapshotsToBeExecuted.size() + " snapshots to be executed at " + displayTime);

        for (final SnapshotScheduleVO snapshotToBeExecuted : snapshotsToBeExecuted) {
            SnapshotScheduleVO tmpSnapshotScheduleVO = null;
            final long snapshotScheId = snapshotToBeExecuted.getId();
            final long policyId = snapshotToBeExecuted.getPolicyId();
            final long volumeId = snapshotToBeExecuted.getVolumeId();
            try {
                final VolumeVO volume = _volsDao.findById(volumeId);
                if (volume.getPoolId() == null) {
                    // this volume is not attached
                    continue;
                }
                Account volAcct = _acctDao.findById(volume.getAccountId());
                if (volAcct == null || volAcct.getState() == Account.State.disabled) {
                    // this account has been removed, so don't trigger recurring snapshot
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Skip snapshot for volume " + volume.getUuid() + " since its account has been removed or disabled");
                    }
                    continue;
                }
                if (_snapshotPolicyDao.findById(policyId) == null) {
                    _snapshotScheduleDao.remove(snapshotToBeExecuted.getId());
                }
                if (s_logger.isDebugEnabled()) {
                    final Date scheduledTimestamp = snapshotToBeExecuted.getScheduledTimestamp();
                    displayTime = DateUtil.displayDateInTimezone(DateUtil.GMT_TIMEZONE, scheduledTimestamp);
                    s_logger.debug("Scheduling 1 snapshot for volume id " + volumeId + " (volume name:" +
                            volume.getName() + ") for schedule id: " + snapshotToBeExecuted.getId() + " at " + displayTime);
                }

                tmpSnapshotScheduleVO = _snapshotScheduleDao.acquireInLockTable(snapshotScheId);
                final Long eventId =
                    ActionEventUtils.onScheduledActionEvent(User.UID_SYSTEM, volume.getAccountId(), EventTypes.EVENT_SNAPSHOT_CREATE, "creating snapshot for volume Id:" +
                        volumeId, true, 0);

                final Map<String, String> params = new HashMap<String, String>();
                params.put(ApiConstants.VOLUME_ID, "" + volumeId);
                params.put(ApiConstants.POLICY_ID, "" + policyId);
                params.put("ctxUserId", "1");
                params.put("ctxAccountId", "" + volume.getAccountId());
                params.put("ctxStartEventId", String.valueOf(eventId));

                final CreateSnapshotCmd cmd = new CreateSnapshotCmd();
                ComponentContext.inject(cmd);
                _dispatcher.dispatchCreateCmd(cmd, params);
                params.put("id", "" + cmd.getEntityId());
                params.put("ctxStartEventId", "1");

                AsyncJobVO job = new AsyncJobVO("", User.UID_SYSTEM, volume.getAccountId(), CreateSnapshotCmd.class.getName(),
                        ApiGsonHelper.getBuilder().create().toJson(params), cmd.getEntityId(),
                        cmd.getInstanceType() != null ? cmd.getInstanceType().toString() : null, null);
                job.setDispatcher(_asyncDispatcher.getName());

                final long jobId = _asyncMgr.submitAsyncJob(job);

                tmpSnapshotScheduleVO.setAsyncJobId(jobId);
                _snapshotScheduleDao.update(snapshotScheId, tmpSnapshotScheduleVO);
            } catch (final Exception e) {
                // TODO Logging this exception is enough?
                s_logger.warn("Scheduling snapshot failed due to " + e.toString());
            } finally {
                if (tmpSnapshotScheduleVO != null) {
                    _snapshotScheduleDao.releaseFromLockTable(snapshotScheId);
                }
            }
        }
    }

    private Date scheduleNextSnapshotJob(final SnapshotScheduleVO snapshotSchedule) {
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
            s_logger.debug("Error while deleting Snapshot schedule with Id: " + schedule.getId());
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

        s_logger.info("Snapshot Scheduler is configured.");

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
                        s_logger.warn("Catch throwable in snapshot scheduler ", t);
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

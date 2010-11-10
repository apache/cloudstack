/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package com.cloud.storage.snapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.async.AsyncJobResult;
import com.cloud.async.AsyncJobVO;
import com.cloud.async.dao.AsyncJobDao;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.storage.Snapshot;
import com.cloud.storage.SnapshotPolicyVO;
import com.cloud.storage.SnapshotScheduleVO;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.storage.dao.SnapshotPolicyDao;
import com.cloud.storage.dao.SnapshotPolicyRefDao;
import com.cloud.storage.dao.SnapshotScheduleDao;
import com.cloud.storage.dao.StoragePoolHostDao;
import com.cloud.utils.DateUtil;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.DateUtil.IntervalType;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.utils.component.Inject;
import com.cloud.utils.concurrency.TestClock;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;

/**
 *
 */
@Local(value={SnapshotScheduler.class})
public class SnapshotSchedulerImpl implements SnapshotScheduler {
    private static final Logger s_logger = Logger.getLogger(SnapshotSchedulerImpl.class);
    
    private String _name = null;
    @Inject protected AsyncJobDao             _asyncJobDao;
    @Inject protected SnapshotDao             _snapshotDao;
    @Inject protected SnapshotScheduleDao     _snapshotScheduleDao;
    @Inject protected SnapshotPolicyDao       _snapshotPolicyDao;
    @Inject protected SnapshotPolicyRefDao    _snapshotPolicyRefDao;
    @Inject protected SnapshotManager         _snapshotManager;
    @Inject protected StoragePoolHostDao      _poolHostDao; 
    
    private static final int ACQUIRE_GLOBAL_LOCK_TIMEOUT_FOR_COOPERATION = 5;    // 5 seconds
    private int        _snapshotPollInterval;
    private Timer      _testClockTimer;
    private Date       _currentTimestamp;
    private TestClock  _testTimerTask;
    
    private Date getNextScheduledTime(long policyId, Date currentTimestamp) {
        SnapshotPolicyVO policy = _snapshotPolicyDao.findById(policyId);
        Date nextTimestamp = null;
        if (policy != null) {
            short intervalType = policy.getInterval();
            IntervalType type = DateUtil.getIntervalType(intervalType);
            String schedule = policy.getSchedule();
            String timezone = policy.getTimezone();
            nextTimestamp = DateUtil.getNextRunTime(type, schedule, timezone, currentTimestamp);
            String currentTime = DateUtil.displayDateInTimezone(DateUtil.GMT_TIMEZONE, currentTimestamp);
            String nextScheduledTime = DateUtil.displayDateInTimezone(DateUtil.GMT_TIMEZONE, nextTimestamp);
            s_logger.debug("Current time is " + currentTime + ". NextScheduledTime of policyId " + policyId + " is " + nextScheduledTime);
        }
        return nextTimestamp;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void poll(Date currentTimestamp) {
        // We don't maintain the time. The timer task does.
        _currentTimestamp = currentTimestamp;
        
        GlobalLock scanLock = GlobalLock.getInternLock(this.getClass().getName());
        try {
            if(scanLock.lock(ACQUIRE_GLOBAL_LOCK_TIMEOUT_FOR_COOPERATION)) {
                try {
                    checkStatusOfCurrentlyExecutingSnapshots();
                } finally {
                    scanLock.unlock();
                }
            }
        } finally {
            scanLock.releaseRef();
        }
        
        scanLock = GlobalLock.getInternLock(this.getClass().getName());
        try {
            if(scanLock.lock(ACQUIRE_GLOBAL_LOCK_TIMEOUT_FOR_COOPERATION)) {
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
        SearchCriteria sc = _snapshotScheduleDao.createSearchCriteria();
        sc.addAnd("asyncJobId", SearchCriteria.Op.NNULL);
        List<SnapshotScheduleVO> snapshotSchedules = _snapshotScheduleDao.search(sc, null);
        for (SnapshotScheduleVO snapshotSchedule : snapshotSchedules) {
            Long asyncJobId = snapshotSchedule.getAsyncJobId();
            AsyncJobVO asyncJob = _asyncJobDao.findById(asyncJobId);
            switch (asyncJob.getStatus()) {
            case AsyncJobResult.STATUS_SUCCEEDED:
                // The snapshot has been successfully backed up.
                // The snapshot state has also been cleaned up.
                // We can schedule the next job for this snapshot.
                // Remove the existing entry in the snapshot_schedule table.
                scheduleNextSnapshotJob(snapshotSchedule);
                break;
            case AsyncJobResult.STATUS_FAILED:
                // Check the snapshot status.
                Long snapshotId = snapshotSchedule.getSnapshotId();
                if (snapshotId == null) {
                    // createSnapshotAsync exited, successfully or unsuccessfully,
                    // even before creating a snapshot record
                    // No cleanup needs to be done.
                    // Schedule the next snapshot.
                    scheduleNextSnapshotJob(snapshotSchedule);
                }
                else {
                    SnapshotVO snapshot = _snapshotDao.findById(snapshotId);
                    if (snapshot == null || snapshot.getRemoved() != null) {
                        // This snapshot has been deleted successfully from the primary storage
                        // Again no cleanup needs to be done.
                        // Schedule the next snapshot.
                        // There's very little probability that the code reaches this point.
                        // The snapshotId is a foreign key for the snapshot_schedule table
                        // set to ON DELETE CASCADE. So if the snapshot entry is deleted, the snapshot_schedule entry will be too.
                        // But what if it has only been marked as removed?
                        scheduleNextSnapshotJob(snapshotSchedule);
                    }
                    else {
                        // The management server executing this snapshot job appears to have crashed
                        // while creating the snapshot on primary storage/or backing it up.
                        // We have no idea whether the snapshot was successfully taken on the primary or not.
                        // Schedule the next snapshot job.
                        // The ValidatePreviousSnapshotCommand will take appropriate action on this snapshot
                        // If the snapshot was taken successfully on primary, it will retry backing it up.
                        // and cleanup the previous snapshot
                        // Set the userId to that of system.
                        _snapshotManager.validateSnapshot(1L, snapshot);
                        // In all cases, schedule the next snapshot job 
                        scheduleNextSnapshotJob(snapshotSchedule);
                    }
                }
 
                break;
            case AsyncJobResult.STATUS_IN_PROGRESS:
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

    /**
     * {@inheritDoc}
     */
    @Override
    @DB
    public Long scheduleManualSnapshot(Long userId, Long volumeId) {
        // Check if there is another manual snapshot scheduled which hasn't been executed yet.
        SearchCriteria sc = _snapshotScheduleDao.createSearchCriteria();
        sc.addAnd("volumeId", SearchCriteria.Op.EQ, volumeId);
        sc.addAnd("policyId", SearchCriteria.Op.EQ, Snapshot.MANUAL_POLICY_ID);
        
        List<SnapshotScheduleVO> snapshotSchedules = _snapshotScheduleDao.search(sc, null);
        if (!snapshotSchedules.isEmpty()) {
            Date scheduledTimestamp = snapshotSchedules.get(0).getScheduledTimestamp();
            String dateDisplay = DateUtil.displayDateInTimezone(DateUtil.GMT_TIMEZONE, scheduledTimestamp);
            s_logger.error("Can't execute another manual snapshot for volume: " + volumeId + 
                           " while another manual snapshot for the same volume is being created/backed up. " +
                           "The older snapshot was scheduled at " + dateDisplay);
            return null;
        }
        
        SnapshotScheduleVO snapshotSchedule = new SnapshotScheduleVO(volumeId, Snapshot.MANUAL_POLICY_ID, _currentTimestamp);
        // There is a race condition here. Two threads enter here. 
        // Both find that there are no manual snapshots for the same volume scheduled.
        // Both try to schedule. One fails, which is what we wanted anyway.
        _snapshotScheduleDao.persist(snapshotSchedule); 
        List<Long> policyIds = new ArrayList<Long>();
        policyIds.add(Snapshot.MANUAL_POLICY_ID);
        return _snapshotManager.createSnapshotAsync(userId, volumeId, policyIds);
    }
    
    @DB
    protected void scheduleSnapshots() {
        String displayTime = DateUtil.displayDateInTimezone(DateUtil.GMT_TIMEZONE, _currentTimestamp);
        s_logger.debug("Snapshot scheduler.poll is being called at " + displayTime);
        
        List<SnapshotScheduleVO> snapshotsToBeExecuted = _snapshotScheduleDao.getSchedulesToExecute(_currentTimestamp);
        s_logger.debug("Got " + snapshotsToBeExecuted.size() + " snapshots to be executed at " + displayTime);
        
        // This is done for recurring snapshots, which are executed by the system automatically
        // Hence set user id to that of system
        long userId = 1;
        
        // The volumes which are going to be snapshotted now.
        // The value contains the list of policies associated with this new snapshot.
        // There can be more than one policy for a list if different policies coincide for the same volume.
        Map<Long, List<Long>> listOfVolumesSnapshotted = new HashMap<Long, List<Long>>();
        Calendar cal = Calendar.getInstance(DateUtil.GMT_TIMEZONE);
        cal.add(Calendar.MINUTE, -15);
        //Skip any snapshots older than 15mins
        Date graceTime = cal.getTime();
        
        for (SnapshotScheduleVO snapshotToBeExecuted : snapshotsToBeExecuted) {
            Date scheduleTime = snapshotToBeExecuted.getScheduledTimestamp();
            if(scheduleTime.before(graceTime)){
                s_logger.info("Snapshot schedule older than 15mins. Skipping snapshot for volume: "+snapshotToBeExecuted.getVolumeId());
                scheduleNextSnapshotJob(snapshotToBeExecuted);
                continue;
            }
            long policyId = snapshotToBeExecuted.getPolicyId();
            long volumeId = snapshotToBeExecuted.getVolumeId();
            List<Long> coincidingPolicies = listOfVolumesSnapshotted.get(volumeId);
            if (coincidingPolicies != null) {
                s_logger.debug("The snapshot for this volume " + volumeId + " and policy " + policyId + " has already been sent for execution along with " + coincidingPolicies.size() + " policies in total");
                // This can happen if this coincided with another schedule with a different policy
                // It would have added all the coinciding policies for the volume to the Map
                
                if (coincidingPolicies.contains(snapshotToBeExecuted.getPolicyId())) {
                    // Don't need to do anything now. The snapshot is already scheduled for execution.
                    s_logger.debug("coincidingPolicies contains snapshotToBeExecuted id: " + snapshotToBeExecuted.getId() + ". Don't need to do anything now. The snapshot is already scheduled for execution.");
                }
                else {
                    // This will not happen
                    s_logger.warn("Snapshot Schedule " + snapshotToBeExecuted.getId() +
                                  " is ready for execution now at timestamp " + _currentTimestamp +
                                  " but is not coincident with one being executed for volume " + volumeId);
                    // Add this to the list of policies for the snapshot schedule
                    coincidingPolicies.add(snapshotToBeExecuted.getPolicyId());
                    listOfVolumesSnapshotted.put(volumeId, coincidingPolicies);
                    
                }
            }
            else {
                coincidingPolicies = new ArrayList<Long>();
                List<SnapshotScheduleVO> coincidingSchedules = _snapshotScheduleDao.getCoincidingSnapshotSchedules(volumeId, _currentTimestamp);

                if (s_logger.isDebugEnabled()) {
                    Date scheduledTimestamp = snapshotToBeExecuted.getScheduledTimestamp();
                    displayTime = DateUtil.displayDateInTimezone(DateUtil.GMT_TIMEZONE, scheduledTimestamp);
                }

                Transaction txn = Transaction.currentTxn();
                txn.start();
                // There are more snapshots scheduled for this volume at the same time.
                // Club all the policies together and append them to the coincidingPolicies List
                StringBuilder coincidentSchedules = new StringBuilder();
                for (SnapshotScheduleVO coincidingSchedule : coincidingSchedules) {
                    coincidingPolicies.add(coincidingSchedule.getPolicyId());
                    coincidentSchedules.append(coincidingSchedule.getId() + ", ");
                }
                txn.commit();
                
                s_logger.debug("Scheduling 1 snapshot for volume " + volumeId + " for schedule ids: " + coincidentSchedules + " at " + displayTime);
                long jobId = _snapshotManager.createSnapshotAsync(userId, volumeId, coincidingPolicies);
				
                // Add this snapshot to the listOfVolumesSnapshotted
				// So that the coinciding schedules don't get scheduled again.
				listOfVolumesSnapshotted.put(volumeId, coincidingPolicies);
                
            }
        }
    }

    private Date scheduleNextSnapshotJob(SnapshotScheduleVO snapshotSchedule) {
        Long policyId = snapshotSchedule.getPolicyId();
        Long expectedId = snapshotSchedule.getId();
        if (_snapshotScheduleDao.findById(expectedId) != null) {
            // We need to acquire a lock and delete it, then release the lock.
            // But I don't know how to.
            _snapshotScheduleDao.delete(expectedId);
        }
        if (policyId.longValue() == Snapshot.MANUAL_POLICY_ID) {
            // Don't need to schedule the next job for this.
            return null;
        }
        SnapshotPolicyVO snapshotPolicy = _snapshotPolicyDao.findById(policyId);
        return scheduleNextSnapshotJob(snapshotPolicy);
    }
    
    @Override @DB
    public Date scheduleNextSnapshotJob(SnapshotPolicyVO policyInstance) {
        long policyId = policyInstance.getId();
        Date nextSnapshotTimestamp = getNextScheduledTime(policyId, _currentTimestamp);
        
        SnapshotScheduleVO snapshotScheduleVO = new SnapshotScheduleVO(policyInstance.getVolumeId(), policyId, nextSnapshotTimestamp);
        _snapshotScheduleDao.persist(snapshotScheduleVO);
        return nextSnapshotTimestamp;
    }
    
    @Override @DB
    public boolean removeSchedule(Long volumeId, Long policyId) {
        // We can only remove schedules which are in the future. Not which are already executed in the past.
        SnapshotScheduleVO schedule = _snapshotScheduleDao.getCurrentSchedule(volumeId, policyId, false);
        boolean success = true;
        if (schedule != null) {
            success = _snapshotScheduleDao.delete(schedule.getId());
        }
        if(!success){
            s_logger.debug("Error while deleting Snapshot schedule with Id: "+schedule.getId());
        }
        return success;
    }


    @Override
    public boolean configure(String name, Map<String, Object> params)
    throws ConfigurationException {
        _name = name;

        ComponentLocator locator = ComponentLocator.getCurrentLocator();
        
        ConfigurationDao configDao = locator.getDao(ConfigurationDao.class);
        if (configDao == null) {
            s_logger.error("Unable to get the configuration dao. " + ConfigurationDao.class.getName());
            return false;
        }
        _snapshotPollInterval = NumbersUtil.parseInt(configDao.getValue("snapshot.poll.interval"), 300);
        boolean snapshotsRecurringTest = Boolean.parseBoolean(configDao.getValue("snapshot.recurring.test"));
        if (snapshotsRecurringTest) {
            // look for some test values in the configuration table so that snapshots can be taken more frequently (QA test code)
            int minutesPerHour = NumbersUtil.parseInt(configDao.getValue("snapshot.test.minutes.per.hour"), 60);
            int hoursPerDay = NumbersUtil.parseInt(configDao.getValue("snapshot.test.hours.per.day"), 24);
            int daysPerWeek = NumbersUtil.parseInt(configDao.getValue("snapshot.test.days.per.week"), 7);
            int daysPerMonth = NumbersUtil.parseInt(configDao.getValue("snapshot.test.days.per.month"), 30);
            int weeksPerMonth = NumbersUtil.parseInt(configDao.getValue("snapshot.test.weeks.per.month"), 4);
            int monthsPerYear = NumbersUtil.parseInt(configDao.getValue("snapshot.test.months.per.year"), 12);
    
            _testTimerTask = new TestClock(this, minutesPerHour, hoursPerDay, daysPerWeek, daysPerMonth, weeksPerMonth, monthsPerYear);
        }
        _currentTimestamp = new Date();
        s_logger.info("Snapshot Scheduler is configured.");
       
        return true;
    }

    @Override
    public String getName() {
        return _name;
    }

    @Override @DB
    public boolean start() {

        // reschedule all policies after management restart
        List<SnapshotScheduleVO> snapshotSchedules = _snapshotScheduleDao.listAll();
        for (SnapshotScheduleVO snapshotSchedule : snapshotSchedules) {
            _snapshotScheduleDao.delete(snapshotSchedule.getId());
        }
        List<SnapshotPolicyVO> policyInstances = _snapshotPolicyDao.listAll();
        for (SnapshotPolicyVO policyInstance : policyInstances) {
            if (policyInstance.getId() != Snapshot.MANUAL_POLICY_ID) {
                scheduleNextSnapshotJob(policyInstance);
            }
        }
        if (_testTimerTask != null) {
            _testClockTimer = new Timer("TestClock");
            // Run the test clock every 60s. Because every tick is counted as 1 minute.
            // Else it becomes too confusing.
            _testClockTimer.schedule(_testTimerTask, 100*1000L, 60*1000L);
        }
        else {
            TimerTask timerTask = new TimerTask() {
                public void run() {
                    Date currentTimestamp = new Date();
                    poll(currentTimestamp);
                }
            };
            _testClockTimer = new Timer("SnapshotPollTask");
            _testClockTimer.schedule(timerTask, _snapshotPollInterval*1000L, _snapshotPollInterval*1000L);
        }
        
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }
}

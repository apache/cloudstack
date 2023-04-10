/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cloudstack.vm.schedule;

import com.cloud.api.ApiDispatcher;
import com.cloud.api.ApiGsonHelper;
import com.cloud.event.ActionEventUtils;
import com.cloud.event.EventTypes;
import com.cloud.user.User;
import com.cloud.utils.DateUtil;
import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineManager;
import com.google.common.primitives.Longs;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.command.user.vm.RebootVMCmd;
import org.apache.cloudstack.api.command.user.vm.StartVMCmd;
import org.apache.cloudstack.api.command.user.vm.StopVMCmd;
import org.apache.cloudstack.framework.jobs.AsyncJobDispatcher;
import org.apache.cloudstack.framework.jobs.AsyncJobManager;
import org.apache.cloudstack.framework.jobs.impl.AsyncJobVO;
import org.apache.cloudstack.managed.context.ManagedContextTimerTask;
import org.apache.cloudstack.vm.schedule.dao.VMScheduleDao;
import org.apache.cloudstack.vm.schedule.dao.VMScheduledJobDao;
import org.apache.log4j.Logger;
import org.springframework.scheduling.support.CronExpression;

import javax.inject.Inject;
import javax.naming.ConfigurationException;
import javax.persistence.EntityExistsException;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class VMSchedulerImpl extends ManagerBase implements VMScheduler {
    private static Logger LOGGER = Logger.getLogger(VMSchedulerImpl.class);

    @Inject
    private VMScheduledJobDao vmScheduledJobDao;

    @Inject
    private VMScheduleDao vmScheduleDao;

    @Inject
    private VirtualMachineManager virtualMachineManager;
    @Inject
    private AsyncJobManager asyncJobManager;
    @Inject
    private ApiDispatcher apiDispatcher;

    private AsyncJobDispatcher asyncJobDispatcher;

    private Timer vmSchedulerTimer;
    private Date currentTimestamp;

    public AsyncJobDispatcher getAsyncJobDispatcher() {
        return asyncJobDispatcher;
    }

    public void setAsyncJobDispatcher(final AsyncJobDispatcher dispatcher) {
        asyncJobDispatcher = dispatcher;
    }

    @Override
    public void removeScheduledJobs(List<Long> vmScheduleIds) {
        if (vmScheduleIds == null || vmScheduleIds.isEmpty()){
            return;
        }
        SearchBuilder<VMScheduledJobVO> sb = vmScheduledJobDao.createSearchBuilder();
        sb.and("vm_schedule_id", sb.entity().getVmScheduleId(), SearchCriteria.Op.IN);
        sb.and("async_job_id", sb.entity().getAsyncJobId(), SearchCriteria.Op.NULL);

        SearchCriteria<VMScheduledJobVO> sc = sb.create();
        sc.setParameters("id", vmScheduleIds.toArray());

        int rowsRemoved =  vmScheduledJobDao.remove(sc);
        LOGGER.info(String.format("Removed [%s] scheduled jobs for VM", rowsRemoved));
    }

    @Override
    public void updateScheduledJob(VMScheduleVO vmSchedule) {
        removeScheduledJobs(Longs.asList(vmSchedule.getId()));
        scheduleNextJob(vmSchedule);
    }

    public Date scheduleNextJob(Long vmScheduleId) {
        VMScheduleVO vmSchedule = vmScheduleDao.findById(vmScheduleId);
        return scheduleNextJob(vmSchedule);
    }

    @Override
    public Date scheduleNextJob(VMScheduleVO vmSchedule) {

        // TODO: Ensure that no duplicate jobs are created. Already created a unique key in database.

        if (!vmSchedule.getEnabled()) {
            return null;
        }

        CronExpression cron = CronExpression.parse(vmSchedule.getSchedule());
        Date startDate = vmSchedule.getStartDate();
        Date endDate = vmSchedule.getEndDate();
        Date now = new Date();

        if (endDate != null && now.compareTo(endDate) > 0) {
            LOGGER.warn("End time is less that current time. Disabling the schedule.");
            vmSchedule.setEnabled(false);
            vmScheduleDao.persist(vmSchedule);
            return null;
        }

        ZonedDateTime ts = null;
        ZoneId tz = ZoneId.of(vmSchedule.getTimeZone());
        if (startDate.compareTo(now) > 0) {
            ts = cron.next(ZonedDateTime.ofInstant(startDate.toInstant(), tz));
        } else {
            ts = cron.next(ZonedDateTime.ofInstant(now.toInstant(), tz));
        }
        Date scheduledDateTime = Date.from(ts.toInstant());
        VMScheduledJobVO scheduledJob = new VMScheduledJobVO(vmSchedule.getVmId(), vmSchedule.getId(), vmSchedule.getAction(), scheduledDateTime);
        try {
            vmScheduledJobDao.persist(scheduledJob);
        } catch (EntityExistsException exception) {
            LOGGER.debug("Job is already scheduled.");
        }
        return scheduledDateTime;
    }

    /**
     * @param name
     * @param params
     * @return
     * @throws ConfigurationException
     */
    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        return false;
    }

    /**
     * Start any background tasks.
     *
     * @return true if the tasks were started, false otherwise.
     */
    @Override
    public boolean start() {
        currentTimestamp = new Date();
        // Remove pending jobs where scheduled time is < current time
        // TODO: Run this at some regular intervals instead of start up.
        // TODO: Don't remove recent scheduled jobs
        for (final VMScheduledJobVO scheduledJob : vmScheduledJobDao.listAllExpiredPendingJobs()) {
            LOGGER.info("Removing pending job since scheduled time has passed");
            vmScheduledJobDao.expunge(scheduledJob.getId());
        }

        // Schedule jobs for tasks
        for (final VMScheduleVO schedule : vmScheduleDao.listAllActiveSchedules()) {
            scheduleNextJob(schedule);
        }

        final TimerTask schedulerPollTask = new ManagedContextTimerTask() {
            @Override
            protected void runInContext() {
                try {
                    poll(new Date());
                } catch (final Throwable t) {
                    LOGGER.warn("Catch throwable in VM scheduler ", t);
                }
            }
        };

        vmSchedulerTimer = new Timer("VMSchedulerPollTask");
        // TODO: use configkeys like this - vmSchedulerTimer.schedule(schedulerPollTask, VMSchedulerPollingInterval.value() * 1000L, VMSchedulerPollingInterval.value() * 1000L);
        vmSchedulerTimer.schedule(schedulerPollTask, 1000L, 60 * 1000L);
        return true;
    }

    /**
     * Stop any background tasks.
     *
     * @return true background tasks were stopped, false otherwise.
     */
    @Override
    public boolean stop() {
        return false;
    }

    @Override
    public void poll(Date timestamp) {
        currentTimestamp = timestamp;
        GlobalLock scanLock = GlobalLock.getInternLock("vmScheduler.poll");
        try {
            if (scanLock.lock(5)) {
                try {
                    checkStatusOfCurrentlyExecutingJobs();
                } finally {
                    scanLock.unlock();
                }
            }
        } finally {
            scanLock.releaseRef();
        }

        scanLock = GlobalLock.getInternLock("vmScheduler.poll");
        try {
            if (scanLock.lock(5)) {
                try {
                    scheduleJobs(); // Create async job and update scheduled job
                } finally {
                    scanLock.unlock();
                }
            }
        } finally {
            scanLock.releaseRef();
        }

        try {
            cleanupVMScheduledJobs();
        }
        catch (Exception e) {
            LOGGER.warn("Error in cleaning up vm scheduled jobs", e);
        }
    }

    private void cleanupVMScheduledJobs() {
        // keep only latest 5 jobs
        //
//        DELETE FROM scheduled_jobs
//        WHERE id NOT IN (SELECT id
//        FROM scheduled_jobs
//        WHERE
//        AND schedule_id IN (SELECT schedule_id FROM (SELECT schedule_id, COUNT(*)
//                FROM scheduled_jobs
//                GROUP BY schedule_id) a
//        WHERE count > 5)
//        ORDER BY id DESC LIMIT 5
//)

//        vmScheduledJobDao.
        return;
    }

    // Create async jobs for VM scheduled jobs
    private void scheduleJobs() {
        String displayTime = DateUtil.displayDateInTimezone(DateUtil.GMT_TIMEZONE, currentTimestamp);
        LOGGER.debug(String.format("VM scheduler.poll is being called at %s", displayTime));

        final List<VMScheduledJobVO> vmScheduledJobs = vmScheduledJobDao.getSchedulesToExecute(currentTimestamp);
        LOGGER.debug("Got " + vmScheduledJobs.size() + " scheduled jobs to be executed at " + displayTime);

        Map<Long,VMScheduledJobVO> jobsToExecute = new HashMap<>();
        Map<Long,List<VMScheduledJobVO>> jobsNotToExecute = new HashMap<>();
        for (final VMScheduledJobVO vmScheduledJobVO : vmScheduledJobs) {
            long vmId = vmScheduledJobVO.getVmId();
            if (jobsToExecute.get(vmId) == null) {
                jobsToExecute.put(vmId, vmScheduledJobVO);
            } else {
                jobsNotToExecute.computeIfAbsent(vmId, k -> new ArrayList<>()).add(vmScheduledJobVO);
            }
        }

        for (Map.Entry<Long, VMScheduledJobVO> entry : jobsToExecute.entrySet()) {
            VMScheduledJobVO vmScheduledJob = entry.getValue();
            VMScheduledJobVO tmpVMScheduleJob = null;
            try {
                if (LOGGER.isDebugEnabled()) {
                    final Date scheduledTimestamp = vmScheduledJob.getScheduledTime();
                    displayTime = DateUtil.displayDateInTimezone(DateUtil.GMT_TIMEZONE, scheduledTimestamp);
                    LOGGER.debug(String.format("Scheduling %s for VM id %d for schedule id: %d at %s",
                            vmScheduledJob.getAction(), vmScheduledJob.getVmId(), vmScheduledJob.getVmScheduleId(), displayTime));
                }

                tmpVMScheduleJob = vmScheduledJobDao.acquireInLockTable(vmScheduledJob.getId());

                Long jobId = null;
                switch (vmScheduledJob.getAction()) {
                    case STOP:
                        jobId = scheduleStopVMJob(vmScheduledJob);
                        break;
                    case START:
                        jobId = scheduleStartVMJob(vmScheduledJob);
                        break;
                    case REBOOT:
                        jobId = scheduleRebootVMJob(vmScheduledJob);
                        break;
                    default:
                        LOGGER.error("Invalid action " + vmScheduledJob.getAction() + " for VM scheduled job id: " + vmScheduledJob.getId());
                        continue;
                }

                tmpVMScheduleJob.setAsyncJobId(jobId);
                vmScheduledJobDao.update(vmScheduledJob.getId(), tmpVMScheduleJob);
            } catch (final Exception e) {
                LOGGER.warn(String.format("Executing scheduled job id: %s failed due to %s", vmScheduledJob.getId(), e.toString()));
            } finally {
                if (tmpVMScheduleJob != null) {
                    vmScheduledJobDao.releaseFromLockTable(vmScheduledJob.getId());
                }
            }
        }

        for (Map.Entry<Long, List<VMScheduledJobVO>> entry : jobsNotToExecute.entrySet()) {
            Long vmId = entry.getKey();
            List<VMScheduledJobVO> skippedVmScheduledJobVOS = entry.getValue();

            for (final VMScheduledJobVO skippedVmScheduledJobVO : skippedVmScheduledJobVOS) {
                VMScheduledJobVO scheduledJob = jobsToExecute.get(vmId);
                LOGGER.info(String.format("Skipping scheduled job [id: %s, vmId: %s] because of conflict with another scheduled job [id: %s]",
                        skippedVmScheduledJobVO.getId(), vmId, scheduledJob.getId()));
            }
        }
    }

    private long scheduleStartVMJob(VMScheduledJobVO vmScheduledJob) throws Exception {
        VirtualMachine vm = virtualMachineManager.findById(vmScheduledJob.getVmId());
        // TODO: Skip and log if the VM is already in Running state
        final Long eventId =
                ActionEventUtils.onScheduledActionEvent(User.UID_SYSTEM, vm.getAccountId(), EventTypes.EVENT_VM_START,
                        String.format("Executing action (%s) for VM Id:%s", vmScheduledJob.getAction(), vm.getUuid()),
                        vm.getId(), ApiCommandResourceType.VirtualMachine.toString(), true, 0);

        final Map<String, String> params = new HashMap<String, String>();
        params.put(ApiConstants.ID, String.valueOf(vm.getId()));
        params.put("ctxUserId", "1");
        params.put("ctxAccountId", String.valueOf(vm.getAccountId()));
        params.put("ctxStartEventId", String.valueOf(eventId));

        final StartVMCmd cmd = new StartVMCmd();
        ComponentContext.inject(cmd);

        AsyncJobVO job = new AsyncJobVO("", User.UID_SYSTEM, vm.getAccountId(), StartVMCmd.class.getName(),
                ApiGsonHelper.getBuilder().create().toJson(params), vm.getId(),
                cmd.getApiResourceType() != null ? cmd.getApiResourceType().toString() : null, null);
        job.setDispatcher(asyncJobDispatcher.getName());

        return  asyncJobManager.submitAsyncJob(job);
    }


    private long scheduleStopVMJob(VMScheduledJobVO vmScheduledJob) throws Exception {
        VirtualMachine vm = virtualMachineManager.findById(vmScheduledJob.getVmId());
        // TODO: Skip and log if the VM is already in Running state
        final Long eventId =
                ActionEventUtils.onScheduledActionEvent(User.UID_SYSTEM, vm.getAccountId(), EventTypes.EVENT_VM_STOP, "Executing action (" + vmScheduledJob.getAction() + ") for VM Id:" +
                        vm.getUuid(), vm.getId(), ApiCommandResourceType.VirtualMachine.toString(), true, 0);

        final Map<String, String> params = new HashMap<String, String>();
        params.put(ApiConstants.ID, String.valueOf(vm.getId()));
        params.put("ctxUserId", "1");
        params.put("ctxAccountId", String.valueOf(vm.getAccountId()));
        params.put("ctxStartEventId", String.valueOf(eventId));

        final StopVMCmd cmd = new StopVMCmd();
        ComponentContext.inject(cmd);

        AsyncJobVO job = new AsyncJobVO("", User.UID_SYSTEM, vm.getAccountId(), StopVMCmd.class.getName(),
                ApiGsonHelper.getBuilder().create().toJson(params), vm.getId(),
                cmd.getApiResourceType() != null ? cmd.getApiResourceType().toString() : null, null);
        job.setDispatcher(asyncJobDispatcher.getName());

        return asyncJobManager.submitAsyncJob(job);
    }

    private long scheduleRebootVMJob(VMScheduledJobVO vmScheduledJob) throws Exception {
        VirtualMachine vm = virtualMachineManager.findById(vmScheduledJob.getVmId());
        // TODO: Skip and log if the VM is already in Running state
        final Long eventId =
                ActionEventUtils.onScheduledActionEvent(User.UID_SYSTEM, vm.getAccountId(), EventTypes.EVENT_VM_REBOOT, "Executing action (" + vmScheduledJob.getAction() + ") for VM Id:" +
                        vm.getUuid(), vm.getId(), ApiCommandResourceType.VirtualMachine.toString(), true, 0);

        final Map<String, String> params = new HashMap<String, String>();
        params.put(ApiConstants.ID, String.valueOf(vm.getId()));
        params.put("ctxUserId", "1");
        params.put("ctxAccountId", String.valueOf(vm.getAccountId()));
        params.put("ctxStartEventId", String.valueOf(eventId));

        final RebootVMCmd cmd = new RebootVMCmd();
        ComponentContext.inject(cmd);

        AsyncJobVO job = new AsyncJobVO("", User.UID_SYSTEM, vm.getAccountId(), RebootVMCmd.class.getName(),
                ApiGsonHelper.getBuilder().create().toJson(params), vm.getId(),
                cmd.getApiResourceType() != null ? cmd.getApiResourceType().toString() : null, null);
        job.setDispatcher(asyncJobDispatcher.getName());

        return  asyncJobManager.submitAsyncJob(job);
    }

    // Check status and schedule next job if required
    private void checkStatusOfCurrentlyExecutingJobs() {
        // TODO: Fix this query to fetch last executed jobs only? Maybe add status column to keep track of this?
        final SearchCriteria<VMScheduledJobVO> sc = vmScheduledJobDao.createSearchCriteria();
        sc.addAnd("asyncJobId", SearchCriteria.Op.NNULL);
        final List<VMScheduledJobVO> vmScheduledJobs = vmScheduledJobDao.search(sc, null);
        for (final VMScheduledJobVO vmScheduledJob : vmScheduledJobs) {
            final Long asyncJobId = vmScheduledJob.getAsyncJobId();
            final AsyncJobVO asyncJob = asyncJobManager.getAsyncJob(asyncJobId);
            switch (asyncJob.getStatus()) {
                case SUCCEEDED:
                case FAILED:
                    final Date nextDateTime = scheduleNextJob(vmScheduledJob.getVmScheduleId());
                    final String nextScheduledTime = DateUtil.displayDateInTimezone(DateUtil.GMT_TIMEZONE, nextDateTime);
                    LOGGER.debug(String.format("Next scheduled time for VM ID %s Schedule ID %s is %s", vmScheduledJob.getVmId(), vmScheduledJob.getVmScheduleId(), nextScheduledTime));
                    break;
                default:
                    LOGGER.debug(String.format("Found async job [id: %s, vmId: %s] with status [%s] and cmd information: [cmd: %s, cmdInfo: %s].", asyncJob.getId(), vmScheduledJob.getVmId(),
                            asyncJob.getStatus(), asyncJob.getCmd(), asyncJob.getCmdInfo()));
                    break;
            }
        }
    }
}

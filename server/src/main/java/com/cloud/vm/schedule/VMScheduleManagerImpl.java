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
package com.cloud.vm.schedule;

import com.cloud.api.ApiDispatcher;
import com.cloud.api.ApiGsonHelper;
import com.cloud.event.ActionEvent;
import com.cloud.event.ActionEventUtils;
import com.cloud.event.EventTypes;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.user.User;
import com.cloud.utils.DateUtil;
import com.cloud.utils.ListUtils;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.component.PluggableService;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionStatus;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.dao.VMInstanceDao;
import com.cloud.vm.schedule.dao.VMScheduleDao;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.command.user.vmschedule.CreateVMScheduleCmd;
import org.apache.cloudstack.api.command.user.vmschedule.ListVMScheduleCmd;
import org.apache.cloudstack.api.command.user.vmschedule.UpdateVMScheduleCmd;
import org.apache.cloudstack.api.command.user.vmschedule.DeleteVMScheduleCmd;
import org.apache.cloudstack.api.command.user.vmschedule.EnableVMScheduleCmd;
import org.apache.cloudstack.api.command.user.vmschedule.DisableVMScheduleCmd;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.framework.jobs.AsyncJobDispatcher;
import org.apache.cloudstack.framework.jobs.AsyncJobManager;
import org.apache.cloudstack.framework.jobs.impl.AsyncJobVO;
import org.apache.cloudstack.managed.context.ManagedContextTimerTask;
import org.apache.cloudstack.poll.BackgroundPollManager;
import org.apache.log4j.Logger;
import javax.inject.Inject;
import javax.naming.ConfigurationException;
import com.cloud.utils.db.TransactionCallback;

import java.util.Date;
import java.util.TimerTask;
import java.util.Timer;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class VMScheduleManagerImpl extends ManagerBase implements VMScheduleManager, Configurable, PluggableService {
    public static final Logger LOGGER = Logger.getLogger(VMScheduleManagerImpl.class);

    @Inject
    private VMScheduleDao vmScheduleDao;

    @Inject
    private VirtualMachineManager vmManager;

    @Inject
    private BackgroundPollManager backgroundPollManager;

    @Inject
    private AsyncJobManager asyncJobManager;

    @Inject
    private VMInstanceDao vmInstanceDao;

    @Inject
    private ApiDispatcher apiDispatcher;

    private AsyncJobDispatcher asyncJobDispatcher;

    private Timer vmTimer;

    private Date currentTimestamp;

    private ScheduledExecutorService backgroundPollTaskScheduler;

    private volatile boolean isConfiguredAndStarted = false;

    private static final ConfigKey<Boolean> EnableVMSchedulerInterval = new ConfigKey<>("Advanced", Boolean.class,
            "vm.scheduler.enable", "false",
            "Enable the VMScheduler Interval  to schedule tasks on VM.", false);

    private static final ConfigKey<Integer> VMSchedulerInterval = new ConfigKey<>("Advanced", Integer.class,
            "vm.scheduler.interval", "60",
            "The interval at which VM Scheduler runs in milliseconds", false, EnableVMSchedulerInterval.key());

    @Override
    public boolean start() {
        currentTimestamp = new Date();
        for (final VMScheduleVO vmSchedule : vmScheduleDao.listAll()) {
            scheduleNextVMJob(vmSchedule);
        }

        if (isConfiguredAndStarted) {
            return true;
        }
        backgroundPollTaskScheduler = Executors.newScheduledThreadPool(100, new NamedThreadFactory("BackgroundTaskPollManager"));
        final TimerTask vmPollTask = new ManagedContextTimerTask() {
            @Override
            protected void runInContext() {
                try {
                    poll(new Date());
                } catch (final Throwable t) {
                    LOGGER.warn("Catch throwable in vm scheduler ", t);
                }
            }
        };
        backgroundPollTaskScheduler.scheduleWithFixedDelay(vmPollTask, VMSchedulerInterval.value() * 1000L, VMSchedulerInterval.value() * 1000L, TimeUnit.MILLISECONDS);
        isConfiguredAndStarted = true;
        return true;
    }

    @Override
    public boolean stop() {
        if (isConfiguredAndStarted) {
            backgroundPollTaskScheduler.shutdown();
        }
        return true;
    }

    @Override
    public String getConfigComponentName() {
        return VMScheduleManager.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[] {
                VMSchedulerInterval,
                EnableVMSchedulerInterval
        };
    }

    @Override
    public boolean configure(final String name, final Map<String, Object> params) throws ConfigurationException {
        return true;
    }

    @Override
    public List<Class<?>> getCommands() {
        final List<Class<?>> cmdList = new ArrayList<>();
        cmdList.add(CreateVMScheduleCmd.class);
        cmdList.add(ListVMScheduleCmd.class);
        cmdList.add(UpdateVMScheduleCmd.class);
        cmdList.add(DeleteVMScheduleCmd.class);
        cmdList.add(EnableVMScheduleCmd.class);
        cmdList.add(DisableVMScheduleCmd.class);
        cmdList.add(UpdateVMScheduleCmd.class);
        return cmdList;
    }

    @Override
    public VMSchedule findVMSchedule(Long id) {
        if (id == null || id < 1L) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace(String.format("VMSchedule ID is invalid [%s]", id));
                return null;
            }
        }

         VMSchedule vmSchedule = vmScheduleDao.findById(id);
        if (vmSchedule == null) {
            if(LOGGER.isTraceEnabled()) {
                LOGGER.trace(String.format("VmSchedule ID not found [id=%s]", id));
                return null;
            }
        }

        return vmSchedule;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_VMSCHEDULE_CREATE, eventDescription = "creating vm schedule", async = true)
    public VMSchedule createVMSchedule(CreateVMScheduleCmd cmd) {
        final String description = cmd.getDescription();
        final String action = cmd.getAction();
        final String tag = cmd.getTag();
        final Long vmId = cmd.getVmId();
        final String intervalType = cmd.getIntervalType();
        final String scheduleString = cmd.getSchedule();
        final TimeZone timeZone = TimeZone.getTimeZone(cmd.getTimezone());

        if (intervalType == null) {
            throw new CloudRuntimeException("Invalid interval type provided");
        }

        final VMInstanceVO vm = findVmById(vmId);

        long accountId = vm.getAccountId();
        final String timezoneId = timeZone.getID();
        if (!timezoneId.equals(cmd.getTimezone())) {
            LOGGER.warn("Using timezone: " + timezoneId + " for running this snapshot policy as an equivalent of " + cmd.getTimezone());
        }

        Date nextDateTime = null;
        try {
            nextDateTime = DateUtil.getNextRunTime(DateUtil.IntervalType.valueOf(intervalType), cmd.getSchedule(), timezoneId, null);
        } catch (Exception e) {
            throw new InvalidParameterValueException("Invalid schedule: " + cmd.getSchedule() + " for interval type: " + cmd.getIntervalType());
        }
        return vmScheduleDao.persist(new VMScheduleVO(vmId, description, action, intervalType, scheduleString, timezoneId, nextDateTime, tag, 1L));
    }

    @Override
    public List<VMSchedule> listVMSchedules(ListVMScheduleCmd cmd) {
        if(cmd.getId() != null) {
            VMSchedule vmSchedule = findVMSchedule(cmd.getId());
            List<VMSchedule> arr = new ArrayList<>();
            arr.add(vmSchedule);
            return arr;
        }

        List<? extends VMSchedule> vmSchedules= vmScheduleDao.listAll();
        return ListUtils.toListOfInterface(vmSchedules);
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_VMSCHEDULE_DELETE, eventDescription = "deleting VM Schedule")
    public boolean deleteVMSchedule(Long vmScheduleId) {
        VMSchedule vmSchedule = vmScheduleDao.findById(vmScheduleId);
        if (vmSchedule == null) {
            throw new InvalidParameterValueException("unable to find the vm schedule with id " + vmScheduleId);
        }

        return vmScheduleDao.remove(vmSchedule.getId());
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_VMSCHEDULE_ENABLE, eventDescription = "enable VM Schedule")
    public boolean enableVMSchedule(Long vmScheduleId) {
        VMScheduleVO vmSchedule = vmScheduleDao.findById(vmScheduleId);
        if (vmSchedule == null) {
            throw new InvalidParameterValueException("unable to find the vm schedule with id " + vmScheduleId);
        }

        vmSchedule.setState(VMSchedule.State.Enabled);
        boolean updateResult = vmScheduleDao.update(vmSchedule.getId(), vmSchedule);

        return updateResult;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_VMSCHEDULE_DISABLE, eventDescription = "disable VM Schedule")
    public boolean disableVMSchedule(Long vmScheduleId) {
        VMScheduleVO vmSchedule = vmScheduleDao.findById(vmScheduleId);
        if (vmSchedule == null) {
            throw new InvalidParameterValueException("unable to find the vm schedule with id " + vmScheduleId);
        }

        vmSchedule.setState(VMSchedule.State.Disabled);
        boolean updateResult = vmScheduleDao.update(vmSchedule.getId(), vmSchedule);

        return updateResult;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_VMSCHEDULE_UPDATE, eventDescription = "update VM Schedule")
    public VMSchedule updateVMSchedule(UpdateVMScheduleCmd cmd) {
        VMScheduleVO vmSchedule = vmScheduleDao.findById(cmd.getId());
        if (vmSchedule == null) {
            throw new InvalidParameterValueException("unable to find the vm schedule with id " + cmd.getId());
        }
        String description = cmd.getDescription();
        String schedule = cmd.getSchedule();
        String intervalType = cmd.getIntervalType();
        String action = cmd.getAction();
        String tag = cmd.getTag();
        String timezone = cmd.getTimezone();

        if (vmSchedule.getState() == VMSchedule.State.Disabled) {
            if (description != null)
                vmSchedule.setDescription(description);
            if(schedule != null)
                vmSchedule.setSchedule(schedule);
            if(intervalType != null)
                vmSchedule.setScheduleType(intervalType);
            if (action != null)
                vmSchedule.setAction(action);
            if (tag != null)
                vmSchedule.setTag(tag);
            if (timezone != null)
                vmSchedule.setTimezone(timezone);

            vmScheduleDao.update(vmSchedule.getId(), vmSchedule);
        } else {
            throw new InvalidParameterValueException("Disable the state of VM Schedule before updating it");
        }
        return vmSchedule;
    }

    public void poll(final Date timestamp) {
        currentTimestamp = timestamp;
        GlobalLock scanLock = GlobalLock.getInternLock("vm.poll");

        try {
            if (scanLock.lock(5)) {
                try {
                    checkStatusOfCurrentlyExecutingVMs();
                } finally {
                    scanLock.unlock();
                }
            }
        } finally {
            scanLock.releaseRef();
        }

        scanLock = GlobalLock.getInternLock("vm.poll");
        try {
            if (scanLock.lock(5)) {
                try {
                    scheduleVMs();
                } finally {
                    scanLock.unlock();
                }
            }
        } finally {
            scanLock.releaseRef();
        }
    }

    @DB
    public void scheduleVMs() {
        String displayTime = DateUtil.displayDateInTimezone(DateUtil.GMT_TIMEZONE, currentTimestamp);
        LOGGER.debug("VM poll is being called at " + displayTime);

        final List<VMScheduleVO> vmsToBeExecuted = vmScheduleDao.listAll();
        for (final VMScheduleVO vmSchedule : vmsToBeExecuted) {
            final long timeDifference = DateUtil.getTimeDifference(vmSchedule.getScheduledTimestamp(), currentTimestamp);

            if (timeDifference <= 30) {
                final Long vmScheduleId = vmSchedule.getId();
                final Long vmId = vmSchedule.getVmId();

                final VMInstanceVO vm = vmInstanceDao.findById(vmId);
                if (vm == null) {
                    vmScheduleDao.remove(vmScheduleId);
                    continue;
                }

                if (LOGGER.isDebugEnabled()) {
                    final Date scheduledTimestamp = vmSchedule.getScheduledTimestamp();
                    displayTime = DateUtil.displayDateInTimezone(DateUtil.GMT_TIMEZONE, scheduledTimestamp);
                    LOGGER.debug(String.format("Scheduling for VM [ID: %s, name: %s] schedule id: [%s] at [%s].",
                            vm.getId(), vm.getInstanceName(), vmSchedule.getId(), displayTime));
                }

                VMScheduleVO tmpVMScheduleVO = null;

                try {
                    if (vmSchedule.getState() == VMSchedule.State.Enabled) {
                        tmpVMScheduleVO = vmScheduleDao.acquireInLockTable(vmScheduleId);
                        Long jobId = performActionOnVM(vmSchedule.getAction(), vm);
                        if (jobId != null) {
                            vmSchedule.setAsyncJobId(jobId);
                            vmScheduleDao.update(vmScheduleId, vmSchedule);
                        }
                        scheduleNextVMJob(vmSchedule);
                    }
                } catch (Exception e) {
                    LOGGER.error(String.format("Scheduling VM failed due to: [%s].", e.getMessage()), e);
                } finally {
                    if (tmpVMScheduleVO != null) {
                        vmScheduleDao.releaseFromLockTable(vmScheduleId);
                    }
                }
            }
        }
    }

    private Long setAsyncJobForVMSchedule(VMInstanceVO vmInstance, Long eventId) {
        Long jobId;
        final Map<String, String> params = new HashMap<String, String>();
        params.put(ApiConstants.VIRTUAL_MACHINE_ID, "" + vmInstance.getId());
        params.put("ctxUserId", "1");
        params.put("ctxAccountId", "" + vmInstance.getAccountId());
        params.put("ctxStartEventId", String.valueOf(eventId));
        params.put("ctxHostId", "" + vmInstance.getHostId());
        params.put("id", "" + vmInstance.getId());

        AsyncJobVO job = new AsyncJobVO("", User.UID_SYSTEM, vmInstance.getAccountId(), VirtualMachineManager.class.getName(),
                ApiGsonHelper.getBuilder().create().toJson(params), vmInstance.getId(), null, null);
        job.setDispatcher(asyncJobDispatcher.getName());
        jobId = asyncJobManager.submitAsyncJob(job);

        return jobId;
    }

    private Long performActionOnVM(String action, VMInstanceVO vmInstance ) throws ResourceUnavailableException, InsufficientCapacityException {
        Long jobId = null;
        switch (action) {
            case "start":
                if (vmInstance.getState() == VirtualMachine.State.Running) {
                    LOGGER.debug("Virtual Machine is already running" + vmInstance.getId());
                    break;
                }
                final Long eventStartId = ActionEventUtils.onScheduledActionEvent(User.UID_SYSTEM, vmInstance.getAccountId(),
                        EventTypes.EVENT_VM_START, "Starting a VM for VM ID:" + vmInstance.getUuid(),
                        vmInstance.getId(), ApiCommandResourceType.VirtualMachine.toString(),
                        true, 0);
                vmManager.start(vmInstance.getUuid(), null);
                jobId = setAsyncJobForVMSchedule(vmInstance, eventStartId);
            case "stop":
                if (vmInstance.getState() == VirtualMachine.State.Stopped) {
                    LOGGER.debug("Virtual Machine is already stopped" + vmInstance.getId());
                    break;
                }
                final Long eventStopId = ActionEventUtils.onScheduledActionEvent(User.UID_SYSTEM, vmInstance.getAccountId(),
                        EventTypes.EVENT_VM_STOP, "stopping a VM for VM ID:" + vmInstance.getUuid(),
                        vmInstance.getId(), ApiCommandResourceType.VirtualMachine.toString(),
                        true, 0);
                vmManager.stop(vmInstance.getUuid());
                jobId = setAsyncJobForVMSchedule(vmInstance, eventStopId);
            case "reboot":
                final Long eventRebootId = ActionEventUtils.onScheduledActionEvent(User.UID_SYSTEM, vmInstance.getAccountId(),
                        EventTypes.EVENT_VM_REBOOT, "Rebooting a VM for VM ID:" + vmInstance.getUuid(),
                        vmInstance.getId(), ApiCommandResourceType.VirtualMachine.toString(),
                        true, 0);
                vmManager.reboot(vmInstance.getUuid(), null);
                jobId = setAsyncJobForVMSchedule(vmInstance, eventRebootId);
            case "forcestop":
                final Long eventForcedStoppedId = ActionEventUtils.onScheduledActionEvent(User.UID_SYSTEM, vmInstance.getAccountId(),
                        EventTypes.EVENT_VM_STOP, "Force Stop a VM for VM ID:" + vmInstance.getUuid(),
                        vmInstance.getId(), ApiCommandResourceType.VirtualMachine.toString(),
                        true, 0);
                vmManager.stopForced(vmInstance.getUuid());
                jobId = setAsyncJobForVMSchedule(vmInstance, eventForcedStoppedId);
        }

        return jobId;
    }

    @DB
    private Date scheduleNextVMJob(final VMScheduleVO vmSchedule) {
        final Date nextTimestamp = DateUtil.getNextRunTime(DateUtil.IntervalType.valueOf(vmSchedule.getScheduleType()), vmSchedule.getSchedule(),
                vmSchedule.getTimezone(), currentTimestamp);
        return Transaction.execute(new TransactionCallback<Date>() {
            @Override
            public Date doInTransaction(TransactionStatus status) {
                vmSchedule.setScheduledTimestamp(nextTimestamp);
                vmScheduleDao.update(vmSchedule.getId(), vmSchedule);
                return nextTimestamp;
            }
        });
    }

    private void checkStatusOfCurrentlyExecutingVMs() {
        final SearchCriteria<VMScheduleVO> sc = vmScheduleDao.createSearchCriteria();
        sc.addAnd("asyncJobId", SearchCriteria.Op.NULL);
        final List<VMScheduleVO> vmSchedules = vmScheduleDao.search(sc, null);
        for (final VMScheduleVO vmSchedule : vmSchedules) {
            final Long asyncJobId = vmSchedule.getAsyncJobId();
            final AsyncJobVO asyncJob = asyncJobManager.getAsyncJob(asyncJobId);
            switch (asyncJob.getStatus()) {
                case SUCCEEDED:
                case FAILED:
                    final Date nextDateTime = scheduleNextVMJob(vmSchedule);
                    final String nextScheduledTime = DateUtil.displayDateInTimezone(DateUtil.GMT_TIMEZONE, nextDateTime);
                    LOGGER.debug("Next vm scheduled time for VM ID " + vmSchedule.getVmId() + " is " + nextScheduledTime);
                    break;
                default:
                    LOGGER.debug(String.format("Found async vm schedule job [id: %s, vmId: %s] with status [%s] and cmd information: [cmd: %s, cmdInfo: %s].", asyncJob.getId(), vmSchedule.getVmId(),
                            asyncJob.getStatus(), asyncJob.getCmd(), asyncJob.getCmdInfo()));
                    break;
            }
        }
    }

    private VMInstanceVO findVmById(final Long vmId) {
        final VMInstanceVO vm = vmInstanceDao.findById(vmId);
        if (vm == null) {
            throw new CloudRuntimeException(String.format("Can't find any VM with ID: [%s].", vmId));
        }
        return vm;
    }
}

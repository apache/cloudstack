/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.cloudstack.vm.lease;

import com.cloud.alert.AlertManager;
import com.cloud.api.ApiGsonHelper;
import com.cloud.api.query.dao.UserVmJoinDao;
import com.cloud.api.query.vo.UserVmJoinVO;
import com.cloud.event.ActionEventUtils;
import com.cloud.user.Account;
import com.cloud.user.User;
import com.cloud.utils.DateUtil;
import com.cloud.utils.StringUtils;
import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.db.GlobalLock;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.command.user.vm.DestroyVMCmd;
import org.apache.cloudstack.api.command.user.vm.StopVMCmd;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.framework.jobs.AsyncJobDispatcher;
import org.apache.cloudstack.framework.jobs.AsyncJobManager;
import org.apache.cloudstack.framework.jobs.impl.AsyncJobVO;
import org.apache.cloudstack.managed.context.ManagedContextTimerTask;
import org.apache.commons.lang3.time.DateUtils;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class VMLeaseManagerImpl extends ManagerBase implements VMLeaseManager, Configurable {

    public static ConfigKey<Boolean> InstanceLeaseEnabled = new ConfigKey<>(ConfigKey.CATEGORY_ADVANCED, Boolean.class,
            "instance.lease.enabled", "false", "Indicates whether to enable the Instance Lease feature",
            true, List.of(ConfigKey.Scope.Global));

    private static final int ACQUIRE_GLOBAL_LOCK_TIMEOUT_FOR_COOPERATION = 5;   // 5 seconds

    @Inject
    private UserVmJoinDao userVmJoinDao;

    @Inject
    private AlertManager alertManager;

    @Inject
    private AsyncJobManager asyncJobManager;

    private AsyncJobDispatcher asyncJobDispatcher;

    Timer vmLeaseTimer;

    Timer vmLeaseAlterTimer;

    @Override
    public String getConfigComponentName() {
        return VMLeaseManager.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey[]{
                InstanceLeaseEnabled,
                InstanceLeaseDuration,
                InstanceLeaseExpiryAction,
                InstanceLeaseSchedulerInterval,
                InstanceLeaseAlertSchedule,
                InstanceLeaseAlertStartsAt
        };
    }

    public void setAsyncJobDispatcher(final AsyncJobDispatcher dispatcher) {
        asyncJobDispatcher = dispatcher;
    }

    @Override
    public boolean start() {
        final TimerTask schedulerPollTask = new ManagedContextTimerTask() {
            @Override
            protected void runInContext() {
                try {
                    poll(new Date());
                } catch (final Throwable t) {
                    logger.warn("Catch throwable in VM lease scheduler ", t);
                }
            }
        };

        final TimerTask leaseAlterSchedulerTask = new ManagedContextTimerTask() {
            @Override
            protected void runInContext() {
                try {
                    alert();
                } catch (final Throwable t) {
                    logger.warn("Catch throwable in VM lease scheduler ", t);
                }
            }
        };

        vmLeaseTimer = new Timer("VMLeasePollTask");
        vmLeaseTimer.scheduleAtFixedRate(schedulerPollTask, 5_000L, InstanceLeaseSchedulerInterval.value() * 1000L);

        vmLeaseAlterTimer = new Timer("VMLeaseAlertPollTask");
        vmLeaseAlterTimer.scheduleAtFixedRate(leaseAlterSchedulerTask, 5_000L, InstanceLeaseAlertSchedule.value() * 1000)
        ;
        return true;
    }

    protected void alert() {
        // as feature is disabled, no action is required
        if (!InstanceLeaseEnabled.value()) {
            return;
        }

        GlobalLock scanLock = GlobalLock.getInternLock("VMLeaseAlertScheduler");
        try {
            if (scanLock.lock(ACQUIRE_GLOBAL_LOCK_TIMEOUT_FOR_COOPERATION)) {
                try {
                    List<UserVmJoinVO> leaseExpiringForInstances = userVmJoinDao.listLeaseInstancesExpiringInDays(InstanceLeaseAlertStartsAt.value().intValue());
                    for (UserVmJoinVO instance : leaseExpiringForInstances) {
                        alertManager.sendAlert(AlertManager.AlertType.ALERT_TYPE_USERVM, instance.getDataCenterId(), instance.getPodId(),
                                "Lease expiring for instance id: " + instance.getUuid(), "Lease expiring for instance");
                    }
                } finally {
                    scanLock.unlock();
                }
            }
        } finally {
            scanLock.releaseRef();
        }
    }

    @Override
    public void poll(Date timestamp) {
        Date currentTimestamp = DateUtils.round(timestamp, Calendar.MINUTE);
        String displayTime = DateUtil.displayDateInTimezone(DateUtil.GMT_TIMEZONE, currentTimestamp);
        logger.debug("VMLeaseScheduler.poll is being called at {}", displayTime);

        if (!InstanceLeaseEnabled.value()) {
            logger.debug("Instance lease feature is disabled, no action is required");
            return;
        }

        GlobalLock scanLock = GlobalLock.getInternLock("VMLeaseScheduler");
        try {
            if (scanLock.lock(ACQUIRE_GLOBAL_LOCK_TIMEOUT_FOR_COOPERATION)) {
                try {
                    reallyRun();
                } finally {
                    scanLock.unlock();
                }
            }
        } finally {
            scanLock.releaseRef();
        }
    }

    protected void reallyRun() {
        // fetch user_instances having leaseDuration configured and has expired
        List<UserVmJoinVO> leaseExpiredInstances = userVmJoinDao.listEligibleInstancesWithExpiredLease();
        List<Long> actionableInstanceIds = new ArrayList<>();
        for (UserVmJoinVO userVmVO : leaseExpiredInstances) {
            // skip instance with delete protection for DESTROY action
            if (ExpiryAction.DESTROY.name().equals(userVmVO.getLeaseExpiryAction())
                    && userVmVO.isDeleteProtection() != null && userVmVO.isDeleteProtection()) {
                logger.debug("Ignoring DESTROY action on instance with id: {} as deleteProtection is enabled", userVmVO.getUuid());
                continue;
            }
            actionableInstanceIds.add(userVmVO.getId());
        }
        if (actionableInstanceIds.isEmpty()) {
            logger.debug("Lease scheduler found no instance to work upon");
            return;
        }

        List<Long> submittedJobIds = new ArrayList<>();
        List<Long> failedToSubmitInstanceIds = new ArrayList<>();
        for (Long instanceId : actionableInstanceIds) {
            UserVmJoinVO instance = userVmJoinDao.findById(instanceId);
            ExpiryAction expiryAction = getLeaseExpiryAction(instance);
            if (expiryAction == null) {
                continue;
            }
            // for qualified vms, prepare Stop/Destroy(Cmd) and submit to Job Manager
            final long eventId = ActionEventUtils.onCompletedActionEvent(User.UID_SYSTEM, instance.getAccountId(), null,
                    expiryAction.name(), true,
                    String.format("Executing action (%s) for VM: %s", instance.getLeaseExpiryAction(), instance),
                    instance.getId(), ApiCommandResourceType.VirtualMachine.toString(), 0);

            Long jobId = executeExpiryAction(instance, expiryAction, eventId);
            if (jobId != null) {
                submittedJobIds.add(jobId);
            } else {
                failedToSubmitInstanceIds.add(instanceId);
            }
        }
        logger.debug("Successfully submitted lease expiry jobs with ids: {}", submittedJobIds);
        if (!failedToSubmitInstanceIds.isEmpty()) {
            logger.debug("Lease scheduler failed to submit jobs for instance ids: {}", failedToSubmitInstanceIds);
        }
    }

    Long executeExpiryAction(UserVmJoinVO instance, ExpiryAction expiryAction, long eventId) {
        // for qualified vms, prepare Stop/Destroy(Cmd) and submit to Job Manager
        switch (expiryAction) {
            case STOP: {
                logger.debug("Stopping instance with id: {} on lease expiry", instance.getUuid());
                return executeStopInstanceJob(instance, true, eventId);
            }
            case DESTROY: {
                logger.debug("Destroying instance with id: {} on lease expiry", instance.getUuid());
                return executeDestroyInstanceJob(instance, true, eventId);
            }
            default: {
                logger.error("Invalid configuration for instance.lease.expiryaction for vm id: {}, " +
                        "valid values are: \"STOP\" and  \"DESTROY\"", instance.getUuid());
            }
        }
        return null;
    }

    long executeStopInstanceJob(UserVmJoinVO vm, boolean isForced, long eventId) {
        final Map<String, String> params = new HashMap<>();
        params.put(ApiConstants.ID, String.valueOf(vm.getId()));
        params.put("ctxUserId", String.valueOf(User.UID_SYSTEM));
        params.put("ctxAccountId", String.valueOf(Account.ACCOUNT_ID_SYSTEM));
        params.put(ApiConstants.CTX_START_EVENT_ID, String.valueOf(eventId));
        params.put(ApiConstants.FORCED, String.valueOf(isForced));
        final StopVMCmd cmd = new StopVMCmd();
        ComponentContext.inject(cmd);
        AsyncJobVO job = new AsyncJobVO("", User.UID_SYSTEM, vm.getAccountId(), StopVMCmd.class.getName(),
                ApiGsonHelper.getBuilder().create().toJson(params), vm.getId(),
                cmd.getApiResourceType() != null ? cmd.getApiResourceType().toString() : null, null);
        job.setDispatcher(asyncJobDispatcher.getName());
        return asyncJobManager.submitAsyncJob(job);
    }

    long executeDestroyInstanceJob(UserVmJoinVO vm, boolean isForced, long eventId) {
        final Map<String, String> params = new HashMap<>();
        params.put(ApiConstants.ID, String.valueOf(vm.getId()));
        params.put("ctxUserId", String.valueOf(User.UID_SYSTEM));
        params.put("ctxAccountId", String.valueOf(Account.ACCOUNT_ID_SYSTEM));
        params.put(ApiConstants.CTX_START_EVENT_ID, String.valueOf(eventId));
        params.put(ApiConstants.FORCED, String.valueOf(isForced));

        final DestroyVMCmd cmd = new DestroyVMCmd();
        ComponentContext.inject(cmd);

        AsyncJobVO job = new AsyncJobVO("", User.UID_SYSTEM, vm.getAccountId(), DestroyVMCmd.class.getName(),
                ApiGsonHelper.getBuilder().create().toJson(params), vm.getId(),
                cmd.getApiResourceType() != null ? cmd.getApiResourceType().toString() : null, null);
        job.setDispatcher(asyncJobDispatcher.getName());
        return asyncJobManager.submitAsyncJob(job);
    }

    public ExpiryAction getLeaseExpiryAction(UserVmJoinVO instance) {
        String action = instance.getLeaseExpiryAction();
        if (StringUtils.isEmpty(action)) {
            return null;
        }

        ExpiryAction expiryAction = null;
        try {
            expiryAction = ExpiryAction.valueOf(action);
        } catch (Exception ex) {
            logger.error("Invalid expiry action configured for instance with id: {}", instance.getUuid(), ex);
        }
        return expiryAction;
    }
}

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
import com.cloud.event.EventTypes;
import com.cloud.user.Account;
import com.cloud.user.User;
import com.cloud.utils.DateUtil;
import com.cloud.utils.StringUtils;
import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.db.GlobalLock;
import com.cloud.vm.VmDetailConstants;
import com.cloud.vm.dao.UserVmDetailsDao;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.command.user.vm.DestroyVMCmd;
import org.apache.cloudstack.api.command.user.vm.StopVMCmd;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.framework.jobs.AsyncJobDispatcher;
import org.apache.cloudstack.framework.jobs.AsyncJobManager;
import org.apache.cloudstack.framework.jobs.impl.AsyncJobVO;
import org.apache.cloudstack.managed.context.ManagedContextRunnable;
import org.apache.commons.lang3.time.DateUtils;

import javax.inject.Inject;
import javax.naming.ConfigurationException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class VMLeaseManagerImpl extends ManagerBase implements VMLeaseManager, Configurable {
    public static final String INSTANCE_LEASE_ENABLED = "instance.lease.enabled";

    public static ConfigKey<Boolean> InstanceLeaseEnabled = new ConfigKey<>(ConfigKey.CATEGORY_ADVANCED, Boolean.class,
            INSTANCE_LEASE_ENABLED, "false", "Indicates whether to enable the Instance lease," +
            " will be applicable only on instances created after lease is enabled. Disabling the feature cancels lease on existing instances with lease." +
            "Re-enabling feature will not cause lease expiry actions on grandfathered instances",
            true, List.of(ConfigKey.Scope.Global));

    private static final int ACQUIRE_GLOBAL_LOCK_TIMEOUT_FOR_COOPERATION = 5;   // 5 seconds

    @Inject
    private UserVmDetailsDao userVmDetailsDao;

    @Inject
    private UserVmJoinDao userVmJoinDao;

    @Inject
    private AlertManager alertManager;

    @Inject
    private AsyncJobManager asyncJobManager;

    private AsyncJobDispatcher asyncJobDispatcher;

    ScheduledExecutorService vmLeaseExecutor;
    ScheduledExecutorService vmLeaseAlertExecutor;

    @Override
    public String getConfigComponentName() {
        return VMLeaseManager.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey[]{
                InstanceLeaseEnabled,
                InstanceLeaseSchedulerInterval,
                InstanceLeaseAlertSchedule,
                InstanceLeaseExpiryAlertDaysBefore
        };
    }

    public void setAsyncJobDispatcher(final AsyncJobDispatcher dispatcher) {
        asyncJobDispatcher = dispatcher;
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        try {
            vmLeaseExecutor = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("VMLeasePollExecutor"));
            vmLeaseAlertExecutor = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("VMLeaseAlertPollExecutor"));
        } catch (final Exception e) {
            throw new ConfigurationException("Unable to to configure VMLeaseManagerImpl");
        }
        return true;
    }

    @Override
    public boolean start() {
        vmLeaseExecutor.scheduleAtFixedRate(new VMLeaseSchedulerTask(),5L, InstanceLeaseSchedulerInterval.value(), TimeUnit.SECONDS);
        vmLeaseAlertExecutor.scheduleAtFixedRate(new VMLeaseAlertSchedulerTask(), 5L, InstanceLeaseAlertSchedule.value(), TimeUnit.SECONDS);
        return true;
    }

    @Override
    public boolean stop() {
        vmLeaseExecutor.shutdown();
        vmLeaseAlertExecutor.shutdown();
        return true;
    }

    @Override
    public void cancelLeaseOnExistingInstances() {
        List<UserVmJoinVO> leaseExpiringForInstances = userVmJoinDao.listLeaseInstancesExpiringInDays(-1);
        logger.debug("Total instances found for lease cancellation: {}", leaseExpiringForInstances.size());
        for (UserVmJoinVO instance : leaseExpiringForInstances) {
            userVmDetailsDao.addDetail(instance.getId(), VmDetailConstants.INSTANCE_LEASE_EXECUTION, "CANCELLED", false);
            String leaseCancellationMsg = String.format("Lease is cancelled for the instancedId: %s ", instance.getUuid());
            ActionEventUtils.onActionEvent(instance.getUserId(), instance.getAccountId(), instance.getDomainId(),
                    EventTypes.VM_LEASE_CANCELLED, leaseCancellationMsg, instance.getId(), ApiCommandResourceType.VirtualMachine.toString());
        }
    }

    class VMLeaseSchedulerTask extends ManagedContextRunnable {
        @Override
        protected void runInContext() {
            Date currentTimestamp = DateUtils.round(new Date(), Calendar.MINUTE);
            String displayTime = DateUtil.displayDateInTimezone(DateUtil.GMT_TIMEZONE, currentTimestamp);
            logger.debug("VMLeaseSchedulerTask is being called at {}", displayTime);
            if (!InstanceLeaseEnabled.value()) {
                logger.debug("Instance lease feature is disabled, no action is required");
                return;
            }

            GlobalLock scanLock = GlobalLock.getInternLock("VMLeaseSchedulerTask");
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
    }

    class VMLeaseAlertSchedulerTask extends ManagedContextRunnable {
        @Override
        protected void runInContext() {
            // as feature is disabled, no action is required
            if (!InstanceLeaseEnabled.value()) {
                return;
            }

            GlobalLock scanLock = GlobalLock.getInternLock("VMLeaseAlertSchedulerTask");
            try {
                if (scanLock.lock(ACQUIRE_GLOBAL_LOCK_TIMEOUT_FOR_COOPERATION)) {
                    try {
                        List<UserVmJoinVO> leaseExpiringForInstances = userVmJoinDao.listLeaseInstancesExpiringInDays(InstanceLeaseExpiryAlertDaysBefore.value().intValue());
                        for (UserVmJoinVO instance : leaseExpiringForInstances) {
                            String leaseExpiryEventMsg =  String.format("Lease expiring for for instanceId: %s with action: %s", instance.getUuid(), instance.getLeaseExpiryAction());
                            ActionEventUtils.onActionEvent(instance.getUserId(), instance.getAccountId(), instance.getDomainId(),
                                    EventTypes.VM_LEASE_EXPIRING, leaseExpiryEventMsg, instance.getId(), ApiCommandResourceType.VirtualMachine.toString());
                        }
                    } finally {
                        scanLock.unlock();
                    }
                }
            } finally {
                scanLock.releaseRef();
            }
        }
    }

    @Override
    public Map<String, Object> getConfigParams() {
        return super.getConfigParams();
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
                    EventTypes.VM_LEASE_EXPIRED, true,
                    String.format("Executing lease expiry action (%s) for instanceId: %s", instance.getLeaseExpiryAction(), instance.getUuid()),
                    instance.getId(), ApiCommandResourceType.VirtualMachine.toString(), 0);

            Long jobId = executeExpiryAction(instance, expiryAction, eventId);
            if (jobId != null) {
                submittedJobIds.add(jobId);
                userVmDetailsDao.addDetail(instanceId, VmDetailConstants.INSTANCE_LEASE_EXECUTION, "DONE", false);
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

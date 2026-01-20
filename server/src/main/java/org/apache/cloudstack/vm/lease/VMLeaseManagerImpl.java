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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.command.user.vm.DestroyVMCmd;
import org.apache.cloudstack.api.command.user.vm.StopVMCmd;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.framework.jobs.AsyncJob;
import org.apache.cloudstack.framework.jobs.AsyncJobDispatcher;
import org.apache.cloudstack.framework.jobs.AsyncJobManager;
import org.apache.cloudstack.framework.jobs.impl.AsyncJobVO;
import org.apache.cloudstack.framework.messagebus.MessageBus;
import org.apache.cloudstack.framework.messagebus.MessageSubscriber;
import org.apache.cloudstack.jobs.JobInfo;
import org.apache.cloudstack.managed.context.ManagedContextRunnable;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.time.DateUtils;

import com.cloud.api.ApiGsonHelper;
import com.cloud.api.query.dao.UserVmJoinDao;
import com.cloud.api.query.vo.UserVmJoinVO;
import com.cloud.event.ActionEventUtils;
import com.cloud.event.EventTypes;
import com.cloud.user.Account;
import com.cloud.user.User;
import com.cloud.utils.DateUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.db.GlobalLock;
import com.cloud.vm.VmDetailConstants;
import com.cloud.vm.dao.VMInstanceDetailsDao;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class VMLeaseManagerImpl extends ManagerBase implements VMLeaseManager, Configurable {
    private static final int ACQUIRE_GLOBAL_LOCK_TIMEOUT_FOR_COOPERATION = 5;   // 5 seconds

    @Inject
    private VMInstanceDetailsDao vmInstanceDetailsDao;

    @Inject
    private UserVmJoinDao userVmJoinDao;

    @Inject
    private AsyncJobManager asyncJobManager;
    @Inject
    private MessageBus messageBus;

    private AsyncJobDispatcher asyncJobDispatcher;

    ScheduledExecutorService vmLeaseExecutor;
    ScheduledExecutorService vmLeaseExpiryEventExecutor;
    Gson gson = ApiGsonHelper.getBuilder().create();
    VMLeaseManagerSubscriber leaseManagerSubscriber;

    public static final String JOB_INITIATOR = "jobInitiator";

    @Override
    public String getConfigComponentName() {
        return VMLeaseManager.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey[]{
                InstanceLeaseEnabled,
                InstanceLeaseSchedulerInterval,
                InstanceLeaseExpiryEventSchedulerInterval,
                InstanceLeaseExpiryEventDaysBefore
        };
    }

    public void setAsyncJobDispatcher(final AsyncJobDispatcher dispatcher) {
        asyncJobDispatcher = dispatcher;
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        if (InstanceLeaseEnabled.value()) {
           scheduleLeaseExecutors();
        }
        return true;
    }

    @Override
    public boolean stop() {
        shutDownLeaseExecutors();
        return true;
    }

    /**
     * This method will cancel lease on instances running under lease
     * will be primarily used when feature gets disabled
     */
    public void cancelLeaseOnExistingInstances() {
        List<UserVmJoinVO> leaseExpiringForInstances = userVmJoinDao.listLeaseInstancesExpiringInDays(-1);
        logger.debug("Total instances found for lease cancellation: {}", leaseExpiringForInstances.size());
        for (UserVmJoinVO instance : leaseExpiringForInstances) {
            vmInstanceDetailsDao.addDetail(instance.getId(), VmDetailConstants.INSTANCE_LEASE_EXECUTION,
                    LeaseActionExecution.CANCELLED.name(), false);
            String leaseCancellationMsg = String.format("Lease is cancelled for the instance: %s (id: %s) ", instance.getName(), instance.getUuid());
            ActionEventUtils.onActionEvent(instance.getUserId(), instance.getAccountId(), instance.getDomainId(),
                    EventTypes.VM_LEASE_CANCELLED, leaseCancellationMsg, instance.getId(), ApiCommandResourceType.VirtualMachine.toString());
        }
    }

    @Override
    public void onLeaseFeatureToggle() {
        boolean isLeaseFeatureEnabled = VMLeaseManager.InstanceLeaseEnabled.value();
        if (isLeaseFeatureEnabled) {
            scheduleLeaseExecutors();
        } else {
            cancelLeaseOnExistingInstances();
            shutDownLeaseExecutors();
        }
    }

    private void scheduleLeaseExecutors() {
        if (vmLeaseExecutor == null || vmLeaseExecutor.isShutdown()) {
            logger.debug("Scheduling lease executor");
            vmLeaseExecutor = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("VMLeasePollExecutor"));
            vmLeaseExecutor.scheduleAtFixedRate(new VMLeaseSchedulerTask(),5L, InstanceLeaseSchedulerInterval.value(), TimeUnit.SECONDS);
        }

        if (vmLeaseExpiryEventExecutor == null || vmLeaseExpiryEventExecutor.isShutdown()) {
            logger.debug("Scheduling lease expiry event executor");
            vmLeaseExpiryEventExecutor = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("VmLeaseExpiryEventExecutor"));
            vmLeaseExpiryEventExecutor.scheduleAtFixedRate(new VMLeaseExpiryEventSchedulerTask(), 5L, InstanceLeaseExpiryEventSchedulerInterval.value(), TimeUnit.SECONDS);
        }
        addLeaseExpiryListener();
    }

    private void shutDownLeaseExecutors() {
        if (vmLeaseExecutor != null) {
            logger.debug("Shutting down lease executor");
            vmLeaseExecutor.shutdown();
            vmLeaseExecutor = null;
        }

        if (vmLeaseExpiryEventExecutor != null) {
            logger.debug("Shutting down lease expiry event executor");
            vmLeaseExpiryEventExecutor.shutdown();
            vmLeaseExpiryEventExecutor = null;
        }
        removeLeaseExpiryListener();
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

    class VMLeaseExpiryEventSchedulerTask extends ManagedContextRunnable {
        @Override
        protected void runInContext() {
            logger.debug("VMLeaseExpiryEventSchedulerTask is being called");
            // as feature is disabled, no action is required
            if (!InstanceLeaseEnabled.value()) {
                return;
            }

            GlobalLock scanLock = GlobalLock.getInternLock("VMLeaseExpiryEventSchedulerTask");
            try {
                if (scanLock.lock(ACQUIRE_GLOBAL_LOCK_TIMEOUT_FOR_COOPERATION)) {
                    try {
                        List<UserVmJoinVO> leaseExpiringForInstances = userVmJoinDao.listLeaseInstancesExpiringInDays(InstanceLeaseExpiryEventDaysBefore.value());
                        for (UserVmJoinVO instance : leaseExpiringForInstances) {
                            String leaseExpiryEventMsg =  String.format("Lease expiring for for instance: %s (id: %s) with action: %s",
                                    instance.getName(), instance.getUuid(), instance.getLeaseExpiryAction());
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

    protected void reallyRun() {
        // fetch user_instances having leaseDuration configured and has expired
        List<UserVmJoinVO> leaseExpiredInstances = userVmJoinDao.listEligibleInstancesWithExpiredLease();
        Set<Long> actionableInstanceIds = new HashSet<>();
        for (UserVmJoinVO userVmVO : leaseExpiredInstances) {
            // skip instance with delete protection for DESTROY action
            if (ExpiryAction.DESTROY.name().equals(userVmVO.getLeaseExpiryAction())
                    && userVmVO.isDeleteProtection() != null && userVmVO.isDeleteProtection()) {
                logger.debug("Ignoring DESTROY action on instance: {} (id: {}) as deleteProtection is enabled", userVmVO.getName(), userVmVO.getUuid());
                continue;
            }
            actionableInstanceIds.add(userVmVO.getId());
        }
        if (actionableInstanceIds.isEmpty()) {
            logger.debug("Lease scheduler found no instance to work upon");
            return;
        }

        List<Long> submittedJobIds = new ArrayList<>();
        List<Long> successfulInstanceIds = new ArrayList<>();
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
                    String.format("Executing lease expiry action (%s) for instance: %s (id: %s)", instance.getLeaseExpiryAction(), instance.getName(), instance.getUuid()),
                    instance.getId(), ApiCommandResourceType.VirtualMachine.toString(), 0);

            Long jobId = executeExpiryAction(instance, expiryAction, eventId);
            if (jobId != null) {
                submittedJobIds.add(jobId);
                successfulInstanceIds.add(instanceId);
            } else {
                failedToSubmitInstanceIds.add(instanceId);
            }
        }
        logger.debug("Successfully submitted lease expiry jobs with ids: {} and instance ids: {}", submittedJobIds, successfulInstanceIds);
        if (!failedToSubmitInstanceIds.isEmpty()) {
            logger.debug("Lease scheduler failed to submit jobs for instance ids: {}", failedToSubmitInstanceIds);
        }
    }

    Long executeExpiryAction(UserVmJoinVO instance, ExpiryAction expiryAction, long eventId) {
        // for qualified vms, prepare Stop/Destroy(Cmd) and submit to Job Manager
        switch (expiryAction) {
            case STOP: {
                logger.debug("Stopping instance: {} (id: {}) on lease expiry", instance.getName(), instance.getUuid());
                return executeStopInstanceJob(instance, eventId);
            }
            case DESTROY: {
                logger.debug("Destroying instance: {} (id: {}) on lease expiry", instance.getName(), instance.getUuid());
                return executeDestroyInstanceJob(instance, eventId);
            }
            default: {
                logger.error("Invalid configuration for instance.lease.expiryaction for instance: {} (id: {}), " +
                        "valid values are: \"STOP\" and  \"DESTROY\"", instance.getName(), instance.getUuid());
            }
        }
        return null;
    }

    long executeStopInstanceJob(UserVmJoinVO vm, long eventId) {
        final Map<String, String> params = new HashMap<>();
        params.put(ApiConstants.ID, String.valueOf(vm.getId()));
        params.put("ctxUserId", String.valueOf(User.UID_SYSTEM));
        params.put("ctxAccountId", String.valueOf(Account.ACCOUNT_ID_SYSTEM));
        params.put(ApiConstants.CTX_START_EVENT_ID, String.valueOf(eventId));
        params.put(JOB_INITIATOR, VMLeaseManager.class.getSimpleName());
        final StopVMCmd cmd = new StopVMCmd();
        ComponentContext.inject(cmd);
        AsyncJobVO job = new AsyncJobVO("", User.UID_SYSTEM, vm.getAccountId(), StopVMCmd.class.getName(), gson.toJson(params), vm.getId(),
                cmd.getApiResourceType() != null ? cmd.getApiResourceType().toString() : null, null);
        job.setDispatcher(asyncJobDispatcher.getName());
        return asyncJobManager.submitAsyncJob(job);
    }

    long executeDestroyInstanceJob(UserVmJoinVO vm, long eventId) {
        final Map<String, String> params = new HashMap<>();
        params.put(ApiConstants.ID, String.valueOf(vm.getId()));
        params.put("ctxUserId", String.valueOf(User.UID_SYSTEM));
        params.put("ctxAccountId", String.valueOf(Account.ACCOUNT_ID_SYSTEM));
        params.put(ApiConstants.CTX_START_EVENT_ID, String.valueOf(eventId));
        params.put(JOB_INITIATOR, VMLeaseManager.class.getSimpleName());
        final DestroyVMCmd cmd = new DestroyVMCmd();
        ComponentContext.inject(cmd);

        AsyncJobVO job = new AsyncJobVO("", User.UID_SYSTEM, vm.getAccountId(), DestroyVMCmd.class.getName(), gson.toJson(params), vm.getId(),
                cmd.getApiResourceType() != null ? cmd.getApiResourceType().toString() : null, null);
        job.setDispatcher(asyncJobDispatcher.getName());
        return asyncJobManager.submitAsyncJob(job);
    }

    public ExpiryAction getLeaseExpiryAction(UserVmJoinVO instance) {
        return EnumUtils.getEnumIgnoreCase(VMLeaseManager.ExpiryAction.class, instance.getLeaseExpiryAction());
    }

    private void addLeaseExpiryListener() {
        logger.debug("Adding Lease subscriber for async job events");
        if (this.leaseManagerSubscriber == null) {
            this.leaseManagerSubscriber = new VMLeaseManagerSubscriber();
        }
        messageBus.subscribe(AsyncJob.Topics.JOB_EVENT_PUBLISH, this.leaseManagerSubscriber);
    }

    private void removeLeaseExpiryListener() {
        logger.debug("Removing Lease subscriber for async job events");
        messageBus.unsubscribe(AsyncJob.Topics.JOB_EVENT_PUBLISH, this.leaseManagerSubscriber);
        this.leaseManagerSubscriber = null;
    }

    class VMLeaseManagerSubscriber implements MessageSubscriber {
        @Override
        public void onPublishMessage(String senderAddress, String subject, Object args) {
            try {
                @SuppressWarnings("unchecked")
                Pair<AsyncJob, String> eventInfo = (Pair<AsyncJob, String>) args;
                AsyncJob asyncExpiryJob = eventInfo.first();
                if (!"ApiAsyncJobDispatcher".equalsIgnoreCase(asyncExpiryJob.getDispatcher()) || !"complete".equalsIgnoreCase(eventInfo.second())) {
                    return;
                }
                String cmd = asyncExpiryJob.getCmd();
                if ((cmd.equalsIgnoreCase(StopVMCmd.class.getName()) || cmd.equalsIgnoreCase(DestroyVMCmd.class.getName()))
                        && asyncExpiryJob.getStatus() == JobInfo.Status.SUCCEEDED && asyncExpiryJob.getInstanceId() != null) {

                    Map<String, String> params = gson.fromJson(asyncExpiryJob.getCmdInfo(), new TypeToken<Map<String, String>>() {
                    }.getType());

                    if (VMLeaseManager.class.getSimpleName().equals(params.get(JOB_INITIATOR))) {
                        logger.debug("Lease expiry job: {} successfully executed for instanceId: {}", asyncExpiryJob.getId(), asyncExpiryJob.getInstanceId());
                        vmInstanceDetailsDao.addDetail(asyncExpiryJob.getInstanceId(), VmDetailConstants.INSTANCE_LEASE_EXECUTION, LeaseActionExecution.DONE.name(), false);
                    }
                }
            } catch (final Exception e) {
                logger.error("Caught exception while executing lease expiry job", e);
            }
        }
    }
}

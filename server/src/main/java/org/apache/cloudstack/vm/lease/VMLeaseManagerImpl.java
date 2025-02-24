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
import com.cloud.user.User;
import com.cloud.utils.StringUtils;
import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.command.user.vm.DestroyVMCmd;
import org.apache.cloudstack.api.command.user.vm.StopVMCmd;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.framework.jobs.AsyncJobDispatcher;
import org.apache.cloudstack.framework.jobs.AsyncJobManager;
import org.apache.cloudstack.framework.jobs.impl.AsyncJobVO;
import org.apache.cloudstack.managed.context.ManagedContextTimerTask;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import static com.cloud.vm.VirtualMachine.State.Destroyed;
import static com.cloud.vm.VirtualMachine.State.Expunging;
import static com.cloud.vm.VirtualMachine.State.Stopped;

public class VMLeaseManagerImpl extends ManagerBase implements VMLeaseManager, Configurable {

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
                InstanceLeaseSchedulerInterval
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

    private void alert() {
        List<UserVmJoinVO> leaseExpiringForInstances = userVmJoinDao.listExpiringInstancesInDays(InstanceLeaseAlertStartsAt.value().intValue());
        for (UserVmJoinVO instance : leaseExpiringForInstances) {
            alertManager.sendAlert(AlertManager.AlertType.ALERT_TYPE_USERVM, 0L, instance.getPodId(),
                    "Lease expiring for instance id: " + instance.getUuid(), "Lease expiring for instance");
        }
    }

    @Override
    public void poll(Date currentTimestamp) {
        // fetch user_instances having leaseDuration configured and has expired
        List<UserVmJoinVO> leaseExpiredInstances = userVmJoinDao.listExpiredInstancesIds();
        List<Long> actionableInstanceIds = new ArrayList<>();
        // iterate over them and ignore if delete protection is enabled
        for (UserVmJoinVO userVmVO : leaseExpiredInstances) {
            if (userVmVO.isDeleteProtection()) {
                logger.debug("Ignoring vm with id: {} as deleteProtection is enabled", userVmVO.getUuid());
                continue;
            }
            // state check, include instances not yet stopped or destroyed
            // it can be done in fetch_user_instances as well
            if (!Arrays.asList(Stopped, Destroyed, Expunging).contains(userVmVO.getState())) {
                actionableInstanceIds.add(userVmVO.getId());
            }
        }

        List<Long> submittedJobIds = new ArrayList<>();
        List<Long> failedToSubmitInstanceIds = new ArrayList<>();
        for (Long instanceId : actionableInstanceIds) {
            UserVmJoinVO instance = userVmJoinDao.findById(instanceId);
            ExpiryAction expiryAction = getLeaseExpiryAction(instance);
            Long jobId = executeExpiryAction(instance, expiryAction);
            if (jobId != null) {
                submittedJobIds.add(jobId);
            } else {
                failedToSubmitInstanceIds.add(instanceId);
            }
        }
        logger.debug("Successfully submitted jobs for ids: {}", submittedJobIds);
        if (!failedToSubmitInstanceIds.isEmpty()) {
            logger.debug("Lease scheduler failed to submit jobs for instance ids: {}", failedToSubmitInstanceIds);
        }
    }

    private Long executeExpiryAction(UserVmJoinVO instance, ExpiryAction expiryAction) {
        // for qualified vms, prepare Stop/Destroy(Cmd) and submit to Job Manager
        switch (expiryAction) {
            case STOP: {
                logger.debug("Stopping instance with id: {} on lease expiry", instance.getUuid());
                return executeStopInstanceJob(instance, true, 1);
            }
            case DESTROY: {
                logger.debug("Destroying instance with id: {} on lease expiry", instance.getUuid());
                return executeDestroyInstanceJob(instance, true, 2);
            }
            default: {
                logger.error("Invalid configuration for instance.lease.expiryaction for vm id: {}, " +
                        "valid values are: \"stop\" and  \"destroy\"", instance.getUuid());
            }
        }
        return null;
    }

    long executeStopInstanceJob(UserVmJoinVO vm, boolean isForced, long eventId) {
        final Map<String, String> params = new HashMap<>();
        params.put(ApiConstants.ID, String.valueOf(vm.getId()));
        params.put("ctxUserId", "1");
        params.put("ctxAccountId", String.valueOf(vm.getAccountId()));
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
        params.put("ctxUserId", "1");
        params.put("ctxAccountId", String.valueOf(vm.getAccountId()));
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

    private ExpiryAction getLeaseExpiryAction(UserVmJoinVO instance) {
        // find expiry action from VM and compute offering
        String action = instance.getLeaseExpiryAction();
        if (StringUtils.isNotEmpty(action)) {
            return ExpiryAction.valueOf(action);
        }
        throw new CloudRuntimeException("No expiry action configured for instance: " + instance.getUuid());
    }
}

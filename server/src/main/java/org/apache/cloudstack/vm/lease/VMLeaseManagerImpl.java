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
import com.cloud.api.ApiDBUtils;
import com.cloud.api.query.dao.UserVmJoinDao;
import com.cloud.api.query.vo.UserVmJoinVO;
import com.cloud.offering.ServiceOffering;
import com.cloud.utils.StringUtils;
import com.cloud.utils.component.ManagerBase;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.managed.context.ManagedContextTimerTask;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static com.cloud.vm.VirtualMachine.State.Destroyed;
import static com.cloud.vm.VirtualMachine.State.Expunging;
import static com.cloud.vm.VirtualMachine.State.Stopped;

public class VMLeaseManagerImpl extends ManagerBase implements VMLeaseManager, Configurable {
    Timer vmLeaseTimer;
    Timer vmLeaseAlterTimer;

    @Inject
    private UserVmJoinDao userVmJoinDao;

    @Inject
    private AlertManager alertManager;

    @Override
    public String getConfigComponentName() {
        return VMLeaseManager.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey[] {
                InstanceLeaseEnabled,
                InstanceLeaseDuration,
                InstanceLeaseExpiryAction,
                InstanceLeaseSchedulerInterval
        };
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
                    alert(new Date());
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

    private void alert(Date date) {
        List<UserVmJoinVO> leaseExpiringForInstances = fetchLeaseExpiredInstances(InstanceLeaseAlertStartsAt.value());
        for (UserVmJoinVO instance : leaseExpiringForInstances) {
            alertManager.sendAlert(AlertManager.AlertType.ALERT_TYPE_USERVM, 0L, instance.getPodId(),
                    "Lease expiring in 7 days", "Lease expiring");
        }
    }

    @Override
    public void poll(Date currentTimestamp) {
        // fetch user_instances having leaseDuration configured and has expired
        List<UserVmJoinVO> leaseExpiredInstances = fetchLeaseExpiredInstances(0L);
        List<Long> actionableInstanceIds = Arrays.asList(1L, 2L);
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

        for (Long instanceId : actionableInstanceIds) {
            UserVmJoinVO instance = userVmJoinDao.findById(instanceId);
            ExpiryAction expiryAction = getLeaseExpiryAction(instance);
            executeExpiryAction(instance, expiryAction);
        }
    }

    private List<UserVmJoinVO> fetchLeaseExpiredInstances(Long expiringInDays) {
        return userVmJoinDao.listExpiredInstances();
    }

    private void executeExpiryAction(UserVmJoinVO instance, ExpiryAction expiryAction) {
        // for qualified vms, prepare Stop/Destroy(Cmd) and submit to Job Manager
        switch (expiryAction) {
            case STOP: {
                logger.debug("Stopping instance");
                break;
            }
            case DESTROY: {
                logger.debug("Sending Destroy instance");
                break;
            }
            default: {
                logger.error("Invalid configuration for instance.lease.expiryaction for vm id: {}, " +
                        "valid values are: \"stop\" and  \"destroy\"", instance.getUuid());
            }
        }
    }

    private ExpiryAction getLeaseExpiryAction(UserVmJoinVO instance) {
        // find expiry action from VM and compute offering
        String action;
        action = instance.getName(); // ToDo: call new method to get expiryAction
        if (StringUtils.isNotEmpty(action)) {
            return ExpiryAction.valueOf(action);
        }

        ServiceOffering serviceOffering = ApiDBUtils.findServiceOfferingById(instance.getServiceOfferingId());
        action = serviceOffering.getName(); // ToDo: call new method to get correct expiryAction
        if (StringUtils.isNotEmpty(action)) {
            return ExpiryAction.valueOf(action);
        }

        // Find expiry Action configured in sequence Account -> Domain -> Global
        return ExpiryAction.valueOf(InstanceLeaseExpiryAction.valueIn(instance.getAccountId()));
    }
}

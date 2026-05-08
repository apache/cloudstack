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
package org.apache.cloudstack.schedule.vm;

import com.cloud.event.ActionEventUtils;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.user.User;
import com.cloud.vm.UserVmManager;
import com.cloud.vm.VirtualMachine;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.command.user.vm.RebootVMCmd;
import org.apache.cloudstack.api.command.user.vm.StartVMCmd;
import org.apache.cloudstack.api.command.user.vm.StopVMCmd;
import org.apache.cloudstack.schedule.BaseScheduleWorker;
import org.apache.cloudstack.schedule.ResourceSchedule;
import org.apache.cloudstack.schedule.ResourceScheduledJobVO;
import org.apache.commons.lang3.EnumUtils;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

public class VMScheduleWorker extends BaseScheduleWorker {

    @Inject
    private UserVmManager userVmManager;

    @Override
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.VirtualMachine;
    }

    @Override
    public boolean isResourceValid(long resourceId) {
        return userVmManager.getUserVm(resourceId) != null;
    }

    @Override
    public long getEntityOwnerId(long resourceId) {
        VirtualMachine vm = userVmManager.getUserVm(resourceId);
        return vm != null ? vm.getAccountId() : User.UID_SYSTEM;
    }

    @Override
    public VMScheduleAction parseAction(String actionName) {
        VMScheduleAction action = EnumUtils.getEnumIgnoreCase(VMScheduleAction.class, actionName);
        if (action == null) {
            throw new InvalidParameterValueException("Invalid action for VirtualMachine schedule: " + actionName +
                    ". Supported actions: " + Arrays.toString(VMScheduleAction.values()));
        }
        return action;
    }

    @Override
    public void validateDetails(ResourceSchedule.Action action, Map<String, String> details) {
        // No special details required/validated for VM schedules right now.
    }

    @Override
    protected Long processJob(ResourceScheduledJobVO job) {
        VirtualMachine vm = userVmManager.getUserVm(job.getResourceId());
        if (vm == null) {
            logger.warn("VM id={} not found; skipping scheduled job {}", job.getResourceId(), job);
            return null;
        }

        if (!Arrays.asList(VirtualMachine.State.Running, VirtualMachine.State.Stopped).contains(vm.getState())) {
            logger.info("Skipping action ({}) for [vm: {}, job: {}] — VM is in state: {}",
                    job.getActionName(), vm, job, vm.getState());
            return null;
        }

        VMScheduleAction action = VMScheduleAction.valueOf(job.getActionName());
        final long eventId = ActionEventUtils.onCompletedActionEvent(
                User.UID_SYSTEM, vm.getAccountId(), null,
                action.getEventType(), true,
                String.format("Executing action (%s) for VM: %s", action, vm),
                vm.getId(), ApiCommandResourceType.VirtualMachine.toString(), 0);

        if (vm.getState() == VirtualMachine.State.Running) {
            switch (action) {
                case STOP:        return submitStopVMJob(vm, false, eventId);
                case FORCE_STOP:  return submitStopVMJob(vm, true, eventId);
                case REBOOT:      return submitRebootVMJob(vm, false, eventId);
                case FORCE_REBOOT: return submitRebootVMJob(vm, true, eventId);
                default: break;
            }
        } else if (vm.getState() == VirtualMachine.State.Stopped && action == VMScheduleAction.START) {
            return submitStartVMJob(vm, eventId);
        }

        logger.warn("Skipping action ({}) for [vm: {}, job: {}] — VM is in state: {}",
                action, vm, job, vm.getState());
        return null;
    }

    private long submitStartVMJob(VirtualMachine vm, long eventId) {
        return submitAsyncJob(StartVMCmd.class, vm.getAccountId(), vm.getId(), eventId, Collections.emptyMap());
    }

    private long submitStopVMJob(VirtualMachine vm, boolean forced, long eventId) {
        return submitAsyncJob(StopVMCmd.class, vm.getAccountId(), vm.getId(), eventId,
                Map.of(ApiConstants.FORCED, String.valueOf(forced)));
    }

    private long submitRebootVMJob(VirtualMachine vm, boolean forced, long eventId) {
        return submitAsyncJob(RebootVMCmd.class, vm.getAccountId(), vm.getId(), eventId,
                Map.of(ApiConstants.FORCED, String.valueOf(forced)));
    }
}

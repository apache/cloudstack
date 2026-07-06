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

package org.apache.cloudstack.vm.bootgroup.readiness;

import java.util.Map;

import javax.inject.Inject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.CheckGuestAgentLivenessCommand;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.UserVmDao;

/**
 * GuestAgentLiveness: dispatched directly to the VM's hypervisor host rather than via the VR,
 * asking libvirt to relay a qemu-guest-agent "guest-ping" over the VM's virtio-serial channel.
 * Only supported on KVM, since that channel is a KVM/libvirt-specific mechanism.
 */
@Component
public class GuestAgentLivenessChecker implements ReadinessChecker {
    protected static Logger LOGGER = LogManager.getLogger(GuestAgentLivenessChecker.class);

    @Inject
    private UserVmDao userVmDao;

    @Inject
    private AgentManager agentManager;

    @Override
    public InstanceBootGroupReadinessRule.RuleType getRuleType() {
        return InstanceBootGroupReadinessRule.RuleType.GuestAgentLiveness;
    }

    @Override
    public Logger getLogger() {
        return LOGGER;
    }

    @Override
    public Result check(InstanceBootGroupReadinessRule rule, Map<String, String> details, long vmId, long remainingMs) {
        UserVmVO vm = userVmDao.findById(vmId);
        if (vm == null) {
            return logAndReturn(rule, vmId, new Result(InstanceBootGroupReadinessRule.Status.Error, "VM not found"));
        }

        LOGGER.debug("Checking guest agent liveness for {} due to rule {}", vm, rule);

        if (vm.getHypervisorType() != HypervisorType.KVM) {
            return logAndReturn(rule, vm, new Result(InstanceBootGroupReadinessRule.Status.Error,
                    "Guest agent liveness checks are only supported on KVM; VM's hypervisor is " + vm.getHypervisorType()));
        }
        if (vm.getState() != VirtualMachine.State.Running || vm.getHostId() == null) {
            return logAndReturn(rule, vm, new Result(InstanceBootGroupReadinessRule.Status.NotReady, "VM is not running"));
        }

        if (remainingMs < MIN_REMAINING_MS_TO_DISPATCH) {
            return logAndReturn(rule, vm, new Result(InstanceBootGroupReadinessRule.Status.Error,
                    "Insufficient time remaining in this attempt's budget (" + remainingMs + "ms) to dispatch a guest agent liveness check"));
        }

        CheckGuestAgentLivenessCommand command = new CheckGuestAgentLivenessCommand(vm.getInstanceName());
        command.setWait(computeWaitSeconds(remainingMs));
        LOGGER.debug("Dispatching guest agent liveness check for {} to host id {} with {}ms remaining budget", vm, vm.getHostId(), remainingMs);
        Answer answer;
        try {
            answer = agentManager.easySend(vm.getHostId(), command);
        } catch (Exception e) {
            return logAndReturn(rule, vm, new Result(InstanceBootGroupReadinessRule.Status.Error, "Failed to dispatch guest agent liveness check: " + e.getMessage()));
        }
        if (answer == null) {
            return logAndReturn(rule, vm, new Result(InstanceBootGroupReadinessRule.Status.Error, "No answer from the VM's host"));
        }
        if (answer.getResult()) {
            return logAndReturn(rule, vm, new Result(InstanceBootGroupReadinessRule.Status.Ready, "guest agent responded"));
        }
        return logAndReturn(rule, vm, new Result(InstanceBootGroupReadinessRule.Status.NotReady, "guest agent did not respond: " + answer.getDetails()));
    }
}

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

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.routing.NetworkElementCommand;
import com.cloud.network.Network;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.router.VirtualRouter;
import com.cloud.network.router.VpcVirtualNetworkApplianceManager;
import com.cloud.vm.NicVO;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.dao.NicDao;
import com.cloud.vm.dao.UserVmDao;

/**
 * PortCheck: dispatches a dedicated {@link InstanceReadinessCheckCommand} to the VR of
 * the VM's default network (same transport {@link VrPingChecker} uses), asking it to attempt a TCP
 * connect to the VM's default IP on the configured port. Only TCP is currently supported.
 */
@Component
public class PortCheckChecker implements ReadinessChecker {
    protected static Logger LOGGER = LogManager.getLogger(PortCheckChecker.class);

    private static final String PORT_KEY = "port";
    private static final String PROTOCOL_KEY = "protocol";

    @Inject
    private UserVmDao userVmDao;

    @Inject
    private NicDao nicDao;

    @Inject
    private NetworkDao networkDao;

    // VirtualNetworkApplianceManager itself is ambiguous (VirtualNetworkApplianceManagerImpl and
    // VpcVirtualNetworkApplianceManagerImpl both implement it) — inject the more specific
    // sub-interface, matching the convention used by VirtualRouterElement/VpcVirtualRouterElement.
    @Inject
    private VpcVirtualNetworkApplianceManager virtualNetworkApplianceManager;

    @Inject
    private VirtualMachineManager virtualMachineManager;

    @Inject
    private NetworkOrchestrationService networkOrchestrationService;

    @Inject
    private AgentManager agentManager;

    @Override
    public InstanceBootGroupReadinessRule.RuleType getRuleType() {
        return InstanceBootGroupReadinessRule.RuleType.PortCheck;
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

        LOGGER.debug("Checking port readiness for {} due to rule {}", vm, rule);

        String protocol = details == null ? null : details.get(PROTOCOL_KEY);
        if (StringUtils.isNotBlank(protocol) && !"tcp".equalsIgnoreCase(protocol)) {
            return logAndReturn(rule, vm, new Result(InstanceBootGroupReadinessRule.Status.Error, "Only tcp port checks are supported, got: " + protocol));
        }

        String portValue = details == null ? null : details.get(PORT_KEY);
        int port;
        try {
            port = Integer.parseInt(portValue);
            if (port < 1 || port > 65535) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            return logAndReturn(rule, vm, new Result(InstanceBootGroupReadinessRule.Status.Error, "Invalid or missing port detail: " + portValue));
        }

        NicVO nic = nicDao.findDefaultNicForVM(vmId);
        if (nic == null || StringUtils.isEmpty(nic.getIPv4Address())) {
            return logAndReturn(rule, vm, new Result(InstanceBootGroupReadinessRule.Status.Error, "VM has no default NIC/IPv4 address yet"));
        }

        NetworkVO network = networkDao.findById(nic.getNetworkId());
        if (network != null && Network.GuestType.L2.equals(network.getGuestType())) {
            return logAndReturn(rule, vm, new Result(InstanceBootGroupReadinessRule.Status.Error, "The VM's default network is an L2 network; there is no VR to check the port from"));
        }

        List<VirtualRouter> routers = virtualNetworkApplianceManager.getRoutersForNetwork(nic.getNetworkId());
        VirtualRouter router = routers.stream()
                .filter(r -> r.getState() == VirtualMachine.State.Running)
                .findFirst()
                .orElse(null);
        if (router == null || router.getHostId() == null) {
            return logAndReturn(rule, vm, new Result(InstanceBootGroupReadinessRule.Status.Error, "No running VR found for the VM's default network"));
        }

        InstanceReadinessCheckCommand command = new InstanceReadinessCheckCommand(nic.getIPv4Address(), port,
                virtualMachineManager.getExecuteInSequence(router.getHypervisorType()));
        Map<String, String> accessDetails = networkOrchestrationService.getSystemVMAccessDetails(router);
        if (StringUtils.isEmpty(accessDetails.get(NetworkElementCommand.ROUTER_IP))) {
            return logAndReturn(rule, vm, new Result(InstanceBootGroupReadinessRule.Status.Error, "Unable to determine the VR's control IP"));
        }
        command.setAccessDetail(accessDetails);

        if (remainingMs < MIN_REMAINING_MS_TO_DISPATCH) {
            return logAndReturn(rule, vm, new Result(InstanceBootGroupReadinessRule.Status.Error,
                    "Insufficient time remaining in this attempt's budget (" + remainingMs + "ms) to dispatch a port check"));
        }
        command.setWait(computeWaitSeconds(remainingMs));

        LOGGER.debug("Dispatching port check of {}:{} via {} with {}ms remaining budget", nic.getIPv4Address(), port, router, remainingMs);
        Answer answer;
        try {
            answer = agentManager.easySend(router.getHostId(), command);
        } catch (Exception e) {
            return logAndReturn(rule, vm, new Result(InstanceBootGroupReadinessRule.Status.Error, "Failed to dispatch port check via VR: " + e.getMessage()));
        }
        if (answer == null) {
            return logAndReturn(rule, vm, new Result(InstanceBootGroupReadinessRule.Status.Error, "No answer from the VR's host"));
        }

        Map<String, String> executionDetails = ((InstanceReadinessCheckAnswer) answer).getExecutionDetails();
        String exitCode = executionDetails.get(InstanceReadinessCheckAnswer.EXITCODE);
        if ("0".equals(exitCode)) {
            return logAndReturn(rule, vm, new Result(InstanceBootGroupReadinessRule.Status.Ready, "port " + port + "/tcp is open"));
        }
        return logAndReturn(rule, vm, new Result(InstanceBootGroupReadinessRule.Status.Error, "port " + port + "/tcp check failed: " + executionDetails.get(InstanceReadinessCheckAnswer.STDERR)));
    }
}

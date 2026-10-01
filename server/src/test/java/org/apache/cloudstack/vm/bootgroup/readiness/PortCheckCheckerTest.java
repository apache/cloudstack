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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.routing.NetworkElementCommand;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
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

@RunWith(MockitoJUnitRunner.class)
public class PortCheckCheckerTest {

    private static final long VM_ID = 100L;
    private static final long NETWORK_ID = 200L;
    private static final long HOST_ID = 300L;
    private static final String IP = "10.1.1.5";

    @InjectMocks
    PortCheckChecker checker;

    @Mock
    UserVmDao userVmDao;
    @Mock
    NicDao nicDao;
    @Mock
    NetworkDao networkDao;
    @Mock
    VpcVirtualNetworkApplianceManager virtualNetworkApplianceManager;
    @Mock
    VirtualMachineManager virtualMachineManager;
    @Mock
    NetworkOrchestrationService networkOrchestrationService;
    @Mock
    AgentManager agentManager;

    private UserVmVO vm;
    private VirtualRouter router;
    private Map<String, String> details;

    @Before
    public void setUp() {
        vm = mock(UserVmVO.class);
        when(userVmDao.findById(VM_ID)).thenReturn(vm);

        details = new HashMap<>();
        details.put("port", "80");
        details.put("protocol", "tcp");

        NicVO nic = mock(NicVO.class);
        when(nic.getIPv4Address()).thenReturn(IP);
        when(nic.getNetworkId()).thenReturn(NETWORK_ID);
        when(nicDao.findDefaultNicForVM(VM_ID)).thenReturn(nic);

        NetworkVO network = mock(NetworkVO.class);
        when(network.getGuestType()).thenReturn(Network.GuestType.Isolated);
        when(networkDao.findById(NETWORK_ID)).thenReturn(network);

        router = mock(VirtualRouter.class);
        when(router.getState()).thenReturn(VirtualMachine.State.Running);
        when(router.getHostId()).thenReturn(HOST_ID);
        when(router.getHypervisorType()).thenReturn(HypervisorType.KVM);
        when(virtualNetworkApplianceManager.getRoutersForNetwork(NETWORK_ID)).thenReturn(Collections.singletonList(router));

        when(virtualMachineManager.getExecuteInSequence(HypervisorType.KVM)).thenReturn(false);

        Map<String, String> accessDetails = new HashMap<>();
        accessDetails.put(NetworkElementCommand.ROUTER_IP, "10.1.1.1");
        when(networkOrchestrationService.getSystemVMAccessDetails(router)).thenReturn(accessDetails);
    }

    private InstanceBootGroupReadinessRule rule() {
        return mock(InstanceBootGroupReadinessRule.class);
    }

    @Test
    public void getRuleTypeIsPortCheck() {
        assertEquals(InstanceBootGroupReadinessRule.RuleType.PortCheck, checker.getRuleType());
    }

    @Test
    public void vmNotFoundIsError() {
        when(userVmDao.findById(VM_ID)).thenReturn(null);
        ReadinessChecker.Result result = checker.check(rule(), details, VM_ID, 60000);
        assertEquals(InstanceBootGroupReadinessRule.Status.Error, result.getStatus());
    }

    @Test
    public void nonTcpProtocolIsError() {
        details.put("protocol", "udp");
        ReadinessChecker.Result result = checker.check(rule(), details, VM_ID, 60000);
        assertEquals(InstanceBootGroupReadinessRule.Status.Error, result.getStatus());
        assertTrue(result.getMessage().contains("Only tcp"));
    }

    @Test
    public void blankProtocolDefaultsToTcp() {
        details.remove("protocol");
        when(agentManager.easySend(eq(HOST_ID), any())).thenReturn(readinessAnswer(true, "out&&err&&0"));
        ReadinessChecker.Result result = checker.check(rule(), details, VM_ID, 60000);
        assertEquals(InstanceBootGroupReadinessRule.Status.Ready, result.getStatus());
    }

    @Test
    public void missingPortIsError() {
        details.remove("port");
        ReadinessChecker.Result result = checker.check(rule(), details, VM_ID, 60000);
        assertEquals(InstanceBootGroupReadinessRule.Status.Error, result.getStatus());
        assertTrue(result.getMessage().contains("Invalid or missing port"));
    }

    @Test
    public void nonNumericPortIsError() {
        details.put("port", "not-a-number");
        ReadinessChecker.Result result = checker.check(rule(), details, VM_ID, 60000);
        assertEquals(InstanceBootGroupReadinessRule.Status.Error, result.getStatus());
    }

    @Test
    public void portOutOfRangeIsError() {
        details.put("port", "70000");
        ReadinessChecker.Result result = checker.check(rule(), details, VM_ID, 60000);
        assertEquals(InstanceBootGroupReadinessRule.Status.Error, result.getStatus());
    }

    @Test
    public void zeroPortIsError() {
        details.put("port", "0");
        ReadinessChecker.Result result = checker.check(rule(), details, VM_ID, 60000);
        assertEquals(InstanceBootGroupReadinessRule.Status.Error, result.getStatus());
    }

    @Test
    public void noDefaultNicIsError() {
        when(nicDao.findDefaultNicForVM(VM_ID)).thenReturn(null);
        ReadinessChecker.Result result = checker.check(rule(), details, VM_ID, 60000);
        assertEquals(InstanceBootGroupReadinessRule.Status.Error, result.getStatus());
    }

    @Test
    public void l2NetworkIsError() {
        NetworkVO l2Network = mock(NetworkVO.class);
        when(l2Network.getGuestType()).thenReturn(Network.GuestType.L2);
        when(networkDao.findById(NETWORK_ID)).thenReturn(l2Network);
        ReadinessChecker.Result result = checker.check(rule(), details, VM_ID, 60000);
        assertEquals(InstanceBootGroupReadinessRule.Status.Error, result.getStatus());
    }

    @Test
    public void noRunningRouterIsError() {
        when(virtualNetworkApplianceManager.getRoutersForNetwork(NETWORK_ID)).thenReturn(Collections.emptyList());
        ReadinessChecker.Result result = checker.check(rule(), details, VM_ID, 60000);
        assertEquals(InstanceBootGroupReadinessRule.Status.Error, result.getStatus());
    }

    @Test
    public void missingRouterControlIpIsError() {
        when(networkOrchestrationService.getSystemVMAccessDetails(router)).thenReturn(new HashMap<>());
        ReadinessChecker.Result result = checker.check(rule(), details, VM_ID, 60000);
        assertEquals(InstanceBootGroupReadinessRule.Status.Error, result.getStatus());
    }

    @Test
    public void insufficientBudgetIsError() {
        ReadinessChecker.Result result = checker.check(rule(), details, VM_ID, 500);
        assertEquals(InstanceBootGroupReadinessRule.Status.Error, result.getStatus());
    }

    @Test
    public void dispatchExceptionIsError() {
        when(agentManager.easySend(eq(HOST_ID), any())).thenThrow(new RuntimeException("agent down"));
        ReadinessChecker.Result result = checker.check(rule(), details, VM_ID, 60000);
        assertEquals(InstanceBootGroupReadinessRule.Status.Error, result.getStatus());
    }

    @Test
    public void nullAnswerIsError() {
        when(agentManager.easySend(eq(HOST_ID), any())).thenReturn(null);
        ReadinessChecker.Result result = checker.check(rule(), details, VM_ID, 60000);
        assertEquals(InstanceBootGroupReadinessRule.Status.Error, result.getStatus());
    }

    @Test
    public void exitCodeZeroIsReady() {
        when(agentManager.easySend(eq(HOST_ID), any())).thenReturn(readinessAnswer(true, "out&&err&&0"));
        ReadinessChecker.Result result = checker.check(rule(), details, VM_ID, 60000);
        assertEquals(InstanceBootGroupReadinessRule.Status.Ready, result.getStatus());
        assertEquals("port 80/tcp is open", result.getMessage());
    }

    @Test
    public void nonZeroExitCodeIsError() {
        when(agentManager.easySend(eq(HOST_ID), any())).thenReturn(readinessAnswer(true, "out&&connection refused&&1"));
        ReadinessChecker.Result result = checker.check(rule(), details, VM_ID, 60000);
        assertEquals(InstanceBootGroupReadinessRule.Status.Error, result.getStatus());
        assertTrue(result.getMessage().contains("connection refused"));
    }

    private InstanceReadinessCheckAnswer readinessAnswer(boolean result, String rawDetails) {
        InstanceReadinessCheckCommand cmd = new InstanceReadinessCheckCommand(IP, 80, false);
        return new InstanceReadinessCheckAnswer(cmd, result, rawDetails);
    }
}

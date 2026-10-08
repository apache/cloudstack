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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.CheckGuestAgentLivenessAnswer;
import com.cloud.agent.api.CheckGuestAgentLivenessCommand;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.UserVmDao;

@RunWith(MockitoJUnitRunner.class)
public class GuestAgentLivenessCheckerTest {

    private static final long VM_ID = 100L;
    private static final long HOST_ID = 300L;

    @InjectMocks
    GuestAgentLivenessChecker checker;

    @Mock
    UserVmDao userVmDao;
    @Mock
    AgentManager agentManager;

    private UserVmVO vm;

    @Before
    public void setUp() {
        vm = mock(UserVmVO.class);
        when(userVmDao.findById(VM_ID)).thenReturn(vm);
        when(vm.getHypervisorType()).thenReturn(HypervisorType.KVM);
        when(vm.getState()).thenReturn(VirtualMachine.State.Running);
        when(vm.getHostId()).thenReturn(HOST_ID);
    }

    private InstanceBootGroupReadinessRule rule() {
        return mock(InstanceBootGroupReadinessRule.class);
    }

    @Test
    public void getRuleTypeIsGuestAgentLiveness() {
        assertEquals(InstanceBootGroupReadinessRule.RuleType.GuestAgentLiveness, checker.getRuleType());
    }

    @Test
    public void vmNotFoundIsError() {
        when(userVmDao.findById(VM_ID)).thenReturn(null);
        ReadinessChecker.Result result = checker.check(rule(), null, VM_ID, 60000);
        assertEquals(InstanceBootGroupReadinessRule.Status.Error, result.getStatus());
    }

    @Test
    public void nonKvmHypervisorIsError() {
        when(vm.getHypervisorType()).thenReturn(HypervisorType.VMware);
        ReadinessChecker.Result result = checker.check(rule(), null, VM_ID, 60000);
        assertEquals(InstanceBootGroupReadinessRule.Status.Error, result.getStatus());
        assertTrue(result.getMessage().contains("only supported on KVM"));
    }

    @Test
    public void notRunningIsNotReady() {
        when(vm.getState()).thenReturn(VirtualMachine.State.Stopped);
        ReadinessChecker.Result result = checker.check(rule(), null, VM_ID, 60000);
        assertEquals(InstanceBootGroupReadinessRule.Status.NotReady, result.getStatus());
    }

    @Test
    public void nullHostIdIsNotReady() {
        when(vm.getHostId()).thenReturn(null);
        ReadinessChecker.Result result = checker.check(rule(), null, VM_ID, 60000);
        assertEquals(InstanceBootGroupReadinessRule.Status.NotReady, result.getStatus());
    }

    @Test
    public void insufficientBudgetIsError() {
        ReadinessChecker.Result result = checker.check(rule(), null, VM_ID, 500);
        assertEquals(InstanceBootGroupReadinessRule.Status.Error, result.getStatus());
    }

    @Test
    public void dispatchExceptionIsError() {
        when(agentManager.easySend(eq(HOST_ID), any())).thenThrow(new RuntimeException("agent down"));
        ReadinessChecker.Result result = checker.check(rule(), null, VM_ID, 60000);
        assertEquals(InstanceBootGroupReadinessRule.Status.Error, result.getStatus());
        assertTrue(result.getMessage().contains("agent down"));
    }

    @Test
    public void nullAnswerIsError() {
        when(agentManager.easySend(eq(HOST_ID), any())).thenReturn(null);
        ReadinessChecker.Result result = checker.check(rule(), null, VM_ID, 60000);
        assertEquals(InstanceBootGroupReadinessRule.Status.Error, result.getStatus());
    }

    @Test
    public void positiveAnswerIsReady() {
        Answer answer = mock(CheckGuestAgentLivenessAnswer.class);
        when(answer.getResult()).thenReturn(true);
        when(agentManager.easySend(eq(HOST_ID), any())).thenReturn(answer);

        ReadinessChecker.Result result = checker.check(rule(), null, VM_ID, 60000);
        assertEquals(InstanceBootGroupReadinessRule.Status.Ready, result.getStatus());
        assertEquals("guest agent responded", result.getMessage());
    }

    @Test
    public void negativeAnswerIsNotReady() {
        Answer answer = mock(CheckGuestAgentLivenessAnswer.class);
        when(answer.getResult()).thenReturn(false);
        when(answer.getDetails()).thenReturn("agent not connected");
        when(agentManager.easySend(eq(HOST_ID), any())).thenReturn(answer);

        ReadinessChecker.Result result = checker.check(rule(), null, VM_ID, 60000);
        assertEquals(InstanceBootGroupReadinessRule.Status.NotReady, result.getStatus());
        assertTrue(result.getMessage().contains("agent not connected"));
    }

    @Test
    public void waitIsHalvedRemainingBudgetFlooredAtOne() {
        Answer answer = mock(CheckGuestAgentLivenessAnswer.class);
        when(answer.getResult()).thenReturn(true);
        when(agentManager.easySend(eq(HOST_ID), any())).thenReturn(answer);
        ArgumentCaptor<CheckGuestAgentLivenessCommand> captor = ArgumentCaptor.forClass(CheckGuestAgentLivenessCommand.class);

        checker.check(rule(), null, VM_ID, 2500);

        verify(agentManager).easySend(eq(HOST_ID), captor.capture());
        assertEquals(1, captor.getValue().getWait());
    }
}

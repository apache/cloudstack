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
package org.apache.cloudstack.vm.bootgroup;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.cloudstack.vm.bootgroup.readiness.InstanceBootGroupReadinessRuleService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.utils.fsm.StateMachine2;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachine.Event;
import com.cloud.vm.VirtualMachine.State;

@RunWith(MockitoJUnitRunner.class)
public class InstanceBootGroupVmStateListenerTest {

    private static final long VM_ID = 100L;

    private InstanceBootGroupReadinessRuleService instanceBootGroupReadinessRuleService;
    private InstanceBootGroupVmStateListener listener;
    private VirtualMachine vm;

    @Before
    public void setUp() {
        instanceBootGroupReadinessRuleService = mock(InstanceBootGroupReadinessRuleService.class);
        listener = new InstanceBootGroupVmStateListener(instanceBootGroupReadinessRuleService);
        vm = mock(VirtualMachine.class);
        when(vm.getId()).thenReturn(VM_ID);
    }

    private StateMachine2.Transition<State, Event> transition(State from, Event event, State to) {
        return new StateMachine2.Transition<>(from, event, to, null);
    }

    @Test
    public void preStateTransitionEventAlwaysAllowsTheTransition() {
        assertTrue(listener.preStateTransitionEvent(State.Starting, Event.OperationSucceeded, State.Running, vm, true, null));
    }

    @Test
    public void invalidatesOnColdStart() {
        listener.postStateTransitionEvent(transition(State.Starting, Event.OperationSucceeded, State.Running), vm, true, null);
        verify(instanceBootGroupReadinessRuleService).invalidateCachedReadinessOnRestart(VM_ID);
    }

    @Test
    public void invalidatesOnColdStartConfirmedByAgentReport() {
        listener.postStateTransitionEvent(transition(State.Starting, Event.AgentReportRunning, State.Running), vm, true, null);
        verify(instanceBootGroupReadinessRuleService).invalidateCachedReadinessOnRestart(VM_ID);
    }

    @Test
    public void ignoresFailedTransitions() {
        listener.postStateTransitionEvent(transition(State.Starting, Event.OperationSucceeded, State.Running), vm, false, null);
        verify(instanceBootGroupReadinessRuleService, never()).invalidateCachedReadinessOnRestart(anyLong());
    }

    @Test
    public void ignoresSameStateConfirmation() {
        listener.postStateTransitionEvent(transition(State.Running, Event.AgentReportRunning, State.Running), vm, true, null);
        verify(instanceBootGroupReadinessRuleService, never()).invalidateCachedReadinessOnRestart(anyLong());
    }

    @Test
    public void ignoresMigrationLandingInRunning() {
        listener.postStateTransitionEvent(transition(State.Migrating, Event.OperationSucceeded, State.Running), vm, true, null);
        verify(instanceBootGroupReadinessRuleService, never()).invalidateCachedReadinessOnRestart(anyLong());
    }

    @Test
    public void ignoresTransitionsNotLandingInRunning() {
        listener.postStateTransitionEvent(transition(State.Running, Event.StopRequested, State.Stopping), vm, true, null);
        verify(instanceBootGroupReadinessRuleService, never()).invalidateCachedReadinessOnRestart(anyLong());
    }

    @Test
    public void exceptionFromServiceDoesNotPropagate() {
        doThrow(new RuntimeException("db down")).when(instanceBootGroupReadinessRuleService).invalidateCachedReadinessOnRestart(VM_ID);
        boolean result = listener.postStateTransitionEvent(transition(State.Starting, Event.OperationSucceeded, State.Running), vm, true, null);
        assertTrue(result);
    }
}

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
package com.cloud.agent.manager;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.CancelCommand;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.UnsupportedAnswer;
import com.cloud.exception.AgentUnavailableException;

import com.cloud.hypervisor.Hypervisor;
import com.cloud.utils.nio.Link;

public class ConnectedAgentAttacheTest {

    @Test
    public void testEquals() throws Exception {

        Link link = mock(Link.class);

        ConnectedAgentAttache agentAttache1 = new ConnectedAgentAttache(null, 0, "uuid", null, Hypervisor.HypervisorType.KVM, link, false);
        ConnectedAgentAttache agentAttache2 = new ConnectedAgentAttache(null, 0, "uuid", null, Hypervisor.HypervisorType.KVM,link, false);

        assertTrue(agentAttache1.equals(agentAttache2));
    }

    @Test
    public void testEqualsFalseNull() throws Exception {

        Link link = mock(Link.class);

        ConnectedAgentAttache agentAttache1 = new ConnectedAgentAttache(null, 0, "uuid", null, Hypervisor.HypervisorType.KVM, link, false);

        assertFalse(agentAttache1.equals(null));
    }

    @Test
    public void testEqualsFalseDiffLink() throws Exception {

        Link link1 = mock(Link.class);
        Link link2 = mock(Link.class);

        ConnectedAgentAttache agentAttache1 = new ConnectedAgentAttache(null, 0, "uuid", null, Hypervisor.HypervisorType.KVM, link1, false);
        ConnectedAgentAttache agentAttache2 = new ConnectedAgentAttache(null, 0, "uuid", null, Hypervisor.HypervisorType.KVM, link2, false);

        assertFalse(agentAttache1.equals(agentAttache2));
    }

    @Test
    public void testEqualsFalseDiffUuid() throws Exception {

        Link link1 = mock(Link.class);

        ConnectedAgentAttache agentAttache1 = new ConnectedAgentAttache(null, 1, "uuid1", null, Hypervisor.HypervisorType.KVM, link1, false);
        ConnectedAgentAttache agentAttache2 = new ConnectedAgentAttache(null, 2, "uuid2", null, Hypervisor.HypervisorType.KVM, link1, false);

        assertFalse(agentAttache1.equals(agentAttache2));
    }

    @Test
    public void testEqualsFalseDiffClass() throws Exception {

        Link link1 = mock(Link.class);

        ConnectedAgentAttache agentAttache1 = new ConnectedAgentAttache(null, 1, "uuid", null, Hypervisor.HypervisorType.KVM, link1, false);

        assertFalse(agentAttache1.equals("abc"));
    }

    @Test
    public void isExecutionCancellableAsksTheAgentWithoutCancelling() throws Exception {
        AgentManagerImpl agentMgr = mock(AgentManagerImpl.class);
        ConnectedAgentAttache attache = new ConnectedAgentAttache(agentMgr, 5L, "uuid", "host", Hypervisor.HypervisorType.KVM, mock(Link.class), false);
        when(agentMgr.send(Mockito.eq(5L), Mockito.any(Command.class))).thenReturn(new Answer(null, true, "can"));

        assertTrue(attache.isExecutionCancellable(77L));

        ArgumentCaptor<Command> sent = ArgumentCaptor.forClass(Command.class);
        verify(agentMgr).send(Mockito.eq(5L), sent.capture());
        CancelCommand cancel = (CancelCommand) sent.getValue();
        assertTrue(cancel.isCheckOnly());
        assertTrue(77L == cancel.getSequence());
        assertFalse(cancel.executeInSequence());
    }

    @Test
    public void cancelRunningSendsARealCancelAndReportsTheAgentsAnswer() throws Exception {
        AgentManagerImpl agentMgr = mock(AgentManagerImpl.class);
        ConnectedAgentAttache attache = new ConnectedAgentAttache(agentMgr, 5L, "uuid", "host", Hypervisor.HypervisorType.KVM, mock(Link.class), false);
        when(agentMgr.send(Mockito.eq(5L), Mockito.any(Command.class))).thenReturn(new Answer(null, false, "nothing to stop"));

        assertFalse(attache.cancelRunning(77L));

        ArgumentCaptor<Command> sent = ArgumentCaptor.forClass(Command.class);
        verify(agentMgr).send(Mockito.eq(5L), sent.capture());
        assertFalse(((CancelCommand) sent.getValue()).isCheckOnly());
    }

    @Test
    public void anAgentThatDoesNotUnderstandCancellationIsNotCancellable() throws Exception {
        // an older agent, or a resource with nothing to stop, answers unsupported; claiming it could
        // be cancelled would record a cancellation that never happened
        AgentManagerImpl agentMgr = mock(AgentManagerImpl.class);
        ConnectedAgentAttache attache = new ConnectedAgentAttache(agentMgr, 5L, "uuid", "host", Hypervisor.HypervisorType.KVM, mock(Link.class), false);
        when(agentMgr.send(Mockito.eq(5L), Mockito.any(Command.class))).thenReturn(new UnsupportedAnswer(null, "unsupported"));

        assertFalse(attache.isExecutionCancellable(77L));
        assertFalse(attache.cancelRunning(77L));
    }

    @Test
    public void anUnreachableAgentIsNotCancellable() throws Exception {
        AgentManagerImpl agentMgr = mock(AgentManagerImpl.class);
        ConnectedAgentAttache attache = new ConnectedAgentAttache(agentMgr, 5L, "uuid", "host", Hypervisor.HypervisorType.KVM, mock(Link.class), false);
        when(agentMgr.send(Mockito.eq(5L), Mockito.any(Command.class))).thenThrow(new AgentUnavailableException(5L));

        assertFalse(attache.isExecutionCancellable(77L));
    }

    @Test
    public void withoutAnAgentManagerNothingIsAsked() {
        ConnectedAgentAttache attache = new ConnectedAgentAttache(null, 5L, "uuid", "host", Hypervisor.HypervisorType.KVM, mock(Link.class), false);

        assertFalse(attache.isExecutionCancellable(77L));
        assertFalse(attache.cancelRunning(77L));
    }
}

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

import java.util.UUID;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.agent.Listener;
import com.cloud.agent.api.CheckHealthCommand;
import com.cloud.agent.transport.Request;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.resource.ServerResource;

@RunWith(MockitoJUnitRunner.class)
public class DirectAgentAttacheTest {
    @Mock
    private AgentManagerImpl _agentMgr;

    @Mock
    private ServerResource _resource;

    @Mock
    private ScheduledExecutorService directAgentPool;

    @Mock
    private Future<Object> runningTask;

    long _id = 0L;

    String _uuid = UUID.randomUUID().toString();

    private DirectAgentAttache directAgentAttache;

    @Before
    public void setup() {
        directAgentAttache = new DirectAgentAttache(_agentMgr, _id, _uuid, "myDirectAgentAttache", Hypervisor.HypervisorType.KVM, _resource, false);
    }

    @Test
    public void testPingTask() throws Exception {
        DirectAgentAttache.PingTask pt = directAgentAttache.new PingTask();
        Mockito.doReturn(2).when(_agentMgr).getDirectAgentThreadCap();
        pt.runInContext();
        Mockito.verify(_resource, Mockito.times(1)).getCurrentStatus(_id);
    }

    @Test
    public void testCancelQueuedTaskDropsItBeforeItReachesTheResource() throws Exception {
        // no free thread, so the task stays queued
        Mockito.doReturn(0).when(_agentMgr).getDirectAgentThreadCap();
        final Request request = newRequest(202L);

        directAgentAttache.send(request, null);
        Assert.assertEquals(1, directAgentAttache.tasks.size());
        Assert.assertTrue(directAgentAttache.isExecutionCancellable(202L));

        directAgentAttache.cancelExecution(202L);

        Assert.assertTrue(request.isCancelled());
        Assert.assertEquals(0, directAgentAttache.tasks.size());
        Mockito.verify(_resource, Mockito.never()).cancelRequestSequence(Mockito.anyLong());
    }

    @Test
    public void testCancelRunningTaskStopsTheResourceThenTheThread() throws Exception {
        final Request request = submitRunning(101L);
        directAgentAttache.registerListener(101L, waiter());
        Mockito.doReturn(true).when(_resource).isRequestSequenceCancellable(101L);
        Mockito.doReturn(true).when(_resource).cancelRequestSequence(101L);

        Assert.assertTrue(directAgentAttache.isExecutionCancellable(101L));
        Assert.assertTrue(directAgentAttache.cancelExecution(101L));

        Mockito.verify(_resource).cancelRequestSequence(101L);
        Mockito.verify(runningTask).cancel(true);
        Assert.assertTrue(request.isCancelled());
        Assert.assertFalse(directAgentAttache._waitForList.containsKey(101L));
    }

    @Test
    public void testCancelLeavesNonCancellableRunningTaskAlone() throws Exception {
        final Request request = submitRunning(303L);
        directAgentAttache.registerListener(303L, waiter());
        Mockito.doReturn(false).when(_resource).isRequestSequenceCancellable(303L);

        Assert.assertFalse(directAgentAttache.isExecutionCancellable(303L));
        Assert.assertFalse(directAgentAttache.cancelExecution(303L));

        Mockito.verify(_resource, Mockito.never()).cancelRequestSequence(Mockito.anyLong());
        Mockito.verify(runningTask, Mockito.never()).cancel(Mockito.anyBoolean());
        Assert.assertFalse(request.isCancelled());
        Assert.assertTrue(directAgentAttache._waitForList.containsKey(303L));
    }

    @Test
    public void testUnknownSequenceIsNotClaimedCancellable() {
        Assert.assertFalse(directAgentAttache.isExecutionCancellable(999L));
    }

    private Request newRequest(final long seq) {
        final Request request = new Request(_id, -1, new CheckHealthCommand(), false);
        request.setSequence(seq);
        return request;
    }

    private Listener waiter() {
        final Listener listener = Mockito.mock(Listener.class);
        Mockito.when(listener.getTimeout()).thenReturn(-1);
        return listener;
    }

    private Request submitRunning(final long seq) throws Exception {
        Mockito.doReturn(2).when(_agentMgr).getDirectAgentThreadCap();
        Mockito.doReturn(directAgentPool).when(_agentMgr).getDirectAgentPool();
        Mockito.doReturn(runningTask).when(directAgentPool).submit(Mockito.any(Runnable.class));
        final Request request = newRequest(seq);
        directAgentAttache.send(request, null);
        return request;
    }
}

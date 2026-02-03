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
//import java.util.concurrent.Future;
//
//import com.cloud.agent.api.Command;
//import com.cloud.agent.transport.Request;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.hypervisor.Hypervisor;
import com.cloud.resource.ServerResource;

@RunWith(MockitoJUnitRunner.class)
public class DirectAgentAttacheTest {
    @Mock
    private AgentManagerImpl _agentMgr;

    @Mock
    private ServerResource _resource;

//    @Mock
//    private ExecutorService directAgentPool;

    long _id = 0L;

    String _uuid = UUID.randomUUID().toString();

    @Before
    public void setup() {
//        Mockito.when(_agentMgr.getDirectAgentPool()).thenReturn(directAgentPool);
        Mockito.doReturn(2).when(_agentMgr).getDirectAgentThreadCap();
        directAgentAttache = new DirectAgentAttache(_agentMgr, _id, _uuid, "myDirectAgentAttache", Hypervisor.HypervisorType.KVM, _resource, false);
    }
    private DirectAgentAttache directAgentAttache;

    @Test
    public void testPingTask() throws Exception {
        DirectAgentAttache.PingTask pt = directAgentAttache.new PingTask();
        pt.runInContext();
        Mockito.verify(_resource, Mockito.times(1)).getCurrentStatus(_id);
    }

//    @Test
//    public void testCancelRunningTaskCancelsFutureAndRequest() throws Exception {
//        final Command command = Mockito.mock(Command.class);
//        final Request request = new Request(_id, -1, command, false);
//        final long seq = 101L;
//        request.setSequence(seq);
//
//        @SuppressWarnings("unchecked")
//        final Future<Object> runningFuture = Mockito.mock(Future.class);
//        Mockito.when(directAgentPool.submit(Mockito.any(Runnable.class))).thenReturn(runningFuture);
//
//        directAgentAttache.send(request);
//        directAgentAttache.cancel(seq);
//
//        Mockito.verify(runningFuture, Mockito.times(1)).cancel(true);
//        org.junit.Assert.assertTrue(request.isCancelled());
//    }
//
//    @Test
//    public void testCancelQueuedTaskMarksRequestCancelledAndRemovesItFromQueue() throws Exception {
//        final Command command = Mockito.mock(Command.class);
//        final Request request = new Request(_id, -1, command, false);
//        final long seq = 202L;
//        request.setSequence(seq);
//
//        Mockito.doReturn(0).when(_agentMgr).getDirectAgentThreadCap();
//
//        directAgentAttache.send(request);
//        org.junit.Assert.assertEquals(1, directAgentAttache.tasks.size());
//
//        directAgentAttache.cancel(seq);
//
//        org.junit.Assert.assertTrue(request.isCancelled());
//        org.junit.Assert.assertEquals(0, directAgentAttache.tasks.size());
//    }
}

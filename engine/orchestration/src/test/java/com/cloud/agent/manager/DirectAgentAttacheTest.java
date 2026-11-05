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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;

import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.StartupAnswer;
import com.cloud.host.Status;
import com.cloud.resource.ServerResource;

@RunWith(MockitoJUnitRunner.class)
public class DirectAgentAttacheTest {
    @Mock
    private AgentManagerImpl _agentMgr;

    @Mock
    private ServerResource _resource;

    @Mock
    private ScheduledExecutorService _cronJobPool;

    @Mock
    private ScheduledFuture<?> _future;

    long _id = 0L;

    String _uuid = UUID.randomUUID().toString();

    @Before
    public void setup() {
        directAgentAttache = new DirectAgentAttache(_agentMgr, _id, _uuid, "myDirectAgentAttache", _resource, false);

        MockitoAnnotations.initMocks(directAgentAttache);
    }
    private DirectAgentAttache directAgentAttache;

    @Test
    public void testPingTask() throws Exception {
        DirectAgentAttache.PingTask pt = directAgentAttache.new PingTask();
        Mockito.doReturn(2).when(_agentMgr).getDirectAgentThreadCap();
        pt.runInContext();
        Mockito.verify(_resource, Mockito.times(1)).getCurrentStatus(_id);
    }

    @Test
    public void testProcessSchedulesPingWhenConnected() {
        Mockito.doReturn(_cronJobPool).when(_agentMgr).getCronJobPool();
        Mockito.doReturn(_future).when(_cronJobPool).scheduleWithFixedDelay(any(Runnable.class), anyLong(), anyLong(), any(TimeUnit.class));

        directAgentAttache.process(new Answer[] {buildStartupAnswer()});

        Mockito.verify(_cronJobPool, Mockito.times(1)).scheduleWithFixedDelay(any(Runnable.class), eq(60L), eq(60L), eq(TimeUnit.SECONDS));
    }

    @Test
    public void testProcessDoesNotSchedulePingAfterDisconnect() {
        // Once disconnect() has cleared the resource, a late StartupAnswer must not schedule
        // a PingTask - otherwise the future would never be cancelled (the leak this fix targets).
        directAgentAttache.disconnect(Status.Disconnected);

        directAgentAttache.process(new Answer[] {buildStartupAnswer()});

        Mockito.verify(_cronJobPool, Mockito.never()).scheduleWithFixedDelay(any(Runnable.class), anyLong(), anyLong(), any(TimeUnit.class));
    }

    private StartupAnswer buildStartupAnswer() {
        StartupAnswer startup = Mockito.mock(StartupAnswer.class);
        Mockito.doReturn(60).when(startup).getPingInterval();
        return startup;
    }
}

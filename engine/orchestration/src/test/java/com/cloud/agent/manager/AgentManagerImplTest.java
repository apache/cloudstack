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

import com.cloud.agent.Listener;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.ReadyCommand;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.StartupRoutingCommand;
import com.cloud.exception.ConnectionException;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.dao.HostDao;
import com.cloud.utils.Pair;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;

public class AgentManagerImplTest {

    private HostDao hostDao;
    private Listener storagePoolMonitor;
    private AgentAttache attache;
    private AgentManagerImpl mgr = Mockito.spy(new AgentManagerImpl());
    private HostVO host;
    private StartupCommand[] cmds;

    @Before
    public void setUp() throws Exception {
        host = new HostVO("some-Uuid");
        host.setDataCenterId(1L);
        cmds = new StartupCommand[]{new StartupRoutingCommand()};
        attache = new ConnectedAgentAttache(null, 1L, "kvm-attache", null, false);

        hostDao = Mockito.mock(HostDao.class);
        storagePoolMonitor = Mockito.mock(Listener.class);

        mgr._hostDao = hostDao;
        mgr._hostMonitors = new ArrayList<>();
        mgr._hostMonitors.add(new Pair<>(0, storagePoolMonitor));
    }

    @Test
    public void testNotifyMonitorsOfConnectionNormal() throws ConnectionException {
        Mockito.when(hostDao.findById(Mockito.anyLong())).thenReturn(host);
        Mockito.doNothing().when(storagePoolMonitor).processConnect(Mockito.eq(host), Mockito.eq(cmds[0]), Mockito.eq(false));
        Mockito.doReturn(true).when(mgr).handleDisconnectWithoutInvestigation(Mockito.any(attache.getClass()), Mockito.any(Status.Event.class), Mockito.anyBoolean(), Mockito.anyBoolean());
        Mockito.doReturn(Mockito.mock(Answer.class)).when(mgr).easySend(Mockito.anyLong(), Mockito.any(ReadyCommand.class));
        Mockito.doReturn(true).when(mgr).agentStatusTransitTo(Mockito.eq(host), Mockito.eq(Status.Event.Ready), Mockito.anyLong());

        final AgentAttache agentAttache = mgr.notifyMonitorsOfConnection(attache, cmds, false);
        Assert.assertTrue(agentAttache.isReady()); // Agent is in UP state
    }

    @Test
    public void testNotifyMonitorsOfConnectionWhenStoragePoolConnectionHostFailure() throws ConnectionException {
        ConnectionException connectionException = new ConnectionException(true, "storage pool could not be connected on host");
        Mockito.when(hostDao.findById(Mockito.anyLong())).thenReturn(host);
        Mockito.doThrow(connectionException).when(storagePoolMonitor).processConnect(Mockito.eq(host), Mockito.eq(cmds[0]), Mockito.eq(false));
        Mockito.doReturn(true).when(mgr).handleDisconnectWithoutInvestigation(Mockito.any(attache.getClass()), Mockito.any(Status.Event.class), Mockito.anyBoolean(), Mockito.anyBoolean());
        try {
            mgr.notifyMonitorsOfConnection(attache, cmds, false);
            Assert.fail("Connection Exception was expected");
        } catch (ConnectionException e) {
            Assert.assertEquals(e.getMessage(), connectionException.getMessage());
        }
        Mockito.verify(mgr, Mockito.times(1)).handleDisconnectWithoutInvestigation(Mockito.any(attache.getClass()), Mockito.eq(Status.Event.AgentDisconnected), Mockito.eq(true), Mockito.eq(true));
    }
}

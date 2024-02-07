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
import org.apache.cloudstack.framework.config.dao.CommandTimeoutDao;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class AgentManagerImplTest {

    @Mock
    private HostDao hostDaoMock;

    @Mock
    private Listener storagePoolMonitor;

    @Mock
    private Commands commandsMock;

    @Mock
    private CommandTimeoutDao commandTimeoutDaoMock;

    @InjectMocks
    @Spy
    private AgentManagerImpl agentManagerImplSpy;

    private HostVO host;
    private StartupCommand[] cmds = new StartupCommand[]{ new StartupRoutingCommand() };
    private AgentAttache attache = new ConnectedAgentAttache(null, 1L, "kvm-attache", null, false);

    @Before
    public void setUp() {
        host = new HostVO("some-Uuid");
        host.setDataCenterId(1L);

        agentManagerImplSpy._hostMonitors = List.of(new Pair<>(0, storagePoolMonitor));
    }

    @Test
    public void testNotifyMonitorsOfConnectionNormal() throws ConnectionException {
        Mockito.when(hostDaoMock.findById(Mockito.anyLong())).thenReturn(host);
        Mockito.doNothing().when(storagePoolMonitor).processConnect(Mockito.eq(host), Mockito.eq(cmds[0]), Mockito.eq(false));
        Mockito.doReturn(true).when(agentManagerImplSpy).handleDisconnectWithoutInvestigation(Mockito.any(attache.getClass()), Mockito.any(Status.Event.class), Mockito.anyBoolean(), Mockito.anyBoolean());
        Mockito.doReturn(Mockito.mock(Answer.class)).when(agentManagerImplSpy).easySend(Mockito.anyLong(), Mockito.any(ReadyCommand.class));
        Mockito.doReturn(true).when(agentManagerImplSpy).agentStatusTransitTo(Mockito.eq(host), Mockito.eq(Status.Event.Ready), Mockito.anyLong());

        final AgentAttache agentAttache = agentManagerImplSpy.notifyMonitorsOfConnection(attache, cmds, false);
        Assert.assertTrue(agentAttache.isReady()); // Agent is in UP state
    }

    @Test
    public void testNotifyMonitorsOfConnectionWhenStoragePoolConnectionHostFailure() throws ConnectionException {
        ConnectionException connectionException = new ConnectionException(true, "storage pool could not be connected on host");
        Mockito.when(hostDaoMock.findById(Mockito.anyLong())).thenReturn(host);
        Mockito.doThrow(connectionException).when(storagePoolMonitor).processConnect(Mockito.eq(host), Mockito.eq(cmds[0]), Mockito.eq(false));
        Mockito.doReturn(true).when(agentManagerImplSpy).handleDisconnectWithoutInvestigation(Mockito.any(attache.getClass()), Mockito.any(Status.Event.class), Mockito.anyBoolean(), Mockito.anyBoolean());
        try {
            agentManagerImplSpy.notifyMonitorsOfConnection(attache, cmds, false);
            Assert.fail("Connection Exception was expected");
        } catch (ConnectionException e) {
            Assert.assertEquals(e.getMessage(), connectionException.getMessage());
        }
        Mockito.verify(agentManagerImplSpy, Mockito.times(1)).handleDisconnectWithoutInvestigation(Mockito.any(attache.getClass()), Mockito.eq(Status.Event.AgentDisconnected), Mockito.eq(true), Mockito.eq(true));
    }

    @Test
    public void getTimeoutForCommandsTestReturnPassedTimeout() {
        int expected = 42;
        int result = agentManagerImplSpy.getTimeoutForCommands(commandsMock, expected);

        Assert.assertEquals(expected, result);
    }

    @Test
    public void getTimeoutForCommandsTestReturnTimeoutFromTable() {
        int expected = 42;

        Mockito.doReturn(expected).when(commandTimeoutDaoMock).findMaxTimeoutBetweenCommands(Mockito.any());
        int result = agentManagerImplSpy.getTimeoutForCommands(commandsMock, 0);

        Assert.assertEquals(expected, result);
    }

    @Test
    public void getTimeoutForCommandsTestFallbackToConfiguration() {
        int expected = 42;

        Mockito.doReturn(0).when(commandTimeoutDaoMock).findMaxTimeoutBetweenCommands(Mockito.any());
        Mockito.doReturn(expected).when(agentManagerImplSpy).getWaitValue();
        int result = agentManagerImplSpy.getTimeoutForCommands(commandsMock, 0);

        Assert.assertEquals(expected, result);
    }
}

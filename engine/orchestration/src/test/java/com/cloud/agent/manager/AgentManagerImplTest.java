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
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.ConnectionException;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.utils.Pair;
import com.cloud.utils.nio.Link;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class AgentManagerImplTest {

    private static final Long HOST_ID = 1L;
    private static final String HOST_UUID = UUID.randomUUID().toString();
    private static final String HOST_NAME = "test-host-name";
    private static final Link LINK = new Link(null, null);
    private static final Status.Event EVENT = Status.Event.AgentDisconnected;

    private HostDao hostDao;
    private Listener storagePoolMonitor;
    private AgentAttache attache;
    private AgentManagerImpl mgr = Mockito.spy(new AgentManagerImpl());
    private HostVO host;
    private StartupCommand[] cmds;

    @Before
    public void setUp() throws Exception {
        host = new HostVO("some-Uuid");
        FieldUtils.writeField(host, "id", HOST_ID, true);
        host.setDataCenterId(1L);
        cmds = new StartupCommand[]{new StartupRoutingCommand()};
        attache = createClusterAttache(false);

        hostDao = Mockito.mock(HostDao.class);
        storagePoolMonitor = Mockito.mock(Listener.class);

        mgr._hostDao = hostDao;
        mgr._hostMonitors = new ArrayList<>();
        mgr._hostMonitors.add(new Pair<>(0, storagePoolMonitor));
    }

    private AgentAttache createClusterAttache(boolean forForward) {
        Link link = forForward ? null : LINK;
        return new ClusteredAgentAttache(mgr, HOST_ID, HOST_UUID, HOST_NAME, Hypervisor.HypervisorType.KVM, link, false);
    }

    @Test
    public void testNotifyMonitorsOfConnectionNormal() throws ConnectionException {
        when(hostDao.findById(Mockito.anyLong())).thenReturn(host);
        Mockito.doNothing().when(storagePoolMonitor).processConnect(eq(host), eq(cmds[0]), eq(false));
        Mockito.doReturn(true).when(mgr).handleDisconnectWithoutInvestigation(Mockito.any(attache.getClass()), Mockito.any(Status.Event.class), Mockito.anyBoolean(), Mockito.anyBoolean());
        Mockito.doReturn(Mockito.mock(Answer.class)).when(mgr).easySend(Mockito.anyLong(), Mockito.any(ReadyCommand.class));
        Mockito.doReturn(true).when(mgr).agentStatusTransitTo(eq(host), eq(Status.Event.Ready), Mockito.anyLong());

        final AgentAttache agentAttache = mgr.notifyMonitorsOfConnection(attache, cmds, false);
        Assert.assertTrue(agentAttache.isReady()); // Agent is in UP state
    }

    @Test
    public void testNotifyMonitorsOfConnectionWhenStoragePoolConnectionHostFailure() throws ConnectionException {
        ConnectionException connectionException = new ConnectionException(true, "storage pool could not be connected on host");
        when(hostDao.findById(Mockito.anyLong())).thenReturn(host);
        Mockito.doThrow(connectionException).when(storagePoolMonitor).processConnect(eq(host), eq(cmds[0]), eq(false));
        Mockito.doReturn(true).when(mgr).handleDisconnectWithoutInvestigation(Mockito.any(attache.getClass()), Mockito.any(Status.Event.class), Mockito.anyBoolean(), Mockito.anyBoolean());
        try {
            mgr.notifyMonitorsOfConnection(attache, cmds, false);
            Assert.fail("Connection Exception was expected");
        } catch (ConnectionException e) {
            Assert.assertEquals(e.getMessage(), connectionException.getMessage());
        }
        Mockito.verify(mgr, Mockito.times(1)).handleDisconnectWithoutInvestigation(Mockito.any(attache.getClass()), eq(Status.Event.AgentDisconnected), eq(true), eq(true));
    }

    @Test
    public void testGetTimeoutWithPositiveTimeout() {
        Commands commands = Mockito.mock(Commands.class);
        int timeout = 30;
        int result = mgr.getTimeout(commands, timeout);

        Assert.assertEquals(30, result);
    }

    @Test
    public void testGetTimeoutWithGranularTimeout() {
        Commands commands = Mockito.mock(Commands.class);
        Mockito.doReturn(50).when(mgr).getTimeoutFromGranularWaitTime(commands);

        int timeout = 0;
        int result = mgr.getTimeout(commands, timeout);

        Assert.assertEquals(50, result);
    }

    @Test
    public void testAliveHostStatusesConfigKey() {
        ConfigKey<String> configKey = mgr.AliveHostStatuses;
        Assert.assertNotNull("Config key should not be null", configKey);
        Assert.assertEquals("Config key should have correct key name", "alive.host.statuses", configKey.key());
        Assert.assertEquals("Config key should have correct default value", "Up,Creating,Connecting,Rebalancing", configKey.defaultValue());
        Assert.assertEquals("Config key should have correct category", "Advanced", configKey.category());
    }

    @Test
    public void testGetConfigKeysIncludesAliveHostStatuses() {
        AgentManagerImpl agentManager = new AgentManagerImpl();
        ConfigKey<?>[] configKeys = agentManager.getConfigKeys();
        boolean found = false;
        for (ConfigKey<?> configKey : configKeys) {
            if ("alive.host.statuses".equals(configKey.key())) {
                found = true;
                break;
            }
        }
        Assert.assertTrue("AliveHostStatuses should be included in getConfigKeys()", found);
    }

    @Test(expected = AgentUnavailableException.class)
    public void testHandleDeregisterAttacheInAlertState() throws Exception {
        FieldUtils.writeField(host, "status", Status.Alert, true);
        attache = createClusterAttache(true);
        when(hostDao.findById(eq(HOST_ID))).thenReturn(host);

        boolean result = mgr.handleDeregisterAttache(attache, EVENT);
        Assert.assertTrue(result);

        mgr.getAttache(HOST_ID);
    }

    @Test
    public void testHandleDeregisterAttacheInAlertStateAnotherAttacheIsPresent() throws Exception {
        FieldUtils.writeField(host, "status", Status.Alert, true);
        attache = createClusterAttache(true);
        when(hostDao.findById(eq(HOST_ID))).thenReturn(host);

        AgentAttache preExistingAttache = mgr.createAttacheForConnect(host, LINK);

        boolean result = mgr.handleDeregisterAttache(attache, EVENT);
        Assert.assertTrue(result);

        // ensure pre-existing attache remains in place
        AgentAttache testAttache = mgr.getAttache(HOST_ID);
        Assert.assertEquals(preExistingAttache, testAttache);
    }

    @Test
    public void testHandleDeregisterAttacheInUpState() throws IllegalAccessException {
        FieldUtils.writeField(host, "status", Status.Up, true);
        attache = createClusterAttache(true);
        when(hostDao.findById(eq(HOST_ID))).thenReturn(host);

        boolean result = mgr.handleDeregisterAttache(attache, EVENT);
        Assert.assertTrue(result);
    }

    @Test
    public void testHandleDeregisterAttacheHostDoesNotExist() {
        attache = createClusterAttache(true);
        when(hostDao.findById(eq(HOST_ID))).thenReturn(null);

        boolean result = mgr.handleDeregisterAttache(attache, EVENT);
        Assert.assertTrue(result);
    }

    @Test
    public void testHandleDeregisterAttacheNotForward() {
        attache = createClusterAttache(false);
        boolean result = mgr.handleDeregisterAttache(attache, EVENT);
        Assert.assertFalse(result);
    }

    @Test
    public void testGetAliveHostStatusesDefaultConfiguration() throws Exception {
        ConfigKey<String> mockAliveHostStatuses = Mockito.mock(ConfigKey.class);
        when(mockAliveHostStatuses.value()).thenReturn("Up");

        FieldUtils.writeField(mgr, "AliveHostStatuses", mockAliveHostStatuses, true);

        Method initializeMethod = AgentManagerImpl.class.getDeclaredMethod("initializeAliveHostStatuses");
        initializeMethod.setAccessible(true);
        initializeMethod.invoke(mgr);

        Method getAliveHostStatusesMethod = AgentManagerImpl.class.getDeclaredMethod("getAliveHostStatuses");
        getAliveHostStatusesMethod.setAccessible(true);

        @SuppressWarnings("unchecked")
        Set<Status> result = (Set<Status>) getAliveHostStatusesMethod.invoke(mgr);

        Assert.assertEquals(1, result.size());
        Assert.assertTrue(result.contains(Status.Up));
    }

    @Test
    public void testGetAliveHostStatusesExpandedConfiguration() throws Exception {
        ConfigKey<String> mockAliveHostStatuses = Mockito.mock(ConfigKey.class);
        when(mockAliveHostStatuses.value()).thenReturn("Up,Alert,Connecting,Creating,Rebalancing");
        FieldUtils.writeField(mgr, "AliveHostStatuses", mockAliveHostStatuses, true);

        Method initializeMethod = AgentManagerImpl.class.getDeclaredMethod("initializeAliveHostStatuses");
        initializeMethod.setAccessible(true);
        initializeMethod.invoke(mgr);

        Method getAliveHostStatusesMethod = AgentManagerImpl.class.getDeclaredMethod("getAliveHostStatuses");
        getAliveHostStatusesMethod.setAccessible(true);

        @SuppressWarnings("unchecked")
        Set<Status> result = (Set<Status>) getAliveHostStatusesMethod.invoke(mgr);

        Assert.assertEquals(5, result.size());
        Assert.assertTrue(result.contains(Status.Up));
        Assert.assertTrue(result.contains(Status.Alert));
        Assert.assertTrue(result.contains(Status.Connecting));
        Assert.assertTrue(result.contains(Status.Creating));
        Assert.assertTrue(result.contains(Status.Rebalancing));
    }

    @Test
    public void testGetHostSshPortWithHostNull() {
        int hostSshPort = mgr.getHostSshPort(null);
        Assert.assertEquals(22, hostSshPort);
    }

    @Test
    public void testGetHostSshPortWithNonKVMHost() {
        HostVO host = Mockito.mock(HostVO.class);
        when(host.getHypervisorType()).thenReturn(Hypervisor.HypervisorType.XenServer);
        int hostSshPort = mgr.getHostSshPort(host);
        Assert.assertEquals(22, hostSshPort);
    }

    @Test
    public void testGetHostSshPortWithKVMHostDefaultPort() {
        HostVO host = Mockito.mock(HostVO.class);
        when(host.getHypervisorType()).thenReturn(Hypervisor.HypervisorType.KVM);
        when(host.getClusterId()).thenReturn(1L);
        int hostSshPort = mgr.getHostSshPort(host);
        Assert.assertEquals(22, hostSshPort);
    }

    @Test
    public void testGetHostSshPortWithKVMHostCustomPort() {
        HostVO host = Mockito.mock(HostVO.class);
        when(host.getHypervisorType()).thenReturn(Hypervisor.HypervisorType.KVM);
        when(host.getDetail(Host.HOST_SSH_PORT)).thenReturn(String.valueOf(3922));
        int hostSshPort = mgr.getHostSshPort(host);
        Assert.assertEquals(3922, hostSshPort);
    }
}

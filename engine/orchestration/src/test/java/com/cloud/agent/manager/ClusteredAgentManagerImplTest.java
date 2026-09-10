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

import com.cloud.configuration.ManagementServiceConfiguration;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.ha.HighAvailabilityManagerImpl;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.dao.HostDao;
import com.cloud.resource.ResourceManagerImpl;
import com.cloud.utils.db.GlobalLock;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ClusteredAgentManagerImplTest {
    private static final Long HOST_ID = 1L;
    private static final Status.Event EVENT = Status.Event.AgentDisconnected;

    @Mock
    ManagementServiceConfiguration mgmtServiceConf;

    @Spy
    private ClusteredAgentManagerImpl mgr = new ClusteredAgentManagerImpl();
    @Mock
    private HostDao hostDao;
    @Mock
    private ConfigKey<Boolean> PerformFullDisconnectOnAgentDisconnectEventBroadcast;
    private GlobalLock hostJoinLockMock;
    private HostVO host;

    MockedStatic<GlobalLock> globalLockMocked;

    @Before
    public void setUp() throws Exception {
        host = new HostVO("some-Uuid");
        FieldUtils.writeField(host, "id", HOST_ID, true);

        mgr._hostDao = hostDao;
        mgr.PerformFullDisconnectOnAgentDisconnectEventBroadcast = PerformFullDisconnectOnAgentDisconnectEventBroadcast;

        mgr.createAttache(host);
        globalLockMocked = Mockito.mockStatic(GlobalLock.class);
        hostJoinLockMock = mock(GlobalLock.class);
    }

    @After
    public void tearDown() throws Exception {
        globalLockMocked.close();
    }

    @Test
    public void testFullDisconnectPerformed() throws AgentUnavailableException {
        when(PerformFullDisconnectOnAgentDisconnectEventBroadcast.value()).thenReturn(Boolean.TRUE);
        globalLockMocked.when(() -> GlobalLock.getInternLock(anyString())).thenReturn(hostJoinLockMock);
        when(hostJoinLockMock.lock(anyInt())).thenReturn(Boolean.TRUE);

        boolean result = mgr.executeUserRequest(HOST_ID, EVENT);
        Assert.assertTrue(result);

        GlobalLock.getInternLock(anyString());

        verify(hostJoinLockMock, times(1)).lock(anyInt());
    }

    @Test
    public void testDeregisterOnlyPerformed() throws AgentUnavailableException {
        lenient().when(PerformFullDisconnectOnAgentDisconnectEventBroadcast.value()).thenReturn(Boolean.FALSE);
        lenient().when(hostJoinLockMock.lock(anyInt())).thenReturn(Boolean.TRUE);

        boolean result = mgr.executeUserRequest(HOST_ID, EVENT);
        Assert.assertTrue(result);

        GlobalLock.getInternLock(anyString());

        // we do not expect any lock to be called as lock happens only in handleDisconnectWithoutInvestigation
        verify(hostJoinLockMock, never()).lock(anyInt());
    }

    @Test
    public void scanDirectAgentToLoadNoHostsTest() {
        ClusteredAgentManagerImpl clusteredAgentManagerImpl = mock(ClusteredAgentManagerImpl.class);
        clusteredAgentManagerImpl._hostDao = hostDao;
        clusteredAgentManagerImpl.scanDirectAgentToLoad();
        verify(clusteredAgentManagerImpl, never()).findAttache(anyLong());
        verify(clusteredAgentManagerImpl, never()).loadDirectlyConnectedHost(any(), anyBoolean());
    }

    @Test
    public void scanDirectAgentToLoadHostWithoutAttacheTest() {
        // Arrange
        ClusteredAgentManagerImpl clusteredAgentManagerImpl = Mockito.spy(ClusteredAgentManagerImpl.class);
        HostVO hostVO = mock(HostVO.class);
        clusteredAgentManagerImpl._hostDao = hostDao;
        clusteredAgentManagerImpl.mgmtServiceConf = mgmtServiceConf;
        clusteredAgentManagerImpl._resourceMgr = mock(ResourceManagerImpl.class);
        when(mgmtServiceConf.getTimeout()).thenReturn(16000L);
        when(hostVO.getId()).thenReturn(1L);
        List<HostVO> hosts = new ArrayList<>();
        hosts.add(hostVO);
        when(hostDao.findAndUpdateDirectAgentToLoad(anyLong(), anyLong(), anyLong())).thenReturn(hosts);
        doReturn(Boolean.TRUE).when(clusteredAgentManagerImpl).loadDirectlyConnectedHost(hostVO, false);
        clusteredAgentManagerImpl.scanDirectAgentToLoad();
        verify(clusteredAgentManagerImpl).loadDirectlyConnectedHost(hostVO, false);
    }

    @Test
    public void scanDirectAgentToLoadHostWithForwardAttacheTest() {
        ClusteredAgentManagerImpl clusteredAgentManagerImpl = Mockito.spy(ClusteredAgentManagerImpl.class);
        HostVO hostVO = mock(HostVO.class);
        clusteredAgentManagerImpl._hostDao = hostDao;
        clusteredAgentManagerImpl.mgmtServiceConf = mgmtServiceConf;
        when(mgmtServiceConf.getTimeout()).thenReturn(16000L);
        when(hostVO.getId()).thenReturn(1L);
        List<HostVO> hosts = new ArrayList<>();
        hosts.add(hostVO);
        when(hostDao.findAndUpdateDirectAgentToLoad(anyLong(), anyLong(), anyLong())).thenReturn(hosts);
        AgentAttache agentAttache = mock(AgentAttache.class);
        when(agentAttache.forForward()).thenReturn(Boolean.TRUE);
        when(clusteredAgentManagerImpl.findAttache(1L)).thenReturn(agentAttache);

        clusteredAgentManagerImpl.scanDirectAgentToLoad();
        verify(clusteredAgentManagerImpl).removeAgent(agentAttache, Status.Disconnected);
    }

    @Test
    public void scanDirectAgentToLoadHostWithNonForwardAttacheTest() {
        // Arrange
        ClusteredAgentManagerImpl clusteredAgentManagerImpl = Mockito.spy(new ClusteredAgentManagerImpl());
        HostVO hostVO = mock(HostVO.class);
        clusteredAgentManagerImpl._hostDao = hostDao;
        clusteredAgentManagerImpl.mgmtServiceConf = mgmtServiceConf;
        clusteredAgentManagerImpl._haMgr = mock(HighAvailabilityManagerImpl.class);
        when(mgmtServiceConf.getTimeout()).thenReturn(16000L);
        when(hostVO.getId()).thenReturn(0L);
        List<HostVO> hosts = new ArrayList<>();
        hosts.add(hostVO);
        when(hostDao.findAndUpdateDirectAgentToLoad(anyLong(), anyLong(), anyLong())).thenReturn(hosts);

        AgentAttache agentAttache = mock(AgentAttache.class);
        when(agentAttache.forForward()).thenReturn(Boolean.FALSE);
        when(clusteredAgentManagerImpl.findAttache(0L)).thenReturn(agentAttache);
        doReturn(Boolean.TRUE).when(clusteredAgentManagerImpl).agentStatusTransitTo(hostVO, Status.Event.Ping, clusteredAgentManagerImpl._nodeId);
        doReturn(Status.Up).when(clusteredAgentManagerImpl).investigate(agentAttache);

        clusteredAgentManagerImpl.scanDirectAgentToLoad();
        verify(clusteredAgentManagerImpl).investigate(agentAttache);
        verify(clusteredAgentManagerImpl).agentStatusTransitTo(hostVO, Status.Event.Ping, clusteredAgentManagerImpl._nodeId);
    }

    @Test
    public void scanDirectAgentToLoadHostWithNonForwardAttacheAndDisconnectedTest() {
        ClusteredAgentManagerImpl clusteredAgentManagerImpl = Mockito.spy(ClusteredAgentManagerImpl.class);
        HostVO hostVO = mock(HostVO.class);
        clusteredAgentManagerImpl._hostDao = hostDao;
        clusteredAgentManagerImpl.mgmtServiceConf = mgmtServiceConf;
        clusteredAgentManagerImpl._haMgr = mock(HighAvailabilityManagerImpl.class);
        clusteredAgentManagerImpl._resourceMgr = mock(ResourceManagerImpl.class);
        when(mgmtServiceConf.getTimeout()).thenReturn(16000L);
        when(hostVO.getId()).thenReturn(0L);
        List<HostVO> hosts = new ArrayList<>();
        hosts.add(hostVO);
        when(hostDao.findAndUpdateDirectAgentToLoad(anyLong(), anyLong(), anyLong())).thenReturn(hosts);
        AgentAttache agentAttache = mock(AgentAttache.class);
        when(agentAttache.forForward()).thenReturn(Boolean.FALSE);
        when(clusteredAgentManagerImpl.findAttache(0L)).thenReturn(agentAttache);
        doReturn(Boolean.TRUE).when(clusteredAgentManagerImpl).loadDirectlyConnectedHost(hostVO, false);
        clusteredAgentManagerImpl.scanDirectAgentToLoad();
        verify(clusteredAgentManagerImpl).investigate(agentAttache);
        verify(clusteredAgentManagerImpl).loadDirectlyConnectedHost(hostVO, false);
    }
}

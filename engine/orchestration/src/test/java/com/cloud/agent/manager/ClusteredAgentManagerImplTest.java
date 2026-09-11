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

import com.cloud.cluster.agentlb.dao.HostTransferMapDao;
import com.cloud.configuration.ManagementServiceConfiguration;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.ha.HighAvailabilityManagerImpl;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.Status.Event;
import com.cloud.host.dao.HostDao;
import com.cloud.resource.ResourceManagerImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ClusteredAgentManagerImplTest {

    private HostDao _hostDao;
    @Mock
    ManagementServiceConfiguration _mgmtServiceConf;

    @Before
    public void setUp() throws Exception {
        _hostDao = mock(HostDao.class);
    }

    @Test
    public void scanDirectAgentToLoadNoHostsTest() {
        ClusteredAgentManagerImpl clusteredAgentManagerImpl = mock(ClusteredAgentManagerImpl.class);
        clusteredAgentManagerImpl._hostDao = _hostDao;
        clusteredAgentManagerImpl.scanDirectAgentToLoad();
        verify(clusteredAgentManagerImpl, never()).findAttache(anyLong());
        verify(clusteredAgentManagerImpl, never()).loadDirectlyConnectedHost(any(), anyBoolean());
    }

    @Test
    public void scanDirectAgentToLoadHostWithoutAttacheTest() {
        // Arrange
        ClusteredAgentManagerImpl clusteredAgentManagerImpl = Mockito.spy(ClusteredAgentManagerImpl.class);
        HostVO hostVO = mock(HostVO.class);
        clusteredAgentManagerImpl._hostDao = _hostDao;
        clusteredAgentManagerImpl.mgmtServiceConf = _mgmtServiceConf;
        clusteredAgentManagerImpl._resourceMgr = mock(ResourceManagerImpl.class);
        when(_mgmtServiceConf.getTimeout()).thenReturn(16000L);
        when(hostVO.getId()).thenReturn(1L);
        List hosts = new ArrayList<>();
        hosts.add(hostVO);
        when(_hostDao.findAndUpdateDirectAgentToLoad(anyLong(), anyLong(), anyLong())).thenReturn(hosts);
        AgentAttache agentAttache = mock(AgentAttache.class);
        doReturn(Boolean.TRUE).when(clusteredAgentManagerImpl).loadDirectlyConnectedHost(hostVO, false);
        clusteredAgentManagerImpl.scanDirectAgentToLoad();
        verify(clusteredAgentManagerImpl).loadDirectlyConnectedHost(hostVO, false);
    }

    @Test
    public void scanDirectAgentToLoadHostWithForwardAttacheTest() {
        ClusteredAgentManagerImpl clusteredAgentManagerImpl = Mockito.spy(ClusteredAgentManagerImpl.class);
        HostVO hostVO = mock(HostVO.class);
        clusteredAgentManagerImpl._hostDao = _hostDao;
        clusteredAgentManagerImpl.mgmtServiceConf = _mgmtServiceConf;
        when(_mgmtServiceConf.getTimeout()).thenReturn(16000L);
        when(hostVO.getId()).thenReturn(1L);
        List hosts = new ArrayList<>();
        hosts.add(hostVO);
        when(_hostDao.findAndUpdateDirectAgentToLoad(anyLong(), anyLong(), anyLong())).thenReturn(hosts);
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
        clusteredAgentManagerImpl._hostDao = _hostDao;
        clusteredAgentManagerImpl.mgmtServiceConf = _mgmtServiceConf;
        clusteredAgentManagerImpl._haMgr = mock(HighAvailabilityManagerImpl.class);
        when(_mgmtServiceConf.getTimeout()).thenReturn(16000L);
        when(hostVO.getId()).thenReturn(0L);
        List hosts = new ArrayList<>();
        hosts.add(hostVO);
        when(_hostDao.findAndUpdateDirectAgentToLoad(anyLong(), anyLong(), anyLong())).thenReturn(hosts);

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
        clusteredAgentManagerImpl._hostDao = _hostDao;
        clusteredAgentManagerImpl.mgmtServiceConf = _mgmtServiceConf;
        clusteredAgentManagerImpl._haMgr = mock(HighAvailabilityManagerImpl.class);
        clusteredAgentManagerImpl._resourceMgr = mock(ResourceManagerImpl.class);
        when(_mgmtServiceConf.getTimeout()).thenReturn(16000L);
        when(hostVO.getId()).thenReturn(0L);
        List hosts = new ArrayList<>();
        hosts.add(hostVO);
        when(_hostDao.findAndUpdateDirectAgentToLoad(anyLong(), anyLong(), anyLong())).thenReturn(hosts);
        AgentAttache agentAttache = mock(AgentAttache.class);
        when(agentAttache.forForward()).thenReturn(Boolean.FALSE);
        when(clusteredAgentManagerImpl.findAttache(0L)).thenReturn(agentAttache);
        doReturn(Boolean.TRUE).when(clusteredAgentManagerImpl).loadDirectlyConnectedHost(hostVO, false);
        clusteredAgentManagerImpl.scanDirectAgentToLoad();
        verify(clusteredAgentManagerImpl).investigate(agentAttache);
        verify(clusteredAgentManagerImpl).loadDirectlyConnectedHost(hostVO, false);
    }

    // https://github.com/apache/cloudstack/issues/9640
    // Indirectly connected agents (KVM hosts, SSVM, CPVM) dial in to a management server rather than
    // being loaded directly by it, so they must be disconnected (and left to reconnect on their own)
    // instead of going through the direct-agent rebalance dance that expects a ClusteredDirectAgentAttache.
    @Test
    public void rebalanceHostDisconnectsIndirectAgentInsteadOfDirectRebalanceTest() throws AgentUnavailableException {
        ClusteredAgentManagerImpl clusteredAgentManagerImpl = Mockito.spy(new ClusteredAgentManagerImpl());
        clusteredAgentManagerImpl._nodeId = 1L;
        clusteredAgentManagerImpl._hostTransferDao = mock(HostTransferMapDao.class);

        long hostId = 10L;
        AgentAttache indirectAttache = mock(ClusteredAgentAttache.class);
        when(clusteredAgentManagerImpl.findAttache(hostId)).thenReturn(indirectAttache);
        doReturn(true).when(clusteredAgentManagerImpl).handleDisconnectWithoutInvestigation(indirectAttache, Event.AgentDisconnected, true, true);
        doNothing().when(clusteredAgentManagerImpl).finishRebalance(hostId, 2L, Event.RebalanceCompleted);

        boolean result = clusteredAgentManagerImpl.rebalanceHost(hostId, 1L, 2L, false);

        assertTrue(result);
        verify(clusteredAgentManagerImpl).handleDisconnectWithoutInvestigation(indirectAttache, Event.AgentDisconnected, true, true);
        verify(clusteredAgentManagerImpl, never()).startRebalance(hostId);
    }

    @Test
    public void rebalanceHostStillUsesDirectRebalanceForDirectAgentTest() throws AgentUnavailableException {
        ClusteredAgentManagerImpl clusteredAgentManagerImpl = Mockito.spy(new ClusteredAgentManagerImpl());
        clusteredAgentManagerImpl._nodeId = 1L;

        long hostId = 11L;
        AgentAttache directAttache = mock(ClusteredDirectAgentAttache.class);
        when(clusteredAgentManagerImpl.findAttache(hostId)).thenReturn(directAttache);
        doReturn(false).when(clusteredAgentManagerImpl).startRebalance(hostId);
        doNothing().when(clusteredAgentManagerImpl).finishRebalance(hostId, 2L, Event.RebalanceFailed);

        boolean result = clusteredAgentManagerImpl.rebalanceHost(hostId, 1L, 2L, false);

        assertFalse(result);
        verify(clusteredAgentManagerImpl).startRebalance(hostId);
        verify(clusteredAgentManagerImpl, never()).handleDisconnectWithoutInvestigation(any(), any(), anyBoolean(), anyBoolean());
    }
}

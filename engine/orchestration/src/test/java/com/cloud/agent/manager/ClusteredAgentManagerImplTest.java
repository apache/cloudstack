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
import com.cloud.ha.HighAvailabilityManagerImpl;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
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
}

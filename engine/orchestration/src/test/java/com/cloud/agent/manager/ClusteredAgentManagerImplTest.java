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

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.CancelJobCommand;
import com.cloud.agent.api.Command;
import com.cloud.cluster.ClusterManager;
import com.cloud.cluster.ClusterServicePdu;
import com.cloud.configuration.ManagementServiceConfiguration;
import com.cloud.serializer.GsonHelper;
import org.apache.cloudstack.framework.jobs.AsyncJobManager;
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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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

    private ClusteredAgentManagerImpl clusteredManagerOnServer1WithPeer2(final long jobId) {
        final ClusteredAgentManagerImpl mgr = Mockito.spy(ClusteredAgentManagerImpl.class);
        mgr._nodeId = 1L;
        mgr._gson = GsonHelper.getGson();
        mgr._clusterMgr = mock(ClusterManager.class);
        final AsyncJobManager asyncJobManager = mock(AsyncJobManager.class);
        mgr.asyncJobManager = asyncJobManager;
        when(asyncJobManager.listMgmtServersExecutingJob(jobId)).thenReturn(new HashSet<>(Arrays.asList(1L, 2L)));
        return mgr;
    }

    private String peerAnswer(final ClusteredAgentManagerImpl mgr, final CancelJobCommand cmd, final boolean result, final String details) {
        return mgr._gson.toJson(new Answer[] {new Answer(cmd, result, details)});
    }

    @Test
    public void testACancelAsksTheServerRunningTheWorkBeforeAccepting() {
        final ClusteredAgentManagerImpl mgr = clusteredManagerOnServer1WithPeer2(42L);
        doReturn(null).when(mgr).checkJobCannotBeCancelledHere(42L);
        when(mgr._clusterMgr.execute(eq("2"), anyLong(), anyString(), anyBoolean()))
                .thenReturn(peerAnswer(mgr, new CancelJobCommand(42L, null, true), false, "no vCenter task or transfer is running for StopCommand (host esxi-2)"));

        assertEquals("no vCenter task or transfer is running for StopCommand (host esxi-2)", mgr.checkJobCannotBeCancelled(42L));
        verify(mgr._clusterMgr, never()).execute(eq("1"), anyLong(), anyString(), anyBoolean());

        when(mgr._clusterMgr.execute(eq("2"), anyLong(), anyString(), anyBoolean()))
                .thenReturn(peerAnswer(mgr, new CancelJobCommand(42L, null, true), true, null));
        assertNull(mgr.checkJobCannotBeCancelled(42L));

        when(mgr._clusterMgr.execute(eq("2"), anyLong(), anyString(), anyBoolean())).thenReturn(null);
        assertTrue(mgr.checkJobCannotBeCancelled(42L).contains("could not be reached"));
    }

    @Test
    public void testALocalRefusalIsAnsweredWithoutAskingThePeers() {
        final ClusteredAgentManagerImpl mgr = clusteredManagerOnServer1WithPeer2(42L);
        doReturn("KVM: MigrateCommand has nothing that can be stopped on this host (host kvm-1)").when(mgr).checkJobCannotBeCancelledHere(42L);

        assertTrue(mgr.checkJobCannotBeCancelled(42L).startsWith("KVM:"));
        verify(mgr._clusterMgr, never()).execute(anyString(), anyLong(), anyString(), anyBoolean());
    }

    @Test
    public void testACancelStopsTheWorkOnEveryServerAndFailsIfOneCannot() {
        final ClusteredAgentManagerImpl mgr = clusteredManagerOnServer1WithPeer2(42L);
        doReturn(true).when(mgr).cancelJobExecutionHere(42L, "Cancelled by admin");
        when(mgr._clusterMgr.execute(eq("2"), anyLong(), anyString(), anyBoolean()))
                .thenReturn(peerAnswer(mgr, new CancelJobCommand(42L, "Cancelled by admin", false), false, "not every in-flight command of job-42 on management server 2 could be stopped"));
        assertFalse(mgr.cancelJobExecution(42L, "Cancelled by admin"));

        when(mgr._clusterMgr.execute(eq("2"), anyLong(), anyString(), anyBoolean()))
                .thenReturn(peerAnswer(mgr, new CancelJobCommand(42L, "Cancelled by admin", false), true, null));
        assertTrue(mgr.cancelJobExecution(42L, "Cancelled by admin"));
    }

    @Test
    public void testThePeerAnswersAJobCancelWithItsOwnInFlightCommands() {
        final ClusteredAgentManagerImpl mgr = clusteredManagerOnServer1WithPeer2(42L);
        doReturn("vCenter reports task RelocateVM_Task(task-9) as not cancelable (host esxi-1)").when(mgr).checkJobCannotBeCancelledHere(42L);
        doReturn(true).when(mgr).cancelJobExecutionHere(42L, "Cancelled by admin");

        final ClusterServicePdu check = new ClusterServicePdu();
        check.setJsonPackage(mgr._gson.toJson(new Command[] {new CancelJobCommand(42L, null, true)}));
        Answer[] answers = mgr._gson.fromJson(mgr.new ClusterDispatcher().dispatch(check), Answer[].class);
        assertFalse(answers[0].getResult());
        assertEquals("vCenter reports task RelocateVM_Task(task-9) as not cancelable (host esxi-1)", answers[0].getDetails());

        final ClusterServicePdu cancel = new ClusterServicePdu();
        cancel.setJsonPackage(mgr._gson.toJson(new Command[] {new CancelJobCommand(42L, "Cancelled by admin", false)}));
        answers = mgr._gson.fromJson(mgr.new ClusterDispatcher().dispatch(cancel), Answer[].class);
        assertTrue(answers[0].getResult());
        // the peer's own check and stop were used, not the forwarding ones
        verify(mgr, never()).checkJobCannotBeCancelled(anyLong());
        verify(mgr, never()).cancelJobExecution(anyLong(), anyString());
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

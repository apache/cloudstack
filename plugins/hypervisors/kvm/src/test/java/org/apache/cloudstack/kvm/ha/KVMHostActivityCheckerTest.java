/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cloudstack.kvm.ha;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.joda.time.DateTime;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.CheckOnHostAnswer;
import com.cloud.agent.api.CheckOnHostCommand;
import com.cloud.agent.api.CheckVMActivityOnStoragePoolCommand;
import com.cloud.agent.api.to.HostTO;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.resource.ResourceManager;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.storage.StorageManager;
import com.cloud.storage.StoragePool;
import com.cloud.storage.clvm.ClvmPoolManager;
import com.cloud.storage.Volume;

@RunWith(MockitoJUnitRunner.Silent.class)
public class KVMHostActivityCheckerTest {

    @Mock
    private ClvmPoolManager clvmPoolManager;
    @Mock
    private StorageManager storageManager;
    @Mock
    private AgentManager agentMgr;
    @Mock
    private ResourceManager resourceManager;
    @Mock
    private ClusterDao clusterDao;
    @Mock
    private Host host;
    @Mock
    private StoragePool pool;
    @Mock
    private Volume volume;
    @InjectMocks
    private KVMHostActivityChecker checker;

    private void reportSanlockHostId(Long sanlockHostId) {
        doAnswer(invocation -> {
            invocation.<HostTO>getArgument(0).setSanlockHostId(sanlockHostId);
            return null;
        }).when(clvmPoolManager).addSanlockHostId(any(HostTO.class), eq(host));
    }

    private void runActivityCheck(StoragePoolType poolType) throws Exception {
        when(pool.getPoolType()).thenReturn(poolType);
        when(host.getId()).thenReturn(8L);
        when(volume.getPath()).thenReturn("vol");
        reportSanlockHostId(2L);
        when(storageManager.sendToPool(any(StoragePool.class), any(long[].class), any(CheckVMActivityOnStoragePoolCommand.class))).thenReturn(new Answer(null));
        HashMap<StoragePool, List<Volume>> poolVolumes = new HashMap<>();
        poolVolumes.put(pool, List.of(volume));
        checker.verifyActivityOfStorageOnHost(poolVolumes, pool, host, new DateTime(), true);
    }

    @Test
    public void testSanlockHostIdIsNotLookedUpForNonClvmPool() throws Exception {
        runActivityCheck(StoragePoolType.NetworkFilesystem);
        verify(clvmPoolManager, never()).addSanlockHostId(any(), any());
    }

    @Test
    public void testSanlockHostIdIsSentForClvmPool() throws Exception {
        runActivityCheck(StoragePoolType.CLVM_NG);
        ArgumentCaptor<CheckVMActivityOnStoragePoolCommand> captor = ArgumentCaptor.forClass(CheckVMActivityOnStoragePoolCommand.class);
        verify(storageManager).sendToPool(any(StoragePool.class), any(long[].class), captor.capture());
        assertEquals(Long.valueOf(2), captor.getValue().getHost().getSanlockHostId());
    }

    @Test
    public void testHostCheckSendsSanlockHostIdWhenHostReportsOne() throws Exception {
        when(host.getId()).thenReturn(8L);
        when(host.getHypervisorType()).thenReturn(HypervisorType.KVM);
        reportSanlockHostId(2L);
        when(agentMgr.easySend(anyLong(), any(CheckOnHostCommand.class))).thenReturn(new Answer(null, false, "down"));
        checker.getHostAgentStatus(host);
        ArgumentCaptor<CheckOnHostCommand> captor = ArgumentCaptor.forClass(CheckOnHostCommand.class);
        verify(agentMgr).easySend(anyLong(), captor.capture());
        assertEquals(Long.valueOf(2), captor.getValue().getHost().getSanlockHostId());
    }

    @Test
    public void testHostCheckSendsNoSanlockHostIdWhenHostReportsNone() throws Exception {
        when(host.getId()).thenReturn(8L);
        when(host.getHypervisorType()).thenReturn(HypervisorType.KVM);
        when(agentMgr.easySend(anyLong(), any(CheckOnHostCommand.class))).thenReturn(new Answer(null, false, "down"));
        checker.getHostAgentStatus(host);
        ArgumentCaptor<CheckOnHostCommand> captor = ArgumentCaptor.forClass(CheckOnHostCommand.class);
        verify(agentMgr).easySend(anyLong(), captor.capture());
        assertNull(captor.getValue().getHost().getSanlockHostId());
    }

    private KVMHostActivityChecker prepareUnreachableHostWithUpNeighbour(boolean hasClvmPool, Boolean... neighbourAliveAnswers) {
        HostVO neighbour = org.mockito.Mockito.mock(HostVO.class);
        when(neighbour.getId()).thenReturn(9L);
        when(neighbour.getHypervisorType()).thenReturn(HypervisorType.KVM);
        when(host.getId()).thenReturn(8L);
        when(host.getHypervisorType()).thenReturn(HypervisorType.KVM);
        when(resourceManager.listHostsInClusterByStatus(anyLong(), eq(Status.Up))).thenReturn(List.of(neighbour));
        when(clvmPoolManager.hasClvmPool(host)).thenReturn(hasClvmPool);
        when(clvmPoolManager.getClvmVolumesByPool(host)).thenReturn(hasClvmPool ? Map.of("pool", List.of("lv")) : Map.of());
        when(agentMgr.easySend(eq(8L), any(CheckOnHostCommand.class))).thenReturn(null);
        CheckOnHostAnswer first = new CheckOnHostAnswer(null, neighbourAliveAnswers[0], "");
        CheckOnHostAnswer[] rest = new CheckOnHostAnswer[neighbourAliveAnswers.length - 1];
        for (int i = 1; i < neighbourAliveAnswers.length; i++) {
            rest[i - 1] = new CheckOnHostAnswer(null, neighbourAliveAnswers[i], "");
        }
        when(agentMgr.easySend(eq(9L), any(CheckOnHostCommand.class))).thenReturn(first, rest);
        KVMHostActivityChecker spied = spy(checker);
        doReturn(true).when(spied).sleep(anyLong());
        return spied;
    }

    @Test
    public void testHostIsDownWhenClvmLeaseExpiresWhileNeighbourSaidUp() {
        KVMHostActivityChecker spied = prepareUnreachableHostWithUpNeighbour(true, true, true, false);
        assertEquals(Status.Down, spied.getHostAgentStatus(host));
        verify(agentMgr, times(3)).easySend(eq(9L), any(CheckOnHostCommand.class));
    }

    @Test
    public void testHostStaysDisconnectedWhenClvmLeaseNeverExpires() {
        KVMHostActivityChecker spied = prepareUnreachableHostWithUpNeighbour(true, true);
        assertEquals(Status.Disconnected, spied.getHostAgentStatus(host));
    }

    @Test
    public void testNoWaitForClusterWithoutClvmPool() {
        KVMHostActivityChecker spied = prepareUnreachableHostWithUpNeighbour(false, true);
        assertEquals(Status.Disconnected, spied.getHostAgentStatus(host));
        verify(agentMgr, times(1)).easySend(eq(9L), any(CheckOnHostCommand.class));
    }

    @Test
    public void testSecondInvestigationDoesNotWaitWhileOneIsAlreadyWaiting() {
        KVMHostActivityChecker spied = prepareUnreachableHostWithUpNeighbour(true, true);
        List<Status> nested = new java.util.ArrayList<>();
        doAnswer(invocation -> {
            if (nested.isEmpty()) {
                nested.add(spied.getHostAgentStatus(host));
            }
            return true;
        }).when(spied).sleep(anyLong());
        assertEquals(Status.Disconnected, spied.getHostAgentStatus(host));
        assertEquals(Status.Disconnected, nested.get(0));
    }

    @Test
    public void testNoWaitForHostWithoutClvmVolumesInClusterWithClvmPool() {
        KVMHostActivityChecker spied = prepareUnreachableHostWithUpNeighbour(true, true);
        when(clvmPoolManager.getClvmVolumesByPool(host)).thenReturn(Map.of());
        assertEquals(Status.Disconnected, spied.getHostAgentStatus(host));
        verify(agentMgr, times(1)).easySend(eq(9L), any(CheckOnHostCommand.class));
    }

    @Test
    public void testHostCheckSendsClvmVolumesOfTheHost() {
        when(host.getId()).thenReturn(8L);
        when(host.getHypervisorType()).thenReturn(HypervisorType.KVM);
        when(agentMgr.easySend(anyLong(), any(CheckOnHostCommand.class))).thenReturn(new Answer(null, false, "down"));
        checker.getHostAgentStatus(host);
        ArgumentCaptor<CheckOnHostCommand> captor = ArgumentCaptor.forClass(CheckOnHostCommand.class);
        verify(agentMgr).easySend(anyLong(), captor.capture());
        verify(clvmPoolManager).addClvmVolumes(eq(captor.getValue().getHost()), eq(host));
    }
}

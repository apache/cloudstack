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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;

import org.joda.time.DateTime;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.CheckOnHostCommand;
import com.cloud.agent.api.CheckVMActivityOnStoragePoolCommand;
import com.cloud.host.DetailVO;
import com.cloud.host.Host;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.host.dao.HostDetailsDao;
import com.cloud.resource.ResourceManager;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.storage.StorageManager;
import com.cloud.storage.StoragePool;
import com.cloud.storage.Volume;

@RunWith(MockitoJUnitRunner.Silent.class)
public class KVMHostActivityCheckerTest {

    @Mock
    private HostDetailsDao hostDetailsDao;
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

    private void runActivityCheck(StoragePoolType poolType) throws Exception {
        when(pool.getPoolType()).thenReturn(poolType);
        when(host.getId()).thenReturn(8L);
        when(volume.getPath()).thenReturn("vol");
        when(hostDetailsDao.findDetail(8L, Host.HOST_SANLOCK_ID)).thenReturn(new DetailVO(8L, Host.HOST_SANLOCK_ID, "2"));
        when(storageManager.sendToPool(any(StoragePool.class), any(long[].class), any(CheckVMActivityOnStoragePoolCommand.class))).thenReturn(new Answer(null));
        HashMap<StoragePool, List<Volume>> poolVolumes = new HashMap<>();
        poolVolumes.put(pool, List.of(volume));
        checker.verifyActivityOfStorageOnHost(poolVolumes, pool, host, new DateTime(), true);
    }

    @Test
    public void testSanlockHostIdIsNotLookedUpForNonClvmPool() throws Exception {
        runActivityCheck(StoragePoolType.NetworkFilesystem);
        verify(hostDetailsDao, never()).findDetail(anyLong(), any());
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
        when(hostDetailsDao.findDetail(8L, Host.HOST_SANLOCK_ID)).thenReturn(new DetailVO(8L, Host.HOST_SANLOCK_ID, "2"));
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
}

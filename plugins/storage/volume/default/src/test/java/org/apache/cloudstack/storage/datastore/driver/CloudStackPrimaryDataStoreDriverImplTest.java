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

package org.apache.cloudstack.storage.datastore.driver;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPoint;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPointSelector;
import org.apache.cloudstack.framework.async.AsyncCompletionCallback;
import org.apache.cloudstack.engine.subsystem.api.storage.CreateCmdResult;
import org.apache.cloudstack.storage.volume.VolumeObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.storage.ResizeVolumeAnswer;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.dao.HostDao;
import com.cloud.storage.ResizeVolumePayload;
import com.cloud.storage.Storage;
import com.cloud.storage.StorageManager;
import com.cloud.storage.StoragePool;
import com.cloud.storage.clvm.ClvmPoolManager;
import com.cloud.vm.VirtualMachine;

@RunWith(MockitoJUnitRunner.class)
public class CloudStackPrimaryDataStoreDriverImplTest {

    @InjectMocks
    private CloudStackPrimaryDataStoreDriverImpl driver;

    @Mock
    private ClvmPoolManager clvmPoolManager;
    @Mock
    private AgentManager agentMgr;
    @Mock
    private HostDao hostDao;
    @Mock
    private EndPointSelector epSelector;
    @Mock
    private StorageManager storageMgr;

    @Mock
    private VolumeObject vol;
    private StoragePool pool;
    @Mock
    private HostVO lockHost;
    @Mock
    private AsyncCompletionCallback<CreateCmdResult> callback;

    private static final long LOCK_HOST_ID = 42L;
    private static final long OTHER_HOST_ID = 99L;
    private static final String VOLUME_UUID = "test-vol-uuid";
    private static final String VOLUME_PATH = "vm-1-disk-0";

    @Before
    public void setUp() {
        pool = Mockito.mock(StoragePool.class, withSettings().extraInterfaces(DataStore.class));

        when(vol.getUuid()).thenReturn(VOLUME_UUID);
        when(vol.getId()).thenReturn(1L);
        when(vol.getPath()).thenReturn(VOLUME_PATH);
        when(vol.getSize()).thenReturn(10L * 1024 * 1024 * 1024);

        when(pool.getPoolType()).thenReturn(Storage.StoragePoolType.CLVM_NG);
        when(pool.getParent()).thenReturn(0L);
        when(pool.getPath()).thenReturn("/vg-test");

        when(vol.getDataStore()).thenReturn((DataStore) pool);

        when(hostDao.findById(LOCK_HOST_ID)).thenReturn(lockHost);
        when(lockHost.getStatus()).thenReturn(Status.Up);
    }

    private ResizeVolumeAnswer mockSuccessAnswer(long newSize) throws Exception {
        ResizeVolumeAnswer answer = Mockito.mock(ResizeVolumeAnswer.class);
        when(answer.getResult()).thenReturn(true);
        when(answer.getNewSize()).thenReturn(newSize);
        return answer;
    }

    private void stubAgentSend(long hostId, Answer answer) throws Exception {
        Mockito.doReturn(answer).when(agentMgr).send(eq(hostId), any(Command.class));
    }

    private void verifyAgentSend(long hostId) throws Exception {
        verify(agentMgr).send(eq(hostId), any(Command.class));
    }

    private void verifyAgentSendNever(long hostId) throws Exception {
        verify(agentMgr, never()).send(eq(hostId), any(Command.class));
    }

    private ResizeVolumePayload makePayload(long[] hosts) {
        ResizeVolumePayload p = new ResizeVolumePayload(
                20L * 1024 * 1024 * 1024, null, null, null, false, "test-vm", hosts, false);
        when(vol.getpayload()).thenReturn(p);
        return p;
    }

    @Test
    public void testResize_clvmNg_vmRunning_usesCallerHost() throws Exception {
        VirtualMachine runningVm = Mockito.mock(VirtualMachine.class);
        when(runningVm.getHostId()).thenReturn(OTHER_HOST_ID);
        when(vol.getAttachedVM()).thenReturn(runningVm);
        makePayload(new long[]{OTHER_HOST_ID});

        stubAgentSend(OTHER_HOST_ID, mockSuccessAnswer(20L * 1024 * 1024 * 1024));

        driver.resize(vol, callback);

        verify(clvmPoolManager, never()).getClvmLockHostId(anyLong(), anyString(), anyString(), any(), anyBoolean());
        verifyAgentSend(OTHER_HOST_ID);
    }

    @Test
    public void testResize_clvmNg_vmStopped_routesToLockHost() throws Exception {
        VirtualMachine stoppedVm = Mockito.mock(VirtualMachine.class);
        when(stoppedVm.getHostId()).thenReturn(null);
        when(vol.getAttachedVM()).thenReturn(stoppedVm);
        makePayload(new long[]{OTHER_HOST_ID}); // stale lastHostId from caller

        when(clvmPoolManager.getClvmLockHostId(anyLong(), anyString(), anyString(), any(), eq(true)))
                .thenReturn(LOCK_HOST_ID);
        stubAgentSend(LOCK_HOST_ID, mockSuccessAnswer(20L * 1024 * 1024 * 1024));

        driver.resize(vol, callback);

        // Must override stale lastHostId with actual lock host
        verifyAgentSend(LOCK_HOST_ID);
        verifyAgentSendNever(OTHER_HOST_ID);
    }

    @Test
    public void testResize_clvmNg_vmStopped_lockHostDown_keepsCallerHosts() throws Exception {
        VirtualMachine stoppedVm = Mockito.mock(VirtualMachine.class);
        when(stoppedVm.getHostId()).thenReturn(null);
        when(vol.getAttachedVM()).thenReturn(stoppedVm);
        makePayload(new long[]{OTHER_HOST_ID});

        when(clvmPoolManager.getClvmLockHostId(anyLong(), anyString(), anyString(), any(), eq(true)))
                .thenReturn(LOCK_HOST_ID);
        when(lockHost.getStatus()).thenReturn(Status.Disconnected);
        stubAgentSend(OTHER_HOST_ID, mockSuccessAnswer(20L * 1024 * 1024 * 1024));

        driver.resize(vol, callback);

        verifyAgentSend(OTHER_HOST_ID);
    }

    @Test
    public void testResize_clvmNg_detached_routesToLockHost() throws Exception {
        when(vol.getAttachedVM()).thenReturn(null);
        makePayload(null); // no hosts from caller

        when(clvmPoolManager.getClvmLockHostId(anyLong(), anyString(), anyString(), any(), eq(true)))
                .thenReturn(LOCK_HOST_ID);
        stubAgentSend(LOCK_HOST_ID, mockSuccessAnswer(20L * 1024 * 1024 * 1024));

        driver.resize(vol, callback);

        verifyAgentSend(LOCK_HOST_ID);
    }

    @Test
    public void testResize_clvmNg_detached_noLockHolder_fallsToEpSelector() throws Exception {
        when(vol.getAttachedVM()).thenReturn(null);
        makePayload(null);

        when(clvmPoolManager.getClvmLockHostId(anyLong(), anyString(), anyString(), any(), eq(true)))
                .thenReturn(null); // no lock holder found

        EndPoint ep = Mockito.mock(EndPoint.class);
        when(ep.getId()).thenReturn(LOCK_HOST_ID);
        when(epSelector.select(any(), anyBoolean())).thenReturn(ep);
        stubAgentSend(LOCK_HOST_ID, mockSuccessAnswer(20L * 1024 * 1024 * 1024));

        driver.resize(vol, callback);

        verify(epSelector).select(any(), anyBoolean());
        verifyAgentSend(LOCK_HOST_ID);
    }

    @Test
    public void testResize_clvmNg_agentUnavailable_failsFast() throws Exception {
        VirtualMachine runningVm = Mockito.mock(VirtualMachine.class);
        when(runningVm.getHostId()).thenReturn(OTHER_HOST_ID);
        when(vol.getAttachedVM()).thenReturn(runningVm);
        makePayload(new long[]{OTHER_HOST_ID});

        Mockito.doThrow(new AgentUnavailableException("host down", OTHER_HOST_ID))
                .when(agentMgr).send(eq(OTHER_HOST_ID), any(Command.class));

        driver.resize(vol, callback);

        verify(callback).complete(any());
        verify(storageMgr, never()).sendToPool(any(StoragePool.class), any(long[].class), any());
    }

    @Test
    public void testResize_clvmNg_operationTimedOut_failsFast() throws Exception {
        VirtualMachine runningVm = Mockito.mock(VirtualMachine.class);
        when(runningVm.getHostId()).thenReturn(OTHER_HOST_ID);
        when(vol.getAttachedVM()).thenReturn(runningVm);
        makePayload(new long[]{OTHER_HOST_ID});

        Mockito.doThrow(new OperationTimedoutException(null, OTHER_HOST_ID, 0, 0, false))
                .when(agentMgr).send(eq(OTHER_HOST_ID), any(Command.class));

        driver.resize(vol, callback);

        verify(callback).complete(any());
        verify(storageMgr, never()).sendToPool(any(StoragePool.class), any(long[].class), any());
    }

    @Test
    public void testResize_nonClvm_usesSendToPool() throws Exception {
        when(pool.getPoolType()).thenReturn(Storage.StoragePoolType.NetworkFilesystem);
        makePayload(null);

        EndPoint ep = Mockito.mock(EndPoint.class);
        when(ep.getId()).thenReturn(OTHER_HOST_ID);
        when(epSelector.select(any(), anyBoolean())).thenReturn(ep);

        ResizeVolumeAnswer answer = mockSuccessAnswer(20L * 1024 * 1024 * 1024);
        when(storageMgr.sendToPool(any(StoragePool.class), any(long[].class), any())).thenReturn(answer);

        driver.resize(vol, callback);

        verify(storageMgr).sendToPool(any(StoragePool.class), any(long[].class), any());
        verify(agentMgr, never()).send(anyLong(), any(Command.class));
        verify(clvmPoolManager, never()).getClvmLockHostId(anyLong(), anyString(), anyString(), any(), anyBoolean());
    }

    @Test
    public void testResize_clvmLegacy_vmStopped_routesToLockHost() throws Exception {
        when(pool.getPoolType()).thenReturn(Storage.StoragePoolType.CLVM);
        VirtualMachine stoppedVm = Mockito.mock(VirtualMachine.class);
        when(stoppedVm.getHostId()).thenReturn(null);
        when(vol.getAttachedVM()).thenReturn(stoppedVm);
        makePayload(new long[]{OTHER_HOST_ID});

        when(clvmPoolManager.getClvmLockHostId(anyLong(), anyString(), anyString(), any(), eq(true)))
                .thenReturn(LOCK_HOST_ID);
        stubAgentSend(LOCK_HOST_ID, mockSuccessAnswer(20L * 1024 * 1024 * 1024));

        driver.resize(vol, callback);

        verifyAgentSend(LOCK_HOST_ID);
        verifyAgentSendNever(OTHER_HOST_ID);
    }
}

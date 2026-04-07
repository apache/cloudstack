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

package com.cloud.storage;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.storage.dao.VolumeDetailsDao;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.storage.command.ClvmLockTransferAnswer;
import org.apache.cloudstack.storage.command.ClvmLockTransferCommand;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ClvmLockManagerTest {

    @Mock
    private VolumeDetailsDao volsDetailsDao;

    @Mock
    private AgentManager agentMgr;

    @Mock
    private HostDao hostDao;

    @InjectMocks
    private ClvmLockManager clvmLockManager;

    private static final Long VOLUME_ID = 100L;
    private static final Long HOST_ID_1 = 1L;
    private static final Long HOST_ID_2 = 2L;
    private static final String VOLUME_UUID = "test-volume-uuid";
    private static final String VOLUME_PATH = "test-volume-path";
    private static final String VG_NAME = "acsvg";

    @Before
    public void setUp() {
        Mockito.reset(volsDetailsDao, agentMgr, hostDao);
    }

    @Test
    public void testGetClvmLockHostId_Success() {
        VolumeDetailVO detail = new VolumeDetailVO();
        detail.setValue("123");
        when(volsDetailsDao.findDetail(VOLUME_ID, VolumeInfo.CLVM_LOCK_HOST_ID)).thenReturn(detail);

        Long result = clvmLockManager.getClvmLockHostId(VOLUME_ID, VOLUME_UUID);

        Assert.assertEquals(Long.valueOf(123), result);
    }

    @Test
    public void testGetClvmLockHostId_NoDetail() {
        when(volsDetailsDao.findDetail(VOLUME_ID, VolumeInfo.CLVM_LOCK_HOST_ID)).thenReturn(null);

        Long result = clvmLockManager.getClvmLockHostId(VOLUME_ID, VOLUME_UUID);

        Assert.assertNull(result);
    }

    @Test
    public void testGetClvmLockHostId_InvalidNumber() {
        VolumeDetailVO detail = new VolumeDetailVO();
        detail.setValue("invalid");
        when(volsDetailsDao.findDetail(VOLUME_ID, VolumeInfo.CLVM_LOCK_HOST_ID)).thenReturn(detail);

        Long result = clvmLockManager.getClvmLockHostId(VOLUME_ID, VOLUME_UUID);

        Assert.assertNull(result);
    }

    @Test
    public void testSetClvmLockHostId_NewDetail() {
        when(volsDetailsDao.findDetail(VOLUME_ID, VolumeInfo.CLVM_LOCK_HOST_ID)).thenReturn(null);

        clvmLockManager.setClvmLockHostId(VOLUME_ID, HOST_ID_1);

        verify(volsDetailsDao, times(1)).addDetail(eq(VOLUME_ID), eq(VolumeInfo.CLVM_LOCK_HOST_ID),
                eq(String.valueOf(HOST_ID_1)), eq(false));
        verify(volsDetailsDao, never()).update(anyLong(), any());
    }

    @Test
    public void testSetClvmLockHostId_UpdateExisting() {
        VolumeDetailVO existingDetail = Mockito.mock(VolumeDetailVO.class);
        when(existingDetail.getId()).thenReturn(50L);
        when(volsDetailsDao.findDetail(VOLUME_ID, VolumeInfo.CLVM_LOCK_HOST_ID)).thenReturn(existingDetail);

        clvmLockManager.setClvmLockHostId(VOLUME_ID, HOST_ID_2);

        verify(existingDetail, times(1)).setValue(String.valueOf(HOST_ID_2));
        verify(volsDetailsDao, times(1)).update(eq(50L), eq(existingDetail));
        verify(volsDetailsDao, never()).addDetail(anyLong(), any(), any(), Mockito.anyBoolean());
    }

    @Test
    public void testClearClvmLockHostDetail_Success() {
        VolumeVO volume = Mockito.mock(VolumeVO.class);
        when(volume.getId()).thenReturn(VOLUME_ID);
        when(volume.getUuid()).thenReturn(VOLUME_UUID);

        VolumeDetailVO detail = Mockito.mock(VolumeDetailVO.class);
        when(detail.getId()).thenReturn(99L);
        when(volsDetailsDao.findDetail(VOLUME_ID, VolumeInfo.CLVM_LOCK_HOST_ID)).thenReturn(detail);

        clvmLockManager.clearClvmLockHostDetail(volume);

        verify(volsDetailsDao, times(1)).remove(99L);
    }

    @Test
    public void testClearClvmLockHostDetail_NoDetail() {
        VolumeVO volume = Mockito.mock(VolumeVO.class);
        when(volume.getId()).thenReturn(VOLUME_ID);
        when(volsDetailsDao.findDetail(VOLUME_ID, VolumeInfo.CLVM_LOCK_HOST_ID)).thenReturn(null);

        clvmLockManager.clearClvmLockHostDetail(volume);

        verify(volsDetailsDao, never()).remove(anyLong());
    }

    @Test
    public void testTransferClvmVolumeLock_Success() throws AgentUnavailableException, OperationTimedoutException {
        StoragePoolVO pool = Mockito.mock(StoragePoolVO.class);
        when(pool.getPath()).thenReturn("/" + VG_NAME);

        HostVO sourceHost = Mockito.mock(HostVO.class);
        when(sourceHost.getStatus()).thenReturn(Status.Up);
        when(hostDao.findById(HOST_ID_1)).thenReturn(sourceHost);

        Answer deactivateAnswer = new Answer(null, true, null);
        Answer activateAnswer = new Answer(null, true, null);

        when(agentMgr.send(eq(HOST_ID_1), any(ClvmLockTransferCommand.class))).thenReturn(deactivateAnswer);
        when(agentMgr.send(eq(HOST_ID_2), any(ClvmLockTransferCommand.class))).thenReturn(activateAnswer);

        boolean result = clvmLockManager.transferClvmVolumeLock(VOLUME_UUID, VOLUME_ID,
                VOLUME_PATH, pool, HOST_ID_1, HOST_ID_2);

        Assert.assertTrue(result);
        verify(agentMgr, times(2)).send(anyLong(), any(ClvmLockTransferCommand.class));
    }

    @Test
    public void testTransferClvmVolumeLock_NullPool() {
        boolean result = clvmLockManager.transferClvmVolumeLock(VOLUME_UUID, VOLUME_ID,
                VOLUME_PATH, null, HOST_ID_1, HOST_ID_2);

        Assert.assertFalse(result);
    }

    @Test
    public void testTransferClvmVolumeLock_SameHost() throws AgentUnavailableException, OperationTimedoutException {
        StoragePoolVO pool = Mockito.mock(StoragePoolVO.class);
        when(pool.getPath()).thenReturn("/" + VG_NAME);

        Answer activateAnswer = new Answer(null, true, null);
        when(agentMgr.send(eq(HOST_ID_1), any(ClvmLockTransferCommand.class))).thenReturn(activateAnswer);

        boolean result = clvmLockManager.transferClvmVolumeLock(VOLUME_UUID, VOLUME_ID,
                VOLUME_PATH, pool, HOST_ID_1, HOST_ID_1);

        Assert.assertTrue(result);
        verify(agentMgr, times(1)).send(anyLong(), any(ClvmLockTransferCommand.class));
    }

    @Test
    public void testTransferClvmVolumeLock_ActivationFails() throws AgentUnavailableException, OperationTimedoutException {
        StoragePoolVO pool = Mockito.mock(StoragePoolVO.class);
        when(pool.getPath()).thenReturn(VG_NAME);

        Answer activateAnswer = new Answer(null, false, "Activation failed");
        when(agentMgr.send(eq(HOST_ID_1), any(ClvmLockTransferCommand.class))).thenReturn(activateAnswer);

        boolean result = clvmLockManager.transferClvmVolumeLock(VOLUME_UUID, VOLUME_ID,
                VOLUME_PATH, pool, HOST_ID_1, HOST_ID_1);

        Assert.assertFalse(result);
    }

    @Test
    public void testTransferClvmVolumeLock_AgentUnavailable() throws AgentUnavailableException, OperationTimedoutException {
        StoragePoolVO pool = Mockito.mock(StoragePoolVO.class);
        when(pool.getPath()).thenReturn(VG_NAME);

        when(agentMgr.send(anyLong(), any(ClvmLockTransferCommand.class)))
                .thenThrow(new AgentUnavailableException("Agent unavailable", HOST_ID_2));

        boolean result = clvmLockManager.transferClvmVolumeLock(VOLUME_UUID, VOLUME_ID,
                VOLUME_PATH, pool, HOST_ID_1, HOST_ID_2);

        Assert.assertFalse(result);
    }

    @Test
    public void testQueryCurrentLockHolder_NullPool() {
        Long result = clvmLockManager.queryCurrentLockHolder(VOLUME_ID, VOLUME_UUID, VOLUME_PATH, null, false);

        Assert.assertNull(result);
        verify(hostDao, never()).findByClusterId(anyLong(), any());
    }

    @Test
    public void testQueryCurrentLockHolder_NoHostsInCluster() {
        StoragePoolVO pool = Mockito.mock(StoragePoolVO.class);
        when(pool.getClusterId()).thenReturn(10L);
        when(pool.getDataCenterId()).thenReturn(1L);
        when(pool.getName()).thenReturn("test-pool");
        when(hostDao.findByClusterId(10L, Host.Type.Routing)).thenReturn(Collections.emptyList());
        when(hostDao.findByDataCenterId(1L)).thenReturn(Collections.emptyList());

        Long result = clvmLockManager.queryCurrentLockHolder(VOLUME_ID, VOLUME_UUID, VOLUME_PATH, pool, false);

        Assert.assertNull(result);
        verify(hostDao, times(1)).findByClusterId(10L, Host.Type.Routing);
        verify(hostDao, times(1)).findByDataCenterId(1L);
    }

    @Test
    public void testQueryCurrentLockHolder_ZoneScopedPool() throws AgentUnavailableException, OperationTimedoutException {
        StoragePoolVO pool = Mockito.mock(StoragePoolVO.class);
        when(pool.getClusterId()).thenReturn(null);
        when(pool.getDataCenterId()).thenReturn(1L);
        when(pool.getName()).thenReturn("zone-pool");
        when(pool.getPath()).thenReturn(VG_NAME);

        HostVO host = createMockHost(HOST_ID_1, "host1", Status.Up, Hypervisor.HypervisorType.KVM);
        when(hostDao.findByDataCenterId(1L)).thenReturn(Collections.singletonList(host));

        ClvmLockTransferAnswer answer = new ClvmLockTransferAnswer(null, true, null, "host1", true, false, null);
        when(agentMgr.send(eq(HOST_ID_1), any(ClvmLockTransferCommand.class))).thenReturn(answer);
        when(hostDao.findByName("host1")).thenReturn(host);

        Long result = clvmLockManager.queryCurrentLockHolder(VOLUME_ID, VOLUME_UUID, VOLUME_PATH, pool, false);

        Assert.assertEquals(HOST_ID_1, result);
        verify(hostDao, never()).findByClusterId(anyLong(), any());
        verify(hostDao, times(1)).findByDataCenterId(1L);
    }

    @Test
    public void testQueryCurrentLockHolder_SuccessfulQuery() throws AgentUnavailableException, OperationTimedoutException {
        StoragePoolVO pool = Mockito.mock(StoragePoolVO.class);
        when(pool.getClusterId()).thenReturn(10L);
        when(pool.getName()).thenReturn("cluster-pool");
        when(pool.getPath()).thenReturn(VG_NAME);

        HostVO host = createMockHost(HOST_ID_1, "host1", Status.Up, Hypervisor.HypervisorType.KVM);
        when(hostDao.findByClusterId(10L, Host.Type.Routing)).thenReturn(Collections.singletonList(host));

        ClvmLockTransferAnswer answer = new ClvmLockTransferAnswer(null, true, null, "host1", true, false, null);
        when(agentMgr.send(eq(HOST_ID_1), any(ClvmLockTransferCommand.class))).thenReturn(answer);
        when(hostDao.findByName("host1")).thenReturn(host);

        Long result = clvmLockManager.queryCurrentLockHolder(VOLUME_ID, VOLUME_UUID, VOLUME_PATH, pool, false);

        Assert.assertEquals(HOST_ID_1, result);
        verify(agentMgr, times(1)).send(eq(HOST_ID_1), any(ClvmLockTransferCommand.class));
    }

    @Test
    public void testQueryCurrentLockHolder_VolumeNotLocked() throws AgentUnavailableException, OperationTimedoutException {
        StoragePoolVO pool = Mockito.mock(StoragePoolVO.class);
        when(pool.getClusterId()).thenReturn(10L);
        when(pool.getName()).thenReturn("cluster-pool");
        when(pool.getPath()).thenReturn(VG_NAME);

        HostVO host = createMockHost(HOST_ID_1, "host1", Status.Up, Hypervisor.HypervisorType.KVM);
        when(hostDao.findByClusterId(10L, Host.Type.Routing)).thenReturn(Collections.singletonList(host));

        ClvmLockTransferAnswer answer = new ClvmLockTransferAnswer(null, true, null, null, false, false, null);
        when(agentMgr.send(eq(HOST_ID_1), any(ClvmLockTransferCommand.class))).thenReturn(answer);

        Long result = clvmLockManager.queryCurrentLockHolder(VOLUME_ID, VOLUME_UUID, VOLUME_PATH, pool, false);

        Assert.assertNull(result);
    }

    @Test
    public void testQueryCurrentLockHolder_EmptyHostname() throws AgentUnavailableException, OperationTimedoutException {
        StoragePoolVO pool = Mockito.mock(StoragePoolVO.class);
        when(pool.getClusterId()).thenReturn(10L);
        when(pool.getName()).thenReturn("cluster-pool");
        when(pool.getPath()).thenReturn(VG_NAME);

        HostVO host = createMockHost(HOST_ID_1, "host1", Status.Up, Hypervisor.HypervisorType.KVM);
        when(hostDao.findByClusterId(10L, Host.Type.Routing)).thenReturn(Collections.singletonList(host));

        ClvmLockTransferAnswer answer = new ClvmLockTransferAnswer(null, true, null, "", false, false, null);
        when(agentMgr.send(eq(HOST_ID_1), any(ClvmLockTransferCommand.class))).thenReturn(answer);

        Long result = clvmLockManager.queryCurrentLockHolder(VOLUME_ID, VOLUME_UUID, VOLUME_PATH, pool, false);

        Assert.assertNull(result);
    }

    @Test
    public void testQueryCurrentLockHolder_HostnameNotResolved() throws AgentUnavailableException, OperationTimedoutException {
        StoragePoolVO pool = Mockito.mock(StoragePoolVO.class);
        when(pool.getClusterId()).thenReturn(10L);
        when(pool.getName()).thenReturn("cluster-pool");
        when(pool.getPath()).thenReturn(VG_NAME);

        HostVO host = createMockHost(HOST_ID_1, "host1", Status.Up, Hypervisor.HypervisorType.KVM);
        when(hostDao.findByClusterId(10L, Host.Type.Routing)).thenReturn(Collections.singletonList(host));

        ClvmLockTransferAnswer answer = new ClvmLockTransferAnswer(null, true, null, "unknown-host", true, false, null);
        when(agentMgr.send(eq(HOST_ID_1), any(ClvmLockTransferCommand.class))).thenReturn(answer);
        when(hostDao.findByName("unknown-host")).thenReturn(null);

        Long result = clvmLockManager.queryCurrentLockHolder(VOLUME_ID, VOLUME_UUID, VOLUME_PATH, pool, false);

        Assert.assertNull(result);
    }

    @Test
    public void testQueryCurrentLockHolder_QueryFails() throws AgentUnavailableException, OperationTimedoutException {
        StoragePoolVO pool = Mockito.mock(StoragePoolVO.class);
        when(pool.getClusterId()).thenReturn(10L);
        when(pool.getName()).thenReturn("cluster-pool");
        when(pool.getPath()).thenReturn(VG_NAME);

        HostVO host = createMockHost(HOST_ID_1, "host1", Status.Up, Hypervisor.HypervisorType.KVM);
        when(hostDao.findByClusterId(10L, Host.Type.Routing)).thenReturn(Collections.singletonList(host));

        Answer failedAnswer = new Answer(null, false, "Query failed");
        when(agentMgr.send(eq(HOST_ID_1), any(ClvmLockTransferCommand.class))).thenReturn(failedAnswer);

        Long result = clvmLockManager.queryCurrentLockHolder(VOLUME_ID, VOLUME_UUID, VOLUME_PATH, pool, false);

        Assert.assertNull(result);
    }

    @Test
    public void testQueryCurrentLockHolder_NullAnswer() throws AgentUnavailableException, OperationTimedoutException {
        StoragePoolVO pool = Mockito.mock(StoragePoolVO.class);
        when(pool.getClusterId()).thenReturn(10L);
        when(pool.getName()).thenReturn("cluster-pool");
        when(pool.getPath()).thenReturn(VG_NAME);

        HostVO host = createMockHost(HOST_ID_1, "host1", Status.Up, Hypervisor.HypervisorType.KVM);
        when(hostDao.findByClusterId(10L, Host.Type.Routing)).thenReturn(Collections.singletonList(host));

        when(agentMgr.send(eq(HOST_ID_1), any(ClvmLockTransferCommand.class))).thenReturn(null);

        Long result = clvmLockManager.queryCurrentLockHolder(VOLUME_ID, VOLUME_UUID, VOLUME_PATH, pool, false);

        Assert.assertNull(result);
    }

    @Test
    public void testQueryCurrentLockHolder_AgentUnavailableException() throws AgentUnavailableException, OperationTimedoutException {
        StoragePoolVO pool = Mockito.mock(StoragePoolVO.class);
        when(pool.getClusterId()).thenReturn(10L);
        when(pool.getName()).thenReturn("cluster-pool");
        when(pool.getPath()).thenReturn(VG_NAME);

        HostVO host = createMockHost(HOST_ID_1, "host1", Status.Up, Hypervisor.HypervisorType.KVM);
        when(hostDao.findByClusterId(10L, Host.Type.Routing)).thenReturn(Collections.singletonList(host));

        when(agentMgr.send(eq(HOST_ID_1), any(ClvmLockTransferCommand.class)))
                .thenThrow(new AgentUnavailableException("Host unavailable", HOST_ID_1));

        Long result = clvmLockManager.queryCurrentLockHolder(VOLUME_ID, VOLUME_UUID, VOLUME_PATH, pool, false);

        Assert.assertNull(result);
    }

    @Test
    public void testQueryCurrentLockHolder_OperationTimedoutException() throws AgentUnavailableException, OperationTimedoutException {
        StoragePoolVO pool = Mockito.mock(StoragePoolVO.class);
        when(pool.getClusterId()).thenReturn(10L);
        when(pool.getName()).thenReturn("cluster-pool");
        when(pool.getPath()).thenReturn(VG_NAME);

        HostVO host = createMockHost(HOST_ID_1, "host1", Status.Up, Hypervisor.HypervisorType.KVM);
        when(hostDao.findByClusterId(10L, Host.Type.Routing)).thenReturn(Collections.singletonList(host));

        when(agentMgr.send(eq(HOST_ID_1), any(ClvmLockTransferCommand.class)))
                .thenThrow(new OperationTimedoutException(null, HOST_ID_1, 0, 0, false));

        Long result = clvmLockManager.queryCurrentLockHolder(VOLUME_ID, VOLUME_UUID, VOLUME_PATH, pool, false);

        Assert.assertNull(result);
    }

    @Test
    public void testQueryCurrentLockHolder_UpdateDatabase_MatchingValue() throws AgentUnavailableException, OperationTimedoutException {
        StoragePoolVO pool = Mockito.mock(StoragePoolVO.class);
        when(pool.getClusterId()).thenReturn(10L);
        when(pool.getName()).thenReturn("cluster-pool");
        when(pool.getPath()).thenReturn(VG_NAME);

        HostVO host = createMockHost(HOST_ID_1, "host1", Status.Up, Hypervisor.HypervisorType.KVM);
        when(hostDao.findByClusterId(10L, Host.Type.Routing)).thenReturn(Collections.singletonList(host));

        VolumeDetailVO detail = new VolumeDetailVO();
        detail.setValue(String.valueOf(HOST_ID_1));
        when(volsDetailsDao.findDetail(VOLUME_ID, VolumeInfo.CLVM_LOCK_HOST_ID)).thenReturn(detail);

        ClvmLockTransferAnswer answer = new ClvmLockTransferAnswer(null, true, null, "host1", true, false, null);
        when(agentMgr.send(eq(HOST_ID_1), any(ClvmLockTransferCommand.class))).thenReturn(answer);
        when(hostDao.findByName("host1")).thenReturn(host);

        Long result = clvmLockManager.queryCurrentLockHolder(VOLUME_ID, VOLUME_UUID, VOLUME_PATH, pool, true);

        Assert.assertEquals(HOST_ID_1, result);
        verify(volsDetailsDao, never()).update(anyLong(), any());
        verify(volsDetailsDao, never()).addDetail(anyLong(), any(), any(), Mockito.anyBoolean());
    }

    @Test
    public void testQueryCurrentLockHolder_UpdateDatabase_DifferentValue() throws AgentUnavailableException, OperationTimedoutException {
        StoragePoolVO pool = Mockito.mock(StoragePoolVO.class);
        when(pool.getClusterId()).thenReturn(10L);
        when(pool.getName()).thenReturn("cluster-pool");
        when(pool.getPath()).thenReturn(VG_NAME);

        HostVO host = createMockHost(HOST_ID_2, "host2", Status.Up, Hypervisor.HypervisorType.KVM);
        when(hostDao.findByClusterId(10L, Host.Type.Routing)).thenReturn(Collections.singletonList(host));

        VolumeDetailVO detail = Mockito.mock(VolumeDetailVO.class);

        detail.setValue(String.valueOf(HOST_ID_1));
        when(volsDetailsDao.findDetail(VOLUME_ID, VolumeInfo.CLVM_LOCK_HOST_ID)).thenReturn(detail);
        when(detail.getId()).thenReturn(99L);

        ClvmLockTransferAnswer answer = new ClvmLockTransferAnswer(null, true, null, "host2", true, false, null);
        when(agentMgr.send(eq(HOST_ID_2), any(ClvmLockTransferCommand.class))).thenReturn(answer);
        when(hostDao.findByName("host2")).thenReturn(host);

        Long result = clvmLockManager.queryCurrentLockHolder(VOLUME_ID, VOLUME_UUID, VOLUME_PATH, pool, true);

        Assert.assertEquals(HOST_ID_2, result);
        verify(detail, times(1)).setValue(String.valueOf(HOST_ID_2));
        verify(volsDetailsDao, times(1)).update(eq(99L), eq(detail));
    }

    @Test
    public void testQueryCurrentLockHolder_UpdateDatabase_NoExistingDetail() throws AgentUnavailableException, OperationTimedoutException {
        StoragePoolVO pool = Mockito.mock(StoragePoolVO.class);
        when(pool.getClusterId()).thenReturn(10L);
        when(pool.getName()).thenReturn("cluster-pool");
        when(pool.getPath()).thenReturn(VG_NAME);

        HostVO host = createMockHost(HOST_ID_1, "host1", Status.Up, Hypervisor.HypervisorType.KVM);
        when(hostDao.findByClusterId(10L, Host.Type.Routing)).thenReturn(Collections.singletonList(host));

        when(volsDetailsDao.findDetail(VOLUME_ID, VolumeInfo.CLVM_LOCK_HOST_ID)).thenReturn(null);

        ClvmLockTransferAnswer answer = new ClvmLockTransferAnswer(null, true, null, "host1", true, false, null);
        when(agentMgr.send(eq(HOST_ID_1), any(ClvmLockTransferCommand.class))).thenReturn(answer);
        when(hostDao.findByName("host1")).thenReturn(host);

        Long result = clvmLockManager.queryCurrentLockHolder(VOLUME_ID, VOLUME_UUID, VOLUME_PATH, pool, true);

        Assert.assertEquals(HOST_ID_1, result);
        verify(volsDetailsDao, times(1)).addDetail(eq(VOLUME_ID), eq(VolumeInfo.CLVM_LOCK_HOST_ID),
                eq(String.valueOf(HOST_ID_1)), eq(false));
    }

    @Test
    public void testQueryCurrentLockHolder_UpdateDatabase_RemoveDetailWhenUnlocked() throws AgentUnavailableException, OperationTimedoutException {
        StoragePoolVO pool = Mockito.mock(StoragePoolVO.class);
        when(pool.getClusterId()).thenReturn(10L);
        when(pool.getName()).thenReturn("cluster-pool");
        when(pool.getPath()).thenReturn(VG_NAME);

        HostVO host = createMockHost(HOST_ID_1, "host1", Status.Up, Hypervisor.HypervisorType.KVM);
        when(hostDao.findByClusterId(10L, Host.Type.Routing)).thenReturn(Collections.singletonList(host));

        VolumeDetailVO detail = Mockito.mock(VolumeDetailVO.class);
        when(detail.getId()).thenReturn(99L);
        when(volsDetailsDao.findDetail(VOLUME_ID, VolumeInfo.CLVM_LOCK_HOST_ID)).thenReturn(detail);

        ClvmLockTransferAnswer answer = new ClvmLockTransferAnswer(null, true, null, null, false, false, null);
        when(agentMgr.send(eq(HOST_ID_1), any(ClvmLockTransferCommand.class))).thenReturn(answer);

        Long result = clvmLockManager.queryCurrentLockHolder(VOLUME_ID, VOLUME_UUID, VOLUME_PATH, pool, true);

        Assert.assertNull(result);
        verify(volsDetailsDao, times(1)).remove(99L);
    }

    @Test
    public void testQueryCurrentLockHolder_SkipsNonKVMHosts() throws AgentUnavailableException, OperationTimedoutException {
        StoragePoolVO pool = Mockito.mock(StoragePoolVO.class);
        when(pool.getClusterId()).thenReturn(10L);
        when(pool.getName()).thenReturn("cluster-pool");
        when(pool.getPath()).thenReturn(VG_NAME);

        HostVO xenHost = createMockHost(10L, "xen-host", Status.Up, Hypervisor.HypervisorType.XenServer);
        HostVO kvmHost = createMockHost(HOST_ID_1, "kvm-host", Status.Up, Hypervisor.HypervisorType.KVM);
        when(hostDao.findByClusterId(10L, Host.Type.Routing)).thenReturn(Arrays.asList(xenHost, kvmHost));

        ClvmLockTransferAnswer answer = new ClvmLockTransferAnswer(null, true, null, "kvm-host", true, false, null);
        when(agentMgr.send(eq(HOST_ID_1), any(ClvmLockTransferCommand.class))).thenReturn(answer);
        when(hostDao.findByName("kvm-host")).thenReturn(kvmHost);

        Long result = clvmLockManager.queryCurrentLockHolder(VOLUME_ID, VOLUME_UUID, VOLUME_PATH, pool, false);

        Assert.assertEquals(HOST_ID_1, result);
        verify(agentMgr, never()).send(eq(10L), any(ClvmLockTransferCommand.class));
        verify(agentMgr, times(1)).send(eq(HOST_ID_1), any(ClvmLockTransferCommand.class));
    }

    @Test
    public void testQueryCurrentLockHolder_SkipsDownHosts() throws AgentUnavailableException, OperationTimedoutException {
        StoragePoolVO pool = Mockito.mock(StoragePoolVO.class);
        when(pool.getClusterId()).thenReturn(10L);
        when(pool.getName()).thenReturn("cluster-pool");
        when(pool.getPath()).thenReturn(VG_NAME);

        HostVO downHost = createMockHost(10L, "down-host", Status.Down, Hypervisor.HypervisorType.KVM);
        HostVO upHost = createMockHost(HOST_ID_1, "up-host", Status.Up, Hypervisor.HypervisorType.KVM);
        when(hostDao.findByClusterId(10L, Host.Type.Routing)).thenReturn(Arrays.asList(downHost, upHost));

        ClvmLockTransferAnswer answer = new ClvmLockTransferAnswer(null, true, null, "up-host", true, false, null);
        when(agentMgr.send(eq(HOST_ID_1), any(ClvmLockTransferCommand.class))).thenReturn(answer);
        when(hostDao.findByName("up-host")).thenReturn(upHost);

        Long result = clvmLockManager.queryCurrentLockHolder(VOLUME_ID, VOLUME_UUID, VOLUME_PATH, pool, false);

        Assert.assertEquals(HOST_ID_1, result);
        verify(agentMgr, never()).send(eq(10L), any(ClvmLockTransferCommand.class));
        verify(agentMgr, times(1)).send(eq(HOST_ID_1), any(ClvmLockTransferCommand.class));
    }

    @Test
    public void testQueryCurrentLockHolder_PathWithLeadingSlash() throws AgentUnavailableException, OperationTimedoutException {
        StoragePoolVO pool = Mockito.mock(StoragePoolVO.class);
        when(pool.getClusterId()).thenReturn(10L);
        when(pool.getName()).thenReturn("cluster-pool");
        when(pool.getPath()).thenReturn("/" + VG_NAME);

        HostVO host = createMockHost(HOST_ID_1, "host1", Status.Up, Hypervisor.HypervisorType.KVM);
        when(hostDao.findByClusterId(10L, Host.Type.Routing)).thenReturn(Collections.singletonList(host));

        ClvmLockTransferAnswer answer = new ClvmLockTransferAnswer(null, true, null, "host1", true, false, null);
        when(agentMgr.send(eq(HOST_ID_1), any(ClvmLockTransferCommand.class))).thenReturn(answer);
        when(hostDao.findByName("host1")).thenReturn(host);

        Long result = clvmLockManager.queryCurrentLockHolder(VOLUME_ID, VOLUME_UUID, VOLUME_PATH, pool, false);

        Assert.assertEquals(HOST_ID_1, result);
    }
    // Helper method to create mock hosts
    private HostVO createMockHost(Long id, String name, Status status, Hypervisor.HypervisorType hypervisor) {
        HostVO host = Mockito.mock(HostVO.class);
        Mockito.lenient().when(host.getId()).thenReturn(id);
        Mockito.lenient().when(host.getName()).thenReturn(name);
        Mockito.lenient().when(host.getStatus()).thenReturn(status);
        Mockito.lenient().when(host.getType()).thenReturn(Host.Type.Routing);
        Mockito.lenient().when(host.getHypervisorType()).thenReturn(hypervisor);
        return host;
    }
}

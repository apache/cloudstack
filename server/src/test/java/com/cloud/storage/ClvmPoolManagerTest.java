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
import com.cloud.agent.api.Command;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.storage.clvm.ClvmPoolManager;
import com.cloud.storage.dao.VolumeDetailsDao;
import org.apache.cloudstack.storage.clvm.command.ClvmLockTransferAnswer;
import org.apache.cloudstack.storage.clvm.command.ClvmLockTransferCommand;
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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ClvmPoolManagerTest {

    @Mock
    private VolumeDetailsDao volsDetailsDao;

    @Mock
    private AgentManager agentMgr;

    @Mock
    private HostDao hostDao;

    @InjectMocks
    private ClvmPoolManager clvmPoolManager;

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
        when(volsDetailsDao.findDetail(VOLUME_ID, ClvmPoolManager.CLVM_LOCK_HOST_ID)).thenReturn(detail);

        Long result = clvmPoolManager.getClvmLockHostId(VOLUME_ID, VOLUME_UUID);

        Assert.assertEquals(Long.valueOf(123), result);
    }

    @Test
    public void testGetClvmLockHostId_NoDetail() {
        when(volsDetailsDao.findDetail(VOLUME_ID, ClvmPoolManager.CLVM_LOCK_HOST_ID)).thenReturn(null);

        Long result = clvmPoolManager.getClvmLockHostId(VOLUME_ID, VOLUME_UUID);

        Assert.assertNull(result);
    }

    @Test
    public void testGetClvmLockHostId_InvalidNumber() {
        VolumeDetailVO detail = new VolumeDetailVO();
        detail.setValue("invalid");
        when(volsDetailsDao.findDetail(VOLUME_ID, ClvmPoolManager.CLVM_LOCK_HOST_ID)).thenReturn(detail);

        Long result = clvmPoolManager.getClvmLockHostId(VOLUME_ID, VOLUME_UUID);

        Assert.assertNull(result);
    }

    @Test
    public void testSetClvmLockHostId_NewDetail() {
        when(volsDetailsDao.findDetail(VOLUME_ID, ClvmPoolManager.CLVM_LOCK_HOST_ID)).thenReturn(null);

        clvmPoolManager.setClvmLockHostId(VOLUME_ID, HOST_ID_1);

        verify(volsDetailsDao, times(1)).addDetail(eq(VOLUME_ID), eq(ClvmPoolManager.CLVM_LOCK_HOST_ID),
                eq(String.valueOf(HOST_ID_1)), eq(false));
        verify(volsDetailsDao, never()).update(anyLong(), any());
    }

    @Test
    public void testSetClvmLockHostId_UpdateExisting() {
        VolumeDetailVO existingDetail = Mockito.mock(VolumeDetailVO.class);
        when(existingDetail.getId()).thenReturn(50L);
        when(volsDetailsDao.findDetail(VOLUME_ID, ClvmPoolManager.CLVM_LOCK_HOST_ID)).thenReturn(existingDetail);

        clvmPoolManager.setClvmLockHostId(VOLUME_ID, HOST_ID_2);

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
        when(volsDetailsDao.findDetail(VOLUME_ID, ClvmPoolManager.CLVM_LOCK_HOST_ID)).thenReturn(detail);

        clvmPoolManager.clearClvmLockHostDetail(volume);

        verify(volsDetailsDao, times(1)).remove(99L);
    }

    @Test
    public void testClearClvmLockHostDetail_NoDetail() {
        VolumeVO volume = Mockito.mock(VolumeVO.class);
        when(volume.getId()).thenReturn(VOLUME_ID);
        when(volsDetailsDao.findDetail(VOLUME_ID, ClvmPoolManager.CLVM_LOCK_HOST_ID)).thenReturn(null);

        clvmPoolManager.clearClvmLockHostDetail(volume);

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

        boolean result = clvmPoolManager.transferClvmVolumeLock(VOLUME_UUID, VOLUME_ID,
                VOLUME_PATH, pool, HOST_ID_1, HOST_ID_2);

        Assert.assertTrue(result);
        verify(agentMgr, times(2)).send(anyLong(), any(ClvmLockTransferCommand.class));
    }

    @Test
    public void testTransferClvmVolumeLock_NullPool() {
        boolean result = clvmPoolManager.transferClvmVolumeLock(VOLUME_UUID, VOLUME_ID,
                VOLUME_PATH, null, HOST_ID_1, HOST_ID_2);

        Assert.assertFalse(result);
    }

    @Test
    public void testTransferClvmVolumeLock_SameHost() throws AgentUnavailableException, OperationTimedoutException {
        StoragePoolVO pool = Mockito.mock(StoragePoolVO.class);
        when(pool.getPath()).thenReturn("/" + VG_NAME);

        Answer activateAnswer = new Answer(null, true, null);
        when(agentMgr.send(eq(HOST_ID_1), any(ClvmLockTransferCommand.class))).thenReturn(activateAnswer);

        boolean result = clvmPoolManager.transferClvmVolumeLock(VOLUME_UUID, VOLUME_ID,
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

        boolean result = clvmPoolManager.transferClvmVolumeLock(VOLUME_UUID, VOLUME_ID,
                VOLUME_PATH, pool, HOST_ID_1, HOST_ID_1);

        Assert.assertFalse(result);
    }

    @Test
    public void testTransferClvmVolumeLock_AgentUnavailable() throws AgentUnavailableException, OperationTimedoutException {
        StoragePoolVO pool = Mockito.mock(StoragePoolVO.class);
        when(pool.getPath()).thenReturn(VG_NAME);

        when(agentMgr.send(anyLong(), any(ClvmLockTransferCommand.class)))
                .thenThrow(new AgentUnavailableException("Agent unavailable", HOST_ID_2));

        boolean result = clvmPoolManager.transferClvmVolumeLock(VOLUME_UUID, VOLUME_ID,
                VOLUME_PATH, pool, HOST_ID_1, HOST_ID_2);

        Assert.assertFalse(result);
    }

    @Test
    public void testQueryCurrentLockHolder_NullPool() {
        Long result = clvmPoolManager.queryCurrentLockHolder(VOLUME_ID, VOLUME_UUID, VOLUME_PATH, null, false);

        Assert.assertNull(result);
        verify(hostDao, never()).findByClusterId(anyLong(), any());
    }

    @Test
    public void testQueryCurrentLockHolder_NoHostsInCluster() {
        StoragePoolVO pool = Mockito.mock(StoragePoolVO.class);
        when(pool.getClusterId()).thenReturn(10L);
        when(pool.getDataCenterId()).thenReturn(1L);
        when(pool.getName()).thenReturn("test-pool");
        when(pool.getPath()).thenReturn(VG_NAME);
        when(volsDetailsDao.findDetail(VOLUME_ID, ClvmPoolManager.CLVM_LOCK_HOST_ID)).thenReturn(null);
        when(hostDao.findByClusterId(10L, Host.Type.Routing)).thenReturn(Collections.emptyList());
        lenient().when(hostDao.findByDataCenterId(1L)).thenReturn(Collections.emptyList());

        Long result = clvmPoolManager.queryCurrentLockHolder(VOLUME_ID, VOLUME_UUID, VOLUME_PATH, pool, false);

        Assert.assertNull(result);
        verify(hostDao, times(1)).findByClusterId(10L, Host.Type.Routing);
        verify(hostDao, times(0)).findByDataCenterId(1L);
    }

    @Test
    public void testQueryCurrentLockHolder_ZoneScopedPool() throws AgentUnavailableException, OperationTimedoutException {
        StoragePoolVO pool = Mockito.mock(StoragePoolVO.class);
        when(pool.getClusterId()).thenReturn(null);
        when(pool.getDataCenterId()).thenReturn(1L);
        Mockito.lenient().when(pool.getName()).thenReturn("zone-pool");
        when(pool.getPath()).thenReturn(VG_NAME);

        when(volsDetailsDao.findDetail(VOLUME_ID, ClvmPoolManager.CLVM_LOCK_HOST_ID)).thenReturn(null);

        HostVO host = createMockHost(HOST_ID_1, "host1", Status.Up, Hypervisor.HypervisorType.KVM);
        when(hostDao.findByDataCenterId(1L)).thenReturn(Collections.singletonList(host));

        ClvmLockTransferAnswer answer = new ClvmLockTransferAnswer(null, true, null, "host1", true, false, null);
        when(agentMgr.send(eq(HOST_ID_1), any(ClvmLockTransferCommand.class))).thenReturn(answer);

        Long result = clvmPoolManager.queryCurrentLockHolder(VOLUME_ID, VOLUME_UUID, VOLUME_PATH, pool, false);

        Assert.assertEquals(HOST_ID_1, result);
        verify(hostDao, never()).findByClusterId(anyLong(), any());
        verify(hostDao, times(1)).findByDataCenterId(1L);
    }

    @Test
    public void testQueryCurrentLockHolder_SuccessfulQuery() throws AgentUnavailableException, OperationTimedoutException {
        StoragePoolVO pool = Mockito.mock(StoragePoolVO.class);
        when(pool.getClusterId()).thenReturn(10L);
        Mockito.lenient().when(pool.getName()).thenReturn("cluster-pool");
        when(pool.getPath()).thenReturn(VG_NAME);

        when(volsDetailsDao.findDetail(VOLUME_ID, ClvmPoolManager.CLVM_LOCK_HOST_ID)).thenReturn(null);

        HostVO host = createMockHost(HOST_ID_1, "host1", Status.Up, Hypervisor.HypervisorType.KVM);
        when(hostDao.findByClusterId(10L, Host.Type.Routing)).thenReturn(Collections.singletonList(host));

        ClvmLockTransferAnswer answer = new ClvmLockTransferAnswer(null, true, null, "host1", true, false, null);
        when(agentMgr.send(eq(HOST_ID_1), any(ClvmLockTransferCommand.class))).thenReturn(answer);

        Long result = clvmPoolManager.queryCurrentLockHolder(VOLUME_ID, VOLUME_UUID, VOLUME_PATH, pool, false);

        Assert.assertEquals(HOST_ID_1, result);
        verify(agentMgr, times(1)).send(eq(HOST_ID_1), any(ClvmLockTransferCommand.class));
        verify(hostDao, never()).findByName(any());
    }

    @Test
    public void testQueryCurrentLockHolder_VolumeNotLocked() throws AgentUnavailableException, OperationTimedoutException {
        StoragePoolVO pool = Mockito.mock(StoragePoolVO.class);
        when(pool.getClusterId()).thenReturn(10L);
        Mockito.lenient().when(pool.getName()).thenReturn("cluster-pool");
        when(pool.getPath()).thenReturn(VG_NAME);

        when(volsDetailsDao.findDetail(VOLUME_ID, ClvmPoolManager.CLVM_LOCK_HOST_ID)).thenReturn(null);

        HostVO host = createMockHost(HOST_ID_1, "host1", Status.Up, Hypervisor.HypervisorType.KVM);
        when(hostDao.findByClusterId(10L, Host.Type.Routing)).thenReturn(Collections.singletonList(host));

        // QUERY reports inactive; ACTIVATE_EXCLUSIVE left unstubbed → null answer → recovery fails → returns null
        ClvmLockTransferAnswer inactiveAnswer = new ClvmLockTransferAnswer(null, true, null, null, false, false, null);
        when(agentMgr.send(eq(HOST_ID_1), Mockito.<Command>argThat(cmd ->
                cmd instanceof ClvmLockTransferCommand
                        && ((ClvmLockTransferCommand) cmd).getOperation()
                                == ClvmLockTransferCommand.Operation.QUERY_LOCK_STATE)))
                .thenReturn(inactiveAnswer);

        Long result = clvmPoolManager.queryCurrentLockHolder(VOLUME_ID, VOLUME_UUID, VOLUME_PATH, pool, false);

        Assert.assertNull(result);
    }

    @Test
    public void testQueryCurrentLockHolder_EmptyHostname() throws AgentUnavailableException, OperationTimedoutException {
        StoragePoolVO pool = Mockito.mock(StoragePoolVO.class);
        when(pool.getClusterId()).thenReturn(10L);
        Mockito.lenient().when(pool.getName()).thenReturn("cluster-pool");
        when(pool.getPath()).thenReturn(VG_NAME);

        when(volsDetailsDao.findDetail(VOLUME_ID, ClvmPoolManager.CLVM_LOCK_HOST_ID)).thenReturn(null);

        HostVO host = createMockHost(HOST_ID_1, "host1", Status.Up, Hypervisor.HypervisorType.KVM);
        when(hostDao.findByClusterId(10L, Host.Type.Routing)).thenReturn(Collections.singletonList(host));

        // QUERY reports inactive with empty hostname; ACTIVATE_EXCLUSIVE left unstubbed → null answer → recovery fails → returns null
        ClvmLockTransferAnswer answer = new ClvmLockTransferAnswer(null, true, null, "", false, false, null);
        when(agentMgr.send(eq(HOST_ID_1), Mockito.<Command>argThat(cmd ->
                cmd instanceof ClvmLockTransferCommand
                        && ((ClvmLockTransferCommand) cmd).getOperation()
                                == ClvmLockTransferCommand.Operation.QUERY_LOCK_STATE)))
                .thenReturn(answer);

        Long result = clvmPoolManager.queryCurrentLockHolder(VOLUME_ID, VOLUME_UUID, VOLUME_PATH, pool, false);

        Assert.assertNull(result);
    }

    @Test
    public void testQueryCurrentLockHolder_HostnameNotResolved() throws AgentUnavailableException, OperationTimedoutException {
        StoragePoolVO pool = Mockito.mock(StoragePoolVO.class);
        when(pool.getClusterId()).thenReturn(10L);
        Mockito.lenient().when(pool.getName()).thenReturn("cluster-pool");
        when(pool.getPath()).thenReturn(VG_NAME);

        when(volsDetailsDao.findDetail(VOLUME_ID, ClvmPoolManager.CLVM_LOCK_HOST_ID)).thenReturn(null);

        HostVO host = createMockHost(HOST_ID_1, "host1", Status.Up, Hypervisor.HypervisorType.KVM);
        when(hostDao.findByClusterId(10L, Host.Type.Routing)).thenReturn(Collections.singletonList(host));

        ClvmLockTransferAnswer answer = new ClvmLockTransferAnswer(null, true, null, "unknown-host", true, false, null);
        when(agentMgr.send(eq(HOST_ID_1), any(ClvmLockTransferCommand.class))).thenReturn(answer);

        Long result = clvmPoolManager.queryCurrentLockHolder(VOLUME_ID, VOLUME_UUID, VOLUME_PATH, pool, false);

        Assert.assertEquals(HOST_ID_1, result);
        verify(hostDao, never()).findByName(any());
    }

    @Test
    public void testQueryCurrentLockHolder_QueryFails() throws AgentUnavailableException, OperationTimedoutException {
        StoragePoolVO pool = Mockito.mock(StoragePoolVO.class);
        when(pool.getClusterId()).thenReturn(10L);
        Mockito.lenient().when(pool.getName()).thenReturn("cluster-pool");
        when(pool.getPath()).thenReturn(VG_NAME);

        when(volsDetailsDao.findDetail(VOLUME_ID, ClvmPoolManager.CLVM_LOCK_HOST_ID)).thenReturn(null);

        HostVO host = createMockHost(HOST_ID_1, "host1", Status.Up, Hypervisor.HypervisorType.KVM);
        when(hostDao.findByClusterId(10L, Host.Type.Routing)).thenReturn(Collections.singletonList(host));

        Answer failedAnswer = new Answer(null, false, "Query failed");
        when(agentMgr.send(eq(HOST_ID_1), any(ClvmLockTransferCommand.class))).thenReturn(failedAnswer);

        Long result = clvmPoolManager.queryCurrentLockHolder(VOLUME_ID, VOLUME_UUID, VOLUME_PATH, pool, false);

        Assert.assertNull(result);
    }

    @Test
    public void testQueryCurrentLockHolder_NullAnswer() throws AgentUnavailableException, OperationTimedoutException {
        StoragePoolVO pool = Mockito.mock(StoragePoolVO.class);
        when(pool.getClusterId()).thenReturn(10L);
        Mockito.lenient().when(pool.getName()).thenReturn("cluster-pool");
        when(pool.getPath()).thenReturn(VG_NAME);

        when(volsDetailsDao.findDetail(VOLUME_ID, ClvmPoolManager.CLVM_LOCK_HOST_ID)).thenReturn(null);

        HostVO host = createMockHost(HOST_ID_1, "host1", Status.Up, Hypervisor.HypervisorType.KVM);
        when(hostDao.findByClusterId(10L, Host.Type.Routing)).thenReturn(Collections.singletonList(host));

        when(agentMgr.send(eq(HOST_ID_1), any(ClvmLockTransferCommand.class))).thenReturn(null);

        Long result = clvmPoolManager.queryCurrentLockHolder(VOLUME_ID, VOLUME_UUID, VOLUME_PATH, pool, false);

        Assert.assertNull(result);
    }

    @Test
    public void testQueryCurrentLockHolder_AgentUnavailableException() throws AgentUnavailableException, OperationTimedoutException {
        StoragePoolVO pool = Mockito.mock(StoragePoolVO.class);
        when(pool.getClusterId()).thenReturn(10L);
        Mockito.lenient().when(pool.getName()).thenReturn("cluster-pool");
        when(pool.getPath()).thenReturn(VG_NAME);

        when(volsDetailsDao.findDetail(VOLUME_ID, ClvmPoolManager.CLVM_LOCK_HOST_ID)).thenReturn(null);

        HostVO host = createMockHost(HOST_ID_1, "host1", Status.Up, Hypervisor.HypervisorType.KVM);
        when(hostDao.findByClusterId(10L, Host.Type.Routing)).thenReturn(Collections.singletonList(host));

        when(agentMgr.send(eq(HOST_ID_1), any(ClvmLockTransferCommand.class)))
                .thenThrow(new AgentUnavailableException("Host unavailable", HOST_ID_1));

        Long result = clvmPoolManager.queryCurrentLockHolder(VOLUME_ID, VOLUME_UUID, VOLUME_PATH, pool, false);

        Assert.assertNull(result);
    }

    @Test
    public void testQueryCurrentLockHolder_OperationTimedoutException() throws AgentUnavailableException, OperationTimedoutException {
        StoragePoolVO pool = Mockito.mock(StoragePoolVO.class);
        when(pool.getClusterId()).thenReturn(10L);
        Mockito.lenient().when(pool.getName()).thenReturn("cluster-pool");
        when(pool.getPath()).thenReturn(VG_NAME);

        when(volsDetailsDao.findDetail(VOLUME_ID, ClvmPoolManager.CLVM_LOCK_HOST_ID)).thenReturn(null);

        HostVO host = createMockHost(HOST_ID_1, "host1", Status.Up, Hypervisor.HypervisorType.KVM);
        when(hostDao.findByClusterId(10L, Host.Type.Routing)).thenReturn(Collections.singletonList(host));

        when(agentMgr.send(eq(HOST_ID_1), any(ClvmLockTransferCommand.class)))
                .thenThrow(new OperationTimedoutException(null, HOST_ID_1, 0, 0, false));

        Long result = clvmPoolManager.queryCurrentLockHolder(VOLUME_ID, VOLUME_UUID, VOLUME_PATH, pool, false);

        Assert.assertNull(result);
    }

    @Test
    public void testQueryCurrentLockHolder_UpdateDatabase_MatchingValue() throws AgentUnavailableException, OperationTimedoutException {
        StoragePoolVO pool = Mockito.mock(StoragePoolVO.class);
        Mockito.lenient().when(pool.getClusterId()).thenReturn(10L);
        Mockito.lenient().when(pool.getName()).thenReturn("cluster-pool");
        when(pool.getPath()).thenReturn(VG_NAME);

        HostVO host = createMockHost(HOST_ID_1, "host1", Status.Up, Hypervisor.HypervisorType.KVM);
        Mockito.lenient().when(hostDao.findByClusterId(10L, Host.Type.Routing)).thenReturn(Collections.singletonList(host));

        // DB has correct value, fast path: query HOST_ID_1, returns active
        VolumeDetailVO detail = new VolumeDetailVO();
        detail.setValue(String.valueOf(HOST_ID_1));
        when(volsDetailsDao.findDetail(VOLUME_ID, ClvmPoolManager.CLVM_LOCK_HOST_ID)).thenReturn(detail);
        when(hostDao.findById(HOST_ID_1)).thenReturn(host);

        ClvmLockTransferAnswer answer = new ClvmLockTransferAnswer(null, true, null, "host1", true, false, null);
        when(agentMgr.send(eq(HOST_ID_1), any(ClvmLockTransferCommand.class))).thenReturn(answer);

        Long result = clvmPoolManager.queryCurrentLockHolder(VOLUME_ID, VOLUME_UUID, VOLUME_PATH, pool, true);

        Assert.assertEquals(HOST_ID_1, result);
        // Fast path succeeded - no DB write needed, no fan-out
        verify(volsDetailsDao, never()).update(anyLong(), any());
        verify(volsDetailsDao, never()).addDetail(anyLong(), any(), any(), Mockito.anyBoolean());
        verify(hostDao, never()).findByClusterId(anyLong(), any());
    }

    @Test
    public void testQueryCurrentLockHolder_UpdateDatabase_DifferentValue() throws AgentUnavailableException, OperationTimedoutException {
        StoragePoolVO pool = Mockito.mock(StoragePoolVO.class);
        when(pool.getClusterId()).thenReturn(10L);
        Mockito.lenient().when(pool.getName()).thenReturn("cluster-pool");
        when(pool.getPath()).thenReturn(VG_NAME);

        // DB says HOST_ID_1, but actual lock is on HOST_ID_2
        // Fast path: query HOST_ID_1, inactive: fall back to fan-out
        VolumeDetailVO detail = Mockito.mock(VolumeDetailVO.class);
        when(detail.getValue()).thenReturn(String.valueOf(HOST_ID_1));
        when(volsDetailsDao.findDetail(VOLUME_ID, ClvmPoolManager.CLVM_LOCK_HOST_ID)).thenReturn(detail);
        when(detail.getId()).thenReturn(99L);

        HostVO host1 = createMockHost(HOST_ID_1, "host1", Status.Up, Hypervisor.HypervisorType.KVM);
        HostVO host2 = createMockHost(HOST_ID_2, "host2", Status.Up, Hypervisor.HypervisorType.KVM);
        when(hostDao.findById(HOST_ID_1)).thenReturn(host1);
        when(hostDao.findByClusterId(10L, Host.Type.Routing)).thenReturn(Arrays.asList(host1, host2));

        // HOST_ID_1 reports inactive (fast path miss), HOST_ID_2 reports active (fan-out)
        ClvmLockTransferAnswer inactiveAnswer = new ClvmLockTransferAnswer(null, true, null, "host1", false, false, null);
        ClvmLockTransferAnswer activeAnswer   = new ClvmLockTransferAnswer(null, true, null, "host2", true,  false, null);
        when(agentMgr.send(eq(HOST_ID_1), any(ClvmLockTransferCommand.class))).thenReturn(inactiveAnswer);
        when(agentMgr.send(eq(HOST_ID_2), any(ClvmLockTransferCommand.class))).thenReturn(activeAnswer);

        Long result = clvmPoolManager.queryCurrentLockHolder(VOLUME_ID, VOLUME_UUID, VOLUME_PATH, pool, true);

        Assert.assertEquals(HOST_ID_2, result);
        // DB should be corrected to HOST_ID_2
        verify(detail, times(1)).setValue(String.valueOf(HOST_ID_2));
        verify(volsDetailsDao, times(1)).update(eq(99L), eq(detail));
    }

    @Test
    public void testQueryCurrentLockHolder_UpdateDatabase_NoExistingDetail() throws AgentUnavailableException, OperationTimedoutException {
        StoragePoolVO pool = Mockito.mock(StoragePoolVO.class);
        when(pool.getClusterId()).thenReturn(10L);
        Mockito.lenient().when(pool.getName()).thenReturn("cluster-pool");
        when(pool.getPath()).thenReturn(VG_NAME);

        // No DB record, fast path skipped, fan-out finds HOST_ID_1
        when(volsDetailsDao.findDetail(VOLUME_ID, ClvmPoolManager.CLVM_LOCK_HOST_ID)).thenReturn(null);

        HostVO host = createMockHost(HOST_ID_1, "host1", Status.Up, Hypervisor.HypervisorType.KVM);
        when(hostDao.findByClusterId(10L, Host.Type.Routing)).thenReturn(Collections.singletonList(host));

        ClvmLockTransferAnswer answer = new ClvmLockTransferAnswer(null, true, null, "host1", true, false, null);
        when(agentMgr.send(eq(HOST_ID_1), any(ClvmLockTransferCommand.class))).thenReturn(answer);

        Long result = clvmPoolManager.queryCurrentLockHolder(VOLUME_ID, VOLUME_UUID, VOLUME_PATH, pool, true);

        Assert.assertEquals(HOST_ID_1, result);
        verify(volsDetailsDao, times(1)).addDetail(eq(VOLUME_ID), eq(ClvmPoolManager.CLVM_LOCK_HOST_ID),
                eq(String.valueOf(HOST_ID_1)), eq(false));
    }

    @Test
    public void testQueryCurrentLockHolder_UpdateDatabase_RemoveDetailWhenUnlocked() throws AgentUnavailableException, OperationTimedoutException {
        StoragePoolVO pool = Mockito.mock(StoragePoolVO.class);
        when(pool.getClusterId()).thenReturn(10L);
        Mockito.lenient().when(pool.getName()).thenReturn("cluster-pool");
        when(pool.getPath()).thenReturn(VG_NAME);

        // DB has HOST_ID_1, fast path query returns inactive: fan-out also finds nothing
        VolumeDetailVO detail = Mockito.mock(VolumeDetailVO.class);
        when(detail.getId()).thenReturn(99L);
        when(detail.getValue()).thenReturn(String.valueOf(HOST_ID_1));
        when(volsDetailsDao.findDetail(VOLUME_ID, ClvmPoolManager.CLVM_LOCK_HOST_ID)).thenReturn(detail);

        HostVO host = createMockHost(HOST_ID_1, "host1", Status.Up, Hypervisor.HypervisorType.KVM);
        when(hostDao.findById(HOST_ID_1)).thenReturn(host);
        when(hostDao.findByClusterId(10L, Host.Type.Routing)).thenReturn(Collections.singletonList(host));

        // QUERY_LOCK_STATE: inactive (fast path miss; HOST_ID_1 skipped in fan-out as dbHostId)
        ClvmLockTransferAnswer inactiveAnswer = new ClvmLockTransferAnswer(null, true, null, null, false, false, null);
        when(agentMgr.send(eq(HOST_ID_1), Mockito.<Command>argThat(cmd ->
                cmd instanceof ClvmLockTransferCommand
                        && ((ClvmLockTransferCommand) cmd).getOperation()
                                == ClvmLockTransferCommand.Operation.QUERY_LOCK_STATE)))
                .thenReturn(inactiveAnswer);

        // ACTIVATE_EXCLUSIVE: recovery attempt fails — stale DB record must still be removed
        Answer failedActivation = new Answer(null, false, "Simulated activation failure for test");
        when(agentMgr.send(eq(HOST_ID_1), Mockito.<Command>argThat(cmd ->
                cmd instanceof ClvmLockTransferCommand
                        && ((ClvmLockTransferCommand) cmd).getOperation()
                                == ClvmLockTransferCommand.Operation.ACTIVATE_EXCLUSIVE)))
                .thenReturn(failedActivation);

        Long result = clvmPoolManager.queryCurrentLockHolder(VOLUME_ID, VOLUME_UUID, VOLUME_PATH, pool, true);

        Assert.assertNull(result);
        verify(volsDetailsDao, times(1)).remove(99L);
    }

    @Test
    public void testQueryCurrentLockHolder_SkipsNonKVMHosts() throws AgentUnavailableException, OperationTimedoutException {
        StoragePoolVO pool = Mockito.mock(StoragePoolVO.class);
        when(pool.getClusterId()).thenReturn(10L);
        Mockito.lenient().when(pool.getName()).thenReturn("cluster-pool");
        when(pool.getPath()).thenReturn(VG_NAME);

        when(volsDetailsDao.findDetail(VOLUME_ID, ClvmPoolManager.CLVM_LOCK_HOST_ID)).thenReturn(null);

        HostVO xenHost = createMockHost(10L, "xen-host", Status.Up, Hypervisor.HypervisorType.XenServer);
        HostVO kvmHost = createMockHost(HOST_ID_1, "kvm-host", Status.Up, Hypervisor.HypervisorType.KVM);
        when(hostDao.findByClusterId(10L, Host.Type.Routing)).thenReturn(Arrays.asList(xenHost, kvmHost));

        ClvmLockTransferAnswer answer = new ClvmLockTransferAnswer(null, true, null, "kvm-host", true, false, null);
        when(agentMgr.send(eq(HOST_ID_1), any(ClvmLockTransferCommand.class))).thenReturn(answer);

        Long result = clvmPoolManager.queryCurrentLockHolder(VOLUME_ID, VOLUME_UUID, VOLUME_PATH, pool, false);

        Assert.assertEquals(HOST_ID_1, result);
        verify(agentMgr, never()).send(eq(10L), any(ClvmLockTransferCommand.class));
        verify(agentMgr, times(1)).send(eq(HOST_ID_1), any(ClvmLockTransferCommand.class));
    }

    @Test
    public void testQueryCurrentLockHolder_SkipsDownHosts() throws AgentUnavailableException, OperationTimedoutException {
        StoragePoolVO pool = Mockito.mock(StoragePoolVO.class);
        when(pool.getClusterId()).thenReturn(10L);
        Mockito.lenient().when(pool.getName()).thenReturn("cluster-pool");
        when(pool.getPath()).thenReturn(VG_NAME);

        when(volsDetailsDao.findDetail(VOLUME_ID, ClvmPoolManager.CLVM_LOCK_HOST_ID)).thenReturn(null);

        HostVO downHost = createMockHost(10L, "down-host", Status.Down, Hypervisor.HypervisorType.KVM);
        HostVO upHost = createMockHost(HOST_ID_1, "up-host", Status.Up, Hypervisor.HypervisorType.KVM);
        when(hostDao.findByClusterId(10L, Host.Type.Routing)).thenReturn(Arrays.asList(downHost, upHost));

        ClvmLockTransferAnswer answer = new ClvmLockTransferAnswer(null, true, null, "up-host", true, false, null);
        when(agentMgr.send(eq(HOST_ID_1), any(ClvmLockTransferCommand.class))).thenReturn(answer);

        Long result = clvmPoolManager.queryCurrentLockHolder(VOLUME_ID, VOLUME_UUID, VOLUME_PATH, pool, false);

        Assert.assertEquals(HOST_ID_1, result);
        verify(agentMgr, never()).send(eq(10L), any(ClvmLockTransferCommand.class));
        verify(agentMgr, times(1)).send(eq(HOST_ID_1), any(ClvmLockTransferCommand.class));
    }

    @Test
    public void testQueryCurrentLockHolder_PathWithLeadingSlash() throws AgentUnavailableException, OperationTimedoutException {
        StoragePoolVO pool = Mockito.mock(StoragePoolVO.class);
        when(pool.getClusterId()).thenReturn(10L);
        Mockito.lenient().when(pool.getName()).thenReturn("cluster-pool");
        when(pool.getPath()).thenReturn("/" + VG_NAME);

        when(volsDetailsDao.findDetail(VOLUME_ID, ClvmPoolManager.CLVM_LOCK_HOST_ID)).thenReturn(null);

        HostVO host = createMockHost(HOST_ID_1, "host1", Status.Up, Hypervisor.HypervisorType.KVM);
        when(hostDao.findByClusterId(10L, Host.Type.Routing)).thenReturn(Collections.singletonList(host));

        ClvmLockTransferAnswer answer = new ClvmLockTransferAnswer(null, true, null, "host1", true, false, null);
        when(agentMgr.send(eq(HOST_ID_1), any(ClvmLockTransferCommand.class))).thenReturn(answer);

        Long result = clvmPoolManager.queryCurrentLockHolder(VOLUME_ID, VOLUME_UUID, VOLUME_PATH, pool, false);

        Assert.assertEquals(HOST_ID_1, result);
    }

    /**
     * Fast path: DB has the correct host, single query confirms isActive=true.
     * No fan-out should occur.
     */
    @Test
    public void testQueryCurrentLockHolder_FastPath_HitOnDbHost() throws AgentUnavailableException, OperationTimedoutException {
        StoragePoolVO pool = Mockito.mock(StoragePoolVO.class);
        when(pool.getPath()).thenReturn(VG_NAME);

        VolumeDetailVO detail = new VolumeDetailVO();
        detail.setValue(String.valueOf(HOST_ID_1));
        when(volsDetailsDao.findDetail(VOLUME_ID, ClvmPoolManager.CLVM_LOCK_HOST_ID)).thenReturn(detail);

        HostVO host = createMockHost(HOST_ID_1, "host1", Status.Up, Hypervisor.HypervisorType.KVM);
        when(hostDao.findById(HOST_ID_1)).thenReturn(host);

        ClvmLockTransferAnswer activeAnswer = new ClvmLockTransferAnswer(null, true, null, "host1", true, false, null);
        when(agentMgr.send(eq(HOST_ID_1), any(ClvmLockTransferCommand.class))).thenReturn(activeAnswer);

        Long result = clvmPoolManager.queryCurrentLockHolder(VOLUME_ID, VOLUME_UUID, VOLUME_PATH, pool, false);

        Assert.assertEquals(HOST_ID_1, result);
        // Only one agent call, no cluster host lookup, no fan-out
        verify(agentMgr, times(1)).send(eq(HOST_ID_1), any(ClvmLockTransferCommand.class));
        verify(hostDao, never()).findByClusterId(anyLong(), any());
        verify(hostDao, never()).findByDataCenterId(anyLong());
    }

    /**
     * Fast path miss: DB has HOST_ID_1 but it's inactive. Fan-out finds HOST_ID_2.
     * HOST_ID_1 should NOT be queried again during fan-out.
     */
    @Test
    public void testQueryCurrentLockHolder_FastPath_MissDbHost_FanOutFindsOther() throws AgentUnavailableException, OperationTimedoutException {
        StoragePoolVO pool = Mockito.mock(StoragePoolVO.class);
        when(pool.getClusterId()).thenReturn(10L);
        when(pool.getPath()).thenReturn(VG_NAME);

        VolumeDetailVO detail = new VolumeDetailVO();
        detail.setValue(String.valueOf(HOST_ID_1));
        when(volsDetailsDao.findDetail(VOLUME_ID, ClvmPoolManager.CLVM_LOCK_HOST_ID)).thenReturn(detail);

        HostVO host1 = createMockHost(HOST_ID_1, "host1", Status.Up, Hypervisor.HypervisorType.KVM);
        HostVO host2 = createMockHost(HOST_ID_2, "host2", Status.Up, Hypervisor.HypervisorType.KVM);
        when(hostDao.findById(HOST_ID_1)).thenReturn(host1);
        when(hostDao.findByClusterId(10L, Host.Type.Routing)).thenReturn(Arrays.asList(host1, host2));

        // Fast path: HOST_ID_1 inactive
        ClvmLockTransferAnswer inactiveAnswer = new ClvmLockTransferAnswer(null, true, null, "host1", false, false, null);
        // Fan-out: HOST_ID_2 active (HOST_ID_1 skipped)
        ClvmLockTransferAnswer activeAnswer   = new ClvmLockTransferAnswer(null, true, null, "host2", true,  false, null);
        when(agentMgr.send(eq(HOST_ID_1), any(ClvmLockTransferCommand.class))).thenReturn(inactiveAnswer);
        when(agentMgr.send(eq(HOST_ID_2), any(ClvmLockTransferCommand.class))).thenReturn(activeAnswer);

        Long result = clvmPoolManager.queryCurrentLockHolder(VOLUME_ID, VOLUME_UUID, VOLUME_PATH, pool, false);

        Assert.assertEquals(HOST_ID_2, result);
        // HOST_ID_1 queried once (fast path only), HOST_ID_2 queried once (fan-out)
        verify(agentMgr, times(1)).send(eq(HOST_ID_1), any(ClvmLockTransferCommand.class));
        verify(agentMgr, times(1)).send(eq(HOST_ID_2), any(ClvmLockTransferCommand.class));
    }

    /**
     * Fast path skip: DB host is DOWN. Fan-out proceeds to all UP hosts.
     */
    @Test
    public void testQueryCurrentLockHolder_FastPath_DbHostDown_FanOut() throws AgentUnavailableException, OperationTimedoutException {
        StoragePoolVO pool = Mockito.mock(StoragePoolVO.class);
        when(pool.getClusterId()).thenReturn(10L);
        when(pool.getPath()).thenReturn(VG_NAME);

        VolumeDetailVO detail = new VolumeDetailVO();
        detail.setValue(String.valueOf(HOST_ID_1));
        when(volsDetailsDao.findDetail(VOLUME_ID, ClvmPoolManager.CLVM_LOCK_HOST_ID)).thenReturn(detail);

        HostVO downHost = createMockHost(HOST_ID_1, "host1", Status.Down, Hypervisor.HypervisorType.KVM);
        HostVO upHost   = createMockHost(HOST_ID_2, "host2", Status.Up,   Hypervisor.HypervisorType.KVM);
        when(hostDao.findById(HOST_ID_1)).thenReturn(downHost);
        when(hostDao.findByClusterId(10L, Host.Type.Routing)).thenReturn(Arrays.asList(downHost, upHost));

        ClvmLockTransferAnswer activeAnswer = new ClvmLockTransferAnswer(null, true, null, "host2", true, false, null);
        when(agentMgr.send(eq(HOST_ID_2), any(ClvmLockTransferCommand.class))).thenReturn(activeAnswer);

        Long result = clvmPoolManager.queryCurrentLockHolder(VOLUME_ID, VOLUME_UUID, VOLUME_PATH, pool, false);

        Assert.assertEquals(HOST_ID_2, result);
        // No query to the down host at all
        verify(agentMgr, never()).send(eq(HOST_ID_1), any(ClvmLockTransferCommand.class));
        verify(agentMgr, times(1)).send(eq(HOST_ID_2), any(ClvmLockTransferCommand.class));
    }


    /**
     * Inactive everywhere, DB host is UP: recovery activates exclusively on the DB host.
     */
    @Test
    public void testQueryCurrentLockHolder_InactiveEverywhere_ActivatesOnDbHost()
            throws AgentUnavailableException, OperationTimedoutException {
        StoragePoolVO pool = Mockito.mock(StoragePoolVO.class);
        when(pool.getClusterId()).thenReturn(10L);
        Mockito.lenient().when(pool.getName()).thenReturn("cluster-pool");
        when(pool.getPath()).thenReturn(VG_NAME);

        VolumeDetailVO detail = Mockito.mock(VolumeDetailVO.class);
        when(detail.getId()).thenReturn(99L);
        when(detail.getValue()).thenReturn(String.valueOf(HOST_ID_1));
        when(volsDetailsDao.findDetail(VOLUME_ID, ClvmPoolManager.CLVM_LOCK_HOST_ID)).thenReturn(detail);

        HostVO host1 = createMockHost(HOST_ID_1, "host1", Status.Up, Hypervisor.HypervisorType.KVM);
        when(hostDao.findById(HOST_ID_1)).thenReturn(host1);
        when(hostDao.findByClusterId(10L, Host.Type.Routing)).thenReturn(Collections.singletonList(host1));

        // Fast path QUERY → inactive
        ClvmLockTransferAnswer inactiveAnswer = new ClvmLockTransferAnswer(null, true, null, null, false, false, null);
        when(agentMgr.send(eq(HOST_ID_1), Mockito.<Command>argThat(cmd ->
                cmd instanceof ClvmLockTransferCommand
                        && ((ClvmLockTransferCommand) cmd).getOperation()
                                == ClvmLockTransferCommand.Operation.QUERY_LOCK_STATE)))
                .thenReturn(inactiveAnswer);

        // Recovery ACTIVATE_EXCLUSIVE → succeeds
        Answer activateAnswer = new Answer(null, true, null);
        when(agentMgr.send(eq(HOST_ID_1), Mockito.<Command>argThat(cmd ->
                cmd instanceof ClvmLockTransferCommand
                        && ((ClvmLockTransferCommand) cmd).getOperation()
                                == ClvmLockTransferCommand.Operation.ACTIVATE_EXCLUSIVE)))
                .thenReturn(activateAnswer);

        Long result = clvmPoolManager.queryCurrentLockHolder(VOLUME_ID, VOLUME_UUID, VOLUME_PATH, pool, true);

        Assert.assertEquals(HOST_ID_1, result);
        verify(detail, times(1)).setValue(String.valueOf(HOST_ID_1));
        verify(volsDetailsDao, times(1)).update(eq(99L), eq(detail));
        verify(volsDetailsDao, never()).remove(anyLong());
    }

    /**
     * Inactive everywhere, no DB record: recovery falls back to the first UP KVM host in cluster.
     */
    @Test
    public void testQueryCurrentLockHolder_InactiveEverywhere_ActivatesOnClusterHostWhenNoDbRecord()
            throws AgentUnavailableException, OperationTimedoutException {
        StoragePoolVO pool = Mockito.mock(StoragePoolVO.class);
        when(pool.getClusterId()).thenReturn(10L);
        Mockito.lenient().when(pool.getName()).thenReturn("cluster-pool");
        when(pool.getPath()).thenReturn(VG_NAME);

        when(volsDetailsDao.findDetail(VOLUME_ID, ClvmPoolManager.CLVM_LOCK_HOST_ID)).thenReturn(null);

        HostVO host1 = createMockHost(HOST_ID_1, "host1", Status.Up, Hypervisor.HypervisorType.KVM);
        when(hostDao.findByClusterId(10L, Host.Type.Routing)).thenReturn(Collections.singletonList(host1));

        // Fan-out QUERY → inactive
        ClvmLockTransferAnswer inactiveAnswer = new ClvmLockTransferAnswer(null, true, null, null, false, false, null);
        when(agentMgr.send(eq(HOST_ID_1), Mockito.<Command>argThat(cmd ->
                cmd instanceof ClvmLockTransferCommand
                        && ((ClvmLockTransferCommand) cmd).getOperation()
                                == ClvmLockTransferCommand.Operation.QUERY_LOCK_STATE)))
                .thenReturn(inactiveAnswer);

        // Recovery ACTIVATE_EXCLUSIVE → succeeds
        Answer activateAnswer = new Answer(null, true, null);
        when(agentMgr.send(eq(HOST_ID_1), Mockito.<Command>argThat(cmd ->
                cmd instanceof ClvmLockTransferCommand
                        && ((ClvmLockTransferCommand) cmd).getOperation()
                                == ClvmLockTransferCommand.Operation.ACTIVATE_EXCLUSIVE)))
                .thenReturn(activateAnswer);

        Long result = clvmPoolManager.queryCurrentLockHolder(VOLUME_ID, VOLUME_UUID, VOLUME_PATH, pool, true);

        Assert.assertEquals(HOST_ID_1, result);
        verify(volsDetailsDao, times(1)).addDetail(eq(VOLUME_ID), eq(ClvmPoolManager.CLVM_LOCK_HOST_ID),
                eq(String.valueOf(HOST_ID_1)), eq(false));
    }

    /**
     * Inactive everywhere, DB host is DOWN: selectActivationTargetHost skips it and picks
     * the first UP host from the cluster list.
     */
    @Test
    public void testQueryCurrentLockHolder_InactiveEverywhere_SkipsDownDbHost_ActivatesOnClusterHost()
            throws AgentUnavailableException, OperationTimedoutException {
        StoragePoolVO pool = Mockito.mock(StoragePoolVO.class);
        when(pool.getClusterId()).thenReturn(10L);
        Mockito.lenient().when(pool.getName()).thenReturn("cluster-pool");
        when(pool.getPath()).thenReturn(VG_NAME);

        VolumeDetailVO detail = new VolumeDetailVO();
        detail.setValue(String.valueOf(HOST_ID_1));
        when(volsDetailsDao.findDetail(VOLUME_ID, ClvmPoolManager.CLVM_LOCK_HOST_ID)).thenReturn(detail);

        HostVO downHost = createMockHost(HOST_ID_1, "host1", Status.Down, Hypervisor.HypervisorType.KVM);
        HostVO upHost   = createMockHost(HOST_ID_2, "host2", Status.Up,   Hypervisor.HypervisorType.KVM);
        when(hostDao.findById(HOST_ID_1)).thenReturn(downHost);
        when(hostDao.findByClusterId(10L, Host.Type.Routing)).thenReturn(Arrays.asList(downHost, upHost));

        // Fan-out QUERY on HOST_ID_2 → inactive (HOST_ID_1 filtered by Status.Down)
        ClvmLockTransferAnswer inactiveAnswer = new ClvmLockTransferAnswer(null, true, null, null, false, false, null);
        when(agentMgr.send(eq(HOST_ID_2), Mockito.<Command>argThat(cmd ->
                cmd instanceof ClvmLockTransferCommand
                        && ((ClvmLockTransferCommand) cmd).getOperation()
                                == ClvmLockTransferCommand.Operation.QUERY_LOCK_STATE)))
                .thenReturn(inactiveAnswer);

        // Recovery ACTIVATE_EXCLUSIVE on HOST_ID_2 → succeeds
        Answer activateAnswer = new Answer(null, true, null);
        when(agentMgr.send(eq(HOST_ID_2), Mockito.<Command>argThat(cmd ->
                cmd instanceof ClvmLockTransferCommand
                        && ((ClvmLockTransferCommand) cmd).getOperation()
                                == ClvmLockTransferCommand.Operation.ACTIVATE_EXCLUSIVE)))
                .thenReturn(activateAnswer);

        Long result = clvmPoolManager.queryCurrentLockHolder(VOLUME_ID, VOLUME_UUID, VOLUME_PATH, pool, true);

        Assert.assertEquals(HOST_ID_2, result);
        verify(agentMgr, never()).send(eq(HOST_ID_1), any(ClvmLockTransferCommand.class));
    }

    /**
     * Inactive everywhere, recovery activation throws AgentUnavailableException: returns null,
     * no crash, no DB side-effects (updateDatabase=false).
     */
    @Test
    public void testQueryCurrentLockHolder_InactiveEverywhere_ActivationThrows_ReturnsNull()
            throws AgentUnavailableException, OperationTimedoutException {
        StoragePoolVO pool = Mockito.mock(StoragePoolVO.class);
        when(pool.getClusterId()).thenReturn(10L);
        Mockito.lenient().when(pool.getName()).thenReturn("cluster-pool");
        when(pool.getPath()).thenReturn(VG_NAME);

        when(volsDetailsDao.findDetail(VOLUME_ID, ClvmPoolManager.CLVM_LOCK_HOST_ID)).thenReturn(null);

        HostVO host1 = createMockHost(HOST_ID_1, "host1", Status.Up, Hypervisor.HypervisorType.KVM);
        when(hostDao.findByClusterId(10L, Host.Type.Routing)).thenReturn(Collections.singletonList(host1));

        ClvmLockTransferAnswer inactiveAnswer = new ClvmLockTransferAnswer(null, true, null, null, false, false, null);
        when(agentMgr.send(eq(HOST_ID_1), Mockito.<Command>argThat(cmd ->
                cmd instanceof ClvmLockTransferCommand
                        && ((ClvmLockTransferCommand) cmd).getOperation()
                                == ClvmLockTransferCommand.Operation.QUERY_LOCK_STATE)))
                .thenReturn(inactiveAnswer);

        when(agentMgr.send(eq(HOST_ID_1), Mockito.<Command>argThat(cmd ->
                cmd instanceof ClvmLockTransferCommand
                        && ((ClvmLockTransferCommand) cmd).getOperation()
                                == ClvmLockTransferCommand.Operation.ACTIVATE_EXCLUSIVE)))
                .thenThrow(new AgentUnavailableException("Host unreachable during recovery", HOST_ID_1));

        Long result = clvmPoolManager.queryCurrentLockHolder(VOLUME_ID, VOLUME_UUID, VOLUME_PATH, pool, false);

        Assert.assertNull(result);
        verify(volsDetailsDao, never()).remove(anyLong());
        verify(volsDetailsDao, never()).addDetail(anyLong(), any(), any(), Mockito.anyBoolean());
    }

    /**
     * Inactive everywhere, no UP KVM host available: selectActivationTargetHost returns null,
     * no activation is attempted, returns null immediately.
     */
    @Test
    public void testQueryCurrentLockHolder_InactiveEverywhere_NoEligibleHost_ReturnsNull()
            throws AgentUnavailableException, OperationTimedoutException {
        StoragePoolVO pool = Mockito.mock(StoragePoolVO.class);
        when(pool.getClusterId()).thenReturn(10L);
        Mockito.lenient().when(pool.getName()).thenReturn("cluster-pool");
        when(pool.getPath()).thenReturn(VG_NAME);

        when(volsDetailsDao.findDetail(VOLUME_ID, ClvmPoolManager.CLVM_LOCK_HOST_ID)).thenReturn(null);

        // Only a DOWN host exists — selectActivationTargetHost finds no eligible host
        HostVO downHost = createMockHost(HOST_ID_1, "host1", Status.Down, Hypervisor.HypervisorType.KVM);
        when(hostDao.findByClusterId(10L, Host.Type.Routing)).thenReturn(Collections.singletonList(downHost));

        Long result = clvmPoolManager.queryCurrentLockHolder(VOLUME_ID, VOLUME_UUID, VOLUME_PATH, pool, false);

        Assert.assertNull(result);
        verify(agentMgr, never()).send(anyLong(), any(ClvmLockTransferCommand.class));
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

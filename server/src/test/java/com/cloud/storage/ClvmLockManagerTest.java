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
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.dao.HostDao;
import com.cloud.storage.dao.VolumeDetailsDao;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
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
        // Reset mocks before each test
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
}

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
package org.apache.cloudstack.storage.listener;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.ModifyStoragePoolAnswer;
import com.cloud.agent.api.StoragePoolInfo;
import com.cloud.alert.AlertManager;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.storage.StoragePoolHostVO;
import com.cloud.storage.dao.StoragePoolHostDao;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolDetailsDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class OntapHostListenerTest {

    private static final long HOST_ID = 1L;
    private static final long POOL_ID = 2L;
    private static final String LOCAL_PATH = "/mnt/ontap/vol1";

    @Mock
    private AgentManager _agentMgr;
    @Mock
    private AlertManager _alertMgr;
    @Mock
    private PrimaryDataStoreDao _storagePoolDao;
    @Mock
    private HostDao _hostDao;
    @Mock
    private StoragePoolHostDao storagePoolHostDao;
    @Mock
    private StoragePoolDetailsDao _storagePoolDetailsDao;

    @Mock
    private HostVO host;
    @Mock
    private StoragePoolVO pool;

    private OntapHostListener listener;

    @BeforeEach
    void setUp() {
        listener = new OntapHostListener();
        ReflectionTestUtils.setField(listener, "_agentMgr", _agentMgr);
        ReflectionTestUtils.setField(listener, "_alertMgr", _alertMgr);
        ReflectionTestUtils.setField(listener, "_storagePoolDao", _storagePoolDao);
        ReflectionTestUtils.setField(listener, "_hostDao", _hostDao);
        ReflectionTestUtils.setField(listener, "storagePoolHostDao", storagePoolHostDao);
        ReflectionTestUtils.setField(listener, "_storagePoolDetailsDao", _storagePoolDetailsDao);
    }

    private void setupValidHostAndPool() {
        when(_hostDao.findById(HOST_ID)).thenReturn(host);
        when(host.getHypervisorType()).thenReturn(HypervisorType.KVM);
        when(_storagePoolDao.findById(POOL_ID)).thenReturn(pool);
        when(_storagePoolDetailsDao.listDetailsKeyPairs(POOL_ID)).thenReturn(new HashMap<>());
    }

    private ModifyStoragePoolAnswer mockSuccessfulAnswer(long capacityBytes, long availableBytes) {
        StoragePoolInfo poolInfo = mock(StoragePoolInfo.class);
        when(poolInfo.getLocalPath()).thenReturn(LOCAL_PATH);
        when(poolInfo.getCapacityBytes()).thenReturn(capacityBytes);
        when(poolInfo.getAvailableBytes()).thenReturn(availableBytes);

        ModifyStoragePoolAnswer answer = mock(ModifyStoragePoolAnswer.class);
        when(answer.getResult()).thenReturn(true);
        when(answer.getPoolInfo()).thenReturn(poolInfo);
        return answer;
    }

    // ---------------------------------------------------------------
    // hostConnect
    // ---------------------------------------------------------------

    @Test
    public void hostConnectReturnsFalseWhenHostNotFound() {
        when(_hostDao.findById(HOST_ID)).thenReturn(null);

        assertFalse(listener.hostConnect(HOST_ID, POOL_ID));

        verify(_storagePoolDao, never()).findById(anyLong());
    }

    @Test
    public void hostConnectReturnsFalseWhenHypervisorIsNotKvm() {
        when(_hostDao.findById(HOST_ID)).thenReturn(host);
        when(host.getHypervisorType()).thenReturn(HypervisorType.XenServer);

        assertFalse(listener.hostConnect(HOST_ID, POOL_ID));

        verify(_storagePoolDao, never()).findById(anyLong());
    }

    @Test
    public void hostConnectReturnsFalseWhenPoolNotFound() {
        when(_hostDao.findById(HOST_ID)).thenReturn(host);
        when(host.getHypervisorType()).thenReturn(HypervisorType.KVM);
        when(_storagePoolDao.findById(POOL_ID)).thenReturn(null);

        assertFalse(listener.hostConnect(HOST_ID, POOL_ID));

        verify(_agentMgr, never()).easySend(anyLong(), any(Command.class));
    }

    @Test
    public void hostConnectPersistsNewStoragePoolHostAndUpdatesCapacityWhenNoExistingRef() {
        setupValidHostAndPool();
        when(storagePoolHostDao.findByPoolHost(POOL_ID, HOST_ID)).thenReturn(null);
        ModifyStoragePoolAnswer answer = mockSuccessfulAnswer(1000L, 400L);
        when(_agentMgr.easySend(eq(HOST_ID), any(Command.class))).thenReturn(answer);

        assertTrue(listener.hostConnect(HOST_ID, POOL_ID));

        verify(storagePoolHostDao).persist(any(StoragePoolHostVO.class));
        verify(storagePoolHostDao, never()).update(anyLong(), any(StoragePoolHostVO.class));
        verify(pool).setCapacityBytes(1000L);
        verify(pool).setUsedBytes(600L);
        verify(_storagePoolDao).update(anyLong(), eq(pool));
    }

    @Test
    public void hostConnectUpdatesExistingStoragePoolHostRef() {
        setupValidHostAndPool();
        StoragePoolHostVO existing = mock(StoragePoolHostVO.class);
        when(existing.getId()).thenReturn(5L);
        when(storagePoolHostDao.findByPoolHost(POOL_ID, HOST_ID)).thenReturn(existing);
        ModifyStoragePoolAnswer answer = mockSuccessfulAnswer(1000L, 400L);
        when(_agentMgr.easySend(eq(HOST_ID), any(Command.class))).thenReturn(answer);

        assertTrue(listener.hostConnect(HOST_ID, POOL_ID));

        verify(existing).setLocalPath(LOCAL_PATH);
        verify(storagePoolHostDao).update(eq(5L), eq(existing));
        verify(storagePoolHostDao, never()).persist(any(StoragePoolHostVO.class));
    }

    @Test
    public void hostConnectDoesNotUpdatePoolCapacityWhenCapacityBytesIsZero() {
        setupValidHostAndPool();
        when(storagePoolHostDao.findByPoolHost(POOL_ID, HOST_ID)).thenReturn(null);
        ModifyStoragePoolAnswer answer = mockSuccessfulAnswer(0L, 0L);
        when(_agentMgr.easySend(eq(HOST_ID), any(Command.class))).thenReturn(answer);

        assertTrue(listener.hostConnect(HOST_ID, POOL_ID));

        verify(pool, never()).setCapacityBytes(anyLong());
        verify(_storagePoolDao, never()).update(anyLong(), any(StoragePoolVO.class));
    }

    @Test
    public void hostConnectReturnsFalseWhenAnswerIsNull() {
        setupValidHostAndPool();
        when(_agentMgr.easySend(eq(HOST_ID), any(Command.class))).thenReturn(null);

        assertFalse(listener.hostConnect(HOST_ID, POOL_ID));

        verify(_alertMgr, never()).sendAlert(any(), anyLong(), any(), anyString(), anyString());
    }

    @Test
    public void hostConnectSendsAlertContainingHostWhenAnswerResultIsFalse() {
        setupValidHostAndPool();
        when(host.toString()).thenReturn("Host {id=1, name=kvm-host-1}");
        ModifyStoragePoolAnswer answer = mock(ModifyStoragePoolAnswer.class);
        when(answer.getResult()).thenReturn(false);
        when(answer.getDetails()).thenReturn("agent could not mount volume");
        when(_agentMgr.easySend(eq(HOST_ID), any(Command.class))).thenReturn(answer);

        assertFalse(listener.hostConnect(HOST_ID, POOL_ID));

        ArgumentCaptor<String> subjectCaptor = ArgumentCaptor.forClass(String.class);
        verify(_alertMgr).sendAlert(eq(AlertManager.AlertType.ALERT_TYPE_HOST), anyLong(), any(),
                subjectCaptor.capture(), anyString());
        assertTrue(subjectCaptor.getValue().contains("Host {id=1, name=kvm-host-1}"));
    }

    @Test
    public void hostConnectReturnsFalseWhenAnswerIsNotModifyStoragePoolAnswer() {
        setupValidHostAndPool();
        Answer answer = mock(Answer.class);
        when(answer.getResult()).thenReturn(true);
        when(_agentMgr.easySend(eq(HOST_ID), any(Command.class))).thenReturn(answer);

        assertFalse(listener.hostConnect(HOST_ID, POOL_ID));

        verify(_alertMgr, never()).sendAlert(any(), anyLong(), any(), anyString(), anyString());
    }

    @Test
    public void hostConnectReturnsFalseWhenPoolInfoIsNull() {
        setupValidHostAndPool();
        ModifyStoragePoolAnswer answer = mock(ModifyStoragePoolAnswer.class);
        when(answer.getResult()).thenReturn(true);
        when(answer.getPoolInfo()).thenReturn(null);
        when(_agentMgr.easySend(eq(HOST_ID), any(Command.class))).thenReturn(answer);

        assertFalse(listener.hostConnect(HOST_ID, POOL_ID));

        verify(storagePoolHostDao, never()).persist(any(StoragePoolHostVO.class));
    }

    @Test
    public void hostConnectReturnsFalseWhenAgentThrowsException() {
        setupValidHostAndPool();
        when(_agentMgr.easySend(eq(HOST_ID), any(Command.class))).thenThrow(new CloudRuntimeException("agent unreachable"));

        assertFalse(listener.hostConnect(HOST_ID, POOL_ID));
    }

    // ---------------------------------------------------------------
    // hostDisconnected
    // ---------------------------------------------------------------

    @Test
    public void hostDisconnectedReturnsFalseWhenHostNotFound() {
        when(_hostDao.findById(HOST_ID)).thenReturn(null);

        assertFalse(listener.hostDisconnected(HOST_ID, POOL_ID));

        verify(_storagePoolDao, never()).findById(anyLong());
    }

    @Test
    public void hostDisconnectedReturnsFalseWhenPoolNotFound() {
        when(_hostDao.findById(HOST_ID)).thenReturn(host);
        when(_storagePoolDao.findById(POOL_ID)).thenReturn(null);

        assertFalse(listener.hostDisconnected(HOST_ID, POOL_ID));

        verify(_agentMgr, never()).easySend(anyLong(), any(Command.class));
    }

    @Test
    public void hostDisconnectedReturnsTrueOnSuccessfulAnswer() {
        when(_hostDao.findById(HOST_ID)).thenReturn(host);
        when(_storagePoolDao.findById(POOL_ID)).thenReturn(pool);
        Answer answer = mock(Answer.class);
        when(answer.getResult()).thenReturn(true);
        when(_agentMgr.easySend(eq(HOST_ID), any(Command.class))).thenReturn(answer);

        assertTrue(listener.hostDisconnected(HOST_ID, POOL_ID));
    }

    @Test
    public void hostDisconnectedReturnsFalseWhenAnswerIsNull() {
        when(_hostDao.findById(HOST_ID)).thenReturn(host);
        when(_storagePoolDao.findById(POOL_ID)).thenReturn(pool);
        when(_agentMgr.easySend(eq(HOST_ID), any(Command.class))).thenReturn(null);

        assertFalse(listener.hostDisconnected(HOST_ID, POOL_ID));
    }

    @Test
    public void hostDisconnectedReturnsFalseWhenAnswerResultIsFalse() {
        when(_hostDao.findById(HOST_ID)).thenReturn(host);
        when(_storagePoolDao.findById(POOL_ID)).thenReturn(pool);
        Answer answer = mock(Answer.class);
        when(answer.getResult()).thenReturn(false);
        when(answer.getDetails()).thenReturn("failed to unmount");
        when(_agentMgr.easySend(eq(HOST_ID), any(Command.class))).thenReturn(answer);

        assertFalse(listener.hostDisconnected(HOST_ID, POOL_ID));
    }

    @Test
    public void hostDisconnectedReturnsFalseWhenAgentThrowsException() {
        when(_hostDao.findById(HOST_ID)).thenReturn(host);
        when(_storagePoolDao.findById(POOL_ID)).thenReturn(pool);
        when(_agentMgr.easySend(eq(HOST_ID), any(Command.class))).thenThrow(new CloudRuntimeException("agent unreachable"));

        assertFalse(listener.hostDisconnected(HOST_ID, POOL_ID));
    }

    // ---------------------------------------------------------------
    // Trivial no-op overrides
    // ---------------------------------------------------------------

    @Test
    public void hostAboutToBeRemovedAlwaysReturnsFalse() {
        assertFalse(listener.hostAboutToBeRemoved(HOST_ID));
    }

    @Test
    public void hostRemovedAlwaysReturnsFalse() {
        assertFalse(listener.hostRemoved(HOST_ID, 99L));
    }

    @Test
    public void hostEnabledAlwaysReturnsFalse() {
        assertFalse(listener.hostEnabled(HOST_ID));
    }

    @Test
    public void hostAddedAlwaysReturnsFalse() {
        assertFalse(listener.hostAdded(HOST_ID));
    }
}

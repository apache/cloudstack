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
package org.apache.cloudstack.storage.datastore.provider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.CleanupPersistentNetworkResourceCommand;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.ModifyStoragePoolAnswer;
import com.cloud.agent.api.SetupPersistentNetworkCommand;
import com.cloud.agent.api.StoragePoolInfo;
import com.cloud.alert.AlertManager;
import com.cloud.configuration.ConfigurationManager;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.exception.StorageConflictException;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.network.NetworkModel;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.offerings.NetworkOfferingVO;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.Storage;
import com.cloud.storage.StorageManager;
import com.cloud.storage.StoragePoolHostVO;
import com.cloud.storage.StorageService;
import com.cloud.storage.dao.StoragePoolHostDao;
import com.cloud.utils.exception.CloudRuntimeException;

import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStore;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolDetailsDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;

@RunWith(MockitoJUnitRunner.class)
public class DefaultHostListenerTest {

    @Mock
    AgentManager agentMgr;
    @Mock
    DataStoreManager dataStoreMgr;
    @Mock
    AlertManager alertMgr;
    @Mock
    StoragePoolHostDao storagePoolHostDao;
    @Mock
    PrimaryDataStoreDao primaryStoreDao;
    @Mock
    StoragePoolDetailsDao storagePoolDetailsDao;
    @Mock
    StorageManager storageManager;
    @Mock
    StorageService storageService;
    @Mock
    DataCenterDao zoneDao;
    @Mock
    NetworkOfferingDao networkOfferingDao;
    @Mock
    HostDao hostDao;
    @Mock
    NetworkModel networkModel;
    @Mock
    ConfigurationManager configManager;
    @Mock
    NetworkDao networkDao;

    @Mock
    PrimaryDataStore pool;
    @Mock
    HostVO host;
    @Mock
    StoragePoolVO poolVO;
    @Mock
    ModifyStoragePoolAnswer mspAnswer;

    private DefaultHostListener listener;

    private static final long HOST_ID = 5L;
    private static final long POOL_ID = 10L;

    @Before
    public void setup() {
        listener = new DefaultHostListener();
        ReflectionTestUtils.setField(listener, "agentMgr", agentMgr);
        ReflectionTestUtils.setField(listener, "dataStoreMgr", dataStoreMgr);
        ReflectionTestUtils.setField(listener, "alertMgr", alertMgr);
        ReflectionTestUtils.setField(listener, "storagePoolHostDao", storagePoolHostDao);
        ReflectionTestUtils.setField(listener, "primaryStoreDao", primaryStoreDao);
        ReflectionTestUtils.setField(listener, "storagePoolDetailsDao", storagePoolDetailsDao);
        ReflectionTestUtils.setField(listener, "storageManager", storageManager);
        ReflectionTestUtils.setField(listener, "storageService", storageService);
        ReflectionTestUtils.setField(listener, "zoneDao", zoneDao);
        ReflectionTestUtils.setField(listener, "networkOfferingDao", networkOfferingDao);
        ReflectionTestUtils.setField(listener, "hostDao", hostDao);
        ReflectionTestUtils.setField(listener, "networkModel", networkModel);
        ReflectionTestUtils.setField(listener, "configManager", configManager);
        ReflectionTestUtils.setField(listener, "networkDao", networkDao);
    }

    private void setUpPoolForConnect(Storage.StoragePoolType poolType) {
        when(dataStoreMgr.getDataStore(POOL_ID, DataStoreRole.Primary)).thenReturn(pool);
        when(pool.getId()).thenReturn(POOL_ID);
        when(pool.getPoolType()).thenReturn(poolType);
        when(pool.getDataCenterId()).thenReturn(1L);
        when(pool.getPodId()).thenReturn(2L);
        when(storageManager.getStoragePoolNFSMountOpts(eq(pool), any())).thenReturn(new com.cloud.utils.Pair<>(null, false));
        when(hostDao.findById(HOST_ID)).thenReturn(host);
        when(primaryStoreDao.findById(POOL_ID)).thenReturn(poolVO);
        when(poolVO.getId()).thenReturn(POOL_ID);
    }

    private StoragePoolInfo newPoolInfo() {
        return new StoragePoolInfo("uuid", "hostAddr", "/host/path", "/local/path", Storage.StoragePoolType.NetworkFilesystem, 1000L, 500L);
    }

    // ---- hostAdded ----

    @Test
    public void hostAddedAlwaysReturnsTrue() {
        assertTrue(listener.hostAdded(123L));
    }

    // ---- hostConnect ----

    @Test
    public void hostConnectHappyPathPersistsStoragePoolHostAndSetsUpPersistentNetwork() throws StorageConflictException {
        setUpPoolForConnect(Storage.StoragePoolType.NetworkFilesystem);
        when(agentMgr.easySend(eq(HOST_ID), any(Command.class))).thenReturn(mspAnswer);
        when(mspAnswer.getResult()).thenReturn(true);
        when(mspAnswer.getPoolInfo()).thenReturn(newPoolInfo());
        when(storagePoolHostDao.findByPoolHost(POOL_ID, HOST_ID)).thenReturn(null);
        when(networkDao.getAllPersistentNetworksFromZone(anyLong())).thenReturn(Collections.emptyList());

        boolean result = listener.hostConnect(HOST_ID, POOL_ID);

        assertTrue(result);
        verify(storagePoolHostDao).persist(any(StoragePoolHostVO.class));
        verify(primaryStoreDao).update(POOL_ID, poolVO);
        verify(storageService).updateStorageCapabilities(POOL_ID, false);
    }

    @Test
    public void hostConnectUpdatesExistingStoragePoolHostWhenAlreadyPresent() throws StorageConflictException {
        setUpPoolForConnect(Storage.StoragePoolType.NetworkFilesystem);
        when(agentMgr.easySend(eq(HOST_ID), any(Command.class))).thenReturn(mspAnswer);
        when(mspAnswer.getResult()).thenReturn(true);
        when(mspAnswer.getPoolInfo()).thenReturn(newPoolInfo());
        StoragePoolHostVO existing = new StoragePoolHostVO(POOL_ID, HOST_ID, "/old/path");
        when(storagePoolHostDao.findByPoolHost(POOL_ID, HOST_ID)).thenReturn(existing);
        when(networkDao.getAllPersistentNetworksFromZone(anyLong())).thenReturn(Collections.emptyList());

        boolean result = listener.hostConnect(HOST_ID, POOL_ID);

        assertTrue(result);
        verify(storagePoolHostDao, never()).persist(any(StoragePoolHostVO.class));
        verify(primaryStoreDao).update(POOL_ID, poolVO);
        assertEquals("/local/path", existing.getLocalPath());
    }

    @Test
    public void hostConnectThrowsWhenAnswerIsNull() {
        setUpPoolForConnect(Storage.StoragePoolType.NetworkFilesystem);
        when(agentMgr.easySend(eq(HOST_ID), any(Command.class))).thenReturn(null);

        assertThrows(CloudRuntimeException.class, () -> listener.hostConnect(HOST_ID, POOL_ID));
    }

    @Test
    public void hostConnectSendsAlertContainingHostAndThrowsWhenAnswerFails() {
        setUpPoolForConnect(Storage.StoragePoolType.NetworkFilesystem);
        when(host.toString()).thenReturn("Host {id=5, name=cs-kvm06}");
        when(agentMgr.easySend(eq(HOST_ID), any(Command.class))).thenReturn(mspAnswer);
        when(mspAnswer.getResult()).thenReturn(false);

        assertThrows(CloudRuntimeException.class, () -> listener.hostConnect(HOST_ID, POOL_ID));

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(alertMgr).sendAlert(eq(AlertManager.AlertType.ALERT_TYPE_HOST), eq(1L), eq(2L), messageCaptor.capture(), messageCaptor.capture());
        assertTrue(messageCaptor.getValue().contains("Host {id=5, name=cs-kvm06}"));
        assertTrue(messageCaptor.getValue().contains("Unable to attach storage pool"));
    }

    @Test
    public void hostConnectThrowsStorageConflictExceptionWhenLocalStorageAlreadyAdded() {
        setUpPoolForConnect(Storage.StoragePoolType.NetworkFilesystem);
        when(pool.isShared()).thenReturn(true);
        when(agentMgr.easySend(eq(HOST_ID), any(Command.class))).thenReturn(mspAnswer);
        when(mspAnswer.getResult()).thenReturn(true);
        when(mspAnswer.getLocalDatastoreName()).thenReturn("datastore1");
        StoragePoolVO conflictingPool = mock(StoragePoolVO.class);
        when(conflictingPool.getPath()).thenReturn("datastore1");
        when(primaryStoreDao.listLocalStoragePoolByPath(1L, "datastore1")).thenReturn(Collections.singletonList(conflictingPool));

        assertThrows(StorageConflictException.class, () -> listener.hostConnect(HOST_ID, POOL_ID));
    }

    @Test
    public void hostConnectValidatesAndSyncsDatastoreClusterChildren() throws StorageConflictException {
        setUpPoolForConnect(Storage.StoragePoolType.DatastoreCluster);
        when(agentMgr.easySend(eq(HOST_ID), any(Command.class))).thenReturn(mspAnswer);
        when(mspAnswer.getResult()).thenReturn(true);
        when(mspAnswer.getPoolInfo()).thenReturn(newPoolInfo());
        List<ModifyStoragePoolAnswer> children = Collections.singletonList(mock(ModifyStoragePoolAnswer.class));
        when(mspAnswer.getDatastoreClusterChildren()).thenReturn(children);
        when(networkDao.getAllPersistentNetworksFromZone(anyLong())).thenReturn(Collections.emptyList());

        boolean result = listener.hostConnect(HOST_ID, POOL_ID);

        assertTrue(result);
        verify(storageManager).validateChildDatastoresToBeAddedInUpState(poolVO, children);
        verify(storageManager).syncDatastoreClusterStoragePool(POOL_ID, children, HOST_ID);
    }

    // Note: the CLVM secure-zero-fill detail-setting branch (ClvmPoolManager.isClvmPoolType(...) together
    // with ClvmPoolManager.CLVMSecureZeroFill.valueIn(poolId)) is intentionally not covered here.
    // CLVMSecureZeroFill is a static ConfigKey whose value() falls back to a process-wide static
    // ConfigDepot (ConfigKey.s_depot) that may already have been initialised as a side effect of other
    // tests running earlier in the same JVM/module test run, making the outcome of valueIn(...)
    // order-dependent and awkward to control from a plain Mockito unit test without also mocking
    // static state shared across the whole test module.

    // ---- hostDisconnected ----

    @Test
    public void hostDisconnectedReturnsFalseWhenHostNotFound() {
        when(hostDao.findById(HOST_ID)).thenReturn(null);

        assertFalse(listener.hostDisconnected(HOST_ID, POOL_ID));
    }

    @Test
    public void hostDisconnectedThrowsWhenDeleteCommandAnswerIsNull() {
        when(hostDao.findById(HOST_ID)).thenReturn(host);
        when(dataStoreMgr.getDataStore(POOL_ID, DataStoreRole.Primary)).thenReturn(pool);
        when(host.getId()).thenReturn(HOST_ID);
        when(agentMgr.easySend(eq(HOST_ID), any(Command.class))).thenReturn(null);

        assertThrows(CloudRuntimeException.class, () -> listener.hostDisconnected(HOST_ID, POOL_ID));
    }

    @Test
    public void hostDisconnectedLogsMessageContainingHostAndPoolAndReturnsFalseWhenAnswerFails() {
        when(hostDao.findById(HOST_ID)).thenReturn(host);
        when(host.getId()).thenReturn(HOST_ID);
        when(host.toString()).thenReturn("Host {id=5, name=cs-kvm06}");
        when(dataStoreMgr.getDataStore(POOL_ID, DataStoreRole.Primary)).thenReturn(pool);
        when(pool.toString()).thenReturn("Pool {id=10, name=primary1}");
        when(pool.getDataCenterId()).thenReturn(1L);
        when(pool.getPodId()).thenReturn(2L);
        Answer answer = mock(Answer.class);
        when(answer.getResult()).thenReturn(false);
        when(agentMgr.easySend(eq(HOST_ID), any(Command.class))).thenReturn(answer);

        boolean result = listener.hostDisconnected(HOST_ID, POOL_ID);

        assertFalse(result);
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(alertMgr).sendAlert(eq(AlertManager.AlertType.ALERT_TYPE_HOST), eq(1L), eq(2L), messageCaptor.capture(), messageCaptor.capture());
        assertTrue(messageCaptor.getValue().contains("Host {id=5, name=cs-kvm06}"));
        assertTrue(messageCaptor.getValue().contains("Pool {id=10, name=primary1}"));
    }

    @Test
    public void hostDisconnectedRemovesStoragePoolHostDetailsAndReturnsTrueOnSuccess() {
        when(hostDao.findById(HOST_ID)).thenReturn(host);
        when(host.getId()).thenReturn(HOST_ID);
        when(dataStoreMgr.getDataStore(POOL_ID, DataStoreRole.Primary)).thenReturn(pool);
        Answer answer = mock(Answer.class);
        when(answer.getResult()).thenReturn(true);
        when(agentMgr.easySend(eq(HOST_ID), any(Command.class))).thenReturn(answer);
        StoragePoolHostVO storagePoolHost = new StoragePoolHostVO(POOL_ID, HOST_ID, "/local/path");
        when(storagePoolHostDao.findByPoolHost(POOL_ID, HOST_ID)).thenReturn(storagePoolHost);

        boolean result = listener.hostDisconnected(HOST_ID, POOL_ID);

        assertTrue(result);
        verify(storagePoolHostDao).deleteStoragePoolHostDetails(HOST_ID, POOL_ID);
    }

    // ---- hostAboutToBeRemoved ----

    @Test
    public void hostAboutToBeRemovedReturnsFalseWhenHostNotFound() {
        when(hostDao.findById(HOST_ID)).thenReturn(null);

        assertFalse(listener.hostAboutToBeRemoved(HOST_ID));
    }

    @Test
    public void hostAboutToBeRemovedSkipsNetworkWhenAnswerIsNullAndStillReturnsTrue() {
        when(hostDao.findById(HOST_ID)).thenReturn(host);
        when(host.getDataCenterId()).thenReturn(1L);
        NetworkVO network = mock(NetworkVO.class);
        NetworkOfferingVO offering = mock(NetworkOfferingVO.class);
        when(networkDao.getAllPersistentNetworksFromZone(1L)).thenReturn(Collections.singletonList(network));
        when(networkOfferingDao.findById(anyLong())).thenReturn(offering);
        when(agentMgr.easySend(eq(HOST_ID), any(CleanupPersistentNetworkResourceCommand.class))).thenReturn(null);

        boolean result = listener.hostAboutToBeRemoved(HOST_ID);

        assertTrue(result);
        verify(agentMgr).easySend(eq(HOST_ID), any(CleanupPersistentNetworkResourceCommand.class));
    }

    @Test
    public void hostAboutToBeRemovedLogsErrorWhenAnswerFailsAndStillReturnsTrue() {
        when(hostDao.findById(HOST_ID)).thenReturn(host);
        when(host.getDataCenterId()).thenReturn(1L);
        NetworkVO network = mock(NetworkVO.class);
        NetworkOfferingVO offering = mock(NetworkOfferingVO.class);
        when(networkDao.getAllPersistentNetworksFromZone(1L)).thenReturn(Collections.singletonList(network));
        when(networkOfferingDao.findById(anyLong())).thenReturn(offering);
        Answer answer = mock(Answer.class);
        when(answer.getResult()).thenReturn(false);
        when(agentMgr.easySend(eq(HOST_ID), any(CleanupPersistentNetworkResourceCommand.class))).thenReturn(answer);

        boolean result = listener.hostAboutToBeRemoved(HOST_ID);

        assertTrue(result);
        verify(agentMgr).easySend(eq(HOST_ID), any(CleanupPersistentNetworkResourceCommand.class));
    }

    // ---- hostEnabled ----

    @Test
    public void hostEnabledReturnsFalseWhenHostNotFound() {
        when(hostDao.findById(HOST_ID)).thenReturn(null);

        assertFalse(listener.hostEnabled(HOST_ID));
    }

    @Test
    public void hostEnabledSetsUpPersistentNetworkForEachPersistentNetwork() {
        when(hostDao.findById(HOST_ID)).thenReturn(host);
        when(host.getId()).thenReturn(HOST_ID);
        when(host.getDataCenterId()).thenReturn(1L);
        NetworkVO network1 = mock(NetworkVO.class);
        NetworkVO network2 = mock(NetworkVO.class);
        NetworkOfferingVO offering = mock(NetworkOfferingVO.class);
        when(networkDao.getAllPersistentNetworksFromZone(1L)).thenReturn(java.util.Arrays.asList(network1, network2));
        when(networkOfferingDao.findById(anyLong())).thenReturn(offering);
        Answer answer = mock(Answer.class);
        when(answer.getResult()).thenReturn(true);
        when(agentMgr.easySend(eq(HOST_ID), any(SetupPersistentNetworkCommand.class))).thenReturn(answer);

        boolean result = listener.hostEnabled(HOST_ID);

        assertTrue(result);
        verify(agentMgr, times(2)).easySend(eq(HOST_ID), any(SetupPersistentNetworkCommand.class));
    }

    @Test
    public void hostEnabledThrowsWhenSetupPersistentNetworkAnswerIsNull() {
        when(hostDao.findById(HOST_ID)).thenReturn(host);
        when(host.getId()).thenReturn(HOST_ID);
        when(host.getDataCenterId()).thenReturn(1L);
        NetworkVO network = mock(NetworkVO.class);
        NetworkOfferingVO offering = mock(NetworkOfferingVO.class);
        when(networkDao.getAllPersistentNetworksFromZone(1L)).thenReturn(Collections.singletonList(network));
        when(networkOfferingDao.findById(anyLong())).thenReturn(offering);
        when(agentMgr.easySend(eq(HOST_ID), any(SetupPersistentNetworkCommand.class))).thenReturn(null);

        assertThrows(CloudRuntimeException.class, () -> listener.hostEnabled(HOST_ID));
    }
}

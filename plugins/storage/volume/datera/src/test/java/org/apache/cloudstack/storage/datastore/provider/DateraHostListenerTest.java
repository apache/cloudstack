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
package org.apache.cloudstack.storage.datastore.provider;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.util.Arrays;
import java.util.Collections;

import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolDetailsDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.datastore.util.DateraObject;
import org.apache.cloudstack.storage.datastore.util.DateraUtil;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.ModifyStoragePoolAnswer;
import com.cloud.agent.api.ModifyStoragePoolCommand;
import com.cloud.agent.api.ModifyTargetsCommand;
import com.cloud.alert.AlertManager;
import com.cloud.dc.ClusterDetailsDao;
import com.cloud.dc.ClusterDetailsVO;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.StoragePool;
import com.cloud.storage.StoragePoolHostVO;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.StoragePoolHostDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.dao.VMInstanceDao;

@RunWith(MockitoJUnitRunner.class)
public class DateraHostListenerTest {

    @Mock
    private AgentManager agentMgr;
    @Mock
    private AlertManager alertMgr;
    @Mock
    private ClusterDao clusterDao;
    @Mock
    private ClusterDetailsDao clusterDetailsDao;
    @Mock
    private DataStoreManager dataStoreMgr;
    @Mock
    private HostDao hostDao;
    @Mock
    private PrimaryDataStoreDao storagePoolDao;
    @Mock
    private StoragePoolDetailsDao storagePoolDetailsDao;
    @Mock
    private StoragePoolHostDao storagePoolHostDao;
    @Mock
    private VMInstanceDao vmDao;
    @Mock
    private VolumeDao volumeDao;

    private DateraHostListener listener;

    @Before
    public void setup() {
        listener = new DateraHostListener();

        ReflectionTestUtils.setField(listener, "_agentMgr", agentMgr);
        ReflectionTestUtils.setField(listener, "_alertMgr", alertMgr);
        ReflectionTestUtils.setField(listener, "_clusterDao", clusterDao);
        ReflectionTestUtils.setField(listener, "_clusterDetailsDao", clusterDetailsDao);
        ReflectionTestUtils.setField(listener, "_dataStoreMgr", dataStoreMgr);
        ReflectionTestUtils.setField(listener, "_hostDao", hostDao);
        ReflectionTestUtils.setField(listener, "_storagePoolDao", storagePoolDao);
        ReflectionTestUtils.setField(listener, "_storagePoolDetailsDao", storagePoolDetailsDao);
        ReflectionTestUtils.setField(listener, "storagePoolHostDao", storagePoolHostDao);
        ReflectionTestUtils.setField(listener, "_vmDao", vmDao);
        ReflectionTestUtils.setField(listener, "_volumeDao", volumeDao);
    }

    private StoragePool mockStoragePool() {
        return mock(StoragePool.class, withSettings().extraInterfaces(DataStore.class));
    }

    // ---------- hostAdded ----------

    @Test
    public void hostAddedAlwaysReturnsTrue() {
        assertTrue(listener.hostAdded(1L));
    }

    // ---------- hostConnect ----------

    @Test
    public void hostConnectReturnsFalseWhenHostNotFound() {
        long hostId = 10L;
        long storagePoolId = 100L;

        when(hostDao.findById(hostId)).thenReturn(null);

        assertFalse(listener.hostConnect(hostId, storagePoolId));

        verify(storagePoolHostDao, never()).persist(any(StoragePoolHostVO.class));
    }

    @Test
    public void hostConnectPersistsNewStoragePoolHostWhenNoneExists() {
        long hostId = 10L;
        long storagePoolId = 100L;

        HostVO host = mock(HostVO.class);
        when(host.getHypervisorType()).thenReturn(HypervisorType.Hyperv);
        when(hostDao.findById(hostId)).thenReturn(host);
        when(storagePoolHostDao.findByPoolHost(storagePoolId, hostId)).thenReturn(null);

        assertTrue(listener.hostConnect(hostId, storagePoolId));

        verify(storagePoolHostDao, times(1)).persist(any(StoragePoolHostVO.class));
    }

    @Test
    public void hostConnectDoesNotPersistWhenStoragePoolHostAlreadyExists() {
        long hostId = 10L;
        long storagePoolId = 100L;

        HostVO host = mock(HostVO.class);
        when(host.getHypervisorType()).thenReturn(HypervisorType.Hyperv);
        when(hostDao.findById(hostId)).thenReturn(host);
        StoragePoolHostVO existingStoragePoolHost = mock(StoragePoolHostVO.class);
        when(storagePoolHostDao.findByPoolHost(storagePoolId, hostId)).thenReturn(existingStoragePoolHost);

        assertTrue(listener.hostConnect(hostId, storagePoolId));

        verify(storagePoolHostDao, never()).persist(any(StoragePoolHostVO.class));
    }

    @Test
    public void hostConnectForXenServerSendsModifyStoragePoolCommandPerStoragePath() {
        long hostId = 10L;
        long storagePoolId = 100L;
        long clusterId = 5L;
        long vmHostId = 20L;

        HostVO host = mock(HostVO.class);
        when(host.getId()).thenReturn(hostId);
        when(host.getClusterId()).thenReturn(clusterId);
        when(host.getHypervisorType()).thenReturn(HypervisorType.XenServer);
        when(hostDao.findById(hostId)).thenReturn(host);
        when(storagePoolHostDao.findByPoolHost(storagePoolId, hostId)).thenReturn(null);

        StoragePool storagePool = mockStoragePool();
        when(dataStoreMgr.getDataStore(storagePoolId, DataStoreRole.Primary)).thenReturn((DataStore) storagePool);

        VolumeVO volume1 = mock(VolumeVO.class);
        when(volume1.getInstanceId()).thenReturn(1001L);
        when(volume1.get_iScsiName()).thenReturn("iqn-1");

        VolumeVO volume2 = mock(VolumeVO.class);
        when(volume2.getInstanceId()).thenReturn(1002L);
        when(volume2.get_iScsiName()).thenReturn("iqn-2");

        when(volumeDao.findNonDestroyedVolumesByPoolId(eq(storagePoolId), isNull())).thenReturn(Arrays.asList(volume1, volume2));

        VMInstanceVO vm1 = mock(VMInstanceVO.class);
        when(vm1.getHostId()).thenReturn(vmHostId);
        when(vmDao.findById(1001L)).thenReturn(vm1);

        VMInstanceVO vm2 = mock(VMInstanceVO.class);
        when(vm2.getHostId()).thenReturn(vmHostId);
        when(vmDao.findById(1002L)).thenReturn(vm2);

        HostVO vmHost = mock(HostVO.class);
        when(vmHost.getClusterId()).thenReturn(clusterId);
        when(hostDao.findById(vmHostId)).thenReturn(vmHost);

        when(agentMgr.easySend(eq(hostId), any(ModifyStoragePoolCommand.class)))
                .thenReturn(new ModifyStoragePoolAnswer(null, true, "ok"));

        assertTrue(listener.hostConnect(hostId, storagePoolId));

        verify(agentMgr, times(2)).easySend(eq(hostId), any(ModifyStoragePoolCommand.class));
        verify(alertMgr, never()).sendAlert(any(), anyLong(), any(), anyString(), anyString());
    }

    @Test
    public void hostConnectForXenServerSendsAlertUsingHostToStringWhenAnswerFails() {
        long hostId = 10L;
        long storagePoolId = 100L;
        long clusterId = 5L;
        long vmHostId = 20L;

        HostVO host = mock(HostVO.class);
        when(host.getId()).thenReturn(hostId);
        when(host.getClusterId()).thenReturn(clusterId);
        when(host.getHypervisorType()).thenReturn(HypervisorType.XenServer);
        when(host.toString()).thenReturn("Host {id=10, name=xen-01}");
        when(hostDao.findById(hostId)).thenReturn(host);
        when(storagePoolHostDao.findByPoolHost(storagePoolId, hostId)).thenReturn(null);

        StoragePool storagePool = mockStoragePool();
        when(dataStoreMgr.getDataStore(storagePoolId, DataStoreRole.Primary)).thenReturn((DataStore) storagePool);

        VolumeVO volume = mock(VolumeVO.class);
        when(volume.getInstanceId()).thenReturn(1001L);
        when(volume.get_iScsiName()).thenReturn("iqn-1");
        when(volumeDao.findNonDestroyedVolumesByPoolId(eq(storagePoolId), isNull())).thenReturn(Collections.singletonList(volume));

        VMInstanceVO vm = mock(VMInstanceVO.class);
        when(vm.getHostId()).thenReturn(vmHostId);
        when(vmDao.findById(1001L)).thenReturn(vm);

        HostVO vmHost = mock(HostVO.class);
        when(vmHost.getClusterId()).thenReturn(clusterId);
        when(hostDao.findById(vmHostId)).thenReturn(vmHost);

        when(agentMgr.easySend(eq(hostId), any(ModifyStoragePoolCommand.class)))
                .thenReturn(new Answer(null, false, "failure"));

        try {
            listener.hostConnect(hostId, storagePoolId);
            fail("Expected a CloudRuntimeException to be thrown");
        } catch (CloudRuntimeException e) {
            // expected
        }

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(alertMgr).sendAlert(eq(AlertManager.AlertType.ALERT_TYPE_HOST), anyLong(), any(), messageCaptor.capture(), anyString());
        assertTrue(messageCaptor.getValue().contains("Host {id=10, name=xen-01}"));
    }

    @Test
    public void hostConnectForKvmSendsModifyStoragePoolCommand() {
        long hostId = 10L;
        long storagePoolId = 100L;

        HostVO host = mock(HostVO.class);
        when(host.getHypervisorType()).thenReturn(HypervisorType.KVM);
        when(hostDao.findById(hostId)).thenReturn(host);
        when(storagePoolHostDao.findByPoolHost(storagePoolId, hostId)).thenReturn(null);

        StoragePool storagePool = mockStoragePool();
        when(dataStoreMgr.getDataStore(storagePoolId, DataStoreRole.Primary)).thenReturn((DataStore) storagePool);

        when(agentMgr.easySend(eq(hostId), any(ModifyStoragePoolCommand.class)))
                .thenReturn(new ModifyStoragePoolAnswer(null, true, "ok"));

        assertTrue(listener.hostConnect(hostId, storagePoolId));

        verify(agentMgr, times(1)).easySend(eq(hostId), any(ModifyStoragePoolCommand.class));
        // the 2-arg handleKVM overload does not consult volumes/VMs at all
        verify(volumeDao, never()).findNonDestroyedVolumesByPoolId(anyLong(), any());
    }

    // ---------- hostDisconnected ----------

    @Test
    public void hostDisconnectedDeletesDetailsWhenStoragePoolHostExists() {
        long hostId = 10L;
        long storagePoolId = 100L;

        StoragePoolHostVO existingStoragePoolHost = mock(StoragePoolHostVO.class);
        when(storagePoolHostDao.findByPoolHost(storagePoolId, hostId)).thenReturn(existingStoragePoolHost);

        assertTrue(listener.hostDisconnected(hostId, storagePoolId));

        verify(storagePoolHostDao, times(1)).deleteStoragePoolHostDetails(hostId, storagePoolId);
    }

    @Test
    public void hostDisconnectedDoesNothingWhenStoragePoolHostDoesNotExist() {
        long hostId = 10L;
        long storagePoolId = 100L;

        when(storagePoolHostDao.findByPoolHost(storagePoolId, hostId)).thenReturn(null);

        assertTrue(listener.hostDisconnected(hostId, storagePoolId));

        verify(storagePoolHostDao, never()).deleteStoragePoolHostDetails(anyLong(), anyLong());
    }

    // ---------- hostAboutToBeRemoved ----------

    @Test
    public void hostAboutToBeRemovedForVmwareSendsModifyTargetsCommandWithAddFalse() {
        long hostId = 10L;
        long clusterId = 5L;

        HostVO host = mock(HostVO.class);
        when(host.getId()).thenReturn(hostId);
        when(host.getClusterId()).thenReturn(clusterId);
        when(host.getHypervisorType()).thenReturn(HypervisorType.VMware);
        when(hostDao.findById(hostId)).thenReturn(host);

        StoragePoolVO storagePool = mock(StoragePoolVO.class);
        when(storagePool.getId()).thenReturn(100L);
        when(storagePoolDao.findPoolsByProvider(DateraUtil.PROVIDER_NAME)).thenReturn(Collections.singletonList(storagePool));
        when(storagePoolDao.findById(100L)).thenReturn(storagePool);

        when(volumeDao.findNonDestroyedVolumesByPoolId(eq(100L), isNull())).thenReturn(Collections.emptyList());

        when(agentMgr.easySend(eq(hostId), any(ModifyTargetsCommand.class)))
                .thenReturn(new Answer(null, true, "ok"));

        assertTrue(listener.hostAboutToBeRemoved(hostId));

        ArgumentCaptor<ModifyTargetsCommand> cmdCaptor = ArgumentCaptor.forClass(ModifyTargetsCommand.class);
        verify(agentMgr).easySend(eq(hostId), cmdCaptor.capture());
        assertFalse(cmdCaptor.getValue().getAdd());
    }

    @Test
    public void hostAboutToBeRemovedForVmwareSendsAlertUsingHostToStringWhenAnswerFails() {
        long hostId = 10L;
        long clusterId = 5L;

        HostVO host = mock(HostVO.class);
        when(host.getId()).thenReturn(hostId);
        when(host.getClusterId()).thenReturn(clusterId);
        when(host.getHypervisorType()).thenReturn(HypervisorType.VMware);
        when(host.toString()).thenReturn("Host {id=10, name=vmware-01}");
        when(hostDao.findById(hostId)).thenReturn(host);

        StoragePoolVO storagePool = mock(StoragePoolVO.class);
        when(storagePool.getId()).thenReturn(100L);
        when(storagePoolDao.findPoolsByProvider(DateraUtil.PROVIDER_NAME)).thenReturn(Collections.singletonList(storagePool));
        when(storagePoolDao.findById(100L)).thenReturn(storagePool);

        when(volumeDao.findNonDestroyedVolumesByPoolId(eq(100L), isNull())).thenReturn(Collections.emptyList());

        when(agentMgr.easySend(eq(hostId), any(ModifyTargetsCommand.class)))
                .thenReturn(new Answer(null, false, "failure"));

        try {
            listener.hostAboutToBeRemoved(hostId);
            fail("Expected a CloudRuntimeException to be thrown");
        } catch (CloudRuntimeException e) {
            // expected
        }

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(alertMgr).sendAlert(eq(AlertManager.AlertType.ALERT_TYPE_HOST), anyLong(), any(), messageCaptor.capture(), anyString());
        assertTrue(messageCaptor.getValue().contains("Host {id=10, name=vmware-01}"));
    }

    @Test
    public void hostAboutToBeRemovedForNonVmwareHostIsANoOpAndReturnsTrue() {
        long hostId = 10L;

        HostVO host = mock(HostVO.class);
        when(host.getHypervisorType()).thenReturn(HypervisorType.KVM);
        when(hostDao.findById(hostId)).thenReturn(host);

        assertTrue(listener.hostAboutToBeRemoved(hostId));

        verify(agentMgr, never()).easySend(anyLong(), any());
        verify(storagePoolDao, never()).findPoolsByProvider(anyString());
    }

    // ---------- hostRemoved ----------
    //
    // hostRemoved() acquires a GlobalLock.getInternLock(...) before doing any work. GlobalLock.lock()
    // delegates to DbUtil.getGlobalLock(), which opens a real JDBC connection (TransactionLegacy.getStandaloneConnection())
    // - not viable/safe in a plain unit test. GlobalLock is a plain (non-final) class though, and its static
    // factory method is just a lookup in an in-process map, so we mock the static factory itself
    // (MockedStatic<GlobalLock>) to hand back a fully-mocked GlobalLock instance. This avoids ever touching the
    // real lock()/unlock() implementation (and therefore the DB), while still exercising all of hostRemoved()'s
    // own logic.

    @Test
    public void hostRemovedReturnsTrueWhenNoStoragePoolsUseTheProvider() {
        long hostId = 10L;
        long clusterId = 5L;

        ClusterVO clusterVO = mock(ClusterVO.class);
        when(clusterVO.getUuid()).thenReturn("cluster-uuid");
        when(clusterDao.findById(clusterId)).thenReturn(clusterVO);

        HostVO hostVO = mock(HostVO.class);
        when(hostVO.getUuid()).thenReturn("host-uuid");
        when(hostDao.findByIdIncludingRemoved(hostId)).thenReturn(hostVO);

        when(storagePoolDao.findPoolsByProvider(DateraUtil.PROVIDER_NAME)).thenReturn(Collections.emptyList());

        GlobalLock lock = mock(GlobalLock.class);
        when(lock.lock(5)).thenReturn(true);

        try (MockedStatic<GlobalLock> globalLockMock = mockStatic(GlobalLock.class)) {
            globalLockMock.when(() -> GlobalLock.getInternLock("cluster-uuid")).thenReturn(lock);

            assertTrue(listener.hostRemoved(hostId, clusterId));
        }

        verify(lock).unlock();
        verify(lock).releaseRef();
    }

    @Test
    public void hostRemovedReturnsTrueWhenNoInitiatorGroupIsConfiguredForCluster() {
        long hostId = 10L;
        long clusterId = 5L;

        ClusterVO clusterVO = mock(ClusterVO.class);
        when(clusterVO.getUuid()).thenReturn("cluster-uuid");
        when(clusterDao.findById(clusterId)).thenReturn(clusterVO);

        HostVO hostVO = mock(HostVO.class);
        when(hostVO.getUuid()).thenReturn("host-uuid");
        when(hostDao.findByIdIncludingRemoved(hostId)).thenReturn(hostVO);

        StoragePoolVO storagePool = mock(StoragePoolVO.class);
        when(storagePool.getId()).thenReturn(100L);
        when(storagePoolDao.findPoolsByProvider(DateraUtil.PROVIDER_NAME)).thenReturn(Collections.singletonList(storagePool));

        // no ClusterDetailsVO configured for the initiator group key -> clusterDetail is null
        when(clusterDetailsDao.findDetail(eq(clusterId), anyString())).thenReturn(null);

        GlobalLock lock = mock(GlobalLock.class);
        when(lock.lock(5)).thenReturn(true);

        try (MockedStatic<GlobalLock> globalLockMock = mockStatic(GlobalLock.class)) {
            globalLockMock.when(() -> GlobalLock.getInternLock("cluster-uuid")).thenReturn(lock);

            assertTrue(listener.hostRemoved(hostId, clusterId));
        }

        verify(lock).unlock();
        verify(lock).releaseRef();
    }

    @Test
    public void hostRemovedRemovesInitiatorFromMatchingInitiatorGroup() {
        long hostId = 10L;
        long clusterId = 5L;
        long storagePoolId = 100L;
        String initiatorGroupName = "CS-InitiatorGroup-1";

        ClusterVO clusterVO = mock(ClusterVO.class);
        when(clusterVO.getUuid()).thenReturn("cluster-uuid");
        when(clusterDao.findById(clusterId)).thenReturn(clusterVO);

        HostVO hostVO = mock(HostVO.class);
        when(hostVO.getUuid()).thenReturn("host-uuid");
        when(hostVO.getStorageUrl()).thenReturn("iqn.host");
        when(hostDao.findByIdIncludingRemoved(hostId)).thenReturn(hostVO);

        StoragePoolVO storagePool = mock(StoragePoolVO.class);
        when(storagePool.getId()).thenReturn(storagePoolId);
        when(storagePoolDao.findPoolsByProvider(DateraUtil.PROVIDER_NAME)).thenReturn(Collections.singletonList(storagePool));

        // computed with the real DateraUtil implementation, before DateraUtil gets static-mocked below
        String initiatorGroupKey = DateraUtil.getInitiatorGroupKey(storagePoolId);

        ClusterDetailsVO clusterDetail = mock(ClusterDetailsVO.class);
        when(clusterDetail.getValue()).thenReturn(initiatorGroupName);
        when(clusterDetailsDao.findDetail(clusterId, initiatorGroupKey)).thenReturn(clusterDetail);

        DateraObject.DateraConnection connection = mock(DateraObject.DateraConnection.class);
        DateraObject.Initiator initiator = mock(DateraObject.Initiator.class);
        when(initiator.getPath()).thenReturn("/initiator-path");
        DateraObject.InitiatorGroup initiatorGroup = mock(DateraObject.InitiatorGroup.class);

        GlobalLock lock = mock(GlobalLock.class);
        when(lock.lock(5)).thenReturn(true);

        // NOTE: no CALLS_REAL_METHODS default here - the real DateraUtil.removeInitiatorFromGroup() would go on
        // to make a genuine HTTP call, so every static method reachable from hostRemoved() is stubbed explicitly.
        try (MockedStatic<GlobalLock> globalLockMock = mockStatic(GlobalLock.class);
             MockedStatic<DateraUtil> dateraUtilMock = mockStatic(DateraUtil.class)) {

            globalLockMock.when(() -> GlobalLock.getInternLock("cluster-uuid")).thenReturn(lock);

            dateraUtilMock.when(() -> DateraUtil.getInitiatorGroupKey(storagePoolId)).thenReturn(initiatorGroupKey);
            dateraUtilMock.when(() -> DateraUtil.hostSupport_iScsi(hostVO)).thenReturn(true);
            dateraUtilMock.when(() -> DateraUtil.getDateraConnection(storagePoolId, storagePoolDetailsDao)).thenReturn(connection);
            dateraUtilMock.when(() -> DateraUtil.getInitiator(connection, "iqn.host")).thenReturn(initiator);
            dateraUtilMock.when(() -> DateraUtil.getInitiatorGroup(connection, initiatorGroupName)).thenReturn(initiatorGroup);
            dateraUtilMock.when(() -> DateraUtil.isInitiatorPresentInGroup(initiator, initiatorGroup)).thenReturn(true);

            assertTrue(listener.hostRemoved(hostId, clusterId));

            dateraUtilMock.verify(() -> DateraUtil.removeInitiatorFromGroup(connection, "/initiator-path", initiatorGroupName));
        }

        verify(lock).unlock();
        verify(lock).releaseRef();
    }

    @Test
    public void hostRemovedThrowsWhenLockCannotBeAcquired() {
        long hostId = 10L;
        long clusterId = 5L;

        ClusterVO clusterVO = mock(ClusterVO.class);
        when(clusterVO.getUuid()).thenReturn("cluster-uuid");
        when(clusterDao.findById(clusterId)).thenReturn(clusterVO);

        HostVO hostVO = mock(HostVO.class);
        when(hostVO.getUuid()).thenReturn("host-uuid");
        when(hostDao.findByIdIncludingRemoved(hostId)).thenReturn(hostVO);

        GlobalLock lock = mock(GlobalLock.class);
        when(lock.lock(5)).thenReturn(false);

        try (MockedStatic<GlobalLock> globalLockMock = mockStatic(GlobalLock.class)) {
            globalLockMock.when(() -> GlobalLock.getInternLock("cluster-uuid")).thenReturn(lock);

            try {
                listener.hostRemoved(hostId, clusterId);
                fail("Expected a CloudRuntimeException to be thrown");
            } catch (CloudRuntimeException e) {
                // expected
            }
        }

        verify(lock, never()).unlock();
        verify(lock, never()).releaseRef();
    }

    // ---------- hostEnabled ----------

    @Test
    public void hostEnabledAlwaysReturnsTrue() {
        assertTrue(listener.hostEnabled(1L));
    }
}

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
package org.apache.cloudstack.storage.endpoint;


import com.cloud.hypervisor.Hypervisor;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.storage.VolumeDetailVO;
import com.cloud.storage.dao.VolumeDetailsDao;
import com.cloud.vm.VirtualMachine;
import org.apache.cloudstack.engine.subsystem.api.storage.DataObject;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPoint;
import org.apache.cloudstack.engine.subsystem.api.storage.Scope;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.StorageAction;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(MockitoJUnitRunner.class)
public class DefaultEndPointSelectorTest {

    @Mock
    private VirtualMachine virtualMachineMock;

    @Mock
    private VolumeInfo volumeInfoMock;

    @Mock
    private SnapshotInfo snapshotInfoMock;

    @Mock
    private DataStore datastoreMock;

    @Mock
    private StoragePoolVO storagePoolVOMock;

    @Mock
    private PrimaryDataStoreDao _storagePoolDao;

    @Mock
    private VolumeDetailsDao _volDetailsDao;

    @Mock
    private VolumeDetailVO volumeDetailVOMock;

    @Mock
    private EndPoint endPointMock;

    @InjectMocks
    private DefaultEndPointSelector defaultEndPointSelectorSpy = Mockito.spy(new DefaultEndPointSelector());

    private static final Long VOLUME_ID = 1L;
    private static final Long HOST_ID = 10L;
    private static final Long DEST_HOST_ID = 20L;
    private static final Long STORE_ID = 100L;
    private static final String VOLUME_UUID = "test-volume-uuid";

    @Before
    public void setup() {
        Mockito.doReturn(volumeInfoMock).when(snapshotInfoMock).getBaseVolume();

        // Common volume mock setup
        Mockito.when(volumeInfoMock.getId()).thenReturn(VOLUME_ID);
        Mockito.when(volumeInfoMock.getUuid()).thenReturn(VOLUME_UUID);
    }

    @Test
    public void getEndPointForBitmapRemovalTestVolumeIsNotAttached() {
        Mockito.doReturn(false).when(volumeInfoMock).isAttachedVM();
        Mockito.doReturn(null).when(defaultEndPointSelectorSpy).select(volumeInfoMock, false);

        defaultEndPointSelectorSpy.getEndPointForBitmapRemoval(snapshotInfoMock, false);

        Mockito.verify(defaultEndPointSelectorSpy, Mockito.times(1)).select(volumeInfoMock, false);
    }

    @Test
    public void getEndPointForBitmapRemovalTestVolumeIsAttachedHostIdIsSet() {
        Mockito.doReturn(true).when(volumeInfoMock).isAttachedVM();
        Mockito.doReturn(virtualMachineMock).when(volumeInfoMock).getAttachedVM();
        long hostId = 12L;
        Mockito.doReturn(hostId).when(virtualMachineMock).getHostId();

        Mockito.doReturn(null).when(defaultEndPointSelectorSpy).getEndPointFromHostId(hostId);

        defaultEndPointSelectorSpy.getEndPointForBitmapRemoval(snapshotInfoMock, false);

        Mockito.verify(defaultEndPointSelectorSpy, Mockito.times(1)).getEndPointFromHostId(hostId);
    }

    @Test
    public void getEndPointForBitmapRemovalTestVolumeIsAttachedLastHostIdIsSet() {
        Mockito.doReturn(true).when(volumeInfoMock).isAttachedVM();
        Mockito.doReturn(virtualMachineMock).when(volumeInfoMock).getAttachedVM();

        Mockito.doReturn(null).when(virtualMachineMock).getHostId();
        long lastHostId = 13L;
        Mockito.doReturn(lastHostId).when(virtualMachineMock).getLastHostId();

        Mockito.doReturn(null).when(defaultEndPointSelectorSpy).getEndPointFromHostId(lastHostId);

        defaultEndPointSelectorSpy.getEndPointForBitmapRemoval(snapshotInfoMock, false);

        Mockito.verify(defaultEndPointSelectorSpy, Mockito.times(1)).getEndPointFromHostId(lastHostId);
    }

    @Test
    public void getEndPointForBitmapRemovalTestVolumeIsAttachedNoHostIsSet() {
        Mockito.doReturn(true).when(volumeInfoMock).isAttachedVM();
        Mockito.doReturn(virtualMachineMock).when(volumeInfoMock).getAttachedVM();

        Mockito.doReturn(null).when(virtualMachineMock).getHostId();
        Mockito.doReturn(null).when(virtualMachineMock).getLastHostId();

        Mockito.doReturn(null).when(defaultEndPointSelectorSpy).select(volumeInfoMock, false);

        defaultEndPointSelectorSpy.getEndPointForBitmapRemoval(snapshotInfoMock, false);

        Mockito.verify(defaultEndPointSelectorSpy, Mockito.times(1)).select(volumeInfoMock, false);
    }

    @Test
    public void getEndPointForSnapshotOperationsInKvmTestVolumeIsNotAttachedToVMAndSnapshotOnPrimary() {
        Mockito.doReturn(null).when(volumeInfoMock).getAttachedVM();
        Mockito.doReturn(datastoreMock).when(snapshotInfoMock).getDataStore();
        Mockito.doReturn(DataStoreRole.Primary).when(datastoreMock).getRole();
        Mockito.doReturn(null).when(defaultEndPointSelectorSpy).select(snapshotInfoMock, false);

        defaultEndPointSelectorSpy.getEndPointForSnapshotOperationsInKvm(snapshotInfoMock, false);

        Mockito.verify(defaultEndPointSelectorSpy, Mockito.times(1)).select(snapshotInfoMock, false);
    }

    @Test
    public void getEndPointForSnapshotOperationsInKvmTestVolumeIsNotAttachedToVMAndSnapshotOnSecondary() {
        Mockito.doReturn(null).when(volumeInfoMock).getAttachedVM();
        Mockito.doReturn(datastoreMock).when(snapshotInfoMock).getDataStore();
        Mockito.doReturn(DataStoreRole.Image).when(datastoreMock).getRole();
        long zoneId = 1L;
        Mockito.doReturn(zoneId).when(snapshotInfoMock).getDataCenterId();

        Mockito.doReturn(null).when(defaultEndPointSelectorSpy).selectRandom(zoneId, Hypervisor.HypervisorType.KVM);

        defaultEndPointSelectorSpy.getEndPointForSnapshotOperationsInKvm(snapshotInfoMock, false);

        Mockito.verify(defaultEndPointSelectorSpy, Mockito.times(1)).selectRandom(zoneId, Hypervisor.HypervisorType.KVM);
    }

    @Test
    public void getEndPointForSnapshotOperationsInKvmTestVolumeAttachedToRunningVm() {
        Mockito.doReturn(virtualMachineMock).when(volumeInfoMock).getAttachedVM();
        Mockito.doReturn(datastoreMock).when(snapshotInfoMock).getDataStore();
        Mockito.doReturn(VirtualMachine.State.Running).when(virtualMachineMock).getState();
        long hostId = 12L;
        Mockito.doReturn(hostId).when(virtualMachineMock).getHostId();

        Mockito.doReturn(null).when(defaultEndPointSelectorSpy).getEndPointFromHostId(hostId);

        defaultEndPointSelectorSpy.getEndPointForSnapshotOperationsInKvm(snapshotInfoMock, false);

        Mockito.verify(defaultEndPointSelectorSpy, Mockito.times(1)).getEndPointFromHostId(hostId);
    }

    @Test
    public void getEndPointForSnapshotOperationsInKvmTestVolumeAttachedToStoppedVmAndLastHostIdIsSet() {
        Mockito.doReturn(virtualMachineMock).when(volumeInfoMock).getAttachedVM();
        Mockito.doReturn(datastoreMock).when(snapshotInfoMock).getDataStore();
        Mockito.doReturn(VirtualMachine.State.Stopped).when(virtualMachineMock).getState();
        long hostId = 13L;
        Mockito.doReturn(hostId).when(virtualMachineMock).getLastHostId();

        Mockito.doReturn(null).when(defaultEndPointSelectorSpy).getEndPointFromHostId(hostId);

        defaultEndPointSelectorSpy.getEndPointForSnapshotOperationsInKvm(snapshotInfoMock, false);

        Mockito.verify(defaultEndPointSelectorSpy, Mockito.times(1)).getEndPointFromHostId(hostId);
    }

    @Test
    public void getEndPointForSnapshotOperationsInKvmTestVolumeAttachedToStoppedVmAndLastHostIdIsNotSetAndSnapshotIsOnSecondary() {
        Mockito.doReturn(virtualMachineMock).when(volumeInfoMock).getAttachedVM();
        Mockito.doReturn(datastoreMock).when(snapshotInfoMock).getDataStore();
        Mockito.doReturn(DataStoreRole.Image).when(datastoreMock).getRole();
        Mockito.doReturn(VirtualMachine.State.Stopped).when(virtualMachineMock).getState();
        Mockito.doReturn(null).when(virtualMachineMock).getLastHostId();
        long zoneId = 1L;
        Mockito.doReturn(zoneId).when(snapshotInfoMock).getDataCenterId();
        Mockito.doReturn(null).when(defaultEndPointSelectorSpy).selectRandom(zoneId, Hypervisor.HypervisorType.KVM);

        defaultEndPointSelectorSpy.getEndPointForSnapshotOperationsInKvm(snapshotInfoMock, false);

        Mockito.verify(defaultEndPointSelectorSpy, Mockito.times(1)).selectRandom(zoneId, Hypervisor.HypervisorType.KVM);
    }


    @Test
    public void getEndPointForSnapshotOperationsInKvmTestVolumeAttachedToStoppedVmAndLastHostIdIsNotSetAndSnapshotIsOnPrimary() {
        Mockito.doReturn(virtualMachineMock).when(volumeInfoMock).getAttachedVM();
        Mockito.doReturn(datastoreMock).when(snapshotInfoMock).getDataStore();
        Mockito.doReturn(DataStoreRole.Primary).when(datastoreMock).getRole();
        Mockito.doReturn(VirtualMachine.State.Stopped).when(virtualMachineMock).getState();
        Mockito.doReturn(null).when(virtualMachineMock).getLastHostId();
        Mockito.doReturn(null).when(defaultEndPointSelectorSpy).select(snapshotInfoMock, false);

        defaultEndPointSelectorSpy.getEndPointForSnapshotOperationsInKvm(snapshotInfoMock, false);

        Mockito.verify(defaultEndPointSelectorSpy, Mockito.times(1)).select(snapshotInfoMock, false);
    }


    @Test
    public void testSelectClvmEndpoint_VolumeWithDestinationHost_CLVM() {
        Mockito.when(datastoreMock.getRole()).thenReturn(DataStoreRole.Primary);
        Mockito.when(datastoreMock.getId()).thenReturn(STORE_ID);
        Mockito.when(volumeInfoMock.getDataStore()).thenReturn(datastoreMock);
        Mockito.when(_storagePoolDao.findById(STORE_ID)).thenReturn(storagePoolVOMock);
        Mockito.when(storagePoolVOMock.getPoolType()).thenReturn(StoragePoolType.CLVM);
        Mockito.when(volumeInfoMock.getDestinationHostId()).thenReturn(DEST_HOST_ID);
        Mockito.doReturn(endPointMock).when(defaultEndPointSelectorSpy).getEndPointFromHostId(DEST_HOST_ID);

        EndPoint result = defaultEndPointSelectorSpy.select(volumeInfoMock, false);

        assertNotNull(result);
        assertEquals(endPointMock, result);
        Mockito.verify(defaultEndPointSelectorSpy, Mockito.times(1)).getEndPointFromHostId(DEST_HOST_ID);
    }

    @Test
    public void testSelectClvmEndpoint_VolumeWithDestinationHost_CLVM_NG() {
        Mockito.when(datastoreMock.getRole()).thenReturn(DataStoreRole.Primary);
        Mockito.when(datastoreMock.getId()).thenReturn(STORE_ID);
        Mockito.when(volumeInfoMock.getDataStore()).thenReturn(datastoreMock);
        Mockito.when(_storagePoolDao.findById(STORE_ID)).thenReturn(storagePoolVOMock);
        Mockito.when(storagePoolVOMock.getPoolType()).thenReturn(StoragePoolType.CLVM_NG);
        Mockito.when(volumeInfoMock.getDestinationHostId()).thenReturn(DEST_HOST_ID);
        Mockito.doReturn(endPointMock).when(defaultEndPointSelectorSpy).getEndPointFromHostId(DEST_HOST_ID);

        EndPoint result = defaultEndPointSelectorSpy.select(volumeInfoMock, false);

        assertNotNull(result);
        assertEquals(endPointMock, result);
        Mockito.verify(defaultEndPointSelectorSpy, Mockito.times(1)).getEndPointFromHostId(DEST_HOST_ID);
    }

    @Test
    public void testSelectClvmEndpoint_VolumeWithoutDestinationHost() {
        Mockito.when(datastoreMock.getRole()).thenReturn(DataStoreRole.Primary);
        Mockito.when(datastoreMock.getId()).thenReturn(STORE_ID);
        Mockito.when(volumeInfoMock.getDataStore()).thenReturn(datastoreMock);
        Mockito.when(_storagePoolDao.findById(STORE_ID)).thenReturn(storagePoolVOMock);
        Mockito.when(storagePoolVOMock.getPoolType()).thenReturn(StoragePoolType.CLVM);
        Mockito.when(volumeInfoMock.getDestinationHostId()).thenReturn(null);
        Mockito.when(datastoreMock.getScope()).thenReturn(Mockito.mock(Scope.class));
        Mockito.doReturn(endPointMock).when(defaultEndPointSelectorSpy).findEndPointInScope(
                Mockito.any(), Mockito.anyString(), Mockito.eq(STORE_ID), Mockito.eq(false));

        EndPoint result = defaultEndPointSelectorSpy.select(volumeInfoMock, false);

        assertNotNull(result);
        Mockito.verify(defaultEndPointSelectorSpy, Mockito.never()).getEndPointFromHostId(DEST_HOST_ID);
    }

    @Test
    public void testSelectClvmEndpoint_NonCLVMPool() {
        Mockito.when(datastoreMock.getRole()).thenReturn(DataStoreRole.Primary);
        Mockito.when(datastoreMock.getId()).thenReturn(STORE_ID);
        Mockito.when(volumeInfoMock.getDataStore()).thenReturn(datastoreMock);
        Mockito.when(_storagePoolDao.findById(STORE_ID)).thenReturn(storagePoolVOMock);
        Mockito.when(storagePoolVOMock.getPoolType()).thenReturn(StoragePoolType.NetworkFilesystem);
        Mockito.when(datastoreMock.getScope()).thenReturn(Mockito.mock(Scope.class));
        Mockito.doReturn(endPointMock).when(defaultEndPointSelectorSpy).findEndPointInScope(
                Mockito.any(), Mockito.anyString(), Mockito.eq(STORE_ID), Mockito.eq(false));

        EndPoint result = defaultEndPointSelectorSpy.select(volumeInfoMock, false);

        assertNotNull(result);
        Mockito.verify(defaultEndPointSelectorSpy, Mockito.never()).getEndPointFromHostId(DEST_HOST_ID);
    }

    @Test
    public void testSelectClvmEndpoint_SnapshotWithBaseVolumeDestHost() {
        Mockito.when(datastoreMock.getRole()).thenReturn(DataStoreRole.Primary);
        Mockito.when(datastoreMock.getId()).thenReturn(STORE_ID);
        Mockito.when(snapshotInfoMock.getDataStore()).thenReturn(datastoreMock);
        Mockito.when(snapshotInfoMock.getBaseVolume()).thenReturn(volumeInfoMock);
        Mockito.when(volumeInfoMock.getDataStore()).thenReturn(datastoreMock);
        Mockito.when(_storagePoolDao.findById(STORE_ID)).thenReturn(storagePoolVOMock);
        Mockito.when(storagePoolVOMock.getPoolType()).thenReturn(StoragePoolType.CLVM_NG);
        Mockito.when(volumeInfoMock.getDestinationHostId()).thenReturn(DEST_HOST_ID);
        Mockito.doReturn(endPointMock).when(defaultEndPointSelectorSpy).getEndPointFromHostId(DEST_HOST_ID);

        EndPoint result = defaultEndPointSelectorSpy.select(snapshotInfoMock, false);

        assertNotNull(result);
        assertEquals(endPointMock, result);
        Mockito.verify(defaultEndPointSelectorSpy, Mockito.times(1)).getEndPointFromHostId(DEST_HOST_ID);
    }

    @Test
    public void testSelectWithAction_DeleteVolume_CLVMWithLockHost() {
        Mockito.when(datastoreMock.getRole()).thenReturn(DataStoreRole.Primary);
        Mockito.when(datastoreMock.getId()).thenReturn(STORE_ID);
        Mockito.when(volumeInfoMock.getDataStore()).thenReturn(datastoreMock);
        Mockito.when(volumeInfoMock.getHypervisorType()).thenReturn(Hypervisor.HypervisorType.KVM);
        Mockito.when(_storagePoolDao.findById(STORE_ID)).thenReturn(storagePoolVOMock);
        Mockito.when(storagePoolVOMock.getPoolType()).thenReturn(StoragePoolType.CLVM);
        Mockito.when(_volDetailsDao.findDetail(VOLUME_ID, VolumeInfo.CLVM_LOCK_HOST_ID)).thenReturn(volumeDetailVOMock);
        Mockito.when(volumeDetailVOMock.getValue()).thenReturn(String.valueOf(HOST_ID));
        Mockito.doReturn(endPointMock).when(defaultEndPointSelectorSpy).getEndPointFromHostId(HOST_ID);

        EndPoint result = defaultEndPointSelectorSpy.select(volumeInfoMock, StorageAction.DELETEVOLUME, false);

        assertNotNull(result);
        assertEquals(endPointMock, result);
        Mockito.verify(defaultEndPointSelectorSpy, Mockito.times(1)).getEndPointFromHostId(HOST_ID);
    }

    @Test
    public void testSelectWithAction_DeleteVolume_CLVM_NG_WithLockHost() {
        Mockito.when(datastoreMock.getRole()).thenReturn(DataStoreRole.Primary);
        Mockito.when(datastoreMock.getId()).thenReturn(STORE_ID);
        Mockito.when(volumeInfoMock.getDataStore()).thenReturn(datastoreMock);
        Mockito.when(volumeInfoMock.getHypervisorType()).thenReturn(Hypervisor.HypervisorType.KVM);
        Mockito.when(_storagePoolDao.findById(STORE_ID)).thenReturn(storagePoolVOMock);
        Mockito.when(storagePoolVOMock.getPoolType()).thenReturn(StoragePoolType.CLVM_NG);
        Mockito.when(_volDetailsDao.findDetail(VOLUME_ID, VolumeInfo.CLVM_LOCK_HOST_ID)).thenReturn(volumeDetailVOMock);
        Mockito.when(volumeDetailVOMock.getValue()).thenReturn(String.valueOf(HOST_ID));
        Mockito.doReturn(endPointMock).when(defaultEndPointSelectorSpy).getEndPointFromHostId(HOST_ID);

        EndPoint result = defaultEndPointSelectorSpy.select(volumeInfoMock, StorageAction.DELETEVOLUME, false);

        assertNotNull(result);
        assertEquals(endPointMock, result);
        Mockito.verify(defaultEndPointSelectorSpy, Mockito.times(1)).getEndPointFromHostId(HOST_ID);
    }

    @Test
    public void testSelectWithAction_DeleteVolume_CLVMWithoutLockHost() {
        Mockito.when(datastoreMock.getRole()).thenReturn(DataStoreRole.Primary);
        Mockito.when(datastoreMock.getId()).thenReturn(STORE_ID);
        Mockito.when(volumeInfoMock.getDataStore()).thenReturn(datastoreMock);
        Mockito.when(volumeInfoMock.getHypervisorType()).thenReturn(Hypervisor.HypervisorType.KVM);
        Mockito.when(_storagePoolDao.findById(STORE_ID)).thenReturn(storagePoolVOMock);
        Mockito.when(storagePoolVOMock.getPoolType()).thenReturn(StoragePoolType.CLVM);
        Mockito.when(_volDetailsDao.findDetail(VOLUME_ID, VolumeInfo.CLVM_LOCK_HOST_ID)).thenReturn(null);
        Mockito.doReturn(endPointMock).when(defaultEndPointSelectorSpy).select(volumeInfoMock, false);

        EndPoint result = defaultEndPointSelectorSpy.select(volumeInfoMock, StorageAction.DELETEVOLUME, false);

        assertNotNull(result);
        assertEquals(endPointMock, result);
        Mockito.verify(defaultEndPointSelectorSpy, Mockito.times(1)).select(volumeInfoMock, false);
    }

    @Test
    public void testSelectWithAction_DeleteVolume_NonCLVM() {
        Mockito.when(datastoreMock.getRole()).thenReturn(DataStoreRole.Primary);
        Mockito.when(datastoreMock.getId()).thenReturn(STORE_ID);
        Mockito.when(volumeInfoMock.getDataStore()).thenReturn(datastoreMock);
        Mockito.when(volumeInfoMock.getHypervisorType()).thenReturn(Hypervisor.HypervisorType.KVM);
        Mockito.when(_storagePoolDao.findById(STORE_ID)).thenReturn(storagePoolVOMock);
        Mockito.when(storagePoolVOMock.getPoolType()).thenReturn(StoragePoolType.NetworkFilesystem);
        Mockito.doReturn(endPointMock).when(defaultEndPointSelectorSpy).select(volumeInfoMock, false);

        EndPoint result = defaultEndPointSelectorSpy.select(volumeInfoMock, StorageAction.DELETEVOLUME, false);

        assertNotNull(result);
        assertEquals(endPointMock, result);
        Mockito.verify(_volDetailsDao, Mockito.never()).findDetail(Mockito.anyLong(), Mockito.anyString());
    }

    @Test
    public void testSelectObject_CLVMVolumeWithLockHost() {
        Mockito.when(datastoreMock.getRole()).thenReturn(DataStoreRole.Primary);
        Mockito.when(datastoreMock.getId()).thenReturn(STORE_ID);
        Mockito.when(volumeInfoMock.getDataStore()).thenReturn(datastoreMock);
        Mockito.when(_storagePoolDao.findById(STORE_ID)).thenReturn(storagePoolVOMock);
        Mockito.when(storagePoolVOMock.getPoolType()).thenReturn(StoragePoolType.CLVM);
        Mockito.when(_volDetailsDao.findDetail(VOLUME_ID, VolumeInfo.CLVM_LOCK_HOST_ID)).thenReturn(volumeDetailVOMock);
        Mockito.when(volumeDetailVOMock.getValue()).thenReturn(String.valueOf(HOST_ID));
        Mockito.doReturn(endPointMock).when(defaultEndPointSelectorSpy).getEndPointFromHostId(HOST_ID);

        EndPoint result = defaultEndPointSelectorSpy.select(volumeInfoMock);

        assertNotNull(result);
        assertEquals(endPointMock, result);
        Mockito.verify(defaultEndPointSelectorSpy, Mockito.times(1)).getEndPointFromHostId(HOST_ID);
    }

    @Test
    public void testSelectObject_CLVM_NG_VolumeWithLockHost() {
        Mockito.when(datastoreMock.getRole()).thenReturn(DataStoreRole.Primary);
        Mockito.when(datastoreMock.getId()).thenReturn(STORE_ID);
        Mockito.when(volumeInfoMock.getDataStore()).thenReturn(datastoreMock);
        Mockito.when(_storagePoolDao.findById(STORE_ID)).thenReturn(storagePoolVOMock);
        Mockito.when(storagePoolVOMock.getPoolType()).thenReturn(StoragePoolType.CLVM_NG);
        Mockito.when(_volDetailsDao.findDetail(VOLUME_ID, VolumeInfo.CLVM_LOCK_HOST_ID)).thenReturn(volumeDetailVOMock);
        Mockito.when(volumeDetailVOMock.getValue()).thenReturn(String.valueOf(HOST_ID));
        Mockito.doReturn(endPointMock).when(defaultEndPointSelectorSpy).getEndPointFromHostId(HOST_ID);

        EndPoint result = defaultEndPointSelectorSpy.select(volumeInfoMock);

        assertNotNull(result);
        assertEquals(endPointMock, result);
        Mockito.verify(defaultEndPointSelectorSpy, Mockito.times(1)).getEndPointFromHostId(HOST_ID);
    }

    @Test
    public void testSelectObject_CLVMVolumeWithoutLockHost() {
        Mockito.when(datastoreMock.getRole()).thenReturn(DataStoreRole.Primary);
        Mockito.when(datastoreMock.getId()).thenReturn(STORE_ID);
        Mockito.when(volumeInfoMock.getDataStore()).thenReturn(datastoreMock);
        Mockito.when(_storagePoolDao.findById(STORE_ID)).thenReturn(storagePoolVOMock);
        Mockito.when(storagePoolVOMock.getPoolType()).thenReturn(StoragePoolType.CLVM);
        Mockito.when(_volDetailsDao.findDetail(VOLUME_ID, VolumeInfo.CLVM_LOCK_HOST_ID)).thenReturn(null);
        Mockito.doReturn(endPointMock).when(defaultEndPointSelectorSpy).select(datastoreMock);

        EndPoint result = defaultEndPointSelectorSpy.select(volumeInfoMock);

        assertNotNull(result);
        assertEquals(endPointMock, result);
        Mockito.verify(defaultEndPointSelectorSpy, Mockito.times(1)).select(datastoreMock);
    }

    @Test
    public void testSelectObject_CLVMVolumeWithInvalidLockHostId() {
        Mockito.when(datastoreMock.getRole()).thenReturn(DataStoreRole.Primary);
        Mockito.when(datastoreMock.getId()).thenReturn(STORE_ID);
        Mockito.when(volumeInfoMock.getDataStore()).thenReturn(datastoreMock);
        Mockito.when(_storagePoolDao.findById(STORE_ID)).thenReturn(storagePoolVOMock);
        Mockito.when(storagePoolVOMock.getPoolType()).thenReturn(StoragePoolType.CLVM);
        Mockito.when(_volDetailsDao.findDetail(VOLUME_ID, VolumeInfo.CLVM_LOCK_HOST_ID)).thenReturn(volumeDetailVOMock);
        Mockito.when(volumeDetailVOMock.getValue()).thenReturn("invalid-host-id");
        Mockito.doReturn(endPointMock).when(defaultEndPointSelectorSpy).select(datastoreMock);

        EndPoint result = defaultEndPointSelectorSpy.select(volumeInfoMock);

        assertNotNull(result);
        assertEquals(endPointMock, result);
        Mockito.verify(defaultEndPointSelectorSpy, Mockito.times(1)).select(datastoreMock);
    }

    @Test
    public void testSelectObject_CLVMVolumeWithEmptyLockHostId() {
        Mockito.when(datastoreMock.getRole()).thenReturn(DataStoreRole.Primary);
        Mockito.when(datastoreMock.getId()).thenReturn(STORE_ID);
        Mockito.when(volumeInfoMock.getDataStore()).thenReturn(datastoreMock);
        Mockito.when(_storagePoolDao.findById(STORE_ID)).thenReturn(storagePoolVOMock);
        Mockito.when(storagePoolVOMock.getPoolType()).thenReturn(StoragePoolType.CLVM);
        Mockito.when(_volDetailsDao.findDetail(VOLUME_ID, VolumeInfo.CLVM_LOCK_HOST_ID)).thenReturn(volumeDetailVOMock);
        Mockito.when(volumeDetailVOMock.getValue()).thenReturn("");
        Mockito.doReturn(endPointMock).when(defaultEndPointSelectorSpy).select(datastoreMock);

        EndPoint result = defaultEndPointSelectorSpy.select(volumeInfoMock);

        assertNotNull(result);
        assertEquals(endPointMock, result);
        Mockito.verify(defaultEndPointSelectorSpy, Mockito.times(1)).select(datastoreMock);
    }

    @Test
    public void testSelectTwoObjects_TemplateToVolume_CLVMWithDestHost() {
        DataObject srcDataMock = Mockito.mock(DataObject.class);

        Mockito.when(volumeInfoMock.getDataStore()).thenReturn(datastoreMock);
        Mockito.when(datastoreMock.getRole()).thenReturn(DataStoreRole.Primary);
        Mockito.when(datastoreMock.getId()).thenReturn(STORE_ID);
        Mockito.when(_storagePoolDao.findById(STORE_ID)).thenReturn(storagePoolVOMock);
        Mockito.when(storagePoolVOMock.getPoolType()).thenReturn(StoragePoolType.CLVM_NG);
        Mockito.when(volumeInfoMock.getDestinationHostId()).thenReturn(DEST_HOST_ID);
        Mockito.doReturn(endPointMock).when(defaultEndPointSelectorSpy).getEndPointFromHostId(DEST_HOST_ID);

        EndPoint result = defaultEndPointSelectorSpy.select(srcDataMock, volumeInfoMock, false);

        assertNotNull(result);
        assertEquals(endPointMock, result);
        Mockito.verify(defaultEndPointSelectorSpy, Mockito.times(1)).getEndPointFromHostId(DEST_HOST_ID);
    }

    @Test
    public void testSelectTwoObjects_TemplateToVolume_CLVMWithoutDestHost() {
        DataObject srcDataMock = Mockito.mock(DataObject.class);
        DataStore srcStoreMock = Mockito.mock(DataStore.class);

        Mockito.when(srcDataMock.getDataStore()).thenReturn(srcStoreMock);
        Mockito.when(srcStoreMock.getRole()).thenReturn(DataStoreRole.Image);

        Mockito.when(volumeInfoMock.getDataStore()).thenReturn(datastoreMock);
        Mockito.when(datastoreMock.getRole()).thenReturn(DataStoreRole.Primary);
        Mockito.when(datastoreMock.getId()).thenReturn(STORE_ID);
        Mockito.when(_storagePoolDao.findById(STORE_ID)).thenReturn(storagePoolVOMock);
        Mockito.when(storagePoolVOMock.getPoolType()).thenReturn(StoragePoolType.CLVM);
        Mockito.when(volumeInfoMock.getDestinationHostId()).thenReturn(null);
        Mockito.doReturn(endPointMock).when(defaultEndPointSelectorSpy).findEndPointForImageMove(
                srcStoreMock, datastoreMock, false);

        EndPoint result = defaultEndPointSelectorSpy.select(srcDataMock, volumeInfoMock, false);

        assertNotNull(result);
        Mockito.verify(defaultEndPointSelectorSpy, Mockito.times(1)).findEndPointForImageMove(srcStoreMock, datastoreMock, false);
    }

}

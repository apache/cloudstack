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
package com.cloud.storage.snapshot;

import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.apache.cloudstack.acl.ControlledEntity;
import org.apache.cloudstack.acl.SecurityChecker.AccessType;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotService;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotStrategy;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotStrategy.SnapshotOperation;
import org.apache.cloudstack.engine.subsystem.api.storage.StorageStrategyFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreVO;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import com.cloud.configuration.Resource.ResourceType;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.resource.ResourceManager;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.ScopeType;
import com.cloud.storage.Snapshot;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountVO;
import com.cloud.user.ResourceLimitService;
import com.cloud.user.User;
import com.cloud.user.UserVO;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VirtualMachine.State;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.snapshot.VMSnapshot;
import com.cloud.vm.snapshot.VMSnapshotVO;
import com.cloud.vm.snapshot.dao.VMSnapshotDao;

public class SnapshotManagerTest {
    @Spy
    SnapshotManagerImpl _snapshotMgr = new SnapshotManagerImpl();
    @Mock
    SnapshotDao _snapshotDao;
    @Mock
    VolumeDao _volumeDao;
    @Mock
    UserVmDao _vmDao;
    @Mock
    PrimaryDataStoreDao _storagePoolDao;
    @Mock
    VMSnapshotDao _vmSnapshotDao;
    @Mock
    VMSnapshotVO vmSnapshotMock;
    @Mock
    Account account;
    @Mock
    UserVmVO vmMock;
    @Mock
    VolumeVO volumeMock;
    @Mock
    VolumeInfo volumeInfoMock;
    @Mock
    VolumeDataFactory volumeFactory;
    @Mock
    SnapshotVO snapshotMock;
    @Mock
    SnapshotInfo snapshotInfoMock;
    @Mock
    SnapshotDataFactory snapshotFactory;
    @Mock
    StorageStrategyFactory _storageStrategyFactory;
    @Mock
    SnapshotStrategy snapshotStrategy;
    @Mock
    AccountManager _accountMgr;
    @Mock
    ResourceLimitService _resourceLimitMgr;
    @Mock
    StoragePoolVO poolMock;
    @Mock
    ResourceManager _resourceMgr;
    @Mock
    DataStore storeMock;
    @Mock
    SnapshotDataStoreDao snapshotStoreDao;
    @Mock
    SnapshotDataStoreVO snapshotStoreMock;
    @Mock
    SnapshotService snapshotSrv;


    private static final long TEST_SNAPSHOT_ID = 3L;
    private static final long TEST_VOLUME_ID = 4L;
    private static final long TEST_VM_ID = 5L;
    private static final long TEST_STORAGE_POOL_ID = 6L;
    private static final long TEST_VM_SNAPSHOT_ID = 6L;

    @Before
    public void setup() throws ResourceAllocationException {
        MockitoAnnotations.initMocks(this);
        _snapshotMgr._snapshotDao = _snapshotDao;
        _snapshotMgr._volsDao = _volumeDao;
        _snapshotMgr._vmDao = _vmDao;
        _snapshotMgr.volFactory = volumeFactory;
        _snapshotMgr.snapshotFactory = snapshotFactory;
        _snapshotMgr._storageStrategyFactory = _storageStrategyFactory;
        _snapshotMgr._accountMgr = _accountMgr;
        _snapshotMgr._resourceLimitMgr = _resourceLimitMgr;
        _snapshotMgr._storagePoolDao = _storagePoolDao;
        _snapshotMgr._resourceMgr = _resourceMgr;
        _snapshotMgr._vmSnapshotDao = _vmSnapshotDao;
        _snapshotMgr._snapshotStoreDao = snapshotStoreDao;

        when(_snapshotDao.findById(anyLong())).thenReturn(snapshotMock);
        when(snapshotMock.getVolumeId()).thenReturn(TEST_VOLUME_ID);
        when(snapshotMock.isRecursive()).thenReturn(false);

        when(_volumeDao.findById(anyLong())).thenReturn(volumeMock);
        when(volumeMock.getState()).thenReturn(Volume.State.Ready);
        when(volumeFactory.getVolume(anyLong())).thenReturn(volumeInfoMock);
        when(volumeInfoMock.getDataStore()).thenReturn(storeMock);
        when(volumeInfoMock.getState()).thenReturn(Volume.State.Ready);
        when(storeMock.getId()).thenReturn(TEST_STORAGE_POOL_ID);

        when(snapshotFactory.getSnapshot(anyLong(), Mockito.any(DataStoreRole.class))).thenReturn(snapshotInfoMock);
        when(_storageStrategyFactory.getSnapshotStrategy(Mockito.any(SnapshotVO.class), Mockito.eq(SnapshotOperation.BACKUP))).thenReturn(snapshotStrategy);
        when(_storageStrategyFactory.getSnapshotStrategy(Mockito.any(SnapshotVO.class), Mockito.eq(SnapshotOperation.REVERT))).thenReturn(snapshotStrategy);
        when(_storageStrategyFactory.getSnapshotStrategy(Mockito.any(SnapshotVO.class), Mockito.eq(SnapshotOperation.DELETE))).thenReturn(snapshotStrategy);

        doNothing().when(_accountMgr).checkAccess(any(Account.class), any(AccessType.class), any(Boolean.class), any(ControlledEntity.class));
        doNothing().when(_snapshotMgr._resourceLimitMgr).checkResourceLimit(any(Account.class), any(ResourceType.class));
        doNothing().when(_snapshotMgr._resourceLimitMgr).checkResourceLimit(any(Account.class), any(ResourceType.class), anyLong());
        doNothing().when(_snapshotMgr._resourceLimitMgr).decrementResourceCount(anyLong(), any(ResourceType.class), anyLong());
        doNothing().when(_snapshotMgr._resourceLimitMgr).incrementResourceCount(anyLong(), any(ResourceType.class));
        doNothing().when(_snapshotMgr._resourceLimitMgr).incrementResourceCount(anyLong(), any(ResourceType.class), anyLong());

        Account account = new AccountVO("testaccount", 1L, "networkdomain", (short)0, "uuid");
        UserVO user = new UserVO(1, "testuser", "password", "firstname", "lastName", "email", "timezone", UUID.randomUUID().toString(), User.Source.UNKNOWN);
        CallContext.register(user, account);
        when(_accountMgr.getAccount(anyLong())).thenReturn(account);

        when(_storagePoolDao.findById(anyLong())).thenReturn(poolMock);
        when(poolMock.getScope()).thenReturn(ScopeType.ZONE);
        when(poolMock.getHypervisor()).thenReturn(HypervisorType.KVM);
        when(_resourceMgr.listAllUpAndEnabledHostsInOneZoneByHypervisor(any(HypervisorType.class), anyLong())).thenReturn(null);
    }

    @After
    public void tearDown() {
        CallContext.unregister();
    }

    // vm is destroyed
    @Test(expected = CloudRuntimeException.class)
    public void testAllocSnapshotF1() throws ResourceAllocationException {
        when(_vmDao.findById(anyLong())).thenReturn(vmMock);
        when(vmMock.getState()).thenReturn(State.Destroyed);
        _snapshotMgr.allocSnapshot(TEST_VOLUME_ID, Snapshot.MANUAL_POLICY_ID, null, null);
    }

    // active snapshots
    @SuppressWarnings("unchecked")
    @Test(expected = InvalidParameterValueException.class)
    public void testAllocSnapshotF2() throws ResourceAllocationException {
        when(_vmDao.findById(anyLong())).thenReturn(vmMock);
        when(vmMock.getId()).thenReturn(TEST_VM_ID);
        when(vmMock.getState()).thenReturn(State.Stopped);
        when(vmMock.getHypervisorType()).thenReturn(Hypervisor.HypervisorType.KVM);
        when(volumeInfoMock.getInstanceId()).thenReturn(TEST_VM_ID);
        List<SnapshotVO> mockList = mock(List.class);
        when(mockList.size()).thenReturn(1);
        when(_snapshotDao.listByInstanceId(TEST_VM_ID, Snapshot.State.Creating, Snapshot.State.CreatedOnPrimary, Snapshot.State.BackingUp)).thenReturn(mockList);
        _snapshotMgr.allocSnapshot(TEST_VOLUME_ID, Snapshot.MANUAL_POLICY_ID, null, null);
    }

    // active vm snapshots
    @SuppressWarnings("unchecked")
    @Test(expected = CloudRuntimeException.class)
    public void testAllocSnapshotF3() throws ResourceAllocationException {
        when(_vmDao.findById(anyLong())).thenReturn(vmMock);
        when(vmMock.getId()).thenReturn(TEST_VM_ID);
        when(vmMock.getState()).thenReturn(State.Stopped);
        when(vmMock.getHypervisorType()).thenReturn(Hypervisor.HypervisorType.KVM);
        when(volumeInfoMock.getInstanceId()).thenReturn(TEST_VM_ID);
        List<SnapshotVO> mockList = mock(List.class);
        when(mockList.size()).thenReturn(0);
        when(_snapshotDao.listByInstanceId(TEST_VM_ID, Snapshot.State.Creating, Snapshot.State.CreatedOnPrimary, Snapshot.State.BackingUp)).thenReturn(mockList);
        List<VMSnapshotVO> mockList2 = mock(List.class);
        when(mockList2.size()).thenReturn(1);
        when(_vmSnapshotDao.listByInstanceId(TEST_VM_ID, VMSnapshot.State.Creating, VMSnapshot.State.Reverting, VMSnapshot.State.Expunging)).thenReturn(mockList2);
        _snapshotMgr.allocSnapshot(TEST_VOLUME_ID, Snapshot.MANUAL_POLICY_ID, null, null);
    }

    // successful test
    @SuppressWarnings("unchecked")
    @Test
    public void testAllocSnapshotF4() throws ResourceAllocationException {
        when(_vmDao.findById(anyLong())).thenReturn(vmMock);
        when(vmMock.getId()).thenReturn(TEST_VM_ID);
        when(vmMock.getState()).thenReturn(State.Stopped);
        when(vmMock.getHypervisorType()).thenReturn(Hypervisor.HypervisorType.KVM);
        when(volumeInfoMock.getInstanceId()).thenReturn(TEST_VM_ID);
        List<SnapshotVO> mockList = mock(List.class);
        when(mockList.size()).thenReturn(0);
        when(_snapshotDao.listByInstanceId(TEST_VM_ID, Snapshot.State.Creating, Snapshot.State.CreatedOnPrimary, Snapshot.State.BackingUp)).thenReturn(mockList);
        List<VMSnapshotVO> mockList2 = mock(List.class);
        when(mockList2.size()).thenReturn(0);
        when(_vmSnapshotDao.listByInstanceId(TEST_VM_ID, VMSnapshot.State.Creating, VMSnapshot.State.Reverting, VMSnapshot.State.Expunging)).thenReturn(mockList2);
        when(_snapshotDao.persist(any(SnapshotVO.class))).thenReturn(snapshotMock);
        _snapshotMgr.allocSnapshot(TEST_VOLUME_ID, Snapshot.MANUAL_POLICY_ID, null, null);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testDeleteSnapshotF1() {
        when(snapshotStrategy.deleteSnapshot(TEST_SNAPSHOT_ID)).thenReturn(true);
        when(snapshotMock.getState()).thenReturn(Snapshot.State.Destroyed);
        when(snapshotMock.getAccountId()).thenReturn(2L);
        when(snapshotMock.getDataCenterId()).thenReturn(2L);

        _snapshotMgr.deleteSnapshot(TEST_SNAPSHOT_ID);
    }

    // vm state not stopped
    @Test(expected = InvalidParameterValueException.class)
    public void testRevertSnapshotF1() {
        when(volumeMock.getInstanceId()).thenReturn(TEST_VM_ID);
        when(_vmDao.findById(anyLong())).thenReturn(vmMock);
        when(vmMock.getState()).thenReturn(State.Running);
        _snapshotMgr.revertSnapshot(TEST_SNAPSHOT_ID);
    }

    // vm on Xenserver, return null
    @Test
    public void testRevertSnapshotF2() {
        when(_vmDao.findById(anyLong())).thenReturn(vmMock);
        when(vmMock.getState()).thenReturn(State.Stopped);
        when(vmMock.getHypervisorType()).thenReturn(Hypervisor.HypervisorType.XenServer);
        when(volumeMock.getFormat()).thenReturn(ImageFormat.VHD);
        Snapshot snapshot = _snapshotMgr.revertSnapshot(TEST_SNAPSHOT_ID);
        Assert.assertNull(snapshot);
    }

    // vm on KVM, successful
    @Test
    public void testRevertSnapshotF3() {
        when(_vmDao.findById(anyLong())).thenReturn(vmMock);
        when(vmMock.getState()).thenReturn(State.Stopped);
        when(vmMock.getHypervisorType()).thenReturn(Hypervisor.HypervisorType.KVM);
        when(volumeMock.getFormat()).thenReturn(ImageFormat.QCOW2);
        when (snapshotStrategy.revertSnapshot(Mockito.any(SnapshotInfo.class))).thenReturn(true);
        when(_volumeDao.update(anyLong(), any(VolumeVO.class))).thenReturn(true);
        Snapshot snapshot = _snapshotMgr.revertSnapshot(TEST_SNAPSHOT_ID);
        Assert.assertNotNull(snapshot);
    }

    // vm on Xenserver, expected exception
    @Test(expected = InvalidParameterValueException.class)
    public void testBackupSnapshotFromVmSnapshotF1() {
        when(_vmDao.findById(anyLong())).thenReturn(vmMock);
        when(vmMock.getHypervisorType()).thenReturn(Hypervisor.HypervisorType.XenServer);
        Snapshot snapshot = _snapshotMgr.backupSnapshotFromVmSnapshot(TEST_SNAPSHOT_ID, TEST_VM_ID, TEST_VOLUME_ID, TEST_VM_SNAPSHOT_ID);
        Assert.assertNull(snapshot);
    }

    // vm on KVM, first time
    @Test
    public void testBackupSnapshotFromVmSnapshotF2() {
        when(_vmDao.findById(nullable(Long.class))).thenReturn(vmMock);
        when(vmMock.getHypervisorType()).thenReturn(Hypervisor.HypervisorType.KVM);
        when(_vmSnapshotDao.findById(nullable(Long.class))).thenReturn(vmSnapshotMock);
        when(snapshotStoreDao.findParent(any(DataStoreRole.class), nullable(Long.class), nullable(Long.class))).thenReturn(null);
        when(snapshotFactory.getSnapshot(nullable(Long.class), nullable(DataStore.class))).thenReturn(snapshotInfoMock);
        when(storeMock.create(snapshotInfoMock)).thenReturn(snapshotInfoMock);
        when(snapshotStoreDao.findByStoreSnapshot(nullable(DataStoreRole.class), nullable(Long.class), nullable(Long.class))).thenReturn(snapshotStoreMock);
        when(snapshotStoreDao.update(nullable(Long.class), nullable(SnapshotDataStoreVO.class))).thenReturn(true);
        when(_snapshotDao.update(nullable(Long.class), nullable(SnapshotVO.class))).thenReturn(true);
        when(vmMock.getAccountId()).thenReturn(2L);
        when(snapshotStrategy.backupSnapshot(nullable(SnapshotInfo.class))).thenReturn(snapshotInfoMock);

        Snapshot snapshot = _snapshotMgr.backupSnapshotFromVmSnapshot(TEST_SNAPSHOT_ID, TEST_VM_ID, TEST_VOLUME_ID, TEST_VM_SNAPSHOT_ID);
        Assert.assertNotNull(snapshot);
    }

    // vm on KVM, already backed up
    @Test//(expected = InvalidParameterValueException.class)
    public void testBackupSnapshotFromVmSnapshotF3() {
        when(_vmDao.findById(nullable(Long.class))).thenReturn(vmMock);
        when(vmMock.getHypervisorType()).thenReturn(Hypervisor.HypervisorType.KVM);
        when(_vmSnapshotDao.findById(nullable(Long.class))).thenReturn(vmSnapshotMock);
        when(snapshotStoreDao.findParent(any(DataStoreRole.class), nullable(Long.class), nullable(Long.class))).thenReturn(snapshotStoreMock);
        when(snapshotStoreDao.findByStoreSnapshot(nullable(DataStoreRole.class), nullable(Long.class), nullable(Long.class))).thenReturn(snapshotStoreMock);
        when(snapshotStoreMock.getInstallPath()).thenReturn("VM_SNAPSHOT_NAME");
        when(vmSnapshotMock.getName()).thenReturn("VM_SNAPSHOT_NAME");
        Snapshot snapshot = _snapshotMgr.backupSnapshotFromVmSnapshot(TEST_SNAPSHOT_ID, TEST_VM_ID, TEST_VOLUME_ID, TEST_VM_SNAPSHOT_ID);
        Assert.assertNull(snapshot);
    }

    @Test(expected = CloudRuntimeException.class)
    public void testArchiveSnapshotSnapshotNotOnPrimary() {
        when(snapshotFactory.getSnapshot(anyLong(), Mockito.eq(DataStoreRole.Primary))).thenReturn(null);
        _snapshotMgr.archiveSnapshot(TEST_SNAPSHOT_ID);
    }

    @Test(expected = CloudRuntimeException.class)
    public void testArchiveSnapshotSnapshotNotReady() {
        when(snapshotFactory.getSnapshot(anyLong(), Mockito.eq(DataStoreRole.Primary))).thenReturn(snapshotInfoMock);
        when(snapshotInfoMock.getStatus()).thenReturn(ObjectInDataStoreStateMachine.State.Destroyed);
        _snapshotMgr.archiveSnapshot(TEST_SNAPSHOT_ID);
    }
}

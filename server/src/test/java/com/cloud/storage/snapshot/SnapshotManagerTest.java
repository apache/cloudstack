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

import com.cloud.configuration.Resource.ResourceType;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.resource.ResourceManager;
import com.cloud.server.TaggedResourceService;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.ScopeType;
import com.cloud.storage.Snapshot;
import com.cloud.storage.SnapshotPolicyVO;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.storage.dao.SnapshotPolicyDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountVO;
import com.cloud.user.ResourceLimitService;
import com.cloud.user.User;
import com.cloud.user.UserVO;
import com.cloud.utils.DateUtil;
import com.cloud.utils.DateUtil.IntervalType;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VirtualMachine.State;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.snapshot.VMSnapshot;
import com.cloud.vm.snapshot.VMSnapshotVO;
import com.cloud.vm.snapshot.dao.VMSnapshotDao;
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
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.snapshot.SnapshotHelper;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreVO;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.verification.VerificationMode;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
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
    @Mock
    SnapshotHelper snapshotHelperMock;

    @Mock
    GlobalLock globalLockMock;

    @Mock
    SnapshotPolicyDao snapshotPolicyDaoMock;

    @Mock
    SnapshotPolicyVO snapshotPolicyVoMock;

    @Mock
    HashMap<String,String> mapStringStringMock;

    @Mock
    SnapshotScheduler snapshotSchedulerMock;

    @Mock
    TaggedResourceService taggedResourceServiceMock;
    @Mock
    DataCenterDao dataCenterDao;

    SnapshotPolicyVO snapshotPolicyVoInstance;

    List<DateUtil.IntervalType> listIntervalTypes = Arrays.asList(DateUtil.IntervalType.values());

    private static final long TEST_SNAPSHOT_ID = 3L;
    private static final long TEST_VOLUME_ID = 4L;
    private static final long TEST_VM_ID = 5L;
    private static final long TEST_STORAGE_POOL_ID = 6L;
    private static final long TEST_VM_SNAPSHOT_ID = 6L;
    private static final String TEST_SNAPSHOT_POLICY_SCHEDULE = "";
    private static final String TEST_SNAPSHOT_POLICY_TIMEZONE = "";
    private static final IntervalType TEST_SNAPSHOT_POLICY_INTERVAL = IntervalType.MONTHLY;
    private static final int TEST_SNAPSHOT_POLICY_MAX_SNAPS = 1;
    private static final boolean TEST_SNAPSHOT_POLICY_DISPLAY = true;
    private static final boolean TEST_SNAPSHOT_POLICY_ACTIVE = true;

    private AutoCloseable closeable;

    @Before
    public void setup() throws ResourceAllocationException {
        closeable = MockitoAnnotations.openMocks(this);
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
        _snapshotMgr.snapshotHelper = snapshotHelperMock;
        _snapshotMgr._snapshotPolicyDao = snapshotPolicyDaoMock;
        _snapshotMgr._snapSchedMgr = snapshotSchedulerMock;
        _snapshotMgr.taggedResourceService = taggedResourceServiceMock;
        _snapshotMgr.dataCenterDao = dataCenterDao;

        when(_snapshotDao.findById(anyLong())).thenReturn(snapshotMock);
        when(snapshotMock.getVolumeId()).thenReturn(TEST_VOLUME_ID);

        when(_volumeDao.findById(anyLong())).thenReturn(volumeMock);
        when(volumeMock.getState()).thenReturn(Volume.State.Ready);
        when(volumeFactory.getVolume(anyLong())).thenReturn(volumeInfoMock);
        when(volumeInfoMock.getDataStore()).thenReturn(storeMock);
        when(volumeInfoMock.getState()).thenReturn(Volume.State.Ready);
        when(storeMock.getId()).thenReturn(TEST_STORAGE_POOL_ID);

        when(snapshotFactory.getSnapshot(anyLong(), any(DataStoreRole.class))).thenReturn(snapshotInfoMock);
        when(_storageStrategyFactory.getSnapshotStrategy(any(SnapshotVO.class), Mockito.eq(SnapshotOperation.BACKUP))).thenReturn(snapshotStrategy);
        when(_storageStrategyFactory.getSnapshotStrategy(any(SnapshotVO.class), Mockito.eq(SnapshotOperation.REVERT))).thenReturn(snapshotStrategy);

        doNothing().when(_snapshotMgr._resourceLimitMgr).checkResourceLimit(any(Account.class), any(ResourceType.class));
        doNothing().when(_snapshotMgr._resourceLimitMgr).checkResourceLimit(any(Account.class), any(ResourceType.class), anyLong());
        doNothing().when(_snapshotMgr._resourceLimitMgr).decrementResourceCount(anyLong(), any(ResourceType.class), anyLong());
        doNothing().when(_snapshotMgr._resourceLimitMgr).incrementResourceCount(anyLong(), any(ResourceType.class));
        doNothing().when(_snapshotMgr._resourceLimitMgr).incrementResourceCount(anyLong(), any(ResourceType.class), anyLong());

        Account account = new AccountVO("testaccount", 1L, "networkdomain", Account.Type.NORMAL, "uuid");
        UserVO user = new UserVO(1, "testuser", "password", "firstname", "lastName", "email", "timezone", UUID.randomUUID().toString(), User.Source.UNKNOWN);
        CallContext.register(user, account);
        when(_accountMgr.getAccount(anyLong())).thenReturn(account);

        when(_storagePoolDao.findById(anyLong())).thenReturn(poolMock);
        when(poolMock.getScope()).thenReturn(ScopeType.ZONE);
        when(poolMock.getHypervisor()).thenReturn(HypervisorType.KVM);
        when(_resourceMgr.listAllUpAndEnabledHostsInOneZoneByHypervisor(any(HypervisorType.class), anyLong())).thenReturn(null);

        snapshotPolicyVoInstance = new SnapshotPolicyVO(TEST_VOLUME_ID, TEST_SNAPSHOT_POLICY_SCHEDULE, TEST_SNAPSHOT_POLICY_TIMEZONE, TEST_SNAPSHOT_POLICY_INTERVAL,
          TEST_SNAPSHOT_POLICY_MAX_SNAPS, TEST_SNAPSHOT_POLICY_DISPLAY);
    }

    @After
    public void tearDown() throws Exception {
        CallContext.unregister();
        closeable.close();
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
        when(snapshotMock.getState()).thenReturn(Snapshot.State.Destroyed);

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
        doReturn(DataStoreRole.Image).when(snapshotHelperMock).getDataStoreRole(any());
        Snapshot snapshot = _snapshotMgr.revertSnapshot(TEST_SNAPSHOT_ID);
        Assert.assertNull(snapshot);
    }

    // vm on KVM, successful
    @Test
    public void testRevertSnapshotF3() {
        when(_vmDao.findById(anyLong())).thenReturn(vmMock);
        when(vmMock.getState()).thenReturn(State.Stopped);
        when (snapshotStrategy.revertSnapshot(any(SnapshotInfo.class))).thenReturn(true);
        when(_volumeDao.update(anyLong(), any(VolumeVO.class))).thenReturn(true);
        doReturn(DataStoreRole.Image).when(snapshotHelperMock).getDataStoreRole(any());
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
        when(snapshotStoreDao.findByStoreSnapshot(nullable(DataStoreRole.class), nullable(Long.class), nullable(Long.class))).thenReturn(snapshotStoreMock);
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

    public void assertSnapshotPolicyResultAgainstPreBuiltInstance(SnapshotPolicyVO snapshotPolicyVo){
        Assert.assertEquals(snapshotPolicyVoInstance.getVolumeId(), snapshotPolicyVo.getVolumeId());
        Assert.assertEquals(snapshotPolicyVoInstance.getSchedule(), snapshotPolicyVo.getSchedule());
        Assert.assertEquals(snapshotPolicyVoInstance.getTimezone(), snapshotPolicyVo.getTimezone());
        Assert.assertEquals(snapshotPolicyVoInstance.getInterval(), snapshotPolicyVo.getInterval());
        Assert.assertEquals(snapshotPolicyVoInstance.getMaxSnaps(), snapshotPolicyVo.getMaxSnaps());
        Assert.assertEquals(snapshotPolicyVoInstance.isDisplay(), snapshotPolicyVo.isDisplay());
        Assert.assertEquals(snapshotPolicyVoInstance.isActive(), snapshotPolicyVo.isActive());
    }

    @Test
    public void validateCreateSnapshotPolicy(){
        Mockito.doReturn(snapshotPolicyVoInstance).when(snapshotPolicyDaoMock).persist(any());
        Mockito.doReturn(null).when(snapshotSchedulerMock).scheduleNextSnapshotJob(any());

        SnapshotPolicyVO result = _snapshotMgr.createSnapshotPolicy(TEST_VOLUME_ID, TEST_SNAPSHOT_POLICY_SCHEDULE, TEST_SNAPSHOT_POLICY_TIMEZONE, TEST_SNAPSHOT_POLICY_INTERVAL,
          TEST_SNAPSHOT_POLICY_MAX_SNAPS, TEST_SNAPSHOT_POLICY_DISPLAY);

        assertSnapshotPolicyResultAgainstPreBuiltInstance(result);
    }

    @Test
    public void validateUpdateSnapshotPolicy(){
        Mockito.doReturn(true).when(snapshotPolicyDaoMock).update(anyLong(), any());
        Mockito.doNothing().when(snapshotSchedulerMock).scheduleOrCancelNextSnapshotJobOnDisplayChange(any(), Mockito.anyBoolean());
        Mockito.doReturn(true).when(taggedResourceServiceMock).deleteTags(any(), any(), any());

        SnapshotPolicyVO snapshotPolicyVo = new SnapshotPolicyVO(TEST_VOLUME_ID, TEST_SNAPSHOT_POLICY_SCHEDULE, TEST_SNAPSHOT_POLICY_TIMEZONE, TEST_SNAPSHOT_POLICY_INTERVAL,
          TEST_SNAPSHOT_POLICY_MAX_SNAPS, TEST_SNAPSHOT_POLICY_DISPLAY);

        _snapshotMgr.updateSnapshotPolicy(snapshotPolicyVo, TEST_SNAPSHOT_POLICY_SCHEDULE, TEST_SNAPSHOT_POLICY_TIMEZONE,
          TEST_SNAPSHOT_POLICY_INTERVAL, TEST_SNAPSHOT_POLICY_MAX_SNAPS, TEST_SNAPSHOT_POLICY_DISPLAY, TEST_SNAPSHOT_POLICY_ACTIVE);

        assertSnapshotPolicyResultAgainstPreBuiltInstance(snapshotPolicyVo);
    }

    @Test
    public void validateCreateTagsForSnapshotPolicyWithNullTags(){
        _snapshotMgr.createTagsForSnapshotPolicy(null, snapshotPolicyVoMock);
        Mockito.verify(taggedResourceServiceMock, Mockito.never()).createTags(any(), any(), any(), any());
    }

    @Test
    public void validateCreateTagsForSnapshotPolicyWithEmptyTags(){
        _snapshotMgr.createTagsForSnapshotPolicy(new HashMap<>(), snapshotPolicyVoMock);
        Mockito.verify(taggedResourceServiceMock, Mockito.never()).createTags(any(), any(), any(), any());
    }

    @Test
    public void validateCreateTagsForSnapshotPolicyWithValidTags(){
        Mockito.doReturn(null).when(taggedResourceServiceMock).createTags(any(), any(), any(), any());

        Map map = new HashMap<>();
        map.put("test", "test");

        _snapshotMgr.createTagsForSnapshotPolicy(map, snapshotPolicyVoMock);
        Mockito.verify(taggedResourceServiceMock, Mockito.times(1)).createTags(any(), any(), any(), any());
    }

    @Test(expected = CloudRuntimeException.class)
    public void validatePersistSnapshotPolicyLockIsNotAquiredMustThrowException() {
        try (MockedStatic<GlobalLock> ignored = Mockito.mockStatic(GlobalLock.class)) {
            BDDMockito.given(GlobalLock.getInternLock(Mockito.anyString())).willReturn(globalLockMock);
            Mockito.doReturn(false).when(globalLockMock).lock(Mockito.anyInt());

            _snapshotMgr.persistSnapshotPolicy(volumeMock, TEST_SNAPSHOT_POLICY_SCHEDULE, TEST_SNAPSHOT_POLICY_TIMEZONE, TEST_SNAPSHOT_POLICY_INTERVAL, TEST_SNAPSHOT_POLICY_MAX_SNAPS,
                    TEST_SNAPSHOT_POLICY_DISPLAY, TEST_SNAPSHOT_POLICY_ACTIVE, mapStringStringMock);
        }
    }

    @Test
    public void validatePersistSnapshotPolicyLockAquiredCreateSnapshotPolicy() {
        try (MockedStatic<GlobalLock> ignored = Mockito.mockStatic(GlobalLock.class)) {

            BDDMockito.given(GlobalLock.getInternLock(Mockito.anyString())).willReturn(globalLockMock);
            Mockito.doReturn(true).when(globalLockMock).lock(Mockito.anyInt());

            for (IntervalType intervalType : listIntervalTypes) {

                Mockito.doReturn(null).when(snapshotPolicyDaoMock).findOneByVolumeInterval(anyLong(), Mockito.eq(intervalType));
                Mockito.doReturn(snapshotPolicyVoInstance).when(_snapshotMgr).createSnapshotPolicy(anyLong(), Mockito.anyString(), Mockito.anyString(), Mockito.eq(intervalType),
                        Mockito.anyInt(), Mockito.anyBoolean());

                SnapshotPolicyVO result = _snapshotMgr.persistSnapshotPolicy(volumeMock, TEST_SNAPSHOT_POLICY_SCHEDULE, TEST_SNAPSHOT_POLICY_TIMEZONE, intervalType,
                        TEST_SNAPSHOT_POLICY_MAX_SNAPS, TEST_SNAPSHOT_POLICY_DISPLAY, TEST_SNAPSHOT_POLICY_ACTIVE, null);

                assertSnapshotPolicyResultAgainstPreBuiltInstance(result);
            }

            VerificationMode timesVerification = Mockito.times(listIntervalTypes.size());
            Mockito.verify(_snapshotMgr, timesVerification).createSnapshotPolicy(anyLong(), Mockito.anyString(), Mockito.anyString(), any(DateUtil.IntervalType.class),
                    Mockito.anyInt(), Mockito.anyBoolean());
            Mockito.verify(_snapshotMgr, Mockito.never()).updateSnapshotPolicy(any(SnapshotPolicyVO.class), Mockito.anyString(), Mockito.anyString(),
                    any(DateUtil.IntervalType.class), Mockito.anyInt(), Mockito.anyBoolean(), Mockito.anyBoolean());
            Mockito.verify(_snapshotMgr, timesVerification).createTagsForSnapshotPolicy(any(), any());
        }
    }

    @Test
    public void validatePersistSnapshotPolicyLockAquiredUpdateSnapshotPolicy() {
        try (MockedStatic<GlobalLock> ignored = Mockito.mockStatic(GlobalLock.class)) {

            BDDMockito.given(GlobalLock.getInternLock(Mockito.anyString())).willReturn(globalLockMock);
            Mockito.doReturn(true).when(globalLockMock).lock(Mockito.anyInt());

            for (IntervalType intervalType : listIntervalTypes) {
                Mockito.doReturn(snapshotPolicyVoInstance).when(snapshotPolicyDaoMock).findOneByVolumeInterval(anyLong(), Mockito.eq(intervalType));
                Mockito.doNothing().when(_snapshotMgr).updateSnapshotPolicy(any(SnapshotPolicyVO.class), Mockito.anyString(), Mockito.anyString(),
                        any(DateUtil.IntervalType.class), Mockito.anyInt(), Mockito.anyBoolean(), Mockito.anyBoolean());

                SnapshotPolicyVO result = _snapshotMgr.persistSnapshotPolicy(volumeMock, TEST_SNAPSHOT_POLICY_SCHEDULE, TEST_SNAPSHOT_POLICY_TIMEZONE, intervalType,
                        TEST_SNAPSHOT_POLICY_MAX_SNAPS, TEST_SNAPSHOT_POLICY_DISPLAY, TEST_SNAPSHOT_POLICY_ACTIVE, null);

                assertSnapshotPolicyResultAgainstPreBuiltInstance(result);
            }

            VerificationMode timesVerification = Mockito.times(listIntervalTypes.size());
            Mockito.verify(_snapshotMgr, Mockito.never()).createSnapshotPolicy(anyLong(), Mockito.anyString(), Mockito.anyString(), any(DateUtil.IntervalType.class),
                    Mockito.anyInt(), Mockito.anyBoolean());
            Mockito.verify(_snapshotMgr, timesVerification).updateSnapshotPolicy(any(SnapshotPolicyVO.class), Mockito.anyString(), Mockito.anyString(),
                    any(DateUtil.IntervalType.class), Mockito.anyInt(), Mockito.anyBoolean(), Mockito.anyBoolean());
            Mockito.verify(_snapshotMgr, timesVerification).createTagsForSnapshotPolicy(any(), any());
        }
    }

    private void mockForBackupSnapshotToSecondaryZoneTest(final Boolean configValue, final DataCenter.Type dcType) {
        try {
            Field f = ConfigKey.class.getDeclaredField("_value");
            f.setAccessible(true);
            f.set(SnapshotInfo.BackupSnapshotAfterTakingSnapshot, configValue);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            Assert.fail(String.format("Failed to override config: %s", e.getMessage()));
        }
        DataCenterVO dc = Mockito.mock(DataCenterVO.class);
        Mockito.when(dc.getType()).thenReturn(dcType);
        Mockito.when(dataCenterDao.findById(1L)).thenReturn(dc);
    }
    @Test
    public void testIsBackupSnapshotToSecondaryForCoreZoneNullConfig() {
        mockForBackupSnapshotToSecondaryZoneTest(null, DataCenter.Type.Core);
        Assert.assertTrue(_snapshotMgr.isBackupSnapshotToSecondaryForZone(1L));
    }
    @Test
    public void testIsBackupSnapshotToSecondaryForCoreZoneEnabledConfig() {
        mockForBackupSnapshotToSecondaryZoneTest(true, DataCenter.Type.Core);
        Assert.assertTrue(_snapshotMgr.isBackupSnapshotToSecondaryForZone(1L));
    }
    @Test
    public void testIsBackupSnapshotToSecondaryForCoreZoneDisabledConfig() {
        mockForBackupSnapshotToSecondaryZoneTest(false, DataCenter.Type.Core);
        Assert.assertFalse(_snapshotMgr.isBackupSnapshotToSecondaryForZone(1L));
    }

    @Test
    public void testIsBackupSnapshotToSecondaryForEdgeZone() {
        mockForBackupSnapshotToSecondaryZoneTest(true, DataCenter.Type.Edge);
        Assert.assertFalse(_snapshotMgr.isBackupSnapshotToSecondaryForZone(1L));
    }
}

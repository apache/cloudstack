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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

import com.cloud.user.User;
import junit.framework.Assert;
import org.apache.cloudstack.api.command.user.volume.CreateVolumeCmd;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import org.apache.cloudstack.acl.ControlledEntity;
import org.apache.cloudstack.acl.SecurityChecker.AccessType;
import org.apache.cloudstack.api.command.user.volume.DetachVolumeCmd;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeService;
import org.apache.cloudstack.framework.jobs.AsyncJobExecutionContext;
import org.apache.cloudstack.framework.jobs.AsyncJobManager;
import org.apache.cloudstack.framework.jobs.dao.AsyncJobJoinMapDao;
import org.apache.cloudstack.framework.jobs.impl.AsyncJobVO;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountVO;
import com.cloud.user.UserVO;
import com.cloud.utils.db.TransactionLegacy;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VirtualMachine.State;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.VMInstanceDao;
import com.cloud.vm.snapshot.VMSnapshotVO;
import com.cloud.vm.snapshot.dao.VMSnapshotDao;

public class VolumeApiServiceImplTest {
    @Inject
    VolumeApiServiceImpl _svc = new VolumeApiServiceImpl();
    @Mock
    VolumeDao _volumeDao;
    @Mock
    AccountManager _accountMgr;
    @Mock
    UserVmDao _userVmDao;
    @Mock
    PrimaryDataStoreDao _storagePoolDao;
    @Mock
    VMSnapshotDao _vmSnapshotDao;
    @Mock
    AsyncJobManager _jobMgr;
    @Mock
    AsyncJobJoinMapDao _joinMapDao;
    @Mock
    VolumeDataFactory _volFactory;

    @Mock
    VMInstanceDao _vmInstanceDao;
    @Mock
    VolumeInfo volumeInfoMock;
    @Mock
    SnapshotInfo snapshotInfoMock;
    @Mock
    VolumeService volService;
    @Mock
    CreateVolumeCmd createVol;

    DetachVolumeCmd detachCmd = new DetachVolumeCmd();
    Class<?> _detachCmdClass = detachCmd.getClass();


    @Before
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);
        _svc._volsDao = _volumeDao;
        _svc._accountMgr = _accountMgr;
        _svc._userVmDao = _userVmDao;
        _svc._storagePoolDao = _storagePoolDao;
        _svc._vmSnapshotDao = _vmSnapshotDao;
        _svc._vmInstanceDao = _vmInstanceDao;
        _svc._jobMgr = _jobMgr;
        _svc.volFactory = _volFactory;
        _svc.volService = volService;

        // mock caller context
        AccountVO account = new AccountVO("admin", 1L, "networkDomain", Account.ACCOUNT_TYPE_NORMAL, "uuid");
        UserVO user = new UserVO(1, "testuser", "password", "firstname", "lastName", "email", "timezone", UUID.randomUUID().toString(), User.Source.UNKNOWN);
        CallContext.register(user, account);
        // mock async context
        AsyncJobExecutionContext context = new AsyncJobExecutionContext();
        AsyncJobExecutionContext.init(_svc._jobMgr, _joinMapDao);
        AsyncJobVO job = new AsyncJobVO();
        context.setJob(job);
        AsyncJobExecutionContext.setCurrentExecutionContext(context);

        TransactionLegacy txn = TransactionLegacy.open("runVolumeDaoImplTest");
        try {
            // volume of running vm id=1
            VolumeVO volumeOfRunningVm = new VolumeVO("root", 1L, 1L, 1L, 1L, 1L, "root", "root", Storage.ProvisioningType.THIN, 1, null,
                    null, "root", Volume.Type.ROOT);
            when(_svc._volsDao.findById(1L)).thenReturn(volumeOfRunningVm);

            UserVmVO runningVm = new UserVmVO(1L, "vm", "vm", 1, HypervisorType.XenServer, 1L, false,
                    false, 1L, 1L, 1, 1L, null, "vm", null);
            runningVm.setState(State.Running);
            runningVm.setDataCenterId(1L);
            when(_svc._userVmDao.findById(1L)).thenReturn(runningVm);

            // volume of stopped vm id=2
            VolumeVO volumeOfStoppedVm = new VolumeVO("root", 1L, 1L, 1L, 1L, 2L, "root", "root", Storage.ProvisioningType.THIN, 1, null,
                    null, "root", Volume.Type.ROOT);
            volumeOfStoppedVm.setPoolId(1L);
            when(_svc._volsDao.findById(2L)).thenReturn(volumeOfStoppedVm);

            UserVmVO stoppedVm = new UserVmVO(2L, "vm", "vm", 1, HypervisorType.XenServer, 1L, false,
                    false, 1L, 1L, 1, 1L, null, "vm", null);
            stoppedVm.setState(State.Stopped);
            stoppedVm.setDataCenterId(1L);
            when(_svc._userVmDao.findById(2L)).thenReturn(stoppedVm);


            // volume of hyperV vm id=3
            UserVmVO hyperVVm = new UserVmVO(3L, "vm", "vm", 1, HypervisorType.Hyperv, 1L, false,
                    false, 1L, 1L, 1, 1L, null, "vm", null);
            hyperVVm.setState(State.Stopped);
            hyperVVm.setDataCenterId(1L);
            when(_svc._userVmDao.findById(3L)).thenReturn(hyperVVm);

            VolumeVO volumeOfStoppeHyperVVm = new VolumeVO("root", 1L, 1L, 1L, 1L, 3L, "root", "root", Storage.ProvisioningType.THIN, 1, null,
                    null, "root", Volume.Type.ROOT);
            volumeOfStoppeHyperVVm.setPoolId(1L);
            when(_svc._volsDao.findById(3L)).thenReturn(volumeOfStoppeHyperVVm);

            StoragePoolVO unmanagedPool = new StoragePoolVO();
            when(_svc._storagePoolDao.findById(1L)).thenReturn(unmanagedPool);

            // volume of managed pool id=4
            StoragePoolVO managedPool = new StoragePoolVO();
            managedPool.setManaged(true);
            when(_svc._storagePoolDao.findById(2L)).thenReturn(managedPool);
            VolumeVO managedPoolVolume = new VolumeVO("root", 1L, 1L, 1L, 1L, 2L, "root", "root", Storage.ProvisioningType.THIN, 1, null,
                    null, "root", Volume.Type.ROOT);
            managedPoolVolume.setPoolId(2L);
            when(_svc._volsDao.findById(4L)).thenReturn(managedPoolVolume);

            // non-root non-datadisk volume
            VolumeInfo volumeWithIncorrectVolumeType = Mockito.mock(VolumeInfo.class);
            when(volumeWithIncorrectVolumeType.getId()).thenReturn(5L);
            when(volumeWithIncorrectVolumeType.getVolumeType()).thenReturn(Volume.Type.ISO);
            when(_svc.volFactory.getVolume(5L)).thenReturn(volumeWithIncorrectVolumeType);

            // correct root volume
            VolumeInfo correctRootVolume = Mockito.mock(VolumeInfo.class);
            when(correctRootVolume.getId()).thenReturn(6L);
            when(correctRootVolume.getDataCenterId()).thenReturn(1L);
            when(correctRootVolume.getVolumeType()).thenReturn(Volume.Type.ROOT);
            when(correctRootVolume.getInstanceId()).thenReturn(null);
            when(_svc.volFactory.getVolume(6L)).thenReturn(correctRootVolume);

            VolumeVO correctRootVolumeVO = new VolumeVO("root", 1L, 1L, 1L, 1L, 2L, "root", "root", Storage.ProvisioningType.THIN, 1, null,
                    null, "root", Volume.Type.ROOT);
            when(_svc._volsDao.findById(6L)).thenReturn(correctRootVolumeVO);

            // managed root volume
            VolumeInfo managedVolume = Mockito.mock(VolumeInfo.class);
            when(managedVolume.getId()).thenReturn(7L);
            when(managedVolume.getDataCenterId()).thenReturn(1L);
            when(managedVolume.getVolumeType()).thenReturn(Volume.Type.ROOT);
            when(managedVolume.getInstanceId()).thenReturn(null);
            when(managedVolume.getPoolId()).thenReturn(2L);
            when(_svc.volFactory.getVolume(7L)).thenReturn(managedVolume);

            VolumeVO managedVolume1 = new VolumeVO("root", 1L, 1L, 1L, 1L, 2L, "root", "root", Storage.ProvisioningType.THIN, 1, null,
                    null, "root", Volume.Type.ROOT);
            managedVolume1.setPoolId(2L);
            managedVolume1.setDataCenterId(1L);
            when(_svc._volsDao.findById(7L)).thenReturn(managedVolume1);

            // vm having root volume
            UserVmVO vmHavingRootVolume = new UserVmVO(4L, "vm", "vm", 1, HypervisorType.XenServer, 1L, false,
                    false, 1L, 1L, 1, 1L, null, "vm", null);
            vmHavingRootVolume.setState(State.Stopped);
            vmHavingRootVolume.setDataCenterId(1L);
            when(_svc._userVmDao.findById(4L)).thenReturn(vmHavingRootVolume);
            List<VolumeVO> vols = new ArrayList<VolumeVO>();
            vols.add(new VolumeVO());
            when(_svc._volsDao.findByInstanceAndDeviceId(4L, 0L)).thenReturn(vols);

            // volume in uploaded state
            VolumeInfo uploadedVolume = Mockito.mock(VolumeInfo.class);
            when(uploadedVolume.getId()).thenReturn(8L);
            when(uploadedVolume.getDataCenterId()).thenReturn(1L);
            when(uploadedVolume.getVolumeType()).thenReturn(Volume.Type.ROOT);
            when(uploadedVolume.getInstanceId()).thenReturn(null);
            when(uploadedVolume.getPoolId()).thenReturn(1L);
            when(uploadedVolume.getState()).thenReturn(Volume.State.Uploaded);
            when(_svc.volFactory.getVolume(8L)).thenReturn(uploadedVolume);

            VolumeVO upVolume = new VolumeVO("root", 1L, 1L, 1L, 1L, 2L, "root", "root", Storage.ProvisioningType.THIN, 1, null,
                    null, "root", Volume.Type.ROOT);
            upVolume.setPoolId(1L);
            upVolume.setDataCenterId(1L);
            upVolume.setState(Volume.State.Uploaded);
            when(_svc._volsDao.findById(8L)).thenReturn(upVolume);

            // helper dao methods mock
            when(_svc._vmSnapshotDao.findByVm(any(Long.class))).thenReturn(new ArrayList<VMSnapshotVO>());
            when(_svc._vmInstanceDao.findById(any(Long.class))).thenReturn(stoppedVm);

        } finally {
            txn.close("runVolumeDaoImplTest");
        }

        // helper methods mock
        doNothing().when(_svc._accountMgr).checkAccess(any(Account.class), any(AccessType.class), any(Boolean.class), any(ControlledEntity.class));
        doNothing().when(_svc._jobMgr).updateAsyncJobAttachment(any(Long.class), any(String.class), any(Long.class));
        when(_svc._jobMgr.submitAsyncJob(any(AsyncJobVO.class), any(String.class), any(Long.class))).thenReturn(1L);
    }

    /**
     * TESTS FOR DETACH ROOT VOLUME, COUNT=4
     * @throws Exception
     */

    @Test(expected = InvalidParameterValueException.class)
    public void testDetachVolumeFromRunningVm() throws NoSuchFieldException, IllegalAccessException {
        Field dedicateIdField = _detachCmdClass.getDeclaredField("id");
        dedicateIdField.setAccessible(true);
        dedicateIdField.set(detachCmd, 1L);
        _svc.detachVolumeFromVM(detachCmd);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testDetachVolumeFromStoppedHyperVVm() throws NoSuchFieldException, IllegalAccessException {
        Field dedicateIdField = _detachCmdClass.getDeclaredField("id");
        dedicateIdField.setAccessible(true);
        dedicateIdField.set(detachCmd, 3L);
        _svc.detachVolumeFromVM(detachCmd);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testDetachVolumeOfManagedDataStore() throws NoSuchFieldException, IllegalAccessException {
        Field dedicateIdField = _detachCmdClass.getDeclaredField("id");
        dedicateIdField.setAccessible(true);
        dedicateIdField.set(detachCmd, 4L);
        _svc.detachVolumeFromVM(detachCmd);
    }

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testDetachVolumeFromStoppedXenVm() throws NoSuchFieldException, IllegalAccessException {
        thrown.expect(NullPointerException.class);
        Field dedicateIdField = _detachCmdClass.getDeclaredField("id");
        dedicateIdField.setAccessible(true);
        dedicateIdField.set(detachCmd, 2L);
        _svc.detachVolumeFromVM(detachCmd);
    }

    /**
     * TESTS FOR ATTACH ROOT VOLUME, COUNT=7
     */

    // Negative test - try to attach non-root non-datadisk volume
    @Test(expected = InvalidParameterValueException.class)
    public void attachIncorrectDiskType() throws NoSuchFieldException, IllegalAccessException {
        _svc.attachVolumeToVM(1L, 5L, 0L);
    }

    // Negative test - attach root volume to running vm
    @Test(expected = InvalidParameterValueException.class)
    public void attachRootDiskToRunningVm() throws NoSuchFieldException, IllegalAccessException {
        _svc.attachVolumeToVM(1L, 6L, 0L);
    }

    // Negative test - attach root volume to non-xen vm
    @Test(expected = InvalidParameterValueException.class)
    public void attachRootDiskToHyperVm() throws NoSuchFieldException, IllegalAccessException {
        _svc.attachVolumeToVM(3L, 6L, 0L);
    }

    // Negative test - attach root volume from the managed data store
    @Test(expected = InvalidParameterValueException.class)
    public void attachRootDiskOfManagedDataStore() throws NoSuchFieldException, IllegalAccessException {
        _svc.attachVolumeToVM(2L, 7L, 0L);
    }

    // Negative test - root volume can't be attached to the vm already having a root volume attached
    @Test(expected = InvalidParameterValueException.class)
    public void attachRootDiskToVmHavingRootDisk() throws NoSuchFieldException, IllegalAccessException {
        _svc.attachVolumeToVM(4L, 6L, 0L);
    }

    // Negative test - root volume in uploaded state can't be attached
    @Test(expected = InvalidParameterValueException.class)
    public void attachRootInUploadedState() throws NoSuchFieldException, IllegalAccessException {
        _svc.attachVolumeToVM(2L, 8L, 0L);
    }

    // Positive test - attach ROOT volume in correct state, to the vm not having root volume attached
    @Test
    public void attachRootVolumePositive() throws NoSuchFieldException, IllegalAccessException {
        thrown.expect(NullPointerException.class);
        _svc.attachVolumeToVM(2L, 6L, 0L);
    }

    // volume not Ready
    @Test(expected = InvalidParameterValueException.class)
    public void testTakeSnapshotF1() throws ResourceAllocationException {
        when(_volFactory.getVolume(anyLong())).thenReturn(volumeInfoMock);
        when(volumeInfoMock.getState()).thenReturn(Volume.State.Allocated);
        _svc.takeSnapshot(5L, Snapshot.MANUAL_POLICY_ID, 3L, null, false);
    }

    @Test
    public void testTakeSnapshotF2() throws ResourceAllocationException {
        when(_volFactory.getVolume(anyLong())).thenReturn(volumeInfoMock);
        when(volumeInfoMock.getState()).thenReturn(Volume.State.Ready);
        when(volumeInfoMock.getInstanceId()).thenReturn(null);
        when (volService.takeSnapshot(Mockito.any(VolumeInfo.class))).thenReturn(snapshotInfoMock);
        _svc.takeSnapshot(5L, Snapshot.MANUAL_POLICY_ID, 3L, null, false);
    }

    @Test
    public void testNullGetVolumeNameFromCmd() {
        when(createVol.getVolumeName()).thenReturn(null);
        Assert.assertNotNull(_svc.getVolumeNameFromCommand(createVol));
    }

    @Test
    public void testEmptyGetVolumeNameFromCmd() {
        when(createVol.getVolumeName()).thenReturn("");
        Assert.assertNotNull(_svc.getVolumeNameFromCommand(createVol));
    }

    @Test
    public void testBlankGetVolumeNameFromCmd() {
        when(createVol.getVolumeName()).thenReturn("   ");
        Assert.assertNotNull(_svc.getVolumeNameFromCommand(createVol));
    }

    @Test
    public void testNonEmptyGetVolumeNameFromCmd() {
        when(createVol.getVolumeName()).thenReturn("abc");
        Assert.assertSame(_svc.getVolumeNameFromCommand(createVol), "abc");
    }

    @After
    public void tearDown() {
        CallContext.unregister();
    }
}

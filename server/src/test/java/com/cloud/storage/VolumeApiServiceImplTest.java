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

import static org.junit.Assert.assertEquals;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.projects.Project;
import com.cloud.projects.ProjectManager;
import com.cloud.storage.dao.SnapshotDao;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import com.cloud.event.EventTypes;
import com.cloud.event.UsageEventUtils;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import org.apache.cloudstack.acl.ControlledEntity;
import org.apache.cloudstack.acl.SecurityChecker.AccessType;
import org.apache.cloudstack.api.command.user.volume.CreateVolumeCmd;
import org.apache.cloudstack.api.command.user.volume.DetachVolumeCmd;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeService;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeService.VolumeApiResult;
import org.apache.cloudstack.framework.async.AsyncCallFuture;
import org.apache.cloudstack.framework.jobs.AsyncJobExecutionContext;
import org.apache.cloudstack.framework.jobs.AsyncJobManager;
import org.apache.cloudstack.framework.jobs.dao.AsyncJobJoinMapDao;
import org.apache.cloudstack.framework.jobs.impl.AsyncJobVO;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.datastore.db.VolumeDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.VolumeDataStoreVO;
import org.apache.cloudstack.utils.bytescale.ByteScaleUtils;
import org.apache.commons.collections.CollectionUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloud.api.query.dao.ServiceOfferingJoinDao;
import com.cloud.api.query.vo.ServiceOfferingJoinVO;
import com.cloud.configuration.Resource;
import com.cloud.configuration.Resource.ResourceType;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.org.Grouping;
import com.cloud.serializer.GsonHelper;
import com.cloud.server.TaggedResourceService;
import com.cloud.storage.Storage.ProvisioningType;
import com.cloud.storage.Volume.Type;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.storage.dao.StoragePoolTagsDao;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.storage.snapshot.SnapshotManager;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountVO;
import com.cloud.user.ResourceLimitService;
import com.cloud.user.User;
import com.cloud.user.UserVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.db.TransactionLegacy;
import com.cloud.utils.fsm.NoTransitionException;
import com.cloud.vm.UserVmManager;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachine.State;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.VMInstanceDao;
import com.cloud.vm.snapshot.VMSnapshotVO;
import com.cloud.vm.snapshot.dao.VMSnapshotDao;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.management.*")
public class VolumeApiServiceImplTest {

    @Spy
    @InjectMocks
    private VolumeApiServiceImpl volumeApiServiceImpl = new VolumeApiServiceImpl();
    @Mock
    private SnapshotManager snapshotManagerMock;
    @Mock
    private VolumeDataStoreDao volumeDataStoreDaoMock;
    @Mock
    private VolumeDao volumeDaoMock;
    @Mock
    private AccountManager accountManagerMock;
    @Mock
    private UserVmDao userVmDaoMock;
    @Mock
    private PrimaryDataStoreDao primaryDataStoreDaoMock;
    @Mock
    private VMSnapshotDao _vmSnapshotDao;
    @Mock
    private AsyncJobManager _jobMgr;
    @Mock
    private AsyncJobJoinMapDao _joinMapDao;
    @Mock
    private VolumeDataFactory volumeDataFactoryMock;
    @Mock
    private VMInstanceDao _vmInstanceDao;
    @Mock
    private VolumeInfo volumeInfoMock;
    @Mock
    private SnapshotInfo snapshotInfoMock;
    @Mock
    private VolumeService volumeServiceMock;
    @Mock
    private CreateVolumeCmd createVol;
    @Mock
    private UserVmManager userVmManager;
    @Mock
    private DataCenterDao _dcDao;
    @Mock
    private ResourceLimitService resourceLimitServiceMock;
    @Mock
    private AccountDao _accountDao;
    @Mock
    private HostDao _hostDao;
    @Mock
    private StoragePoolTagsDao storagePoolTagsDao;
    @Mock
    private VMTemplateDao templateDao;
    @Mock
    private ServiceOfferingJoinDao serviceOfferingJoinDao;
    @Mock
    private ServiceOfferingDao serviceOfferingDao;
    @Mock
    private DiskOfferingDao _diskOfferingDao;

    private DetachVolumeCmd detachCmd = new DetachVolumeCmd();
    private Class<?> _detachCmdClass = detachCmd.getClass();

    @Mock
    private StoragePool storagePoolMock;
    private long storagePoolMockId = 1;
    @Mock
    private DiskOfferingVO newDiskOfferingMock;

    @Mock
    private VolumeVO volumeVoMock;
    @Mock
    private Account accountMock;
    @Mock
    private VolumeDataStoreVO volumeDataStoreVoMock;
    @Mock
    private AsyncCallFuture<VolumeApiResult> asyncCallFutureVolumeapiResultMock;
    @Mock
    private ArrayList<SnapshotVO> snapshotVOArrayListMock;

    @Mock
    private SnapshotDao snapshotDaoMock;

    @Mock
    private Project projectMock;

    @Mock
    private ProjectManager projectManagerMock;

    private long accountMockId = 456l;
    private long volumeMockId = 12313l;
    private long vmInstanceMockId = 1123l;
    private long volumeSizeMock = 456789921939l;

    private String projectMockUuid = "projectUuid";
    private long projecMockId = 13801801923810L;

    private long projectMockAccountId = 132329390L;

    private long diskOfferingMockId = 100203L;

    private long offeringMockId = 31902L;

    @Before
    public void setup() throws InterruptedException, ExecutionException {
        Mockito.lenient().doReturn(volumeMockId).when(volumeDataStoreVoMock).getVolumeId();
        Mockito.doReturn(volumeMockId).when(volumeVoMock).getId();
        Mockito.lenient().doReturn(accountMockId).when(accountMock).getId();
        Mockito.doReturn(volumeSizeMock).when(volumeVoMock).getSize();
        Mockito.doReturn(volumeSizeMock).when(newDiskOfferingMock).getDiskSize();
        Mockito.doReturn(projectMockUuid).when(projectMock).getUuid();
        Mockito.doReturn(projecMockId).when(projectMock).getId();
        Mockito.doReturn(projectMockAccountId).when(projectMock).getProjectAccountId();

        Mockito.doReturn(Mockito.mock(VolumeApiResult.class)).when(asyncCallFutureVolumeapiResultMock).get();

        Mockito.when(storagePoolMock.getId()).thenReturn(storagePoolMockId);

        volumeApiServiceImpl._gson = GsonHelper.getGsonLogger();

        // mock caller context
        AccountVO account = new AccountVO("admin", 1L, "networkDomain", Account.Type.NORMAL, "uuid");
        UserVO user = new UserVO(1, "testuser", "password", "firstname", "lastName", "email", "timezone", UUID.randomUUID().toString(), User.Source.UNKNOWN);
        CallContext.register(user, account);
        // mock async context
        AsyncJobExecutionContext context = new AsyncJobExecutionContext();
        AsyncJobExecutionContext.init(_jobMgr, _joinMapDao);
        AsyncJobVO job = new AsyncJobVO();
        context.setJob(job);
        AsyncJobExecutionContext.setCurrentExecutionContext(context);

        TransactionLegacy txn = TransactionLegacy.open("runVolumeDaoImplTest");
        try {
            // volume of running vm id=1
            VolumeVO volumeOfRunningVm = new VolumeVO("root", 1L, 1L, 1L, 1L, 1L, "root", "root", Storage.ProvisioningType.THIN, 1, null, null, "root", Volume.Type.ROOT);
            when(volumeDaoMock.findById(1L)).thenReturn(volumeOfRunningVm);

            UserVmVO runningVm = new UserVmVO(1L, "vm", "vm", 1, HypervisorType.XenServer, 1L, false, false, 1L, 1L, 1, 1L, null, null, null, "vm");
            runningVm.setState(State.Running);
            runningVm.setDataCenterId(1L);
            when(userVmDaoMock.findById(1L)).thenReturn(runningVm);

            // volume of stopped vm id=2
            VolumeVO volumeOfStoppedVm = new VolumeVO("root", 1L, 1L, 1L, 1L, 2L, "root", "root", Storage.ProvisioningType.THIN, 1, null, null, "root", Volume.Type.ROOT);
            volumeOfStoppedVm.setPoolId(1L);
            when(volumeDaoMock.findById(2L)).thenReturn(volumeOfStoppedVm);

            UserVmVO stoppedVm = new UserVmVO(2L, "vm", "vm", 1, HypervisorType.XenServer, 1L, false, false, 1L, 1L, 1, 1L, null, null, null, "vm");
            stoppedVm.setState(State.Stopped);
            stoppedVm.setDataCenterId(1L);
            when(userVmDaoMock.findById(2L)).thenReturn(stoppedVm);

            // volume of hyperV vm id=3
            UserVmVO hyperVVm = new UserVmVO(3L, "vm", "vm", 1, HypervisorType.Hyperv, 1L, false, false, 1L, 1L, 1, 1L, null, null, null, "vm");
            hyperVVm.setState(State.Stopped);
            hyperVVm.setDataCenterId(1L);
            when(userVmDaoMock.findById(3L)).thenReturn(hyperVVm);

            VolumeVO volumeOfStoppeHyperVVm = new VolumeVO("root", 1L, 1L, 1L, 1L, 3L, "root", "root", Storage.ProvisioningType.THIN, 1, null, null, "root", Volume.Type.ROOT);
            volumeOfStoppeHyperVVm.setPoolId(1L);
            when(volumeDaoMock.findById(3L)).thenReturn(volumeOfStoppeHyperVVm);

            StoragePoolVO unmanagedPool = new StoragePoolVO();

            when(primaryDataStoreDaoMock.findById(1L)).thenReturn(unmanagedPool);

            // volume of managed pool id=4
            StoragePoolVO managedPool = new StoragePoolVO();
            managedPool.setManaged(true);
            when(primaryDataStoreDaoMock.findById(2L)).thenReturn(managedPool);
            VolumeVO managedPoolVolume = new VolumeVO("root", 1L, 1L, 1L, 1L, 2L, "root", "root", Storage.ProvisioningType.THIN, 1, null, null, "root", Volume.Type.ROOT);
            managedPoolVolume.setPoolId(2L);
            when(volumeDaoMock.findById(4L)).thenReturn(managedPoolVolume);

            // non-root non-datadisk volume
            VolumeInfo volumeWithIncorrectVolumeType = Mockito.mock(VolumeInfo.class);
            lenient().when(volumeWithIncorrectVolumeType.getId()).thenReturn(5L);
            when(volumeWithIncorrectVolumeType.getVolumeType()).thenReturn(Volume.Type.ISO);
            when(volumeDataFactoryMock.getVolume(5L)).thenReturn(volumeWithIncorrectVolumeType);

            // correct root volume
            VolumeInfo correctRootVolume = Mockito.mock(VolumeInfo.class);
            when(correctRootVolume.getId()).thenReturn(6L);
            when(correctRootVolume.getDataCenterId()).thenReturn(1L);
            when(correctRootVolume.getVolumeType()).thenReturn(Volume.Type.ROOT);
            when(correctRootVolume.getInstanceId()).thenReturn(null);
            when(correctRootVolume.getState()).thenReturn(Volume.State.Ready);
            when(correctRootVolume.getTemplateId()).thenReturn(null);
            when(correctRootVolume.getPoolId()).thenReturn(1L);
            when(volumeDataFactoryMock.getVolume(6L)).thenReturn(correctRootVolume);

            VolumeVO correctRootVolumeVO = new VolumeVO("root", 1L, 1L, 1L, 1L, 2L, "root", "root", Storage.ProvisioningType.THIN, 1, null, null, "root", Volume.Type.ROOT);
            when(volumeDaoMock.findById(6L)).thenReturn(correctRootVolumeVO);
            when(volumeDaoMock.getHypervisorType(6L)).thenReturn(HypervisorType.XenServer);

            // managed root volume
            VolumeInfo managedVolume = Mockito.mock(VolumeInfo.class);
            when(managedVolume.getVolumeType()).thenReturn(Volume.Type.ROOT);
            when(managedVolume.getInstanceId()).thenReturn(null);
            lenient().when(managedVolume.getPoolId()).thenReturn(2L);
            when(volumeDataFactoryMock.getVolume(7L)).thenReturn(managedVolume);

            VolumeVO managedVolume1 = new VolumeVO("root", 1L, 1L, 1L, 1L, 2L, "root", "root", Storage.ProvisioningType.THIN, 1, null, null, "root", Volume.Type.ROOT);
            managedVolume1.setPoolId(2L);
            managedVolume1.setDataCenterId(1L);

            // vm having root volume
            UserVmVO vmHavingRootVolume = new UserVmVO(4L, "vm", "vm", 1, HypervisorType.XenServer, 1L, false, false, 1L, 1L, 1, 1L, null, null, null, "vm");
            vmHavingRootVolume.setState(State.Stopped);
            vmHavingRootVolume.setDataCenterId(1L);
            when(userVmDaoMock.findById(4L)).thenReturn(vmHavingRootVolume);
            List<VolumeVO> vols = new ArrayList<VolumeVO>();
            vols.add(new VolumeVO());
            lenient().when(volumeDaoMock.findByInstanceAndDeviceId(4L, 0L)).thenReturn(vols);

            // volume in uploaded state
            VolumeInfo uploadedVolume = Mockito.mock(VolumeInfo.class);
            when(uploadedVolume.getId()).thenReturn(8L);
            when(uploadedVolume.getDataCenterId()).thenReturn(1L);
            when(uploadedVolume.getVolumeType()).thenReturn(Volume.Type.ROOT);
            when(uploadedVolume.getInstanceId()).thenReturn(null);
            lenient().when(uploadedVolume.getPoolId()).thenReturn(1L);
            when(uploadedVolume.getState()).thenReturn(Volume.State.Uploaded);
            when(volumeDataFactoryMock.getVolume(8L)).thenReturn(uploadedVolume);

            VolumeVO upVolume = new VolumeVO("root", 1L, 1L, 1L, 1L, 2L, "root", "root", Storage.ProvisioningType.THIN, 1, null, null, "root", Volume.Type.ROOT);
            upVolume.setPoolId(1L);
            upVolume.setDataCenterId(1L);
            upVolume.setState(Volume.State.Uploaded);
            when(volumeDaoMock.findById(8L)).thenReturn(upVolume);

            UserVmVO kvmVm = new UserVmVO(4L, "vm", "vm", 1, HypervisorType.KVM, 1L, false, false, 1L, 1L, 1, 1L, null, null, null, "vm");
            kvmVm.setState(State.Running);
            kvmVm.setDataCenterId(1L);
            when(userVmDaoMock.findById(4L)).thenReturn(kvmVm);

            VolumeVO volumeOfKvmVm = new VolumeVO("root", 1L, 1L, 1L, 1L, 4L, "root", "root", Storage.ProvisioningType.THIN, 1, null, null, "root", Volume.Type.ROOT);
            volumeOfKvmVm.setPoolId(1L);
            lenient().when(volumeDaoMock.findById(9L)).thenReturn(volumeOfKvmVm);
            lenient().when(volumeDaoMock.getHypervisorType(9L)).thenReturn(HypervisorType.KVM);

            VolumeVO dataVolumeVO = new VolumeVO("data", 1L, 1L, 1L, 1L, 2L, "data", "data", Storage.ProvisioningType.THIN, 1, null, null, "data", Type.DATADISK);
            lenient().when(volumeDaoMock.findById(10L)).thenReturn(dataVolumeVO);

            VolumeInfo dataVolume = Mockito.mock(VolumeInfo.class);
            when(dataVolume.getId()).thenReturn(10L);
            when(dataVolume.getDataCenterId()).thenReturn(1L);
            when(dataVolume.getVolumeType()).thenReturn(Volume.Type.DATADISK);
            when(dataVolume.getInstanceId()).thenReturn(null);
            when(dataVolume.getState()).thenReturn(Volume.State.Allocated);
            when(volumeDataFactoryMock.getVolume(10L)).thenReturn(dataVolume);

            // helper dao methods mock
            when(_vmSnapshotDao.findByVm(any(Long.class))).thenReturn(new ArrayList<VMSnapshotVO>());
            when(_vmInstanceDao.findById(any(Long.class))).thenReturn(stoppedVm);

            DataCenterVO enabledZone = Mockito.mock(DataCenterVO.class);
            when(enabledZone.getAllocationState()).thenReturn(Grouping.AllocationState.Enabled);

            when(_dcDao.findById(anyLong())).thenReturn(enabledZone);

        } finally {
            txn.close("runVolumeDaoImplTest");
        }

        DiskOfferingVO diskOffering = Mockito.mock(DiskOfferingVO.class);
        when(diskOffering.getEncrypt()).thenReturn(false);
        when(_diskOfferingDao.findById(anyLong())).thenReturn(diskOffering);

        // helper methods mock
        lenient().doNothing().when(accountManagerMock).checkAccess(any(Account.class), any(AccessType.class), any(Boolean.class), any(ControlledEntity.class));
        doNothing().when(_jobMgr).updateAsyncJobAttachment(any(Long.class), any(String.class), any(Long.class));
        when(_jobMgr.submitAsyncJob(any(AsyncJobVO.class), any(String.class), any(Long.class))).thenReturn(1L);
    }

    /**
     * TESTS FOR DETACH ROOT VOLUME, COUNT=4
     */

    @Test(expected = InvalidParameterValueException.class)
    public void testDetachVolumeFromRunningVm() throws NoSuchFieldException, IllegalAccessException {
        Field dedicateIdField = _detachCmdClass.getDeclaredField("id");
        dedicateIdField.setAccessible(true);
        dedicateIdField.set(detachCmd, 1L);
        volumeApiServiceImpl.detachVolumeFromVM(detachCmd);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testDetachVolumeFromStoppedHyperVVm() throws NoSuchFieldException, IllegalAccessException {
        Field dedicateIdField = _detachCmdClass.getDeclaredField("id");
        dedicateIdField.setAccessible(true);
        dedicateIdField.set(detachCmd, 3L);
        volumeApiServiceImpl.detachVolumeFromVM(detachCmd);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testDetachVolumeOfManagedDataStore() throws NoSuchFieldException, IllegalAccessException {
        Field dedicateIdField = _detachCmdClass.getDeclaredField("id");
        dedicateIdField.setAccessible(true);
        dedicateIdField.set(detachCmd, 4L);
        volumeApiServiceImpl.detachVolumeFromVM(detachCmd);
    }

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testDetachVolumeFromStoppedXenVm() throws NoSuchFieldException, IllegalAccessException {
        thrown.expect(NullPointerException.class);
        Field dedicateIdField = _detachCmdClass.getDeclaredField("id");
        dedicateIdField.setAccessible(true);
        dedicateIdField.set(detachCmd, 2L);
        volumeApiServiceImpl.detachVolumeFromVM(detachCmd);
    }

    /**
     * TESTS FOR ATTACH ROOT VOLUME, COUNT=7
     */

    // Negative test - try to attach non-root non-datadisk volume
    @Test(expected = InvalidParameterValueException.class)
    public void attachIncorrectDiskType() throws NoSuchFieldException, IllegalAccessException {
        volumeApiServiceImpl.attachVolumeToVM(1L, 5L, 0L);
    }

    // Negative test - attach root volume to running vm
    @Test(expected = InvalidParameterValueException.class)
    public void attachRootDiskToRunningVm() throws NoSuchFieldException, IllegalAccessException {
        volumeApiServiceImpl.attachVolumeToVM(1L, 6L, 0L);
    }

    // Negative test - attach root volume to non-xen vm
    @Test(expected = InvalidParameterValueException.class)
    public void attachRootDiskToHyperVm() throws NoSuchFieldException, IllegalAccessException {
        volumeApiServiceImpl.attachVolumeToVM(3L, 6L, 0L);
    }

    // Negative test - attach root volume from the managed data store
    @Test(expected = InvalidParameterValueException.class)
    public void attachRootDiskOfManagedDataStore() throws NoSuchFieldException, IllegalAccessException {
        volumeApiServiceImpl.attachVolumeToVM(2L, 7L, 0L);
    }

    // Negative test - root volume can't be attached to the vm already having a root volume attached
    @Test(expected = InvalidParameterValueException.class)
    public void attachRootDiskToVmHavingRootDisk() throws NoSuchFieldException, IllegalAccessException {
        volumeApiServiceImpl.attachVolumeToVM(4L, 6L, 0L);
    }

    // Negative test - root volume in uploaded state can't be attached
    @Test(expected = InvalidParameterValueException.class)
    public void attachRootInUploadedState() throws NoSuchFieldException, IllegalAccessException {
        volumeApiServiceImpl.attachVolumeToVM(2L, 8L, 0L);
    }

    // Positive test - attach ROOT volume in correct state, to the vm not having root volume attached
    @Test
    public void attachRootVolumePositive() throws NoSuchFieldException, IllegalAccessException {
        thrown.expect(NullPointerException.class);
        volumeApiServiceImpl.attachVolumeToVM(2L, 6L, 0L);
    }

    // Negative test - attach data volume, to the vm on non-kvm hypervisor
    @Test(expected = InvalidParameterValueException.class)
    public void attachDiskWithEncryptEnabledOfferingonNonKVM() throws NoSuchFieldException, IllegalAccessException {
        DiskOfferingVO diskOffering = Mockito.mock(DiskOfferingVO.class);
        when(diskOffering.getEncrypt()).thenReturn(true);
        when(_diskOfferingDao.findById(anyLong())).thenReturn(diskOffering);
        volumeApiServiceImpl.attachVolumeToVM(2L, 10L, 1L);
    }

    // Positive test - attach data volume, to the vm on kvm hypervisor
    @Test
    public void attachDiskWithEncryptEnabledOfferingOnKVM() throws NoSuchFieldException, IllegalAccessException {
        thrown.expect(NullPointerException.class);
        DiskOfferingVO diskOffering = Mockito.mock(DiskOfferingVO.class);
        when(diskOffering.getEncrypt()).thenReturn(true);
        when(_diskOfferingDao.findById(anyLong())).thenReturn(diskOffering);
        volumeApiServiceImpl.attachVolumeToVM(4L, 10L, 1L);
    }

    // volume not Ready
    @Test(expected = InvalidParameterValueException.class)
    public void testTakeSnapshotF1() throws ResourceAllocationException {
        when(volumeDataFactoryMock.getVolume(anyLong())).thenReturn(volumeInfoMock);
        when(volumeInfoMock.getState()).thenReturn(Volume.State.Allocated);
        lenient().when(volumeInfoMock.getPoolId()).thenReturn(1L);
        volumeApiServiceImpl.takeSnapshot(5L, Snapshot.MANUAL_POLICY_ID, 3L, null, false, null, false, null);
    }

    @Test
    public void testTakeSnapshotF2() throws ResourceAllocationException {
        when(volumeDataFactoryMock.getVolume(anyLong())).thenReturn(volumeInfoMock);
        when(volumeInfoMock.getState()).thenReturn(Volume.State.Ready);
        when(volumeInfoMock.getInstanceId()).thenReturn(null);
        when(volumeInfoMock.getPoolId()).thenReturn(1L);
        when(volumeServiceMock.takeSnapshot(Mockito.any(VolumeInfo.class))).thenReturn(snapshotInfoMock);
        final TaggedResourceService taggedResourceService = Mockito.mock(TaggedResourceService.class);
        Mockito.lenient().when(taggedResourceService.createTags(anyObject(), anyObject(), anyObject(), anyObject())).thenReturn(null);
        ReflectionTestUtils.setField(volumeApiServiceImpl, "taggedResourceService", taggedResourceService);
        volumeApiServiceImpl.takeSnapshot(5L, Snapshot.MANUAL_POLICY_ID, 3L, null, false, null, false, null);
    }

    @Test
    public void testNullGetVolumeNameFromCmd() {
        when(createVol.getVolumeName()).thenReturn(null);
        Assert.assertNotNull(volumeApiServiceImpl.getVolumeNameFromCommand(createVol));
    }

    @Test
    public void testEmptyGetVolumeNameFromCmd() {
        when(createVol.getVolumeName()).thenReturn("");
        Assert.assertNotNull(volumeApiServiceImpl.getVolumeNameFromCommand(createVol));
    }

    @Test
    public void testBlankGetVolumeNameFromCmd() {
        when(createVol.getVolumeName()).thenReturn("   ");
        Assert.assertNotNull(volumeApiServiceImpl.getVolumeNameFromCommand(createVol));
    }

    @Test
    public void testNonEmptyGetVolumeNameFromCmd() {
        when(createVol.getVolumeName()).thenReturn("abc");
        Assert.assertSame(volumeApiServiceImpl.getVolumeNameFromCommand(createVol), "abc");
    }

    @Test
    public void testUpdateMissingRootDiskControllerWithNullChainInfo() {
        volumeApiServiceImpl.updateMissingRootDiskController(null, null);
        verify(userVmManager, times(0)).persistDeviceBusInfo(any(UserVmVO.class), anyString());
    }

    @Test
    public void testUpdateMissingRootDiskControllerWithValidChainInfo() {
        UserVmVO vm = userVmDaoMock.findById(1L);

        Mockito.doNothing().when(userVmManager).persistDeviceBusInfo(any(UserVmVO.class), eq("scsi"));
        volumeApiServiceImpl.updateMissingRootDiskController(vm, "{\"diskDeviceBusName\":\"scsi0:0\",\"diskChain\":[\"[somedatastore] i-3-VM-somePath/ROOT-1.vmdk\"]}");
        verify(userVmManager, times(1)).persistDeviceBusInfo(any(UserVmVO.class), eq("scsi"));
    }

    /**
     * Setting locationType for a non-managed storage should give an error
     */
    @Test
    public void testAllocSnapshotNonManagedStorageArchive() {
        try {
            volumeApiServiceImpl.allocSnapshot(6L, 1L, "test", Snapshot.LocationType.SECONDARY);
        } catch (InvalidParameterValueException e) {
            Assert.assertEquals(e.getMessage(), "VolumeId: 6 LocationType is supported only for managed storage");
            return;
        } catch (ResourceAllocationException e) {
            Assert.fail("Unexpected excepiton " + e.getMessage());
        }

        Assert.fail("Expected Exception for archive in non-managed storage");
    }

    /**
     * The resource limit check for primary storage should not be skipped for Volume in 'Uploaded' state.
     */
    @Test
    public void testResourceLimitCheckForUploadedVolume() throws NoSuchFieldException, IllegalAccessException, ResourceAllocationException {
        doThrow(new ResourceAllocationException("primary storage resource limit check failed", Resource.ResourceType.primary_storage)).when(resourceLimitServiceMock)
        .checkResourceLimit(any(AccountVO.class), any(Resource.ResourceType.class), any(Long.class));
        UserVmVO vm = Mockito.mock(UserVmVO.class);
        AccountVO acc = Mockito.mock(AccountVO.class);
        VolumeInfo volumeToAttach = Mockito.mock(VolumeInfo.class);
        lenient().when(volumeToAttach.getId()).thenReturn(9L);
        when(volumeToAttach.getDataCenterId()).thenReturn(34L);
        when(volumeToAttach.getVolumeType()).thenReturn(Volume.Type.DATADISK);
        when(volumeToAttach.getInstanceId()).thenReturn(null);
        when(volumeToAttach.getAccountId()).thenReturn(3L);
        when(_accountDao.findById(anyLong())).thenReturn(acc);
        when(userVmDaoMock.findById(anyLong())).thenReturn(vm);
        when(vm.getType()).thenReturn(VirtualMachine.Type.User);
        when(vm.getState()).thenReturn(State.Running);
        when(vm.getDataCenterId()).thenReturn(34L);
        when(vm.getBackupOfferingId()).thenReturn(null);
        when(vm.getBackupVolumeList()).thenReturn(Collections.emptyList());
        when(volumeDaoMock.findByInstanceAndType(anyLong(), any(Volume.Type.class))).thenReturn(new ArrayList<>(10));
        when(volumeDataFactoryMock.getVolume(9L)).thenReturn(volumeToAttach);
        when(volumeToAttach.getState()).thenReturn(Volume.State.Uploaded);
        DataCenterVO zoneWithDisabledLocalStorage = Mockito.mock(DataCenterVO.class);
        when(_dcDao.findById(anyLong())).thenReturn(zoneWithDisabledLocalStorage);
        when(zoneWithDisabledLocalStorage.isLocalStorageEnabled()).thenReturn(true);
        try {
            volumeApiServiceImpl.attachVolumeToVM(2L, 9L, null);
        } catch (InvalidParameterValueException e) {
            Assert.assertEquals(e.getMessage(), ("primary storage resource limit check failed"));
        }
    }

    @After
    public void tearDown() {
        CallContext.unregister();
    }

    @Test
    public void getStoragePoolTagsTestStorageWithoutTags() {
        Mockito.when(storagePoolTagsDao.getStoragePoolTags(storagePoolMockId)).thenReturn(new ArrayList<>());

        String returnedStoragePoolTags = volumeApiServiceImpl.getStoragePoolTags(storagePoolMock);

        Assert.assertNull(returnedStoragePoolTags);
    }

    @Test
    public void getStoragePoolTagsTestStorageWithTags() {
        ArrayList<String> tags = new ArrayList<>();
        String tag1 = "tag1";
        String tag2 = "tag2";
        String tag3 = "tag3";

        tags.add(tag1);
        tags.add(tag2);
        tags.add(tag3);

        Mockito.when(storagePoolTagsDao.getStoragePoolTags(storagePoolMockId)).thenReturn(tags);

        String returnedStoragePoolTags = volumeApiServiceImpl.getStoragePoolTags(storagePoolMock);

        Assert.assertEquals("tag1,tag2,tag3", returnedStoragePoolTags);
    }

    @Test
    public void validateConditionsToReplaceDiskOfferingOfVolumeTestNoNewDiskOffering() {
        volumeApiServiceImpl.validateConditionsToReplaceDiskOfferingOfVolume(volumeVoMock, null, storagePoolMock);

        Mockito.verify(volumeVoMock, times(0)).getVolumeType();
    }

    @Test
    public void validateConditionsToReplaceDiskOfferingOfVolumeTestRootVolume() {
        Mockito.lenient().when(volumeVoMock.getVolumeType()).thenReturn(Type.ROOT);
        Mockito.doReturn(vmInstanceMockId).when(volumeVoMock).getInstanceId();
        UserVmVO vm = Mockito.mock(UserVmVO.class);
        when(_vmInstanceDao.findById(anyLong())).thenReturn(vm);
        when(vm.getServiceOfferingId()).thenReturn(1L);
        ServiceOfferingVO serviceOfferingVO = Mockito.mock(ServiceOfferingVO.class);
        serviceOfferingVO.setDiskOfferingStrictness(false);
        when(serviceOfferingDao.findById(anyLong())).thenReturn(serviceOfferingVO);

        volumeApiServiceImpl.validateConditionsToReplaceDiskOfferingOfVolume(volumeVoMock, newDiskOfferingMock, storagePoolMock);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void validateConditionsToReplaceDiskOfferingOfVolumeTestRootVolumeWithDiskOfferingStrictnessTrue() {
        Mockito.lenient().when(volumeVoMock.getVolumeType()).thenReturn(Type.ROOT);
        Mockito.doReturn(vmInstanceMockId).when(volumeVoMock).getInstanceId();
        UserVmVO vm = Mockito.mock(UserVmVO.class);
        when(_vmInstanceDao.findById(anyLong())).thenReturn(vm);
        when(vm.getServiceOfferingId()).thenReturn(1L);
        ServiceOfferingVO serviceOfferingVO = Mockito.mock(ServiceOfferingVO.class);
        when(serviceOfferingDao.findById(anyLong())).thenReturn(serviceOfferingVO);
        when(serviceOfferingVO.getDiskOfferingStrictness()).thenReturn(true);

        volumeApiServiceImpl.validateConditionsToReplaceDiskOfferingOfVolume(volumeVoMock, newDiskOfferingMock, storagePoolMock);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void validateConditionsToReplaceDiskOfferingOfVolumeTestTargetPoolSharedDiskOfferingLocal() {
        Mockito.lenient().when(volumeVoMock.getVolumeType()).thenReturn(Type.DATADISK);
        Mockito.when(newDiskOfferingMock.isUseLocalStorage()).thenReturn(true);
        Mockito.when(storagePoolMock.isShared()).thenReturn(true);

        volumeApiServiceImpl.validateConditionsToReplaceDiskOfferingOfVolume(volumeVoMock, newDiskOfferingMock, storagePoolMock);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void validateConditionsToReplaceDiskOfferingOfVolumeTestTargetPoolLocalDiskOfferingShared() {
        Mockito.lenient().when(volumeVoMock.getVolumeType()).thenReturn(Type.DATADISK);
        Mockito.when(newDiskOfferingMock.isShared()).thenReturn(true);
        Mockito.when(storagePoolMock.isLocal()).thenReturn(true);

        volumeApiServiceImpl.validateConditionsToReplaceDiskOfferingOfVolume(volumeVoMock, newDiskOfferingMock, storagePoolMock);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void validateConditionsToReplaceDiskOfferingOfVolumeTestTagsDoNotMatch() {
        Mockito.lenient().when(volumeVoMock.getVolumeType()).thenReturn(Type.DATADISK);

        Mockito.when(newDiskOfferingMock.isUseLocalStorage()).thenReturn(false);
        Mockito.when(storagePoolMock.isShared()).thenReturn(true);

        Mockito.lenient().when(newDiskOfferingMock.isShared()).thenReturn(true);
        Mockito.when(storagePoolMock.isLocal()).thenReturn(false);

        Mockito.when(newDiskOfferingMock.getTags()).thenReturn("tag1");

        Mockito.doReturn(null).when(volumeApiServiceImpl).getStoragePoolTags(storagePoolMock);

        volumeApiServiceImpl.validateConditionsToReplaceDiskOfferingOfVolume(volumeVoMock, newDiskOfferingMock, storagePoolMock);
    }

    @Test
    public void validateConditionsToReplaceDiskOfferingOfVolumeTestEverythingWorking() {
        Mockito.lenient().when(volumeVoMock.getVolumeType()).thenReturn(Type.DATADISK);

        Mockito.when(newDiskOfferingMock.isUseLocalStorage()).thenReturn(false);
        Mockito.when(storagePoolMock.isShared()).thenReturn(true);

        Mockito.lenient().when(newDiskOfferingMock.isShared()).thenReturn(true);
        Mockito.when(storagePoolMock.isLocal()).thenReturn(false);

        Mockito.when(newDiskOfferingMock.getTags()).thenReturn("tag1");

        Mockito.doReturn("tag1").when(volumeApiServiceImpl).getStoragePoolTags(storagePoolMock);

        volumeApiServiceImpl.validateConditionsToReplaceDiskOfferingOfVolume(volumeVoMock, newDiskOfferingMock, storagePoolMock);

        InOrder inOrder = Mockito.inOrder(volumeVoMock, newDiskOfferingMock, storagePoolMock, volumeApiServiceImpl);
        inOrder.verify(storagePoolMock).isShared();
        inOrder.verify(newDiskOfferingMock).isUseLocalStorage();
        inOrder.verify(storagePoolMock).isLocal();
        inOrder.verify(newDiskOfferingMock, times(0)).isShared();
        inOrder.verify(volumeApiServiceImpl).getStoragePoolTags(storagePoolMock);

        inOrder.verify(volumeVoMock).getSize();
        inOrder.verify(newDiskOfferingMock).getDiskSize();
    }

    @Test(expected = InvalidParameterValueException.class)
    public void retrieveAndValidateVolumeTestVolumeNotFound() {
        Mockito.doReturn(null).when(volumeDaoMock).findById(volumeMockId);
        volumeApiServiceImpl.retrieveAndValidateVolume(volumeMockId, accountMock);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void retrieveAndValidateVolumeTestCannotOperateOnVolumeDueToSnapshot() {
        Mockito.doReturn(volumeVoMock).when(volumeDaoMock).findById(volumeMockId);
        Mockito.doReturn(false).when(snapshotManagerMock).canOperateOnVolume(volumeVoMock);

        volumeApiServiceImpl.retrieveAndValidateVolume(volumeMockId, accountMock);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void retrieveAndValidateVolumeTestVolumePluggedIntoVm() {
        Mockito.doReturn(volumeVoMock).when(volumeDaoMock).findById(volumeMockId);
        Mockito.doReturn(vmInstanceMockId).when(volumeVoMock).getInstanceId();

        Mockito.doReturn(true).when(snapshotManagerMock).canOperateOnVolume(volumeVoMock);

        volumeApiServiceImpl.retrieveAndValidateVolume(volumeMockId, accountMock);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void retrieveAndValidateVolumeTestStateUploadOpAndDownloadInProgress() {
        Mockito.doReturn(volumeVoMock).when(volumeDaoMock).findById(volumeMockId);
        Mockito.doReturn(null).when(volumeVoMock).getInstanceId();
        Mockito.doReturn(Volume.State.UploadOp).when(volumeVoMock).getState();

        Mockito.doReturn(true).when(snapshotManagerMock).canOperateOnVolume(volumeVoMock);
        Mockito.doReturn(volumeDataStoreVoMock).when(volumeDataStoreDaoMock).findByVolume(volumeMockId);
        Mockito.doReturn(VMTemplateStorageResourceAssoc.Status.DOWNLOAD_IN_PROGRESS).when(volumeDataStoreVoMock).getDownloadState();

        volumeApiServiceImpl.retrieveAndValidateVolume(volumeMockId, accountMock);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void retrieveAndValidateVolumeTestStateNotUploaded() {
        Mockito.doReturn(volumeVoMock).when(volumeDaoMock).findById(volumeMockId);
        Mockito.doReturn(null).when(volumeVoMock).getInstanceId();
        Mockito.doReturn(Volume.State.NotUploaded).when(volumeVoMock).getState();

        Mockito.doReturn(true).when(snapshotManagerMock).canOperateOnVolume(volumeVoMock);

        volumeApiServiceImpl.retrieveAndValidateVolume(volumeMockId, accountMock);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void retrieveAndValidateVolumeTestUploadInProgress() {
        Mockito.doReturn(volumeVoMock).when(volumeDaoMock).findById(volumeMockId);
        Mockito.doReturn(null).when(volumeVoMock).getInstanceId();
        Mockito.doReturn(Volume.State.UploadInProgress).when(volumeVoMock).getState();

        Mockito.doReturn(true).when(snapshotManagerMock).canOperateOnVolume(volumeVoMock);

        volumeApiServiceImpl.retrieveAndValidateVolume(volumeMockId, accountMock);
    }

    @Test
    public void retrieveAndValidateVolumeTest() {
        Mockito.doReturn(volumeVoMock).when(volumeDaoMock).findById(volumeMockId);
        Mockito.doReturn(null).when(volumeVoMock).getInstanceId();
        Mockito.doReturn(Volume.State.Ready).when(volumeVoMock).getState();

        Mockito.doReturn(true).when(snapshotManagerMock).canOperateOnVolume(volumeVoMock);
        Mockito.doNothing().when(accountManagerMock).checkAccess(accountMock, null, true, volumeVoMock);
        volumeApiServiceImpl.retrieveAndValidateVolume(volumeMockId, accountMock);

        Mockito.verify(accountManagerMock).checkAccess(accountMock, null, true, volumeVoMock);
    }

    @Test
    public void destroyVolumeIfPossibleTestVolumeStateIsDestroy() {
        Mockito.doReturn(Volume.State.Destroy).when(volumeVoMock).getState();
        configureMocksForTestDestroyVolumeWhenVolume();

        volumeApiServiceImpl.destroyVolumeIfPossible(volumeVoMock);

        verifyMocksForTestDestroyVolumeWhenVolumeIsNotInRightState();
    }

    @Test
    public void destroyVolumeIfPossibleTestVolumeStateIsExpunging() {
        Mockito.doReturn(Volume.State.Expunging).when(volumeVoMock).getState();
        configureMocksForTestDestroyVolumeWhenVolume();

        volumeApiServiceImpl.destroyVolumeIfPossible(volumeVoMock);

        verifyMocksForTestDestroyVolumeWhenVolumeIsNotInRightState();
    }

    @Test
    public void destroyVolumeIfPossibleTestVolumeStateIsExpunged() {
        Mockito.doReturn(Volume.State.Expunged).when(volumeVoMock).getState();
        configureMocksForTestDestroyVolumeWhenVolume();

        volumeApiServiceImpl.destroyVolumeIfPossible(volumeVoMock);

        verifyMocksForTestDestroyVolumeWhenVolumeIsNotInRightState();
    }

    @Test
    public void destroyVolumeIfPossibleTestVolumeStateReady() {
        Mockito.doReturn(Volume.State.Ready).when(volumeVoMock).getState();
        configureMocksForTestDestroyVolumeWhenVolume();

        volumeApiServiceImpl.destroyVolumeIfPossible(volumeVoMock);

        Mockito.verify(volumeServiceMock, Mockito.times(1)).destroyVolume(volumeMockId);
    }

    private void verifyMocksForTestDestroyVolumeWhenVolumeIsNotInRightState() {
        Mockito.verify(volumeServiceMock, Mockito.times(0)).destroyVolume(volumeMockId);
        Mockito.verify(resourceLimitServiceMock, Mockito.times(0)).decrementResourceCount(accountMockId, ResourceType.volume, true);
        Mockito.verify(resourceLimitServiceMock, Mockito.times(0)).decrementResourceCount(accountMockId, ResourceType.primary_storage, true, volumeSizeMock);
    }

    private void configureMocksForTestDestroyVolumeWhenVolume() {
        Mockito.lenient().doReturn(accountMockId).when(volumeVoMock).getAccountId();
        Mockito.lenient().doReturn(true).when(volumeVoMock).isDisplayVolume();

        Mockito.lenient().doNothing().when(volumeServiceMock).destroyVolume(volumeMockId);
        Mockito.lenient().doNothing().when(resourceLimitServiceMock).decrementResourceCount(accountMockId, ResourceType.volume, true);
        Mockito.lenient().doNothing().when(resourceLimitServiceMock).decrementResourceCount(accountMockId, ResourceType.primary_storage, true, volumeSizeMock);
    }

    @Test
    public void expungeVolumesInPrimaryStorageIfNeededTestVolumeNotInPrimaryDataStore() throws InterruptedException, ExecutionException, NoTransitionException {
        Mockito.lenient().doReturn(asyncCallFutureVolumeapiResultMock).when(volumeServiceMock).expungeVolumeAsync(volumeInfoMock);
        Mockito.doReturn(null).when(volumeDataFactoryMock).getVolume(volumeMockId, DataStoreRole.Primary);

        volumeApiServiceImpl.expungeVolumesInPrimaryStorageIfNeeded(volumeVoMock);

        Mockito.verify(volumeServiceMock, Mockito.times(0)).expungeVolumeAsync(volumeInfoMock);
        Mockito.verify(asyncCallFutureVolumeapiResultMock, Mockito.times(0)).get();
    }

    @Test
    public void expungeVolumesInPrimaryStorageIfNeededTestVolumeInPrimaryDataStore() throws InterruptedException, ExecutionException, NoTransitionException {
        Mockito.doReturn(asyncCallFutureVolumeapiResultMock).when(volumeServiceMock).expungeVolumeAsync(volumeInfoMock);
        Mockito.doReturn(volumeInfoMock).when(volumeDataFactoryMock).getVolume(volumeMockId, DataStoreRole.Primary);

        volumeApiServiceImpl.expungeVolumesInPrimaryStorageIfNeeded(volumeVoMock);

        Mockito.verify(volumeServiceMock, Mockito.times(1)).expungeVolumeAsync(volumeInfoMock);
        Mockito.verify(asyncCallFutureVolumeapiResultMock, Mockito.times(1)).get();
    }

    @Test(expected = InterruptedException.class)
    public void expungeVolumesInPrimaryStorageIfNeededTestThrowingInterruptedException() throws InterruptedException, ExecutionException, NoTransitionException {
        Mockito.doReturn(asyncCallFutureVolumeapiResultMock).when(volumeServiceMock).expungeVolumeAsync(volumeInfoMock);
        Mockito.doReturn(volumeInfoMock).when(volumeDataFactoryMock).getVolume(volumeMockId, DataStoreRole.Primary);
        Mockito.doThrow(InterruptedException.class).when(asyncCallFutureVolumeapiResultMock).get();

        volumeApiServiceImpl.expungeVolumesInPrimaryStorageIfNeeded(volumeVoMock);
    }

    @Test(expected = ExecutionException.class)
    public void expungeVolumesInPrimaryStorageIfNeededTestThrowingExecutionException() throws InterruptedException, ExecutionException, NoTransitionException {
        Mockito.doReturn(asyncCallFutureVolumeapiResultMock).when(volumeServiceMock).expungeVolumeAsync(volumeInfoMock);
        Mockito.doReturn(volumeInfoMock).when(volumeDataFactoryMock).getVolume(volumeMockId, DataStoreRole.Primary);
        Mockito.doThrow(ExecutionException.class).when(asyncCallFutureVolumeapiResultMock).get();

        volumeApiServiceImpl.expungeVolumesInPrimaryStorageIfNeeded(volumeVoMock);
    }

    @Test
    public void expungeVolumesInSecondaryStorageIfNeededTestVolumeNotFoundInSecondaryStorage() throws InterruptedException, ExecutionException {
        Mockito.lenient().doReturn(asyncCallFutureVolumeapiResultMock).when(volumeServiceMock).expungeVolumeAsync(volumeInfoMock);
        Mockito.doReturn(null).when(volumeDataFactoryMock).getVolume(volumeMockId, DataStoreRole.Image);
        Mockito.lenient().doNothing().when(resourceLimitServiceMock).decrementResourceCount(accountMockId, ResourceType.secondary_storage, volumeSizeMock);
        Mockito.lenient().doReturn(accountMockId).when(volumeInfoMock).getAccountId();
        Mockito.lenient().doReturn(volumeSizeMock).when(volumeInfoMock).getSize();

        volumeApiServiceImpl.expungeVolumesInSecondaryStorageIfNeeded(volumeVoMock);

        Mockito.verify(volumeServiceMock, Mockito.times(0)).expungeVolumeAsync(volumeInfoMock);
        Mockito.verify(asyncCallFutureVolumeapiResultMock, Mockito.times(0)).get();
        Mockito.verify(resourceLimitServiceMock, Mockito.times(0)).decrementResourceCount(accountMockId, ResourceType.secondary_storage, volumeSizeMock);
    }

    @Test
    public void expungeVolumesInSecondaryStorageIfNeededTestVolumeFoundInSecondaryStorage() throws InterruptedException, ExecutionException {
        Mockito.doReturn(asyncCallFutureVolumeapiResultMock).when(volumeServiceMock).expungeVolumeAsync(volumeInfoMock);
        Mockito.doReturn(volumeInfoMock).when(volumeDataFactoryMock).getVolume(volumeMockId, DataStoreRole.Image);
        Mockito.doNothing().when(resourceLimitServiceMock).decrementResourceCount(accountMockId, ResourceType.secondary_storage, volumeSizeMock);
        Mockito.doReturn(accountMockId).when(volumeInfoMock).getAccountId();
        Mockito.doReturn(volumeSizeMock).when(volumeInfoMock).getSize();

        volumeApiServiceImpl.expungeVolumesInSecondaryStorageIfNeeded(volumeVoMock);

        Mockito.verify(volumeServiceMock, Mockito.times(1)).expungeVolumeAsync(volumeInfoMock);
        Mockito.verify(asyncCallFutureVolumeapiResultMock, Mockito.times(1)).get();
        Mockito.verify(resourceLimitServiceMock, Mockito.times(1)).decrementResourceCount(accountMockId, ResourceType.secondary_storage, volumeSizeMock);
    }

    @Test(expected = InterruptedException.class)
    public void expungeVolumesInSecondaryStorageIfNeededTestThrowinInterruptedException() throws InterruptedException, ExecutionException {
        Mockito.doReturn(asyncCallFutureVolumeapiResultMock).when(volumeServiceMock).expungeVolumeAsync(volumeInfoMock);
        Mockito.doReturn(volumeInfoMock).when(volumeDataFactoryMock).getVolume(volumeMockId, DataStoreRole.Image);
        Mockito.lenient().doNothing().when(resourceLimitServiceMock).decrementResourceCount(accountMockId, ResourceType.secondary_storage, volumeSizeMock);
        Mockito.lenient().doReturn(accountMockId).when(volumeInfoMock).getAccountId();
        Mockito.lenient().doReturn(volumeSizeMock).when(volumeInfoMock).getSize();

        Mockito.doThrow(InterruptedException.class).when(asyncCallFutureVolumeapiResultMock).get();

        volumeApiServiceImpl.expungeVolumesInSecondaryStorageIfNeeded(volumeVoMock);

    }

    @Test(expected = ExecutionException.class)
    public void expungeVolumesInSecondaryStorageIfNeededTestThrowingExecutionException() throws InterruptedException, ExecutionException {
        Mockito.doReturn(asyncCallFutureVolumeapiResultMock).when(volumeServiceMock).expungeVolumeAsync(volumeInfoMock);
        Mockito.doReturn(volumeInfoMock).when(volumeDataFactoryMock).getVolume(volumeMockId, DataStoreRole.Image);
        Mockito.lenient().doNothing().when(resourceLimitServiceMock).decrementResourceCount(accountMockId, ResourceType.secondary_storage, volumeSizeMock);
        Mockito.lenient().doReturn(accountMockId).when(volumeInfoMock).getAccountId();
        Mockito.lenient().doReturn(volumeSizeMock).when(volumeInfoMock).getSize();

        Mockito.doThrow(ExecutionException.class).when(asyncCallFutureVolumeapiResultMock).get();

        volumeApiServiceImpl.expungeVolumesInSecondaryStorageIfNeeded(volumeVoMock);

    }

    @Test
    public void cleanVolumesCacheTest() {
        List<VolumeInfo> volumeInfos = new ArrayList<>();
        VolumeInfo volumeInfoMock1 = Mockito.mock(VolumeInfo.class);
        VolumeInfo volumeInfoMock2 = Mockito.mock(VolumeInfo.class);

        DataStore dataStoreMock1 = Mockito.mock(DataStore.class);
        DataStore dataStoreMock2 = Mockito.mock(DataStore.class);
        Mockito.doReturn(dataStoreMock1).when(volumeInfoMock1).getDataStore();
        Mockito.doReturn(dataStoreMock2).when(volumeInfoMock2).getDataStore();

        volumeInfos.add(volumeInfoMock1);
        volumeInfos.add(volumeInfoMock2);

        Mockito.doReturn(volumeInfos).when(volumeDataFactoryMock).listVolumeOnCache(volumeMockId);

        volumeApiServiceImpl.cleanVolumesCache(volumeVoMock);

        Mockito.verify(dataStoreMock1).getName();
        Mockito.verify(dataStoreMock2).getName();

        Mockito.verify(volumeInfoMock1).delete();
        Mockito.verify(volumeInfoMock2).delete();
    }

    @Test
    public void deleteVolumeTestVolumeStateAllocated() throws InterruptedException, ExecutionException, NoTransitionException {
        Mockito.doReturn(Volume.State.Allocated).when(volumeVoMock).getState();

        Mockito.doReturn(volumeVoMock).when(volumeApiServiceImpl).retrieveAndValidateVolume(volumeMockId, accountMock);
        Mockito.doNothing().when(volumeApiServiceImpl).destroyVolumeIfPossible(volumeVoMock);
        Mockito.lenient().doNothing().when(volumeApiServiceImpl).expungeVolumesInPrimaryStorageIfNeeded(volumeVoMock);
        Mockito.lenient().doNothing().when(volumeApiServiceImpl).expungeVolumesInSecondaryStorageIfNeeded(volumeVoMock);
        Mockito.lenient().doNothing().when(volumeApiServiceImpl).cleanVolumesCache(volumeVoMock);

        Mockito.doReturn(true).when(volumeDaoMock).remove(volumeMockId);
        Mockito.doReturn(true).when(volumeApiServiceImpl).stateTransitTo(volumeVoMock, Volume.Event.DestroyRequested);

        boolean result = volumeApiServiceImpl.deleteVolume(volumeMockId, accountMock);

        Assert.assertTrue(result);
        Mockito.verify(volumeApiServiceImpl).retrieveAndValidateVolume(volumeMockId, accountMock);
        Mockito.verify(volumeApiServiceImpl).destroyVolumeIfPossible(volumeVoMock);
        Mockito.verify(volumeDaoMock).remove(volumeMockId);
        Mockito.verify(volumeApiServiceImpl).stateTransitTo(volumeVoMock, Volume.Event.DestroyRequested);

        Mockito.verify(volumeApiServiceImpl, Mockito.times(0)).expungeVolumesInPrimaryStorageIfNeeded(volumeVoMock);
        Mockito.verify(volumeApiServiceImpl, Mockito.times(0)).expungeVolumesInSecondaryStorageIfNeeded(volumeVoMock);
        Mockito.verify(volumeApiServiceImpl, Mockito.times(0)).cleanVolumesCache(volumeVoMock);
    }

    @Test
    public void deleteVolumeTestVolumeStateReady() throws InterruptedException, ExecutionException, NoTransitionException {
        Mockito.doReturn(Volume.State.Ready).when(volumeVoMock).getState();

        Mockito.doReturn(volumeVoMock).when(volumeApiServiceImpl).retrieveAndValidateVolume(volumeMockId, accountMock);
        Mockito.doNothing().when(volumeApiServiceImpl).destroyVolumeIfPossible(volumeVoMock);
        Mockito.doNothing().when(volumeApiServiceImpl).expungeVolumesInPrimaryStorageIfNeeded(volumeVoMock);
        Mockito.doNothing().when(volumeApiServiceImpl).expungeVolumesInSecondaryStorageIfNeeded(volumeVoMock);
        Mockito.doNothing().when(volumeApiServiceImpl).cleanVolumesCache(volumeVoMock);

        Mockito.lenient().doReturn(true).when(volumeDaoMock).remove(volumeMockId);
        Mockito.lenient().doReturn(true).when(volumeApiServiceImpl).stateTransitTo(volumeVoMock, Volume.Event.DestroyRequested);

        boolean result = volumeApiServiceImpl.deleteVolume(volumeMockId, accountMock);

        Assert.assertTrue(result);
        Mockito.verify(volumeApiServiceImpl).retrieveAndValidateVolume(volumeMockId, accountMock);
        Mockito.verify(volumeApiServiceImpl).destroyVolumeIfPossible(volumeVoMock);
        Mockito.verify(volumeDaoMock, Mockito.times(0)).remove(volumeMockId);
        Mockito.verify(volumeApiServiceImpl, Mockito.times(0)).stateTransitTo(volumeVoMock, Volume.Event.DestroyRequested);

        Mockito.verify(volumeApiServiceImpl, Mockito.times(1)).expungeVolumesInPrimaryStorageIfNeeded(volumeVoMock);
        Mockito.verify(volumeApiServiceImpl, Mockito.times(1)).expungeVolumesInSecondaryStorageIfNeeded(volumeVoMock);
        Mockito.verify(volumeApiServiceImpl, Mockito.times(1)).cleanVolumesCache(volumeVoMock);
    }

    @Test
    public void deleteVolumeTestVolumeStateReadyThrowingInterruptedException() throws InterruptedException, ExecutionException, NoTransitionException {
        Mockito.doReturn(Volume.State.Ready).when(volumeVoMock).getState();

        Mockito.doReturn(volumeVoMock).when(volumeApiServiceImpl).retrieveAndValidateVolume(volumeMockId, accountMock);
        Mockito.doNothing().when(volumeApiServiceImpl).destroyVolumeIfPossible(volumeVoMock);
        Mockito.doThrow(InterruptedException.class).when(volumeApiServiceImpl).expungeVolumesInPrimaryStorageIfNeeded(volumeVoMock);

        Mockito.lenient().doReturn(true).when(volumeDaoMock).remove(volumeMockId);
        Mockito.lenient().doReturn(true).when(volumeApiServiceImpl).stateTransitTo(volumeVoMock, Volume.Event.DestroyRequested);

        boolean result = volumeApiServiceImpl.deleteVolume(volumeMockId, accountMock);

        Assert.assertFalse(result);
        Mockito.verify(volumeApiServiceImpl).retrieveAndValidateVolume(volumeMockId, accountMock);
        Mockito.verify(volumeApiServiceImpl).destroyVolumeIfPossible(volumeVoMock);
        Mockito.verify(volumeDaoMock, Mockito.times(0)).remove(volumeMockId);
        Mockito.verify(volumeApiServiceImpl, Mockito.times(0)).stateTransitTo(volumeVoMock, Volume.Event.DestroyRequested);
    }

    @Test
    public void deleteVolumeTestVolumeStateReadyThrowingExecutionException() throws InterruptedException, ExecutionException, NoTransitionException {
        Mockito.doReturn(Volume.State.Ready).when(volumeVoMock).getState();

        Mockito.doReturn(volumeVoMock).when(volumeApiServiceImpl).retrieveAndValidateVolume(volumeMockId, accountMock);
        Mockito.doNothing().when(volumeApiServiceImpl).destroyVolumeIfPossible(volumeVoMock);
        Mockito.doThrow(ExecutionException.class).when(volumeApiServiceImpl).expungeVolumesInPrimaryStorageIfNeeded(volumeVoMock);

        Mockito.lenient().doReturn(true).when(volumeDaoMock).remove(volumeMockId);
        Mockito.lenient().doReturn(true).when(volumeApiServiceImpl).stateTransitTo(volumeVoMock, Volume.Event.DestroyRequested);

        boolean result = volumeApiServiceImpl.deleteVolume(volumeMockId, accountMock);

        Assert.assertFalse(result);
        Mockito.verify(volumeApiServiceImpl).retrieveAndValidateVolume(volumeMockId, accountMock);
        Mockito.verify(volumeApiServiceImpl).destroyVolumeIfPossible(volumeVoMock);
        Mockito.verify(volumeDaoMock, Mockito.times(0)).remove(volumeMockId);
        Mockito.verify(volumeApiServiceImpl, Mockito.times(0)).stateTransitTo(volumeVoMock, Volume.Event.DestroyRequested);
    }


    @Test(expected = RuntimeException.class)
    public void deleteVolumeTestVolumeStateReadyThrowingRuntimeException() throws InterruptedException, ExecutionException, NoTransitionException {
        Mockito.doReturn(Volume.State.Ready).when(volumeVoMock).getState();

        Mockito.doReturn(volumeVoMock).when(volumeApiServiceImpl).retrieveAndValidateVolume(volumeMockId, accountMock);
        Mockito.doNothing().when(volumeApiServiceImpl).destroyVolumeIfPossible(volumeVoMock);
        Mockito.doThrow(RuntimeException.class).when(volumeApiServiceImpl).expungeVolumesInPrimaryStorageIfNeeded(volumeVoMock);

        Mockito.lenient().doReturn(true).when(volumeDaoMock).remove(volumeMockId);
        Mockito.lenient().doReturn(true).when(volumeApiServiceImpl).stateTransitTo(volumeVoMock, Volume.Event.DestroyRequested);

        volumeApiServiceImpl.deleteVolume(volumeMockId, accountMock);
    }

    @Test
    public void doesTargetStorageSupportDiskOfferingTestDiskOfferingMoreTagsThanStorageTags() {
        DiskOfferingVO diskOfferingVoMock = Mockito.mock(DiskOfferingVO.class);
        Mockito.doReturn("A,B,C").when(diskOfferingVoMock).getTags();

        StoragePool storagePoolMock = Mockito.mock(StoragePool.class);
        Mockito.doReturn("A").when(volumeApiServiceImpl).getStoragePoolTags(storagePoolMock);

        boolean result = volumeApiServiceImpl.doesTargetStorageSupportDiskOffering(storagePoolMock, diskOfferingVoMock);

        Assert.assertFalse(result);
    }

    @Test
    public void doesTargetStorageSupportDiskOfferingTestDiskOfferingTagsIsSubSetOfStorageTags() {
        DiskOfferingVO diskOfferingVoMock = Mockito.mock(DiskOfferingVO.class);
        Mockito.doReturn("A,B,C").when(diskOfferingVoMock).getTags();

        StoragePool storagePoolMock = Mockito.mock(StoragePool.class);
        Mockito.doReturn("A,B,C,D,X,Y").when(volumeApiServiceImpl).getStoragePoolTags(storagePoolMock);

        boolean result = volumeApiServiceImpl.doesTargetStorageSupportDiskOffering(storagePoolMock, diskOfferingVoMock);

        Assert.assertTrue(result);
    }

    @Test
    public void doesTargetStorageSupportDiskOfferingTestDiskOfferingTagsEmptyAndStorageTagsNotEmpty() {
        DiskOfferingVO diskOfferingVoMock = Mockito.mock(DiskOfferingVO.class);
        Mockito.doReturn("").when(diskOfferingVoMock).getTags();

        StoragePool storagePoolMock = Mockito.mock(StoragePool.class);
        Mockito.lenient().doReturn("A,B,C,D,X,Y").when(volumeApiServiceImpl).getStoragePoolTags(storagePoolMock);

        boolean result = volumeApiServiceImpl.doesTargetStorageSupportDiskOffering(storagePoolMock, diskOfferingVoMock);

        Assert.assertTrue(result);
    }

    @Test
    public void doesTargetStorageSupportDiskOfferingTestDiskOfferingTagsNotEmptyAndStorageTagsEmpty() {
        DiskOfferingVO diskOfferingVoMock = Mockito.mock(DiskOfferingVO.class);
        Mockito.doReturn("A").when(diskOfferingVoMock).getTags();

        StoragePool storagePoolMock = Mockito.mock(StoragePool.class);
        Mockito.doReturn("").when(volumeApiServiceImpl).getStoragePoolTags(storagePoolMock);

        boolean result = volumeApiServiceImpl.doesTargetStorageSupportDiskOffering(storagePoolMock, diskOfferingVoMock);

        Assert.assertFalse(result);
    }

    @Test
    public void doesTargetStorageSupportDiskOfferingTestDiskOfferingTagsEmptyAndStorageTagsEmpty() {
        DiskOfferingVO diskOfferingVoMock = Mockito.mock(DiskOfferingVO.class);
        Mockito.doReturn("").when(diskOfferingVoMock).getTags();

        StoragePool storagePoolMock = Mockito.mock(StoragePool.class);
        Mockito.lenient().doReturn("").when(volumeApiServiceImpl).getStoragePoolTags(storagePoolMock);

        boolean result = volumeApiServiceImpl.doesTargetStorageSupportDiskOffering(storagePoolMock, diskOfferingVoMock);

        Assert.assertTrue(result);
    }

    @Test
    public void doesTargetStorageSupportDiskOfferingTestDiskOfferingTagsDifferentFromdStorageTags() {
        DiskOfferingVO diskOfferingVoMock = Mockito.mock(DiskOfferingVO.class);
        Mockito.doReturn("A,B").when(diskOfferingVoMock).getTags();

        StoragePool storagePoolMock = Mockito.mock(StoragePool.class);
        Mockito.doReturn("C,D").when(volumeApiServiceImpl).getStoragePoolTags(storagePoolMock);

        boolean result = volumeApiServiceImpl.doesTargetStorageSupportDiskOffering(storagePoolMock, diskOfferingVoMock);

        Assert.assertFalse(result);
    }

    @Test
    public void doesTargetStorageSupportDiskOfferingTestDiskOfferingTagsEqualsStorageTags() {
        DiskOfferingVO diskOfferingVoMock = Mockito.mock(DiskOfferingVO.class);
        Mockito.doReturn("A").when(diskOfferingVoMock).getTags();

        StoragePool storagePoolMock = Mockito.mock(StoragePool.class);
        Mockito.doReturn("A").when(volumeApiServiceImpl).getStoragePoolTags(storagePoolMock);

        boolean result = volumeApiServiceImpl.doesTargetStorageSupportDiskOffering(storagePoolMock, diskOfferingVoMock);

        Assert.assertTrue(result);
    }

    @Test
    public void validateIfVmHaveBackupsTestExceptionWhenTryToDetachVolumeFromVMWhichBackupOffering() {
        try {
            UserVmVO vm = Mockito.mock(UserVmVO.class);
            when(vm.getBackupOfferingId()).thenReturn(1l);
            volumeApiServiceImpl.validateIfVmHasBackups(vm, false);
        } catch (Exception e) {
            Assert.assertEquals("Unable to detach volume, cannot detach volume from a VM that has backups. First remove the VM from the backup offering or set the global configuration 'backup.enable.attach.detach.of.volumes' to true.", e.getMessage());
        }
    }

    @Test
    public void validateIfVmHaveBackupsTestExceptionWhenTryToAttachVolumeFromVMWhichBackupOffering() {
        try {
            UserVmVO vm = Mockito.mock(UserVmVO.class);
            when(vm.getBackupOfferingId()).thenReturn(1l);
            volumeApiServiceImpl.validateIfVmHasBackups(vm, true);
        } catch (Exception e) {
            Assert.assertEquals("Unable to attach volume, please specify a VM that does not have any backups or set the global configuration 'backup.enable.attach.detach.of.volumes' to true.", e.getMessage());
        }
    }

    @Test
    public void validateIfVmHaveBackupsTestSuccessWhenVMDontHaveBackupOffering() {
        UserVmVO vm = Mockito.mock(UserVmVO.class);
        when(vm.getBackupOfferingId()).thenReturn(null);
        volumeApiServiceImpl.validateIfVmHasBackups(vm, true);
    }

    @Test
    public void createVolumeInfoFromVolumesTestEmptyVolumeListReturnEmptyArray() {
        String volumeInfo = volumeApiServiceImpl.createVolumeInfoFromVolumes(new ArrayList<>());
        assertEquals("[]", volumeInfo);
    }

    @Test(expected = NullPointerException.class)
    public void createVolumeInfoFromVolumesTestNullVolume() {
        volumeApiServiceImpl.createVolumeInfoFromVolumes(null);
    }

    @Test
    public void createVolumeInfoFromVolumesTestCorrectlyConvertOfVolumes() {
        List<VolumeVO> volumesToTest = new ArrayList<>();

        VolumeVO root = new VolumeVO("test", 1l, 1l, 1l, 1l, 1l, "test", "/root/dir", ProvisioningType.THIN, 555l, Type.ROOT);
        String rootUuid = root.getUuid();

        VolumeVO data = new VolumeVO("test", 1l, 1l, 1l, 1l, 1l, "test", "/root/dir/data", ProvisioningType.THIN, 1111000l, Type.DATADISK);
        String dataUuid = data.getUuid();

        volumesToTest.add(root);
        volumesToTest.add(data);

        String result = volumeApiServiceImpl.createVolumeInfoFromVolumes(volumesToTest);
        String expected = String.format("[{\"uuid\":\"%s\",\"type\":\"ROOT\",\"size\":555,\"path\":\"/root/dir\"},{\"uuid\":\"%s\",\"type\":\"DATADISK\",\"size\":1111000,\"path\":\"/root/dir/data\"}]", rootUuid, dataUuid);

        assertEquals(expected, result);
    }

    @Test
    public void isNotPossibleToResizeTestAllFormats() {
        Storage.ImageFormat[] imageFormat = Storage.ImageFormat.values();
        for (int i = 0; i < imageFormat.length - 1; i++) {
            if (imageFormat[i] != Storage.ImageFormat.ISO) {
                prepareAndRunTestOfIsNotPossibleToResize(Type.ROOT, 10l, imageFormat[i], true);
            } else {
                prepareAndRunTestOfIsNotPossibleToResize(Type.ROOT, 10l, imageFormat[i], false);
            }
        }
    }

    @Test
    public void isNotPossibleToResizeTestAllTypes() {
        Type[] types = Type.values();
        for (int i = 0; i < types.length - 1; i++) {
            if (types[i] != Type.ROOT) {
                prepareAndRunTestOfIsNotPossibleToResize(types[i], 10l, Storage.ImageFormat.QCOW2, false);
            } else {
                prepareAndRunTestOfIsNotPossibleToResize(types[i], 10l, Storage.ImageFormat.QCOW2, true);
            }
        }
    }

    @Test
    public void isNotPossibleToResizeTestNoRootDiskSize() {
        prepareAndRunTestOfIsNotPossibleToResize(Type.ROOT, 0l, Storage.ImageFormat.QCOW2, false);
    }

    private void prepareAndRunTestOfIsNotPossibleToResize(Type volumeType, Long rootDisk, Storage.ImageFormat imageFormat, boolean expectedIsNotPossibleToResize) {
        VolumeVO volume = Mockito.mock(VolumeVO.class);
        when(volume.getVolumeType()).thenReturn(volumeType);

        when(volume.getTemplateId()).thenReturn(1l);
        DiskOfferingVO diskOffering = Mockito.mock(DiskOfferingVO.class);

        ServiceOfferingJoinVO serviceOfferingJoinVO = Mockito.mock(ServiceOfferingJoinVO.class);
        when(serviceOfferingJoinVO.getRootDiskSize()).thenReturn(rootDisk);
        when(serviceOfferingJoinDao.findById(Mockito.anyLong())).thenReturn(serviceOfferingJoinVO);

        VMTemplateVO template = Mockito.mock(VMTemplateVO.class);
        when(template.getFormat()).thenReturn(imageFormat);
        when(templateDao.findByIdIncludingRemoved(Mockito.anyLong())).thenReturn(template);

        boolean result = volumeApiServiceImpl.isNotPossibleToResize(volume, diskOffering);
        Assert.assertEquals(expectedIsNotPossibleToResize, result);
    }

    @Test (expected = InvalidParameterValueException.class)
    public void checkIfVolumeCanBeReassignedTestNullVolume() {
        volumeApiServiceImpl.validateVolume(volumeVoMock.getUuid(), null);
    }

    @Test (expected = PermissionDeniedException.class)
    public void checkIfVolumeCanBeReassignedTestAttachedVolume() {
        Mockito.doReturn(vmInstanceMockId).when(volumeVoMock).getInstanceId();

        volumeApiServiceImpl.validateVolume(volumeVoMock.getUuid(), volumeVoMock);
    }

    @Test (expected = PermissionDeniedException.class)
    @PrepareForTest (CollectionUtils.class)
    public void checkIfVolumeCanBeReassignedTestVolumeWithSnapshots() {
        Mockito.doReturn(null).when(volumeVoMock).getInstanceId();
        Mockito.doReturn(snapshotVOArrayListMock).when(snapshotDaoMock).listByStatusNotIn(Mockito.anyLong(), Mockito.any(), Mockito.any());

        PowerMockito.mockStatic(CollectionUtils.class);
        PowerMockito.when(CollectionUtils.isNotEmpty(snapshotVOArrayListMock)).thenReturn(true);

        volumeApiServiceImpl.validateVolume(volumeVoMock.getUuid(), volumeVoMock);
    }

    @Test
    @PrepareForTest (CollectionUtils.class)
    public void checkIfVolumeCanBeReassignedTestValidVolume() {
        Mockito.doReturn(null).when(volumeVoMock).getInstanceId();
        Mockito.doReturn(snapshotVOArrayListMock).when(snapshotDaoMock).listByStatusNotIn(Mockito.anyLong(), Mockito.any(), Mockito.any());

        PowerMockito.mockStatic(CollectionUtils.class);
        PowerMockito.when(CollectionUtils.isNotEmpty(snapshotVOArrayListMock)).thenReturn(false);

        volumeApiServiceImpl.validateVolume(volumeVoMock.getUuid(), volumeVoMock);
    }

    @Test (expected = InvalidParameterValueException.class)
    public void validateAccountsTestNullOldAccount() {
        volumeApiServiceImpl.validateAccounts(accountMock.getUuid(), volumeVoMock, null, accountMock);
    }

    @Test (expected = InvalidParameterValueException.class)
    public void validateAccountsTestNullNewAccount() {
        volumeApiServiceImpl.validateAccounts(accountMock.getUuid(), volumeVoMock, accountMock, null);
    }

    @Test (expected = InvalidParameterValueException.class)
    public void validateAccountsTestDisabledNewAccount() {
        Mockito.doReturn(Account.State.DISABLED).when(accountMock).getState();
        volumeApiServiceImpl.validateAccounts(accountMock.getUuid(), volumeVoMock, null, accountMock);
    }

    @Test (expected = InvalidParameterValueException.class)
    public void validateAccountsTestLockedNewAccount() {
        Mockito.doReturn(Account.State.LOCKED).when(accountMock).getState();
        volumeApiServiceImpl.validateAccounts(accountMock.getUuid(), volumeVoMock, null, accountMock);
    }

    @Test (expected = InvalidParameterValueException.class)
    public void validateAccountsTestSameAccounts() {
        volumeApiServiceImpl.validateAccounts(accountMock.getUuid(), volumeVoMock, accountMock, accountMock);
    }

    @Test
    public void validateAccountsTestValidAccounts() {
        Account newAccount = new AccountVO(accountMockId+1);
        volumeApiServiceImpl.validateAccounts(accountMock.getUuid(), volumeVoMock, accountMock, newAccount);
    }

    @Test
    @PrepareForTest(UsageEventUtils.class)
    public void updateVolumeAccountTest() {
        PowerMockito.mockStatic(UsageEventUtils.class);
        Account newAccountMock = new AccountVO(accountMockId+1);

        Mockito.doReturn(volumeVoMock).when(volumeDaoMock).persist(volumeVoMock);

        volumeApiServiceImpl.updateVolumeAccount(accountMock, volumeVoMock, newAccountMock);

        PowerMockito.verifyStatic(UsageEventUtils.class);
        UsageEventUtils.publishUsageEvent(EventTypes.EVENT_VOLUME_DELETE, volumeVoMock.getAccountId(), volumeVoMock.getDataCenterId(), volumeVoMock.getId(),
                volumeVoMock.getName(), Volume.class.getName(), volumeVoMock.getUuid(), volumeVoMock.isDisplayVolume());

        Mockito.verify(resourceLimitServiceMock).decrementResourceCount(accountMock.getAccountId(), ResourceType.volume, ByteScaleUtils.bytesToGibibytes(volumeVoMock.getSize()));
        Mockito.verify(resourceLimitServiceMock).decrementResourceCount(accountMock.getAccountId(), ResourceType.primary_storage, volumeVoMock.getSize());

        Mockito.verify(volumeVoMock).setAccountId(newAccountMock.getAccountId());
        Mockito.verify(volumeVoMock).setDomainId(newAccountMock.getDomainId());

        Mockito.verify(volumeDaoMock).persist(volumeVoMock);

        Mockito.verify(resourceLimitServiceMock).incrementResourceCount(newAccountMock.getAccountId(), ResourceType.volume, ByteScaleUtils.bytesToGibibytes(volumeVoMock.getSize()));
        Mockito.verify(resourceLimitServiceMock).incrementResourceCount(newAccountMock.getAccountId(), ResourceType.primary_storage, volumeVoMock.getSize());

        PowerMockito.verifyStatic(UsageEventUtils.class);
        UsageEventUtils.publishUsageEvent(EventTypes.EVENT_VOLUME_DELETE, volumeVoMock.getAccountId(), volumeVoMock.getDataCenterId(), volumeVoMock.getId(),
                volumeVoMock.getName(), Volume.class.getName(), volumeVoMock.getUuid(), volumeVoMock.isDisplayVolume());

        Mockito.verify(volumeServiceMock).moveVolumeOnSecondaryStorageToAnotherAccount(volumeVoMock, accountMock, newAccountMock);
    }


    @Test (expected = InvalidParameterValueException.class)
    public void getAccountOrProjectTestAccountAndProjectInformed() {
        volumeApiServiceImpl.getAccountOrProject(projectMock.getUuid(), accountMock.getId(), projectMock.getId(), accountMock);
    }

    @Test (expected = InvalidParameterValueException.class)
    public void getAccountOrProjectTestUnableToFindProject() {
        Mockito.doReturn(null).when(projectManagerMock).getProject(projecMockId);
        volumeApiServiceImpl.getAccountOrProject(projectMock.getUuid(), null, projectMock.getId(), accountMock);
    }

    @Test (expected = PermissionDeniedException.class)
    public void getAccountOrProjectTestCallerDoesNotHaveAccessToProject() {
        Mockito.doReturn(projectMock).when(projectManagerMock).getProject(projecMockId);
        Mockito.doReturn(false).when(projectManagerMock).canAccessProjectAccount(accountMock, projectMockAccountId);
        volumeApiServiceImpl.getAccountOrProject(projectMock.getUuid(), null, projectMock.getId(), accountMock);
    }

    @Test
    public void getAccountOrProjectTestValidProject() {
        Mockito.doReturn(projectMock).when(projectManagerMock).getProject(projecMockId);
        Mockito.doReturn(true).when(projectManagerMock).canAccessProjectAccount(accountMock, projectMockAccountId);
        volumeApiServiceImpl.getAccountOrProject(projectMock.getUuid(), null, projectMock.getId(), accountMock);
    }

    @Test
    public void getAccountOrProjectTestValidAccount() {
        volumeApiServiceImpl.getAccountOrProject(projectMock.getUuid(), accountMock.getId(),null, accountMock);
    }

    @Test
    @PrepareForTest(UsageEventUtils.class)
    public void publishVolumeCreationUsageEventTestNullDiskOfferingId() {
        Mockito.doReturn(null).when(volumeVoMock).getDiskOfferingId();
        PowerMockito.mockStatic(UsageEventUtils.class);

        volumeApiServiceImpl.publishVolumeCreationUsageEvent(volumeVoMock);

        PowerMockito.verifyStatic(UsageEventUtils.class);
        UsageEventUtils.publishUsageEvent(EventTypes.EVENT_VOLUME_CREATE, volumeVoMock.getAccountId(), volumeVoMock.getDataCenterId(), volumeVoMock.getId(), volumeVoMock.getName(),
                null, volumeVoMock.getTemplateId(), volumeVoMock.getSize(), Volume.class.getName(), volumeVoMock.getUuid(), volumeVoMock.isDisplay());

    }

    @Test
    @PrepareForTest(UsageEventUtils.class)
    public void publishVolumeCreationUsageEventTestNullDiskOfferingVo() {
        Mockito.doReturn(diskOfferingMockId).when(volumeVoMock).getDiskOfferingId();
        Mockito.doReturn(null).when(_diskOfferingDao).findById(diskOfferingMockId);
        PowerMockito.mockStatic(UsageEventUtils.class);

        volumeApiServiceImpl.publishVolumeCreationUsageEvent(volumeVoMock);

        PowerMockito.verifyStatic(UsageEventUtils.class);
        UsageEventUtils.publishUsageEvent(EventTypes.EVENT_VOLUME_CREATE, volumeVoMock.getAccountId(), volumeVoMock.getDataCenterId(), volumeVoMock.getId(), volumeVoMock.getName(),
                null, volumeVoMock.getTemplateId(), volumeVoMock.getSize(), Volume.class.getName(), volumeVoMock.getUuid(), volumeVoMock.isDisplay());

    }

    @Test
    @PrepareForTest(UsageEventUtils.class)
    public void publishVolumeCreationUsageEventTestDiskOfferingVoTypeNotDisk() {
        Mockito.doReturn(diskOfferingMockId).when(volumeVoMock).getDiskOfferingId();
        Mockito.doReturn(newDiskOfferingMock).when(_diskOfferingDao).findById(diskOfferingMockId);
        Mockito.doReturn(true).when(newDiskOfferingMock).isComputeOnly();

        PowerMockito.mockStatic(UsageEventUtils.class);

        volumeApiServiceImpl.publishVolumeCreationUsageEvent(volumeVoMock);

        PowerMockito.verifyStatic(UsageEventUtils.class);
        UsageEventUtils.publishUsageEvent(EventTypes.EVENT_VOLUME_CREATE, volumeVoMock.getAccountId(), volumeVoMock.getDataCenterId(), volumeVoMock.getId(), volumeVoMock.getName(),
                null, volumeVoMock.getTemplateId(), volumeVoMock.getSize(), Volume.class.getName(), volumeVoMock.getUuid(), volumeVoMock.isDisplay());

    }

    @Test
    @PrepareForTest(UsageEventUtils.class)
    public void publishVolumeCreationUsageEventTestOfferingIdNotNull() {
        Mockito.doReturn(diskOfferingMockId).when(volumeVoMock).getDiskOfferingId();
        Mockito.doReturn(newDiskOfferingMock).when(_diskOfferingDao).findById(diskOfferingMockId);
        Mockito.doReturn(false).when(newDiskOfferingMock).isComputeOnly();
        Mockito.doReturn(offeringMockId).when(newDiskOfferingMock).getId();

        PowerMockito.mockStatic(UsageEventUtils.class);

        volumeApiServiceImpl.publishVolumeCreationUsageEvent(volumeVoMock);

        PowerMockito.verifyStatic(UsageEventUtils.class);
        UsageEventUtils.publishUsageEvent(EventTypes.EVENT_VOLUME_CREATE, volumeVoMock.getAccountId(), volumeVoMock.getDataCenterId(), volumeVoMock.getId(), volumeVoMock.getName(),
                offeringMockId, volumeVoMock.getTemplateId(), volumeVoMock.getSize(), Volume.class.getName(), volumeVoMock.getUuid(), volumeVoMock.isDisplay());

    }

    private void testBaseListOrderedHostsHypervisorVersionInDc(List<String> hwVersions, HypervisorType hypervisorType,
                                                               String expected) {
        when(_hostDao.listOrderedHostsHypervisorVersionsInDatacenter(anyLong(), any(HypervisorType.class)))
                .thenReturn(hwVersions);
        String min = volumeApiServiceImpl.getMinimumHypervisorVersionInDatacenter(1L, hypervisorType);
        Assert.assertEquals(expected, min);
    }

    @Test
    public void testGetMinimumHypervisorVersionInDatacenterSimulator() {
        List<String> hwVersions = List.of("4.17.3.0-SNAPSHOT");
        HypervisorType hypervisorType = HypervisorType.Simulator;
        String expected = "default";
        testBaseListOrderedHostsHypervisorVersionInDc(hwVersions, hypervisorType, expected);
    }

    @Test
    public void testGetMinimumHypervisorVersionInDatacenterEmptyVersion() {
        List<String> hwVersions = List.of("", "xxxx", "yyyy");
        HypervisorType hypervisorType = HypervisorType.KVM;
        String expected = "default";
        testBaseListOrderedHostsHypervisorVersionInDc(hwVersions, hypervisorType, expected);
    }

    @Test
    public void testGetMinimumHypervisorVersionInDatacenterVersions() {
        List<String> hwVersions = List.of("6.7", "6.7.1", "6.7.2");
        HypervisorType hypervisorType = HypervisorType.VMware;
        String expected = "6.7";
        testBaseListOrderedHostsHypervisorVersionInDc(hwVersions, hypervisorType, expected);
    }
}

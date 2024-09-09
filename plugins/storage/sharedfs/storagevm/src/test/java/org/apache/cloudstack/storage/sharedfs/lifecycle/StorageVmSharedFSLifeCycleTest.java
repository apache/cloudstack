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

package org.apache.cloudstack.storage.sharedfs.lifecycle;

import static org.apache.cloudstack.storage.sharedfs.provider.StorageVmSharedFSProvider.SHAREDFSVM_MIN_CPU_COUNT;
import static org.apache.cloudstack.storage.sharedfs.provider.StorageVmSharedFSProvider.SHAREDFSVM_MIN_RAM_SIZE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.storage.sharedfs.SharedFS;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.network.Network;
import com.cloud.offering.ServiceOffering;
import com.cloud.resource.ResourceManager;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeApiService;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.storage.dao.LaunchPermissionDao;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.uservm.UserVm;
import com.cloud.utils.FileUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.UserVmManager;
import com.cloud.vm.UserVmService;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.dao.NicDao;
import com.cloud.vm.dao.UserVmDao;

@RunWith(MockitoJUnitRunner.class)
public class StorageVmSharedFSLifeCycleTest {
    @Mock
    private AccountManager accountMgr;

    @Mock
    protected ResourceManager resourceMgr;

    @Mock
    private VirtualMachineManager virtualMachineManager;

    @Mock
    private VolumeApiService volumeApiService;

    @Mock
    protected UserVmService userVmService;

    @Mock
    protected UserVmManager userVmManager;

    @Mock
    private DataCenterDao dataCenterDao;

    @Mock
    private VMTemplateDao templateDao;

    @Mock
    VolumeDao volumeDao;

    @Mock
    private UserVmDao userVmDao;

    @Mock
    NicDao nicDao;

    @Mock
    ServiceOfferingDao serviceOfferingDao;

    @Mock
    private DiskOfferingDao diskOfferingDao;

    @Mock
    protected LaunchPermissionDao launchPermissionDao;

    @Spy
    @InjectMocks
    StorageVmSharedFSLifeCycle lifeCycle;

    private static final long s_ownerId = 1L;
    private static final long s_zoneId = 2L;
    private static final long s_diskOfferingId = 3L;
    private static final long s_serviceOfferingId = 4L;
    private static final long s_templateId = 5L;
    private static final long s_volumeId = 6L;
    private static final long s_vmId = 7L;
    private static final long s_networkId = 8L;
    private static final long s_size = 10L;
    private static final long s_minIops = 1000L;
    private static final long s_maxIops = 2000L;
    private static final String s_fsFormat = "EXT4";
    private static final String s_name = "TestSharedFS";

    private MockedStatic<FileUtil> fileUtilMocked;
    private MockedStatic<CallContext> callContextMocked;

    private AutoCloseable closeable;

    @Before
    public void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
        callContextMocked = mockStatic(CallContext.class);
        CallContext callContextMock = mock(CallContext.class);
        callContextMocked.when(CallContext::current).thenReturn(callContextMock);
        Account owner = mock(Account.class);
        when(callContextMock.getCallingAccount()).thenReturn(owner);
        CallContext vmContext = mock(CallContext.class);
        when(callContextMock.register(CallContext.current(), ApiCommandResourceType.VirtualMachine)).thenReturn(vmContext);

        fileUtilMocked = mockStatic(FileUtil.class);
        fileUtilMocked.when(() -> FileUtil.readResourceFile("/conf/fsvm-init.yml")).thenReturn("");
    }

    @After
    public void tearDown() throws Exception {
        fileUtilMocked.close();
        callContextMocked.close();
        closeable.close();
    }

    @Test
    public void testCheckPrerequisites() {
        DataCenterVO zone = mock(DataCenterVO.class);
        when(zone.getId()).thenReturn(s_zoneId);
        ServiceOfferingVO serviceOfferingVO = mock(ServiceOfferingVO.class);
        when(serviceOfferingVO.getCpu()).thenReturn(4);
        when(serviceOfferingVO.getRamSize()).thenReturn(1024);
        when(serviceOfferingVO.isOfferHA()).thenReturn(true);
        when(serviceOfferingDao.findById(s_serviceOfferingId)).thenReturn(serviceOfferingVO);
        lifeCycle.checkPrerequisites(zone, s_serviceOfferingId);
    }

    @Test
    public void testCheckPrerequisitesMinCpuException() {
        DataCenterVO zone = mock(DataCenterVO.class);
        when(zone.getId()).thenReturn(s_zoneId);
        ServiceOfferingVO serviceOfferingVO = mock(ServiceOfferingVO.class);
        when(serviceOfferingDao.findById(s_serviceOfferingId)).thenReturn(serviceOfferingVO);
        InvalidParameterValueException exception = Assert.assertThrows(InvalidParameterValueException.class, () -> lifeCycle.checkPrerequisites(zone, s_serviceOfferingId));
        Assert.assertEquals(exception.getMessage(), "Service offering's number of cpu should be greater than or equal to " + SHAREDFSVM_MIN_CPU_COUNT.key());
    }

    @Test
    public void testCheckPrerequisitesMinRamException() {
        DataCenterVO zone = mock(DataCenterVO.class);
        when(zone.getId()).thenReturn(s_zoneId);
        ServiceOfferingVO serviceOfferingVO = mock(ServiceOfferingVO.class);
        when(serviceOfferingDao.findById(s_serviceOfferingId)).thenReturn(serviceOfferingVO);
        when(serviceOfferingVO.getCpu()).thenReturn(4);
        InvalidParameterValueException exception = Assert.assertThrows(InvalidParameterValueException.class, () -> lifeCycle.checkPrerequisites(zone, s_serviceOfferingId));
        Assert.assertEquals(exception.getMessage(), "Service offering's ram size should be greater than or equal to " + SHAREDFSVM_MIN_RAM_SIZE.key());
    }

    @Test
    public void testCheckPrerequisitesHAException() {
        DataCenterVO zone = mock(DataCenterVO.class);
        when(zone.getId()).thenReturn(s_zoneId);
        ServiceOfferingVO serviceOfferingVO = mock(ServiceOfferingVO.class);
        when(serviceOfferingDao.findById(s_serviceOfferingId)).thenReturn(serviceOfferingVO);
        when(serviceOfferingVO.getCpu()).thenReturn(4);
        when(serviceOfferingVO.getRamSize()).thenReturn(1024);
        InvalidParameterValueException exception = Assert.assertThrows(InvalidParameterValueException.class, () -> lifeCycle.checkPrerequisites(zone, s_serviceOfferingId));
        Assert.assertEquals(exception.getMessage(), "Service offering's should be HA enabled");
    }

    private SharedFS prepareDeploySharedFS() throws ResourceUnavailableException, InsufficientCapacityException, ResourceAllocationException {
        SharedFS sharedFS = mock(SharedFS.class);
        when(sharedFS.getDataCenterId()).thenReturn(s_zoneId);
        when(sharedFS.getName()).thenReturn(s_name);
        when(sharedFS.getServiceOfferingId()).thenReturn(s_serviceOfferingId);
        when(sharedFS.getFsType()).thenReturn(SharedFS.FileSystemType.valueOf(s_fsFormat));
        when(sharedFS.getAccountId()).thenReturn(s_ownerId);

        DataCenterVO zone = mock(DataCenterVO.class);
        when(dataCenterDao.findById(s_zoneId)).thenReturn(zone);
        when(resourceMgr.getSupportedHypervisorTypes(s_zoneId, false, null)).thenReturn(List.of(Hypervisor.HypervisorType.KVM));

        ServiceOfferingVO serviceOffering = mock(ServiceOfferingVO.class);
        when(serviceOfferingDao.findById(s_serviceOfferingId)).thenReturn(serviceOffering);

        VMTemplateVO template = mock(VMTemplateVO.class);
        when(templateDao.findSystemVMReadyTemplate(s_zoneId, Hypervisor.HypervisorType.KVM)).thenReturn(template);
        when(template.getId()).thenReturn(s_templateId);

        return sharedFS;
    }

    @Test
    public void testDeploySharedFS() throws ResourceUnavailableException, InsufficientCapacityException, ResourceAllocationException, IOException, OperationTimedoutException {
        SharedFS sharedFS = prepareDeploySharedFS();
        when(sharedFS.getAccountId()).thenReturn(s_ownerId);

        Account owner = mock(Account.class);
        when(owner.getId()).thenReturn(s_ownerId);
        when(accountMgr.getActiveAccountById(s_ownerId)).thenReturn(owner);

        UserVm vm = mock(UserVm.class);
        when(vm.getId()).thenReturn(s_vmId);
        when(userVmService.createAdvancedVirtualMachine(
                any(DataCenter.class), any(ServiceOffering.class), any(VirtualMachineTemplate.class), anyList(), any(Account.class), anyString(),
                anyString(), anyLong(), anyLong(), isNull(), any(Hypervisor.HypervisorType.class), any(BaseCmd.HTTPMethod.class), anyString(),
                isNull(), isNull(), anyList(), isNull(), any(Network.IpAddresses.class), isNull(), isNull(), isNull(),
                anyMap(), isNull(), isNull(), isNull(), isNull(),
                anyBoolean(), anyString(), isNull())).thenReturn(vm);

        VolumeVO volume = mock(VolumeVO.class);
        when(volume.getId()).thenReturn(s_volumeId);
        when(volumeDao.findByInstanceAndType(s_vmId, Volume.Type.DATADISK)).thenReturn(List.of(volume));

         Pair<Long, Long> result = lifeCycle.deploySharedFS(sharedFS, s_networkId, s_diskOfferingId, s_size, s_minIops, s_maxIops);
         Assert.assertEquals(Optional.ofNullable(result.first()), Optional.ofNullable(s_volumeId));
         Assert.assertEquals(Optional.ofNullable(result.second()), Optional.ofNullable(s_vmId));
    }

    @Test(expected = CloudRuntimeException.class)
    public void testDeploySharedFSHypervisorNotFound() throws ResourceUnavailableException, InsufficientCapacityException, ResourceAllocationException, IOException, OperationTimedoutException {
        SharedFS sharedFS = mock(SharedFS.class);
        when(sharedFS.getDataCenterId()).thenReturn(s_zoneId);
        when(sharedFS.getName()).thenReturn(s_name);
        when(sharedFS.getServiceOfferingId()).thenReturn(s_serviceOfferingId);
        when(sharedFS.getFsType()).thenReturn(SharedFS.FileSystemType.valueOf(s_fsFormat));
        when(sharedFS.getAccountId()).thenReturn(s_ownerId);

        when(accountMgr.getActiveAccountById(s_ownerId)).thenReturn(null);
        DataCenterVO zone = mock(DataCenterVO.class);
        when(dataCenterDao.findById(s_zoneId)).thenReturn(zone);
        lifeCycle.deploySharedFS(sharedFS, s_networkId, s_diskOfferingId, s_size, s_minIops, s_maxIops);
    }

    @Test(expected = CloudRuntimeException.class)
    public void testDeploySharedFSTemplateNotFound() throws ResourceUnavailableException, InsufficientCapacityException, ResourceAllocationException, IOException, OperationTimedoutException {
        SharedFS sharedFS = mock(SharedFS.class);
        when(sharedFS.getDataCenterId()).thenReturn(s_zoneId);
        when(sharedFS.getName()).thenReturn(s_name);
        when(sharedFS.getServiceOfferingId()).thenReturn(s_serviceOfferingId);
        when(sharedFS.getFsType()).thenReturn(SharedFS.FileSystemType.valueOf(s_fsFormat));
        when(sharedFS.getAccountId()).thenReturn(s_ownerId);

        when(accountMgr.getActiveAccountById(s_ownerId)).thenReturn(null);
        DataCenterVO zone = mock(DataCenterVO.class);
        when(dataCenterDao.findById(s_zoneId)).thenReturn(zone);
        when(resourceMgr.getSupportedHypervisorTypes(s_zoneId, false, null)).thenReturn(List.of(Hypervisor.HypervisorType.KVM));

        when(templateDao.findSystemVMReadyTemplate(s_zoneId, Hypervisor.HypervisorType.KVM)).thenReturn(null);
        lifeCycle.deploySharedFS(sharedFS, s_networkId, s_diskOfferingId, s_size, s_minIops, s_maxIops);
    }

    @Test
    public void testDeleteSharedFS() throws ResourceUnavailableException {
        SharedFS sharedFS = mock(SharedFS.class);
        when(sharedFS.getVmId()).thenReturn(s_vmId);
        when(sharedFS.getVolumeId()).thenReturn(s_volumeId);

        UserVmVO vm = mock(UserVmVO.class);
        when(vm.getId()).thenReturn(s_vmId);
        when(userVmDao.findById(s_vmId)).thenReturn(vm);
        when(userVmService.destroyVm(s_vmId, true)).thenReturn(vm);
        when(userVmManager.expunge(vm)).thenReturn(true);

        VolumeVO volume = mock(VolumeVO.class);
        when(volumeDao.findById(s_volumeId)).thenReturn(volume);
        when(volume.getId()).thenReturn(s_volumeId);
        when(volume.getState()).thenReturn(Volume.State.Allocated);

        Assert.assertEquals(lifeCycle.deleteSharedFS(sharedFS), true);
    }

    @Test
    public void testReDeploySharedFS() throws ResourceUnavailableException, InsufficientCapacityException, ResourceAllocationException, IOException, OperationTimedoutException {
        SharedFS sharedFS = mock(SharedFS.class);
        Long vmId = 1L;
        when(sharedFS.getVmId()).thenReturn(vmId);
        UserVm vm = mock(UserVm.class);
        when(virtualMachineManager.restoreVirtualMachine(vmId, null, null, true, null)).thenReturn(vm);
        boolean result = lifeCycle.reDeploySharedFS(sharedFS);
        Assert.assertEquals(result, true);
    }
}

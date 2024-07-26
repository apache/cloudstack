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
package com.cloud.hypervisor.guru;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.cloud.hypervisor.vmware.mo.VirtualMachineMO;
import com.cloud.storage.VolumeApiService;
import com.cloud.storage.dao.VolumeDao;
import com.vmware.vim25.VirtualDisk;
import com.vmware.vim25.VirtualDiskFlatVer2BackingInfo;
import org.apache.cloudstack.backup.Backup;
import org.apache.cloudstack.backup.BackupVO;
import org.apache.cloudstack.storage.NfsMountManager;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.vm.UnmanagedInstanceTO;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import com.cloud.agent.api.Command;
import com.cloud.agent.api.MigrateVmToPoolCommand;
import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.agent.api.to.NfsTO;
import com.cloud.dc.ClusterDetailsDao;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.vmware.mo.DatacenterMO;
import com.cloud.hypervisor.vmware.mo.DatastoreMO;
import com.cloud.hypervisor.vmware.mo.HostMO;
import com.cloud.hypervisor.vmware.util.VmwareClient;
import com.cloud.hypervisor.vmware.util.VmwareContext;
import com.cloud.hypervisor.vmware.util.VmwareHelper;
import com.cloud.storage.Storage;
import com.cloud.storage.Storage.ProvisioningType;
import com.cloud.storage.StoragePool;
import com.cloud.storage.StoragePoolHostVO;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.StoragePoolHostDao;
import com.cloud.utils.Pair;
import com.cloud.vm.VMInstanceVO;
import com.cloud.utils.UuidUtils;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.VmDetailConstants;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.VimPortType;
import com.vmware.vim25.VirtualMachinePowerState;

@RunWith(MockitoJUnitRunner.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class)
public class VMwareGuruTest {

    @Spy
    @InjectMocks
    private VMwareGuru vMwareGuru = new VMwareGuru();

    @Mock
    PrimaryDataStoreDao _storagePoolDao;

    @Mock
    StoragePoolHostDao storagePoolHostDao;

    @Mock
    HostDao _hostDao;

    @Mock
    VirtualMachineManager vmManager;

    @Mock
    ClusterDetailsDao _clusterDetailsDao;

    @Mock
    VolumeDao volumeDao;

    @Mock
    VolumeApiService volumeService;

    AutoCloseable closeable;

    @Mock
    NfsMountManager mountManager;

    private static MockedStatic<VmwareHelper> mockedVmwareHelper;

    @BeforeClass
    public static void init() {
        mockedVmwareHelper = Mockito.mockStatic(VmwareHelper.class);
    }

    @AfterClass
    public static void close() {
        mockedVmwareHelper.close();
    }

    @Before
    public void testSetUp() throws Exception {
        closeable = MockitoAnnotations.openMocks(this);
    }

    @After
    public void tearDown() throws Exception {
        closeable.close();
    }

    @Test
    public void finalizeMigrateForLocalStorageToHaveTargetHostGuid(){
        VirtualMachine vm = Mockito.mock(VirtualMachine.class);
        Map<Volume, StoragePool> volumeToPool = new HashMap<>();
        Volume rootVolume = Mockito.mock(Volume.class);
        Volume dataVolume = Mockito.mock(Volume.class);
        StoragePool localStorage = Mockito.mock(StoragePool.class);
        volumeToPool.put(rootVolume, localStorage);
        volumeToPool.put(dataVolume, localStorage);

        // prepare localstorage host guid
        StoragePoolVO storagePoolVO = Mockito.mock(StoragePoolVO.class);
        StoragePoolHostVO storagePoolHostVO = Mockito.mock(StoragePoolHostVO.class);
        HostVO hostVO = Mockito.mock(HostVO.class);

        Mockito.when(localStorage.getId()).thenReturn(1L);
        Mockito.when(_storagePoolDao.findById(1L)).thenReturn(storagePoolVO);
        Mockito.when(rootVolume.getVolumeType()).thenReturn(Volume.Type.ROOT);
        Mockito.when(dataVolume.getVolumeType()).thenReturn(Volume.Type.DATADISK);
        Mockito.when(localStorage.isLocal()).thenReturn(true);
        Pair<Long, Long> clusterAndHost = new Pair<>(1L, 1L);

        Mockito.when(vmManager.findClusterAndHostIdForVm(vm, true)).thenReturn(clusterAndHost);

        List<StoragePoolHostVO> storagePoolHostVOS = new ArrayList<>();
        storagePoolHostVOS.add(storagePoolHostVO);
        Mockito.when(storagePoolHostDao.listByPoolId(1L)).thenReturn(storagePoolHostVOS);
        Mockito.when(storagePoolHostVO.getHostId()).thenReturn(2L);
        Mockito.when(_hostDao.findById(2L)).thenReturn(hostVO);
        Mockito.when(hostVO.getGuid()).thenReturn("HostSystem:host-a@x.x.x.x");

        List<Command> commandsList = vMwareGuru.finalizeMigrate(vm, volumeToPool);

        MigrateVmToPoolCommand migrateVmToPoolCommand = (MigrateVmToPoolCommand) commandsList.get(0);
        Assert.assertEquals("HostSystem:host-a@x.x.x.x", migrateVmToPoolCommand.getHostGuidInTargetCluster());
    }

    @Test
    public void detachVolumeTestWhenVolumePathDontExistsInDb() {
        VirtualDisk virtualDisk = Mockito.mock(VirtualDisk.class);
        VirtualDiskFlatVer2BackingInfo info = Mockito.mock(VirtualDiskFlatVer2BackingInfo.class);
        Mockito.when(virtualDisk.getBacking()).thenReturn(info);
        Mockito.when(info.getFileName()).thenReturn("[ae4e2064cdbf3587908f726a23f9a5a3] i-2-444-VM/6b10e0316c5e441dbaeb23a806679c8d.vmdk");
        Mockito.when(volumeDao.findByPath(Mockito.eq("6b10e0316c5e441dbaeb23a806679c8d"))).thenReturn(null);
        VMInstanceVO vmInstanceVO = new VMInstanceVO();
        BackupVO backupVO = new BackupVO();

        VolumeVO detachVolume = vMwareGuru.detachVolume(vmInstanceVO, virtualDisk, backupVO);
        Assert.assertEquals(null, detachVolume);
    }

    @Test
    public void detachVolumeTestWhenVolumeExistsButIsOwnedByAnotherInstance() {
        VirtualDisk virtualDisk = Mockito.mock(VirtualDisk.class);
        VirtualDiskFlatVer2BackingInfo info = Mockito.mock(VirtualDiskFlatVer2BackingInfo.class);
        VolumeVO volumeVO = Mockito.mock(VolumeVO.class);
        VMInstanceVO vmInstanceVO = Mockito.mock(VMInstanceVO.class);

        Mockito.when(virtualDisk.getBacking()).thenReturn(info);
        Mockito.when(info.getFileName()).thenReturn("[ae4e2064cdbf3587908f726a23f9a5a3] i-2-444-VM/6b10e0316c5e441dbaeb23a806679c8d.vmdk");
        Mockito.when(volumeDao.findByPath(Mockito.eq("6b10e0316c5e441dbaeb23a806679c8d"))).thenReturn(volumeVO);
        Mockito.when(volumeVO.getInstanceId()).thenReturn(2L);
        Mockito.when(vmInstanceVO.getId()).thenReturn(1L);
        BackupVO backupVO = new BackupVO();

        Mockito.verify(volumeService, Mockito.never()).detachVolumeFromVM(Mockito.any());
        vMwareGuru.detachVolume(vmInstanceVO, virtualDisk, backupVO);
    }

    @Test
    public void detachVolumeTestWhenVolumeExistsButIsRemoved() {
        VirtualDisk virtualDisk = Mockito.mock(VirtualDisk.class);
        VirtualDiskFlatVer2BackingInfo info = Mockito.mock(VirtualDiskFlatVer2BackingInfo.class);
        VolumeVO volumeVO = Mockito.mock(VolumeVO.class);
        VMInstanceVO vmInstanceVO = Mockito.mock(VMInstanceVO.class);

        Mockito.when(volumeVO.getInstanceId()).thenReturn(1L);
        Mockito.when(volumeVO.getRemoved()).thenReturn(new Date());
        Mockito.when(virtualDisk.getBacking()).thenReturn(info);
        Mockito.when(info.getFileName()).thenReturn("[ae4e2064cdbf3587908f726a23f9a5a3] i-2-444-VM/6b10e0316c5e441dbaeb23a806679c8d.vmdk");
        Mockito.when(volumeDao.findByPath(Mockito.eq("6b10e0316c5e441dbaeb23a806679c8d"))).thenReturn(volumeVO);
        Mockito.when(vmInstanceVO.getId()).thenReturn(1L);
        BackupVO backupVO = new BackupVO();

        Mockito.verify(volumeService, Mockito.never()).detachVolumeFromVM(Mockito.any());
        vMwareGuru.detachVolume(vmInstanceVO, virtualDisk, backupVO);
    }

    @Test
    public void detachVolumeTestWhenVolumeExistsButDetachFail() {
        VirtualDisk virtualDisk = Mockito.mock(VirtualDisk.class);
        VirtualDiskFlatVer2BackingInfo info = Mockito.mock(VirtualDiskFlatVer2BackingInfo.class);
        VolumeVO volumeVO = Mockito.mock(VolumeVO.class);
        VMInstanceVO vmInstanceVO = Mockito.mock(VMInstanceVO.class);
        BackupVO backupVO = Mockito.mock(BackupVO.class);

        Mockito.when(volumeVO.getInstanceId()).thenReturn(1L);
        Mockito.when(volumeVO.getUuid()).thenReturn("123");
        Mockito.when(backupVO.getUuid()).thenReturn("321");
        Mockito.when(vmInstanceVO.getInstanceName()).thenReturn("test1");
        Mockito.when(vmInstanceVO.getUuid()).thenReturn("1234");
        Mockito.when(virtualDisk.getBacking()).thenReturn(info);
        Mockito.when(info.getFileName()).thenReturn("[ae4e2064cdbf3587908f726a23f9a5a3] i-2-444-VM/6b10e0316c5e441dbaeb23a806679c8d.vmdk");
        Mockito.when(volumeDao.findByPath(Mockito.eq("6b10e0316c5e441dbaeb23a806679c8d"))).thenReturn(volumeVO);
        Mockito.when(vmInstanceVO.getId()).thenReturn(1L);
        Mockito.when(volumeService.detachVolumeFromVM(Mockito.any())).thenReturn(null);

        vMwareGuru.detachVolume(vmInstanceVO, virtualDisk, backupVO);
        Mockito.verify(volumeService, Mockito.times(1)).detachVolumeFromVM(Mockito.any());
    }

    @Test
    public void detachVolumeTestWhenVolumeExistsAndDetachDontFail() {
        VirtualDisk virtualDisk = Mockito.mock(VirtualDisk.class);
        VirtualDiskFlatVer2BackingInfo info = Mockito.mock(VirtualDiskFlatVer2BackingInfo.class);
        VolumeVO volumeVO = Mockito.mock(VolumeVO.class);
        VMInstanceVO vmInstanceVO = Mockito.mock(VMInstanceVO.class);
        BackupVO backupVO = Mockito.mock(BackupVO.class);

        Mockito.when(volumeVO.getInstanceId()).thenReturn(1L);
        Mockito.when(volumeVO.getUuid()).thenReturn("123");
        Mockito.when(backupVO.getUuid()).thenReturn("321");
        Mockito.when(vmInstanceVO.getInstanceName()).thenReturn("test1");
        Mockito.when(vmInstanceVO.getUuid()).thenReturn("1234");
        Mockito.when(virtualDisk.getBacking()).thenReturn(info);
        Mockito.when(info.getFileName()).thenReturn("[ae4e2064cdbf3587908f726a23f9a5a3] i-2-444-VM/6b10e0316c5e441dbaeb23a806679c8d.vmdk");
        Mockito.when(volumeDao.findByPath(Mockito.eq("6b10e0316c5e441dbaeb23a806679c8d"))).thenReturn(volumeVO);
        Mockito.when(vmInstanceVO.getId()).thenReturn(1L);
        Mockito.when(volumeService.detachVolumeFromVM(Mockito.any())).thenReturn(volumeVO);

        vMwareGuru.detachVolume(vmInstanceVO, virtualDisk, backupVO);
        Mockito.verify(volumeService, Mockito.times(1)).detachVolumeFromVM(Mockito.any());
    }

    @Test
    public void createVolumeInfoFromVolumesTestEmptyVolumeListReturnEmptyArray() {
        String volumeInfo = vMwareGuru.createVolumeInfoFromVolumes(new ArrayList<>());
        assertEquals("[]", volumeInfo);
    }

    @Test(expected = NullPointerException.class)
    public void createVolumeInfoFromVolumesTestNullVolume() {
        vMwareGuru.createVolumeInfoFromVolumes(null);
    }

    @Test
    public void createVolumeInfoFromVolumesTestCorrectlyConvertOfVolumes() {
        List<VolumeVO> volumesToTest = new ArrayList<>();

        VolumeVO root = new VolumeVO("test", 1l, 1l, 1l, 1l, 1l, "test", "/root/dir", ProvisioningType.THIN, 555l, Volume.Type.ROOT);
        String rootUuid = root.getUuid();

        VolumeVO data = new VolumeVO("test", 1l, 1l, 1l, 1l, 1l, "test", "/root/dir/data", ProvisioningType.THIN, 1111000l, Volume.Type.DATADISK);
        String dataUuid = data.getUuid();

        volumesToTest.add(root);
        volumesToTest.add(data);

        String result = vMwareGuru.createVolumeInfoFromVolumes(volumesToTest);
        String expected = String.format("[{\"uuid\":\"%s\",\"type\":\"ROOT\",\"size\":555,\"path\":\"/root/dir\"},{\"uuid\":\"%s\",\"type\":\"DATADISK\",\"size\":1111000,\"path\":\"/root/dir/data\"}]", rootUuid, dataUuid);

        assertEquals(expected, result);
    }

    @Test
    public void findRestoredVolumeTestNotFindRestoredVolume() throws Exception {
        VirtualMachineMO vmInstanceVO = Mockito.mock(VirtualMachineMO.class);
        Backup.VolumeInfo volumeInfo = Mockito.mock(Backup.VolumeInfo.class);
        Mockito.when(volumeInfo.getSize()).thenReturn(52l);
        Mockito.when(vmInstanceVO.getVirtualDisks()).thenReturn(new ArrayList<>());
        try {
            vMwareGuru.findRestoredVolume(volumeInfo, vmInstanceVO, null, 0);
        } catch (Exception e) {
            assertEquals("Volume to restore could not be found", e.getMessage());
        }
    }

    @Test(expected=CloudRuntimeException.class)
    public void testCloneHypervisorVM_NoExternalVM() throws Exception {
        String vCenterHost = "10.1.1.2";
        String datacenterName = "datacenter";
        String hostIp = "10.1.1.3";
        String vmName = "test-vm";
        Map<String, String> params = new HashMap<>();
        params.put(VmDetailConstants.VMWARE_VCENTER_HOST, vCenterHost);
        params.put(VmDetailConstants.VMWARE_DATACENTER_NAME, datacenterName);
        params.put(VmDetailConstants.VMWARE_VCENTER_USERNAME, "username");
        params.put(VmDetailConstants.VMWARE_VCENTER_PASSWORD, "password");

        ManagedObjectReference mor = Mockito.mock(ManagedObjectReference.class);
        ServiceContent serviceContent = Mockito.mock(ServiceContent.class);
        VimPortType vimPort = Mockito.mock(VimPortType.class);
        VmwareClient vimClient = spy(new VmwareClient(vCenterHost));
        VmwareContext vmwareContext = spy(new VmwareContext(vimClient, vCenterHost));
        Mockito.doReturn(vimClient).when(vmwareContext).getVimClient();
        Mockito.doReturn(mor).when(vmwareContext).getRootFolder();
        Mockito.doReturn(mor).when(vimClient).getDecendentMoRef(any(ManagedObjectReference.class), anyString(), anyString());
        DatacenterMO dataCenterMO = spy(new DatacenterMO(vmwareContext, datacenterName));

        try (MockedConstruction<VmwareClient> ignored1 = Mockito.mockConstruction(VmwareClient.class, withSettings().spiedInstance(vimClient), (mockVmwareClient, contextVmwareClient) -> {
            Mockito.doReturn(vimPort).when(mockVmwareClient).getService();
            Mockito.doReturn(serviceContent).when(mockVmwareClient).getServiceContent();
            Mockito.doNothing().when(mockVmwareClient).connect(anyString(), anyString(), anyString());
            Mockito.doReturn(mor).when(mockVmwareClient).getRootFolder();
        }); MockedConstruction<VmwareContext> ignored2 = Mockito.mockConstruction(VmwareContext.class, withSettings().spiedInstance(vmwareContext), (mockVmwareContext, contextVmwareContext) -> {
            Mockito.doReturn(vimClient).when(mockVmwareContext).getVimClient();
            Mockito.doReturn(mor).when(mockVmwareContext).getRootFolder();
        }); MockedConstruction<DatacenterMO> ignored3 = Mockito.mockConstruction(DatacenterMO.class, withSettings().spiedInstance(dataCenterMO), (mockDatacenterMO, contextDatacenterMO) -> {
            Mockito.doReturn(null).when(mockDatacenterMO).findVm(vmName);
        })) {
            vMwareGuru.getHypervisorVMOutOfBandAndCloneIfRequired(hostIp, vmName, params);
        }
    }

    @Test(expected=CloudRuntimeException.class)
    public void testCloneHypervisorVM_WindowsVMRunning() throws Exception {
        String vCenterHost = "10.1.1.2";
        String datacenterName = "datacenter";
        String hostIp = "10.1.1.3";
        String vmName = "test-vm";
        Map<String, String> params = new HashMap<>();
        params.put(VmDetailConstants.VMWARE_VCENTER_HOST, vCenterHost);
        params.put(VmDetailConstants.VMWARE_DATACENTER_NAME, datacenterName);
        params.put(VmDetailConstants.VMWARE_VCENTER_USERNAME, "username");
        params.put(VmDetailConstants.VMWARE_VCENTER_PASSWORD, "password");

        ManagedObjectReference mor = Mockito.mock(ManagedObjectReference.class);
        ServiceContent serviceContent = Mockito.mock(ServiceContent.class);
        VimPortType vimPort = Mockito.mock(VimPortType.class);
        VmwareClient vimClient = spy(new VmwareClient(vCenterHost));
        VmwareContext vmwareContext = spy(new VmwareContext(vimClient, vCenterHost));
        Mockito.doReturn(vimClient).when(vmwareContext).getVimClient();
        Mockito.doReturn(mor).when(vmwareContext).getRootFolder();
        Mockito.doReturn(mor).when(vimClient).getDecendentMoRef(any(ManagedObjectReference.class), anyString(), anyString());
        DatacenterMO dataCenterMO = spy(new DatacenterMO(vmwareContext, datacenterName));
        VirtualMachineMO vmMo = Mockito.mock(VirtualMachineMO.class);
        HostMO hostMo = Mockito.mock(HostMO.class);
        Mockito.doReturn(VirtualMachinePowerState.POWERED_ON).when(vmMo).getPowerState();
        Mockito.doReturn(hostMo).when(vmMo).getRunningHost();
        UnmanagedInstanceTO instance = Mockito.mock(UnmanagedInstanceTO.class);
        Mockito.doReturn("Windows 2019").when(instance).getOperatingSystem();
        when(VmwareHelper.getUnmanagedInstance(hostMo, vmMo)).thenReturn(instance);

        try (MockedConstruction<VmwareClient> ignored1 = Mockito.mockConstruction(VmwareClient.class, withSettings().spiedInstance(vimClient), (mockVmwareClient, contextVmwareClient) -> {
            Mockito.doReturn(vimPort).when(mockVmwareClient).getService();
            Mockito.doReturn(serviceContent).when(mockVmwareClient).getServiceContent();
            Mockito.doNothing().when(mockVmwareClient).connect(anyString(), anyString(), anyString());
            Mockito.doReturn(mor).when(mockVmwareClient).getRootFolder();
        }); MockedConstruction<VmwareContext> ignored2 = Mockito.mockConstruction(VmwareContext.class, withSettings().spiedInstance(vmwareContext), (mockVmwareContext, contextVmwareContext) -> {
            Mockito.doReturn(vimClient).when(mockVmwareContext).getVimClient();
            Mockito.doReturn(mor).when(mockVmwareContext).getRootFolder();
        }); MockedConstruction<DatacenterMO> ignored3 = Mockito.mockConstruction(DatacenterMO.class, withSettings().spiedInstance(dataCenterMO), (mockDatacenterMO, contextDatacenterMO) -> {
            Mockito.doReturn(vmMo).when(mockDatacenterMO).findVm(vmName);
        })) {
            vMwareGuru.getHypervisorVMOutOfBandAndCloneIfRequired(hostIp, vmName, params);
        }
    }

    @Test(expected=CloudRuntimeException.class)
    public void testCloneHypervisorVM_GetDatastoresFailed() throws Exception {
        String vCenterHost = "10.1.1.2";
        String datacenterName = "datacenter";
        String hostIp = "10.1.1.3";
        String vmName = "test-vm";
        Map<String, String> params = new HashMap<>();
        params.put(VmDetailConstants.VMWARE_VCENTER_HOST, vCenterHost);
        params.put(VmDetailConstants.VMWARE_DATACENTER_NAME, datacenterName);
        params.put(VmDetailConstants.VMWARE_VCENTER_USERNAME, "username");
        params.put(VmDetailConstants.VMWARE_VCENTER_PASSWORD, "password");

        ManagedObjectReference mor = Mockito.mock(ManagedObjectReference.class);
        ServiceContent serviceContent = Mockito.mock(ServiceContent.class);
        VimPortType vimPort = Mockito.mock(VimPortType.class);
        VmwareClient vimClient = spy(new VmwareClient(vCenterHost));
        VmwareContext vmwareContext = spy(new VmwareContext(vimClient, vCenterHost));
        Mockito.doReturn(vimClient).when(vmwareContext).getVimClient();
        Mockito.doReturn(mor).when(vmwareContext).getRootFolder();
        Mockito.doReturn(mor).when(vimClient).getDecendentMoRef(any(ManagedObjectReference.class), anyString(), anyString());
        DatacenterMO dataCenterMO = spy(new DatacenterMO(vmwareContext, datacenterName));
        VirtualMachineMO vmMo = Mockito.mock(VirtualMachineMO.class);
        HostMO hostMo = Mockito.mock(HostMO.class);
        Mockito.doReturn(VirtualMachinePowerState.POWERED_ON).when(vmMo).getPowerState();
        Mockito.doReturn(hostMo).when(vmMo).getRunningHost();
        UnmanagedInstanceTO instance = Mockito.mock(UnmanagedInstanceTO.class);
        Mockito.doReturn("CentOS").when(instance).getOperatingSystem();
        Mockito.doReturn("test-cluster").when(instance).getClusterName();
        when(VmwareHelper.getUnmanagedInstance(hostMo, vmMo)).thenReturn(instance);
        List<DatastoreMO> datastores = new ArrayList<>();
        Mockito.doReturn(datastores).when(vmMo).getAllDatastores();

        try (MockedConstruction<VmwareClient> ignored1 = Mockito.mockConstruction(VmwareClient.class, withSettings().spiedInstance(vimClient), (mockVmwareClient, contextVmwareClient) -> {
            Mockito.doReturn(vimPort).when(mockVmwareClient).getService();
            Mockito.doReturn(serviceContent).when(mockVmwareClient).getServiceContent();
            Mockito.doNothing().when(mockVmwareClient).connect(anyString(), anyString(), anyString());
            Mockito.doReturn(mor).when(mockVmwareClient).getRootFolder();
        }); MockedConstruction<VmwareContext> ignored2 = Mockito.mockConstruction(VmwareContext.class, withSettings().spiedInstance(vmwareContext), (mockVmwareContext, contextVmwareContext) -> {
            Mockito.doReturn(vimClient).when(mockVmwareContext).getVimClient();
            Mockito.doReturn(mor).when(mockVmwareContext).getRootFolder();
        }); MockedConstruction<DatacenterMO> ignored3 = Mockito.mockConstruction(DatacenterMO.class, withSettings().spiedInstance(dataCenterMO), (mockDatacenterMO, contextDatacenterMO) -> {
            Mockito.doReturn(vmMo).when(mockDatacenterMO).findVm(vmName);
        })) {
            vMwareGuru.getHypervisorVMOutOfBandAndCloneIfRequired(hostIp, vmName, params);
        }
    }

    @Test(expected=CloudRuntimeException.class)
    public void testCloneHypervisorVM_CloneVMFailed() throws Exception {
        String vCenterHost = "10.1.1.2";
        String datacenterName = "datacenter";
        String hostIp = "10.1.1.3";
        String vmName = "test-vm";
        Map<String, String> params = new HashMap<>();
        params.put(VmDetailConstants.VMWARE_VCENTER_HOST, vCenterHost);
        params.put(VmDetailConstants.VMWARE_DATACENTER_NAME, datacenterName);
        params.put(VmDetailConstants.VMWARE_VCENTER_USERNAME, "username");
        params.put(VmDetailConstants.VMWARE_VCENTER_PASSWORD, "password");

        ManagedObjectReference mor = Mockito.mock(ManagedObjectReference.class);
        ServiceContent serviceContent = Mockito.mock(ServiceContent.class);
        VimPortType vimPort = Mockito.mock(VimPortType.class);
        VmwareClient vimClient = spy(new VmwareClient(vCenterHost));
        VmwareContext vmwareContext = spy(new VmwareContext(vimClient, vCenterHost));
        Mockito.doReturn(vimClient).when(vmwareContext).getVimClient();
        Mockito.doReturn(mor).when(vmwareContext).getRootFolder();
        Mockito.doReturn(mor).when(vimClient).getDecendentMoRef(any(ManagedObjectReference.class), anyString(), anyString());
        DatacenterMO dataCenterMO = spy(new DatacenterMO(vmwareContext, datacenterName));
        VirtualMachineMO vmMo = Mockito.mock(VirtualMachineMO.class);
        HostMO hostMo = Mockito.mock(HostMO.class);
        Mockito.doReturn(VirtualMachinePowerState.POWERED_ON).when(vmMo).getPowerState();
        Mockito.doReturn(hostMo).when(vmMo).getRunningHost();
        Mockito.doReturn(mor).when(hostMo).getHyperHostOwnerResourcePool();
        UnmanagedInstanceTO instance = Mockito.mock(UnmanagedInstanceTO.class);
        Mockito.doReturn("CentOS").when(instance).getOperatingSystem();
        Mockito.doReturn("test-cluster").when(instance).getClusterName();
        when(VmwareHelper.getUnmanagedInstance(hostMo, vmMo)).thenReturn(instance);
        DatastoreMO datastoreMO = Mockito.mock(DatastoreMO.class);
        Mockito.doReturn(mor).when(datastoreMO).getMor();
        List<DatastoreMO> datastores = new ArrayList<>();
        datastores.add(datastoreMO);
        Mockito.doReturn(datastores).when(vmMo).getAllDatastores();
        Mockito.lenient().doReturn(false).when(vmMo).createFullClone(anyString(), any(ManagedObjectReference.class), any(ManagedObjectReference.class), any(ManagedObjectReference.class), any(Storage.ProvisioningType.class));

        try (MockedConstruction<VmwareClient> ignored1 = Mockito.mockConstruction(VmwareClient.class, withSettings().spiedInstance(vimClient), (mockVmwareClient, contextVmwareClient) -> {
            Mockito.doReturn(vimPort).when(mockVmwareClient).getService();
            Mockito.doReturn(serviceContent).when(mockVmwareClient).getServiceContent();
            Mockito.doNothing().when(mockVmwareClient).connect(anyString(), anyString(), anyString());
            Mockito.doReturn(mor).when(mockVmwareClient).getRootFolder();
        }); MockedConstruction<VmwareContext> ignored2 = Mockito.mockConstruction(VmwareContext.class, withSettings().spiedInstance(vmwareContext), (mockVmwareContext, contextVmwareContext) -> {
            Mockito.doReturn(vimClient).when(mockVmwareContext).getVimClient();
            Mockito.doReturn(mor).when(mockVmwareContext).getRootFolder();
        }); MockedConstruction<DatacenterMO> ignored3 = Mockito.mockConstruction(DatacenterMO.class, withSettings().spiedInstance(dataCenterMO), (mockDatacenterMO, contextDatacenterMO) -> {
            Mockito.doReturn(vmMo).when(mockDatacenterMO).findVm(anyString());
            Mockito.doReturn(mor).when(mockDatacenterMO).getVmFolder();
            Mockito.doReturn(mor).when(mockDatacenterMO).getMor();
        })) {
            vMwareGuru.getHypervisorVMOutOfBandAndCloneIfRequired(hostIp, vmName, params);
        }
    }

    @Test
    public void findRestoredVolumeTestFindRestoredVolume() throws Exception {
        Backup.VolumeInfo volumeInfo = Mockito.mock(Backup.VolumeInfo.class);
        VirtualMachineMO vmInstanceVO = Mockito.mock(VirtualMachineMO.class);
        VirtualDisk virtualDisk = Mockito.mock(VirtualDisk.class);
        VirtualDiskFlatVer2BackingInfo info = Mockito.mock(VirtualDiskFlatVer2BackingInfo.class);
        ArrayList<VirtualDisk> disks = new ArrayList<>();
        disks.add(virtualDisk);
        Mockito.when(volumeInfo.getSize()).thenReturn(52l);
        Mockito.when(virtualDisk.getCapacityInBytes()).thenReturn(52l);
        Mockito.when(info.getFileName()).thenReturn("test.vmdk");
        Mockito.when(virtualDisk.getBacking()).thenReturn(info);
        Mockito.when(virtualDisk.getUnitNumber()).thenReturn(1);
        Mockito.when(vmInstanceVO.getVirtualDisks()).thenReturn(disks);
        VirtualDisk findRestoredVolume = vMwareGuru.findRestoredVolume(volumeInfo, vmInstanceVO, "test", 1);
        assertNotNull(findRestoredVolume);
    }

    @Test
    public void getPoolIdTestDatastoreNameIsUuid() {
        VirtualDisk virtualDisk = Mockito.mock(VirtualDisk.class);
        VirtualDiskFlatVer2BackingInfo backingInfo = Mockito.mock(VirtualDiskFlatVer2BackingInfo.class);
        StoragePoolVO poolVO = Mockito.mock(StoragePoolVO.class);

        Mockito.when(poolVO.getId()).thenReturn(10L);
        Mockito.when(backingInfo.getFileName()).thenReturn("[5758578ed6a6454087a6c62f13df8ce2] test/test.vmdk");
        Mockito.when(virtualDisk.getBacking()).thenReturn(backingInfo);
        Mockito.when(_storagePoolDao.findByUuid("5758578e-d6a6-4540-87a6-c62f13df8ce2")).thenReturn(poolVO);

        Long result = vMwareGuru.getPoolId(virtualDisk, 1L, 2L);
        assertEquals(10l, (long)result);
    }

    @Test
    public void getPoolIdTestDatastoreNameIsNotUuid() {
        VirtualDisk virtualDisk = Mockito.mock(VirtualDisk.class);
        VirtualDiskFlatVer2BackingInfo backingInfo = Mockito.mock(VirtualDiskFlatVer2BackingInfo.class);
        StoragePoolVO poolVO = Mockito.mock(StoragePoolVO.class);

        Mockito.when(poolVO.getId()).thenReturn(11L);
        Mockito.when(backingInfo.getFileName()).thenReturn("[storage-name] test/test.vmdk");
        Mockito.when(virtualDisk.getBacking()).thenReturn(backingInfo);
        Mockito.when(_storagePoolDao.findPoolByName("storage-name", 1L, 2L)).thenReturn(poolVO);

        Long result = vMwareGuru.getPoolId(virtualDisk, 1L, 2L);
        assertEquals(11l, (long)result);
    }

    @Test
    public void getPoolIdFromDatastoreNameOrPathTestStorageDoesNotExist() {
        VirtualDisk virtualDisk = Mockito.mock(VirtualDisk.class);
        VirtualDiskFlatVer2BackingInfo backingInfo = Mockito.mock(VirtualDiskFlatVer2BackingInfo.class);

        Mockito.when(backingInfo.getFileName()).thenReturn("[storage-name] test/test.vmdk");
        Mockito.when(virtualDisk.getBacking()).thenReturn(backingInfo);
        Mockito.when(_storagePoolDao.findPoolByPathLike("storage-name", 1L, 2L)).thenReturn(null);

        try {
            vMwareGuru.getPoolId(virtualDisk, 1L, 2L);
            fail();
        } catch (Exception e) {
            assertEquals("Could not find storage pool with name or path [storage-name].", e.getMessage());
        }
    }


    @Test
    public void testCloneHypervisorVM() throws Exception {
        String vCenterHost = "10.1.1.2";
        String datacenterName = "datacenter";
        String hostIp = "10.1.1.3";
        String vmName = "test-vm";
        Map<String, String> params = new HashMap<>();
        params.put(VmDetailConstants.VMWARE_VCENTER_HOST, vCenterHost);
        params.put(VmDetailConstants.VMWARE_DATACENTER_NAME, datacenterName);
        params.put(VmDetailConstants.VMWARE_VCENTER_USERNAME, "username");
        params.put(VmDetailConstants.VMWARE_VCENTER_PASSWORD, "password");

        ManagedObjectReference mor = Mockito.mock(ManagedObjectReference.class);
        ServiceContent serviceContent = Mockito.mock(ServiceContent.class);
        VimPortType vimPort = Mockito.mock(VimPortType.class);
        VmwareClient vimClient = spy(new VmwareClient(vCenterHost));
        VmwareContext vmwareContext = spy(new VmwareContext(vimClient, vCenterHost));
        Mockito.doReturn(vimClient).when(vmwareContext).getVimClient();
        Mockito.doReturn(mor).when(vmwareContext).getRootFolder();
        Mockito.doReturn(mor).when(vimClient).getDecendentMoRef(any(ManagedObjectReference.class), anyString(), anyString());
        DatacenterMO dataCenterMO = spy(new DatacenterMO(vmwareContext, datacenterName));
        VirtualMachineMO vmMo = Mockito.mock(VirtualMachineMO.class);
        HostMO hostMo = Mockito.mock(HostMO.class);
        Mockito.doReturn(VirtualMachinePowerState.POWERED_ON).when(vmMo).getPowerState();
        Mockito.doReturn(hostMo).when(vmMo).getRunningHost();
        Mockito.doReturn(mor).when(hostMo).getHyperHostOwnerResourcePool();
        Mockito.doReturn(mor).when(hostMo).getMor();
        DatastoreMO datastoreMO = Mockito.mock(DatastoreMO.class);
        Mockito.doReturn(mor).when(datastoreMO).getMor();
        List<DatastoreMO> datastores = new ArrayList<>();
        datastores.add(datastoreMO);
        Mockito.doReturn(datastores).when(vmMo).getAllDatastores();
        Mockito.lenient().doReturn(true).when(vmMo).createFullClone(anyString(), any(ManagedObjectReference.class), any(ManagedObjectReference.class), any(ManagedObjectReference.class), any(Storage.ProvisioningType.class));
        UnmanagedInstanceTO instance = Mockito.mock(UnmanagedInstanceTO.class);
        Mockito.doReturn("CentOS").when(instance).getOperatingSystem();
        Mockito.doReturn("test-cluster").when(instance).getClusterName();
        when(VmwareHelper.getUnmanagedInstance(hostMo, vmMo)).thenReturn(instance);
        UnmanagedInstanceTO.Disk disk = Mockito.mock(UnmanagedInstanceTO.Disk.class);
        Mockito.doReturn("1").when(disk).getDiskId();
        List<UnmanagedInstanceTO.Disk> disks = new ArrayList<>();
        disks.add(disk);
        Mockito.doReturn(disks).when(instance).getDisks();

        try (MockedConstruction<VmwareClient> ignored1 = Mockito.mockConstruction(VmwareClient.class, withSettings().spiedInstance(vimClient), (mockVmwareClient, contextVmwareClient) -> {
            Mockito.doReturn(vimPort).when(mockVmwareClient).getService();
            Mockito.doReturn(serviceContent).when(mockVmwareClient).getServiceContent();
            Mockito.doNothing().when(mockVmwareClient).connect(anyString(), anyString(), anyString());
            Mockito.doReturn(mor).when(mockVmwareClient).getRootFolder();
        }); MockedConstruction<VmwareContext> ignored2 = Mockito.mockConstruction(VmwareContext.class, withSettings().spiedInstance(vmwareContext), (mockVmwareContext, contextVmwareContext) -> {
            Mockito.doReturn(vimClient).when(mockVmwareContext).getVimClient();
            Mockito.doReturn(mor).when(mockVmwareContext).getRootFolder();
        }); MockedConstruction<DatacenterMO> ignored3 = Mockito.mockConstruction(DatacenterMO.class, withSettings().spiedInstance(dataCenterMO), (mockDatacenterMO, contextDatacenterMO) -> {
            Mockito.doReturn(vmMo).when(mockDatacenterMO).findVm(anyString());
            Mockito.doReturn(mor).when(mockDatacenterMO).getVmFolder();
            Mockito.doReturn(mor).when(mockDatacenterMO).getMor();
        })) {
            Pair<UnmanagedInstanceTO, Boolean> clonedVm = vMwareGuru.getHypervisorVMOutOfBandAndCloneIfRequired(hostIp, vmName, params);
            assertNotNull(clonedVm);
        }
    }

    @Test(expected=CloudRuntimeException.class)
    public void testCreateVMTemplateFileOutOfBand_NoClonedVM() throws Exception {
        String vCenterHost = "10.1.1.2";
        String datacenterName = "datacenter";
        String hostIp = "10.1.1.3";
        String vmName = "cloned-test-vm";
        Map<String, String> params = new HashMap<>();
        params.put(VmDetailConstants.VMWARE_VCENTER_HOST, vCenterHost);
        params.put(VmDetailConstants.VMWARE_DATACENTER_NAME, datacenterName);
        params.put(VmDetailConstants.VMWARE_VCENTER_USERNAME, "username");
        params.put(VmDetailConstants.VMWARE_VCENTER_PASSWORD, "password");

        DataStoreTO dataStore = Mockito.mock(DataStoreTO.class);
        ManagedObjectReference mor = Mockito.mock(ManagedObjectReference.class);
        ServiceContent serviceContent = Mockito.mock(ServiceContent.class);
        VimPortType vimPort = Mockito.mock(VimPortType.class);
        VmwareClient vimClient = spy(new VmwareClient(vCenterHost));
        VmwareContext vmwareContext = spy(new VmwareContext(vimClient, vCenterHost));
        Mockito.doReturn(vimClient).when(vmwareContext).getVimClient();
        Mockito.doReturn(mor).when(vmwareContext).getRootFolder();
        Mockito.doReturn(mor).when(vimClient).getDecendentMoRef(any(ManagedObjectReference.class), anyString(), anyString());
        DatacenterMO dataCenterMO = spy(new DatacenterMO(vmwareContext, datacenterName));

        try (MockedConstruction<VmwareClient> ignored1 = Mockito.mockConstruction(VmwareClient.class, withSettings().spiedInstance(vimClient), (mockVmwareClient, contextVmwareClient) -> {
            Mockito.doReturn(vimPort).when(mockVmwareClient).getService();
            Mockito.doReturn(serviceContent).when(mockVmwareClient).getServiceContent();
            Mockito.doNothing().when(mockVmwareClient).connect(anyString(), anyString(), anyString());
            Mockito.doReturn(mor).when(mockVmwareClient).getRootFolder();
        }); MockedConstruction<VmwareContext> ignored2 = Mockito.mockConstruction(VmwareContext.class, withSettings().spiedInstance(vmwareContext), (mockVmwareContext, contextVmwareContext) -> {
            Mockito.doReturn(vimClient).when(mockVmwareContext).getVimClient();
            Mockito.doReturn(mor).when(mockVmwareContext).getRootFolder();
        }); MockedConstruction<DatacenterMO> ignored3 = Mockito.mockConstruction(DatacenterMO.class, withSettings().spiedInstance(dataCenterMO), (mockDatacenterMO, contextDatacenterMO) -> {
            Mockito.doReturn(null).when(mockDatacenterMO).findVm(vmName);
        })) {
            vMwareGuru.createVMTemplateOutOfBand(hostIp, vmName, params, dataStore, -1);
        }
    }

    @Test
    public void getPoolIdFromDatastoreNameOrPathTestStorageNameExist() {
        VirtualDisk virtualDisk = Mockito.mock(VirtualDisk.class);
        VirtualDiskFlatVer2BackingInfo backingInfo = Mockito.mock(VirtualDiskFlatVer2BackingInfo.class);
        StoragePoolVO poolVO = Mockito.mock(StoragePoolVO.class);

        Mockito.when(poolVO.getId()).thenReturn(13l);
        Mockito.when(_storagePoolDao.findPoolByName("storage-name", 1L, 2L)).thenReturn(poolVO);
        Mockito.when(backingInfo.getFileName()).thenReturn("[storage-name] test/test.vmdk");
        Mockito.when(virtualDisk.getBacking()).thenReturn(backingInfo);

        Long result = vMwareGuru.getPoolId(virtualDisk, 1L, 2L);
        assertEquals(13l, (long)result);
    }

    @Test
    public void getPoolIdFromDatastoreNameOrPathTestStoragePathExist() {
        VirtualDisk virtualDisk = Mockito.mock(VirtualDisk.class);
        VirtualDiskFlatVer2BackingInfo backingInfo = Mockito.mock(VirtualDiskFlatVer2BackingInfo.class);
        StoragePoolVO poolVO = Mockito.mock(StoragePoolVO.class);

        Mockito.when(poolVO.getId()).thenReturn(14l);
        Mockito.when(_storagePoolDao.findPoolByPathLike("storage-name", 1l, 2L)).thenReturn(poolVO);
        Mockito.when(backingInfo.getFileName()).thenReturn("[storage-name] test/test.vmdk");
        Mockito.when(virtualDisk.getBacking()).thenReturn(backingInfo);

        Long result = vMwareGuru.getPoolId(virtualDisk, 1L, 2L);
        assertEquals(14l, (long)result);
    }


    @Test
    public void testCreateVMTemplateFileOutOfBand() throws Exception {
        String vCenterHost = "10.1.1.2";
        String datacenterName = "datacenter";
        String hostIp = "10.1.1.3";
        String vmName = "cloned-test-vm";
        Map<String, String> params = new HashMap<>();
        params.put(VmDetailConstants.VMWARE_VCENTER_HOST, vCenterHost);
        params.put(VmDetailConstants.VMWARE_DATACENTER_NAME, datacenterName);
        params.put(VmDetailConstants.VMWARE_VCENTER_USERNAME, "username");
        params.put(VmDetailConstants.VMWARE_VCENTER_PASSWORD, "password");

        ManagedObjectReference mor = Mockito.mock(ManagedObjectReference.class);
        ServiceContent serviceContent = Mockito.mock(ServiceContent.class);
        VimPortType vimPort = Mockito.mock(VimPortType.class);
        VmwareClient vimClient = spy(new VmwareClient(vCenterHost));
        VmwareContext vmwareContext = spy(new VmwareContext(vimClient, vCenterHost));
        Mockito.doReturn(vimClient).when(vmwareContext).getVimClient();
        Mockito.doReturn(mor).when(vmwareContext).getRootFolder();
        Mockito.doReturn(mor).when(vimClient).getDecendentMoRef(any(ManagedObjectReference.class), anyString(), anyString());
        DatacenterMO dataCenterMO = spy(new DatacenterMO(vmwareContext, datacenterName));
        VirtualMachineMO vmMo = Mockito.mock(VirtualMachineMO.class);
        Mockito.doNothing().when(vmMo).exportVm(anyString(), anyString(), anyBoolean(), anyBoolean(), anyInt());
        NfsTO dataStore = Mockito.mock(NfsTO.class);
        Mockito.doReturn("nfs://10.1.1.4/testdir").when(dataStore).getUrl();

        try (MockedConstruction<VmwareClient> ignored1 = Mockito.mockConstruction(VmwareClient.class, withSettings().spiedInstance(vimClient), (mockVmwareClient, contextVmwareClient) -> {
            Mockito.doReturn(vimPort).when(mockVmwareClient).getService();
            Mockito.doReturn(serviceContent).when(mockVmwareClient).getServiceContent();
            Mockito.doNothing().when(mockVmwareClient).connect(anyString(), anyString(), anyString());
            Mockito.doReturn(mor).when(mockVmwareClient).getRootFolder();
        }); MockedConstruction<VmwareContext> ignored2 = Mockito.mockConstruction(VmwareContext.class, withSettings().spiedInstance(vmwareContext), (mockVmwareContext, contextVmwareContext) -> {
            Mockito.doReturn(vimClient).when(mockVmwareContext).getVimClient();
            Mockito.doReturn(mor).when(mockVmwareContext).getRootFolder();
        }); MockedConstruction<DatacenterMO> ignored3 = Mockito.mockConstruction(DatacenterMO.class, withSettings().spiedInstance(dataCenterMO), (mockDatacenterMO, contextDatacenterMO) -> {
            Mockito.doReturn(vmMo).when(mockDatacenterMO).findVm(vmName);
        })) {
            String vmTemplate = vMwareGuru.createVMTemplateOutOfBand(hostIp, vmName, params, dataStore, -1);
            assertNotNull(vmTemplate);
            assertTrue(UuidUtils.isUuid(vmTemplate));
        }
    }

    @Test
    public void testRemoveClonedHypervisorVM_NoClonedVM() throws Exception {
        String vCenterHost = "10.1.1.2";
        String datacenterName = "datacenter";
        String hostIp = "10.1.1.3";
        String vmName = "cloned-test-vm";
        Map<String, String> params = new HashMap<>();
        params.put(VmDetailConstants.VMWARE_VCENTER_HOST, vCenterHost);
        params.put(VmDetailConstants.VMWARE_DATACENTER_NAME, datacenterName);
        params.put(VmDetailConstants.VMWARE_VCENTER_USERNAME, "username");
        params.put(VmDetailConstants.VMWARE_VCENTER_PASSWORD, "password");

        ManagedObjectReference mor = Mockito.mock(ManagedObjectReference.class);
        ServiceContent serviceContent = Mockito.mock(ServiceContent.class);
        VimPortType vimPort = Mockito.mock(VimPortType.class);
        VmwareClient vimClient = spy(new VmwareClient(vCenterHost));
        VmwareContext vmwareContext = spy(new VmwareContext(vimClient, vCenterHost));
        Mockito.doReturn(vimClient).when(vmwareContext).getVimClient();
        Mockito.doReturn(mor).when(vmwareContext).getRootFolder();
        Mockito.doReturn(mor).when(vimClient).getDecendentMoRef(any(ManagedObjectReference.class), anyString(), anyString());
        DatacenterMO dataCenterMO = spy(new DatacenterMO(vmwareContext, datacenterName));

        try (MockedConstruction<VmwareClient> ignored1 = Mockito.mockConstruction(VmwareClient.class, withSettings().spiedInstance(vimClient), (mockVmwareClient, contextVmwareClient) -> {
            Mockito.doReturn(vimPort).when(mockVmwareClient).getService();
            Mockito.doReturn(serviceContent).when(mockVmwareClient).getServiceContent();
            Mockito.doNothing().when(mockVmwareClient).connect(anyString(), anyString(), anyString());
            Mockito.doReturn(mor).when(mockVmwareClient).getRootFolder();
        }); MockedConstruction<VmwareContext> ignored2 = Mockito.mockConstruction(VmwareContext.class, withSettings().spiedInstance(vmwareContext), (mockVmwareContext, contextVmwareContext) -> {
            Mockito.doReturn(vimClient).when(mockVmwareContext).getVimClient();
            Mockito.doReturn(mor).when(mockVmwareContext).getRootFolder();
        }); MockedConstruction<DatacenterMO> ignored3 = Mockito.mockConstruction(DatacenterMO.class, withSettings().spiedInstance(dataCenterMO), (mockDatacenterMO, contextDatacenterMO) -> {
            Mockito.doReturn(null).when(mockDatacenterMO).findVm(vmName);
        })) {
            boolean result = vMwareGuru.removeClonedHypervisorVMOutOfBand(hostIp, vmName, params);
            assertFalse(result);
        }
    }

    @Test
    public void testRemoveClonedHypervisorVM() throws Exception {
        String vCenterHost = "10.1.1.2";
        String datacenterName = "datacenter";
        String hostIp = "10.1.1.3";
        String vmName = "cloned-test-vm";
        Map<String, String> params = new HashMap<>();
        params.put(VmDetailConstants.VMWARE_VCENTER_HOST, vCenterHost);
        params.put(VmDetailConstants.VMWARE_DATACENTER_NAME, datacenterName);
        params.put(VmDetailConstants.VMWARE_VCENTER_USERNAME, "username");
        params.put(VmDetailConstants.VMWARE_VCENTER_PASSWORD, "password");

        ManagedObjectReference mor = Mockito.mock(ManagedObjectReference.class);
        ServiceContent serviceContent = Mockito.mock(ServiceContent.class);
        VimPortType vimPort = Mockito.mock(VimPortType.class);
        VmwareClient vimClient = spy(new VmwareClient(vCenterHost));
        VmwareContext vmwareContext = spy(new VmwareContext(vimClient, vCenterHost));
        Mockito.doReturn(vimClient).when(vmwareContext).getVimClient();
        Mockito.doReturn(mor).when(vmwareContext).getRootFolder();
        Mockito.doReturn(mor).when(vimClient).getDecendentMoRef(any(ManagedObjectReference.class), anyString(), anyString());
        DatacenterMO dataCenterMO = spy(new DatacenterMO(vmwareContext, datacenterName));
        VirtualMachineMO vmMo = Mockito.mock(VirtualMachineMO.class);
        Mockito.doReturn(true).when(vmMo).destroy();

        try (MockedConstruction<VmwareClient> ignored1 = Mockito.mockConstruction(VmwareClient.class, withSettings().spiedInstance(vimClient), (mockVmwareClient, contextVmwareClient) -> {
            Mockito.doReturn(vimPort).when(mockVmwareClient).getService();
            Mockito.doReturn(serviceContent).when(mockVmwareClient).getServiceContent();
            Mockito.doNothing().when(mockVmwareClient).connect(anyString(), anyString(), anyString());
            Mockito.doReturn(mor).when(mockVmwareClient).getRootFolder();
        }); MockedConstruction<VmwareContext> ignored2 = Mockito.mockConstruction(VmwareContext.class, withSettings().spiedInstance(vmwareContext), (mockVmwareContext, contextVmwareContext) -> {
            Mockito.doReturn(vimClient).when(mockVmwareContext).getVimClient();
            Mockito.doReturn(mor).when(mockVmwareContext).getRootFolder();
        }); MockedConstruction<DatacenterMO> ignored3 = Mockito.mockConstruction(DatacenterMO.class, withSettings().spiedInstance(dataCenterMO), (mockDatacenterMO, contextDatacenterMO) -> {
            Mockito.doReturn(vmMo).when(mockDatacenterMO).findVm(vmName);
        })) {
            boolean result = vMwareGuru.removeClonedHypervisorVMOutOfBand(hostIp, vmName, params);
            assertTrue(result);
        }
    }

    @Test
    public void testRemoveVMTemplateFileOutOfBand() throws Exception {
        NfsTO dataStore = Mockito.mock(NfsTO.class);
        Mockito.doReturn("nfs://10.1.1.4/testdir").when(dataStore).getUrl();
        String templateDir = "f887b7b3-3d1f-4a7d-93e5-3147f58866c6";
        boolean result = vMwareGuru.removeVMTemplateOutOfBand(dataStore, templateDir);
        assertTrue(result);
    }
}

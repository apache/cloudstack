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

import com.cloud.agent.api.Command;
import com.cloud.agent.api.MigrateVmToPoolCommand;
import com.cloud.dc.ClusterDetailsDao;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.storage.Storage.ProvisioningType;
import com.cloud.storage.StoragePool;
import com.cloud.storage.StoragePoolHostVO;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeApiService;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.StoragePoolHostDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.utils.Pair;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineManager;
import com.vmware.vim25.VirtualDisk;
import com.vmware.vim25.VirtualDiskFlatVer2BackingInfo;

import org.apache.cloudstack.backup.BackupVO;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Logger.class, VMwareGuru.class})
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
    Logger logger;

    @Mock
    VolumeDao volumeDao;

    @Mock
    VolumeApiService volumeService;


    @Before
    public void testSetUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        vMwareGuru.volumeService = volumeService;
        vMwareGuru._volumeDao = volumeDao;
        vMwareGuru.s_logger = logger;

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
        Mockito.when(vm.getId()).thenReturn(1L);
        Mockito.when(_storagePoolDao.findById(1L)).thenReturn(storagePoolVO);
        Mockito.when(rootVolume.getVolumeType()).thenReturn(Volume.Type.ROOT);
        Mockito.when(dataVolume.getVolumeType()).thenReturn(Volume.Type.DATADISK);
        Mockito.when(localStorage.isLocal()).thenReturn(true);
        Pair<Long, Long> clusterAndHost = new Pair<>(1L, 1L);

        Mockito.when(vmManager.findClusterAndHostIdForVm(1L)).thenReturn(clusterAndHost);

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
        PowerMockito.mockStatic(Logger.class);
        PowerMockito.when(Logger.getLogger(Mockito.eq(VMwareGuru.class))).thenReturn(logger);
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
        Mockito.verify(logger, Mockito.times(1)).warn("Failed to detach volume [uuid: 123] from VM [uuid: 1234, name: test1], during the backup restore process (as this volume does not exist in the metadata of backup [uuid: 321]).");
    }

    @Test
    public void detachVolumeTestWhenVolumeExistsAndDetachDontFail() {
        PowerMockito.mockStatic(Logger.class);
        PowerMockito.when(Logger.getLogger(Mockito.eq(VMwareGuru.class))).thenReturn(logger);
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
        Mockito.verify(logger, Mockito.times(1)).debug("Volume [uuid: 123] detached with success from VM [uuid: 1234, name: test1], during the backup restore process (as this volume does not exist in the metadata of backup [uuid: 321]).");
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
}

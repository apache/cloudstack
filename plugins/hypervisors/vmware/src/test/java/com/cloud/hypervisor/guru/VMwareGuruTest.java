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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import com.cloud.agent.api.Command;
import com.cloud.agent.api.MigrateVmToPoolCommand;
import com.cloud.dc.ClusterDetailsDao;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.storage.Storage.ProvisioningType;
import com.cloud.storage.StoragePool;
import com.cloud.storage.StoragePoolHostVO;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.StoragePoolHostDao;
import com.cloud.utils.Pair;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineManager;

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

    AutoCloseable closeable;

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

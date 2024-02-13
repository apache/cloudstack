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

import com.cloud.agent.api.StoragePoolInfo;
import com.cloud.exception.ConnectionException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.host.Host;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.dao.VMInstanceDao;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.commons.collections.MapUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(MockitoJUnitRunner.class)
public class StorageManagerImplTest {

    @Mock
    VolumeDao _volumeDao;

    @Mock
    VMInstanceDao vmInstanceDao;

    @Spy
    @InjectMocks
    private StorageManagerImpl storageManagerImpl;

    @Mock
    private StoragePoolVO storagePoolVOMock;

    @Mock
    private VolumeVO volume1VOMock;

    @Mock
    private VolumeVO volume2VOMock;

    @Mock
    private VMInstanceVO vmInstanceVOMock;

    @Test
    public void createLocalStoragePoolName() {
        String hostMockName = "host1";
        executeCreateLocalStoragePoolNameForHostName(hostMockName);
    }

    @Test
    public void createLocalStoragePoolNameUsingHostNameWithSpaces() {
        String hostMockName = "      hostNameWithSpaces      ";
        executeCreateLocalStoragePoolNameForHostName(hostMockName);
    }

    private void executeCreateLocalStoragePoolNameForHostName(String hostMockName) {
        String firstBlockUuid = "dsdsh665";

        String expectedLocalStorageName = hostMockName.trim() + "-local-" + firstBlockUuid;

        Host hostMock = Mockito.mock(Host.class);
        StoragePoolInfo storagePoolInfoMock = Mockito.mock(StoragePoolInfo.class);

        Mockito.when(hostMock.getName()).thenReturn(hostMockName);
        Mockito.when(storagePoolInfoMock.getUuid()).thenReturn(firstBlockUuid + "-213151-df21ef333d-2d33f1");

        String localStoragePoolName = storageManagerImpl.createLocalStoragePoolName(hostMock, storagePoolInfoMock);
        Assert.assertEquals(expectedLocalStorageName, localStoragePoolName);
    }

    private VolumeVO mockVolumeForIsVolumeSuspectedDestroyDuplicateTest() {
        VolumeVO volumeVO = new VolumeVO("data", 1L, 1L, 1L, 1L, 1L, "data", "data", Storage.ProvisioningType.THIN, 1, null, null, "data", Volume.Type.DATADISK);
        volumeVO.setPoolId(1L);
        return volumeVO;
    }

    @Test
    public void testIsVolumeSuspectedDestroyDuplicateNoPool() {
        VolumeVO volume = mockVolumeForIsVolumeSuspectedDestroyDuplicateTest();
        volume.setPoolId(null);
        Assert.assertFalse(storageManagerImpl.isVolumeSuspectedDestroyDuplicateOfVmVolume(volume));
    }

    @Test
    public void testIsVolumeSuspectedDestroyDuplicateNoPath() {
        VolumeVO volume = mockVolumeForIsVolumeSuspectedDestroyDuplicateTest();
        Assert.assertFalse(storageManagerImpl.isVolumeSuspectedDestroyDuplicateOfVmVolume(volume));
    }

    @Test
    public void testIsVolumeSuspectedDestroyDuplicateNoVmId() {
        VolumeVO volume = mockVolumeForIsVolumeSuspectedDestroyDuplicateTest();
        volume.setInstanceId(null);
        Assert.assertFalse(storageManagerImpl.isVolumeSuspectedDestroyDuplicateOfVmVolume(volume));
    }

    @Test
    public void testIsVolumeSuspectedDestroyDuplicateNoVm() {
        VolumeVO volume = mockVolumeForIsVolumeSuspectedDestroyDuplicateTest();
        Assert.assertFalse(storageManagerImpl.isVolumeSuspectedDestroyDuplicateOfVmVolume(volume));
    }

    @Test
    public void testIsVolumeSuspectedDestroyDuplicateNoVmVolumes() {
        VolumeVO volume = mockVolumeForIsVolumeSuspectedDestroyDuplicateTest();
        Mockito.when(vmInstanceDao.findById(1L)).thenReturn(Mockito.mock(VMInstanceVO.class));
        Mockito.when(_volumeDao.findUsableVolumesForInstance(1L)).thenReturn(new ArrayList<>());
        Assert.assertFalse(storageManagerImpl.isVolumeSuspectedDestroyDuplicateOfVmVolume(volume));
    }

    @Test
    public void testIsVolumeSuspectedDestroyDuplicateTrue() {
        Long poolId = 1L;
        String path = "data";
        VolumeVO volume = mockVolumeForIsVolumeSuspectedDestroyDuplicateTest();
        volume.setPoolId(poolId);
        Mockito.when(vmInstanceDao.findById(1L)).thenReturn(Mockito.mock(VMInstanceVO.class));
        VolumeVO volumeVO = Mockito.mock(VolumeVO.class);
        Mockito.when(volumeVO.getPoolId()).thenReturn(poolId);
        Mockito.when(volumeVO.getPath()).thenReturn(path);
        Mockito.when(_volumeDao.findUsableVolumesForInstance(1L)).thenReturn(List.of(volumeVO, Mockito.mock(VolumeVO.class)));
        Assert.assertTrue(storageManagerImpl.isVolumeSuspectedDestroyDuplicateOfVmVolume(volume));
    }

    @Test
    public void storagePoolCompatibleWithVolumePoolTestVolumeWithPoolIdInAllocatedState() {
        StoragePoolVO storagePool = new StoragePoolVO();
        storagePool.setPoolType(Storage.StoragePoolType.PowerFlex);
        storagePool.setId(1L);
        VolumeVO volume = new VolumeVO();
        volume.setState(Volume.State.Allocated);
        volume.setPoolId(1L);
        PrimaryDataStoreDao storagePoolDao = Mockito.mock(PrimaryDataStoreDao.class);
        storageManagerImpl._storagePoolDao = storagePoolDao;
        Mockito.doReturn(storagePool).when(storagePoolDao).findById(volume.getPoolId());
        Assert.assertFalse(storageManagerImpl.storagePoolCompatibleWithVolumePool(storagePool, volume));

    }

    @Test
    public void storagePoolCompatibleWithVolumePoolTestVolumeWithoutPoolIdInAllocatedState() {
        StoragePoolVO storagePool = new StoragePoolVO();
        storagePool.setPoolType(Storage.StoragePoolType.PowerFlex);
        storagePool.setId(1L);
        VolumeVO volume = new VolumeVO();
        volume.setState(Volume.State.Allocated);
        PrimaryDataStoreDao storagePoolDao = Mockito.mock(PrimaryDataStoreDao.class);
        storageManagerImpl._storagePoolDao = storagePoolDao;
        Assert.assertTrue(storageManagerImpl.storagePoolCompatibleWithVolumePool(storagePool, volume));

    }

    @Test
    public void testExtractUriParamsAsMapWithSolidFireUrl() {
        String sfUrl = "MVIP=1.2.3.4;SVIP=6.7.8.9;clusterAdminUsername=admin;" +
                "clusterAdminPassword=password;clusterDefaultMinIops=1000;" +
                "clusterDefaultMaxIops=2000;clusterDefaultBurstIopsPercentOfMaxIops=2";
        Map<String,String> uriParams = storageManagerImpl.extractUriParamsAsMap(sfUrl);
        Assert.assertTrue(MapUtils.isEmpty(uriParams));
    }

    @Test
    public void testExtractUriParamsAsMapWithNFSUrl() {
        String scheme = "nfs";
        String host = "HOST";
        String path = "/PATH";
        String sfUrl = String.format("%s://%s%s", scheme, host, path);
        Map<String,String> uriParams = storageManagerImpl.extractUriParamsAsMap(sfUrl);
        Assert.assertTrue(MapUtils.isNotEmpty(uriParams));
        Assert.assertEquals(scheme, uriParams.get("scheme"));
        Assert.assertEquals(host, uriParams.get("host"));
        Assert.assertEquals(path, uriParams.get("hostPath"));
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testCreateLocalStorageHostFailure() {
        Map<String, Object> test = new HashMap<>();
        test.put("host", null);
        try {
            storageManagerImpl.createLocalStorage(test);
        } catch (ConnectionException e) {
            throw new RuntimeException(e);
        }
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testCreateLocalStoragePathFailure() {
        Map<String, Object> test = new HashMap<>();
        test.put("host", "HOST");
        test.put("hostPath", "");
        try {
            storageManagerImpl.createLocalStorage(test);
        } catch (ConnectionException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void getStoragePoolNonDestroyedVolumesLogTestNonDestroyedVolumesReturnLog() {
        Mockito.doReturn(1L).when(storagePoolVOMock).getId();
        Mockito.doReturn(1L).when(volume1VOMock).getInstanceId();
        Mockito.doReturn("786633d1-a942-4374-9d56-322dd4b0d202").when(volume1VOMock).getUuid();
        Mockito.doReturn(1L).when(volume2VOMock).getInstanceId();
        Mockito.doReturn("ffb46333-e983-4c21-b5f0-51c5877a3805").when(volume2VOMock).getUuid();
        Mockito.doReturn("58760044-928f-4c4e-9fef-d0e48423595e").when(vmInstanceVOMock).getUuid();

        Mockito.when(_volumeDao.findByPoolId(storagePoolVOMock.getId(), null)).thenReturn(List.of(volume1VOMock, volume2VOMock));
        Mockito.doReturn(vmInstanceVOMock).when(vmInstanceDao).findById(Mockito.anyLong());

        String log = storageManagerImpl.getStoragePoolNonDestroyedVolumesLog(storagePoolVOMock.getId());
        String expected = String.format("[Volume [%s] (attached to VM [%s]), Volume [%s] (attached to VM [%s])]", volume1VOMock.getUuid(), vmInstanceVOMock.getUuid(), volume2VOMock.getUuid(), vmInstanceVOMock.getUuid());

        Assert.assertEquals(expected, log);
    }
}

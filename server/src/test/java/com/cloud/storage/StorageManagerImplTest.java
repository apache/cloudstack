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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.cloud.dc.HostPodVO;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.resource.ResourceManager;
import com.cloud.storage.dao.StoragePoolAndAccessGroupMapDao;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.command.admin.storage.ChangeStoragePoolScopeCmd;
import org.apache.cloudstack.api.command.admin.storage.ConfigureStorageAccessCmd;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreDriver;
import org.apache.cloudstack.framework.config.ConfigDepot;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.resourcedetail.dao.DiskOfferingDetailsDao;
import org.apache.cloudstack.storage.command.CheckDataStoreStoragePolicyComplainceCommand;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolDetailVO;
import org.apache.cloudstack.storage.datastore.db.StoragePoolDetailsDao;
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
import org.mockito.stubbing.Answer;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.StoragePoolInfo;
import com.cloud.capacity.CapacityManager;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.VsphereStoragePolicyVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.VsphereStoragePolicyDao;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.ConnectionException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.StorageUnavailableException;
import com.cloud.host.Host;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.hypervisor.HypervisorGuruManager;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.user.AccountManagerImpl;
import com.cloud.utils.Pair;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.DiskProfile;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.dao.VMInstanceDao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;

@RunWith(MockitoJUnitRunner.class)
public class StorageManagerImplTest {

    @Mock
    VolumeDao _volumeDao;

    @Mock
    VMInstanceDao vmInstanceDao;
    @Mock
    PrimaryDataStoreDao storagePoolDao;
    @Mock
    CapacityManager capacityManager;
    @Mock
    DiskOfferingDetailsDao diskOfferingDetailsDao;
    @Mock
    VsphereStoragePolicyDao vsphereStoragePolicyDao;
    @Mock
    HypervisorGuruManager hvGuruMgr;
    @Mock
    AgentManager agentManager;
    @Mock
    ConfigDepot configDepot;
    @Mock
    ConfigurationDao configurationDao;
    @Mock
    DataCenterDao dataCenterDao;
    @Mock
    AccountManagerImpl accountMgr;
    @Mock
    StoragePoolDetailsDao storagePoolDetailsDao;

    @Mock
    ClusterDao clusterDao;

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

    @Mock
    private HostDao hostDao;
    @Mock
    private HostPodDao podDao;

    @Mock
    private StoragePoolAndAccessGroupMapDao storagePoolAccessGroupMapDao;

    @Mock
    private ResourceManager resourceMgr;


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
        assertEquals(expectedLocalStorageName, localStoragePoolName);
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
        assertTrue(storageManagerImpl.isVolumeSuspectedDestroyDuplicateOfVmVolume(volume));
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
        assertTrue(storageManagerImpl.storagePoolCompatibleWithVolumePool(storagePool, volume));

    }

    @Test
    public void testExtractUriParamsAsMapWithSolidFireUrl() {
        String sfUrl = "MVIP=1.2.3.4;SVIP=6.7.8.9;clusterAdminUsername=admin;" +
                "clusterAdminPassword=password;clusterDefaultMinIops=1000;" +
                "clusterDefaultMaxIops=2000;clusterDefaultBurstIopsPercentOfMaxIops=2";
        Map<String,String> uriParams = storageManagerImpl.extractUriParamsAsMap(sfUrl);
        assertTrue(MapUtils.isEmpty(uriParams));
    }

    @Test
    public void testExtractUriParamsAsMapWithNFSUrl() {
        String scheme = "nfs";
        String host = "HOST";
        String path = "/PATH";
        String sfUrl = String.format("%s://%s%s", scheme, host, path);
        Map<String,String> uriParams = storageManagerImpl.extractUriParamsAsMap(sfUrl);
        assertTrue(MapUtils.isNotEmpty(uriParams));
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
    public void testStoragePoolHasEnoughIopsNullPoolIops() {
        StoragePool pool = Mockito.mock(StoragePool.class);
        Mockito.when(pool.getCapacityIops()).thenReturn(null);
        List<Pair<Volume, DiskProfile>> list = List.of(new Pair<>(Mockito.mock(Volume.class), Mockito.mock(DiskProfile.class)));
        assertTrue(storageManagerImpl.storagePoolHasEnoughIops(100L, list, pool, false));
    }

    @Test
    public void testStoragePoolHasEnoughIopsSuccess() {
        StoragePoolVO pool = Mockito.mock(StoragePoolVO.class);
        Mockito.when(pool.getId()).thenReturn(1L);
        Mockito.when(pool.getCapacityIops()).thenReturn(1000L);
        Mockito.when(storagePoolDao.findById(1L)).thenReturn(pool);
        Mockito.when(capacityManager.getUsedIops(pool)).thenReturn(500L);
        List<Pair<Volume, DiskProfile>> list = List.of(new Pair<>(Mockito.mock(Volume.class), Mockito.mock(DiskProfile.class)));
        assertTrue(storageManagerImpl.storagePoolHasEnoughIops(100L, list, pool, true));
    }

    @Test
    public void testStoragePoolHasEnoughIopsNegative() {
        StoragePoolVO pool = Mockito.mock(StoragePoolVO.class);
        Mockito.when(pool.getId()).thenReturn(1L);
        Mockito.when(pool.getCapacityIops()).thenReturn(550L);
        Mockito.when(storagePoolDao.findById(1L)).thenReturn(pool);
        Mockito.when(capacityManager.getUsedIops(pool)).thenReturn(500L);
        List<Pair<Volume, DiskProfile>> list = List.of(new Pair<>(Mockito.mock(Volume.class), Mockito.mock(DiskProfile.class)));
        Assert.assertFalse(storageManagerImpl.storagePoolHasEnoughIops(100L, list, pool, true));
    }

    @Test
    public void testStoragePoolHasEnoughIopsNullPool() {
        Assert.assertFalse(storageManagerImpl.storagePoolHasEnoughIops(100L, null));
    }

    @Test
    public void testStoragePoolHasEnoughIopsNullRequestedIops() {
        StoragePoolVO pool = Mockito.mock(StoragePoolVO.class);
        List<Long> iopsList = Arrays.asList(null, 0L);
        for (Long iops : iopsList) {
            assertTrue(storageManagerImpl.storagePoolHasEnoughIops(iops, pool));
        }
    }

    @Test
    public void testStoragePoolHasEnoughIopsSuccess1() {
        StoragePoolVO pool = Mockito.mock(StoragePoolVO.class);
        Mockito.doReturn(true).when(storageManagerImpl).storagePoolHasEnoughIops(
                Mockito.eq(100L), Mockito.anyList(), Mockito.eq(pool), Mockito.eq(false));
        assertTrue(storageManagerImpl.storagePoolHasEnoughIops(100L, pool));
    }

    @Test
    public void testStoragePoolHasEnoughIopsNoVolumesOrPool() {
        List<Pair<Volume, DiskProfile>> list = new ArrayList<>();
        StoragePoolVO pool = Mockito.mock(StoragePoolVO.class);
        Assert.assertFalse(storageManagerImpl.storagePoolHasEnoughIops(list, pool));
        list = List.of(new Pair<>(Mockito.mock(Volume.class), Mockito.mock(DiskProfile.class)));
        Assert.assertFalse(storageManagerImpl.storagePoolHasEnoughIops(list, null));
    }

    @Test
    public void testStoragePoolHasEnoughIopsWithVolPoolNullIops() {
        List<Pair<Volume, DiskProfile>> list = List.of(
                new Pair<>(Mockito.mock(Volume.class), Mockito.mock(DiskProfile.class)));
        StoragePoolVO pool = Mockito.mock(StoragePoolVO.class);
        Mockito.when(pool.getCapacityIops()).thenReturn(null);
        assertTrue(storageManagerImpl.storagePoolHasEnoughIops(list, pool));
    }

    @Test
    public void testStoragePoolHasEnoughIopsWithVolPoolCompare() {
        Volume volume = Mockito.mock(Volume.class);
        Mockito.when(volume.getDiskOfferingId()).thenReturn(1L);
        Mockito.when(volume.getMinIops()).thenReturn(100L);
        DiskProfile profile = Mockito.mock(DiskProfile.class);
        Mockito.when(profile.getDiskOfferingId()).thenReturn(1L);
        List<Pair<Volume, DiskProfile>> list = List.of(new Pair<>(volume, profile));
        StoragePoolVO pool = Mockito.mock(StoragePoolVO.class);
        Mockito.doReturn(true).when(storageManagerImpl)
                .storagePoolHasEnoughIops(100L, list, pool, true);
        assertTrue(storageManagerImpl.storagePoolHasEnoughIops(list, pool));

        Mockito.when(profile.getDiskOfferingId()).thenReturn(2L);
        Mockito.when(profile.getMinIops()).thenReturn(200L);
        Mockito.doReturn(false).when(storageManagerImpl)
                .storagePoolHasEnoughIops(200L, list, pool, true);
        Assert.assertFalse(storageManagerImpl.storagePoolHasEnoughIops(list, pool));
    }

    @Test
    public void testStoragePoolHasEnoughSpaceNullSize() {
        StoragePoolVO pool = Mockito.mock(StoragePoolVO.class);
        List<Long> sizeList = Arrays.asList(null, 0L);
        for (Long size : sizeList) {
            assertTrue(storageManagerImpl.storagePoolHasEnoughSpace(size, pool));
        }
    }

    @Test
    public void testStoragePoolHasEnoughSpaceCompare() {
        StoragePoolVO pool = Mockito.mock(StoragePoolVO.class);
        Mockito.when(pool.getId()).thenReturn(1L);
        Mockito.when(storagePoolDao.findById(1L)).thenReturn(pool);
        Mockito.when(capacityManager.getAllocatedPoolCapacity(pool, null)).thenReturn(2000L);
        Mockito.doAnswer((Answer<Boolean>) invocationOnMock -> {
            long total = invocationOnMock.getArgument(1);
            long asking = invocationOnMock.getArgument(2);
            return total > asking;
        }).when(storageManagerImpl).checkPoolforSpace(Mockito.any(StoragePool.class),
                Mockito.anyLong(), Mockito.anyLong());
        assertTrue(storageManagerImpl.storagePoolHasEnoughSpace(1000L, pool));
        Assert.assertFalse(storageManagerImpl.storagePoolHasEnoughSpace(2200L, pool));
    }

    @Test
    public void testIsStoragePoolCompliantWithStoragePolicy() {
        StoragePoolVO pool = Mockito.mock(StoragePoolVO.class);
        Mockito.when(diskOfferingDetailsDao.getDetail(1L, ApiConstants.STORAGE_POLICY))
                .thenReturn("policy");
        try {
            Mockito.doReturn(null)
                    .when(storageManagerImpl).getCheckDatastorePolicyComplianceAnswer("policy", pool);
            assertTrue(storageManagerImpl.isStoragePoolCompliantWithStoragePolicy(1L, pool));
        } catch (StorageUnavailableException e) {
            Assert.fail(e.getMessage());
        }
        try {
            Mockito.doReturn(new com.cloud.agent.api.Answer(
                    Mockito.mock(CheckDataStoreStoragePolicyComplainceCommand.class)))
                    .when(storageManagerImpl).getCheckDatastorePolicyComplianceAnswer("policy", pool);
            assertTrue(storageManagerImpl.isStoragePoolCompliantWithStoragePolicy(1L, pool));
        } catch (StorageUnavailableException e) {
            Assert.fail(e.getMessage());
        }
        try {
            com.cloud.agent.api.Answer answer =
                    new com.cloud.agent.api.Answer(Mockito.mock(CheckDataStoreStoragePolicyComplainceCommand.class),
                            false, "");
            Mockito.doReturn(answer)
                    .when(storageManagerImpl).getCheckDatastorePolicyComplianceAnswer("policy", pool);
            Assert.assertFalse(storageManagerImpl.isStoragePoolCompliantWithStoragePolicy(1L, pool));
        } catch (StorageUnavailableException e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testGetCheckDatastorePolicyComplianceAnswerNullAnswer() {
        StoragePoolVO pool = Mockito.mock(StoragePoolVO.class);
        try {
            Assert.assertNull(storageManagerImpl.getCheckDatastorePolicyComplianceAnswer(null, pool));
            Assert.assertNull(storageManagerImpl.getCheckDatastorePolicyComplianceAnswer("", pool));
        } catch (StorageUnavailableException e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test(expected = StorageUnavailableException.class)
    public void testGetCheckDatastorePolicyComplianceAnswerNoHost() throws StorageUnavailableException {
        StoragePoolVO pool = Mockito.mock(StoragePoolVO.class);
        Mockito.when(pool.getId()).thenReturn(1L);
        Mockito.when(vsphereStoragePolicyDao.findById(Mockito.anyLong()))
                .thenReturn(Mockito.mock(VsphereStoragePolicyVO.class));
        Mockito.doReturn(new ArrayList<>()).when(storageManagerImpl).getUpHostsInPool(Mockito.anyLong());
        storageManagerImpl.getCheckDatastorePolicyComplianceAnswer("1", pool);
    }

    @Test(expected = StorageUnavailableException.class)
    public void testGetCheckDatastorePolicyComplianceAnswerAgentException() throws StorageUnavailableException {
        StoragePoolVO pool = Mockito.mock(StoragePoolVO.class);
        Mockito.when(pool.getId()).thenReturn(1L);
        VsphereStoragePolicyVO policy = Mockito.mock(VsphereStoragePolicyVO.class);
        Mockito.when(policy.getPolicyId()).thenReturn("some");
        Mockito.when(vsphereStoragePolicyDao.findById(Mockito.anyLong()))
                .thenReturn(policy);
        Mockito.doReturn(new ArrayList<>(List.of(1L, 2L)))
                .when(storageManagerImpl).getUpHostsInPool(Mockito.anyLong());
        Mockito.when(hvGuruMgr.getGuruProcessedCommandTargetHost(Mockito.anyLong(),
                Mockito.any(CheckDataStoreStoragePolicyComplainceCommand.class))).thenReturn(1L);
        try {
            Mockito.when(agentManager.send(Mockito.anyLong(), Mockito.any(Command.class)))
                    .thenThrow(AgentUnavailableException.class);
        } catch (AgentUnavailableException | OperationTimedoutException e) {
            Assert.fail(e.getMessage());
        }
        storageManagerImpl.getCheckDatastorePolicyComplianceAnswer("1", pool);
        try {
            Mockito.when(agentManager.send(Mockito.anyLong(), Mockito.any(Command.class)))
                    .thenThrow(OperationTimedoutException.class);
        } catch (AgentUnavailableException | OperationTimedoutException e) {
            Assert.fail(e.getMessage());
        }
        storageManagerImpl.getCheckDatastorePolicyComplianceAnswer("1", pool);
    }

    @Test
    public void testGetCheckDatastorePolicyComplianceAnswerSuccess() throws StorageUnavailableException {
        StoragePoolVO pool = Mockito.mock(StoragePoolVO.class);
        Mockito.when(pool.getId()).thenReturn(1L);
        VsphereStoragePolicyVO policy = Mockito.mock(VsphereStoragePolicyVO.class);
        Mockito.when(policy.getPolicyId()).thenReturn("some");
        Mockito.when(vsphereStoragePolicyDao.findById(Mockito.anyLong()))
                .thenReturn(policy);
        Mockito.doReturn(new ArrayList<>(List.of(1L, 2L))).when(storageManagerImpl).getUpHostsInPool(Mockito.anyLong());
        Mockito.when(hvGuruMgr.getGuruProcessedCommandTargetHost(Mockito.anyLong(),
                Mockito.any(CheckDataStoreStoragePolicyComplainceCommand.class))).thenReturn(1L);
        try {
            Mockito.when(agentManager.send(Mockito.anyLong(),
                            Mockito.any(CheckDataStoreStoragePolicyComplainceCommand.class)))
                    .thenReturn(new com.cloud.agent.api.Answer(
                            Mockito.mock(CheckDataStoreStoragePolicyComplainceCommand.class)));
        } catch (AgentUnavailableException | OperationTimedoutException e) {
            Assert.fail(e.getMessage());
        }
        com.cloud.agent.api.Answer answer =
                storageManagerImpl.getCheckDatastorePolicyComplianceAnswer("1", pool);
        assertTrue(answer.getResult());
    }

    @Test
    public void testEnableDefaultDatastoreDownloadRedirectionForExistingInstallationsNoChange() {
        Mockito.when(configDepot.isNewConfig(StorageManager.DataStoreDownloadFollowRedirects))
                .thenReturn(false);
        storageManagerImpl.enableDefaultDatastoreDownloadRedirectionForExistingInstallations();
        Mockito.verify(configurationDao, Mockito.never()).update(Mockito.anyString(), Mockito.anyString());
    }

    @Test
    public void testEnableDefaultDatastoreDownloadRedirectionForExistingInstallationsOldInstall() {
        Mockito.when(configDepot.isNewConfig(StorageManager.DataStoreDownloadFollowRedirects))
                .thenReturn(true);
        Mockito.when(dataCenterDao.listAll(Mockito.any()))
                .thenReturn(List.of(Mockito.mock(DataCenterVO.class)));
        Mockito.doReturn(true).when(configurationDao).update(Mockito.anyString(), Mockito.anyString());
        storageManagerImpl.enableDefaultDatastoreDownloadRedirectionForExistingInstallations();
        Mockito.verify(configurationDao, Mockito.times(1))
                .update(StorageManager.DataStoreDownloadFollowRedirects.key(), "true");
    }

    @Test
    public void testEnableDefaultDatastoreDownloadRedirectionForExistingInstallationsNewInstall() {
        Mockito.when(configDepot.isNewConfig(StorageManager.DataStoreDownloadFollowRedirects))
                .thenReturn(true);
        Mockito.when(dataCenterDao.listAll(Mockito.any()))
                .thenReturn(new ArrayList<>()); //new installation
        storageManagerImpl.enableDefaultDatastoreDownloadRedirectionForExistingInstallations();
        Mockito.verify(configurationDao, Mockito.never())
                .update(StorageManager.DataStoreDownloadFollowRedirects.key(),StorageManager.DataStoreDownloadFollowRedirects.defaultValue());
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

    private ChangeStoragePoolScopeCmd mockChangeStoragePooolScopeCmd(String newScope) {
        ChangeStoragePoolScopeCmd cmd = new ChangeStoragePoolScopeCmd();
        ReflectionTestUtils.setField(cmd, "id", 1L);
        ReflectionTestUtils.setField(cmd, "clusterId", 1L);
        ReflectionTestUtils.setField(cmd, "scope", newScope);
        return cmd;
    }

    private StoragePoolVO mockStoragePoolVOForChangeStoragePoolScope(ScopeType currentScope, StoragePoolStatus status) {
        StoragePoolVO primaryStorage = new StoragePoolVO();
        primaryStorage.setId(1L);
        primaryStorage.setDataCenterId(1L);
        primaryStorage.setClusterId(1L);
        primaryStorage.setStatus(StoragePoolStatus.Disabled);
        primaryStorage.setScope(currentScope);
        primaryStorage.setStatus(status);
        return primaryStorage;
    }

    private void prepareTestChangeStoragePoolScope(ScopeType currentScope, StoragePoolStatus status) {
        final DataCenterVO zone = new DataCenterVO(1L, null, null, null, null, null, null, null, null, null, DataCenter.NetworkType.Advanced, null, null);
        StoragePoolVO primaryStorage = mockStoragePoolVOForChangeStoragePoolScope(currentScope, status);

        Mockito.when(accountMgr.isRootAdmin(Mockito.any())).thenReturn(true);
        Mockito.when(dataCenterDao.findById(1L)).thenReturn(zone);
        Mockito.when(storagePoolDao.findById(1L)).thenReturn(primaryStorage);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testChangeStoragePoolScopeNotDisabledException() {
        prepareTestChangeStoragePoolScope(ScopeType.CLUSTER, StoragePoolStatus.Initialized);

        ChangeStoragePoolScopeCmd cmd = mockChangeStoragePooolScopeCmd("ZONE");
        storageManagerImpl.changeStoragePoolScope(cmd);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testChangeStoragePoolScopeToZoneHypervisorNotSupported() {
        prepareTestChangeStoragePoolScope(ScopeType.CLUSTER, StoragePoolStatus.Disabled);

        final ClusterVO cluster = new ClusterVO();
        cluster.setHypervisorType(String.valueOf(HypervisorType.XenServer));
        Mockito.when(clusterDao.findById(1L)).thenReturn(cluster);

        ChangeStoragePoolScopeCmd cmd = mockChangeStoragePooolScopeCmd("ZONE");
        storageManagerImpl.changeStoragePoolScope(cmd);
    }

    @Test(expected = CloudRuntimeException.class)
    public void testChangeStoragePoolScopeToClusterVolumesPresentException() {
        prepareTestChangeStoragePoolScope(ScopeType.ZONE, StoragePoolStatus.Disabled);

        final ClusterVO cluster = new ClusterVO();
        Mockito.when(clusterDao.findById(1L)).thenReturn(cluster);

        VMInstanceVO instance = Mockito.mock(VMInstanceVO.class);
        Pair<List<VMInstanceVO>, Integer> vms = new Pair<>(List.of(instance), 1);
        Mockito.when(vmInstanceDao.listByVmsNotInClusterUsingPool(1L, 1L)).thenReturn(vms);

        ChangeStoragePoolScopeCmd cmd = mockChangeStoragePooolScopeCmd("CLUSTER");
        storageManagerImpl.changeStoragePoolScope(cmd);
    }

    @Test
    public void testCheckNFSMountOptionsForCreateNoNFSMountOptions() {
        Map<String, String> details = new HashMap<>();
        try {
            storageManagerImpl.checkNFSMountOptionsForCreate(details, HypervisorType.XenServer, "");
        } catch (Exception e) {
            Assert.fail();
        }
    }

    @Test
    public void testCheckNFSMountOptionsForCreateNotKVM() {
        Map<String, String> details = new HashMap<>();
        details.put(ApiConstants.NFS_MOUNT_OPTIONS, "vers=4.1");
        InvalidParameterValueException exception = assertThrows(InvalidParameterValueException.class,
                () -> storageManagerImpl.checkNFSMountOptionsForCreate(details, HypervisorType.XenServer, ""));
        Assert.assertEquals(exception.getMessage(), "NFS options can not be set for the hypervisor type " + HypervisorType.XenServer);
    }

    @Test
    public void testCheckNFSMountOptionsForCreateNotNFS() {
        Map<String, String> details = new HashMap<>();
        details.put(ApiConstants.NFS_MOUNT_OPTIONS, "vers=4.1");
        InvalidParameterValueException exception = assertThrows(InvalidParameterValueException.class,
                () -> storageManagerImpl.checkNFSMountOptionsForCreate(details, HypervisorType.KVM, ""));
        Assert.assertEquals(exception.getMessage(), "NFS options can only be set on pool type " + Storage.StoragePoolType.NetworkFilesystem);
    }

    @Test
    public void testCheckNFSMountOptionsForUpdateNoNFSMountOptions() {
        Map<String, String> details = new HashMap<>();
        StoragePoolVO pool = new StoragePoolVO();
        Long accountId = 1L;
        try {
            storageManagerImpl.checkNFSMountOptionsForUpdate(details, pool, accountId);
        } catch (Exception e) {
            Assert.fail();
        }
    }

    @Test
    public void testCheckNFSMountOptionsForUpdateNotRootAdmin() {
        Map<String, String> details = new HashMap<>();
        StoragePoolVO pool = new StoragePoolVO();
        Long accountId = 1L;
        details.put(ApiConstants.NFS_MOUNT_OPTIONS, "vers=4.1");
        Mockito.when(accountMgr.isRootAdmin(accountId)).thenReturn(false);
        PermissionDeniedException exception = assertThrows(PermissionDeniedException.class,
                () -> storageManagerImpl.checkNFSMountOptionsForUpdate(details, pool, accountId));
        Assert.assertEquals(exception.getMessage(), "Only root admin can modify nfs options");
    }

    @Test
    public void testCheckNFSMountOptionsForUpdateNotKVM() {
        Map<String, String> details = new HashMap<>();
        StoragePoolVO pool = new StoragePoolVO();
        Long accountId = 1L;
        details.put(ApiConstants.NFS_MOUNT_OPTIONS, "vers=4.1");
        Mockito.when(accountMgr.isRootAdmin(accountId)).thenReturn(true);
        pool.setHypervisor(HypervisorType.XenServer);
        InvalidParameterValueException exception = assertThrows(InvalidParameterValueException.class,
                () -> storageManagerImpl.checkNFSMountOptionsForUpdate(details, pool, accountId));
        Assert.assertEquals(exception.getMessage(), "NFS options can only be set for the hypervisor type " + HypervisorType.KVM);
    }

    @Test
    public void testCheckNFSMountOptionsForUpdateNotNFS() {
        Map<String, String> details = new HashMap<>();
        StoragePoolVO pool = new StoragePoolVO();
        Long accountId = 1L;
        details.put(ApiConstants.NFS_MOUNT_OPTIONS, "vers=4.1");
        Mockito.when(accountMgr.isRootAdmin(accountId)).thenReturn(true);
        pool.setHypervisor(HypervisorType.KVM);
        pool.setPoolType(Storage.StoragePoolType.FiberChannel);
        InvalidParameterValueException exception = assertThrows(InvalidParameterValueException.class,
                () -> storageManagerImpl.checkNFSMountOptionsForUpdate(details, pool, accountId));
        Assert.assertEquals(exception.getMessage(), "NFS options can only be set on pool type " + Storage.StoragePoolType.NetworkFilesystem);
    }

    @Test
    public void testCheckNFSMountOptionsForUpdateNotMaintenance() {
        Map<String, String> details = new HashMap<>();
        StoragePoolVO pool = new StoragePoolVO();
        Long accountId = 1L;
        details.put(ApiConstants.NFS_MOUNT_OPTIONS, "vers=4.1");
        Mockito.when(accountMgr.isRootAdmin(accountId)).thenReturn(true);
        pool.setHypervisor(HypervisorType.KVM);
        pool.setPoolType(Storage.StoragePoolType.NetworkFilesystem);
        pool.setStatus(StoragePoolStatus.Up);
        InvalidParameterValueException exception = assertThrows(InvalidParameterValueException.class,
                () -> storageManagerImpl.checkNFSMountOptionsForUpdate(details, pool, accountId));
        Assert.assertEquals(exception.getMessage(), "The storage pool should be in maintenance mode to edit nfs options");
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testDuplicateNFSMountOptions() {
        String nfsMountOpts = "vers=4.1, nconnect=4,vers=4.2";
        Map<String, String> details = new HashMap<>();
        details.put(ApiConstants.NFS_MOUNT_OPTIONS, nfsMountOpts);
        storageManagerImpl.checkNFSMountOptionsForCreate(details, HypervisorType.KVM, "nfs");
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testInvalidNFSMountOptions() {
        String nfsMountOpts = "vers=4.1=2,";
        Map<String, String> details = new HashMap<>();
        details.put(ApiConstants.NFS_MOUNT_OPTIONS, nfsMountOpts);
        StoragePoolVO pool = new StoragePoolVO();
        pool.setHypervisor(HypervisorType.KVM);
        pool.setPoolType(Storage.StoragePoolType.NetworkFilesystem);
        pool.setStatus(StoragePoolStatus.Maintenance);
        Long accountId = 1L;
        Mockito.when(accountMgr.isRootAdmin(accountId)).thenReturn(true);
        storageManagerImpl.checkNFSMountOptionsForUpdate(details, pool, accountId);
    }

    @Test
    public void testGetStoragePoolMountOptionsNotNFS() {
        StoragePoolVO pool = new StoragePoolVO();

        pool.setPoolType(Storage.StoragePoolType.FiberChannel);
        Pair<Map<String, String>, Boolean> details = storageManagerImpl.getStoragePoolNFSMountOpts(pool, null);
        Assert.assertEquals(details.second(), false);
        Assert.assertEquals(details.first(), null);
    }

    @Test
    public void testGetStoragePoolMountOptions() {
        Long poolId = 1L;
        String key = "nfsmountopts";
        String value = "vers=4.1,nconnect=2";
        StoragePoolDetailVO nfsMountOpts = new StoragePoolDetailVO(poolId, key, value, true);
        StoragePoolVO pool = new StoragePoolVO();
        pool.setId(poolId);
        pool.setPoolType(Storage.StoragePoolType.NetworkFilesystem);
        Mockito.when(storagePoolDetailsDao.findDetail(poolId, ApiConstants.NFS_MOUNT_OPTIONS)).thenReturn(nfsMountOpts);

        Pair<Map<String, String>, Boolean> details = storageManagerImpl.getStoragePoolNFSMountOpts(pool, null);
        Assert.assertEquals(details.second(), true);
        Assert.assertEquals(details.first().get(key), value);
    }

    @Test
    public void testGetStoragePoolMountFailureReason() {
        String error = "Mount failed on kvm host. An incorrect mount option was specified.\nIncorrect mount option.";
        String failureReason = storageManagerImpl.getStoragePoolMountFailureReason(error);
        Assert.assertEquals(failureReason, "An incorrect mount option was specified");
    }

    private void overrideDefaultConfigValue(final ConfigKey configKey, final String name, final Object o) throws IllegalAccessException, NoSuchFieldException {
        Field f = ConfigKey.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(configKey, o);
    }

    private Long testCheckPoolforSpaceForResizeSetup(StoragePoolVO pool, Long allocatedSizeWithTemplate) {
        Long poolId = 10L;
        Long zoneId = 2L;

        Long capacityBytes = (long) (allocatedSizeWithTemplate / Double.valueOf(CapacityManager.StorageAllocatedCapacityDisableThreshold.defaultValue())
                        / Double.valueOf(CapacityManager.StorageOverprovisioningFactor.defaultValue()));
        Long maxAllocatedSizeForResize = (long) (capacityBytes * Double.valueOf(CapacityManager.StorageOverprovisioningFactor.defaultValue())
                * Double.valueOf(CapacityManager.StorageAllocatedCapacityDisableThresholdForVolumeSize.defaultValue()));

        System.out.println("maxAllocatedSizeForResize = " + maxAllocatedSizeForResize);
        System.out.println("allocatedSizeWithTemplate = " + allocatedSizeWithTemplate);

        Mockito.when(pool.getId()).thenReturn(poolId);
        Mockito.when(pool.getCapacityBytes()).thenReturn(capacityBytes);
        Mockito.when(storagePoolDao.findById(poolId)).thenReturn(pool);
        Mockito.when(pool.getPoolType()).thenReturn(Storage.StoragePoolType.NetworkFilesystem);

        return maxAllocatedSizeForResize - allocatedSizeWithTemplate;
    }

    @Test
    public void testCheckPoolforSpaceForResize1() {
        StoragePoolVO pool = Mockito.mock(StoragePoolVO.class);
        Long allocatedSizeWithTemplate = 100L * 1024 * 1024 * 1024;

        Long maxAskingSize = testCheckPoolforSpaceForResizeSetup(pool, allocatedSizeWithTemplate);
        Long totalAskingSize = maxAskingSize / 2;

        boolean result = storageManagerImpl.checkPoolforSpace(pool, allocatedSizeWithTemplate, totalAskingSize, false);
        Assert.assertFalse(result);
    }

    @Test
    public void testCheckPoolforSpaceForResize2() {
        StoragePoolVO pool = Mockito.mock(StoragePoolVO.class);
        Long allocatedSizeWithTemplate = 100L * 1024 * 1024 * 1024;

        Long maxAskingSize = testCheckPoolforSpaceForResizeSetup(pool, allocatedSizeWithTemplate);
        Long totalAskingSize = maxAskingSize / 2;

        boolean result = storageManagerImpl.checkPoolforSpace(pool, allocatedSizeWithTemplate, totalAskingSize, true);
        Assert.assertFalse(result);
    }

    @Test
    public void testCheckPoolforSpaceForResize3() throws NoSuchFieldException, IllegalAccessException {
        StoragePoolVO pool = Mockito.mock(StoragePoolVO.class);
        Long allocatedSizeWithTemplate = 100L * 1024 * 1024 * 1024;

        Long maxAskingSize = testCheckPoolforSpaceForResizeSetup(pool, allocatedSizeWithTemplate);
        Long totalAskingSize = maxAskingSize + 1;
        overrideDefaultConfigValue(StorageManagerImpl.AllowVolumeReSizeBeyondAllocation, "_defaultValue", "true");

        boolean result = storageManagerImpl.checkPoolforSpace(pool, allocatedSizeWithTemplate, totalAskingSize, true);
        Assert.assertFalse(result);
    }

    @Test
    public void testCheckPoolforSpaceForResize4() throws NoSuchFieldException, IllegalAccessException {
        StoragePoolVO pool = Mockito.mock(StoragePoolVO.class);
        Long allocatedSizeWithTemplate = 100L * 1024 * 1024 * 1024;

        Long maxAskingSize = testCheckPoolforSpaceForResizeSetup(pool, allocatedSizeWithTemplate);
        Long totalAskingSize = maxAskingSize / 2;
        overrideDefaultConfigValue(StorageManagerImpl.AllowVolumeReSizeBeyondAllocation, "_defaultValue", "true");

        boolean result = storageManagerImpl.checkPoolforSpace(pool, allocatedSizeWithTemplate, totalAskingSize, true);
        assertTrue(result);
    }

    @Test
    public void testGetStorageAccessGroupsOnHostAllSAGsPresent() {
        long hostId = 1L;

        HostVO host = Mockito.mock(HostVO.class);
        ClusterVO cluster = Mockito.mock(ClusterVO.class);
        HostPodVO pod = Mockito.mock(HostPodVO.class);
        DataCenterVO zone = Mockito.mock(DataCenterVO.class);

        Mockito.when(hostDao.findById(hostId)).thenReturn(host);
        Mockito.when(host.getClusterId()).thenReturn(2L);
        Mockito.when(clusterDao.findById(2L)).thenReturn(cluster);
        Mockito.when(cluster.getPodId()).thenReturn(3L);
        Mockito.when(podDao.findById(3L)).thenReturn(pod);
        Mockito.when(pod.getDataCenterId()).thenReturn(4L);
        Mockito.when(dataCenterDao.findById(4L)).thenReturn(zone);

        Mockito.when(host.getStorageAccessGroups()).thenReturn("sag1");
        Mockito.when(cluster.getStorageAccessGroups()).thenReturn("sag2");
        Mockito.when(pod.getStorageAccessGroups()).thenReturn("sag3");
        Mockito.when(zone.getStorageAccessGroups()).thenReturn("sag4");

        String[] sags = storageManagerImpl.getStorageAccessGroups(null, null, null, hostId);

        assertNotNull(sags);
        assertEquals(4, sags.length);
        assertEquals("sag1", sags[0]);
        assertEquals("sag2", sags[1]);
        assertEquals("sag3", sags[2]);
        assertEquals("sag4", sags[3]);
    }

    @Test
    public void testGetSingleStorageAccessGroupOnHost() {
        long hostId = 1L;

        HostVO host = Mockito.mock(HostVO.class);
        ClusterVO cluster = Mockito.mock(ClusterVO.class);
        HostPodVO pod = Mockito.mock(HostPodVO.class);
        DataCenterVO zone = Mockito.mock(DataCenterVO.class);

        Mockito.when(hostDao.findById(hostId)).thenReturn(host);
        Mockito.when(host.getClusterId()).thenReturn(2L);
        Mockito.when(clusterDao.findById(2L)).thenReturn(cluster);
        Mockito.when(cluster.getPodId()).thenReturn(3L);
        Mockito.when(podDao.findById(3L)).thenReturn(pod);
        Mockito.when(pod.getDataCenterId()).thenReturn(4L);
        Mockito.when(dataCenterDao.findById(4L)).thenReturn(zone);

        Mockito.when(host.getStorageAccessGroups()).thenReturn("");
        Mockito.when(cluster.getStorageAccessGroups()).thenReturn("sag2");
        Mockito.when(pod.getStorageAccessGroups()).thenReturn(null);

        String[] sags = storageManagerImpl.getStorageAccessGroups(null, null, null, hostId);

        assertNotNull(sags);
        assertEquals(1, sags.length);
        assertEquals("sag2", sags[0]);
    }

    @Test
    public void testGetStoragePoolIopsStats_ReturnsDriverResultWhenNotNull() {
        StoragePool pool = Mockito.mock(StoragePool.class);
        PrimaryDataStoreDriver primaryStoreDriver = Mockito.mock(PrimaryDataStoreDriver.class);
        Pair<Long, Long> expectedResult = new Pair<>(1000L, 500L);
        Mockito.when(primaryStoreDriver.getStorageIopsStats(pool)).thenReturn(expectedResult);

        Pair<Long, Long> result = storageManagerImpl.getStoragePoolIopsStats(primaryStoreDriver, pool);

        Assert.assertSame("Should return the result from primaryStoreDriver.getStorageIopsStats", expectedResult, result);
        Mockito.verify(primaryStoreDriver, Mockito.never()).getUsedIops(Mockito.any());
        Mockito.verify(pool, Mockito.never()).getCapacityIops();
    }

    @Test
    public void testGetStoragePoolIopsStats_UsedIopsPositive() {
        StoragePool pool = Mockito.mock(StoragePool.class);
        PrimaryDataStoreDriver primaryStoreDriver = Mockito.mock(PrimaryDataStoreDriver.class);
        Mockito.when(primaryStoreDriver.getStorageIopsStats(pool)).thenReturn(null);
        Mockito.when(primaryStoreDriver.getUsedIops(pool)).thenReturn(500L);
        Mockito.when(pool.getCapacityIops()).thenReturn(1000L);

        Pair<Long, Long> result = storageManagerImpl.getStoragePoolIopsStats(primaryStoreDriver, pool);

        Assert.assertNotNull(result);
        Assert.assertEquals("Capacity IOPS should match pool's capacity IOPS", 1000L, result.first().longValue());
        Assert.assertEquals("Used IOPS should match the positive value returned", 500L, result.second().longValue());
    }

    @Test
    public void testGetStoragePoolIopsStats_UsedIopsZero() {
        StoragePool pool = Mockito.mock(StoragePool.class);
        PrimaryDataStoreDriver primaryStoreDriver = Mockito.mock(PrimaryDataStoreDriver.class);
        Mockito.when(primaryStoreDriver.getStorageIopsStats(pool)).thenReturn(null);
        Mockito.when(primaryStoreDriver.getUsedIops(pool)).thenReturn(0L);
        Mockito.when(pool.getCapacityIops()).thenReturn(1000L);

        Pair<Long, Long> result = storageManagerImpl.getStoragePoolIopsStats(primaryStoreDriver, pool);

        Assert.assertNotNull(result);
        Assert.assertEquals("Capacity IOPS should match pool's capacity IOPS", 1000L, result.first().longValue());
        Assert.assertNull("Used IOPS should be null when usedIops <= 0", result.second());
    }

    @Test
    public void testGetStoragePoolIopsStats_UsedIopsNegative() {
        StoragePool pool = Mockito.mock(StoragePool.class);
        PrimaryDataStoreDriver primaryStoreDriver = Mockito.mock(PrimaryDataStoreDriver.class);
        Mockito.when(primaryStoreDriver.getStorageIopsStats(pool)).thenReturn(null);
        Mockito.when(primaryStoreDriver.getUsedIops(pool)).thenReturn(-100L);
        Mockito.when(pool.getCapacityIops()).thenReturn(1000L);

        Pair<Long, Long> result = storageManagerImpl.getStoragePoolIopsStats(primaryStoreDriver, pool);

        Assert.assertNotNull(result);
        Assert.assertEquals("Capacity IOPS should match pool's capacity IOPS", 1000L, result.first().longValue());
        Assert.assertNull("Used IOPS should be null when usedIops <= 0", result.second());
    }


    @Test
    public void testNoStorageAccessGroupsOnHostAndStoragePool() {
        HostVO host = Mockito.mock(HostVO.class);
        StoragePoolVO storagePool = Mockito.mock(StoragePoolVO.class);
        long hostId = 1L;
        long poolId = 2L;

        Mockito.when(host.getId()).thenReturn(hostId);
        doReturn(new String[0]).when(storageManagerImpl).getStorageAccessGroups(null, null, null, hostId);

        Mockito.when(storagePool.getId()).thenReturn(poolId);
        storageManagerImpl._storagePoolAccessGroupMapDao = storagePoolAccessGroupMapDao;
        Mockito.when(storagePoolAccessGroupMapDao.getStorageAccessGroups(poolId))
                .thenReturn(new ArrayList<>());

        boolean result = storageManagerImpl.checkIfHostAndStoragePoolHasCommonStorageAccessGroups(host, storagePool);

        assertTrue("Host without storage access groups should connect to a storage pool without storage access groups.", result);
    }

    @Test
    public void testHostWithStorageAccessGroupsAndStoragePoolWithoutStorageAccessGroups() {
        HostVO host = Mockito.mock(HostVO.class);
        StoragePoolVO storagePool = Mockito.mock(StoragePoolVO.class);
        long hostId = 1L;
        long poolId = 2L;

        Mockito.when(host.getId()).thenReturn(hostId);
        doReturn(new String[]{"StorageAccessGroup1"}).when(storageManagerImpl).getStorageAccessGroups(null, null, null, hostId);

        Mockito.when(storagePool.getId()).thenReturn(poolId);
        storageManagerImpl._storagePoolAccessGroupMapDao = storagePoolAccessGroupMapDao;
        Mockito.when(storagePoolAccessGroupMapDao.getStorageAccessGroups(poolId))
                .thenReturn(new ArrayList<>());

        boolean result = storageManagerImpl.checkIfHostAndStoragePoolHasCommonStorageAccessGroups(host, storagePool);

        assertTrue("Host with storage access groups should connect to a storage pool without storage access groups.", result);
    }

    @Test
    public void testHostWithStorageAccessGroupsAndStoragePoolWithDifferentStorageAccessGroups() {
        HostVO host = Mockito.mock(HostVO.class);
        StoragePoolVO storagePool = Mockito.mock(StoragePoolVO.class);
        long hostId = 1L;
        long poolId = 2L;

        Mockito.when(host.getId()).thenReturn(hostId);
        doReturn(new String[]{"StorageAccessGroup1"}).when(storageManagerImpl).getStorageAccessGroups(null, null, null, hostId);

        Mockito.when(storagePool.getId()).thenReturn(poolId);
        storageManagerImpl._storagePoolAccessGroupMapDao = storagePoolAccessGroupMapDao;
        Mockito.when(storagePoolAccessGroupMapDao.getStorageAccessGroups(poolId))
                .thenReturn(Arrays.asList("StorageAccessGroup2", "StorageAccessGroup3"));

        boolean result = storageManagerImpl.checkIfHostAndStoragePoolHasCommonStorageAccessGroups(host, storagePool);

        Assert.assertFalse("Host with storage access groups should not connect to a storage pool with different storage access groups.", result);
    }

    @Test
    public void testHostWithStorageAccessGroupsAndStoragePoolWithMatchingStorageAccessGroups() {
        HostVO host = Mockito.mock(HostVO.class);
        StoragePoolVO storagePool = Mockito.mock(StoragePoolVO.class);
        long hostId = 1L;
        long poolId = 2L;

        Mockito.when(host.getId()).thenReturn(hostId);
        doReturn(new String[]{"StorageAccessGroup1"}).when(storageManagerImpl).getStorageAccessGroups(null, null, null, hostId);

        Mockito.when(storagePool.getId()).thenReturn(poolId);
        storageManagerImpl._storagePoolAccessGroupMapDao = storagePoolAccessGroupMapDao;
        Mockito.when(storagePoolAccessGroupMapDao.getStorageAccessGroups(poolId))
                .thenReturn(Arrays.asList("StorageAccessGroup1", "StorageAccessGroup2"));

        boolean result = storageManagerImpl.checkIfHostAndStoragePoolHasCommonStorageAccessGroups(host, storagePool);

        assertTrue("Host with matching storage access groups should connect to a storage pool with matching storage access groups.", result);
    }

    @Test
    public void testHostWithEmptySAGsOnHost() {
        HostVO host = Mockito.mock(HostVO.class);
        StoragePoolVO storagePool = Mockito.mock(StoragePoolVO.class);
        long hostId = 1L;
        long poolId = 2L;

        Mockito.when(host.getId()).thenReturn(hostId);
        doReturn(new String[0]).when(storageManagerImpl).getStorageAccessGroups(null, null, null, hostId);

        Mockito.when(storagePool.getId()).thenReturn(poolId);
        storageManagerImpl._storagePoolAccessGroupMapDao = storagePoolAccessGroupMapDao;
        Mockito.when(storagePoolAccessGroupMapDao.getStorageAccessGroups(poolId))
                .thenReturn(Arrays.asList("StorageAccessGroup1", "StorageAccessGroup2"));

        boolean result = storageManagerImpl.checkIfHostAndStoragePoolHasCommonStorageAccessGroups(host, storagePool);

        Assert.assertFalse("Host with matching storage access groups should connect to a storage pool with matching storage access groups.", result);
    }

    @Test
    public void testVolumeReadyNoVMOrVMStoppedAndPoolsWithMatchingStorageAccessGroups() {
        StoragePoolVO destPool = Mockito.mock(StoragePoolVO.class);
        Volume volume = Mockito.mock(Volume.class);
        long srcPoolId = 2L;
        long destPoolId = 3L;

        Mockito.when(volume.getState()).thenReturn(Volume.State.Ready);
        Mockito.when(volume.getInstanceId()).thenReturn(null);
        Mockito.when(volume.getPoolId()).thenReturn(srcPoolId);

        Mockito.when(destPool.getId()).thenReturn(destPoolId);

        List<String> srcStorageAccessGroups = Arrays.asList("StorageAccessGroup1", "StorageAccessGroup2");
        List<String> destStorageAccessGroups = Arrays.asList("StorageAccessGroup1", "StorageAccessGroup2");

        doReturn(srcStorageAccessGroups).when(storagePoolAccessGroupMapDao).getStorageAccessGroups(srcPoolId);
        doReturn(destStorageAccessGroups).when(storagePoolAccessGroupMapDao).getStorageAccessGroups(destPoolId);

        Pair<Boolean, String> result = storageManagerImpl.checkIfReadyVolumeFitsInStoragePoolWithStorageAccessGroups(destPool, volume);

        assertTrue("Volume in Ready state and no VM or VM stopped should migrate if both pools have matching storage access groups.", result.first());
    }

    @Test
    public void testVolumeReadyNoVMOrVMStoppedAndPoolsWithEmptyStorageAccessGroups() {
        StoragePoolVO destPool = Mockito.mock(StoragePoolVO.class);
        Volume volume = Mockito.mock(Volume.class);
        long srcPoolId = 2L;
        long destPoolId = 3L;

        Mockito.when(volume.getState()).thenReturn(Volume.State.Ready);
        Mockito.when(volume.getInstanceId()).thenReturn(null);
        Mockito.when(volume.getPoolId()).thenReturn(srcPoolId);

        Mockito.when(destPool.getId()).thenReturn(destPoolId);

        List<String> srcStorageAccessGroups = new ArrayList<>();
        List<String> destStorageAccessGroups = new ArrayList<>();

        doReturn(srcStorageAccessGroups).when(storagePoolAccessGroupMapDao).getStorageAccessGroups(srcPoolId);
        doReturn(destStorageAccessGroups).when(storagePoolAccessGroupMapDao).getStorageAccessGroups(destPoolId);

        Pair<Boolean, String> result = storageManagerImpl.checkIfReadyVolumeFitsInStoragePoolWithStorageAccessGroups(destPool, volume);

        assertTrue("Volume with empty storage access groups should be able to fit in the destination pool.", result.first());
    }

    @Test
    public void testVolumeReadyVMRunningAndHostHasCommonSAGsForBothPools() {
        StoragePoolVO destPool = Mockito.mock(StoragePoolVO.class);
        Volume volume = Mockito.mock(Volume.class);
        long vmId = 10L;
        long srcPoolId = 2L;
        long destPoolId = 3L;

        Mockito.when(volume.getState()).thenReturn(Volume.State.Ready);
        Mockito.when(volume.getInstanceId()).thenReturn(vmId);
        Mockito.when(volume.getPoolId()).thenReturn(srcPoolId);

        Mockito.when(destPool.getId()).thenReturn(destPoolId);

        List<String> srcStorageAccessGroups = Arrays.asList("StorageAccessGroup1", "StorageAccessGroup2");
        List<String> destStorageAccessGroups = Arrays.asList("StorageAccessGroup2", "StorageAccessGroup3");

        doReturn(srcStorageAccessGroups).when(storagePoolAccessGroupMapDao).getStorageAccessGroups(srcPoolId);
        doReturn(destStorageAccessGroups).when(storagePoolAccessGroupMapDao).getStorageAccessGroups(destPoolId);

        Pair<Boolean, String> result = storageManagerImpl.checkIfReadyVolumeFitsInStoragePoolWithStorageAccessGroups(destPool, volume);

        assertTrue("Volume with host having common storage access groups should fit in both source and destination pools.", result.first());
    }

    @Test
    public void testVolumeReadyVMRunningAndHostHasCommonSAGForSourcePoolButNotDestinationPool() {
        StoragePoolVO destPool = Mockito.mock(StoragePoolVO.class);
        Volume volume = Mockito.mock(Volume.class);
        StoragePoolVO srcPool = Mockito.mock(StoragePoolVO.class);
        long vmId = 10L;
        long srcPoolId = 2L;
        long destPoolId = 3L;

        Mockito.when(volume.getState()).thenReturn(Volume.State.Ready);
        Mockito.when(volume.getInstanceId()).thenReturn(vmId);
        Mockito.when(volume.getPoolId()).thenReturn(srcPoolId);

        Mockito.when(destPool.getId()).thenReturn(destPoolId);
        Mockito.when(srcPool.getId()).thenReturn(destPoolId);
        Mockito.doReturn(srcPool).when(storagePoolDao).findById(srcPoolId);

        List<String> srcStorageAccessGroups = Arrays.asList("StorageAccessGroup1", "StorageAccessGroup2");
        List<String> destStorageAccessGroups = Arrays.asList("StorageAccessGroup3", "StorageAccessGroup4");

        doReturn(srcStorageAccessGroups).when(storagePoolAccessGroupMapDao).getStorageAccessGroups(srcPoolId);
        doReturn(destStorageAccessGroups).when(storagePoolAccessGroupMapDao).getStorageAccessGroups(destPoolId);
        List<Long> poolIds = new ArrayList<>();
        poolIds.add(srcPool.getId());
        poolIds.add(destPool.getId());
        Mockito.doReturn(null).when(storageManagerImpl).findUpAndEnabledHostWithAccessToStoragePools(poolIds);

        Pair<Boolean, String> result = storageManagerImpl.checkIfReadyVolumeFitsInStoragePoolWithStorageAccessGroups(destPool, volume);

        Assert.assertFalse("Volume with host having common storage access group for source pool but not destination pool should not fit.", result.first());
    }

    @Test
    public void testNoCommonHostConnected() {
        StoragePoolVO destPool = Mockito.mock(StoragePoolVO.class);
        StoragePoolVO srcPool = Mockito.mock(StoragePoolVO.class);
        Volume volume = Mockito.mock(Volume.class);
        long vmId = 10L;
        long srcPoolId = 2L;
        long destPoolId = 3L;

        Mockito.when(volume.getState()).thenReturn(Volume.State.Ready);
        Mockito.when(volume.getInstanceId()).thenReturn(vmId);
        Mockito.when(volume.getPoolId()).thenReturn(srcPoolId);

        Mockito.when(destPool.getId()).thenReturn(destPoolId);
        Mockito.when(srcPool.getId()).thenReturn(destPoolId);
        Mockito.doReturn(srcPool).when(storagePoolDao).findById(srcPoolId);
        List<String> srcStorageAccessGroups = Arrays.asList("StorageAccessGroup3", "StorageAccessGroup4");
        List<String> destStorageAccessGroups = Arrays.asList("StorageAccessGroup1", "StorageAccessGroup2");

        Mockito.doReturn(srcStorageAccessGroups).when(storagePoolAccessGroupMapDao).getStorageAccessGroups(srcPoolId);
        Mockito.doReturn(destStorageAccessGroups).when(storagePoolAccessGroupMapDao).getStorageAccessGroups(destPoolId);
        List<Long> poolIds = new ArrayList<>();
        poolIds.add(srcPool.getId());
        poolIds.add(destPool.getId());
        Mockito.doReturn(null).when(storageManagerImpl).findUpAndEnabledHostWithAccessToStoragePools(poolIds);
        Pair<Boolean, String> result = storageManagerImpl.checkIfReadyVolumeFitsInStoragePoolWithStorageAccessGroups(destPool, volume);

        Assert.assertFalse("Volume with host having common storage access group for destination pool but not source pool should not fit.", result.first());
        Assert.assertEquals("No common host connected to source and destination storages", result.second());
    }

    @Test
    public void testConfigureStorageAccess_SkipUpdateForZone() {
        Long zoneId = 1L;
        List<String> storageAccessGroups = Arrays.asList("sag1", "sag2");

        ConfigureStorageAccessCmd cmd = Mockito.mock(ConfigureStorageAccessCmd.class);
        Mockito.when(cmd.getZoneId()).thenReturn(zoneId);
        Mockito.when(cmd.getPodId()).thenReturn(null);
        Mockito.when(cmd.getClusterId()).thenReturn(null);
        Mockito.when(cmd.getHostId()).thenReturn(null);
        Mockito.when(cmd.getStorageId()).thenReturn(null);
        Mockito.when(cmd.getStorageAccessGroups()).thenReturn(storageAccessGroups);

        DataCenterVO zone = Mockito.mock(DataCenterVO.class);
        Mockito.when(zone.getStorageAccessGroups()).thenReturn("sag2,sag1");
        Mockito.when(dataCenterDao.findById(zoneId)).thenReturn(zone);

        boolean result = storageManagerImpl.configureStorageAccess(cmd);

        Mockito.verify(resourceMgr, Mockito.never()).updateZoneStorageAccessGroups(Mockito.anyLong(), Mockito.anyList());
        Mockito.verify(dataCenterDao, Mockito.never()).update(Mockito.eq(zoneId), Mockito.any(DataCenterVO.class));

        assertTrue(result);
    }

    @Test
    public void testConfigureStorageAccess_SkipUpdateForPod() {
        Long podId = 1L;
        List<String> storageAccessGroups = Arrays.asList("sag1", "sag2");

        ConfigureStorageAccessCmd cmd = Mockito.mock(ConfigureStorageAccessCmd.class);
        Mockito.when(cmd.getZoneId()).thenReturn(null);
        Mockito.when(cmd.getPodId()).thenReturn(podId);
        Mockito.when(cmd.getClusterId()).thenReturn(null);
        Mockito.when(cmd.getHostId()).thenReturn(null);
        Mockito.when(cmd.getStorageId()).thenReturn(null);
        Mockito.when(cmd.getStorageAccessGroups()).thenReturn(storageAccessGroups);

        HostPodVO pod = Mockito.mock(HostPodVO.class);
        Mockito.when(pod.getDataCenterId()).thenReturn(1L);
        Mockito.when(pod.getStorageAccessGroups()).thenReturn("sag1,sag2");
        Mockito.when(podDao.findById(podId)).thenReturn(pod);
        Mockito.doNothing().when(storageManagerImpl).checkIfStorageAccessGroupsExistsOnZone(1L, storageAccessGroups);

        boolean result = storageManagerImpl.configureStorageAccess(cmd);

        Mockito.verify(resourceMgr, Mockito.never()).updatePodStorageAccessGroups(Mockito.anyLong(), Mockito.anyList());
        Mockito.verify(podDao, Mockito.never()).update(Mockito.eq(podId), Mockito.any(HostPodVO.class));

        assertTrue(result);
    }

    @Test
    public void testConfigureStorageAccess_SkipUpdateForCluster() {
        Long clusterId = 1L;
        List<String> storageAccessGroups = Arrays.asList("sag1", "sag2");

        ConfigureStorageAccessCmd cmd = Mockito.mock(ConfigureStorageAccessCmd.class);
        Mockito.when(cmd.getZoneId()).thenReturn(null);
        Mockito.when(cmd.getPodId()).thenReturn(null);
        Mockito.when(cmd.getClusterId()).thenReturn(clusterId);
        Mockito.when(cmd.getHostId()).thenReturn(null);
        Mockito.when(cmd.getStorageId()).thenReturn(null);
        Mockito.when(cmd.getStorageAccessGroups()).thenReturn(storageAccessGroups);

        ClusterVO cluster = Mockito.mock(ClusterVO.class);
        Mockito.when(cluster.getPodId()).thenReturn(1L);
        Mockito.when(cluster.getStorageAccessGroups()).thenReturn("sag1,sag2");
        Mockito.when(clusterDao.findById(clusterId)).thenReturn(cluster);
        Mockito.doNothing().when(storageManagerImpl).checkIfStorageAccessGroupsExistsOnPod(1L, storageAccessGroups);

        boolean result = storageManagerImpl.configureStorageAccess(cmd);

        Mockito.verify(resourceMgr, Mockito.never()).updateClusterStorageAccessGroups(Mockito.anyLong(), Mockito.anyList());
        Mockito.verify(clusterDao, Mockito.never()).update(Mockito.eq(clusterId), Mockito.any(ClusterVO.class));

        assertTrue(result);
    }

    @Test
    public void testConfigureStorageAccess_SkipUpdateForHost() {
        Long hostId = 1L;
        List<String> storageAccessGroups = Arrays.asList("sag1", "sag2");

        ConfigureStorageAccessCmd cmd = Mockito.mock(ConfigureStorageAccessCmd.class);
        Mockito.when(cmd.getZoneId()).thenReturn(null);
        Mockito.when(cmd.getPodId()).thenReturn(null);
        Mockito.when(cmd.getClusterId()).thenReturn(null);
        Mockito.when(cmd.getHostId()).thenReturn(hostId);
        Mockito.when(cmd.getStorageId()).thenReturn(null);
        Mockito.when(cmd.getStorageAccessGroups()).thenReturn(storageAccessGroups);

        HostVO host = Mockito.mock(HostVO.class);
        Mockito.when(host.getClusterId()).thenReturn(1L);
        Mockito.when(host.getStorageAccessGroups()).thenReturn("sag1,sag2");
        Mockito.when(hostDao.findById(hostId)).thenReturn(host);
        Mockito.doNothing().when(storageManagerImpl).checkIfStorageAccessGroupsExistsOnCluster(1L, storageAccessGroups);

        boolean result = storageManagerImpl.configureStorageAccess(cmd);

        Mockito.verify(resourceMgr, Mockito.never()).updateHostStorageAccessGroups(Mockito.anyLong(), Mockito.anyList());
        Mockito.verify(hostDao, Mockito.never()).update(Mockito.eq(hostId), Mockito.any(HostVO.class));

        assertTrue(result);
    }

    @Test
    public void testConfigureStorageAccess_InvalidNonNullCount() {
        ConfigureStorageAccessCmd cmd = Mockito.mock(ConfigureStorageAccessCmd.class);

        Mockito.when(cmd.getZoneId()).thenReturn(1L);
        Mockito.when(cmd.getPodId()).thenReturn(1L);
        Mockito.when(cmd.getClusterId()).thenReturn(null);
        Mockito.when(cmd.getHostId()).thenReturn(null);
        Mockito.when(cmd.getStorageId()).thenReturn(null);

        try {
            storageManagerImpl.configureStorageAccess(cmd);
            Assert.fail("Expected IllegalArgumentException to be thrown due to nonNullCount validation");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Exactly one of zoneid, podid, clusterid, hostid or storagepoolid is required"));
        }
    }

    @Test
    public void testConfigureStorageAccess_MissingStorageAccessGroups() {
        ConfigureStorageAccessCmd cmd = Mockito.mock(ConfigureStorageAccessCmd.class);

        Mockito.when(cmd.getZoneId()).thenReturn(1L);
        Mockito.when(cmd.getPodId()).thenReturn(null);
        Mockito.when(cmd.getClusterId()).thenReturn(null);
        Mockito.when(cmd.getHostId()).thenReturn(null);
        Mockito.when(cmd.getStorageId()).thenReturn(null);
        Mockito.when(cmd.getStorageAccessGroups()).thenReturn(null);

        try {
            storageManagerImpl.configureStorageAccess(cmd);
            Assert.fail("Expected InvalidParameterValueException to be thrown due to missing storageAccessGroups");
        } catch (InvalidParameterValueException e) {
            assertTrue(e.getMessage().contains("storageaccessgroups parameter is required"));
        }
    }

    @Test
    public void testCheckIfStorageAccessGroupsExistsOnZone_NoException() {
        long zoneId = 1L;
        List<String> newStorageAccessGroups = Arrays.asList("group1", "group2");

        DataCenterVO zoneVO = Mockito.mock(DataCenterVO.class);
        Mockito.when(dataCenterDao.findById(zoneId)).thenReturn(zoneVO);
        Mockito.when(zoneVO.getStorageAccessGroups()).thenReturn("group3,group4");

        storageManagerImpl.checkIfStorageAccessGroupsExistsOnZone(zoneId, newStorageAccessGroups);
    }

    @Test
    public void testCheckIfStorageAccessGroupsExistsOnZone_ThrowsException() {
        long zoneId = 1L;
        List<String> newStorageAccessGroups = Arrays.asList("group1", "group2", "group3");

        DataCenterVO zoneVO = Mockito.mock(DataCenterVO.class);
        Mockito.when(dataCenterDao.findById(zoneId)).thenReturn(zoneVO);
        Mockito.when(zoneVO.getStorageAccessGroups()).thenReturn("group3,group4");

        CloudRuntimeException thrownException = assertThrows(CloudRuntimeException.class, () -> {
            storageManagerImpl.checkIfStorageAccessGroupsExistsOnZone(zoneId, newStorageAccessGroups);
        });

        assertTrue(thrownException.getMessage().contains("access groups already exist on the zone: [group3]"));
    }

    @Test
    public void testCheckIfStorageAccessGroupsExistsOnPod_NoException() {
        long podId = 1L;
        long zoneId = 2L;
        List<String> newStorageAccessGroups = Arrays.asList("group1", "group2");

        HostPodVO podVO = Mockito.mock(HostPodVO.class);
        DataCenterVO zoneVO = Mockito.mock(DataCenterVO.class);

        Mockito.when(podVO.getDataCenterId()).thenReturn(zoneId);
        Mockito.when(podVO.getStorageAccessGroups()).thenReturn("group3,group4");

        Mockito.when(zoneVO.getStorageAccessGroups()).thenReturn("group5,group6");

        Mockito.when(podDao.findById(podId)).thenReturn(podVO);
        Mockito.when(dataCenterDao.findById(zoneId)).thenReturn(zoneVO);

        storageManagerImpl.checkIfStorageAccessGroupsExistsOnPod(podId, newStorageAccessGroups);
    }

    @Test
    public void testCheckIfStorageAccessGroupsExistsOnPod_ThrowsException() {
        long podId = 1L;
        long zoneId = 2L;
        List<String> newStorageAccessGroups = Arrays.asList("group1", "group2", "group3");

        HostPodVO podVO = Mockito.mock(HostPodVO.class);
        DataCenterVO zoneVO = Mockito.mock(DataCenterVO.class);

        Mockito.when(podVO.getDataCenterId()).thenReturn(zoneId);
        Mockito.when(podVO.getStorageAccessGroups()).thenReturn("group3,group4");

        Mockito.when(zoneVO.getStorageAccessGroups()).thenReturn("group5,group6");

        Mockito.when(podDao.findById(podId)).thenReturn(podVO);
        Mockito.when(dataCenterDao.findById(zoneId)).thenReturn(zoneVO);

        CloudRuntimeException thrownException = assertThrows(CloudRuntimeException.class, () -> {
            storageManagerImpl.checkIfStorageAccessGroupsExistsOnPod(podId, newStorageAccessGroups);
        });

        assertTrue(thrownException.getMessage().contains("access groups already exist on the pod: [group3]"));
    }

    @Test
    public void testCheckIfStorageAccessGroupsExistsOnCluster_NoException() {
        long clusterId = 1L;
        long podId = 2L;
        long zoneId = 3L;
        List<String> newStorageAccessGroups = Arrays.asList("group1", "group2");

        ClusterVO clusterVO = Mockito.mock(ClusterVO.class);
        HostPodVO podVO = Mockito.mock(HostPodVO.class);
        DataCenterVO zoneVO = Mockito.mock(DataCenterVO.class);

        Mockito.when(clusterVO.getPodId()).thenReturn(podId);
        Mockito.when(clusterVO.getStorageAccessGroups()).thenReturn("group4,group5");

        Mockito.when(podVO.getDataCenterId()).thenReturn(zoneId);
        Mockito.when(podVO.getStorageAccessGroups()).thenReturn("group6,group7");

        Mockito.when(zoneVO.getStorageAccessGroups()).thenReturn("group8,group9");

        Mockito.when(clusterDao.findById(clusterId)).thenReturn(clusterVO);
        Mockito.when(podDao.findById(podId)).thenReturn(podVO);
        Mockito.when(dataCenterDao.findById(zoneId)).thenReturn(zoneVO);

        storageManagerImpl.checkIfStorageAccessGroupsExistsOnCluster(clusterId, newStorageAccessGroups);
    }

    @Test
    public void testCheckIfStorageAccessGroupsExistsOnCluster_ThrowsException() {
        long clusterId = 1L;
        long podId = 2L;
        long zoneId = 3L;
        List<String> newStorageAccessGroups = Arrays.asList("group1", "group2", "group4");

        ClusterVO clusterVO = Mockito.mock(ClusterVO.class);
        HostPodVO podVO = Mockito.mock(HostPodVO.class);
        DataCenterVO zoneVO = Mockito.mock(DataCenterVO.class);

        Mockito.when(clusterVO.getPodId()).thenReturn(podId);
        Mockito.when(clusterVO.getStorageAccessGroups()).thenReturn("group4,group5");

        Mockito.when(podVO.getDataCenterId()).thenReturn(zoneId);
        Mockito.when(podVO.getStorageAccessGroups()).thenReturn("group6,group7");

        Mockito.when(zoneVO.getStorageAccessGroups()).thenReturn("group8,group9");

        Mockito.when(clusterDao.findById(clusterId)).thenReturn(clusterVO);
        Mockito.when(podDao.findById(podId)).thenReturn(podVO);
        Mockito.when(dataCenterDao.findById(zoneId)).thenReturn(zoneVO);

        CloudRuntimeException thrownException = assertThrows(CloudRuntimeException.class, () -> {
            storageManagerImpl.checkIfStorageAccessGroupsExistsOnCluster(clusterId, newStorageAccessGroups);
        });

        assertTrue(thrownException.getMessage().contains("access groups already exist on the cluster: [group4]"));
    }
}

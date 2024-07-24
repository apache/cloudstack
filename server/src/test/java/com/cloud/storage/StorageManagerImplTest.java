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

import org.apache.cloudstack.api.command.admin.storage.ChangeStoragePoolScopeCmd;
import com.cloud.agent.api.StoragePoolInfo;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.exception.ConnectionException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.host.Host;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.user.AccountManagerImpl;
import com.cloud.utils.Pair;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.dao.VMInstanceDao;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.framework.config.ConfigDepot;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
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
import org.springframework.test.util.ReflectionTestUtils;

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

    @Mock
    PrimaryDataStoreDao storagePoolDao;

    @Spy
    @InjectMocks
    private StorageManagerImpl storageManagerImpl;

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
        InvalidParameterValueException exception = Assert.assertThrows(InvalidParameterValueException.class,
                () -> storageManagerImpl.checkNFSMountOptionsForCreate(details, HypervisorType.XenServer, ""));
        Assert.assertEquals(exception.getMessage(), "NFS options can not be set for the hypervisor type " + HypervisorType.XenServer);
    }

    @Test
    public void testCheckNFSMountOptionsForCreateNotNFS() {
        Map<String, String> details = new HashMap<>();
        details.put(ApiConstants.NFS_MOUNT_OPTIONS, "vers=4.1");
        InvalidParameterValueException exception = Assert.assertThrows(InvalidParameterValueException.class,
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
        PermissionDeniedException exception = Assert.assertThrows(PermissionDeniedException.class,
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
        InvalidParameterValueException exception = Assert.assertThrows(InvalidParameterValueException.class,
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
        InvalidParameterValueException exception = Assert.assertThrows(InvalidParameterValueException.class,
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
        InvalidParameterValueException exception = Assert.assertThrows(InvalidParameterValueException.class,
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
}

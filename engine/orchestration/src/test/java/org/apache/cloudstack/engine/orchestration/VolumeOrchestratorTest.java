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
package org.apache.cloudstack.engine.orchestration;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.lang.reflect.Field;

import com.cloud.configuration.Resource;
import com.cloud.deploy.DeploymentClusterPlanner;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.StorageAccessException;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.offering.DiskOffering;
import com.cloud.storage.ScopeType;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.Storage;
import com.cloud.storage.Volume;
import com.cloud.storage.Volume.Type;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.user.Account;
import com.cloud.user.ResourceLimitService;
import com.cloud.uservm.UserVm;
import com.cloud.utils.db.EntityManager;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VirtualMachine;
import com.cloud.utils.Pair;
import org.apache.cloudstack.engine.subsystem.api.storage.DataObject;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreDriver;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeService;
import org.apache.cloudstack.storage.datastore.db.ImageStoreDao;
import org.apache.cloudstack.storage.datastore.db.ImageStoreVO;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreVO;
import org.apache.cloudstack.framework.config.ConfigDepot;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.secret.PassphraseVO;
import org.apache.cloudstack.secret.dao.PassphraseDao;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.commons.lang3.ObjectUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import static org.apache.cloudstack.engine.orchestration.service.VolumeOrchestrationService.VolumeAllocationAlgorithm;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class VolumeOrchestratorTest {

    @Mock
    protected ResourceLimitService resourceLimitMgr;
    @Mock
    protected VolumeService volumeService;
    @Mock
    protected VolumeDataFactory volumeDataFactory;
    @Mock
    protected VolumeDao volumeDao;
    @Mock
    protected PassphraseDao passphraseDao;
    @Mock
    protected PrimaryDataStoreDao storagePoolDao;
    @Mock
    protected EntityManager entityMgr;
    @Mock
    ConfigDepot configDepot;
    @Mock
    ConfigurationDao configurationDao;


    @Mock
    private SnapshotDataStoreDao snapshotDataStoreDaoMock;

    @Mock
    private ImageStoreDao imageStoreDaoMock;


    @Spy
    @InjectMocks
    private VolumeOrchestrator volumeOrchestrator = new VolumeOrchestrator();

    private static final Long DEFAULT_ACCOUNT_PS_RESOURCE_COUNT = 100L;
    private Long accountPSResourceCount;
    private static final long MOCK_VM_ID = 202L;
    private static final long MOCK_POOL_ID = 303L;
    private static final String MOCK_VM_NAME = "Test-VM";

    @Before
    public void setUp() throws Exception {
        accountPSResourceCount = DEFAULT_ACCOUNT_PS_RESOURCE_COUNT;
        Mockito.when(resourceLimitMgr.recalculateResourceCount(Mockito.anyLong(), Mockito.anyLong(), Mockito.anyInt())).thenReturn(new ArrayList<>());
        Mockito.doAnswer((Answer<Void>) invocation -> {
            Resource.ResourceType type = (Resource.ResourceType)invocation.getArguments()[1];
            Long increment = (Long)invocation.getArguments()[3];
            if (Resource.ResourceType.primary_storage.equals(type)) {
                accountPSResourceCount += increment;
            }
            return null;
        }).when(resourceLimitMgr).incrementResourceCount(Mockito.anyLong(), Mockito.any(Resource.ResourceType.class), Mockito.anyBoolean(), Mockito.anyLong());
        Mockito.doAnswer((Answer<Void>) invocation -> {
            Resource.ResourceType type = (Resource.ResourceType)invocation.getArguments()[1];
            Long decrement = (Long)invocation.getArguments()[3];
            if (Resource.ResourceType.primary_storage.equals(type)) {
                accountPSResourceCount -= decrement;
            }
            return null;
        }).when(resourceLimitMgr).decrementResourceCount(Mockito.anyLong(), Mockito.any(Resource.ResourceType.class), Mockito.anyBoolean(), Mockito.anyLong());
    }

    private void runCheckAndUpdateVolumeAccountResourceCountTest(Long originalSize, Long newSize) {
        VolumeVO v1 = Mockito.mock(VolumeVO.class);
        Mockito.when(v1.getSize()).thenReturn(originalSize);
        VolumeVO v2 = Mockito.mock(VolumeVO.class);
        Mockito.when(v2.getSize()).thenReturn(newSize);
        volumeOrchestrator.checkAndUpdateVolumeAccountResourceCount(v1, v2);
        Long expected = ObjectUtils.anyNull(originalSize, newSize) ?
                DEFAULT_ACCOUNT_PS_RESOURCE_COUNT : DEFAULT_ACCOUNT_PS_RESOURCE_COUNT + (newSize - originalSize);
        Assert.assertEquals(expected, accountPSResourceCount);
    }

    @Test
    public void testCheckAndUpdateVolumeAccountResourceCountSameSize() {
        runCheckAndUpdateVolumeAccountResourceCountTest(10L, 10L);
    }

    @Test
    public void testCheckAndUpdateVolumeAccountResourceCountEitherSizeNull() {
        runCheckAndUpdateVolumeAccountResourceCountTest(null, 10L);
        runCheckAndUpdateVolumeAccountResourceCountTest(10L, null);
    }

    @Test
    public void testCheckAndUpdateVolumeAccountResourceCountMoreSize() {
        runCheckAndUpdateVolumeAccountResourceCountTest(10L, 20L);
    }

    @Test
    public void testCheckAndUpdateVolumeAccountResourceCountLessSize() {
        runCheckAndUpdateVolumeAccountResourceCountTest(20L, 10L);
    }

    @Test
    public void testGrantVolumeAccessToHostIfNeededDriverNoNeed() {
        PrimaryDataStore store = Mockito.mock(PrimaryDataStore.class);
        PrimaryDataStoreDriver driver = Mockito.mock(PrimaryDataStoreDriver.class);
        Mockito.when(driver.volumesRequireGrantAccessWhenUsed()).thenReturn(false);
        Mockito.when(store.getDriver()).thenReturn(driver);
        volumeOrchestrator.grantVolumeAccessToHostIfNeeded(store, 1L,
                Mockito.mock(HostVO.class), "");
        Mockito.verify(volumeService, Mockito.never())
                .grantAccess(Mockito.any(DataObject.class), Mockito.any(Host.class), Mockito.any(DataStore.class));
    }

    @Test
    public void testGrantVolumeAccessToHostIfNeededDriverNeeds() {
        PrimaryDataStore store = Mockito.mock(PrimaryDataStore.class);
        PrimaryDataStoreDriver driver = Mockito.mock(PrimaryDataStoreDriver.class);
        Mockito.when(driver.volumesRequireGrantAccessWhenUsed()).thenReturn(true);
        Mockito.when(store.getDriver()).thenReturn(driver);
        Mockito.when(volumeDataFactory.getVolume(Mockito.anyLong())).thenReturn(Mockito.mock(VolumeInfo.class));
        Mockito.doReturn(true).when(volumeService)
                .grantAccess(Mockito.any(DataObject.class), Mockito.any(Host.class), Mockito.any(DataStore.class));
        volumeOrchestrator.grantVolumeAccessToHostIfNeeded(store, 1L,
                Mockito.mock(HostVO.class), "");
        Mockito.verify(volumeService, Mockito.times(1))
                .grantAccess(Mockito.any(DataObject.class), Mockito.any(Host.class), Mockito.any(DataStore.class));
    }

    @Test(expected = StorageAccessException.class)
    public void testGrantVolumeAccessToHostIfNeededDriverNeedsButException() {
        PrimaryDataStore store = Mockito.mock(PrimaryDataStore.class);
        PrimaryDataStoreDriver driver = Mockito.mock(PrimaryDataStoreDriver.class);
        Mockito.when(driver.volumesRequireGrantAccessWhenUsed()).thenReturn(true);
        Mockito.when(store.getDriver()).thenReturn(driver);
        Mockito.when(volumeDataFactory.getVolume(Mockito.anyLong())).thenReturn(Mockito.mock(VolumeInfo.class));
        Mockito.doThrow(CloudRuntimeException.class).when(volumeService)
                .grantAccess(Mockito.any(DataObject.class), Mockito.any(Host.class), Mockito.any(DataStore.class));
        volumeOrchestrator.grantVolumeAccessToHostIfNeeded(store, 1L,
                Mockito.mock(HostVO.class), "");
    }

    @Test
    public void testImportVolume() {
        Type volumeType = Type.DATADISK;
        String name = "new-volume";
        Long sizeInBytes = 1000000L;
        Long zoneId = 1L;
        Long domainId = 2L;
        Long accountId = 3L;
        Long diskOfferingId = 4L;
        DiskOffering diskOffering = Mockito.mock(DiskOffering.class);
        Hypervisor.HypervisorType hypervisorType = Hypervisor.HypervisorType.KVM;
        Account owner = Mockito.mock(Account.class);
        Mockito.when(owner.getDomainId()).thenReturn(domainId);
        Mockito.when(owner.getId()).thenReturn(accountId);
        Mockito.when(diskOffering.getId()).thenReturn(diskOfferingId);
        Long deviceId = 2L;
        Long poolId = 3L;
        String path = "volume path";
        String chainInfo = "chain info";

        MockedConstruction<VolumeVO> volumeVOMockedConstructionConstruction = Mockito.mockConstruction(VolumeVO.class, (mock, context) -> {
        });

        VolumeVO volumeVO = Mockito.mock(VolumeVO.class);
        Mockito.when(volumeDao.persist(Mockito.any(VolumeVO.class))).thenReturn(volumeVO);

        volumeOrchestrator.importVolume(volumeType, name, diskOffering, sizeInBytes, null, null,
                zoneId, hypervisorType, null, null, owner,
                deviceId, poolId, path, chainInfo);

        VolumeVO volume = volumeVOMockedConstructionConstruction.constructed().get(0);
        Mockito.verify(volume, Mockito.never()).setInstanceId(Mockito.anyLong());
        Mockito.verify(volume, Mockito.never()).setAttached(Mockito.any(Date.class));
        Mockito.verify(volume, Mockito.times(1)).setDeviceId(deviceId);
        Mockito.verify(volume, Mockito.never()).setDisplayVolume(Mockito.any(Boolean.class));
        Mockito.verify(volume, Mockito.times(1)).setFormat(Storage.ImageFormat.QCOW2);
        Mockito.verify(volume, Mockito.times(1)).setPoolId(poolId);
        Mockito.verify(volume, Mockito.times(1)).setPath(path);
        Mockito.verify(volume, Mockito.times(1)).setChainInfo(chainInfo);
        Mockito.verify(volume, Mockito.times(1)).setState(Volume.State.Ready);
    }

    @Test
    public void testAllocateDuplicateVolumeVOBasic() {
        Volume oldVol = Mockito.mock(Volume.class);
        Mockito.when(oldVol.getVolumeType()).thenReturn(Volume.Type.ROOT);
        Mockito.when(oldVol.getName()).thenReturn("testVol");
        Mockito.when(oldVol.getDataCenterId()).thenReturn(1L);
        Mockito.when(oldVol.getDomainId()).thenReturn(2L);
        Mockito.when(oldVol.getAccountId()).thenReturn(3L);
        Mockito.when(oldVol.getDiskOfferingId()).thenReturn(4L);
        Mockito.when(oldVol.getProvisioningType()).thenReturn(Storage.ProvisioningType.THIN);
        Mockito.when(oldVol.getSize()).thenReturn(10L);
        Mockito.when(oldVol.getMinIops()).thenReturn(100L);
        Mockito.when(oldVol.getMaxIops()).thenReturn(200L);
        Mockito.when(oldVol.get_iScsiName()).thenReturn("iqn.test");
        Mockito.when(oldVol.getTemplateId()).thenReturn(5L);
        Mockito.when(oldVol.getDeviceId()).thenReturn(1L);
        Mockito.when(oldVol.getInstanceId()).thenReturn(6L);
        Mockito.when(oldVol.isRecreatable()).thenReturn(false);
        Mockito.when(oldVol.getFormat()).thenReturn(Storage.ImageFormat.QCOW2);
        Mockito.when(oldVol.getPassphraseId()).thenReturn(null); // no encryption

        VolumeVO persistedVol = Mockito.mock(VolumeVO.class);
        Mockito.when(volumeDao.persist(Mockito.any(VolumeVO.class))).thenReturn(persistedVol);

        VolumeVO result = volumeOrchestrator.allocateDuplicateVolumeVO(oldVol, null, null);
        assertNotNull(result);
        Mockito.verify(volumeDao, Mockito.times(1)).persist(Mockito.any(VolumeVO.class));
    }

    @Test
    public void testAllocateDuplicateVolumeVOWithEncryption() {
        Volume oldVol = Mockito.mock(Volume.class);
        Mockito.when(oldVol.getVolumeType()).thenReturn(Volume.Type.ROOT);
        Mockito.when(oldVol.getName()).thenReturn("secureVol");
        Mockito.when(oldVol.getDataCenterId()).thenReturn(1L);
        Mockito.when(oldVol.getDomainId()).thenReturn(2L);
        Mockito.when(oldVol.getAccountId()).thenReturn(3L);
        Mockito.when(oldVol.getDiskOfferingId()).thenReturn(4L);
        Mockito.when(oldVol.getProvisioningType()).thenReturn(Storage.ProvisioningType.THIN);
        Mockito.when(oldVol.getSize()).thenReturn(10L);
        Mockito.when(oldVol.getMinIops()).thenReturn(100L);
        Mockito.when(oldVol.getMaxIops()).thenReturn(200L);
        Mockito.when(oldVol.get_iScsiName()).thenReturn("iqn.secure");
        Mockito.when(oldVol.getTemplateId()).thenReturn(5L);
        Mockito.when(oldVol.getDeviceId()).thenReturn(2L);
        Mockito.when(oldVol.getInstanceId()).thenReturn(7L);
        Mockito.when(oldVol.isRecreatable()).thenReturn(true);
        Mockito.when(oldVol.getFormat()).thenReturn(Storage.ImageFormat.RAW);
        Mockito.when(oldVol.getPassphraseId()).thenReturn(42L);

        PassphraseVO passphrase = Mockito.mock(PassphraseVO.class);
        Mockito.when(passphrase.getId()).thenReturn(999L);
        Mockito.when(passphraseDao.persist(Mockito.any())).thenReturn(passphrase);

        VolumeVO persistedVol = Mockito.mock(VolumeVO.class);
        Mockito.when(volumeDao.persist(Mockito.any())).thenReturn(persistedVol);

        VolumeVO result = volumeOrchestrator.allocateDuplicateVolumeVO(oldVol, null, null);
        assertNotNull(result);
        Mockito.verify(passphraseDao).persist(Mockito.any(PassphraseVO.class));
        Mockito.verify(volumeDao).persist(Mockito.any());
    }

    @Test
    public void testAllocateDuplicateVolumeVOWithTemplateOverride() {
        Volume oldVol = Mockito.mock(Volume.class);
        Mockito.when(oldVol.getVolumeType()).thenReturn(Volume.Type.ROOT);
        Mockito.when(oldVol.getName()).thenReturn("tmplVol");
        Mockito.when(oldVol.getDataCenterId()).thenReturn(1L);
        Mockito.when(oldVol.getDomainId()).thenReturn(2L);
        Mockito.when(oldVol.getAccountId()).thenReturn(3L);
        Mockito.when(oldVol.getDiskOfferingId()).thenReturn(4L);
        Mockito.when(oldVol.getProvisioningType()).thenReturn(Storage.ProvisioningType.THIN);
        Mockito.when(oldVol.getSize()).thenReturn(20L);
        Mockito.when(oldVol.getMinIops()).thenReturn(50L);
        Mockito.when(oldVol.getMaxIops()).thenReturn(250L);
        Mockito.when(oldVol.get_iScsiName()).thenReturn("iqn.tmpl");

        VolumeVO persistedVol = Mockito.mock(VolumeVO.class);
        Mockito.when(volumeDao.persist(Mockito.any())).thenReturn(persistedVol);

        PassphraseVO mockPassPhrase = Mockito.mock(PassphraseVO.class);
        Mockito.when(passphraseDao.persist(Mockito.any())).thenReturn(mockPassPhrase);

        VolumeVO result = volumeOrchestrator.allocateDuplicateVolumeVO(oldVol, null, 222L);
        assertNotNull(result);
    }

    @Test
    public void testAllocateDuplicateVolumeVOEncryptionFromOldVolumeOnly() {
        Volume oldVol = Mockito.mock(Volume.class);
        Mockito.when(oldVol.getVolumeType()).thenReturn(Volume.Type.ROOT);
        Mockito.when(oldVol.getName()).thenReturn("vol-old");
        Mockito.when(oldVol.getDataCenterId()).thenReturn(1L);
        Mockito.when(oldVol.getDomainId()).thenReturn(2L);
        Mockito.when(oldVol.getAccountId()).thenReturn(3L);
        Mockito.when(oldVol.getDiskOfferingId()).thenReturn(4L);
        Mockito.when(oldVol.getProvisioningType()).thenReturn(Storage.ProvisioningType.SPARSE);
        Mockito.when(oldVol.getSize()).thenReturn(30L);
        Mockito.when(oldVol.getMinIops()).thenReturn(10L);
        Mockito.when(oldVol.getMaxIops()).thenReturn(500L);
        Mockito.when(oldVol.get_iScsiName()).thenReturn("iqn.old");
        Mockito.when(oldVol.getTemplateId()).thenReturn(123L);
        Mockito.when(oldVol.getDeviceId()).thenReturn(1L);
        Mockito.when(oldVol.getInstanceId()).thenReturn(100L);
        Mockito.when(oldVol.isRecreatable()).thenReturn(false);
        Mockito.when(oldVol.getFormat()).thenReturn(Storage.ImageFormat.RAW);

        DiskOffering diskOffering = Mockito.mock(DiskOffering.class);
        Mockito.when(diskOffering.getEncrypt()).thenReturn(false); // explicitly disables encryption

        VolumeVO persistedVol = Mockito.mock(VolumeVO.class);
        Mockito.when(volumeDao.persist(Mockito.any())).thenReturn(persistedVol);

        VolumeVO result = volumeOrchestrator.allocateDuplicateVolumeVO(oldVol, diskOffering, null);
        assertNotNull(result);
        Mockito.verify(volumeDao).persist(Mockito.any());
    }

    @Test
    public void testVolumeOnSharedStoragePoolTrue() {
        VolumeVO volume = Mockito.mock(VolumeVO.class);
        Mockito.when(volume.getPoolId()).thenReturn(MOCK_POOL_ID);

        StoragePoolVO pool = Mockito.mock(StoragePoolVO.class);
        Mockito.when(pool.getScope()).thenReturn(ScopeType.CLUSTER); // Shared scope
        Mockito.when(storagePoolDao.findById(MOCK_POOL_ID)).thenReturn(pool);

        assertTrue(volumeOrchestrator.volumeOnSharedStoragePool(volume));
    }

    @Test
    public void testVolumeOnSharedStoragePoolFalseHostScope() {
        VolumeVO volume = Mockito.mock(VolumeVO.class);
        Mockito.when(volume.getPoolId()).thenReturn(MOCK_POOL_ID);

        StoragePoolVO pool = Mockito.mock(StoragePoolVO.class);
        Mockito.when(pool.getScope()).thenReturn(ScopeType.HOST); // Local scope
        Mockito.when(storagePoolDao.findById(MOCK_POOL_ID)).thenReturn(pool);

        Assert.assertFalse(volumeOrchestrator.volumeOnSharedStoragePool(volume));
    }

    @Test
    public void testVolumeOnSharedStoragePoolFalseNoPool() {
        VolumeVO volume = Mockito.mock(VolumeVO.class);
        Mockito.when(volume.getPoolId()).thenReturn(null); // No pool associated

        Assert.assertFalse(volumeOrchestrator.volumeOnSharedStoragePool(volume));
        Mockito.verify(storagePoolDao, Mockito.never()).findById(Mockito.anyLong());
    }

    @Test
    public void testVolumeOnSharedStoragePoolFalsePoolNotFound() {
        VolumeVO volume = Mockito.mock(VolumeVO.class);
        Mockito.when(volume.getPoolId()).thenReturn(MOCK_POOL_ID);

        Mockito.when(storagePoolDao.findById(MOCK_POOL_ID)).thenReturn(null); // Pool not found in DB

        Assert.assertFalse(volumeOrchestrator.volumeOnSharedStoragePool(volume));
    }


    @Test
    public void testVolumeInactiveNoVmId() {
        VolumeVO volume = Mockito.mock(VolumeVO.class);
        Mockito.when(volume.getInstanceId()).thenReturn(null);
        assertTrue(volumeOrchestrator.volumeInactive(volume));
        Mockito.verify(entityMgr, Mockito.never()).findById(Mockito.eq(UserVm.class), Mockito.anyLong());
    }

    @Test
    public void testVolumeInactiveVmNotFound() {
        VolumeVO volume = Mockito.mock(VolumeVO.class);
        Mockito.when(volume.getInstanceId()).thenReturn(MOCK_VM_ID);
        Mockito.when(entityMgr.findById(UserVm.class, MOCK_VM_ID)).thenReturn(null);
        assertTrue(volumeOrchestrator.volumeInactive(volume));
    }

    @Test
    public void testVolumeInactiveVmStopped() {
        VolumeVO volume = Mockito.mock(VolumeVO.class);
        Mockito.when(volume.getInstanceId()).thenReturn(MOCK_VM_ID);
        UserVm vm = Mockito.mock(UserVm.class);
        Mockito.when(vm.getState()).thenReturn(VirtualMachine.State.Stopped);
        Mockito.when(entityMgr.findById(UserVm.class, MOCK_VM_ID)).thenReturn(vm);
        assertTrue(volumeOrchestrator.volumeInactive(volume));
    }

    @Test
    public void testVolumeInactiveVmDestroyed() {
        VolumeVO volume = Mockito.mock(VolumeVO.class);
        Mockito.when(volume.getInstanceId()).thenReturn(MOCK_VM_ID);
        UserVm vm = Mockito.mock(UserVm.class);
        Mockito.when(vm.getState()).thenReturn(VirtualMachine.State.Destroyed);
        Mockito.when(entityMgr.findById(UserVm.class, MOCK_VM_ID)).thenReturn(vm);
        assertTrue(volumeOrchestrator.volumeInactive(volume));
    }

    @Test
    public void testVolumeInactiveVmRunning() {
        VolumeVO volume = Mockito.mock(VolumeVO.class);
        Mockito.when(volume.getInstanceId()).thenReturn(MOCK_VM_ID);
        UserVm vm = Mockito.mock(UserVm.class);
        Mockito.when(vm.getState()).thenReturn(VirtualMachine.State.Running); // Active state
        Mockito.when(entityMgr.findById(UserVm.class, MOCK_VM_ID)).thenReturn(vm);
        Assert.assertFalse(volumeOrchestrator.volumeInactive(volume));
    }

    @Test
    public void testGetVmNameOnVolumeNoVmId() {
        VolumeVO volume = Mockito.mock(VolumeVO.class);
        Mockito.when(volume.getInstanceId()).thenReturn(null);
        Assert.assertNull(volumeOrchestrator.getVmNameOnVolume(volume));
        Mockito.verify(entityMgr, Mockito.never()).findById(Mockito.eq(VirtualMachine.class), Mockito.anyLong());
    }

    @Test
    public void testGetVmNameOnVolumeVmNotFound() {
        VolumeVO volume = Mockito.mock(VolumeVO.class);
        Mockito.when(volume.getInstanceId()).thenReturn(MOCK_VM_ID);
        Mockito.when(entityMgr.findById(VirtualMachine.class, MOCK_VM_ID)).thenReturn(null);
        Assert.assertNull(volumeOrchestrator.getVmNameOnVolume(volume));
    }

    @Test
    public void testGetVmNameOnVolumeSuccess() {
        VolumeVO volume = Mockito.mock(VolumeVO.class);
        Mockito.when(volume.getInstanceId()).thenReturn(MOCK_VM_ID);
        VirtualMachine vm = Mockito.mock(VirtualMachine.class);
        Mockito.when(vm.getInstanceName()).thenReturn(MOCK_VM_NAME);
        Mockito.when(entityMgr.findById(VirtualMachine.class, MOCK_VM_ID)).thenReturn(vm);
        Assert.assertEquals(MOCK_VM_NAME, volumeOrchestrator.getVmNameOnVolume(volume));
    }

    @Test
    public void testValidateVolumeSizeRangeValid() throws Exception {
        overrideDefaultConfigValue(VolumeOrchestrator.MaxVolumeSize, "2000");
        assertTrue(volumeOrchestrator.validateVolumeSizeRange(1024 * 1024 * 1024)); // 1 GiB
        assertTrue(volumeOrchestrator.validateVolumeSizeRange(2000 * 1024 * 1024 * 1024)); // 2 TiB
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testValidateVolumeSizeRangeTooSmall() {
        volumeOrchestrator.validateVolumeSizeRange(1024L); // Less than 1GiB
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testValidateVolumeSizeRangeNegative() {
        volumeOrchestrator.validateVolumeSizeRange(-10); // Negative size
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testValidateVolumeSizeRangeTooLarge() throws Exception {
        overrideDefaultConfigValue(VolumeOrchestrator.MaxVolumeSize, "100L");
        volumeOrchestrator.validateVolumeSizeRange(101);
    }

    @Test
    public void testCanVmRestartOnAnotherServerAllShared() {
        VolumeVO vol1 = Mockito.mock(VolumeVO.class);
        VolumeVO vol2 = Mockito.mock(VolumeVO.class);
        Mockito.when(vol1.getPoolId()).thenReturn(10L);
        Mockito.when(vol2.getPoolId()).thenReturn(20L);
        Mockito.when(vol1.isRecreatable()).thenReturn(false);
        Mockito.when(vol2.isRecreatable()).thenReturn(false);


        StoragePoolVO pool1 = Mockito.mock(StoragePoolVO.class);
        StoragePoolVO pool2 = Mockito.mock(StoragePoolVO.class);
        Mockito.when(pool1.getPoolType()).thenReturn(Storage.StoragePoolType.NetworkFilesystem); // Shared
        Mockito.when(pool2.getPoolType()).thenReturn(Storage.StoragePoolType.RBD); // Shared

        Mockito.when(volumeDao.findCreatedByInstance(MOCK_VM_ID)).thenReturn(List.of(vol1, vol2));
        Mockito.when(storagePoolDao.findById(10L)).thenReturn(pool1);
        Mockito.when(storagePoolDao.findById(20L)).thenReturn(pool2);


        assertTrue(volumeOrchestrator.canVmRestartOnAnotherServer(MOCK_VM_ID));
    }

    @Test
    public void testCanVmRestartOnAnotherServerOneLocalNotRecreatable() {
        VolumeVO vol1 = Mockito.mock(VolumeVO.class);
        VolumeVO vol2 = Mockito.mock(VolumeVO.class); // Local, not recreatable
        Mockito.when(vol1.getPoolId()).thenReturn(10L);
        Mockito.when(vol2.getPoolId()).thenReturn(30L);
        Mockito.when(vol1.isRecreatable()).thenReturn(false);
        Mockito.when(vol2.isRecreatable()).thenReturn(false); // Not recreatable

        StoragePoolVO pool1 = Mockito.mock(StoragePoolVO.class);
        StoragePoolVO pool2 = Mockito.mock(StoragePoolVO.class);
        Mockito.when(pool1.getPoolType()).thenReturn(Storage.StoragePoolType.NetworkFilesystem); // Shared
        Mockito.when(pool2.getPoolType()).thenReturn(Storage.StoragePoolType.LVM); // Local

        Mockito.when(volumeDao.findCreatedByInstance(MOCK_VM_ID)).thenReturn(List.of(vol1, vol2));
        Mockito.when(storagePoolDao.findById(10L)).thenReturn(pool1);
        Mockito.when(storagePoolDao.findById(30L)).thenReturn(pool2);

        Assert.assertFalse("VM restart should be false if a non-recreatable local disk exists",
                volumeOrchestrator.canVmRestartOnAnotherServer(MOCK_VM_ID));
    }

    @Test
    public void testCanVmRestartOnAnotherServerOneLocalRecreatable() {
        VolumeVO vol1 = Mockito.mock(VolumeVO.class);
        VolumeVO vol2 = Mockito.mock(VolumeVO.class); // Local, but recreatable
        Mockito.when(vol1.getPoolId()).thenReturn(10L);
        Mockito.when(vol2.getPoolId()).thenReturn(30L);
        Mockito.when(vol1.isRecreatable()).thenReturn(false);
        Mockito.when(vol2.isRecreatable()).thenReturn(true); // Recreatable

        StoragePoolVO pool1 = Mockito.mock(StoragePoolVO.class);
        StoragePoolVO pool2 = Mockito.mock(StoragePoolVO.class);
        Mockito.when(pool1.getPoolType()).thenReturn(Storage.StoragePoolType.NetworkFilesystem); // Shared

        Mockito.when(volumeDao.findCreatedByInstance(MOCK_VM_ID)).thenReturn(List.of(vol1, vol2));
        Mockito.when(storagePoolDao.findById(10L)).thenReturn(pool1);
        Mockito.when(storagePoolDao.findById(30L)).thenReturn(pool2);

        assertTrue("VM restart should be true if local disk is recreatable",
                volumeOrchestrator.canVmRestartOnAnotherServer(MOCK_VM_ID));
    }

    private void overrideDefaultConfigValue(final ConfigKey configKey, final String value) throws IllegalAccessException, NoSuchFieldException {
        final Field f = ConfigKey.class.getDeclaredField("_defaultValue");
        f.setAccessible(true);
        f.set(configKey, value);
    }

    @Test
    public void testStart() throws Exception {
        Mockito.when(configDepot.isNewConfig(VolumeAllocationAlgorithm)).thenReturn(true);
        overrideDefaultConfigValue(DeploymentClusterPlanner.VmAllocationAlgorithm, "firstfit");
        Mockito.when(configurationDao.update(Mockito.anyString(), Mockito.anyString())).thenReturn(true);
        volumeOrchestrator.start();
    }

    @Test
    public void testConfigKeys() {
        assertTrue(volumeOrchestrator.getConfigKeys().length > 0);
    }

    @Test
    public void getVolumeCheckpointPathsAndImageStoreUrlsTestReturnEmptyListsIfNotKVM() {
        Pair<List<String>, Set<String>> result = volumeOrchestrator.getVolumeCheckpointPathsAndImageStoreUrls(0, Hypervisor.HypervisorType.VMware);

        Assert.assertTrue(result.first().isEmpty());
        Assert.assertTrue(result.second().isEmpty());
    }

    @Test
    public void getVolumeCheckpointPathsAndImageStoreUrlsTestReturnCheckpointIfKVM() {
        SnapshotDataStoreVO snapshotDataStoreVO = new SnapshotDataStoreVO();
        snapshotDataStoreVO.setKvmCheckpointPath("Test");
        snapshotDataStoreVO.setRole(DataStoreRole.Primary);

        Mockito.doReturn(List.of(snapshotDataStoreVO)).when(snapshotDataStoreDaoMock).listReadyByVolumeIdAndCheckpointPathNotNull(Mockito.anyLong());

        Pair<List<String>, Set<String>> result = volumeOrchestrator.getVolumeCheckpointPathsAndImageStoreUrls(0, Hypervisor.HypervisorType.KVM);

        Assert.assertEquals("Test", result.first().get(0));
        Assert.assertTrue(result.second().isEmpty());
    }

    @Test
    public void getVolumeCheckpointPathsAndImageStoreUrlsTestReturnCheckpointIfKVMAndImageStore() {
        SnapshotDataStoreVO snapshotDataStoreVO = new SnapshotDataStoreVO();
        snapshotDataStoreVO.setKvmCheckpointPath("Test");
        snapshotDataStoreVO.setRole(DataStoreRole.Image);
        snapshotDataStoreVO.setDataStoreId(13);

        Mockito.doReturn(List.of(snapshotDataStoreVO)).when(snapshotDataStoreDaoMock).listReadyByVolumeIdAndCheckpointPathNotNull(Mockito.anyLong());

        ImageStoreVO imageStoreVO = new ImageStoreVO();
        imageStoreVO.setUrl("URL");
        Mockito.doReturn(imageStoreVO).when(imageStoreDaoMock).findById(Mockito.anyLong());

        Pair<List<String>, Set<String>> result = volumeOrchestrator.getVolumeCheckpointPathsAndImageStoreUrls(0, Hypervisor.HypervisorType.KVM);

        Assert.assertEquals("Test", result.first().get(0));
        Assert.assertTrue(result.second().contains("URL"));
        Assert.assertEquals(1, result.second().size());
    }

}

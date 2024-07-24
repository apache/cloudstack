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

import com.cloud.hypervisor.Hypervisor;
import com.cloud.offering.DiskOffering;
import com.cloud.storage.Storage;
import com.cloud.storage.Volume;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.user.Account;
import org.apache.cloudstack.engine.subsystem.api.storage.DataObject;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreDriver;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeService;
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

import com.cloud.configuration.Resource;
import com.cloud.exception.StorageAccessException;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.Volume.Type;
import com.cloud.user.ResourceLimitService;
import com.cloud.utils.exception.CloudRuntimeException;

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

    @Spy
    @InjectMocks
    private VolumeOrchestrator volumeOrchestrator = new VolumeOrchestrator();

    private static final Long DEFAULT_ACCOUNT_PS_RESOURCE_COUNT = 100L;
    private Long accountPSResourceCount;

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
}

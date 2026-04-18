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
package org.apache.cloudstack.storage.datastore.driver;

import com.linbit.linstor.api.ApiException;
import com.linbit.linstor.api.DevelopersApi;
import com.linbit.linstor.api.model.AutoSelectFilter;
import com.linbit.linstor.api.model.LayerType;
import com.linbit.linstor.api.model.ResourceGroup;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.cloud.agent.api.to.DataObjectType;
import com.cloud.storage.DataStoreRole;
import org.apache.cloudstack.engine.subsystem.api.storage.DataObject;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreCapabilities;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.datastore.util.LinstorUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class LinstorPrimaryDataStoreDriverImplTest {

    private DevelopersApi api;

    @Mock
    private PrimaryDataStoreDao _storagePoolDao;

    @InjectMocks
    private LinstorPrimaryDataStoreDriverImpl linstorPrimaryDataStoreDriver;

    @Before
    public void setUp() {
        api = mock(DevelopersApi.class);
    }

    @Test
    public void testGetEncryptedLayerList() throws ApiException  {
        ResourceGroup dfltRscGrp = new ResourceGroup();
        dfltRscGrp.setName("DfltRscGrp");

        ResourceGroup bCacheRscGrp = new ResourceGroup();
        bCacheRscGrp.setName("BcacheGrp");
        AutoSelectFilter asf = new AutoSelectFilter();
        asf.setLayerStack(Arrays.asList(LayerType.DRBD.name(), LayerType.BCACHE.name(), LayerType.STORAGE.name()));
        asf.setStoragePool("nvmePool");
        bCacheRscGrp.setSelectFilter(asf);

        ResourceGroup encryptedGrp = new ResourceGroup();
        encryptedGrp.setName("EncryptedGrp");
        AutoSelectFilter asf2 = new AutoSelectFilter();
        asf2.setLayerStack(Arrays.asList(LayerType.DRBD.name(), LayerType.LUKS.name(), LayerType.STORAGE.name()));
        asf2.setStoragePool("ssdPool");
        encryptedGrp.setSelectFilter(asf2);

        when(api.resourceGroupList(Collections.singletonList("DfltRscGrp"), Collections.emptyList(), null, null))
                .thenReturn(Collections.singletonList(dfltRscGrp));
        when(api.resourceGroupList(Collections.singletonList("BcacheGrp"), Collections.emptyList(), null, null))
                .thenReturn(Collections.singletonList(bCacheRscGrp));
        when(api.resourceGroupList(Collections.singletonList("EncryptedGrp"), Collections.emptyList(), null, null))
                .thenReturn(Collections.singletonList(encryptedGrp));

        List<LayerType> layers = LinstorUtil.getEncryptedLayerList(api, "DfltRscGrp");
        Assert.assertEquals(Arrays.asList(LayerType.DRBD, LayerType.LUKS, LayerType.STORAGE), layers);

        layers = LinstorUtil.getEncryptedLayerList(api, "BcacheGrp");
        Assert.assertEquals(Arrays.asList(LayerType.DRBD, LayerType.BCACHE, LayerType.LUKS, LayerType.STORAGE), layers);

        layers = LinstorUtil.getEncryptedLayerList(api, "EncryptedGrp");
        Assert.assertEquals(Arrays.asList(LayerType.DRBD, LayerType.LUKS, LayerType.STORAGE), layers);
    }

    @Test
    public void testGetCapabilitiesIncludesCreateTemplateFromSnapshot() {
        Map<String, String> caps = linstorPrimaryDataStoreDriver.getCapabilities();

        Assert.assertTrue("Linstor should advertise CAN_CREATE_TEMPLATE_FROM_SNAPSHOT",
                Boolean.parseBoolean(caps.get(DataStoreCapabilities.CAN_CREATE_TEMPLATE_FROM_SNAPSHOT.toString())));
    }

    @Test
    public void testCanCopySnapshotToVolumeOnSamePrimary() {
        DataStore primaryStore = mock(DataStore.class);
        when(primaryStore.getRole()).thenReturn(DataStoreRole.Primary);
        when(primaryStore.getId()).thenReturn(1L);

        SnapshotInfo snapshot = mock(SnapshotInfo.class);
        when(snapshot.getType()).thenReturn(DataObjectType.SNAPSHOT);
        when(snapshot.getDataStore()).thenReturn(primaryStore);

        VolumeInfo volume = mock(VolumeInfo.class);
        when(volume.getType()).thenReturn(DataObjectType.VOLUME);
        when(volume.getDataStore()).thenReturn(primaryStore);

        StoragePoolVO pool = mock(StoragePoolVO.class);
        when(pool.getStorageProviderName()).thenReturn(LinstorUtil.PROVIDER_NAME);
        when(_storagePoolDao.findById(1L)).thenReturn(pool);

        Assert.assertTrue("canCopy should return true for SNAPSHOT -> VOLUME on same Linstor primary",
                linstorPrimaryDataStoreDriver.canCopy(snapshot, volume));
    }

    @Test
    public void testCanCopySnapshotToVolumeRejectsNonLinstor() {
        DataStore primaryStore = mock(DataStore.class);
        when(primaryStore.getRole()).thenReturn(DataStoreRole.Primary);
        when(primaryStore.getId()).thenReturn(1L);

        SnapshotInfo snapshot = mock(SnapshotInfo.class);
        when(snapshot.getType()).thenReturn(DataObjectType.SNAPSHOT);
        when(snapshot.getDataStore()).thenReturn(primaryStore);

        VolumeInfo volume = mock(VolumeInfo.class);
        when(volume.getType()).thenReturn(DataObjectType.VOLUME);
        when(volume.getDataStore()).thenReturn(primaryStore);

        StoragePoolVO pool = mock(StoragePoolVO.class);
        when(pool.getStorageProviderName()).thenReturn("SomeOtherProvider");
        when(_storagePoolDao.findById(1L)).thenReturn(pool);

        Assert.assertFalse("canCopy should return false for non-Linstor storage",
                linstorPrimaryDataStoreDriver.canCopy(snapshot, volume));
    }

    @Test
    public void testCanCopySnapshotToVolumeRejectsCrossPrimary() {
        DataStore srcStore = mock(DataStore.class);
        when(srcStore.getRole()).thenReturn(DataStoreRole.Primary);
        when(srcStore.getId()).thenReturn(1L);

        DataStore destStore = mock(DataStore.class);
        when(destStore.getRole()).thenReturn(DataStoreRole.Primary);
        when(destStore.getId()).thenReturn(2L);

        SnapshotInfo snapshot = mock(SnapshotInfo.class);
        when(snapshot.getType()).thenReturn(DataObjectType.SNAPSHOT);
        when(snapshot.getDataStore()).thenReturn(srcStore);

        VolumeInfo volume = mock(VolumeInfo.class);
        when(volume.getType()).thenReturn(DataObjectType.VOLUME);
        when(volume.getDataStore()).thenReturn(destStore);

        Assert.assertFalse("canCopy should return false for SNAPSHOT -> VOLUME across different primary stores",
                linstorPrimaryDataStoreDriver.canCopy(snapshot, volume));
    }

    @Test
    public void testCanCopySnapshotToVolumeRejectsImageDest() {
        DataStore primaryStore = mock(DataStore.class);
        when(primaryStore.getRole()).thenReturn(DataStoreRole.Primary);

        DataStore imageStore = mock(DataStore.class);
        when(imageStore.getRole()).thenReturn(DataStoreRole.Image);

        SnapshotInfo snapshot = mock(SnapshotInfo.class);
        when(snapshot.getType()).thenReturn(DataObjectType.SNAPSHOT);
        when(snapshot.getDataStore()).thenReturn(primaryStore);

        VolumeInfo volume = mock(VolumeInfo.class);
        when(volume.getType()).thenReturn(DataObjectType.VOLUME);
        when(volume.getDataStore()).thenReturn(imageStore);

        Assert.assertFalse("canCopy should return false when destination is Image store",
                linstorPrimaryDataStoreDriver.canCopy(snapshot, volume));
    }
}

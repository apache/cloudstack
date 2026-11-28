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
import com.linbit.linstor.api.model.DrbdVolume;
import com.linbit.linstor.api.model.LayerType;
import com.linbit.linstor.api.model.Properties;
import com.linbit.linstor.api.model.ResourceDefinition;
import com.linbit.linstor.api.model.ResourceGroup;
import com.linbit.linstor.api.model.ResourceWithVolumes;
import com.linbit.linstor.api.model.Volume;
import com.linbit.linstor.api.model.VolumeDefinition;
import com.linbit.linstor.api.model.VolumeLayer;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine;
import org.apache.cloudstack.storage.command.CopyCmdAnswer;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreVO;
import org.apache.cloudstack.storage.datastore.util.LinstorUtil;
import org.apache.cloudstack.storage.to.SnapshotObjectTO;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.storage.DataStoreRole;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.user.ResourceLimitService;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class LinstorPrimaryDataStoreDriverImplTest {

    private DevelopersApi api;

    @Mock
    private SnapshotDataStoreDao snapshotStoreDao;

    @Mock
    private VolumeDao volumeDao;

    @Mock
    private DiskOfferingDao diskOfferingDao;

    @Mock
    private ResourceLimitService resourceLimitMgr;

    @InjectMocks
    private LinstorPrimaryDataStoreDriverImpl linstorPrimaryDataStoreDriver;

    @Before
    public void setUp() {
        api = mock(DevelopersApi.class);
    }

    private SnapshotDataStoreVO mockDestRef(long parentSnapshotId, long size) {
        SnapshotDataStoreVO destRef = mock(SnapshotDataStoreVO.class);
        when(destRef.getParentSnapshotId()).thenReturn(parentSnapshotId);
        if (parentSnapshotId > 0) {
            Mockito.lenient().when(destRef.getRole()).thenReturn(DataStoreRole.Image);
            Mockito.lenient().when(destRef.getDataStoreId()).thenReturn(10L);
            Mockito.lenient().when(destRef.getSize()).thenReturn(size);
        }
        return destRef;
    }

    private SnapshotDataStoreVO mockParentRef(String installPath, ObjectInDataStoreStateMachine.State state, long size) {
        SnapshotDataStoreVO parentRef = mock(SnapshotDataStoreVO.class);
        Mockito.lenient().when(parentRef.getInstallPath()).thenReturn(installPath);
        Mockito.lenient().when(parentRef.getState()).thenReturn(state);
        Mockito.lenient().when(parentRef.getSize()).thenReturn(size);
        when(snapshotStoreDao.findByStoreSnapshot(DataStoreRole.Image, 10L, 2L)).thenReturn(parentRef);
        return parentRef;
    }

    @Test
    public void testGetIncrementalParentPathNoDestRef() {
        Assert.assertNull(linstorPrimaryDataStoreDriver.getIncrementalParentPath(null));
    }

    @Test
    public void testGetIncrementalParentPathNoParentLink() {
        Assert.assertNull(linstorPrimaryDataStoreDriver.getIncrementalParentPath(mockDestRef(0, 100L)));
    }

    @Test
    public void testGetIncrementalParentPathParentRefMissing() {
        when(snapshotStoreDao.findByStoreSnapshot(DataStoreRole.Image, 10L, 2L)).thenReturn(null);
        Assert.assertNull(linstorPrimaryDataStoreDriver.getIncrementalParentPath(mockDestRef(2L, 100L)));
    }

    @Test
    public void testGetIncrementalParentPathParentNotReady() {
        mockParentRef("snapshots/2/5/parent", ObjectInDataStoreStateMachine.State.Destroyed, 100L);
        Assert.assertNull(linstorPrimaryDataStoreDriver.getIncrementalParentPath(mockDestRef(2L, 100L)));
    }

    @Test
    public void testGetIncrementalParentPathVolumeResized() {
        mockParentRef("snapshots/2/5/parent", ObjectInDataStoreStateMachine.State.Ready, 50L);
        Assert.assertNull(linstorPrimaryDataStoreDriver.getIncrementalParentPath(mockDestRef(2L, 100L)));
    }

    @Test
    public void testGetIncrementalParentPathReadyParent() {
        mockParentRef("snapshots/2/5/parent", ObjectInDataStoreStateMachine.State.Ready, 100L);
        Assert.assertEquals("snapshots/2/5/parent",
            linstorPrimaryDataStoreDriver.getIncrementalParentPath(mockDestRef(2L, 100L)));
    }

    @Test
    public void testClearChainParentIfFullCopyClearsOnFullBackup() {
        SnapshotDataStoreVO destRef = mock(SnapshotDataStoreVO.class);
        when(destRef.getParentSnapshotId()).thenReturn(2L);

        SnapshotObjectTO to = new SnapshotObjectTO();
        to.setKvmIncrementalSnapshot(false);
        linstorPrimaryDataStoreDriver.clearChainParentIfFullCopy(destRef, new CopyCmdAnswer(to));

        Mockito.verify(destRef).setParentSnapshotId(0);
        Mockito.verify(snapshotStoreDao).update(Mockito.anyLong(), Mockito.eq(destRef));
    }

    @Test
    public void testClearChainParentIfFullCopyKeepsLinkOnIncremental() {
        SnapshotDataStoreVO destRef = mock(SnapshotDataStoreVO.class);
        when(destRef.getParentSnapshotId()).thenReturn(2L);

        SnapshotObjectTO to = new SnapshotObjectTO();
        to.setKvmIncrementalSnapshot(true);
        linstorPrimaryDataStoreDriver.clearChainParentIfFullCopy(destRef, new CopyCmdAnswer(to));

        Mockito.verify(destRef, Mockito.never()).setParentSnapshotId(Mockito.anyLong());
        Mockito.verify(snapshotStoreDao, Mockito.never()).update(Mockito.anyLong(), Mockito.any());
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

    /**
     * A 512 MiB volume definition whose DRBD device has 540909568 bytes (LVM-thin rounds the storage up).
     */
    private void mockLinstorVolume(String rscName) throws ApiException {
        VolumeDefinition vlmDfn = new VolumeDefinition();
        vlmDfn.setSizeKib(524288L);
        ResourceDefinition rscDfn = new ResourceDefinition();
        rscDfn.setName(rscName);
        rscDfn.addVolumeDefinitionsItem(vlmDfn);
        rscDfn.setProps(new Properties());
        when(api.resourceDefinitionList(Collections.singletonList(rscName), true, null, null, null))
                .thenReturn(Collections.singletonList(rscDfn));

        DrbdVolume drbdVolume = new DrbdVolume();
        drbdVolume.setUsableSizeKib(528232L);
        VolumeLayer layer = new VolumeLayer();
        layer.setType(LayerType.DRBD);
        layer.setData(drbdVolume);
        Volume volume = new Volume();
        volume.addLayerDataListItem(layer);
        ResourceWithVolumes rsc = new ResourceWithVolumes();
        rsc.setNodeName("nodeA");
        rsc.setVolumes(Collections.singletonList(volume));
        when(api.viewResources(Collections.emptyList(), Collections.singletonList(rscName), Collections.emptyList(),
                null, null, null)).thenReturn(Collections.singletonList(rsc));
    }

    private VolumeVO mockVolume(long size) {
        VolumeVO volume = mock(VolumeVO.class);
        Mockito.lenient().when(volume.getId()).thenReturn(7L);
        Mockito.lenient().when(volume.getAccountId()).thenReturn(2L);
        Mockito.lenient().when(volume.isDisplayVolume()).thenReturn(true);
        Mockito.lenient().when(volume.getDiskOfferingId()).thenReturn(3L);
        when(volume.getSize()).thenReturn(size);
        return volume;
    }

    @Test
    public void testRecordDeviceSizeRecordsTheLargerDevice() throws ApiException {
        mockLinstorVolume("cs-vol");
        VolumeVO volume = mockVolume(536870912L);
        DiskOfferingVO diskOffering = mock(DiskOfferingVO.class);
        when(diskOfferingDao.findById(3L)).thenReturn(diskOffering);

        long size = linstorPrimaryDataStoreDriver.recordDeviceSize(api, volume, "cs-vol", true);

        Assert.assertEquals(540909568L, size);
        Mockito.verify(volume).setSize(540909568L);
        Mockito.verify(volumeDao).update(7L, volume);
        Mockito.verify(resourceLimitMgr).incrementVolumePrimaryStorageResourceCount(2L, true, 4038656L, diskOffering);
    }

    @Test
    public void testRecordDeviceSizeWithoutResourceCount() throws ApiException {
        // a resize counts the recorded size itself
        mockLinstorVolume("cs-vol");
        VolumeVO volume = mockVolume(536870912L);

        Assert.assertEquals(540909568L, linstorPrimaryDataStoreDriver.recordDeviceSize(api, volume, "cs-vol", false));
        Mockito.verify(volumeDao).update(7L, volume);
        Mockito.verifyNoInteractions(resourceLimitMgr);
    }

    @Test
    public void testRecordDeviceSizeKeepsRecordedDeviceSize() throws ApiException {
        mockLinstorVolume("cs-vol");
        VolumeVO volume = mockVolume(540909568L);

        Assert.assertEquals(540909568L, linstorPrimaryDataStoreDriver.recordDeviceSize(api, volume, "cs-vol", true));
        Mockito.verify(volume, Mockito.never()).setSize(Mockito.anyLong());
        Mockito.verifyNoInteractions(volumeDao, resourceLimitMgr);
    }

    @Test
    public void testRecordDeviceSizeKeepsSizeIfLinstorFails() throws ApiException {
        when(api.resourceDefinitionList(Collections.singletonList("cs-vol"), true, null, null, null))
                .thenThrow(new ApiException(500, "controller unreachable"));
        VolumeVO volume = mockVolume(536870912L);

        Assert.assertEquals(536870912L, linstorPrimaryDataStoreDriver.recordDeviceSize(api, volume, "cs-vol", true));
        Mockito.verifyNoInteractions(volumeDao, resourceLimitMgr);
    }
}

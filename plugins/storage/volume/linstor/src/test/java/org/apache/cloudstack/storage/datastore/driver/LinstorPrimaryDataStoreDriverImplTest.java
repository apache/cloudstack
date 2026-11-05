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

import com.linbit.linstor.api.ApiConsts;
import com.linbit.linstor.api.ApiException;
import com.linbit.linstor.api.DevelopersApi;
import com.linbit.linstor.api.model.ApiCallRc;
import com.linbit.linstor.api.model.ApiCallRcList;
import com.linbit.linstor.api.model.AutoSelectFilter;
import com.linbit.linstor.api.model.CbtExtents;
import com.linbit.linstor.api.model.CbtSession;
import com.linbit.linstor.api.model.CbtSessionStart;
import com.linbit.linstor.api.model.CbtSessionVolume;
import com.linbit.linstor.api.model.ControllerVersion;
import com.linbit.linstor.api.model.CreateMultiSnapshotRequest;
import com.linbit.linstor.api.model.LayerType;
import com.linbit.linstor.api.model.ResourceGroup;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.cloud.agent.api.to.DataObjectType;
import com.cloud.host.Host;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.StoragePool;
import com.cloud.utils.exception.CloudRuntimeException;

import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreCapabilities;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeBackupExport;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.storage.command.CopyCmdAnswer;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreVO;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.datastore.util.LinstorUtil;
import org.apache.cloudstack.storage.to.SnapshotObjectTO;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class LinstorPrimaryDataStoreDriverImplTest {

    private DevelopersApi api;

    @Mock
    private PrimaryDataStoreDao _storagePoolDao;

    @Mock
    private SnapshotDataStoreDao snapshotStoreDao;

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

    @Test
    public void testGetCapabilitiesIncludesCreateTemplateFromSnapshotMatchesSystemSnapshot() {
        Map<String, String> caps = linstorPrimaryDataStoreDriver.getCapabilities();

        Assert.assertEquals(
                "CAN_CREATE_TEMPLATE_FROM_SNAPSHOT should match STORAGE_SYSTEM_SNAPSHOT",
                caps.get(DataStoreCapabilities.STORAGE_SYSTEM_SNAPSHOT.toString()),
                caps.get(DataStoreCapabilities.CAN_CREATE_TEMPLATE_FROM_SNAPSHOT.toString()));
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

    private LinstorPrimaryDataStoreDriverImpl driverWithApi(StoragePool pool) {
        LinstorPrimaryDataStoreDriverImpl driver = Mockito.spy(linstorPrimaryDataStoreDriver);
        Mockito.doReturn(api).when(driver).getLinstorAPI(pool);
        return driver;
    }

    @Test
    public void testCanExportVolumesForBackupCachesOnlyAnsweredVersions() throws ApiException {
        StoragePool pool = mock(StoragePool.class);
        when(pool.getId()).thenReturn(1L);
        LinstorPrimaryDataStoreDriverImpl driver = driverWithApi(pool);
        ControllerVersion version = new ControllerVersion();
        version.setRestApiVersion("1.30.0");
        when(api.controllerVersion()).thenThrow(new ApiException(503, "unavailable")).thenReturn(version);

        // a controller that can't be asked isn't cached as not supporting CBT
        Assert.assertFalse(driver.canExportVolumesForBackup(pool));
        Assert.assertTrue(driver.canExportVolumesForBackup(pool));
        Assert.assertTrue(driver.canExportVolumesForBackup(pool));
        Mockito.verify(api, Mockito.times(2)).controllerVersion();
    }

    private VolumeInfo mockBackupVolume(long id, String path, long size) {
        VolumeInfo volume = mock(VolumeInfo.class);
        Mockito.lenient().when(volume.getId()).thenReturn(id);
        when(volume.getPath()).thenReturn(path);
        Mockito.lenient().when(volume.getSize()).thenReturn(size);
        return volume;
    }

    private Host mockHost(long id, String name) {
        Host host = mock(Host.class);
        Mockito.lenient().when(host.getId()).thenReturn(id);
        when(host.getName()).thenReturn(name);
        return host;
    }

    private static CbtSession cbtSession(String rscName, String node, String devicePath, String cbtId) {
        CbtSessionVolume volume = new CbtSessionVolume();
        volume.setVolumeNumber(0);
        volume.setDevicePath(devicePath);
        volume.setVolumeSize(1077665792L);
        CbtSession session = new CbtSession();
        session.setResourceName(rscName);
        session.setNode(node);
        session.setCbtId(cbtId);
        session.setVolumes(Collections.singletonList(volume));
        return session;
    }

    private static ApiCallRcList answers(long retCode, String message) {
        ApiCallRc rc = new ApiCallRc();
        rc.setRetCode(retCode);
        rc.setMessage(message);
        ApiCallRcList list = new ApiCallRcList();
        list.add(rc);
        return list;
    }

    @Test
    public void testStartBackupExportTakesCbtMultiSnapshotAndReadsOnDiskfulHosts() throws ApiException {
        StoragePool pool = mock(StoragePool.class);
        LinstorPrimaryDataStoreDriverImpl driver = driverWithApi(pool);
        VolumeInfo root = mockBackupVolume(1L, "vol-a", 1073741824L);
        VolumeInfo data = mockBackupVolume(2L, "vol-b", 2147483648L);
        Host hostA = mockHost(11L, "node-a");
        Host hostB = mockHost(12L, "node-b");
        Mockito.doReturn(hostA).when(driver).getDiskfulHost(api, pool, "cs-vol-a");
        Mockito.doReturn(hostB).when(driver).getDiskfulHost(api, pool, "cs-vol-b");
        when(api.createMultiSnapshot(Mockito.any())).thenReturn(answers(ApiConsts.MASK_SUCCESS, "snapshot created"));
        when(api.snapshotCbtSessionStart(Mockito.eq("cs-vol-a"), Mockito.eq("ckp-1"), Mockito.any()))
                .thenReturn(cbtSession("cs-vol-a_cbt_1", "node-a", "/dev/drbd1004", "CBT-A"));
        when(api.snapshotCbtSessionStart(Mockito.eq("cs-vol-b"), Mockito.eq("ckp-1"), Mockito.any()))
                .thenReturn(cbtSession("cs-vol-b_cbt_1", "node-b", "/dev/drbd1005", "CBT-B"));

        List<VolumeBackupExport> exports = driver.startBackupExport(pool, "ckp-1", Arrays.asList(root, data),
                Collections.emptyMap());

        ArgumentCaptor<CreateMultiSnapshotRequest> snapshotCaptor = ArgumentCaptor.forClass(CreateMultiSnapshotRequest.class);
        Mockito.verify(api).createMultiSnapshot(snapshotCaptor.capture());
        Assert.assertEquals(2, snapshotCaptor.getValue().getSnapshots().size());
        snapshotCaptor.getValue().getSnapshots().forEach(snap -> {
            Assert.assertEquals("ckp-1", snap.getName());
            Assert.assertEquals(Collections.singletonList("CBT"), snap.getFlags());
        });
        Assert.assertEquals("cs-vol-a", snapshotCaptor.getValue().getSnapshots().get(0).getResourceName());
        Assert.assertEquals("cs-vol-b", snapshotCaptor.getValue().getSnapshots().get(1).getResourceName());

        ArgumentCaptor<CbtSessionStart> sessionCaptor = ArgumentCaptor.forClass(CbtSessionStart.class);
        Mockito.verify(api).snapshotCbtSessionStart(Mockito.eq("cs-vol-a"), Mockito.eq("ckp-1"), sessionCaptor.capture());
        Assert.assertEquals("node-a", sessionCaptor.getValue().getNode());

        Assert.assertEquals(2, exports.size());
        VolumeBackupExport rootExport = exports.get(0);
        Assert.assertEquals(1L, rootExport.getVolumeId());
        Assert.assertEquals(11L, rootExport.getHostId());
        Assert.assertEquals("/dev/drbd1004", rootExport.getDevicePath());
        // clipped to the CloudStack volume size, not the larger DRBD device
        Assert.assertEquals(1073741824L, rootExport.getSizeBytes());
        Assert.assertEquals("CBT-A", rootExport.getChangeTrackingId());
        Assert.assertEquals(12L, exports.get(1).getHostId());
        Assert.assertEquals("CBT-B", exports.get(1).getChangeTrackingId());
    }

    private static CbtExtents cbtExtents(String baseCbtId) {
        CbtExtents extents = new CbtExtents();
        extents.setBaseCbtId(baseCbtId);
        extents.setVolumeSize(1077665792L);
        return extents;
    }

    private LinstorPrimaryDataStoreDriverImpl driverForSessions(StoragePool pool, String... rscNames) throws ApiException {
        LinstorPrimaryDataStoreDriverImpl driver = driverWithApi(pool);
        Host host = mockHost(11L, "node-a");
        when(api.createMultiSnapshot(Mockito.any())).thenReturn(answers(ApiConsts.MASK_SUCCESS, "snapshot created"));
        for (String rscName : rscNames) {
            Mockito.doReturn(host).when(driver).getDiskfulHost(api, pool, rscName);
            when(api.snapshotCbtSessionStart(Mockito.eq(rscName), Mockito.eq("ckp-2"), Mockito.any()))
                    .thenReturn(cbtSession(rscName + "_cbt_1", "node-a", "/dev/drbd1004", "CBT-NEW-" + rscName));
        }
        return driver;
    }

    @Test
    public void testStartBackupExportIncrementalOnlyIfChangesAreSinceLastBackup() throws ApiException {
        StoragePool pool = mock(StoragePool.class);
        LinstorPrimaryDataStoreDriverImpl driver = driverForSessions(pool, "cs-vol-a", "cs-vol-b");
        VolumeInfo root = mockBackupVolume(1L, "vol-a", 1073741824L);
        VolumeInfo data = mockBackupVolume(2L, "vol-b", 2147483648L);
        when(api.snapshotCbtSessionExtents("cs-vol-a", "ckp-2", 0, 0L, 1, "dirty")).thenReturn(cbtExtents("CBT-A"));
        // tracking restarted since the last backup, e.g. by a failed backup
        when(api.snapshotCbtSessionExtents("cs-vol-b", "ckp-2", 0, 0L, 1, "dirty")).thenReturn(cbtExtents("CBT-FAILED"));
        when(api.snapshotCbtSessionExtents("cs-vol-b", "ckp-2", 0, 0L, 1, "zero")).thenReturn(cbtExtents(null));
        Map<Long, String> lastIds = new HashMap<>();
        lastIds.put(1L, "CBT-A");
        lastIds.put(2L, "CBT-B");

        List<VolumeBackupExport> exports = driver.startBackupExport(pool, "ckp-2", Arrays.asList(root, data), lastIds);

        VolumeBackupExport rootExport = exports.get(0);
        Assert.assertTrue(rootExport.isIncremental());
        Assert.assertEquals("CBT-NEW-cs-vol-a", rootExport.getChangeTrackingId());
        Assert.assertEquals("cs-vol-a", rootExport.getExtentsSource().get(LinstorUtil.CBT_EXTENTS_RESOURCE));
        Assert.assertEquals("ckp-2", rootExport.getExtentsSource().get(LinstorUtil.CBT_EXTENTS_SNAPSHOT));
        Assert.assertEquals("dirty", rootExport.getExtentsSource().get(LinstorUtil.CBT_EXTENTS_MODE));
        Assert.assertEquals("CBT-A", rootExport.getExtentsSource().get(LinstorUtil.CBT_EXTENTS_BASE_CBT_ID));
        VolumeBackupExport dataExport = exports.get(1);
        Assert.assertFalse(dataExport.isIncremental());
        Assert.assertEquals("CBT-NEW-cs-vol-b", dataExport.getChangeTrackingId());
        // a full backup still skips the never written extents
        Assert.assertEquals("zero", dataExport.getExtentsSource().get(LinstorUtil.CBT_EXTENTS_MODE));
        Assert.assertNull(dataExport.getExtentsSource().get(LinstorUtil.CBT_EXTENTS_BASE_CBT_ID));
    }

    @Test
    public void testStartBackupExportFullWithoutUsableChanges() throws ApiException {
        StoragePool pool = mock(StoragePool.class);
        LinstorPrimaryDataStoreDriverImpl driver = driverForSessions(pool, "cs-vol-a", "cs-vol-b");
        VolumeInfo root = mockBackupVolume(1L, "vol-a", 1073741824L);
        VolumeInfo data = mockBackupVolume(2L, "vol-b", 2147483648L);
        when(api.snapshotCbtSessionExtents("cs-vol-a", "ckp-2", 0, 0L, 1, "dirty"))
                .thenThrow(new ApiException(500, "The snapshot holds no changes to rely on"));
        // e.g. drbd-utils without the zero extents
        when(api.snapshotCbtSessionExtents("cs-vol-a", "ckp-2", 0, 0L, 1, "zero"))
                .thenThrow(new ApiException(500, "The snapshot holds no record of the never written extents"));
        when(api.snapshotCbtSessionExtents("cs-vol-b", "ckp-2", 0, 0L, 1, "zero")).thenReturn(cbtExtents(null));

        List<VolumeBackupExport> exports = driver.startBackupExport(pool, "ckp-2", Arrays.asList(root, data),
                Collections.singletonMap(1L, "CBT-A"));

        Assert.assertFalse(exports.get(0).isIncremental());
        Assert.assertNull(exports.get(0).getExtentsSource());
        Assert.assertFalse(exports.get(1).isIncremental());
        Assert.assertEquals("zero", exports.get(1).getExtentsSource().get(LinstorUtil.CBT_EXTENTS_MODE));
        // without a last backup there is nothing to compare
        Mockito.verify(api, Mockito.never()).snapshotCbtSessionExtents("cs-vol-b", "ckp-2", 0, 0L, 1, "dirty");
    }

    @Test
    public void testStartBackupExportFailsWithoutSessionsIfSnapshotFails() throws ApiException {
        StoragePool pool = mock(StoragePool.class);
        LinstorPrimaryDataStoreDriverImpl driver = driverWithApi(pool);
        VolumeInfo root = mockBackupVolume(1L, "vol-a", 1073741824L);
        when(api.createMultiSnapshot(Mockito.any()))
                .thenReturn(answers(ApiConsts.FAIL_CBT_NOT_SUPPORTED, "CBT needs DRBD on LVM, LVM-thin, ZFS or ZFS-thin"));

        CloudRuntimeException ex = Assert.assertThrows(CloudRuntimeException.class,
                () -> driver.startBackupExport(pool, "ckp-1", Collections.singletonList(root), Collections.emptyMap()));

        Assert.assertTrue(ex.getMessage().contains("CBT needs DRBD"));
        Mockito.verify(api, Mockito.never()).snapshotCbtSessionStart(Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    public void testFinishBackupExportFinishesSessionsBeforeDeletingSnapshots() throws ApiException {
        StoragePool pool = mock(StoragePool.class);
        LinstorPrimaryDataStoreDriverImpl driver = driverWithApi(pool);
        VolumeInfo root = mockBackupVolume(1L, "vol-a", 1073741824L);
        when(api.snapshotCbtSessionFinish("cs-vol-a", "ckp-1")).thenReturn(answers(ApiConsts.MASK_SUCCESS, "finished"));
        when(api.resourceSnapshotDelete("cs-vol-a", "ckp-1", Collections.emptyList(), null))
                .thenReturn(answers(ApiConsts.MASK_SUCCESS, "deleted"));

        driver.finishBackupExport(pool, "ckp-1", Collections.singletonList(root));

        InOrder inOrder = Mockito.inOrder(api);
        inOrder.verify(api).snapshotCbtSessionFinish("cs-vol-a", "ckp-1");
        inOrder.verify(api).resourceSnapshotDelete("cs-vol-a", "ckp-1", Collections.emptyList(), null);
    }

    @Test
    public void testFinishBackupExportIgnoresAlreadyReleasedParts() throws ApiException {
        StoragePool pool = mock(StoragePool.class);
        LinstorPrimaryDataStoreDriverImpl driver = driverWithApi(pool);
        VolumeInfo root = mockBackupVolume(1L, "vol-a", 1073741824L);
        when(api.snapshotCbtSessionFinish("cs-vol-a", "ckp-1")).thenThrow(new ApiException(404, "not found"));
        when(api.resourceSnapshotDelete("cs-vol-a", "ckp-1", Collections.emptyList(), null))
                .thenThrow(new ApiException(404, "not found"));

        driver.finishBackupExport(pool, "ckp-1", Collections.singletonList(root));
    }

    @Test
    public void testFinishBackupExportReportsSnapshotDeleteErrorAndContinues() throws ApiException {
        StoragePool pool = mock(StoragePool.class);
        LinstorPrimaryDataStoreDriverImpl driver = driverWithApi(pool);
        VolumeInfo root = mockBackupVolume(1L, "vol-a", 1073741824L);
        VolumeInfo data = mockBackupVolume(2L, "vol-b", 1073741824L);
        when(api.snapshotCbtSessionFinish(Mockito.anyString(), Mockito.eq("ckp-1")))
                .thenReturn(answers(ApiConsts.MASK_SUCCESS, "finished"));
        when(api.resourceSnapshotDelete("cs-vol-a", "ckp-1", Collections.emptyList(), null))
                .thenReturn(answers(ApiConsts.FAIL_IN_USE, "snapshot in use"));
        when(api.resourceSnapshotDelete("cs-vol-b", "ckp-1", Collections.emptyList(), null))
                .thenReturn(answers(ApiConsts.MASK_SUCCESS, "deleted"));

        CloudRuntimeException ex = Assert.assertThrows(CloudRuntimeException.class,
                () -> driver.finishBackupExport(pool, "ckp-1", Arrays.asList(root, data)));

        Assert.assertTrue(ex.getMessage().contains("cs-vol-a/ckp-1: snapshot in use"));
        Mockito.verify(api).resourceSnapshotDelete("cs-vol-b", "ckp-1", Collections.emptyList(), null);
    }

    @Test
    public void testPrepareVolumeForRestoreUsesDiskfulHost() throws ApiException {
        StoragePool pool = mock(StoragePool.class);
        LinstorPrimaryDataStoreDriverImpl driver = driverWithApi(pool);
        VolumeInfo volume = mockBackupVolume(3L, "vol-c", 1073741824L);
        Host host = mock(Host.class);
        when(host.getId()).thenReturn(13L);
        Mockito.doReturn(host).when(driver).getDiskfulHost(api, pool, "cs-vol-c");
        try (MockedStatic<LinstorUtil> linstorUtil = Mockito.mockStatic(LinstorUtil.class)) {
            linstorUtil.when(() -> LinstorUtil.getDevicePath(api, "cs-vol-c")).thenReturn("/dev/drbd/by-res/cs-vol-c/0");

            VolumeBackupExport target = driver.prepareVolumeForRestore(pool, volume);

            Assert.assertEquals(13L, target.getHostId());
            Assert.assertEquals("/dev/drbd/by-res/cs-vol-c/0", target.getDevicePath());
            Assert.assertEquals(1073741824L, target.getSizeBytes());
        }
        Mockito.verify(api, Mockito.never()).resourceMakeAvailableOnNode(Mockito.any(), Mockito.any(), Mockito.any());
    }
}

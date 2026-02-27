/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cloudstack.storage.snapshot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;

import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotStrategy.SnapshotOperation;
import org.apache.cloudstack.engine.subsystem.api.storage.StrategyPriority;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.utils.Constants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.cloud.storage.Snapshot;
import com.cloud.storage.VolumeDetailVO;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.SnapshotDetailsDao;
import com.cloud.storage.dao.SnapshotDetailsVO;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.storage.dao.VolumeDetailsDao;
import com.cloud.utils.exception.CloudRuntimeException;

/**
 * Unit tests for {@link OntapSnapshotStrategy}.
 *
 * <p>Tests cover:
 * <ul>
 *   <li>canHandle — TAKE, REVERT, DELETE, COPY, BACKUP operations</li>
 *   <li>isVolumeOnOntapManagedStorage — various pool configurations</li>
 *   <li>resolveVolumePathOnOntap — NFS and iSCSI paths</li>
 *   <li>buildSnapshotName — name generation and truncation</li>
 *   <li>OntapSnapshotDetail — serialization/deserialization</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OntapSnapshotStrategyTest {

    private static final long VOLUME_ID = 100L;
    private static final long POOL_ID = 200L;
    private static final long SNAPSHOT_ID = 300L;

    @Spy
    private OntapSnapshotStrategy strategy;

    @Mock
    private VolumeDao volumeDao;

    @Mock
    private VolumeDetailsDao volumeDetailsDao;

    @Mock
    private PrimaryDataStoreDao storagePoolDao;

    @Mock
    private SnapshotDetailsDao snapshotDetailsDao;

    @BeforeEach
    void setUp() throws Exception {
        injectField("volumeDao", volumeDao);
        injectField("volumeDetailsDao", volumeDetailsDao);
        injectField("storagePoolDao", storagePoolDao);
        injectField("snapshotDetailsDao", snapshotDetailsDao);
    }

    private void injectField(String fieldName, Object value) throws Exception {
        Field field = findField(strategy.getClass(), fieldName);
        field.setAccessible(true);
        field.set(strategy, value);
    }

    private Field findField(Class<?> clazz, String fieldName) throws NoSuchFieldException {
        while (clazz != null) {
            try {
                return clazz.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        throw new NoSuchFieldException(fieldName);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // canHandle tests
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void testCanHandle_copyOperation_returnsCantHandle() {
        Snapshot snapshot = mock(Snapshot.class);
        assertEquals(StrategyPriority.CANT_HANDLE,
                strategy.canHandle(snapshot, null, SnapshotOperation.COPY));
    }

    @Test
    void testCanHandle_backupOperation_returnsCantHandle() {
        Snapshot snapshot = mock(Snapshot.class);
        assertEquals(StrategyPriority.CANT_HANDLE,
                strategy.canHandle(snapshot, null, SnapshotOperation.BACKUP));
    }

    @Test
    void testCanHandle_takeOperation_volumeOnOntap_returnsHighest() {
        Snapshot snapshot = mock(Snapshot.class);
        when(snapshot.getVolumeId()).thenReturn(VOLUME_ID);

        VolumeVO volumeVO = mock(VolumeVO.class);
        when(volumeVO.getPoolId()).thenReturn(POOL_ID);
        when(volumeDao.findById(VOLUME_ID)).thenReturn(volumeVO);

        StoragePoolVO pool = mock(StoragePoolVO.class);
        when(pool.isManaged()).thenReturn(true);
        when(pool.getStorageProviderName()).thenReturn(Constants.ONTAP_PLUGIN_NAME);
        when(storagePoolDao.findById(POOL_ID)).thenReturn(pool);

        assertEquals(StrategyPriority.HIGHEST,
                strategy.canHandle(snapshot, null, SnapshotOperation.TAKE));
    }

    @Test
    void testCanHandle_takeOperation_volumeNotOnOntap_returnsCantHandle() {
        Snapshot snapshot = mock(Snapshot.class);
        when(snapshot.getVolumeId()).thenReturn(VOLUME_ID);

        VolumeVO volumeVO = mock(VolumeVO.class);
        when(volumeVO.getPoolId()).thenReturn(POOL_ID);
        when(volumeDao.findById(VOLUME_ID)).thenReturn(volumeVO);

        StoragePoolVO pool = mock(StoragePoolVO.class);
        when(pool.isManaged()).thenReturn(true);
        when(pool.getStorageProviderName()).thenReturn("SomeOtherProvider");
        when(storagePoolDao.findById(POOL_ID)).thenReturn(pool);

        assertEquals(StrategyPriority.CANT_HANDLE,
                strategy.canHandle(snapshot, null, SnapshotOperation.TAKE));
    }

    @Test
    void testCanHandle_takeOperation_volumeNotManaged_returnsCantHandle() {
        Snapshot snapshot = mock(Snapshot.class);
        when(snapshot.getVolumeId()).thenReturn(VOLUME_ID);

        VolumeVO volumeVO = mock(VolumeVO.class);
        when(volumeVO.getPoolId()).thenReturn(POOL_ID);
        when(volumeDao.findById(VOLUME_ID)).thenReturn(volumeVO);

        StoragePoolVO pool = mock(StoragePoolVO.class);
        when(pool.isManaged()).thenReturn(false);
        when(storagePoolDao.findById(POOL_ID)).thenReturn(pool);

        assertEquals(StrategyPriority.CANT_HANDLE,
                strategy.canHandle(snapshot, null, SnapshotOperation.TAKE));
    }

    @Test
    void testCanHandle_deleteOperation_withOntapDetail_returnsHighest() {
        Snapshot snapshot = mock(Snapshot.class);
        when(snapshot.getId()).thenReturn(SNAPSHOT_ID);

        SnapshotDetailsVO detailVO = mock(SnapshotDetailsVO.class);
        when(detailVO.getValue()).thenReturn("flexVol123::snap456::volsnap_300_123::file.qcow2::200::NFS3");
        when(snapshotDetailsDao.findDetail(SNAPSHOT_ID, OntapSnapshotStrategy.ONTAP_FLEXVOL_SNAPSHOT_DETAIL))
                .thenReturn(detailVO);

        assertEquals(StrategyPriority.HIGHEST,
                strategy.canHandle(snapshot, null, SnapshotOperation.DELETE));
    }

    @Test
    void testCanHandle_deleteOperation_withoutOntapDetail_returnsCantHandle() {
        Snapshot snapshot = mock(Snapshot.class);
        when(snapshot.getId()).thenReturn(SNAPSHOT_ID);

        when(snapshotDetailsDao.findDetail(SNAPSHOT_ID, OntapSnapshotStrategy.ONTAP_FLEXVOL_SNAPSHOT_DETAIL))
                .thenReturn(null);

        assertEquals(StrategyPriority.CANT_HANDLE,
                strategy.canHandle(snapshot, null, SnapshotOperation.DELETE));
    }

    @Test
    void testCanHandle_revertOperation_withOntapDetail_returnsHighest() {
        Snapshot snapshot = mock(Snapshot.class);
        when(snapshot.getId()).thenReturn(SNAPSHOT_ID);

        SnapshotDetailsVO detailVO = mock(SnapshotDetailsVO.class);
        when(detailVO.getValue()).thenReturn("flexVol123::snap456::volsnap_300_123::file.qcow2::200::NFS3");
        when(snapshotDetailsDao.findDetail(SNAPSHOT_ID, OntapSnapshotStrategy.ONTAP_FLEXVOL_SNAPSHOT_DETAIL))
                .thenReturn(detailVO);

        assertEquals(StrategyPriority.HIGHEST,
                strategy.canHandle(snapshot, null, SnapshotOperation.REVERT));
    }

    @Test
    void testCanHandle_revertOperation_withoutOntapDetail_returnsCantHandle() {
        Snapshot snapshot = mock(Snapshot.class);
        when(snapshot.getId()).thenReturn(SNAPSHOT_ID);

        when(snapshotDetailsDao.findDetail(SNAPSHOT_ID, OntapSnapshotStrategy.ONTAP_FLEXVOL_SNAPSHOT_DETAIL))
                .thenReturn(null);

        assertEquals(StrategyPriority.CANT_HANDLE,
                strategy.canHandle(snapshot, null, SnapshotOperation.REVERT));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // isVolumeOnOntapManagedStorage tests
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void testIsVolumeOnOntapManagedStorage_volumeNotFound_returnsFalse() {
        when(volumeDao.findById(VOLUME_ID)).thenReturn(null);
        assertFalse(strategy.isVolumeOnOntapManagedStorage(VOLUME_ID));
    }

    @Test
    void testIsVolumeOnOntapManagedStorage_noPoolId_returnsFalse() {
        VolumeVO volumeVO = mock(VolumeVO.class);
        when(volumeVO.getPoolId()).thenReturn(null);
        when(volumeDao.findById(VOLUME_ID)).thenReturn(volumeVO);
        assertFalse(strategy.isVolumeOnOntapManagedStorage(VOLUME_ID));
    }

    @Test
    void testIsVolumeOnOntapManagedStorage_poolNotFound_returnsFalse() {
        VolumeVO volumeVO = mock(VolumeVO.class);
        when(volumeVO.getPoolId()).thenReturn(POOL_ID);
        when(volumeDao.findById(VOLUME_ID)).thenReturn(volumeVO);
        when(storagePoolDao.findById(POOL_ID)).thenReturn(null);
        assertFalse(strategy.isVolumeOnOntapManagedStorage(VOLUME_ID));
    }

    @Test
    void testIsVolumeOnOntapManagedStorage_poolNotManaged_returnsFalse() {
        VolumeVO volumeVO = mock(VolumeVO.class);
        when(volumeVO.getPoolId()).thenReturn(POOL_ID);
        when(volumeDao.findById(VOLUME_ID)).thenReturn(volumeVO);

        StoragePoolVO pool = mock(StoragePoolVO.class);
        when(pool.isManaged()).thenReturn(false);
        when(storagePoolDao.findById(POOL_ID)).thenReturn(pool);

        assertFalse(strategy.isVolumeOnOntapManagedStorage(VOLUME_ID));
    }

    @Test
    void testIsVolumeOnOntapManagedStorage_poolNotOntap_returnsFalse() {
        VolumeVO volumeVO = mock(VolumeVO.class);
        when(volumeVO.getPoolId()).thenReturn(POOL_ID);
        when(volumeDao.findById(VOLUME_ID)).thenReturn(volumeVO);

        StoragePoolVO pool = mock(StoragePoolVO.class);
        when(pool.isManaged()).thenReturn(true);
        when(pool.getStorageProviderName()).thenReturn("SomeOtherPlugin");
        when(storagePoolDao.findById(POOL_ID)).thenReturn(pool);

        assertFalse(strategy.isVolumeOnOntapManagedStorage(VOLUME_ID));
    }

    @Test
    void testIsVolumeOnOntapManagedStorage_poolIsOntap_returnsTrue() {
        VolumeVO volumeVO = mock(VolumeVO.class);
        when(volumeVO.getPoolId()).thenReturn(POOL_ID);
        when(volumeDao.findById(VOLUME_ID)).thenReturn(volumeVO);

        StoragePoolVO pool = mock(StoragePoolVO.class);
        when(pool.isManaged()).thenReturn(true);
        when(pool.getStorageProviderName()).thenReturn(Constants.ONTAP_PLUGIN_NAME);
        when(storagePoolDao.findById(POOL_ID)).thenReturn(pool);

        assertTrue(strategy.isVolumeOnOntapManagedStorage(VOLUME_ID));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // resolveVolumePathOnOntap tests
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void testResolveVolumePathOnOntap_nfsProtocol_returnsVolumePath() {
        VolumeVO volumeVO = mock(VolumeVO.class);
        when(volumeVO.getPath()).thenReturn("abc-def-123.qcow2");
        when(volumeDao.findById(VOLUME_ID)).thenReturn(volumeVO);

        String path = strategy.resolveVolumePathOnOntap(VOLUME_ID, "NFS3", null);
        assertEquals("abc-def-123.qcow2", path);
    }

    @Test
    void testResolveVolumePathOnOntap_iscsiProtocol_returnsLunName() {
        VolumeDetailVO lunDetail = mock(VolumeDetailVO.class);
        when(lunDetail.getValue()).thenReturn("/vol/vol1/lun_test");
        when(volumeDetailsDao.findDetail(VOLUME_ID, Constants.LUN_DOT_NAME)).thenReturn(lunDetail);

        String path = strategy.resolveVolumePathOnOntap(VOLUME_ID, "ISCSI", null);
        assertEquals("/vol/vol1/lun_test", path);
    }

    @Test
    void testResolveVolumePathOnOntap_iscsiProtocol_noLunDetail_throws() {
        when(volumeDetailsDao.findDetail(VOLUME_ID, Constants.LUN_DOT_NAME)).thenReturn(null);
        assertThrows(CloudRuntimeException.class,
                () -> strategy.resolveVolumePathOnOntap(VOLUME_ID, "ISCSI", null));
    }

    @Test
    void testResolveVolumePathOnOntap_nfsProtocol_volumeNotFound_throws() {
        when(volumeDao.findById(VOLUME_ID)).thenReturn(null);
        assertThrows(CloudRuntimeException.class,
                () -> strategy.resolveVolumePathOnOntap(VOLUME_ID, "NFS3", null));
    }

    @Test
    void testResolveVolumePathOnOntap_nfsProtocol_emptyPath_throws() {
        VolumeVO volumeVO = mock(VolumeVO.class);
        when(volumeVO.getPath()).thenReturn("");
        when(volumeDao.findById(VOLUME_ID)).thenReturn(volumeVO);

        assertThrows(CloudRuntimeException.class,
                () -> strategy.resolveVolumePathOnOntap(VOLUME_ID, "NFS3", null));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // buildSnapshotName tests
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void testBuildSnapshotName_containsPrefix() {
        org.apache.cloudstack.engine.subsystem.api.storage.SnapshotInfo snapshotInfo =
                mock(org.apache.cloudstack.engine.subsystem.api.storage.SnapshotInfo.class);
        when(snapshotInfo.getId()).thenReturn(SNAPSHOT_ID);

        String name = strategy.buildSnapshotName(snapshotInfo);
        assertTrue(name.startsWith("volsnap_300_"));
    }

    @Test
    void testBuildSnapshotName_doesNotExceedMaxLength() {
        org.apache.cloudstack.engine.subsystem.api.storage.SnapshotInfo snapshotInfo =
                mock(org.apache.cloudstack.engine.subsystem.api.storage.SnapshotInfo.class);
        when(snapshotInfo.getId()).thenReturn(SNAPSHOT_ID);

        String name = strategy.buildSnapshotName(snapshotInfo);
        assertTrue(name.length() <= Constants.MAX_SNAPSHOT_NAME_LENGTH);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // OntapSnapshotDetail serialization/deserialization tests
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void testOntapSnapshotDetail_serializeAndParse() {
        OntapSnapshotStrategy.OntapSnapshotDetail detail = new OntapSnapshotStrategy.OntapSnapshotDetail(
                "flexVol-uuid-123", "snap-uuid-456", "volsnap_300_123",
                "abc-def.qcow2", 200L, "NFS3");

        String serialized = detail.toString();
        assertEquals("flexVol-uuid-123::snap-uuid-456::volsnap_300_123::abc-def.qcow2::200::NFS3", serialized);

        OntapSnapshotStrategy.OntapSnapshotDetail parsed = OntapSnapshotStrategy.OntapSnapshotDetail.parse(serialized);
        assertEquals("flexVol-uuid-123", parsed.flexVolUuid);
        assertEquals("snap-uuid-456", parsed.snapshotUuid);
        assertEquals("volsnap_300_123", parsed.snapshotName);
        assertEquals("abc-def.qcow2", parsed.volumePath);
        assertEquals(200L, parsed.poolId);
        assertEquals("NFS3", parsed.protocol);
    }

    @Test
    void testOntapSnapshotDetail_serializeAndParse_iscsi() {
        OntapSnapshotStrategy.OntapSnapshotDetail detail = new OntapSnapshotStrategy.OntapSnapshotDetail(
                "flexVol-uuid-789", "snap-uuid-012", "volsnap_400_456",
                "/vol/vol1/lun_test", 300L, "ISCSI");

        String serialized = detail.toString();
        OntapSnapshotStrategy.OntapSnapshotDetail parsed = OntapSnapshotStrategy.OntapSnapshotDetail.parse(serialized);
        assertEquals("/vol/vol1/lun_test", parsed.volumePath);
        assertEquals("ISCSI", parsed.protocol);
    }

    @Test
    void testOntapSnapshotDetail_parseInvalidFormat_throws() {
        assertThrows(CloudRuntimeException.class,
                () -> OntapSnapshotStrategy.OntapSnapshotDetail.parse("invalid::data"));
    }

    @Test
    void testOntapSnapshotDetail_parseIncompleteFormat_throws() {
        assertThrows(CloudRuntimeException.class,
                () -> OntapSnapshotStrategy.OntapSnapshotDetail.parse("a::b::c::d::e"));
    }
}

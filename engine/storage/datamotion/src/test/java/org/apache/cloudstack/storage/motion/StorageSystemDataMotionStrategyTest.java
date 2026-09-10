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
package org.apache.cloudstack.storage.motion;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.HashMap;
import java.util.Map;

import org.apache.cloudstack.engine.subsystem.api.storage.DataObject;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.StrategyPriority;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.storage.datastore.PrimaryDataStoreImpl;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.image.store.ImageStoreImpl;
import org.apache.cloudstack.storage.volume.VolumeObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.agent.api.MigrateCommand;
import com.cloud.host.HostVO;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.ImageStore;
import com.cloud.storage.ScopeType;
import com.cloud.storage.Storage;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeVO;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RunWith(MockitoJUnitRunner.class)
public class StorageSystemDataMotionStrategyTest {

    @Spy
    @InjectMocks
    private StorageSystemDataMotionStrategy strategy;

    @Mock
    private VolumeObject volumeObjectSource;
    @Mock
    private DataObject dataObjectDestination;
    @Mock
    private PrimaryDataStore sourceStore;
    @Mock
    private ImageStore destinationStore;
    @Mock
    private PrimaryDataStoreDao primaryDataStoreDao;

    @Mock
    StoragePoolVO sourceStoragePoolVoMock, destinationStoragePoolVoMock;

    @Mock
    Map<String, Storage.StoragePoolType> mapStringStoragePoolTypeMock;

    List<ScopeType> scopeTypes = Arrays.asList(ScopeType.CLUSTER, ScopeType.ZONE);

    @Before
    public void setUp() throws Exception {
        sourceStore = mock(PrimaryDataStoreImpl.class);
        destinationStore = mock(ImageStoreImpl.class);
        volumeObjectSource = mock(VolumeObject.class);
        dataObjectDestination = mock(VolumeObject.class);

        initMocks(strategy);
    }

    @Test
    public void cantHandleSecondary() {
        lenient().doReturn(sourceStore).when(volumeObjectSource).getDataStore();
        doReturn(DataStoreRole.Primary).when(sourceStore).getRole();
        lenient().doReturn(destinationStore).when(dataObjectDestination).getDataStore();
        doReturn(DataStoreRole.Image).when((DataStore)destinationStore).getRole();
        doReturn(sourceStore).when(volumeObjectSource).getDataStore();
        doReturn(destinationStore).when(dataObjectDestination).getDataStore();
        StoragePoolVO storeVO = new StoragePoolVO();
        doReturn(storeVO).when(primaryDataStoreDao).findById(0l);

        assertTrue(strategy.canHandle(volumeObjectSource, dataObjectDestination) == StrategyPriority.CANT_HANDLE);
    }

    @Test
    public void internalCanHandleTestAllStoragePoolsAreManaged() {
        configureAndTestInternalCanHandle(true, true, StrategyPriority.HIGHEST);
    }

    @Test
    public void internalCanHandleTestFirstStoragePoolsIsManaged() {
        configureAndTestInternalCanHandle(false, true, StrategyPriority.HIGHEST);
    }

    @Test
    public void internalCanHandleTestSecondStoragePoolsIsManaged() {
        configureAndTestInternalCanHandle(true, false, StrategyPriority.HIGHEST);
    }

    @Test
    public void internalCanHandleTestNoStoragePoolsIsManaged() {
        configureAndTestInternalCanHandle(false, false, StrategyPriority.CANT_HANDLE);
    }

    private void configureAndTestInternalCanHandle(boolean sPool0IsManaged, boolean sPool1IsManaged, StrategyPriority expectedStrategyPriority) {
        VolumeObject volumeInfo = Mockito.spy(new VolumeObject());
        Mockito.doReturn(0l).when(volumeInfo).getPoolId();

        DataStore ds = Mockito.spy(new PrimaryDataStoreImpl());
        Mockito.doReturn(1l).when(ds).getId();

        Map<VolumeInfo, DataStore> volumeMap = new HashMap<>();
        volumeMap.put(volumeInfo, ds);

        StoragePoolVO storagePool0 = Mockito.spy(new StoragePoolVO());
        Mockito.doReturn(sPool0IsManaged).when(storagePool0).isManaged();
        StoragePoolVO storagePool1 = Mockito.spy(new StoragePoolVO());
        Mockito.doReturn(sPool1IsManaged).when(storagePool1).isManaged();

        Mockito.doReturn(storagePool0).when(primaryDataStoreDao).findById(0l);
        Mockito.doReturn(storagePool1).when(primaryDataStoreDao).findById(1l);

        StrategyPriority strategyPriority = strategy.internalCanHandle(volumeMap, new HostVO("srcHostUuid"), new HostVO("destHostUuid"));

        Assert.assertEquals(expectedStrategyPriority, strategyPriority);
    }

    @Test
    public void isStoragePoolTypeOfFileTest() {
        StoragePoolVO sourceStoragePool = Mockito.spy(new StoragePoolVO());
        StoragePoolType[] storagePoolTypeArray = StoragePoolType.values();
        for (int i = 0; i < storagePoolTypeArray.length; i++) {
            Mockito.doReturn(storagePoolTypeArray[i]).when(sourceStoragePool).getPoolType();
            boolean result = strategy.isStoragePoolTypeOfFile(sourceStoragePool);
            if (sourceStoragePool.getPoolType() == StoragePoolType.Filesystem) {
                Assert.assertTrue(result);
            } else {
                Assert.assertFalse(result);
            }
        }
    }

    @Test
    public void generateDestPathTest() {
        VolumeObject destVolumeInfo = Mockito.spy(new VolumeObject());
        HostVO destHost = new HostVO("guid");
        Mockito.doReturn("iScsiName").when(destVolumeInfo).get_iScsiName();
        Mockito.doReturn(0l).when(destVolumeInfo).getPoolId();
        Mockito.doReturn("expected").when(strategy).connectHostToVolume(destHost, 0l, "iScsiName");

        String expected = strategy.generateDestPath(destHost, Mockito.mock(StoragePoolVO.class), destVolumeInfo);

        Assert.assertEquals(expected, "expected");
        Mockito.verify(strategy).connectHostToVolume(destHost, 0l, "iScsiName");
    }

    @Test
    public void configureMigrateDiskInfoTest() {
        VolumeObject srcVolumeInfo = Mockito.spy(new VolumeObject());
        Mockito.doReturn("volume path").when(srcVolumeInfo).getPath();
        MigrateCommand.MigrateDiskInfo migrateDiskInfo = strategy.configureMigrateDiskInfo(srcVolumeInfo, "destPath", null);
        Assert.assertEquals(MigrateCommand.MigrateDiskInfo.DiskType.BLOCK, migrateDiskInfo.getDiskType());
        Assert.assertEquals(MigrateCommand.MigrateDiskInfo.DriverType.RAW, migrateDiskInfo.getDriverType());
        Assert.assertEquals(MigrateCommand.MigrateDiskInfo.Source.DEV, migrateDiskInfo.getSource());
        Assert.assertEquals("destPath", migrateDiskInfo.getSourceText());
        Assert.assertEquals("volume path", migrateDiskInfo.getSerialNumber());
    }

    @Test
    public void configureMigrateDiskInfoWithBackingTest() {
        VolumeObject srcVolumeInfo = Mockito.spy(new VolumeObject());
        Mockito.doReturn("volume path").when(srcVolumeInfo).getPath();
        MigrateCommand.MigrateDiskInfo migrateDiskInfo = strategy.configureMigrateDiskInfo(srcVolumeInfo, "destPath", "backingPath");
        Assert.assertEquals(MigrateCommand.MigrateDiskInfo.DiskType.BLOCK, migrateDiskInfo.getDiskType());
        Assert.assertEquals(MigrateCommand.MigrateDiskInfo.DriverType.RAW, migrateDiskInfo.getDriverType());
        Assert.assertEquals(MigrateCommand.MigrateDiskInfo.Source.DEV, migrateDiskInfo.getSource());
        Assert.assertEquals("destPath", migrateDiskInfo.getSourceText());
        Assert.assertEquals("volume path", migrateDiskInfo.getSerialNumber());
        Assert.assertEquals("backingPath", migrateDiskInfo.getBackingStoreText());
    }

    @Test
    public void setVolumePathTest() {
        VolumeVO volume = new VolumeVO("name", 0l, 0l, 0l, 0l, 0l, "folder", "path", Storage.ProvisioningType.THIN, 0l, Volume.Type.ROOT);
        String volumePath = "iScsiName";
        volume.set_iScsiName(volumePath);

        strategy.setVolumePath(volume);

        Assert.assertEquals(volumePath, volume.getPath());
    }

    @Test
    public void shouldMigrateVolumeTest() {
        StoragePoolVO sourceStoragePool = Mockito.spy(new StoragePoolVO());
        HostVO destHost = new HostVO("guid");
        StoragePoolVO destStoragePool = new StoragePoolVO();
        StoragePoolType[] storagePoolTypes = StoragePoolType.values();
        for (int i = 0; i < storagePoolTypes.length; i++) {
            Mockito.lenient().doReturn(storagePoolTypes[i]).when(sourceStoragePool).getPoolType();
            boolean result = strategy.shouldMigrateVolume(sourceStoragePool, destHost, destStoragePool);
            Assert.assertTrue(result);
        }
    }

    @Test
    public void isSourceAndDestinationPoolTypeOfNfsTestNfsNfs() {
        configureAndVerifyIsSourceAndDestinationPoolTypeOfNfs(StoragePoolType.NetworkFilesystem, StoragePoolType.NetworkFilesystem, true);
    }

    @Test
    public void isSourceAndDestinationPoolTypeOfNfsTestNfsAny() {
        StoragePoolType[] storagePoolTypeArray = StoragePoolType.values();
        for (int i = 0; i < storagePoolTypeArray.length - 1; i++) {
            if (storagePoolTypeArray[i] != StoragePoolType.NetworkFilesystem) {
                configureAndVerifyIsSourceAndDestinationPoolTypeOfNfs(StoragePoolType.NetworkFilesystem, storagePoolTypeArray[i], false);
            }
        }
    }

    @Test
    public void isSourceAndDestinationPoolTypeOfNfsTestAnyNfs() {
        StoragePoolType[] storagePoolTypeArray = StoragePoolType.values();
        for (int i = 0; i < storagePoolTypeArray.length - 1; i++) {
            if (storagePoolTypeArray[i] != StoragePoolType.NetworkFilesystem) {
                configureAndVerifyIsSourceAndDestinationPoolTypeOfNfs(storagePoolTypeArray[i], StoragePoolType.NetworkFilesystem, false);
            }
        }
    }

    @Test
    public void isSourceAndDestinationPoolTypeOfNfsTestAnyAny() {
        StoragePoolType[] storagePoolTypeArray = StoragePoolType.values();
        for (int i = 0; i < storagePoolTypeArray.length - 1; i++) {
            for (int j = 0; j < storagePoolTypeArray.length - 1; j++) {
                if (storagePoolTypeArray[i] != StoragePoolType.NetworkFilesystem || storagePoolTypeArray[j] != StoragePoolType.NetworkFilesystem) {
                    configureAndVerifyIsSourceAndDestinationPoolTypeOfNfs(storagePoolTypeArray[i], storagePoolTypeArray[j], false);
                }
            }
        }
    }

    private void configureAndVerifyIsSourceAndDestinationPoolTypeOfNfs(StoragePoolType destStoragePoolType, StoragePoolType sourceStoragePoolType, boolean expected) {
        VolumeInfo srcVolumeInfo = Mockito.mock(VolumeObject.class);
        Mockito.lenient().when(srcVolumeInfo.getId()).thenReturn(0l);

        DataStore destDataStore = Mockito.mock(PrimaryDataStoreImpl.class);
        Mockito.when(destDataStore.getId()).thenReturn(1l);

        StoragePoolVO destStoragePool = Mockito.mock(StoragePoolVO.class);
        Mockito.when(destStoragePool.getPoolType()).thenReturn(destStoragePoolType);

        StoragePoolVO sourceStoragePool = Mockito.mock(StoragePoolVO.class);
        Mockito.when(sourceStoragePool.getPoolType()).thenReturn(sourceStoragePoolType);

        Map<VolumeInfo, DataStore> volumeDataStoreMap = new HashMap<>();
        volumeDataStoreMap.put(srcVolumeInfo, destDataStore);

        Mockito.doReturn(sourceStoragePool).when(primaryDataStoreDao).findById(0l);
        Mockito.doReturn(destStoragePool).when(primaryDataStoreDao).findById(1l);

        boolean result = strategy.isSourceAndDestinationPoolTypeOfNfs(volumeDataStoreMap);
        Assert.assertEquals(expected, result);
    }

    @Test
    public void formatMigrationElementsAsJsonToDisplayOnLogValidateFormat(){
        String objectName = "test";
        Long object = 1L, from = 2L, to = 3L;

        Assert.assertEquals(String.format("{%s: \"%s\", from: \"%s\", to:\"%s\"}", objectName, object, from, to), strategy.formatMigrationElementsAsJsonToDisplayOnLog(objectName,
                object, from, to));
    }

    @Test
    public void formatEntryOfVolumesAndStoragesAsJsonToDisplayOnLogValidateFormat(){
        Long volume = 1L, from = 2L, to = 3L;
        VolumeInfo volumeInfo = Mockito.mock(VolumeInfo.class);
        DataStore dataStore = Mockito.mock(DataStore.class);

        Mockito.when(volumeInfo.getId()).thenReturn(volume);
        Mockito.when(volumeInfo.getPoolId()).thenReturn(from);
        Mockito.when(dataStore.getId()).thenReturn(to);

        Assert.assertEquals(String.format("{volume: \"%s\", from: \"%s\", to:\"%s\"}", volume, from, to), strategy.formatEntryOfVolumesAndStoragesAsJsonToDisplayOnLog(new AbstractMap.SimpleEntry<>(volumeInfo, dataStore)));
    }

    @Test
    public void validateSupportStoragePoolTypeDefaultValues() {
        Set<StoragePoolType> supportedTypes = new HashSet<>();
        supportedTypes.add(StoragePoolType.NetworkFilesystem);
        supportedTypes.add(StoragePoolType.SharedMountPoint);

        for (StoragePoolType poolType : StoragePoolType.values()) {
            boolean isSupported = strategy.supportStoragePoolType(poolType);
            if (supportedTypes.contains(poolType)) {
                assertTrue(isSupported);
            } else {
                assertFalse(isSupported);
            }
        }
    }

    @Test
    public void validateSupportStoragePoolTypeExtraValues() {
        Set<StoragePoolType> supportedTypes = new HashSet<>();
        supportedTypes.add(StoragePoolType.NetworkFilesystem);
        supportedTypes.add(StoragePoolType.SharedMountPoint);
        supportedTypes.add(StoragePoolType.Iscsi);
        supportedTypes.add(StoragePoolType.CLVM);

        for (StoragePoolType poolType : StoragePoolType.values()) {
            boolean isSupported = strategy.supportStoragePoolType(poolType, StoragePoolType.Iscsi, StoragePoolType.CLVM);
            if (supportedTypes.contains(poolType)) {
                assertTrue(isSupported);
            } else {
                assertFalse(isSupported);
            }
        }
    }

    @Test
    public void validateIsStoragePoolTypeInListReturnsTrue() {
        StoragePoolType[] listTypes = new StoragePoolType[3];
        listTypes[0] = StoragePoolType.LVM;
        listTypes[1] = StoragePoolType.NetworkFilesystem;
        listTypes[2] = StoragePoolType.SharedMountPoint;

        assertTrue(strategy.isStoragePoolTypeInList(StoragePoolType.SharedMountPoint, listTypes));
    }

    @Test
    public void validateIsStoragePoolTypeInListReturnsFalse() {
        StoragePoolType[] listTypes = new StoragePoolType[3];
        listTypes[0] = StoragePoolType.LVM;
        listTypes[1] = StoragePoolType.NetworkFilesystem;
        listTypes[2] = StoragePoolType.RBD;

        assertFalse(strategy.isStoragePoolTypeInList(StoragePoolType.SharedMountPoint, listTypes));
    }

    /**
     * Test updateMigrateDiskInfoForBlockDevice with CLVM destination pool
     * Should set driver type to RAW for CLVM
     */
    @Test
    public void testUpdateMigrateDiskInfoForBlockDevice_ClvmDestination() {
        MigrateCommand.MigrateDiskInfo originalDiskInfo = new MigrateCommand.MigrateDiskInfo(
                "serial123",
                MigrateCommand.MigrateDiskInfo.DiskType.FILE,
                MigrateCommand.MigrateDiskInfo.DriverType.QCOW2,
                MigrateCommand.MigrateDiskInfo.Source.FILE,
                "/source/path",
                null
        );

        StoragePoolVO destStoragePool = new StoragePoolVO();
        destStoragePool.setPoolType(StoragePoolType.CLVM);

        MigrateCommand.MigrateDiskInfo updatedDiskInfo = strategy.updateMigrateDiskInfoForBlockDevice(
                originalDiskInfo, destStoragePool);

        Assert.assertEquals(MigrateCommand.MigrateDiskInfo.DiskType.BLOCK, updatedDiskInfo.getDiskType());
        Assert.assertEquals(MigrateCommand.MigrateDiskInfo.DriverType.RAW, updatedDiskInfo.getDriverType());
        Assert.assertEquals(MigrateCommand.MigrateDiskInfo.Source.DEV, updatedDiskInfo.getSource());
        Assert.assertEquals("serial123", updatedDiskInfo.getSerialNumber());
        Assert.assertEquals("/source/path", updatedDiskInfo.getSourceText());
    }

    /**
     * Test updateMigrateDiskInfoForBlockDevice with CLVM_NG destination pool
     * Should set driver type to QCOW2 for CLVM_NG
     */
    @Test
    public void testUpdateMigrateDiskInfoForBlockDevice_ClvmNgDestination() {
        MigrateCommand.MigrateDiskInfo originalDiskInfo = new MigrateCommand.MigrateDiskInfo(
                "serial456",
                MigrateCommand.MigrateDiskInfo.DiskType.FILE,
                MigrateCommand.MigrateDiskInfo.DriverType.RAW,
                MigrateCommand.MigrateDiskInfo.Source.FILE,
                "/source/path",
                "/backing/path"
        );

        StoragePoolVO destStoragePool = new StoragePoolVO();
        destStoragePool.setPoolType(StoragePoolType.CLVM_NG);

        MigrateCommand.MigrateDiskInfo updatedDiskInfo = strategy.updateMigrateDiskInfoForBlockDevice(
                originalDiskInfo, destStoragePool);

        Assert.assertEquals(MigrateCommand.MigrateDiskInfo.DiskType.BLOCK, updatedDiskInfo.getDiskType());
        Assert.assertEquals(MigrateCommand.MigrateDiskInfo.DriverType.QCOW2, updatedDiskInfo.getDriverType());
        Assert.assertEquals(MigrateCommand.MigrateDiskInfo.Source.DEV, updatedDiskInfo.getSource());
        Assert.assertEquals("serial456", updatedDiskInfo.getSerialNumber());
        Assert.assertEquals("/source/path", updatedDiskInfo.getSourceText());
        Assert.assertEquals("/backing/path", updatedDiskInfo.getBackingStoreText());
    }

    /**
     * Test updateMigrateDiskInfoForBlockDevice with non-CLVM destination pool
     * Should return original DiskInfo unchanged
     */
    @Test
    public void testUpdateMigrateDiskInfoForBlockDevice_NonClvmDestination() {
        MigrateCommand.MigrateDiskInfo originalDiskInfo = new MigrateCommand.MigrateDiskInfo(
                "serial789",
                MigrateCommand.MigrateDiskInfo.DiskType.FILE,
                MigrateCommand.MigrateDiskInfo.DriverType.QCOW2,
                MigrateCommand.MigrateDiskInfo.Source.FILE,
                "/source/path",
                null
        );

        StoragePoolVO destStoragePool = new StoragePoolVO();
        destStoragePool.setPoolType(StoragePoolType.NetworkFilesystem);

        MigrateCommand.MigrateDiskInfo updatedDiskInfo = strategy.updateMigrateDiskInfoForBlockDevice(
                originalDiskInfo, destStoragePool);

        Assert.assertSame(originalDiskInfo, updatedDiskInfo);
        Assert.assertEquals(MigrateCommand.MigrateDiskInfo.DiskType.FILE, updatedDiskInfo.getDiskType());
        Assert.assertEquals(MigrateCommand.MigrateDiskInfo.DriverType.QCOW2, updatedDiskInfo.getDriverType());
        Assert.assertEquals(MigrateCommand.MigrateDiskInfo.Source.FILE, updatedDiskInfo.getSource());
    }

    /**
     * Test supportStoragePoolType with CLVM and CLVM_NG types
     */
    @Test
    public void testSupportStoragePoolType_ClvmTypes() {
        assertTrue(strategy.supportStoragePoolType(StoragePoolType.CLVM, StoragePoolType.CLVM, StoragePoolType.CLVM_NG));
        assertTrue(strategy.supportStoragePoolType(StoragePoolType.CLVM_NG, StoragePoolType.CLVM, StoragePoolType.CLVM_NG));

        assertFalse(strategy.supportStoragePoolType(StoragePoolType.CLVM));
        assertFalse(strategy.supportStoragePoolType(StoragePoolType.CLVM_NG));
    }

    /**
     * Test configureMigrateDiskInfo with CLVM destination
     */
    @Test
    public void testConfigureMigrateDiskInfo_ForClvm() {
        VolumeObject srcVolumeInfo = Mockito.spy(new VolumeObject());
        Mockito.doReturn("/dev/vg/volume-path").when(srcVolumeInfo).getPath();

        MigrateCommand.MigrateDiskInfo migrateDiskInfo = strategy.configureMigrateDiskInfo(
                srcVolumeInfo, "/dev/vg/dest-path", null);

        Assert.assertEquals(MigrateCommand.MigrateDiskInfo.DiskType.BLOCK, migrateDiskInfo.getDiskType());
        Assert.assertEquals(MigrateCommand.MigrateDiskInfo.DriverType.RAW, migrateDiskInfo.getDriverType());
        Assert.assertEquals(MigrateCommand.MigrateDiskInfo.Source.DEV, migrateDiskInfo.getSource());
        Assert.assertEquals("/dev/vg/dest-path", migrateDiskInfo.getSourceText());
        Assert.assertEquals("/dev/vg/volume-path", migrateDiskInfo.getSerialNumber());
    }

    /**
     * Test configureMigrateDiskInfo with CLVM_NG destination and backing file
     */
    @Test
    public void testConfigureMigrateDiskInfo_ForClvmNgWithBacking() {
        VolumeObject srcVolumeInfo = Mockito.spy(new VolumeObject());
        Mockito.doReturn("/dev/vg/volume-path").when(srcVolumeInfo).getPath();

        MigrateCommand.MigrateDiskInfo migrateDiskInfo = strategy.configureMigrateDiskInfo(
                srcVolumeInfo, "/dev/vg/dest-path", "/dev/vg/backing-template");

        Assert.assertEquals(MigrateCommand.MigrateDiskInfo.DiskType.BLOCK, migrateDiskInfo.getDiskType());
        Assert.assertEquals(MigrateCommand.MigrateDiskInfo.DriverType.RAW, migrateDiskInfo.getDriverType());
        Assert.assertEquals(MigrateCommand.MigrateDiskInfo.Source.DEV, migrateDiskInfo.getSource());
        Assert.assertEquals("/dev/vg/dest-path", migrateDiskInfo.getSourceText());
        Assert.assertEquals("/dev/vg/backing-template", migrateDiskInfo.getBackingStoreText());
        Assert.assertEquals("/dev/vg/volume-path", migrateDiskInfo.getSerialNumber());
    }

    /**
     * Test isStoragePoolTypeInList with CLVM types
     */
    @Test
    public void testIsStoragePoolTypeInList_WithClvmTypes() {
        StoragePoolType[] clvmTypes = new StoragePoolType[] {
            StoragePoolType.CLVM,
            StoragePoolType.CLVM_NG,
            StoragePoolType.Filesystem
        };

        assertTrue(strategy.isStoragePoolTypeInList(StoragePoolType.CLVM, clvmTypes));
        assertTrue(strategy.isStoragePoolTypeInList(StoragePoolType.CLVM_NG, clvmTypes));
        assertTrue(strategy.isStoragePoolTypeInList(StoragePoolType.Filesystem, clvmTypes));
        assertFalse(strategy.isStoragePoolTypeInList(StoragePoolType.NetworkFilesystem, clvmTypes));
    }

    /**
     * Test supportStoragePoolType with mixed CLVM and NFS types
     */
    @Test
    public void testSupportStoragePoolType_MixedClvmAndNfs() {
        assertTrue(strategy.supportStoragePoolType(
                StoragePoolType.CLVM,
                StoragePoolType.CLVM,
                StoragePoolType.CLVM_NG,
                StoragePoolType.NetworkFilesystem
        ));

        assertTrue(strategy.supportStoragePoolType(
                StoragePoolType.CLVM_NG,
                StoragePoolType.CLVM,
                StoragePoolType.CLVM_NG,
                StoragePoolType.NetworkFilesystem
        ));

        assertTrue(strategy.supportStoragePoolType(
                StoragePoolType.NetworkFilesystem,
                StoragePoolType.CLVM,
                StoragePoolType.CLVM_NG
        ));
    }

    /**
     * Test internalCanHandle with CLVM source and managed destination
     */
    @Test
    public void testInternalCanHandle_ClvmSourceManagedDestination() {
        VolumeObject volumeInfo = Mockito.spy(new VolumeObject());
        Mockito.doReturn(0L).when(volumeInfo).getPoolId();

        DataStore ds = Mockito.spy(new PrimaryDataStoreImpl());

        Map<VolumeInfo, DataStore> volumeMap = new HashMap<>();
        volumeMap.put(volumeInfo, ds);

        StoragePoolVO sourcePool = Mockito.spy(new StoragePoolVO());
        Mockito.lenient().doReturn(StoragePoolType.CLVM).when(sourcePool).getPoolType();
        Mockito.doReturn(true).when(sourcePool).isManaged();

        Mockito.doReturn(sourcePool).when(primaryDataStoreDao).findById(0L);

        StrategyPriority result = strategy.internalCanHandle(
                volumeMap, new HostVO("srcHostUuid"), new HostVO("destHostUuid"));

        Assert.assertEquals(StrategyPriority.HIGHEST, result);
    }

    /**
     * Test internalCanHandle with CLVM_NG source and managed destination
     */
    @Test
    public void testInternalCanHandle_ClvmNgSourceManagedDestination() {
        VolumeObject volumeInfo = Mockito.spy(new VolumeObject());
        Mockito.doReturn(0L).when(volumeInfo).getPoolId();

        DataStore ds = Mockito.spy(new PrimaryDataStoreImpl());

        Map<VolumeInfo, DataStore> volumeMap = new HashMap<>();
        volumeMap.put(volumeInfo, ds);

        StoragePoolVO sourcePool = Mockito.spy(new StoragePoolVO());
        Mockito.lenient().doReturn(StoragePoolType.CLVM_NG).when(sourcePool).getPoolType();
        Mockito.doReturn(true).when(sourcePool).isManaged();

        Mockito.doReturn(sourcePool).when(primaryDataStoreDao).findById(0L);

        StrategyPriority result = strategy.internalCanHandle(
                volumeMap, new HostVO("srcHostUuid"), new HostVO("destHostUuid"));

        Assert.assertEquals(StrategyPriority.HIGHEST, result);
    }

    /**
     * Test internalCanHandle with both CLVM source and CLVM_NG destination
     */
    @Test
    public void testInternalCanHandle_ClvmToClvmNg() {
        VolumeObject volumeInfo = Mockito.spy(new VolumeObject());
        Mockito.doReturn(0L).when(volumeInfo).getPoolId();

        DataStore ds = Mockito.spy(new PrimaryDataStoreImpl());

        Map<VolumeInfo, DataStore> volumeMap = new HashMap<>();
        volumeMap.put(volumeInfo, ds);

        StoragePoolVO sourcePool = Mockito.spy(new StoragePoolVO());
        Mockito.lenient().doReturn(StoragePoolType.CLVM).when(sourcePool).getPoolType();
        Mockito.doReturn(true).when(sourcePool).isManaged();

        StoragePoolVO destPool = Mockito.spy(new StoragePoolVO());
        Mockito.lenient().doReturn(StoragePoolType.CLVM_NG).when(destPool).getPoolType();

        Mockito.doReturn(sourcePool).when(primaryDataStoreDao).findById(0L);

        StrategyPriority result = strategy.internalCanHandle(
                volumeMap, new HostVO("srcHostUuid"), new HostVO("destHostUuid"));

        Assert.assertEquals(StrategyPriority.HIGHEST, result);
    }

    /**
     * Test internalCanHandle with CLVM_NG to CLVM migration
     */
    @Test
    public void testInternalCanHandle_ClvmNgToClvm() {
        VolumeObject volumeInfo = Mockito.spy(new VolumeObject());
        Mockito.doReturn(0L).when(volumeInfo).getPoolId();

        DataStore ds = Mockito.spy(new PrimaryDataStoreImpl());

        Map<VolumeInfo, DataStore> volumeMap = new HashMap<>();
        volumeMap.put(volumeInfo, ds);

        StoragePoolVO sourcePool = Mockito.spy(new StoragePoolVO());
        Mockito.lenient().doReturn(StoragePoolType.CLVM_NG).when(sourcePool).getPoolType();
        Mockito.doReturn(true).when(sourcePool).isManaged();

        StoragePoolVO destPool = Mockito.spy(new StoragePoolVO());
        Mockito.lenient().doReturn(StoragePoolType.CLVM).when(destPool).getPoolType();

        Mockito.doReturn(sourcePool).when(primaryDataStoreDao).findById(0L);

        StrategyPriority result = strategy.internalCanHandle(
                volumeMap, new HostVO("srcHostUuid"), new HostVO("destHostUuid"));

        Assert.assertEquals(StrategyPriority.HIGHEST, result);
    }
}

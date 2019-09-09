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
import static org.mockito.Mockito.doReturn;
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
import org.mockito.runners.MockitoJUnitRunner;

import com.cloud.agent.api.MigrateCommand;
import com.cloud.host.HostVO;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.ImageStore;
import com.cloud.storage.Storage;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeVO;

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
        doReturn(sourceStore).when(volumeObjectSource).getDataStore();
        doReturn(DataStoreRole.Primary).when(sourceStore).getRole();
        doReturn(destinationStore).when(dataObjectDestination).getDataStore();
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
        MigrateCommand.MigrateDiskInfo migrateDiskInfo = strategy.configureMigrateDiskInfo(srcVolumeInfo, "destPath");
        Assert.assertEquals(MigrateCommand.MigrateDiskInfo.DiskType.BLOCK, migrateDiskInfo.getDiskType());
        Assert.assertEquals(MigrateCommand.MigrateDiskInfo.DriverType.RAW, migrateDiskInfo.getDriverType());
        Assert.assertEquals(MigrateCommand.MigrateDiskInfo.Source.DEV, migrateDiskInfo.getSource());
        Assert.assertEquals("destPath", migrateDiskInfo.getSourceText());
        Assert.assertEquals("volume path", migrateDiskInfo.getSerialNumber());
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
            Mockito.doReturn(storagePoolTypes[i]).when(sourceStoragePool).getPoolType();
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
        Mockito.when(srcVolumeInfo.getId()).thenReturn(0l);

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

}
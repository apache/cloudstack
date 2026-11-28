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

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreCapabilities;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreDriver;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotStrategy.SnapshotOperation;
import org.apache.cloudstack.engine.subsystem.api.storage.StrategyPriority;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreVO;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.storage.DataStoreRole;
import com.cloud.storage.Snapshot;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.SnapshotDetailsVO;
import com.cloud.storage.dao.SnapshotDetailsDao;
import com.cloud.storage.dao.VolumeDao;

@RunWith(MockitoJUnitRunner.class)
public class StorageSystemSnapshotStrategyTest {

    @InjectMocks
    private StorageSystemSnapshotStrategy storageSystemSnapshotStrategy;
    @Mock
    private DataStoreManager dataStoreMgr;
    @Mock
    private PrimaryDataStoreDao storagePoolDao;
    @Mock
    private SnapshotDataStoreDao snapshotStoreDao;
    @Mock
    private SnapshotDetailsDao snapshotDetailsDao;
    @Mock
    private VolumeDao volumeDao;

    private Snapshot mockSnapshotOfVolume(VolumeVO volumeVO) {
        Snapshot snapshot = Mockito.mock(Snapshot.class);
        Mockito.when(snapshot.getVolumeId()).thenReturn(1L);
        Mockito.when(volumeDao.findByIdIncludingRemoved(1L)).thenReturn(volumeVO);
        return snapshot;
    }

    @Test
    public void canHandleRevertTestVolumeRowMissing() {
        Snapshot snapshot = mockSnapshotOfVolume(null);

        Assert.assertEquals(StrategyPriority.CANT_HANDLE, storageSystemSnapshotStrategy.canHandle(snapshot, null, SnapshotOperation.REVERT));
        Mockito.verifyNoInteractions(dataStoreMgr, storagePoolDao, snapshotStoreDao);
    }

    @Test
    public void canHandleRevertTestRemovedVolume() {
        VolumeVO volumeVO = Mockito.mock(VolumeVO.class);
        Mockito.when(volumeVO.getRemoved()).thenReturn(new Date());
        Snapshot snapshot = mockSnapshotOfVolume(volumeVO);

        Assert.assertEquals(StrategyPriority.CANT_HANDLE, storageSystemSnapshotStrategy.canHandle(snapshot, null, SnapshotOperation.REVERT));
        Mockito.verifyNoInteractions(dataStoreMgr, storagePoolDao, snapshotStoreDao);
    }

    @Test
    public void canHandleRevertTestVolumeWithoutPool() {
        VolumeVO volumeVO = Mockito.mock(VolumeVO.class);
        Mockito.when(volumeVO.getRemoved()).thenReturn(null);
        Mockito.when(volumeVO.getFormat()).thenReturn(ImageFormat.RAW);
        Mockito.when(volumeVO.getPoolId()).thenReturn(null);
        Mockito.when(volumeVO.getLastPoolId()).thenReturn(null);
        Snapshot snapshot = mockSnapshotOfVolume(volumeVO);

        Assert.assertEquals(StrategyPriority.CANT_HANDLE, storageSystemSnapshotStrategy.canHandle(snapshot, null, SnapshotOperation.REVERT));
        Mockito.verifyNoInteractions(dataStoreMgr, storagePoolDao);
    }

    @Test
    public void canHandleRevertTestBackendSnapshotOfVolumeWithoutPool() {
        VolumeVO volumeVO = Mockito.mock(VolumeVO.class);
        Mockito.when(volumeVO.getRemoved()).thenReturn(null);
        Mockito.when(volumeVO.getFormat()).thenReturn(ImageFormat.RAW);
        Mockito.when(volumeVO.getPoolId()).thenReturn(null);
        Mockito.when(volumeVO.getLastPoolId()).thenReturn(null);
        Snapshot snapshot = mockSnapshotOfVolume(volumeVO);
        Mockito.when(snapshot.getId()).thenReturn(2L);

        SnapshotDetailsVO takeSnapshot = Mockito.mock(SnapshotDetailsVO.class);
        Mockito.when(takeSnapshot.getValue()).thenReturn("true");
        Mockito.when(snapshotDetailsDao.findDetail(2L, "takeSnapshot")).thenReturn(takeSnapshot);

        SnapshotDataStoreVO snapshotStore = Mockito.mock(SnapshotDataStoreVO.class);
        Mockito.when(snapshotStore.getDataStoreId()).thenReturn(10L);
        Mockito.when(snapshotStoreDao.findOneBySnapshotAndDatastoreRole(2L, DataStoreRole.Primary)).thenReturn(snapshotStore);

        DataStore dataStore = Mockito.mock(DataStore.class);
        DataStoreDriver driver = Mockito.mock(DataStoreDriver.class);
        Mockito.when(driver.getCapabilities()).thenReturn(new HashMap<>());
        Mockito.when(dataStore.getDriver()).thenReturn(driver);
        Mockito.when(dataStoreMgr.getDataStore(10L, DataStoreRole.Primary)).thenReturn(dataStore);

        Assert.assertEquals(StrategyPriority.CANT_HANDLE, storageSystemSnapshotStrategy.canHandle(snapshot, null, SnapshotOperation.REVERT));
        Mockito.verify(dataStoreMgr, Mockito.times(1)).getDataStore(Mockito.anyLong(), Mockito.any());
    }

    @Test
    public void canHandleRevertTestBackendSnapshotOnRevertCapablePoolOfVolumeWithoutPool() {
        VolumeVO volumeVO = Mockito.mock(VolumeVO.class);
        Mockito.when(volumeVO.getRemoved()).thenReturn(null);
        Mockito.when(volumeVO.getFormat()).thenReturn(ImageFormat.RAW);
        Mockito.when(volumeVO.getPoolId()).thenReturn(null);
        Mockito.when(volumeVO.getLastPoolId()).thenReturn(null);
        Snapshot snapshot = mockSnapshotOfVolume(volumeVO);
        Mockito.when(snapshot.getId()).thenReturn(2L);

        SnapshotDetailsVO takeSnapshot = Mockito.mock(SnapshotDetailsVO.class);
        Mockito.when(takeSnapshot.getValue()).thenReturn("true");
        Mockito.when(snapshotDetailsDao.findDetail(2L, "takeSnapshot")).thenReturn(takeSnapshot);

        SnapshotDataStoreVO snapshotStore = Mockito.mock(SnapshotDataStoreVO.class);
        Mockito.when(snapshotStore.getDataStoreId()).thenReturn(10L);
        Mockito.when(snapshotStoreDao.findOneBySnapshotAndDatastoreRole(2L, DataStoreRole.Primary)).thenReturn(snapshotStore);

        mockDataStoreCapability(10L, DataStoreCapabilities.CAN_REVERT_VOLUME_TO_SNAPSHOT.toString());

        Assert.assertEquals(StrategyPriority.HIGHEST, storageSystemSnapshotStrategy.canHandle(snapshot, null, SnapshotOperation.REVERT));
    }

    @Test
    public void canHandleTakeAndBackupTestVolumeOnLastPool() {
        VolumeVO volumeVO = Mockito.mock(VolumeVO.class);
        Mockito.when(volumeVO.getPoolId()).thenReturn(null);
        Mockito.when(volumeVO.getLastPoolId()).thenReturn(20L);
        Snapshot snapshot = mockSnapshotOfVolume(volumeVO);

        mockDataStoreCapability(20L, DataStoreCapabilities.STORAGE_SYSTEM_SNAPSHOT.toString());

        for (SnapshotOperation op : List.of(SnapshotOperation.TAKE, SnapshotOperation.BACKUP)) {
            Assert.assertEquals(StrategyPriority.HIGHEST, storageSystemSnapshotStrategy.canHandle(snapshot, null, op));
        }
    }

    private void mockDataStoreCapability(long storeId, String capability) {
        DataStore dataStore = Mockito.mock(DataStore.class);
        DataStoreDriver driver = Mockito.mock(DataStoreDriver.class);
        Map<String, String> capabilities = new HashMap<>();
        capabilities.put(capability, Boolean.TRUE.toString());
        Mockito.when(driver.getCapabilities()).thenReturn(capabilities);
        Mockito.when(dataStore.getDriver()).thenReturn(driver);
        Mockito.when(dataStoreMgr.getDataStore(storeId, DataStoreRole.Primary)).thenReturn(dataStore);
    }

    @Test
    public void canHandleRevertTestVolumeOnManagedLastPool() {
        VolumeVO volumeVO = Mockito.mock(VolumeVO.class);
        Mockito.when(volumeVO.getRemoved()).thenReturn(null);
        Mockito.when(volumeVO.getFormat()).thenReturn(ImageFormat.RAW);
        Mockito.when(volumeVO.getPoolId()).thenReturn(null);
        Mockito.when(volumeVO.getLastPoolId()).thenReturn(20L);
        Snapshot snapshot = mockSnapshotOfVolume(volumeVO);

        StoragePoolVO pool = Mockito.mock(StoragePoolVO.class);
        Mockito.when(pool.isManaged()).thenReturn(true);
        Mockito.when(storagePoolDao.findById(20L)).thenReturn(pool);

        Assert.assertEquals(StrategyPriority.HIGHEST, storageSystemSnapshotStrategy.canHandle(snapshot, null, SnapshotOperation.REVERT));
    }
}

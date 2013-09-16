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
package org.apache.cloudstack.storage.test;

import com.cloud.hypervisor.Hypervisor;
import com.cloud.storage.ScopeType;
import com.cloud.storage.Snapshot;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.Storage;
import com.cloud.storage.StoragePoolStatus;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.utils.component.ComponentContext;
import junit.framework.Assert;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreProvider;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreDriver;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreProvider;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotResult;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotService;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeService;
import org.apache.cloudstack.storage.datastore.PrimaryDataStore;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreVO;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.inject.Inject;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:/fakeDriverTestContext.xml" })
public class SnapshotTestWithFakeData {
    @Inject
    SnapshotService snapshotService;
    @Inject
    SnapshotDao snapshotDao;
    @Inject
    PrimaryDataStoreDao primaryDataStoreDao;
    @Inject
    DataStoreManager dataStoreManager;
    @Inject
    SnapshotDataFactory snapshotDataFactory;
    @Inject
    PrimaryDataStoreProvider primaryDataStoreProvider;
    @Inject
    SnapshotDataStoreDao snapshotDataStoreDao;
    @Inject
    VolumeDao volumeDao;
    @Inject
    VolumeService volumeService;
    @Inject
    VolumeDataFactory volumeDataFactory;
    FakePrimaryDataStoreDriver driver = new FakePrimaryDataStoreDriver();

    @Before
    public void setUp() {
        Mockito.when(primaryDataStoreProvider.configure(Mockito.anyMap())).thenReturn(true);
        Set<DataStoreProvider.DataStoreProviderType> types = new HashSet<DataStoreProvider.DataStoreProviderType>();
        types.add(DataStoreProvider.DataStoreProviderType.PRIMARY);

        Mockito.when(primaryDataStoreProvider.getTypes()).thenReturn(types);
        Mockito.when(primaryDataStoreProvider.getName()).thenReturn(DataStoreProvider.DEFAULT_PRIMARY);
        Mockito.when(primaryDataStoreProvider.getDataStoreDriver()).thenReturn(driver);

        ComponentContext.initComponentsLifeCycle();
    }
    private SnapshotVO createSnapshotInDb() {
        Snapshot.Type snapshotType = Snapshot.Type.MANUAL;
        SnapshotVO snapshotVO = new SnapshotVO(1, 2, 1, 1L, 1L, UUID.randomUUID()
                .toString(), (short) snapshotType.ordinal(), snapshotType.name(), 100,
                Hypervisor.HypervisorType.XenServer);
        return this.snapshotDao.persist(snapshotVO);
    }

    private VolumeInfo createVolume(Long templateId, DataStore store) {
        VolumeVO volume = new VolumeVO(Volume.Type.DATADISK, UUID.randomUUID().toString(), 1L, 1L, 1L, 1L, 1000, 0L, 0L, "");
        ;
        volume.setPoolId(store.getId());

        volume = volumeDao.persist(volume);
        VolumeInfo volumeInfo = volumeDataFactory.getVolume(volume.getId(), store);
        volumeInfo.stateTransit(Volume.Event.CreateRequested);
        volumeInfo.stateTransit(Volume.Event.OperationSucceeded);
        return volumeInfo;
    }
    private DataStore createDataStore() throws URISyntaxException {
        StoragePoolVO pool = new StoragePoolVO();
        pool.setClusterId(1L);
        pool.setDataCenterId(1);
        URI uri = new URI("nfs://jfkdkf/fjdkfj");
        pool.setHostAddress(uri.getHost());
        pool.setPath(uri.getPath());
        pool.setPort(0);
        pool.setName(UUID.randomUUID().toString());
        pool.setUuid(UUID.randomUUID().toString());
        pool.setStatus(StoragePoolStatus.Up);
        pool.setPoolType(Storage.StoragePoolType.NetworkFilesystem);
        pool.setPodId(1L);
        pool.setScope(ScopeType.CLUSTER);
        pool.setStorageProviderName(DataStoreProvider.DEFAULT_PRIMARY);
        pool = this.primaryDataStoreDao.persist(pool);
        DataStore store = this.dataStoreManager.getPrimaryDataStore(pool.getId());
        return store;
    }
    @Test
    public void testTakeSnapshot() throws URISyntaxException {
        SnapshotVO snapshotVO = createSnapshotInDb();
        DataStore store = createDataStore();
        try {
            SnapshotInfo snapshotInfo = snapshotDataFactory.getSnapshot(snapshotVO.getId(), store);
            SnapshotResult result = snapshotService.takeSnapshot(snapshotInfo);
            Assert.assertTrue(result.isSuccess());
            SnapshotDataStoreVO storeRef = snapshotDataStoreDao.findByStoreSnapshot(store.getRole(), store.getId(), snapshotVO.getId());
            Assert.assertTrue(storeRef != null);
            Assert.assertTrue(storeRef.getState() == ObjectInDataStoreStateMachine.State.Ready);
            snapshotInfo = result.getSnashot();
            boolean deletResult = snapshotService.deleteSnapshot(snapshotInfo);
            Assert.assertTrue(deletResult);
            snapshotDataStoreDao.expunge(storeRef.getId());
        } finally {
            snapshotDao.expunge(snapshotVO.getId());
            primaryDataStoreDao.remove(store.getId());
        }
    }

    @Test
    public void testTakeSnapshotWithFailed() throws URISyntaxException {
        SnapshotVO snapshotVO = createSnapshotInDb();
        DataStore store = null;
        try {
            store = createDataStore();
            FakePrimaryDataStoreDriver dataStoreDriver = (FakePrimaryDataStoreDriver)store.getDriver();
            dataStoreDriver.makeTakeSnapshotSucceed(false);
            SnapshotInfo snapshotInfo = snapshotDataFactory.getSnapshot(snapshotVO.getId(), store);
            SnapshotResult result = snapshotService.takeSnapshot(snapshotInfo);
            Assert.assertFalse(result.isSuccess());
            SnapshotDataStoreVO storeRef = snapshotDataStoreDao.findByStoreSnapshot(store.getRole(), store.getId(), snapshotVO.getId());
            Assert.assertTrue(storeRef == null);
        } finally {
            snapshotDao.expunge(snapshotVO.getId());
            if (store != null) {
                primaryDataStoreDao.remove(store.getId());
            }
        }
    }

    @Test
    public void testTakeSnapshotFromVolume() throws URISyntaxException {
        DataStore store = createDataStore();
        FakePrimaryDataStoreDriver dataStoreDriver = (FakePrimaryDataStoreDriver)store.getDriver();
        dataStoreDriver.makeTakeSnapshotSucceed(false);
        VolumeInfo volumeInfo = createVolume(1L, store);
        Assert.assertTrue(volumeInfo.getState() == Volume.State.Ready);
        SnapshotInfo result = volumeService.takeSnapshot(volumeInfo);
        Assert.assertTrue(volumeInfo.getState() == Volume.State.Ready);
        Assert.assertTrue(result == null);
    }

}

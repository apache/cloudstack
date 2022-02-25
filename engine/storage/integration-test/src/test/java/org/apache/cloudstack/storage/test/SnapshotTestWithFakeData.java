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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreProvider;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreProvider;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotResult;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotService;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeService;
import org.apache.cloudstack.storage.datastore.db.ImageStoreDao;
import org.apache.cloudstack.storage.datastore.db.ImageStoreVO;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreVO;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.volume.VolumeObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.cloud.dc.ClusterVO;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.HostPodVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.org.Cluster;
import com.cloud.org.Managed;
import com.cloud.server.LockControllerListener;
import com.cloud.storage.CreateSnapshotPayload;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.ScopeType;
import com.cloud.storage.Snapshot;
import com.cloud.storage.Snapshot.LocationType;
import com.cloud.storage.SnapshotPolicyVO;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.Storage;
import com.cloud.storage.StoragePoolStatus;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.storage.dao.SnapshotPolicyDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.User;
import com.cloud.utils.DateUtil;
import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.db.Merovingian2;

import junit.framework.Assert;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:/fakeDriverTestContext.xml"})
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
    @Inject
    DataCenterDao dcDao;
    Long dcId;
    @Inject
    HostPodDao podDao;
    Long podId;
    @Inject
    ClusterDao clusterDao;
    Long clusterId;
    @Inject
    ImageStoreDao imageStoreDao;
    ImageStoreVO imageStore;
    @Inject
    AccountManager accountManager;
    LockControllerListener lockControllerListener;
    VolumeInfo vol = null;
    FakePrimaryDataStoreDriver driver = new FakePrimaryDataStoreDriver();
    @Inject
    MockStorageMotionStrategy mockStorageMotionStrategy;
    Merovingian2 _lockController;
    @Inject
    SnapshotPolicyDao snapshotPolicyDao;

    @Before
    public void setUp() {
        // create data center

        DataCenterVO dc =
                new DataCenterVO(UUID.randomUUID().toString(), "test", "8.8.8.8", null, "10.0.0.1", null, "10.0.0.1/24", null, null, DataCenter.NetworkType.Basic, null,
                        null, true, true, null, null);
        dc = dcDao.persist(dc);
        dcId = dc.getId();
        // create pod

        HostPodVO pod = new HostPodVO(UUID.randomUUID().toString(), dc.getId(), "10.223.0.1", "10.233.2.2/25", 8, "test");
        pod = podDao.persist(pod);
        podId = pod.getId();
        // create xenserver cluster
        ClusterVO cluster = new ClusterVO(dc.getId(), pod.getId(), "devcloud cluster");
        cluster.setHypervisorType(Hypervisor.HypervisorType.XenServer.toString());
        cluster.setClusterType(Cluster.ClusterType.CloudManaged);
        cluster.setManagedState(Managed.ManagedState.Managed);
        cluster = clusterDao.persist(cluster);
        clusterId = cluster.getId();

        imageStore = new ImageStoreVO();
        imageStore.setName(UUID.randomUUID().toString());
        imageStore.setDataCenterId(dcId);
        imageStore.setProviderName(DataStoreProvider.NFS_IMAGE);
        imageStore.setRole(DataStoreRole.Image);
        imageStore.setUrl(UUID.randomUUID().toString());
        imageStore.setUuid(UUID.randomUUID().toString());
        imageStore.setProtocol("nfs");
        imageStore = imageStoreDao.persist(imageStore);

        when(primaryDataStoreProvider.configure(Matchers.anyMap())).thenReturn(true);
        Set<DataStoreProvider.DataStoreProviderType> types = new HashSet<DataStoreProvider.DataStoreProviderType>();
        types.add(DataStoreProvider.DataStoreProviderType.PRIMARY);

        when(primaryDataStoreProvider.getTypes()).thenReturn(types);
        when(primaryDataStoreProvider.getName()).thenReturn(DataStoreProvider.DEFAULT_PRIMARY);
        when(primaryDataStoreProvider.getDataStoreDriver()).thenReturn(driver);
        User user = mock(User.class);
        when(user.getId()).thenReturn(1L);
        Account account = mock(Account.class);
        when(account.getId()).thenReturn(1L);
        when(accountManager.getSystemAccount()).thenReturn(account);
        when(accountManager.getSystemUser()).thenReturn(user);

        if (Merovingian2.getLockController() == null) {
            _lockController = Merovingian2.createLockController(1234);
        } else {
            _lockController = Merovingian2.getLockController();
        }
        _lockController.cleanupThisServer();
        ComponentContext.initComponentsLifeCycle();
    }

    @After
    public void tearDown() throws Exception {
        _lockController.cleanupThisServer();
    }

    private SnapshotVO createSnapshotInDb() {
        Snapshot.Type snapshotType = Snapshot.Type.RECURRING;
        SnapshotVO snapshotVO =
                new SnapshotVO(dcId, 2, 1, 1L, 1L, UUID.randomUUID().toString(), (short)snapshotType.ordinal(), snapshotType.name(), 100, 1L, 100L, Hypervisor.HypervisorType.XenServer,
                        LocationType.PRIMARY);
        return snapshotDao.persist(snapshotVO);
    }

    private SnapshotVO createSnapshotInDb(Long volumeId) {
        Snapshot.Type snapshotType = Snapshot.Type.DAILY;
        SnapshotVO snapshotVO =
                new SnapshotVO(dcId, 2, 1, 1L, 1L, UUID.randomUUID().toString(), (short)snapshotType.ordinal(), snapshotType.name(), 100, 1L, 100L, Hypervisor.HypervisorType.XenServer,
                        LocationType.PRIMARY);
        return snapshotDao.persist(snapshotVO);
    }

    private VolumeInfo createVolume(Long templateId, DataStore store) {
        VolumeVO volume = new VolumeVO(Volume.Type.DATADISK, UUID.randomUUID().toString(), dcId, 1L, 1L, 1L, Storage.ProvisioningType.THIN, 1000, 0L, 0L, "");
        volume.setPoolId(store.getId());

        volume = volumeDao.persist(volume);
        VolumeInfo volumeInfo = volumeDataFactory.getVolume(volume.getId(), store);
        volumeInfo.stateTransit(Volume.Event.CreateRequested);
        volumeInfo.stateTransit(Volume.Event.OperationSucceeded);
        return volumeInfo;
    }

    private DataStore createDataStore() throws URISyntaxException {
        StoragePoolVO pool = new StoragePoolVO();
        pool.setClusterId(clusterId);
        pool.setDataCenterId(dcId);
        URI uri = new URI("nfs://jfkdkf/fjdkfj");
        pool.setHostAddress(uri.getHost());
        pool.setPath(uri.getPath());
        pool.setPort(0);
        pool.setName(UUID.randomUUID().toString());
        pool.setUuid(UUID.randomUUID().toString());
        pool.setStatus(StoragePoolStatus.Up);
        pool.setPoolType(Storage.StoragePoolType.NetworkFilesystem);
        pool.setPodId(podId);
        pool.setScope(ScopeType.CLUSTER);
        pool.setStorageProviderName(DataStoreProvider.DEFAULT_PRIMARY);
        pool = primaryDataStoreDao.persist(pool);
        DataStore store = dataStoreManager.getPrimaryDataStore(pool.getId());
        return store;
    }

    //@Test
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
            snapshotInfo = result.getSnapshot();
            boolean deletResult = snapshotService.deleteSnapshot(snapshotInfo);
            Assert.assertTrue(deletResult);
            snapshotDataStoreDao.expunge(storeRef.getId());
        } finally {
            snapshotDao.expunge(snapshotVO.getId());
            primaryDataStoreDao.remove(store.getId());
        }
    }

    //@Test
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

    //@Test
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

    protected SnapshotPolicyVO createSnapshotPolicy(Long volId) {
        SnapshotPolicyVO policyVO = new SnapshotPolicyVO(volId, "jfkd", "fdfd", DateUtil.IntervalType.DAILY, 8, true);
            policyVO = snapshotPolicyDao.persist(policyVO);
            return policyVO;
        }

        @Test
        public void testConcurrentSnapshot() throws URISyntaxException, InterruptedException, ExecutionException {
            DataStore store = createDataStore();
            final FakePrimaryDataStoreDriver dataStoreDriver = (FakePrimaryDataStoreDriver)store.getDriver();
            dataStoreDriver.makeTakeSnapshotSucceed(true);
            final VolumeInfo volumeInfo = createVolume(1L, store);
            Assert.assertTrue(volumeInfo.getState() == Volume.State.Ready);
            vol = volumeInfo;
            // final SnapshotPolicyVO policyVO = createSnapshotPolicy(vol.getId());

            ExecutorService pool = Executors.newFixedThreadPool(2);
            boolean result = false;
            List<Future<Boolean>> future = new ArrayList<Future<Boolean>>();
            for (int i = 0; i < 12; i++) {
                final int cnt = i;
                Future<Boolean> task = pool.submit(new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        boolean r = true;
                        try {
                            SnapshotVO snapshotVO = createSnapshotInDb(vol.getId());
                            VolumeObject volumeObject = (VolumeObject)vol;
                            Account account = mock(Account.class);
                            when(account.getId()).thenReturn(1L);
                            CreateSnapshotPayload createSnapshotPayload = mock(CreateSnapshotPayload.class);
                            when(createSnapshotPayload.getAccount()).thenReturn(account);
                            when(createSnapshotPayload.getSnapshotId()).thenReturn(snapshotVO.getId());
                            when(createSnapshotPayload.getSnapshotPolicyId()).thenReturn(0L);
                            volumeObject.addPayload(createSnapshotPayload);
                            if (cnt > 8) {
                                mockStorageMotionStrategy.makeBackupSnapshotSucceed(false);
                            }
                            SnapshotInfo newSnapshot = volumeService.takeSnapshot(vol);
                            if (newSnapshot == null) {
                                r = false;
                            }
                        } catch (Exception e) {
                            r = false;
                        }
                        return r;
                    }
                });
                Assert.assertTrue(task.get());
            }

        }
    }

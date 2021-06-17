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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import org.apache.cloudstack.engine.subsystem.api.storage.ClusterScope;
import org.apache.cloudstack.engine.subsystem.api.storage.DataObject;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreProvider;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPoint;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPointSelector;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreProvider;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotService;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeService;
import org.apache.cloudstack.engine.subsystem.api.storage.ZoneScope;
import org.apache.cloudstack.storage.datastore.db.ImageStoreDao;
import org.apache.cloudstack.storage.datastore.db.ImageStoreVO;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;

import com.cloud.agent.AgentManager;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.HostPodVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.org.Cluster;
import com.cloud.org.Managed;
import com.cloud.resource.ResourceState;
import com.cloud.server.LockControllerListener;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.ScopeType;
import com.cloud.storage.Storage;
import com.cloud.storage.StoragePoolHostVO;
import com.cloud.storage.StoragePoolStatus;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.storage.dao.SnapshotPolicyDao;
import com.cloud.storage.dao.StoragePoolHostDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.User;
import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.db.Merovingian2;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:/fakeDriverTestContext.xml"})
public class EndpointSelectorTest {
    @Inject
    SnapshotService snapshotService;
    @Inject
    SnapshotDao snapshotDao;
    @Inject
    SnapshotDataFactory snapshotDataFactory;
    @Inject
    PrimaryDataStoreProvider primaryDataStoreProvider;
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
    DataStoreManager dataStoreManager;
    @Inject
    PrimaryDataStoreDao primaryDataStoreDao;
    @Inject
    SnapshotPolicyDao snapshotPolicyDao;
    @Inject
    HostDao hostDao;
    @Inject
    StoragePoolHostDao storagePoolHostDao;
    @Inject
    EndPointSelector endPointSelector;
    @Inject
    AgentManager agentMgr;

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

    public DataStore createPrimaryDataStore(ScopeType scope) {
        String uuid = UUID.randomUUID().toString();
        List<StoragePoolVO> pools = primaryDataStoreDao.findPoolByName(uuid);
        if (pools.size() > 0) {
            return dataStoreManager.getPrimaryDataStore(pools.get(0).getId());
        }

        StoragePoolVO pool = new StoragePoolVO();
        if (scope != ScopeType.ZONE) {
            pool.setClusterId(clusterId);
        }
        pool.setDataCenterId(dcId);

        pool.setHostAddress(uuid);
        pool.setPath(uuid);
        pool.setPort(0);
        pool.setName(uuid);
        pool.setUuid(uuid);
        pool.setStatus(StoragePoolStatus.Up);
        pool.setPoolType(Storage.StoragePoolType.NetworkFilesystem);
        pool.setPodId(podId);
        pool.setScope(scope);
        pool.setStorageProviderName(DataStoreProvider.DEFAULT_PRIMARY);
        pool = primaryDataStoreDao.persist(pool);
        DataStore store = dataStoreManager.getPrimaryDataStore(pool.getId());
        return store;
    }

    public HostVO createHost(Hypervisor.HypervisorType hypervisorType) {
        String uuid = UUID.randomUUID().toString();
        HostVO host = new HostVO(uuid);
        host.setName("devcloud xenserver host");
        host.setType(Host.Type.Routing);
        host.setPrivateIpAddress(uuid);
        host.setDataCenterId(dcId);
        host.setVersion("6.0.1");
        host.setAvailable(true);
        host.setSetup(true);
        host.setPodId(podId);
        host.setLastPinged(0);
        host.setResourceState(ResourceState.Enabled);
        host.setHypervisorType(hypervisorType);
        host.setClusterId(clusterId);

        host = hostDao.persist(host);
        agentMgr.agentStatusTransitTo(host, Status.Event.AgentConnected, 1L);
        host = hostDao.findById(host.getId());
        agentMgr.agentStatusTransitTo(host, Status.Event.Ready, 1L);
        return hostDao.findById(host.getId());
    }

    public void addStorageToHost(DataStore store, HostVO host) {
        StoragePoolHostVO storagePoolHostVO = new StoragePoolHostVO(store.getId(), host.getId(), UUID.randomUUID().toString());
        storagePoolHostDao.persist(storagePoolHostVO);
    }

    @Test
    public void testMixZonePrimaryStorages() {
        Long srcStoreId = null;
        Long destStoreId = imageStore.getId();
        DataStore store = createPrimaryDataStore(ScopeType.ZONE);
        srcStoreId = store.getId();
        HostVO host = createHost(Hypervisor.HypervisorType.VMware);
        addStorageToHost(store, host);

        store = createPrimaryDataStore(ScopeType.ZONE);
        host = createHost(Hypervisor.HypervisorType.VMware);
        addStorageToHost(store, host);

        Long xenStoreId = null;
        store = createPrimaryDataStore(ScopeType.CLUSTER);
        xenStoreId = store.getId();
        host = createHost(Hypervisor.HypervisorType.XenServer);
        addStorageToHost(store, host);

        store = createPrimaryDataStore(ScopeType.CLUSTER);
        host = createHost(Hypervisor.HypervisorType.XenServer);
        addStorageToHost(store, host);

        ZoneScope srcScope = new ZoneScope(dcId);

        DataStore srcStore = mock(DataStore.class);
        DataStore destStore = mock(DataStore.class);

        when(srcStore.getScope()).thenReturn(srcScope);
        when(srcStore.getRole()).thenReturn(DataStoreRole.Primary);
        when(srcStore.getId()).thenReturn(srcStoreId);
        when(destStore.getScope()).thenReturn(srcScope);
        when(destStore.getRole()).thenReturn(DataStoreRole.Image);
        when(destStore.getId()).thenReturn(destStoreId);

        DataObject srcObj = mock(DataObject.class);
        DataObject destObj = mock(DataObject.class);
        when(srcObj.getDataStore()).thenReturn(srcStore);
        when(destObj.getDataStore()).thenReturn(destStore);
        EndPoint ep = endPointSelector.select(srcObj, destObj);

        Assert.assertTrue(ep != null);
        Long hostId = ep.getId();
        HostVO newHost = hostDao.findById(hostId);
        Assert.assertTrue(newHost.getHypervisorType() == Hypervisor.HypervisorType.VMware);

        when(srcStore.getRole()).thenReturn(DataStoreRole.Image);
        when(srcStore.getId()).thenReturn(destStoreId);
        when(destStore.getId()).thenReturn(srcStoreId);
        when(destStore.getRole()).thenReturn(DataStoreRole.Primary);
        ep = endPointSelector.select(srcObj, destObj);

        Assert.assertTrue(ep != null);
        hostId = ep.getId();
        newHost = hostDao.findById(hostId);
        Assert.assertTrue(newHost.getHypervisorType() == Hypervisor.HypervisorType.VMware);

        ClusterScope clusterScope = new ClusterScope(clusterId, podId, dcId);
        when(srcStore.getRole()).thenReturn(DataStoreRole.Primary);
        when(srcStore.getScope()).thenReturn(clusterScope);
        when(srcStore.getId()).thenReturn(xenStoreId);
        ep = endPointSelector.select(srcStore);
        Assert.assertTrue(ep != null);
        newHost = hostDao.findById(ep.getId());
        Assert.assertTrue(newHost.getHypervisorType() == Hypervisor.HypervisorType.XenServer);

    }

}

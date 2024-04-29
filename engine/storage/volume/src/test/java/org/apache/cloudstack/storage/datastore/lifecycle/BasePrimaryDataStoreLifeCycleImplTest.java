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

package org.apache.cloudstack.storage.datastore.lifecycle;

import static org.mockito.ArgumentMatchers.eq;

import java.util.Arrays;
import java.util.List;

import org.apache.cloudstack.engine.subsystem.api.storage.ClusterScope;
import org.apache.cloudstack.storage.datastore.PrimaryDataStoreImpl;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.volume.datastore.PrimaryDataStoreHelper;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.DeleteStoragePoolCommand;
import com.cloud.host.HostVO;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.resource.ResourceManager;
import com.cloud.storage.Storage;
import com.cloud.storage.StorageManager;
import com.cloud.storage.StoragePoolHostVO;
import com.cloud.storage.dao.StoragePoolHostDao;
import com.cloud.utils.Pair;

@RunWith(MockitoJUnitRunner.class)
public class BasePrimaryDataStoreLifeCycleImplTest {

    @Mock
    private StoragePoolHostDao storagePoolHostDao;

    @Mock
    private PrimaryDataStoreHelper dataStoreHelper;

    @Mock
    private AgentManager agentManager;

    @Mock
    private ResourceManager resourceManager;

    @Mock
    private StorageManager storageManager;

    @Spy
    @InjectMocks
    private BasePrimaryDataStoreLifeCycleImpl dataStoreLifeCycle;

    private static final Long POOL_ID = 1L;
    private static final Long CLUSTER_ID = 2L;
    private static final Long POD_ID = 3L;
    private static final Long ZONE_ID = 4L;
    private static final Long HOST_ID = 5L;

    private static ClusterScope clusterScope;
    private static PrimaryDataStoreImpl store;


    @BeforeClass
    public static void init() {
        clusterScope = new ClusterScope(CLUSTER_ID, POD_ID, ZONE_ID);
        StoragePoolVO pool = new StoragePoolVO(POOL_ID, null, null, Storage.StoragePoolType.NetworkFilesystem, 0L, 0L, 0L, 0L, null, 0, null);
        store = new PrimaryDataStoreImpl();
        store.configure(pool, null, null);
    }

    @Test
    public void testChangeStoragePoolScopeToZone() throws Exception {
        Mockito.when(resourceManager.listAllHostsInOneZoneNotInClusterByHypervisor(HypervisorType.KVM, ZONE_ID, CLUSTER_ID)).thenReturn(null);

        dataStoreLifeCycle.changeStoragePoolScopeToZone(store, clusterScope, HypervisorType.KVM);

        Mockito.verify(dataStoreHelper, Mockito.times(1)).switchToZone(store, HypervisorType.KVM);

        HostVO host = new HostVO(null);
        ReflectionTestUtils.setField(host, "id", HOST_ID);
        List<HypervisorType> hypervisorTypes = Arrays.asList(HypervisorType.KVM, HypervisorType.VMware);
        Mockito.when(resourceManager.listAllHostsInOneZoneNotInClusterByHypervisors(hypervisorTypes, ZONE_ID, CLUSTER_ID)).thenReturn(Arrays.asList(host));
        Mockito.when(storageManager.connectHostToSharedPool(HOST_ID, POOL_ID)).thenReturn(true);

        dataStoreLifeCycle.changeStoragePoolScopeToZone(store, clusterScope, null);

        Mockito.verify(dataStoreHelper, Mockito.times(1)).switchToZone(store, null);
    }

    @Test
    public void testChangeStoragePoolScopeToCluster() {
        Pair<List<StoragePoolHostVO>, Integer> hostPoolRecords = new Pair<>(null, 0);
        Mockito.when(storagePoolHostDao.listByPoolIdNotInCluster(CLUSTER_ID, POOL_ID)).thenReturn(hostPoolRecords);
        Mockito.doNothing().when(dataStoreHelper).switchToCluster(store, clusterScope);

        dataStoreLifeCycle.changeStoragePoolScopeToCluster(store, clusterScope, HypervisorType.KVM);

        hostPoolRecords.set(Arrays.asList(new StoragePoolHostVO(POOL_ID, HOST_ID, null)), 1);
        Answer answer = new Answer(null, false, null);
        Mockito.when(storagePoolHostDao.listByPoolIdNotInCluster(CLUSTER_ID, POOL_ID)).thenReturn(hostPoolRecords);
        Mockito.when(agentManager.easySend(eq(HOST_ID), Mockito.any(DeleteStoragePoolCommand.class))).thenReturn(answer);

        dataStoreLifeCycle.changeStoragePoolScopeToCluster(store, clusterScope, HypervisorType.KVM);

        Mockito.verify(dataStoreHelper, Mockito.times(2)).switchToCluster(store, clusterScope);
    }
}

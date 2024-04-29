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

package org.apache.cloudstack.storage.volume.datastore;

import java.util.List;

import org.apache.cloudstack.engine.subsystem.api.storage.ClusterScope;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.capacity.Capacity;
import com.cloud.capacity.CapacityVO;
import com.cloud.capacity.dao.CapacityDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.storage.ScopeType;
import com.cloud.storage.Storage;
import com.cloud.storage.StoragePoolHostVO;
import com.cloud.storage.dao.StoragePoolHostDao;
import com.cloud.utils.Pair;

@RunWith(MockitoJUnitRunner.class)
public class PrimaryDataStoreHelperTest {

    @Mock
    private PrimaryDataStoreDao dataStoreDao;

    @Mock
    private CapacityDao capacityDao;

    @Mock
    private StoragePoolHostDao storagePoolHostDao;

    @Spy
    @InjectMocks
    PrimaryDataStoreHelper dataStoreHelper;

    private static final Long ZONE_ID = 1L;
    private static final Long CLUSTER_ID = 2L;
    private static final Long POD_ID = 3L;
    private static final Long POOL_ID = 4L;
    private static final Short capacityType = 0;
    private static final Float usedPercentage = 0.0f;

    @Test
    public void testSwitchToZone() {
        StoragePoolVO pool = new StoragePoolVO(POOL_ID, null, null, Storage.StoragePoolType.NetworkFilesystem, ZONE_ID, POD_ID, 0L, 0L, null, 0, null);
        pool.setClusterId(CLUSTER_ID);
        pool.setScope(ScopeType.CLUSTER);
        CapacityVO capacity = new CapacityVO(ZONE_ID, POD_ID, CLUSTER_ID, capacityType, usedPercentage);

        Mockito.when(dataStoreDao.findById(pool.getId())).thenReturn(pool);
        Mockito.when(capacityDao.findByHostIdType(pool.getId(), Capacity.CAPACITY_TYPE_STORAGE_ALLOCATED)).thenReturn(capacity);
        DataStore storeMock = Mockito.mock(DataStore.class);
        Mockito.when(storeMock.getId()).thenReturn(POOL_ID);

        dataStoreHelper.switchToZone(storeMock, HypervisorType.KVM);

        Assert.assertEquals(pool.getScope(), ScopeType.ZONE);
        Assert.assertEquals(pool.getPodId(), null);
        Assert.assertEquals(pool.getClusterId(), null);
        Assert.assertEquals(pool.getHypervisor(), HypervisorType.KVM);
        Assert.assertEquals(capacity.getPodId(), null);
        Assert.assertEquals(capacity.getClusterId(), null);
    }

    @Test
    public void testSwitchToCluster() {
        StoragePoolVO pool = new StoragePoolVO(POOL_ID, null, null, Storage.StoragePoolType.NetworkFilesystem, ZONE_ID, null, 0L, 0L, null, 0, null);
        pool.setScope(ScopeType.ZONE);
        CapacityVO capacity = new CapacityVO(ZONE_ID, null, null, capacityType, usedPercentage);
        ClusterScope clusterScope = new ClusterScope(CLUSTER_ID, POD_ID, ZONE_ID);

        Pair<List<StoragePoolHostVO>, Integer> hostPoolRecords = new Pair<>(null, 0);
        Mockito.when(storagePoolHostDao.listByPoolIdNotInCluster(CLUSTER_ID, POOL_ID)).thenReturn(hostPoolRecords);
        Mockito.when(dataStoreDao.findById(pool.getId())).thenReturn(pool);
        Mockito.when(capacityDao.findByHostIdType(pool.getId(), Capacity.CAPACITY_TYPE_STORAGE_ALLOCATED)).thenReturn(capacity);
        DataStore storeMock = Mockito.mock(DataStore.class);
        Mockito.when(storeMock.getId()).thenReturn(POOL_ID);

        dataStoreHelper.switchToCluster(storeMock, clusterScope);

        Mockito.verify(storagePoolHostDao, Mockito.never()).deleteStoragePoolHostDetails(Mockito.anyLong(), Mockito.anyLong());

        Assert.assertEquals(pool.getScope(), ScopeType.CLUSTER);
        Assert.assertEquals(pool.getPodId(), POD_ID);
        Assert.assertEquals(pool.getClusterId(), CLUSTER_ID);
        Assert.assertEquals(capacity.getPodId(), POD_ID);
        Assert.assertEquals(capacity.getClusterId(), CLUSTER_ID);
    }
}

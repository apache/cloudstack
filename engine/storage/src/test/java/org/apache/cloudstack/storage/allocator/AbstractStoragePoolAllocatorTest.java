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
package org.apache.cloudstack.storage.allocator;


import com.cloud.capacity.Capacity;
import com.cloud.capacity.dao.CapacityDao;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.deploy.DeploymentPlanner;
import com.cloud.storage.Storage;
import com.cloud.storage.StoragePool;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.user.Account;
import com.cloud.utils.Pair;
import com.cloud.vm.DiskProfile;
import com.cloud.vm.VirtualMachineProfile;
import org.apache.cloudstack.engine.orchestration.service.VolumeOrchestrationService;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AbstractStoragePoolAllocatorTest {

    AbstractStoragePoolAllocator allocator = Mockito.spy(MockStorapoolAllocater.class);

    @Mock
    DeploymentPlan plan;

    @Mock
    Account account;

    @Mock
    CapacityDao capacityDao;

    private List<StoragePool> pools;

    @Mock
    VolumeDao volumeDao;

    @Before
    public void setUp() {
        pools = new ArrayList<>();
        for (int i = 0 ; i < 10 ; ++i) {
            pools.add(new StoragePoolVO(i, "pool-"+i, "pool-"+i+"-uuid", Storage.StoragePoolType.NetworkFilesystem,
                    1, 1l, 10000000000l, 10000000000l, "10.10.10.10",
                    1000, "/spool/share-" + i));
        }
    }

    @After
    public void tearDown() {
    }

    @Test
    public void reorderStoragePoolsBasedOnAlgorithm_random() throws Exception {
        overrideDefaultConfigValue( VolumeOrchestrationService.VolumeAllocationAlgorithm, "random");
        allocator.reorderStoragePoolsBasedOnAlgorithm(pools, plan, account);
        Mockito.verify(allocator, Mockito.times(0)).reorderPoolsByCapacity(plan, pools);
        Mockito.verify(allocator, Mockito.times(0)).reorderPoolsByNumberOfVolumes(plan, pools, account);
        Mockito.verify(allocator, Mockito.times(1)).reorderRandomPools(pools);
    }

    @Test
    public void reorderStoragePoolsBasedOnAlgorithm_userdispersing() throws Exception {
        overrideDefaultConfigValue(VolumeOrchestrationService.VolumeAllocationAlgorithm, "userdispersing");
        Mockito.doReturn(pools).when(allocator).reorderPoolsByNumberOfVolumes(plan, pools, account);
        allocator.reorderStoragePoolsBasedOnAlgorithm(pools, plan, account);
        Mockito.verify(allocator, Mockito.times(0)).reorderPoolsByCapacity(plan, pools);
        Mockito.verify(allocator, Mockito.times(1)).reorderPoolsByNumberOfVolumes(plan, pools, account);
        Mockito.verify(allocator, Mockito.times(0)).reorderRandomPools(pools);
    }

    @Test
    public void reorderStoragePoolsBasedOnAlgorithm_userdispersing_reorder_check() throws Exception {
        overrideDefaultConfigValue(VolumeOrchestrationService.VolumeAllocationAlgorithm, "userdispersing");
        allocator.volumeDao = volumeDao;
        when(plan.getDataCenterId()).thenReturn(1l);
        when(plan.getPodId()).thenReturn(1l);
        when(plan.getClusterId()).thenReturn(1l);
        when(account.getAccountId()).thenReturn(1l);
        List<Long> poolIds = new ArrayList<>();
        poolIds.add(1l);
        poolIds.add(9l);
        when(volumeDao.listPoolIdsByVolumeCount(1l,1l,1l,1l)).thenReturn(poolIds);

        List<StoragePool> reorderedPools = allocator.reorderStoragePoolsBasedOnAlgorithm(pools, plan, account);
        Assert.assertEquals(poolIds.size(),reorderedPools.size());

        Mockito.verify(allocator, Mockito.times(0)).reorderPoolsByCapacity(plan, pools);
        Mockito.verify(allocator, Mockito.times(1)).reorderPoolsByNumberOfVolumes(plan, pools, account);
        Mockito.verify(allocator, Mockito.times(0)).reorderRandomPools(pools);
        Mockito.verify(volumeDao, Mockito.times(1)).listPoolIdsByVolumeCount(1l,1l,1l,1l);
    }

    @Test
    public void reorderStoragePoolsBasedOnAlgorithm_firstfitleastconsumed() throws Exception {
        overrideDefaultConfigValue(VolumeOrchestrationService.VolumeAllocationAlgorithm, "firstfitleastconsumed");
        Mockito.doReturn(pools).when(allocator).reorderPoolsByCapacity(plan, pools);
        allocator.reorderStoragePoolsBasedOnAlgorithm(pools, plan, account);
        Mockito.verify(allocator, Mockito.times(1)).reorderPoolsByCapacity(plan, pools);
        Mockito.verify(allocator, Mockito.times(0)).reorderPoolsByNumberOfVolumes(plan, pools, account);
        Mockito.verify(allocator, Mockito.times(0)).reorderRandomPools(pools);
    }

    @Test
    public void reorderRandomPools() {
        Set<Long> firstchoice = new HashSet<>();
        for (int i = 0; i <= 20; ++i) {
            allocator.reorderRandomPools(pools);
            firstchoice.add(pools.get(0).getId());
        }
        Assert.assertTrue(firstchoice.size() > 2);
    }

    @Test
    public void reorderStoragePoolsBasedOnAlgorithmFirstFitLeastConsumed() throws Exception {
        overrideDefaultConfigValue(VolumeOrchestrationService.VolumeAllocationAlgorithm, "firstfitleastconsumed");
        when(plan.getDataCenterId()).thenReturn(1L);
        when(plan.getClusterId()).thenReturn(1L);
        StoragePool pool1 = mock(StoragePool.class);
        StoragePool pool2 = mock(StoragePool.class);
        when(pool1.getId()).thenReturn(1L);
        when(pool2.getId()).thenReturn(2L);
        List<StoragePool> pools = Arrays.asList(pool1, pool2);
        List<Long> poolIds = Arrays.asList(2L, 1L);
        Map<Long, Double> hostCapacityMap = new HashMap<>();
        hostCapacityMap.put(1L, 8.0);
        hostCapacityMap.put(2L, 8.5);
        Pair<List<Long>, Map<Long, Double>> poolsOrderedByCapacity = new Pair<>(poolIds, hostCapacityMap);

        allocator.capacityDao = capacityDao;
        Mockito.when(capacityDao.orderHostsByFreeCapacity(1L, 1L, Capacity.CAPACITY_TYPE_LOCAL_STORAGE)).thenReturn(poolsOrderedByCapacity);
        List<StoragePool> result = allocator.reorderPoolsByCapacity(plan, pools);
        assertEquals(Arrays.asList(pool2, pool1), result);
    }

    private void overrideDefaultConfigValue(final ConfigKey configKey, final String value) throws IllegalAccessException, NoSuchFieldException {
        final Field f = ConfigKey.class.getDeclaredField("_defaultValue");
        f.setAccessible(true);
        f.set(configKey, value);
    }
}

class MockStorapoolAllocater extends AbstractStoragePoolAllocator {

    @Override
    protected List<StoragePool> select(DiskProfile dskCh, VirtualMachineProfile vmProfile, DeploymentPlan plan, DeploymentPlanner.ExcludeList avoid, int returnUpTo, boolean bypassStorageTypeCheck, String keyword) {
        return null;
    }
}

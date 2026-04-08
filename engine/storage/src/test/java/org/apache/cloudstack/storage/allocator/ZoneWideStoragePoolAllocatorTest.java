/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.cloudstack.storage.allocator;

import com.cloud.capacity.Capacity;
import com.cloud.capacity.dao.CapacityDao;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.storage.Storage;
import com.cloud.storage.StoragePool;
import com.cloud.utils.Pair;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ZoneWideStoragePoolAllocatorTest {
    private ZoneWideStoragePoolAllocator allocator;
    private DeploymentPlan plan;

    @Before
    public void setUp() {
        allocator = new ZoneWideStoragePoolAllocator();
        plan = mock(DeploymentPlan.class);
    }

    @Test
    public void testReorderPoolsByCapacity() {
        when(plan.getDataCenterId()).thenReturn(1L);
        when(plan.getClusterId()).thenReturn(null);
        StoragePool pool1 = mock(StoragePool.class);
        StoragePool pool2 = mock(StoragePool.class);
        when(pool1.getPoolType()).thenReturn(Storage.StoragePoolType.Filesystem);
        when(pool1.getId()).thenReturn(1L);
        when(pool2.getId()).thenReturn(2L);
        List<StoragePool> pools = Arrays.asList(pool1, pool2);
        List<Long> poolIds = Arrays.asList(2L, 1L);
        Map<Long, Double> hostCapacityMap = new HashMap<>();
        hostCapacityMap.put(1L, 8.0);
        hostCapacityMap.put(2L, 8.5);
        Pair<List<Long>, Map<Long, Double>> poolsOrderedByCapacity = new Pair<>(poolIds, hostCapacityMap);
        CapacityDao capacityDao = mock(CapacityDao.class);
        Mockito.when(capacityDao.orderHostsByFreeCapacity(1L, null, Capacity.CAPACITY_TYPE_LOCAL_STORAGE)).thenReturn(poolsOrderedByCapacity);
        allocator.capacityDao = capacityDao;
        List<StoragePool> result = allocator.reorderPoolsByCapacity(plan, pools);
        assertEquals(Arrays.asList(pool2, pool1), result);
    }
}

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


import com.cloud.deploy.DeploymentPlan;
import com.cloud.deploy.DeploymentPlanner;
import com.cloud.storage.Storage;
import com.cloud.storage.StoragePool;
import com.cloud.user.Account;
import com.cloud.vm.DiskProfile;
import com.cloud.vm.VirtualMachineProfile;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RunWith(MockitoJUnitRunner.class)
public class AbstractStoragePoolAllocatorTest {

    AbstractStoragePoolAllocator allocator = Mockito.spy(MockStorapoolAllocater.class);

    @Mock
    DeploymentPlan plan;

    @Mock
    Account account;
    private List<StoragePool> pools;

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
    public void reorderStoragePoolsBasedOnAlgorithm_random() {
        allocator.reorderStoragePoolsBasedOnAlgorithm(pools, plan, account);
        Mockito.verify(allocator, Mockito.times(0)).reorderPoolsByCapacity(plan, pools);
        Mockito.verify(allocator, Mockito.times(0)).reorderPoolsByNumberOfVolumes(plan, pools, account);
        Mockito.verify(allocator, Mockito.times(1)).reorderRandomPools(pools);
    }

    @Test
    public void reorderStoragePoolsBasedOnAlgorithm_userdispersing() {
        allocator.allocationAlgorithm = "userdispersing";
        Mockito.doReturn(pools).when(allocator).reorderPoolsByNumberOfVolumes(plan, pools, account);
        allocator.reorderStoragePoolsBasedOnAlgorithm(pools, plan, account);
        Mockito.verify(allocator, Mockito.times(0)).reorderPoolsByCapacity(plan, pools);
        Mockito.verify(allocator, Mockito.times(1)).reorderPoolsByNumberOfVolumes(plan, pools, account);
        Mockito.verify(allocator, Mockito.times(0)).reorderRandomPools(pools);
    }

    @Test
    public void reorderStoragePoolsBasedOnAlgorithm_firstfitleastconsumed() {
        allocator.allocationAlgorithm = "firstfitleastconsumed";
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
}

class MockStorapoolAllocater extends AbstractStoragePoolAllocator {

    @Override
    protected List<StoragePool> select(DiskProfile dskCh, VirtualMachineProfile vmProfile, DeploymentPlan plan, DeploymentPlanner.ExcludeList avoid, int returnUpTo, boolean bypassStorageTypeCheck) {
        return null;
    }
}

// Licensed to the Apache Software Foundation (ASF) under one or more
// contributor license agreements.  See the NOTICE file distributed with
// this work for additional information regarding copyright ownership.
// The ASF licenses this file to You under the Apache License, Version 2.0
// (the "License"); you may not use this file except in compliance with
// the License.  You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.apache.cloudstack.storage.allocator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


import org.apache.log4j.Logger;

import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;

import com.cloud.deploy.DeploymentPlan;
import com.cloud.deploy.DeploymentPlanner.ExcludeList;
import com.cloud.storage.ScopeType;
import com.cloud.storage.StoragePool;
import com.cloud.vm.DiskProfile;
import com.cloud.vm.VirtualMachineProfile;

public class RandomStoragePoolAllocator extends AbstractStoragePoolAllocator {
    private static final Logger s_logger = Logger.getLogger(RandomStoragePoolAllocator.class);

    @Override
    public List<StoragePool> select(DiskProfile dskCh, VirtualMachineProfile vmProfile, DeploymentPlan plan, ExcludeList avoid, int returnUpTo, boolean bypassStorageTypeCheck) {
        logStartOfSearch(dskCh, vmProfile, plan, returnUpTo, bypassStorageTypeCheck);

        List<StoragePool> suitablePools = new ArrayList<StoragePool>();

        long dcId = plan.getDataCenterId();
        Long podId = plan.getPodId();
        Long clusterId = plan.getClusterId();

        if (podId == null) {
            s_logger.debug("RandomStoragePoolAllocator is returning null since the pod ID is null. This may be a zone wide storage.");
            return null;
        }

        s_logger.debug(String.format("Looking for pools in dc [%s], pod [%s] and cluster [%s].", dcId, podId, clusterId));
        List<StoragePoolVO> pools = storagePoolDao.listBy(dcId, podId, clusterId, ScopeType.CLUSTER);
        if (pools.size() == 0) {
            s_logger.debug(String.format("RandomStoragePoolAllocator found no storage pools available for allocation in dc [%s], pod [%s] and cluster [%s]. Returning an empty list.",
                    dcId, podId, clusterId));
            return suitablePools;
        }

        Collections.shuffle(pools);

        s_logger.debug(String.format("RandomStoragePoolAllocator has [%s] pools to check for allocation [%s].", pools.size(), pools));

        for (StoragePoolVO pool : pools) {
            if (suitablePools.size() == returnUpTo) {
                break;
            }
            StoragePool pol = (StoragePool)this.dataStoreMgr.getPrimaryDataStore(pool.getId());

            if (filter(avoid, pol, dskCh, plan)) {
                s_logger.trace(String.format("Found suitable local storage pool [%s], adding to list.", pool));
                suitablePools.add(pol);
            }
        }

        logEndOfSearch(suitablePools);

        return suitablePools;
    }
}

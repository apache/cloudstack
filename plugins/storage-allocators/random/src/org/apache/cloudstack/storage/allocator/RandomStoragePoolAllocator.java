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

import javax.ejb.Local;


import org.apache.cloudstack.engine.subsystem.api.storage.StoragePoolAllocator;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;

import com.cloud.deploy.DeploymentPlan;
import com.cloud.deploy.DeploymentPlanner.ExcludeList;
import com.cloud.storage.ScopeType;
import com.cloud.storage.StoragePool;
import com.cloud.vm.DiskProfile;
import com.cloud.vm.VirtualMachineProfile;

@Local(value = StoragePoolAllocator.class)
public class RandomStoragePoolAllocator extends AbstractStoragePoolAllocator {

    @Override
    public List<StoragePool> select(DiskProfile dskCh, VirtualMachineProfile vmProfile, DeploymentPlan plan, ExcludeList avoid, int returnUpTo) {

        List<StoragePool> suitablePools = new ArrayList<StoragePool>();

        long dcId = plan.getDataCenterId();
        Long podId = plan.getPodId();
        Long clusterId = plan.getClusterId();

        if (podId == null) {
            return null;
        }

        logger.debug("Looking for pools in dc: " + dcId + "  pod:" + podId + "  cluster:" + clusterId);
        List<StoragePoolVO> pools = _storagePoolDao.listBy(dcId, podId, clusterId, ScopeType.CLUSTER);
        if (pools.size() == 0) {
            if (logger.isDebugEnabled()) {
                logger.debug("No storage pools available for allocation, returning");
            }
            return suitablePools;
        }

        Collections.shuffle(pools);
        if (logger.isDebugEnabled()) {
            logger.debug("RandomStoragePoolAllocator has " + pools.size() + " pools to check for allocation");
        }
        for (StoragePoolVO pool : pools) {
            if (suitablePools.size() == returnUpTo) {
                break;
            }
            StoragePool pol = (StoragePool)this.dataStoreMgr.getPrimaryDataStore(pool.getId());

            if (filter(avoid, pol, dskCh, plan)) {
                suitablePools.add(pol);
            }
        }

        if (logger.isDebugEnabled()) {
            logger.debug("RandomStoragePoolAllocator returning " + suitablePools.size() + " suitable storage pools");
        }

        return suitablePools;
    }
}

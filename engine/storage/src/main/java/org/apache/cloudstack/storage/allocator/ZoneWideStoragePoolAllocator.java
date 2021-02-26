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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;

import com.cloud.capacity.Capacity;
import com.cloud.capacity.dao.CapacityDao;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.deploy.DeploymentPlanner.ExcludeList;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.storage.ScopeType;
import com.cloud.storage.StoragePool;
import com.cloud.user.Account;
import com.cloud.vm.DiskProfile;
import com.cloud.vm.VirtualMachineProfile;

@Component
public class ZoneWideStoragePoolAllocator extends AbstractStoragePoolAllocator {
    private static final Logger LOGGER = Logger.getLogger(ZoneWideStoragePoolAllocator.class);
    @Inject
    private DataStoreManager dataStoreMgr;
    @Inject
    private CapacityDao capacityDao;

    @Override
    protected List<StoragePool> select(DiskProfile dskCh, VirtualMachineProfile vmProfile, DeploymentPlan plan, ExcludeList avoid, int returnUpTo, boolean bypassStorageTypeCheck) {
        LOGGER.debug("ZoneWideStoragePoolAllocator to find storage pool");

        if (!bypassStorageTypeCheck && dskCh.useLocalStorage()) {
            return null;
        }

        if (LOGGER.isTraceEnabled()) {
            // Log the pools details that are ignored because they are in disabled state
            List<StoragePoolVO> disabledPools = storagePoolDao.findDisabledPoolsByScope(plan.getDataCenterId(), null, null, ScopeType.ZONE);
            if (disabledPools != null && !disabledPools.isEmpty()) {
                for (StoragePoolVO pool : disabledPools) {
                    LOGGER.trace("Ignoring pool " + pool + " as it is in disabled state.");
                }
            }
        }

        List<StoragePool> suitablePools = new ArrayList<>();

        List<StoragePoolVO> storagePools = storagePoolDao.findZoneWideStoragePoolsByTags(plan.getDataCenterId(), dskCh.getTags());
        if (storagePools == null) {
            storagePools = new ArrayList<>();
        }

        List<StoragePoolVO> anyHypervisorStoragePools = new ArrayList<>();
        for (StoragePoolVO storagePool : storagePools) {
            if (HypervisorType.Any.equals(storagePool.getHypervisor())) {
                anyHypervisorStoragePools.add(storagePool);
            }
        }

        List<StoragePoolVO> storagePoolsByHypervisor = storagePoolDao.findZoneWideStoragePoolsByHypervisor(plan.getDataCenterId(), dskCh.getHypervisorType());
        storagePools.retainAll(storagePoolsByHypervisor);
        storagePools.addAll(anyHypervisorStoragePools);

        // add remaining pools in zone, that did not match tags, to avoid set
        List<StoragePoolVO> allPools = storagePoolDao.findZoneWideStoragePoolsByTags(plan.getDataCenterId(), null);
        allPools.removeAll(storagePools);
        for (StoragePoolVO pool : allPools) {
            avoid.addPool(pool.getId());
        }

        for (StoragePoolVO storage : storagePools) {
            if (suitablePools.size() == returnUpTo) {
                break;
            }
            StoragePool storagePool = (StoragePool)this.dataStoreMgr.getPrimaryDataStore(storage.getId());
            if (filter(avoid, storagePool, dskCh, plan)) {
                suitablePools.add(storagePool);
            } else {
                if (canAddStoragePoolToAvoidSet(storage)) {
                    avoid.addPool(storagePool.getId());
                }
            }
        }
        return suitablePools;
    }

    // Don't add zone-wide, managed storage to the avoid list because it may be usable for another cluster.
    private boolean canAddStoragePoolToAvoidSet(StoragePoolVO storagePoolVO) {
        return !ScopeType.ZONE.equals(storagePoolVO.getScope()) || !storagePoolVO.isManaged();
    }

    @Override
    protected List<StoragePool> reorderPoolsByCapacity(DeploymentPlan plan,
        List<StoragePool> pools) {
        Long zoneId = plan.getDataCenterId();
        short capacityType;
        if(pools != null && pools.size() != 0){
            capacityType = pools.get(0).getPoolType().isShared() ? Capacity.CAPACITY_TYPE_STORAGE_ALLOCATED : Capacity.CAPACITY_TYPE_LOCAL_STORAGE;
        } else{
            return null;
        }

        List<Long> poolIdsByCapacity = capacityDao.orderHostsByFreeCapacity(zoneId, null, capacityType);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("List of zone-wide storage pools in descending order of free capacity: "+ poolIdsByCapacity);
        }

      //now filter the given list of Pools by this ordered list
      Map<Long, StoragePool> poolMap = new HashMap<>();
      for (StoragePool pool : pools) {
          poolMap.put(pool.getId(), pool);
      }
      List<Long> matchingPoolIds = new ArrayList<>(poolMap.keySet());

      poolIdsByCapacity.retainAll(matchingPoolIds);

      List<StoragePool> reorderedPools = new ArrayList<>();
      for(Long id: poolIdsByCapacity){
          reorderedPools.add(poolMap.get(id));
      }

      return reorderedPools;
    }

    @Override
    protected List<StoragePool> reorderPoolsByNumberOfVolumes(DeploymentPlan plan, List<StoragePool> pools, Account account) {
        if (account == null) {
            return pools;
        }
        long dcId = plan.getDataCenterId();

        List<Long> poolIdsByVolCount = volumeDao.listZoneWidePoolIdsByVolumeCount(dcId, account.getAccountId());
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("List of pools in ascending order of number of volumes for account id: " + account.getAccountId() + " is: " + poolIdsByVolCount);
        }

        // now filter the given list of Pools by this ordered list
        Map<Long, StoragePool> poolMap = new HashMap<>();
        for (StoragePool pool : pools) {
            poolMap.put(pool.getId(), pool);
        }
        List<Long> matchingPoolIds = new ArrayList<>(poolMap.keySet());

        poolIdsByVolCount.retainAll(matchingPoolIds);

        List<StoragePool> reorderedPools = new ArrayList<>();
        for (Long id : poolIdsByVolCount) {
            reorderedPools.add(poolMap.get(id));
        }

        return reorderedPools;
    }
}

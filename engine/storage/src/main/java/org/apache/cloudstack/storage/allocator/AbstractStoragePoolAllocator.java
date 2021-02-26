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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.StoragePoolAllocator;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.log4j.Logger;

import com.cloud.capacity.Capacity;
import com.cloud.capacity.dao.CapacityDao;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.deploy.DeploymentPlanner.ExcludeList;
import com.cloud.exception.StorageUnavailableException;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.storage.Storage;
import com.cloud.storage.StorageManager;
import com.cloud.storage.StoragePool;
import com.cloud.storage.StoragePoolStatus;
import com.cloud.storage.StorageUtil;
import com.cloud.storage.Volume;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.user.Account;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.component.AdapterBase;
import com.cloud.vm.DiskProfile;
import com.cloud.vm.VirtualMachineProfile;

public abstract class AbstractStoragePoolAllocator extends AdapterBase implements StoragePoolAllocator {
    private static final Logger s_logger = Logger.getLogger(AbstractStoragePoolAllocator.class);

    protected BigDecimal storageOverprovisioningFactor = new BigDecimal(1);
    protected String allocationAlgorithm = "random";
    protected long extraBytesPerVolume = 0;
    @Inject protected DataStoreManager dataStoreMgr;
    @Inject protected PrimaryDataStoreDao storagePoolDao;
    @Inject protected VolumeDao volumeDao;
    @Inject protected ConfigurationDao configDao;
    @Inject private CapacityDao capacityDao;
    @Inject private ClusterDao clusterDao;
    @Inject private StorageManager storageMgr;
    @Inject private StorageUtil storageUtil;

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);
        if(configDao != null) {
            Map<String, String> configs = configDao.getConfiguration(null, params);
            String globalStorageOverprovisioningFactor = configs.get("storage.overprovisioning.factor");
            storageOverprovisioningFactor = new BigDecimal(NumbersUtil.parseFloat(globalStorageOverprovisioningFactor, 2.0f));
            extraBytesPerVolume = 0;
            String allocationAlgorithm = configs.get("vm.allocation.algorithm");
            if (allocationAlgorithm != null) {
                this.allocationAlgorithm = allocationAlgorithm;
            }
            return true;
        }
        return false;
    }

    protected abstract List<StoragePool> select(DiskProfile dskCh, VirtualMachineProfile vmProfile, DeploymentPlan plan, ExcludeList avoid, int returnUpTo, boolean bypassStorageTypeCheck);

    @Override
    public List<StoragePool> allocateToPool(DiskProfile dskCh, VirtualMachineProfile vmProfile, DeploymentPlan plan, ExcludeList avoid, int returnUpTo) {
        return allocateToPool(dskCh, vmProfile, plan, avoid, returnUpTo, false);
    }

    @Override
    public List<StoragePool> allocateToPool(DiskProfile dskCh, VirtualMachineProfile vmProfile, DeploymentPlan plan, ExcludeList avoid, int returnUpTo, boolean bypassStorageTypeCheck) {
        List<StoragePool> pools = select(dskCh, vmProfile, plan, avoid, returnUpTo, bypassStorageTypeCheck);
        return reorderPools(pools, vmProfile, plan);
    }

    protected List<StoragePool> reorderPoolsByCapacity(DeploymentPlan plan,
        List<StoragePool> pools) {
        Long zoneId = plan.getDataCenterId();
        Long clusterId = plan.getClusterId();
        short capacityType;
        if(pools != null && pools.size() != 0){
            capacityType = pools.get(0).getPoolType().isShared() ? Capacity.CAPACITY_TYPE_STORAGE_ALLOCATED : Capacity.CAPACITY_TYPE_LOCAL_STORAGE;
        } else{
            return null;
        }

        List<Long> poolIdsByCapacity = capacityDao.orderHostsByFreeCapacity(zoneId, clusterId, capacityType);
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("List of pools in descending order of free capacity: "+ poolIdsByCapacity);
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

    protected List<StoragePool> reorderPoolsByNumberOfVolumes(DeploymentPlan plan, List<StoragePool> pools, Account account) {
        if (account == null) {
            return pools;
        }
        long dcId = plan.getDataCenterId();
        Long podId = plan.getPodId();
        Long clusterId = plan.getClusterId();

        List<Long> poolIdsByVolCount = volumeDao.listPoolIdsByVolumeCount(dcId, podId, clusterId, account.getAccountId());
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("List of pools in ascending order of number of volumes for account id: " + account.getAccountId() + " is: " + poolIdsByVolCount);
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

    @Override
    public List<StoragePool> reorderPools(List<StoragePool> pools, VirtualMachineProfile vmProfile, DeploymentPlan plan) {
        if (pools == null) {
            return null;
        }
        Account account = null;
        if (vmProfile.getVirtualMachine() != null) {
            account = vmProfile.getOwner();
        }

        if (allocationAlgorithm.equals("random") || allocationAlgorithm.equals("userconcentratedpod_random") || (account == null)) {
            // Shuffle this so that we don't check the pools in the same order.
            Collections.shuffle(pools);
        } else if (allocationAlgorithm.equals("userdispersing")) {
            pools = reorderPoolsByNumberOfVolumes(plan, pools, account);
        } else if(allocationAlgorithm.equals("firstfitleastconsumed")){
            pools = reorderPoolsByCapacity(plan, pools);
        }
        return pools;
    }

    protected boolean filter(ExcludeList avoid, StoragePool pool, DiskProfile dskCh, DeploymentPlan plan) {

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Checking if storage pool is suitable, name: " + pool.getName() + " ,poolId: " + pool.getId());
        }
        if (avoid.shouldAvoid(pool)) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("StoragePool is in avoid set, skipping this pool");
            }
            return false;
        }

        Long clusterId = pool.getClusterId();
        if (clusterId != null) {
            ClusterVO cluster = clusterDao.findById(clusterId);
            if (!(cluster.getHypervisorType() == dskCh.getHypervisorType())) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("StoragePool's Cluster does not have required hypervisorType, skipping this pool");
                }
                return false;
            }
        } else if (pool.getHypervisor() != null && !pool.getHypervisor().equals(HypervisorType.Any) && !(pool.getHypervisor() == dskCh.getHypervisorType())) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("StoragePool does not have required hypervisorType, skipping this pool");
            }
            return false;
        }

        if(!checkHypervisorCompatibility(dskCh.getHypervisorType(), dskCh.getType(), pool.getPoolType())){
            return false;
        }

        Volume volume = volumeDao.findById(dskCh.getVolumeId());
        if(!storageMgr.storagePoolCompatibleWithVolumePool(pool, volume)) {
            return false;
        }

        if (pool.isManaged() && !storageUtil.managedStoragePoolCanScale(pool, plan.getClusterId(), plan.getHostId())) {
            return false;
        }

        // check capacity
        List<Volume> requestVolumes = new ArrayList<>();
        requestVolumes.add(volume);
        if (dskCh.getHypervisorType() == HypervisorType.VMware) {
            // Skip the parent datastore cluster, consider only child storage pools in it
            if (pool.getPoolType() == Storage.StoragePoolType.DatastoreCluster && storageMgr.isStoragePoolDatastoreClusterParent(pool)) {
                return false;
            }
            // Skip the storage pool whose parent datastore cluster is not in UP state.
            if (pool.getParent() != 0L) {
                StoragePoolVO datastoreCluster = storagePoolDao.findById(pool.getParent());
                if (datastoreCluster == null || (datastoreCluster != null && datastoreCluster.getStatus() != StoragePoolStatus.Up)) {
                    return false;
                }
            }

            try {
                boolean isStoragePoolStoragepolicyComplaince = storageMgr.isStoragePoolComplaintWithStoragePolicy(requestVolumes, pool);
                if (!isStoragePoolStoragepolicyComplaince) {
                    return false;
                }
            } catch (StorageUnavailableException e) {
                s_logger.warn(String.format("Could not verify storage policy complaince against storage pool %s due to exception %s", pool.getUuid(), e.getMessage()));
                return false;
            }
        }
        return storageMgr.storagePoolHasEnoughIops(requestVolumes, pool) && storageMgr.storagePoolHasEnoughSpace(requestVolumes, pool, plan.getClusterId());
    }

    /*
    Check StoragePool and Volume type compatibility for the hypervisor
     */
    private boolean checkHypervisorCompatibility(HypervisorType hyperType, Volume.Type volType, Storage.StoragePoolType poolType){
        if(HypervisorType.LXC.equals(hyperType)){
            if(Volume.Type.ROOT.equals(volType)){
                //LXC ROOT disks supports NFS and local storage pools only
                if(!(Storage.StoragePoolType.NetworkFilesystem.equals(poolType) ||
                        Storage.StoragePoolType.Filesystem.equals(poolType)) ){
                    s_logger.debug("StoragePool does not support LXC ROOT disk, skipping this pool");
                    return false;
                }
            } else if (Volume.Type.DATADISK.equals(volType)){
                //LXC DATA disks supports RBD storage pool only
                if(!Storage.StoragePoolType.RBD.equals(poolType)){
                    s_logger.debug("StoragePool does not support LXC DATA disk, skipping this pool");
                    return false;
                }
            }
        }
        return true;
    }
}

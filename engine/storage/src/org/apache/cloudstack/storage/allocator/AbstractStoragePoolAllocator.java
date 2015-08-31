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
import java.util.Random;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import com.cloud.storage.Storage;
import org.apache.log4j.Logger;

import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.StoragePoolAllocator;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;

import com.cloud.capacity.Capacity;
import com.cloud.capacity.dao.CapacityDao;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.deploy.DeploymentPlanner.ExcludeList;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.storage.StorageManager;
import com.cloud.storage.StoragePool;
import com.cloud.storage.Volume;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.user.Account;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.component.AdapterBase;
import com.cloud.vm.DiskProfile;
import com.cloud.vm.VirtualMachineProfile;

public abstract class AbstractStoragePoolAllocator extends AdapterBase implements StoragePoolAllocator {
    private static final Logger s_logger = Logger.getLogger(AbstractStoragePoolAllocator.class);
    @Inject
    StorageManager storageMgr;
    protected @Inject
    PrimaryDataStoreDao _storagePoolDao;
    @Inject
    VolumeDao _volumeDao;
    @Inject
    ConfigurationDao _configDao;
    @Inject
    ClusterDao _clusterDao;
    protected @Inject
    DataStoreManager dataStoreMgr;
    protected BigDecimal _storageOverprovisioningFactor = new BigDecimal(1);
    long _extraBytesPerVolume = 0;
    Random _rand;
    boolean _dontMatter;
    protected String _allocationAlgorithm = "random";
    @Inject
    DiskOfferingDao _diskOfferingDao;
    @Inject
    CapacityDao _capacityDao;

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);
        if(_configDao != null) {
            Map<String, String> configs = _configDao.getConfiguration(null, params);
            String globalStorageOverprovisioningFactor = configs.get("storage.overprovisioning.factor");
            _storageOverprovisioningFactor = new BigDecimal(NumbersUtil.parseFloat(globalStorageOverprovisioningFactor, 2.0f));
            _extraBytesPerVolume = 0;
            _rand = new Random(System.currentTimeMillis());
            _dontMatter = Boolean.parseBoolean(configs.get("storage.overwrite.provisioning"));
            String allocationAlgorithm = configs.get("vm.allocation.algorithm");
            if (allocationAlgorithm != null) {
                _allocationAlgorithm = allocationAlgorithm;
            }
            return true;
        }
        return false;
    }

    protected abstract List<StoragePool> select(DiskProfile dskCh, VirtualMachineProfile vmProfile, DeploymentPlan plan, ExcludeList avoid, int returnUpTo);

    @Override
    public List<StoragePool> allocateToPool(DiskProfile dskCh, VirtualMachineProfile vmProfile, DeploymentPlan plan, ExcludeList avoid, int returnUpTo) {
        List<StoragePool> pools = select(dskCh, vmProfile, plan, avoid, returnUpTo);
        return reOrder(pools, vmProfile, plan);
    }

    protected List<StoragePool> reorderPoolsByCapacity(DeploymentPlan plan,
        List<StoragePool> pools) {
        Long clusterId = plan.getClusterId();
        short capacityType;
        if(pools != null && pools.size() != 0){
            capacityType = pools.get(0).getPoolType().isShared() == true ?
                    Capacity.CAPACITY_TYPE_STORAGE_ALLOCATED : Capacity.CAPACITY_TYPE_LOCAL_STORAGE;
        } else{
            return null;
        }

        List<Long> poolIdsByCapacity = _capacityDao.orderHostsByFreeCapacity(clusterId, capacityType);
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("List of pools in descending order of free capacity: "+ poolIdsByCapacity);
        }

      //now filter the given list of Pools by this ordered list
      Map<Long, StoragePool> poolMap = new HashMap<Long, StoragePool>();
      for (StoragePool pool : pools) {
          poolMap.put(pool.getId(), pool);
      }
      List<Long> matchingPoolIds = new ArrayList<Long>(poolMap.keySet());

      poolIdsByCapacity.retainAll(matchingPoolIds);

      List<StoragePool> reorderedPools = new ArrayList<StoragePool>();
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

        List<Long> poolIdsByVolCount = _volumeDao.listPoolIdsByVolumeCount(dcId, podId, clusterId, account.getAccountId());
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("List of pools in ascending order of number of volumes for account id: " + account.getAccountId() + " is: " + poolIdsByVolCount);
        }

        // now filter the given list of Pools by this ordered list
        Map<Long, StoragePool> poolMap = new HashMap<Long, StoragePool>();
        for (StoragePool pool : pools) {
            poolMap.put(pool.getId(), pool);
        }
        List<Long> matchingPoolIds = new ArrayList<Long>(poolMap.keySet());

        poolIdsByVolCount.retainAll(matchingPoolIds);

        List<StoragePool> reorderedPools = new ArrayList<StoragePool>();
        for (Long id : poolIdsByVolCount) {
            reorderedPools.add(poolMap.get(id));
        }

        return reorderedPools;
    }

    protected List<StoragePool> reOrder(List<StoragePool> pools, VirtualMachineProfile vmProfile, DeploymentPlan plan) {
        if (pools == null) {
            return null;
        }
        Account account = null;
        if (vmProfile.getVirtualMachine() != null) {
            account = vmProfile.getOwner();
        }

        if (_allocationAlgorithm.equals("random") || _allocationAlgorithm.equals("userconcentratedpod_random") || (account == null)) {
            // Shuffle this so that we don't check the pools in the same order.
            Collections.shuffle(pools);
        } else if (_allocationAlgorithm.equals("userdispersing")) {
            pools = reorderPoolsByNumberOfVolumes(plan, pools, account);
        } else if(_allocationAlgorithm.equals("firstfitleastconsumed")){
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
            ClusterVO cluster = _clusterDao.findById(clusterId);
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

        // check capacity
        Volume volume = _volumeDao.findById(dskCh.getVolumeId());
        List<Volume> requestVolumes = new ArrayList<Volume>();
        requestVolumes.add(volume);
        return storageMgr.storagePoolHasEnoughIops(requestVolumes, pool) && storageMgr.storagePoolHasEnoughSpace(requestVolumes, pool);
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

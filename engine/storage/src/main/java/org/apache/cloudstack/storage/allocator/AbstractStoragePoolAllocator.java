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
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import com.cloud.exception.StorageUnavailableException;
import com.cloud.storage.ScopeType;
import com.cloud.storage.StoragePoolStatus;
import org.apache.cloudstack.storage.datastore.db.StoragePoolDetailVO;
import org.apache.cloudstack.storage.datastore.db.StoragePoolDetailsDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.commons.lang3.StringUtils;
import org.apache.cloudstack.utils.reflectiontostringbuilderutils.ReflectionToStringBuilderUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.log4j.Logger;

import com.cloud.utils.Pair;
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
import com.cloud.storage.Storage;
import com.cloud.storage.StorageManager;
import com.cloud.storage.StoragePool;
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
    @Inject private StoragePoolDetailsDao storagePoolDetailsDao;

    /**
     * make sure shuffled lists of Pools are really shuffled
     */
    private SecureRandom secureRandom = new SecureRandom();

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
        return reorderPools(pools, vmProfile, plan, dskCh);
    }

    protected List<StoragePool> reorderPoolsByCapacity(DeploymentPlan plan, List<StoragePool> pools) {
        Long zoneId = plan.getDataCenterId();
        Long clusterId = plan.getClusterId();
        short capacityType;

        if (CollectionUtils.isEmpty(pools)) {
            return null;
        }

        if (pools.get(0).getPoolType().isShared()) {
            capacityType = Capacity.CAPACITY_TYPE_STORAGE_ALLOCATED;
        } else {
            capacityType = Capacity.CAPACITY_TYPE_LOCAL_STORAGE;
        }

        List<Long> poolIdsByCapacity = capacityDao.orderHostsByFreeCapacity(zoneId, clusterId, capacityType);

        s_logger.debug(String.format("List of pools in descending order of available capacity [%s].", poolIdsByCapacity));


      //now filter the given list of Pools by this ordered list
        Map<Long, StoragePool> poolMap = new HashMap<>();
        for (StoragePool pool : pools) {
            poolMap.put(pool.getId(), pool);
        }
        List<Long> matchingPoolIds = new ArrayList<>(poolMap.keySet());

        poolIdsByCapacity.retainAll(matchingPoolIds);

        List<StoragePool> reorderedPools = new ArrayList<>();
        for (Long id: poolIdsByCapacity) {
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
        s_logger.debug(String.format("List of pools in ascending order of number of volumes for account [%s] is [%s].", account, poolIdsByVolCount));

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
    public List<StoragePool> reorderPools(List<StoragePool> pools, VirtualMachineProfile vmProfile, DeploymentPlan plan, DiskProfile dskCh) {
        if (s_logger.isTraceEnabled()) {
            s_logger.trace("reordering pools");
        }
        if (pools == null) {
            s_logger.trace("There are no pools to reorder; returning null.");
            return null;
        }
        if (s_logger.isTraceEnabled()) {
            s_logger.trace(String.format("reordering %d pools", pools.size()));
        }
        Account account = null;
        if (vmProfile.getVirtualMachine() != null) {
            account = vmProfile.getOwner();
        }

        pools = reorderStoragePoolsBasedOnAlgorithm(pools, plan, account);

        if (vmProfile.getVirtualMachine() == null) {
            if (s_logger.isTraceEnabled()) {
                s_logger.trace("The VM is null, skipping pools reordering by disk provisioning type.");
            }
            return pools;
        }

        if (vmProfile.getHypervisorType() == HypervisorType.VMware &&
                !storageMgr.DiskProvisioningStrictness.valueIn(plan.getDataCenterId())) {
            pools = reorderPoolsByDiskProvisioningType(pools, dskCh);
        }

        return pools;
    }

    List<StoragePool> reorderStoragePoolsBasedOnAlgorithm(List<StoragePool> pools, DeploymentPlan plan, Account account) {
        if (allocationAlgorithm.equals("random") || allocationAlgorithm.equals("userconcentratedpod_random") || (account == null)) {
            reorderRandomPools(pools);
        } else if (StringUtils.equalsAny(allocationAlgorithm, "userdispersing", "firstfitleastconsumed")) {
            if (s_logger.isTraceEnabled()) {
                s_logger.trace(String.format("Using reordering algorithm [%s]", allocationAlgorithm));
            }

            if (allocationAlgorithm.equals("userdispersing")) {
                pools = reorderPoolsByNumberOfVolumes(plan, pools, account);
            } else {
                pools = reorderPoolsByCapacity(plan, pools);
            }
        }
        return pools;
    }

    void reorderRandomPools(List<StoragePool> pools) {
        StorageUtil.traceLogStoragePools(pools, s_logger, "pools to choose from: ");
        if (s_logger.isTraceEnabled()) {
            s_logger.trace(String.format("Shuffle this so that we don't check the pools in the same order. Algorithm == '%s' (or no account?)", allocationAlgorithm));
        }
        StorageUtil.traceLogStoragePools(pools, s_logger, "pools to shuffle: ");
        Collections.shuffle(pools, secureRandom);
        StorageUtil.traceLogStoragePools(pools, s_logger, "shuffled list of pools to choose from: ");
    }

    private List<StoragePool> reorderPoolsByDiskProvisioningType(List<StoragePool> pools, DiskProfile diskProfile) {
        if (diskProfile != null && diskProfile.getProvisioningType() != null && !diskProfile.getProvisioningType().equals(Storage.ProvisioningType.THIN)) {
            List<StoragePool> reorderedPools = new ArrayList<>();
            int preferredIndex = 0;
            for (StoragePool pool : pools) {
                StoragePoolDetailVO hardwareAcceleration = storagePoolDetailsDao.findDetail(pool.getId(), Storage.Capability.HARDWARE_ACCELERATION.toString());
                if (pool.getPoolType() == Storage.StoragePoolType.NetworkFilesystem &&
                        (hardwareAcceleration == null || !hardwareAcceleration.getValue().equals("true"))) {
                    // add to the bottom of the list
                    reorderedPools.add(pool);
                } else {
                    // add to the top of the list
                    reorderedPools.add(preferredIndex++, pool);
                }
            }
            return reorderedPools;
        } else {
            return pools;
        }
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

        if (dskCh.requiresEncryption() && !pool.getPoolType().supportsEncryption()) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug(String.format("Storage pool type '%s' doesn't support encryption required for volume, skipping this pool", pool.getPoolType()));
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

        if (!checkDiskProvisioningSupport(dskCh, pool)) {
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
        List<Pair<Volume, DiskProfile>> requestVolumeDiskProfilePairs = new ArrayList<>();
        requestVolumeDiskProfilePairs.add(new Pair<>(volume, dskCh));
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
                boolean isStoragePoolStoragepolicyComplaince = storageMgr.isStoragePoolCompliantWithStoragePolicy(requestVolumeDiskProfilePairs, pool);
                if (!isStoragePoolStoragepolicyComplaince) {
                    return false;
                }
            } catch (StorageUnavailableException e) {
                s_logger.warn(String.format("Could not verify storage policy complaince against storage pool %s due to exception %s", pool.getUuid(), e.getMessage()));
                return false;
            }
        }
        return storageMgr.storagePoolHasEnoughIops(requestVolumeDiskProfilePairs, pool) && storageMgr.storagePoolHasEnoughSpace(requestVolumeDiskProfilePairs, pool, plan.getClusterId());
    }

    private boolean checkDiskProvisioningSupport(DiskProfile dskCh, StoragePool pool) {
        if (dskCh.getHypervisorType() != null && dskCh.getHypervisorType() == HypervisorType.VMware && pool.getPoolType() == Storage.StoragePoolType.NetworkFilesystem &&
                storageMgr.DiskProvisioningStrictness.valueIn(pool.getDataCenterId())) {
            StoragePoolDetailVO hardwareAcceleration = storagePoolDetailsDao.findDetail(pool.getId(), Storage.Capability.HARDWARE_ACCELERATION.toString());
            if (dskCh.getProvisioningType() == null || !dskCh.getProvisioningType().equals(Storage.ProvisioningType.THIN) &&
                    (hardwareAcceleration == null || hardwareAcceleration.getValue() == null || !hardwareAcceleration.getValue().equals("true"))) {
                return false;
            }
        }
        return true;
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

    protected void logDisabledStoragePools(long dcId, Long podId, Long clusterId, ScopeType scope) {
        List<StoragePoolVO> disabledPools = storagePoolDao.findDisabledPoolsByScope(dcId, podId, clusterId, scope);
        if (disabledPools != null && !disabledPools.isEmpty()) {
            s_logger.trace(String.format("Ignoring pools [%s] as they are in disabled state.", ReflectionToStringBuilderUtils.reflectOnlySelectedFields(disabledPools)));
        }
    }

    protected void logStartOfSearch(DiskProfile dskCh, VirtualMachineProfile vmProfile, DeploymentPlan plan, int returnUpTo,
            boolean bypassStorageTypeCheck){
        s_logger.trace(String.format("%s is looking for storage pools that match the VM's disk profile [%s], virtual machine profile [%s] and "
                + "deployment plan [%s]. Returning up to [%d] and bypassStorageTypeCheck [%s].", this.getClass().getSimpleName(), dskCh, vmProfile, plan, returnUpTo, bypassStorageTypeCheck));
    }

}

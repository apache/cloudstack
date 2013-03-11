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

import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.StoragePoolAllocator;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.log4j.Logger;

import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.deploy.DeploymentPlanner.ExcludeList;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.storage.StorageManager;
import com.cloud.storage.StoragePool;
import com.cloud.storage.Volume;
import com.cloud.storage.Volume.Type;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.user.Account;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.component.AdapterBase;
import com.cloud.vm.DiskProfile;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;

public abstract class AbstractStoragePoolAllocator extends AdapterBase implements StoragePoolAllocator {
	private static final Logger s_logger = Logger.getLogger(AbstractStoragePoolAllocator.class);
    @Inject StorageManager storageMgr;
    protected @Inject PrimaryDataStoreDao _storagePoolDao;
    @Inject VolumeDao _volumeDao;
    @Inject ConfigurationDao _configDao;
    @Inject ClusterDao _clusterDao;
    protected @Inject DataStoreManager dataStoreMgr;
    protected BigDecimal _storageOverprovisioningFactor = new BigDecimal(1);    
    long _extraBytesPerVolume = 0;
    Random _rand;
    boolean _dontMatter;
    protected String _allocationAlgorithm = "random";
    @Inject
    DiskOfferingDao _diskOfferingDao;
    
    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);
        
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
	
	protected abstract List<StoragePool> select(DiskProfile dskCh, VirtualMachineProfile<? extends VirtualMachine> vmProfile, DeploymentPlan plan, ExcludeList avoid, int returnUpTo);
    
    @Override
	public
    List<StoragePool> allocateToPool(DiskProfile dskCh, VirtualMachineProfile<? extends VirtualMachine> vmProfile, DeploymentPlan plan, ExcludeList avoid, int returnUpTo) {
    	List<StoragePool> pools = select(dskCh, vmProfile, plan, avoid, returnUpTo);
    	return reOrder(pools, vmProfile, plan);
    }
    
    protected List<StoragePool> reorderPoolsByNumberOfVolumes(DeploymentPlan plan, List<StoragePool> pools, Account account) {
        if(account == null){
            return pools;
        }
        long dcId = plan.getDataCenterId();
        Long podId = plan.getPodId();
        Long clusterId = plan.getClusterId();
            
        List<Long> poolIdsByVolCount = _volumeDao.listPoolIdsByVolumeCount(dcId, podId, clusterId, account.getAccountId());
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("List of pools in ascending order of number of volumes for account id: "+ account.getAccountId() + " is: "+ poolIdsByVolCount);
        }
            
        //now filter the given list of Pools by this ordered list
        Map<Long, StoragePool> poolMap = new HashMap<Long, StoragePool>();        
        for (StoragePool pool : pools) {
            poolMap.put(pool.getId(), pool);
        }
        List<Long> matchingPoolIds = new ArrayList<Long>(poolMap.keySet());
        
        poolIdsByVolCount.retainAll(matchingPoolIds);
        
        List<StoragePool> reorderedPools = new ArrayList<StoragePool>();
        for(Long id: poolIdsByVolCount){
            reorderedPools.add(poolMap.get(id));
        }
        
        return reorderedPools;
    }
    
    protected List<StoragePool> reOrder(List<StoragePool> pools, 
    		VirtualMachineProfile<? extends VirtualMachine> vmProfile,
    		DeploymentPlan plan) {
    	Account account = null;
    	if(vmProfile.getVirtualMachine() != null){
    		account = vmProfile.getOwner();
    	}
    	
    	if(_allocationAlgorithm.equals("random") || _allocationAlgorithm.equals("userconcentratedpod_random") || (account == null)) {
    		// Shuffle this so that we don't check the pools in the same order.
    		Collections.shuffle(pools);
    	}else if(_allocationAlgorithm.equals("userdispersing")){
    		pools = reorderPoolsByNumberOfVolumes(plan, pools, account);
    	}
    	return pools;
    }
	
	protected boolean filter(ExcludeList avoid, StoragePool pool, DiskProfile dskCh, 
		 DeploymentPlan plan) {
		
		if (s_logger.isDebugEnabled()) {
            s_logger.debug("Checking if storage pool is suitable, name: " + pool.getName()+ " ,poolId: "+ pool.getId());
        }
		if (avoid.shouldAvoid(pool)) {
			if (s_logger.isDebugEnabled()) {
                s_logger.debug("StoragePool is in avoid set, skipping this pool");
            }			
			return false;
		}
		
        if(dskCh.getType().equals(Type.ROOT) && pool.getPoolType().equals(StoragePoolType.Iscsi)){
    		if (s_logger.isDebugEnabled()) {
                s_logger.debug("Disk needed for ROOT volume, but StoragePoolType is Iscsi, skipping this and trying other available pools");
            }	
            return false;
        }
        
        DiskOfferingVO diskOffering = _diskOfferingDao.findById(dskCh.getDiskOfferingId());
        if (diskOffering.getSystemUse() && pool.getPoolType() == StoragePoolType.RBD) {
            s_logger.debug("Skipping RBD pool " + pool.getName() + " as a suitable pool. RBD is not supported for System VM's");
            return false;
        }

        
		Long clusterId = pool.getClusterId();
		ClusterVO cluster = _clusterDao.findById(clusterId);
		if (!(cluster.getHypervisorType() == dskCh.getHypersorType())) {
    		if (s_logger.isDebugEnabled()) {
                s_logger.debug("StoragePool's Cluster does not have required hypervisorType, skipping this pool");
            }
			return false;
		}

        // check capacity  
        Volume volume =  _volumeDao.findById(dskCh.getVolumeId());
        List<Volume> requestVolumes = new ArrayList<Volume>();
        requestVolumes.add(volume);
        return storageMgr.storagePoolHasEnoughSpace(requestVolumes, pool);
	}
}

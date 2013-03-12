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
package com.cloud.storage.allocator;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.log4j.Logger;

import com.cloud.capacity.CapacityManager;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.deploy.DataCenterDeployment;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.deploy.DeploymentPlanner.ExcludeList;
import com.cloud.host.Host;
import com.cloud.server.StatsCollector;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.storage.StorageManager;
import com.cloud.storage.StoragePool;
import com.cloud.storage.StoragePoolStatus;
import com.cloud.storage.VMTemplateStoragePoolVO;
import com.cloud.storage.VMTemplateStorageResourceAssoc;
import com.cloud.storage.VMTemplateStorageResourceAssoc.Status;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.Volume;
import com.cloud.storage.Volume.Type;
import com.cloud.storage.dao.StoragePoolDao;
import com.cloud.storage.dao.StoragePoolHostDao;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VMTemplateHostDao;
import com.cloud.storage.dao.VMTemplatePoolDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.storage.swift.SwiftManager;
import com.cloud.template.TemplateManager;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.component.AdapterBase;
import com.cloud.vm.DiskProfile;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;

public abstract class AbstractStoragePoolAllocator extends AdapterBase implements StoragePoolAllocator {
	private static final Logger s_logger = Logger.getLogger(AbstractStoragePoolAllocator.class);
    @Inject TemplateManager _tmpltMgr;
    @Inject StorageManager _storageMgr;
    @Inject StoragePoolDao _storagePoolDao;
    @Inject VMTemplateHostDao _templateHostDao;
    @Inject VMTemplatePoolDao _templatePoolDao;
    @Inject VMTemplateDao _templateDao;
    @Inject VolumeDao _volumeDao;
    @Inject StoragePoolHostDao _poolHostDao;
    @Inject ConfigurationDao _configDao;
    @Inject ClusterDao _clusterDao;
    @Inject SwiftManager _swiftMgr;
    @Inject CapacityManager _capacityMgr;
    @Inject DataStoreManager dataStoreMgr;
    protected BigDecimal _storageOverprovisioningFactor = new BigDecimal(1);    
    long _extraBytesPerVolume = 0;
    Random _rand;
    boolean _dontMatter;
    
    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);
        
        Map<String, String> configs = _configDao.getConfiguration(null, params);
        
        String globalStorageOverprovisioningFactor = configs.get("storage.overprovisioning.factor");
        _storageOverprovisioningFactor = new BigDecimal(NumbersUtil.parseFloat(globalStorageOverprovisioningFactor, 2.0f));
        
        _extraBytesPerVolume = 0;
        
        _rand = new Random(System.currentTimeMillis());
        
        _dontMatter = Boolean.parseBoolean(configs.get("storage.overwrite.provisioning"));
        
        return true;
    }
    
    abstract boolean allocatorIsCorrectType(DiskProfile dskCh);
    
	protected boolean templateAvailable(long templateId, long poolId) {
    	VMTemplateStorageResourceAssoc thvo = _templatePoolDao.findByPoolTemplate(poolId, templateId);
    	if (thvo != null) {
    		if (s_logger.isDebugEnabled()) {
    			s_logger.debug("Template id : " + templateId + " status : " + thvo.getDownloadState().toString());
    		}
    		return (thvo.getDownloadState()==Status.DOWNLOADED);
    	} else {
    		return false;
    	}
    }
	
	protected boolean localStorageAllocationNeeded(DiskProfile dskCh) {
	    return dskCh.useLocalStorage();
	}
	
	protected boolean poolIsCorrectType(DiskProfile dskCh, StoragePool pool) {
		boolean localStorageAllocationNeeded = localStorageAllocationNeeded(dskCh);
		if (s_logger.isDebugEnabled()) {
            s_logger.debug("Is localStorageAllocationNeeded? "+ localStorageAllocationNeeded);
            s_logger.debug("Is storage pool shared? "+ pool.isShared());
        }
		
		return ((!localStorageAllocationNeeded && pool.getPoolType().isShared()) || (localStorageAllocationNeeded && !pool.getPoolType().isShared()));
	}
	
	protected boolean checkPool(ExcludeList avoid, StoragePoolVO pool, DiskProfile dskCh, VMTemplateVO template, List<VMTemplateStoragePoolVO> templatesInPool, 
			StatsCollector sc, DeploymentPlan plan) {
		
		if (s_logger.isDebugEnabled()) {
            s_logger.debug("Checking if storage pool is suitable, name: " + pool.getName()+ " ,poolId: "+ pool.getId());
        }
		StoragePool pol = (StoragePool)this.dataStoreMgr.getPrimaryDataStore(pool.getId());
		if (avoid.shouldAvoid(pol)) {
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
        
        //by default, all pools are up when successfully added
		//don't return the pool if not up (if in maintenance/prepareformaintenance/errorinmaintenance)
        if(!pool.getStatus().equals(StoragePoolStatus.Up)){
    		if (s_logger.isDebugEnabled()) {
                s_logger.debug("StoragePool status is not UP, status is: "+pool.getStatus().name()+", skipping this pool");
            }
        	return false;
        }
        
		// Check that the pool type is correct
		if (!poolIsCorrectType(dskCh, pol)) {
    		if (s_logger.isDebugEnabled()) {
                s_logger.debug("StoragePool is not of correct type, skipping this pool");
            }
			return false;
		}
		
		/*hypervisor type is correct*/
		// TODO : when creating a standalone volume, offering is passed as NULL, need to 
		// refine the logic of checking hypervisorType based on offering info
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
        return _storageMgr.storagePoolHasEnoughSpace(requestVolumes, pol);
	}


	
	@Override
	public String chooseStorageIp(VirtualMachine vm, Host host, Host storage) {
		return storage.getStorageIpAddress();
	}
	
	
	@Override
	public List<StoragePool> allocateToPool(DiskProfile dskCh, VirtualMachineProfile<? extends VirtualMachine> vmProfile, long dcId, long podId, Long clusterId, Long hostId, Set<? extends StoragePool> avoids, int returnUpTo) {
	    
	    ExcludeList avoid = new ExcludeList();
	    for(StoragePool pool : avoids){
	    	avoid.addPool(pool.getId());
	    }
	    
	    DataCenterDeployment plan = new DataCenterDeployment(dcId, podId, clusterId, hostId, null, null);
	    return allocateToPool(dskCh, vmProfile, plan, avoid, returnUpTo);
	}

}

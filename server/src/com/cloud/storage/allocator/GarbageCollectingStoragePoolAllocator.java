/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package com.cloud.storage.allocator;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.agent.api.to.DiskCharacteristicsTO;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.HostPodVO;
import com.cloud.service.ServiceOffering;
import com.cloud.storage.StorageManager;
import com.cloud.storage.StoragePool;
import com.cloud.storage.VMTemplateVO;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.vm.VMInstanceVO;

@Local(value=StoragePoolAllocator.class)
public class GarbageCollectingStoragePoolAllocator extends AbstractStoragePoolAllocator {
    private static final Logger s_logger = Logger.getLogger(LocalStoragePoolAllocator.class);
    
    StoragePoolAllocator _firstFitStoragePoolAllocator;
    StoragePoolAllocator _localStoragePoolAllocator;
    StorageManager _storageMgr;
    ConfigurationDao _configDao;
    boolean _storagePoolCleanupEnabled;
    
    @Override
    public boolean allocatorIsCorrectType(DiskCharacteristicsTO dskCh, VMInstanceVO vm, ServiceOffering offering) {
    	return true;
    }
    
    public Integer getStorageOverprovisioningFactor() {
    	return null;
    }
    
    public Long getExtraBytesPerVolume() {
    	return null;
    }
    
    @Override
    public StoragePool allocateToPool(DiskCharacteristicsTO dskCh,
                                      ServiceOffering offering,
                                      DataCenterVO dc,
                                      HostPodVO pod,
                                      Long clusterId,
                                      VMInstanceVO vm,
                                      VMTemplateVO template,
                                      Set<? extends StoragePool> avoid) {
    	
    	if (!_storagePoolCleanupEnabled) {
    		s_logger.debug("Storage pool cleanup is not enabled, so GarbageCollectingStoragePoolAllocator is being skipped.");
    		return null;
    	}
    	
    	// Clean up all storage pools
    	_storageMgr.cleanupStorage(false);
    	
    	// Determine what allocator to use
    	StoragePoolAllocator allocator;
    	if (localStorageAllocationNeeded(dskCh, vm, offering)) {
    		allocator = _localStoragePoolAllocator;
    	} else {
    		allocator = _firstFitStoragePoolAllocator;
    	}

    	// Try to find a storage pool after cleanup
        Set<StoragePool> myAvoids = new HashSet<StoragePool>();
        for (StoragePool pool : avoid) {
            myAvoids.add(pool);
        }
        
        return allocator.allocateToPool(dskCh, offering, dc, pod, clusterId, vm, template, myAvoids);
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);
        
        ComponentLocator locator = ComponentLocator.getCurrentLocator();
        
        _firstFitStoragePoolAllocator = ComponentLocator.inject(FirstFitStoragePoolAllocator.class);
        _firstFitStoragePoolAllocator.configure("GCFirstFitStoragePoolAllocator", params);
        _localStoragePoolAllocator = ComponentLocator.inject(LocalStoragePoolAllocator.class);
        _localStoragePoolAllocator.configure("GCLocalStoragePoolAllocator", params);
        
        _storageMgr = locator.getManager(StorageManager.class);
        if (_storageMgr == null) {
        	throw new ConfigurationException("Unable to get " + StorageManager.class.getName());
        }
        
        _configDao = locator.getDao(ConfigurationDao.class);
        if (_configDao == null) {
            throw new ConfigurationException("Unable to get the configuration dao.");
        }
        
        String storagePoolCleanupEnabled = _configDao.getValue("storage.pool.cleanup.enabled");
        _storagePoolCleanupEnabled = (storagePoolCleanupEnabled == null) ? true : Boolean.parseBoolean(storagePoolCleanupEnabled);
        
        return true;
    }
    
    public GarbageCollectingStoragePoolAllocator() {
    }
    
}

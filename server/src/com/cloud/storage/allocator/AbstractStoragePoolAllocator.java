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

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.deploy.DeployDestination;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.server.StatsCollector;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.storage.StorageManager;
import com.cloud.storage.StoragePool;
import com.cloud.storage.StoragePoolVO;
import com.cloud.storage.StorageStats;
import com.cloud.storage.VMTemplateHostVO;
import com.cloud.storage.VMTemplateStoragePoolVO;
import com.cloud.storage.VMTemplateStorageResourceAssoc;
import com.cloud.storage.VMTemplateStorageResourceAssoc.Status;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.Volume;
import com.cloud.storage.Volume.VolumeType;
import com.cloud.storage.dao.StoragePoolDao;
import com.cloud.storage.dao.StoragePoolHostDao;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VMTemplateHostDao;
import com.cloud.storage.dao.VMTemplatePoolDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.template.TemplateManager;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.component.Inject;
import com.cloud.vm.DiskProfile;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;

public abstract class AbstractStoragePoolAllocator extends AdapterBase implements StoragePoolAllocator {
	private static final Logger s_logger = Logger.getLogger(FirstFitStoragePoolAllocator.class);
    @Inject TemplateManager _tmpltMgr;
    @Inject StorageManager _storageMgr;
    @Inject StoragePoolDao _storagePoolDao;
    @Inject VMTemplateHostDao _templateHostDao;
    @Inject VMTemplatePoolDao _templatePoolDao;
    @Inject VMTemplateDao _templateDao;
    @Inject VolumeDao _volumeDao;
    @Inject StoragePoolHostDao _poolHostDao;
    @Inject ConfigurationDao _configDao;
    int _storageOverprovisioningFactor;
    long _extraBytesPerVolume = 0;
    Random _rand;
    boolean _dontMatter;
    double _storageUsedThreshold = 1.0d;
    
    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);
        
        Map<String, String> configs = _configDao.getConfiguration(null, params);
        
        String globalStorageOverprovisioningFactor = configs.get("storage.overprovisioning.factor");
        _storageOverprovisioningFactor = NumbersUtil.parseInt(globalStorageOverprovisioningFactor, 2);
        
        _extraBytesPerVolume = 0;
        
        String storageUsedThreshold = configs.get("storage.capacity.threshold");
        if (storageUsedThreshold != null) {
            _storageUsedThreshold = Double.parseDouble(storageUsedThreshold);
        }

        _rand = new Random(System.currentTimeMillis());
        
        _dontMatter = Boolean.parseBoolean(configs.get("storage.overwrite.provisioning"));
        
        return true;
    }
    
    abstract boolean allocatorIsCorrectType(DiskProfile dskCh, VMInstanceVO vm);
    
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
	
	protected boolean localStorageAllocationNeeded(DiskProfile dskCh, VMInstanceVO vm) {
	    return dskCh.useLocalStorage();
	}
	
	protected boolean poolIsCorrectType(DiskProfile dskCh, StoragePool pool, VMInstanceVO vm) {
		boolean localStorageAllocationNeeded = localStorageAllocationNeeded(dskCh, vm);
		return ((!localStorageAllocationNeeded && pool.getPoolType().isShared()) || (localStorageAllocationNeeded && !pool.getPoolType().isShared()));
	}
	
	protected boolean checkPool(Set<? extends StoragePool> avoid, StoragePoolVO pool, DiskProfile dskCh, VMTemplateVO template, List<VMTemplateStoragePoolVO> templatesInPool, 
			VMInstanceVO vm, StatsCollector sc) {
		if (avoid.contains(pool)) {
			return false;
		}
        if(dskCh.getType().equals(VolumeType.ROOT) && pool.getPoolType().equals(StoragePoolType.Iscsi)){
            return false;
        }
            
		
		// Check that the pool type is correct
		if (!poolIsCorrectType(dskCh, pool, vm)) {
			return false;
		}

		// check the used size against the total size, skip this host if it's greater than the configured
		// capacity check "storage.capacity.threshold"
		if (sc != null) {
			long totalSize = pool.getCapacityBytes();
			StorageStats stats = sc.getStorageStats(pool.getId());
			if (stats != null) {
				double usedPercentage = ((double)stats.getByteUsed() / (double)totalSize);
				if (s_logger.isDebugEnabled()) {
					s_logger.debug("Attempting to look for pool " + pool.getId() + " for storage, totalSize: " + pool.getCapacityBytes() + ", usedBytes: " + stats.getByteUsed() + ", usedPct: " + usedPercentage + ", threshold: " + _storageUsedThreshold);
				}
				if (usedPercentage >= _storageUsedThreshold) {
					return false;
				}
			}
		}

		Pair<Long, Long> sizes = _volumeDao.getCountAndTotalByPool(pool.getId());
		
		long totalAllocatedSize = sizes.second() + sizes.first() * _extraBytesPerVolume;

		// Iterate through all templates on this storage pool
		boolean tmpinstalled = false;
		List<VMTemplateStoragePoolVO> templatePoolVOs;
		if (templatesInPool != null) {
			templatePoolVOs = templatesInPool;
		} else {
			templatePoolVOs = _templatePoolDao.listByPoolId(pool.getId());
		}

		for (VMTemplateStoragePoolVO templatePoolVO : templatePoolVOs) {
			VMTemplateVO templateInPool = _templateDao.findById(templatePoolVO.getTemplateId());
			int templateSizeMultiplier = pool.getPoolType() == StoragePoolType.NetworkFilesystem ? 1 : 2;

			if ((template != null) && !tmpinstalled && (templateInPool.getId() == template.getId())) {
				tmpinstalled = true;
			}
			
			s_logger.debug("For template: " + templateInPool.getName() + ", using template size multiplier: " + templateSizeMultiplier);

			long templateSize = templatePoolVO.getTemplateSize();
			totalAllocatedSize += templateSizeMultiplier * (templateSize + _extraBytesPerVolume);
		}

		if ((template != null) && !tmpinstalled) {
			// If the template that was passed into this allocator is not installed in the storage pool,
			// add 3 * (template size on secondary storage) to the running total
			HostVO secondaryStorageHost = _storageMgr.getSecondaryStorageHost(pool.getDataCenterId());
			if (secondaryStorageHost == null) {
				return false;
			} else {
				VMTemplateHostVO templateHostVO = _templateHostDao.findByHostTemplate(secondaryStorageHost.getId(), template.getId());
				if (templateHostVO == null) {
					return false;
				} else {
					s_logger.debug("For template: " + template.getName() + ", using template size multiplier: " + 2);
					long templateSize = templateHostVO.getSize();
					totalAllocatedSize += 2 * (templateSize + _extraBytesPerVolume);
				}
			}
		}

		long askingSize = dskCh.getSize();
		
		int storageOverprovisioningFactor = 1;
		if (pool.getPoolType() == StoragePoolType.NetworkFilesystem) {
			storageOverprovisioningFactor = _storageOverprovisioningFactor;
		}

		if (s_logger.isDebugEnabled()) {
			s_logger.debug("Attempting to look for pool " + pool.getId() + " for storage, maxSize : " + (pool.getCapacityBytes() * storageOverprovisioningFactor) + ", totalSize : " + totalAllocatedSize + ", askingSize : " + askingSize);
		}

		if ((pool.getCapacityBytes() * storageOverprovisioningFactor) < (totalAllocatedSize + askingSize)) {
			if (s_logger.isDebugEnabled()) {
				s_logger.debug("Found pool " + pool.getId() + " for storage, maxSize : " + (pool.getCapacityBytes() * storageOverprovisioningFactor) + ", totalSize : " + totalAllocatedSize + ", askingSize : " + askingSize);
			}

			return false;
		}

		return true;
	}
	
	@Override
	public String chooseStorageIp(VirtualMachine vm, Host host, Host storage) {
		return storage.getStorageIpAddress();
	}
	
	@Override
	public StoragePool allocateTo(DiskProfile dskCh, VirtualMachineProfile vm, DeployDestination dest, List<Volume> disks, Set<StoragePool> avoids) {
	    
	    return null;
	}
}

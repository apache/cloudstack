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

import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.ejb.Local;

import org.apache.log4j.Logger;

import com.cloud.agent.api.to.DiskCharacteristicsTO;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.HostPodVO;
import com.cloud.server.StatsCollector;
import com.cloud.service.ServiceOffering;
import com.cloud.storage.StoragePool;
import com.cloud.storage.StoragePoolVO;
import com.cloud.storage.VMTemplateVO;
import com.cloud.vm.VMInstanceVO;

@Local(value=StoragePoolAllocator.class)
public class FirstFitStoragePoolAllocator extends AbstractStoragePoolAllocator {
    private static final Logger s_logger = Logger.getLogger(FirstFitStoragePoolAllocator.class);

    @Override
    public boolean allocatorIsCorrectType(DiskCharacteristicsTO dskCh, VMInstanceVO vm, ServiceOffering offering) {
    	return !localStorageAllocationNeeded(dskCh, vm, offering);
    }

    @Override
	public StoragePool allocateToPool(DiskCharacteristicsTO dskCh, ServiceOffering offering, DataCenterVO dc, HostPodVO pod, Long clusterId,
									  VMInstanceVO vm, VMTemplateVO template, Set<? extends StoragePool> avoid) {
		// Check that the allocator type is correct
        if (!allocatorIsCorrectType(dskCh, vm, offering)) {
        	return null;
        }

		List<StoragePoolVO> pools = _storagePoolDao.findPoolsByTags(dc.getId(), pod.getId(), clusterId, dskCh.getTags(), null);
        if (pools.size() == 0) {
    		if (s_logger.isDebugEnabled()) {
    			s_logger.debug("No storage pools available for pod id : " + pod.getId());
    		}
            return null;
        }
        
        StatsCollector sc = StatsCollector.getInstance();

        Collections.shuffle(pools);
        
        for (StoragePoolVO pool: pools) {
        	if (checkPool(avoid, pool, dskCh, template, null, offering, vm, sc)) {
        		return pool;
        	}
        }
        
        if (s_logger.isDebugEnabled()) {
			s_logger.debug("Unable to find any storage pool");
		}
        
        if (_dontMatter && pools.size() > 0) {
        	return pools.get(0);
        } else {
        	return null;
        }
	}
}

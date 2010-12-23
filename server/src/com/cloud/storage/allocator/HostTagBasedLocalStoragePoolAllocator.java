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

import java.util.ArrayList;
import java.util.List;
import javax.ejb.Local;
import org.apache.log4j.Logger;
import com.cloud.agent.api.to.DiskCharacteristicsTO;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.HostPodVO;
import com.cloud.service.ServiceOffering;
import com.cloud.storage.StoragePoolVO;


@Local(value=StoragePoolAllocator.class)
public class HostTagBasedLocalStoragePoolAllocator extends LocalStoragePoolAllocator {
    private static final Logger s_logger = Logger.getLogger(HostTagBasedLocalStoragePoolAllocator.class);

    public HostTagBasedLocalStoragePoolAllocator() {
    }
    
    @Override
    protected List<StoragePoolVO> findPools(DiskCharacteristicsTO dskCh, ServiceOffering offering, DataCenterVO dc, HostPodVO pod, Long clusterId){
    	List<StoragePoolVO> pools = new ArrayList<StoragePoolVO>();
    	if(offering != null && offering.getHostTag() != null){
    		String hostTag = offering.getHostTag();
    		pools = _storagePoolDao.findPoolsByHostTag(dc.getId(), pod.getId(), clusterId, hostTag);
    		if(pools.size() == 0){
    	    	if (s_logger.isInfoEnabled()) {
    	    		s_logger.info("No storage pools having associated Host with host tag: '"+ hostTag +"' available for pod id : " + pod.getId());
    	    	}
    		}else{
    	    	if (s_logger.isDebugEnabled()) {
    	    		s_logger.debug("Found "+pools.size() +" storage pools having associated Host with host tag: '"+ hostTag +"' available for pod id : " + pod.getId());
    	    	}
    		}
    	}else{
    		pools = super.findPools(dskCh, offering, dc, pod, clusterId);
    	}
    	return pools;
    }
}

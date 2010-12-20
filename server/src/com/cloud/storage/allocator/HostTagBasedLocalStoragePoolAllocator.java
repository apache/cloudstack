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

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.agent.manager.allocator.HostAllocator;
import com.cloud.agent.manager.allocator.impl.FirstFitAllocator;
import com.cloud.agent.api.to.DiskCharacteristicsTO;
import com.cloud.capacity.CapacityVO;
import com.cloud.capacity.dao.CapacityDao;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.HostPodVO;
import com.cloud.host.Host;
import com.cloud.service.ServiceOffering;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.ServiceOffering.GuestIpType;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.StoragePool;
import com.cloud.storage.StoragePoolHostVO;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.StoragePoolHostDao;
import com.cloud.utils.DateUtil;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.component.Inject;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Func;
import com.cloud.vm.State;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VmCharacteristics;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.VMInstanceDao;
import com.cloud.storage.StoragePoolVO;

//
// TODO
// Rush to make LocalStoragePoolAllocator use static allocation status, we should revisit the overall
// allocation process to make it more reliable in next release. The code put in here is pretty ugly
//
@Local(value=StoragePoolAllocator.class)
public class HostTagBasedLocalStoragePoolAllocator extends LocalStoragePoolAllocator {
    private static final Logger s_logger = Logger.getLogger(HostTagBasedLocalStoragePoolAllocator.class);

    public HostTagBasedLocalStoragePoolAllocator() {
    }
    
    @Override
    protected List<StoragePoolVO> findPools(DiskCharacteristicsTO dskCh, ServiceOffering offering, DataCenterVO dc, HostPodVO pod, Long clusterId){
    	List<StoragePoolVO> pools = _storagePoolDao.findPoolsByHostTags(dc.getId(), pod.getId(), clusterId, dskCh.getTags(), null);
    	return pools;
    }
}

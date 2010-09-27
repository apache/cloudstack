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
public class LocalStoragePoolAllocator extends FirstFitStoragePoolAllocator {
    private static final Logger s_logger = Logger.getLogger(LocalStoragePoolAllocator.class);

    @Inject StoragePoolHostDao _poolHostDao;
    @Inject VMInstanceDao _vmInstanceDao;
    @Inject UserVmDao _vmDao;
    @Inject ServiceOfferingDao _offeringDao;
    @Inject CapacityDao _capacityDao;
    @Inject ConfigurationDao _configDao;

    HostAllocator _allocator = null;    
    protected SearchBuilder<VMInstanceVO> VmsOnPoolSearch;

    
    private int _hoursToSkipStoppedVMs = 24;
    private int _secStorageVmRamSize = 1024;
    private int _proxyRamSize =  256;
    private int _routerRamSize = 128;
    
    
    @Override
    public boolean allocatorIsCorrectType(DiskCharacteristicsTO dskCh, VMInstanceVO vm, ServiceOffering offering) {
    	return localStorageAllocationNeeded(dskCh, vm, offering);
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
    	
    	// Check that the allocator type is correct
        if (!allocatorIsCorrectType(dskCh, vm, offering)) {
        	return null;
        }

        Set<StoragePool> myAvoids = new HashSet<StoragePool>(avoid);
        VmCharacteristics vmc = new VmCharacteristics(vm.getType());
        StoragePool pool = null;
        while ((pool = super.allocateToPool(dskCh, offering, dc, pod, clusterId, vm, template, myAvoids)) != null) {
            myAvoids.add(pool);
            if (pool.getPoolType().isShared()) {
                return pool;
            }
            
            if (_allocator.allocateTo(vmc, offering, Host.Type.Routing, dc, pod, (StoragePoolVO)pool, template, new HashSet<Host>()) == null) {
                continue;
            }
            
        	List<StoragePoolHostVO> hostsInSPool = _poolHostDao.listByPoolId(pool.getId());
        	assert(hostsInSPool.size() == 1) : "Local storage pool should be one host per pool";
        	
        	StoragePoolHostVO spHost = hostsInSPool.get(0);
        	
        	SearchCriteria sc = VmsOnPoolSearch.create();
        	sc.setJoinParameters("volumeJoin", "poolId", pool.getId());
        	sc.setParameters("state", State.Expunging);
        	List<Object[]> results = _vmInstanceDao.searchAll(sc, null);
            
            List<Long> vmsOnHost = new ArrayList<Long>();
            for(Object[] row : results) {
                vmsOnHost.add(new Long(((BigInteger)row[0]).longValue()));
            }
        	
        	if(s_logger.isDebugEnabled()) {
        		s_logger.debug("Found " + vmsOnHost.size() + " VM instances are alloacated at host " + spHost.getHostId() + " with local storage pool " + pool.getName());
        		for(Long vmId : vmsOnHost)
        			s_logger.debug("VM " + vmId + " is allocated on host " + spHost.getHostId() + " with local storage pool " + pool.getName());
        	}
        	
        	if(hostHasCpuMemoryCapacity(spHost.getHostId(), vmsOnHost, vm))
        		return pool;
        	
            s_logger.debug("Found pool " + pool.getId() + " but host doesn't fit.");
        }
        
        s_logger.debug("Unable to find storage pool to fit the vm");
        return null;
    }
    
    private boolean hostHasCpuMemoryCapacity(long hostId, List<Long> vmOnHost, VMInstanceVO vm) {
    	
        ServiceOffering so = null;
    	if(vm.getType() == VirtualMachine.Type.User) {
    		UserVmVO userVm = _vmDao.findById(vm.getId());
    		if (userVm != null) 
    			so = _offeringDao.findById(userVm.getServiceOfferingId());
    	} else if(vm.getType() == VirtualMachine.Type.ConsoleProxy) {
    		so = new ServiceOfferingVO("Fake Offering For DomP", 1,
				_proxyRamSize, 0, 0, 0, false, null, GuestIpType.Virtualized, false, true, null);
    	} else if(vm.getType() == VirtualMachine.Type.SecondaryStorageVm) {
    		so = new ServiceOfferingVO("Fake Offering For Secondary Storage VM", 1, _secStorageVmRamSize, 0, 0, 0, false, null, GuestIpType.Virtualized, false, true, null);
    	} else if(vm.getType() == VirtualMachine.Type.DomainRouter) {
            so = new ServiceOfferingVO("Fake Offering For DomR", 1, _routerRamSize, 0, 0, 0, false, null, GuestIpType.Virtualized, false, true, null);
    	} else {
    		assert(false) : "Unsupported system vm type";
            so = new ServiceOfferingVO("Fake Offering For unknow system VM", 1, 128, 0, 0, 0, false, null, GuestIpType.Virtualized, false, true, null);
    	}
    	
    	long usedMemory = calcHostAllocatedCpuMemoryCapacity(vmOnHost, CapacityVO.CAPACITY_TYPE_MEMORY);
    	if(s_logger.isDebugEnabled())
    		s_logger.debug("Calculated static-allocated memory for VMs on host " + hostId + ": " + usedMemory + " bytes, requesting memory: "
    			+ (so != null ? so.getRamSize()*1024L*1024L : "") + " bytes");
    	
    	SearchCriteria sc = _capacityDao.createSearchCriteria();
    	sc.addAnd("hostOrPoolId", SearchCriteria.Op.EQ, hostId);
    	sc.addAnd("capacityType", SearchCriteria.Op.EQ, CapacityVO.CAPACITY_TYPE_MEMORY);
    	List<CapacityVO> capacities = _capacityDao.search(sc, null);
    	if(capacities.size() > 0) {
    		if(capacities.get(0).getTotalCapacity() < usedMemory + (so != null ? so.getRamSize()* 1024L * 1024L : 0)) {
    			if(s_logger.isDebugEnabled())
    				s_logger.debug("Host " + hostId + " runs out of memory capacity");
    			return false;
    		}
    	} else {
    		s_logger.warn("Host " + hostId + " has not reported memory capacity yet");
    		return false;
    	}
    	
    	long usedCpu = calcHostAllocatedCpuMemoryCapacity(vmOnHost, CapacityVO.CAPACITY_TYPE_CPU);
    	if(s_logger.isDebugEnabled())
    		s_logger.debug("Calculated static-allocated CPU for VMs on host " + hostId + ": " + usedCpu + " GHz, requesting cpu: "
    			+ (so != null ? so.getCpu()*so.getSpeed() : "") + " GHz");
    	
    	sc = _capacityDao.createSearchCriteria();
    	sc.addAnd("hostOrPoolId", SearchCriteria.Op.EQ, hostId);
    	sc.addAnd("capacityType", SearchCriteria.Op.EQ, CapacityVO.CAPACITY_TYPE_CPU);
    	capacities = _capacityDao.search(sc, null);
    	if(capacities.size() > 0) {
    		if(capacities.get(0).getTotalCapacity() < usedCpu + (so != null ? so.getCpu() * so.getSpeed() : 0)) {
    			if(s_logger.isDebugEnabled())
    				s_logger.debug("Host " + hostId + " runs out of CPU capacity");
    			return false;
    		}
    	} else {
    		s_logger.warn("Host " + hostId + " has not reported CPU capacity yet");
    		return false;
    	}
    	
    	return true;
    }
    
    private boolean skipCalculation(VMInstanceVO vm) {
    	if(vm == null)
    		return true;
    	
    	if(vm.getState() == State.Expunging) {
    		if(s_logger.isDebugEnabled())
    			s_logger.debug("Skip counting capacity for Expunging VM : " + vm.getInstanceName());
    		return true;
    	}
    	
    	if(vm.getState() == State.Destroyed && vm.getType() != VirtualMachine.Type.User)
    		return true;
    	
    	if(vm.getState() == State.Stopped || vm.getState() == State.Destroyed) {
    		// for stopped/Destroyed VMs, we will skip counting it if it hasn't been used for a while
    		
    		long millisecondsSinceLastUpdate = DateUtil.currentGMTTime().getTime() - vm.getUpdateTime().getTime();
    		if(millisecondsSinceLastUpdate > _hoursToSkipStoppedVMs*3600000L) {
    			if(s_logger.isDebugEnabled())
    				s_logger.debug("Skip counting vm " + vm.getInstanceName() + " in capacity allocation as it has been stopped for " + millisecondsSinceLastUpdate/60000 + " minutes");
    			return true;
    		}
    	}
    	return false;
    }
    
    private long calcHostAllocatedCpuMemoryCapacity(List<Long> vmOnHost, short capacityType) {
        assert(capacityType == CapacityVO.CAPACITY_TYPE_MEMORY || capacityType == CapacityVO.CAPACITY_TYPE_CPU) : "Invalid capacity type passed in calcHostAllocatedCpuCapacity()";
    	
        long usedCapacity = 0;
        for (Long vmId : vmOnHost) {
        	VMInstanceVO vm = _vmInstanceDao.findById(vmId);
        	if(skipCalculation(vm))
        		continue;
        	
            ServiceOffering so = null;
        	if(vm.getType() == VirtualMachine.Type.User) {
        		UserVmVO userVm = _vmDao.findById(vm.getId());
        		if (userVm == null) {
        		    continue;
        		}
        		so = _offeringDao.findById(userVm.getServiceOfferingId());
        	} else if(vm.getType() == VirtualMachine.Type.ConsoleProxy) {
        		so = new ServiceOfferingVO("Fake Offering For DomP", 1,
    				_proxyRamSize, 0, 0, 0, false, null, GuestIpType.Virtualized, false, true, null);
        	} else if(vm.getType() == VirtualMachine.Type.SecondaryStorageVm) {
        		so = new ServiceOfferingVO("Fake Offering For Secondary Storage VM", 1, _secStorageVmRamSize, 0, 0, 0, false, null, GuestIpType.Virtualized, false, true, null);
        	} else if(vm.getType() == VirtualMachine.Type.DomainRouter) {
                so = new ServiceOfferingVO("Fake Offering For DomR", 1, _routerRamSize, 0, 0, 0, false, null, GuestIpType.Virtualized, false, true, null);
        	} else {
        		assert(false) : "Unsupported system vm type";
                so = new ServiceOfferingVO("Fake Offering For unknow system VM", 1, 128, 0, 0, 0, false, null, GuestIpType.Virtualized, false, true, null);
        	}
            
            if(capacityType == CapacityVO.CAPACITY_TYPE_MEMORY) {
            	usedCapacity += so.getRamSize() * 1024L * 1024L;
            } else if(capacityType == CapacityVO.CAPACITY_TYPE_CPU) {
            	usedCapacity += so.getCpu() * so.getSpeed();
            }
        }
        
    	return usedCapacity;
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);
        
        _storageOverprovisioningFactor = 1;
        _extraBytesPerVolume = NumbersUtil.parseLong((String) params.get("extra.bytes.per.volume"), 50 * 1024L * 1024L);

		Map<String, String> configs = _configDao.getConfiguration("management-server", params);
		String value = configs.get("capacity.skipcounting.hours");
		_hoursToSkipStoppedVMs = NumbersUtil.parseInt(value, 24);

		// TODO this is not good, there should be one place to get these values
		_secStorageVmRamSize = NumbersUtil.parseInt(configs.get("secstorage.vm.ram.size"), 256);
        _routerRamSize = NumbersUtil.parseInt(configs.get("router.ram.size"), 128);
		_proxyRamSize = NumbersUtil.parseInt(configs.get("consoleproxy.ram.size"), 1024);
		
		VmsOnPoolSearch = _vmInstanceDao.createSearchBuilder();
        VmsOnPoolSearch.select(Func.DISTINCT, VmsOnPoolSearch.entity().getId());
        VmsOnPoolSearch.and("removed", VmsOnPoolSearch.entity().getRemoved(), SearchCriteria.Op.NULL);
        VmsOnPoolSearch.and("state", VmsOnPoolSearch.entity().getState(), SearchCriteria.Op.NIN);
        
        SearchBuilder<VolumeVO> sbVolume = _volumeDao.createSearchBuilder();
        sbVolume.and("poolId", sbVolume.entity().getPoolId(), SearchCriteria.Op.EQ);
        
        VmsOnPoolSearch.join("volumeJoin", sbVolume, VmsOnPoolSearch.entity().getId(), sbVolume.entity().getInstanceId());
        
        sbVolume.done();
        VmsOnPoolSearch.done();
        
        _allocator = new FirstFitAllocator();
        _allocator.configure("First fit", params);
        
        return true;
    }
    
    public LocalStoragePoolAllocator() {
    }
}

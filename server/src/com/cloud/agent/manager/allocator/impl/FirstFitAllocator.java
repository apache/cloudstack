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
package com.cloud.agent.manager.allocator.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.agent.manager.allocator.HostAllocator;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.HostPodVO;
import com.cloud.host.DetailVO;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.Host.Type;
import com.cloud.host.dao.DetailsDao;
import com.cloud.host.dao.HostDao;
import com.cloud.service.ServiceOffering;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.GuestOSCategoryVO;
import com.cloud.storage.GuestOSVO;
import com.cloud.storage.StoragePoolHostVO;
import com.cloud.storage.StoragePoolVO;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.dao.GuestOSCategoryDao;
import com.cloud.storage.dao.GuestOSDao;
import com.cloud.storage.dao.StoragePoolHostDao;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.vm.ConsoleProxyVO;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.SecondaryStorageVmVO;
import com.cloud.vm.UserVm;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VmCharacteristics;
import com.cloud.vm.dao.ConsoleProxyDao;
import com.cloud.vm.dao.DomainRouterDao;
import com.cloud.vm.dao.SecondaryStorageVmDao;
import com.cloud.vm.dao.UserVmDao;

/**
 * An allocator that tries to find a fit on a computing host.  This allocator does not care whether or not the host supports routing.
 */
@Local(value={HostAllocator.class})
public class FirstFitAllocator implements HostAllocator {
    private static final Logger s_logger = Logger.getLogger(FirstFitAllocator.class);
    private String _name;
    protected HostDao _hostDao;
    protected DetailsDao _hostDetailsDao;
    protected UserVmDao _vmDao;
    protected ServiceOfferingDao _offeringDao;
    protected DomainRouterDao _routerDao;
    protected ConsoleProxyDao _consoleProxyDao;
    protected SecondaryStorageVmDao _secStorgaeVmDao;
    protected StoragePoolHostDao _storagePoolHostDao;
    protected ConfigurationDao _configDao;
    protected GuestOSDao _guestOSDao;
    protected GuestOSCategoryDao _guestOSCategoryDao;
    float _factor = 1;
    protected String _allocationAlgorithm = "random";
    
	@Override
	public Host allocateTo(VmCharacteristics vm, ServiceOffering offering, Type type, DataCenterVO dc,
			HostPodVO pod, StoragePoolVO sp, VMTemplateVO template,
			Set<Host> avoid) {

        if (type == Host.Type.Storage) {
            // FirstFitAllocator should be used for user VMs only since it won't care whether the host is capable of routing or not
            return null;
        }
        
        s_logger.debug("Looking for hosts associated with storage pool " + sp.getId());

        List<StoragePoolHostVO> poolhosts = _storagePoolHostDao.listByPoolId(sp.getId());
        List<HostVO> hosts = new ArrayList<HostVO>();
        for( StoragePoolHostVO poolhost : poolhosts ){
            HostVO h = _hostDao.findById(poolhost.getHostId());
            if( h != null && h.getType().equals(Type.Routing) && h.getStatus().equals(Status.Up)) {
                 hosts.add(h);
            }
        }
        
        long podId = pod.getId();
        List<HostVO> podHosts = new ArrayList<HostVO>(hosts.size());
        Iterator<HostVO> it = hosts.iterator();
        while (it.hasNext()) {
        	HostVO host = it.next();
        	if (host.getPodId() == podId && !avoid.contains(host)) {
        	    if (s_logger.isDebugEnabled()) {
        	        s_logger.debug("Adding host " + host + " as possible pod host");
        	    }
        		podHosts.add(host);
        	}
        }
        
        return allocateTo(offering, template, avoid, podHosts);
    }

    protected Host allocateTo(ServiceOffering offering, VMTemplateVO template, Set<Host> avoid, List<HostVO> hosts) {
        if (_allocationAlgorithm.equals("random")) {
        	// Shuffle this so that we don't check the hosts in the same order.
            Collections.shuffle(hosts);
        }
    	
    	if (s_logger.isDebugEnabled()) {
        	StringBuffer sb = new StringBuffer();
            for(Host h : avoid) {
            	sb.append(h.getName()).append(" ");
            }
            s_logger.debug("Found " + hosts.size() + " hosts for allocation and a avoid set of [" + sb + "]");
        }
        
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Looking for speed=" + (offering.getCpu() * offering.getSpeed()) + "Mhz, Ram=" + offering.getRamSize());
        }

        // We will try to reorder the host lists such that we give priority to hosts that have
        // the minimums to support a VM's requirements
        hosts = prioritizeHosts(template, hosts);

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Found " + hosts.size() + " hosts for allocation after prioritization");
        }

        for (HostVO host : hosts) {
            if (avoid.contains(host)) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("host " + host.getName() + " is in avoid set, skip and try other available hosts");
                }
                continue;
            }

            long usedMemory = 0;
            double totalSpeed = 0d;

            List<DomainRouterVO> domainRouters = _routerDao.listUpByHostId(host.getId());
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Found " + domainRouters.size() + " router domains on host " + host.getId());
            }
            for (DomainRouterVO router : domainRouters) {
                usedMemory += router.getRamSize() * 1024L * 1024L;
            }

            List<ConsoleProxyVO> proxys = _consoleProxyDao.listUpByHostId(host.getId());
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Found " + proxys.size() + " console proxy on host " + host.getId());
            }
            for(ConsoleProxyVO proxy : proxys) {
                usedMemory += proxy.getRamSize() * 1024L * 1024L;
            }
            
            List<SecondaryStorageVmVO> secStorageVms = _secStorgaeVmDao.listUpByHostId(host.getId());
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Found " + secStorageVms.size() + " secondary storage VM on host " + host.getId());
            }
            for(SecondaryStorageVmVO secStorageVm : secStorageVms) {
                usedMemory += secStorageVm.getRamSize() * 1024L * 1024L;
            }
            		
            List<UserVmVO> vms = _vmDao.listUpByHostId(host.getId());
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Found " + vms.size() + " user VM on host " + host.getId());
            }
            
            for (UserVmVO vm : vms) {
                ServiceOffering so = _offeringDao.findById(vm.getServiceOfferingId());
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("vm " + vm.getId() + ": speed=" + (so.getCpu() * so.getSpeed()) + "Mhz, RAM=" + so.getRamSize() + "MB");
                }
                usedMemory += so.getRamSize() * 1024L * 1024L;
                totalSpeed += so.getCpu() * (so.getSpeed() * 0.99);
            }

            if (s_logger.isDebugEnabled()) {
                long availableSpeed = (long)(host.getCpus() * host.getSpeed() * _factor);
                double desiredSpeed = offering.getCpu() * (offering.getSpeed() * 0.99);
                long coreSpeed = host.getSpeed();
                s_logger.debug("Host " + host.getId() + ": available speed=" + availableSpeed + "Mhz, core speed=" + coreSpeed + "Mhz, used speed=" + totalSpeed + "Mhz, desired speed=" + desiredSpeed +
                        "Mhz, desired cores: " + offering.getCpu() + ", available cores: " + host.getCpus() + ", RAM=" + host.getTotalMemory() +
                        ", avail RAM=" + (host.getTotalMemory() - usedMemory) + ", desired RAM=" + (offering.getRamSize() * 1024L * 1024L));
            }

            boolean numCpusGood = host.getCpus().intValue() >= offering.getCpu();
            boolean coreSpeedGood = host.getSpeed().doubleValue() >= (offering.getSpeed() * 0.99);
            boolean totalSpeedGood = ((host.getCpus().doubleValue() * host.getSpeed().doubleValue() * _factor) - totalSpeed) >= (offering.getCpu() * (offering.getSpeed() * 0.99));
            boolean memoryGood = (host.getTotalMemory() - usedMemory) >= (offering.getRamSize() * 1024L * 1024L);
            if (numCpusGood && totalSpeedGood && coreSpeedGood && memoryGood) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("found host " + host.getId());
                }
                return host;
            } else {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("not using host " + host.getId() + "; numCpusGood: " + numCpusGood + ", coreSpeedGood: " + coreSpeedGood + ", totalSpeedGood: " + totalSpeedGood + ", memoryGood: " + memoryGood);
                }
            }
        }
        return null;
    }

    @Override
    public boolean isVirtualMachineUpgradable(UserVm vm, ServiceOffering offering) {
        // currently we do no special checks to rule out a VM being upgradable to an offering, so
        // return true
        return true;
    }

    protected List<HostVO> prioritizeHosts(VMTemplateVO template, List<HostVO> hosts) {
    	if (template == null) {
    		return hosts;
    	}
    	
    	// Determine the guest OS category of the template
    	String templateGuestOSCategory = getTemplateGuestOSCategory(template);
    	
    	List<HostVO> prioritizedHosts = new ArrayList<HostVO>();
    	
    	// If a template requires HVM and a host doesn't support HVM, remove it from consideration
    	List<HostVO> hostsToCheck = new ArrayList<HostVO>();
    	if (template.isRequiresHvm()) {
    		for (HostVO host : hosts) {
    			if (hostSupportsHVM(host)) {
    				hostsToCheck.add(host);
    			}
    		}
    	} else {
    		hostsToCheck.addAll(hosts);
    	}
    	
    	// If a host is tagged with the same guest OS category as the template, move it to a high priority list
    	// If a host is tagged with a different guest OS category than the template, move it to a low priority list
    	List<HostVO> highPriorityHosts = new ArrayList<HostVO>();
    	List<HostVO> lowPriorityHosts = new ArrayList<HostVO>();
    	for (HostVO host : hostsToCheck) {
    		String hostGuestOSCategory = getHostGuestOSCategory(host);
    		if (hostGuestOSCategory == null) {
    			continue;
    		} else if (templateGuestOSCategory.equals(hostGuestOSCategory)) {
    			highPriorityHosts.add(host);
    		} else {
    			lowPriorityHosts.add(host);
    		}
    	}
    	
    	hostsToCheck.removeAll(highPriorityHosts);
    	hostsToCheck.removeAll(lowPriorityHosts);
    	
    	// Prioritize the remaining hosts by HVM capability
    	for (HostVO host : hostsToCheck) {
    		if (!template.isRequiresHvm() && !hostSupportsHVM(host)) {
    			// Host and template both do not support hvm, put it as first consideration
    			prioritizedHosts.add(0, host);
    		} else {
    			// Template doesn't require hvm, but the machine supports it, make it last for consideration
    			prioritizedHosts.add(host);
    		}
    	}
    	
    	// Merge the lists
    	prioritizedHosts.addAll(0, highPriorityHosts);
    	prioritizedHosts.addAll(lowPriorityHosts);
    	
    	return prioritizedHosts;
    }
    
    protected boolean hostSupportsHVM(HostVO host) {
    	// Determine host capabilities
		String caps = host.getCapabilities();
		
		if (caps != null) {
            String[] tokens = caps.split(",");
            for (String token : tokens) {
            	if (token.contains("hvm")) {
            	    return true;
            	}
            }
		}
		
		return false;
    }
    
    protected String getHostGuestOSCategory(HostVO host) {
		DetailVO hostDetail = _hostDetailsDao.findDetail(host.getId(), "guest.os.category.id");
		if (hostDetail != null) {
			String guestOSCategoryIdString = hostDetail.getValue();
			long guestOSCategoryId;
			
			try {
				guestOSCategoryId = Long.parseLong(guestOSCategoryIdString);
			} catch (Exception e) {
				return null;
			}
			
			GuestOSCategoryVO guestOSCategory = _guestOSCategoryDao.findById(guestOSCategoryId);
			
			if (guestOSCategory != null) {
				return guestOSCategory.getName();
			} else {
				return null;
			}
		} else {
			return null;
		}
    }
    
    protected String getTemplateGuestOSCategory(VMTemplateVO template) {
    	long guestOSId = template.getGuestOSId();
    	GuestOSVO guestOS = _guestOSDao.findById(guestOSId);
    	long guestOSCategoryId = guestOS.getCategoryId();
    	GuestOSCategoryVO guestOSCategory = _guestOSCategoryDao.findById(guestOSCategoryId);
    	return guestOSCategory.getName();
    }
    
    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        _name = name;
        ComponentLocator locator = ComponentLocator.getCurrentLocator();
        _hostDao = locator.getDao(HostDao.class);
        _hostDetailsDao = locator.getDao(DetailsDao.class);
        _vmDao = locator.getDao(UserVmDao.class);
        _offeringDao = locator.getDao(ServiceOfferingDao.class);
        _routerDao = locator.getDao(DomainRouterDao.class);
        _consoleProxyDao = locator.getDao(ConsoleProxyDao.class);
        _secStorgaeVmDao = locator.getDao(SecondaryStorageVmDao.class);
        _storagePoolHostDao  = locator.getDao(StoragePoolHostDao.class);
        _configDao = locator.getDao(ConfigurationDao.class);
        _guestOSDao = locator.getDao(GuestOSDao.class);
        _guestOSCategoryDao = locator.getDao(GuestOSCategoryDao.class);
    	if (_configDao != null) {
    		Map<String, String> configs = _configDao.getConfiguration(params);
            String opFactor = configs.get("cpu.overprovisioning.factor");
            _factor = NumbersUtil.parseFloat(opFactor, 1);
            //Over provisioning factor cannot be < 1. Reset to 1 in such cases
            if (_factor < 1){
            	_factor = 1;
            }
            
            String allocationAlgorithm = configs.get("vm.allocation.algorithm");
            if (allocationAlgorithm != null && (allocationAlgorithm.equals("random") || allocationAlgorithm.equals("firstfit"))) {
            	_allocationAlgorithm = allocationAlgorithm;
            }
        }
        return true;
    }

    @Override
    public String getName() {
        return _name;
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }


}

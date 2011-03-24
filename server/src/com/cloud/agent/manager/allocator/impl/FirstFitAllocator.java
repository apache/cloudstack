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
import com.cloud.deploy.DeploymentPlan;
import com.cloud.deploy.DeploymentPlanner.ExcludeList;
import com.cloud.host.DetailVO;
import com.cloud.host.Host;
import com.cloud.host.Host.Type;
import com.cloud.host.HostVO;
import com.cloud.host.dao.DetailsDao;
import com.cloud.host.dao.HostDao;
import com.cloud.offering.ServiceOffering;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.GuestOSCategoryVO;
import com.cloud.storage.GuestOSVO;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.dao.GuestOSCategoryDao;
import com.cloud.storage.dao.GuestOSDao;
import com.cloud.uservm.UserVm;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.utils.component.Inject;
import com.cloud.vm.ConsoleProxyVO;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.SecondaryStorageVmVO;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.dao.ConsoleProxyDao;
import com.cloud.vm.dao.DomainRouterDao;
import com.cloud.vm.dao.SecondaryStorageVmDao;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.capacity.CapacityManager;

/**
 * An allocator that tries to find a fit on a computing host.  This allocator does not care whether or not the host supports routing.
 */
@Local(value={HostAllocator.class})
public class FirstFitAllocator implements HostAllocator {
    private static final Logger s_logger = Logger.getLogger(FirstFitAllocator.class);
    private String _name;
    @Inject HostDao _hostDao = null;
    @Inject DetailsDao _hostDetailsDao = null;
    @Inject UserVmDao _vmDao = null;
    @Inject ServiceOfferingDao _offeringDao = null;
    @Inject DomainRouterDao _routerDao = null;
    @Inject ConsoleProxyDao _consoleProxyDao = null;
    @Inject SecondaryStorageVmDao _secStorgaeVmDao = null;
    @Inject ConfigurationDao _configDao = null;
    @Inject GuestOSDao _guestOSDao = null; 
    @Inject GuestOSCategoryDao _guestOSCategoryDao = null;
    float _factor = 1;
    protected String _allocationAlgorithm = "random";
    @Inject CapacityManager _capacityMgr;
    
	@Override
	public List<Host> allocateTo(VirtualMachineProfile<? extends VirtualMachine> vmProfile, DeploymentPlan plan, Type type,
			ExcludeList avoid, int returnUpTo) {

		long dcId = plan.getDataCenterId();
		Long podId = plan.getPodId();
		Long clusterId = plan.getClusterId();
		ServiceOffering offering = vmProfile.getServiceOffering();
		VMTemplateVO template = (VMTemplateVO)vmProfile.getTemplate();

        if (type == Host.Type.Storage) {
            // FirstFitAllocator should be used for user VMs only since it won't care whether the host is capable of routing or not
        	return new ArrayList<Host>();
        }
        
        String hostTag = offering.getHostTag();
        if(hostTag != null){
        	s_logger.debug("Looking for hosts in dc: " + dcId + "  pod:" + podId + "  cluster:" + clusterId + " having host tag:" + hostTag);
        }else{
        	s_logger.debug("Looking for hosts in dc: " + dcId + "  pod:" + podId + "  cluster:" + clusterId);
        }

        List<HostVO> clusterHosts = new ArrayList<HostVO>();
        if(hostTag != null){
        	clusterHosts = _hostDao.listByHostTag(type, clusterId, podId, dcId, hostTag);
        }else{
        	clusterHosts = _hostDao.listBy(type, clusterId, podId, dcId);
        }

        return allocateTo(offering, template, avoid, clusterHosts, returnUpTo);
    }

    protected List<Host> allocateTo(ServiceOffering offering, VMTemplateVO template, ExcludeList avoid, List<HostVO> hosts, int returnUpTo) {
        if (_allocationAlgorithm.equals("random")) {
        	// Shuffle this so that we don't check the hosts in the same order.
            Collections.shuffle(hosts);
        }
    	
    	if (s_logger.isDebugEnabled()) {
            s_logger.debug("FirstFitAllocator has " + hosts.size() + " hosts to check for allocation: "+hosts);
        }
        
        // We will try to reorder the host lists such that we give priority to hosts that have
        // the minimums to support a VM's requirements
        hosts = prioritizeHosts(template, hosts);

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Found " + hosts.size() + " hosts for allocation after prioritization: "+ hosts);
        }

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Looking for speed=" + (offering.getCpu() * offering.getSpeed()) + "Mhz, Ram=" + offering.getRamSize());
        }
        
        List<Host> suitableHosts = new ArrayList<Host>();

        for (HostVO host : hosts) {
        	if(suitableHosts.size() == returnUpTo){
        		break;
        	}
            if (avoid.shouldAvoid(host)) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Host name: " + host.getName() + ", hostId: "+ host.getId() +" is in avoid set, skipping this and trying other available hosts");
                }
                continue;
            }
            
            if(host.getHostAllocationState() != Host.HostAllocationState.Enabled){
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Host name: " + host.getName() + ", hostId: "+ host.getId() +" is in " + host.getHostAllocationState().name() + " state, skipping this and trying other available hosts");
                }
                continue;
            }

            boolean numCpusGood = host.getCpus().intValue() >= offering.getCpu();
    		int cpu_requested = offering.getCpu() * offering.getSpeed();
    		long ram_requested = offering.getRamSize() * 1024L * 1024L;	
    		boolean hostHasCapacity = _capacityMgr.checkIfHostHasCapacity(host.getId(), cpu_requested, ram_requested, false);

            if (numCpusGood && hostHasCapacity) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Found a suitable host, adding to list: " + host.getId());
                }
                suitableHosts.add(host);
            } else {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Not using host " + host.getId() + "; numCpusGood: " + numCpusGood + ", host has capacity?" + hostHasCapacity);
                }
            }
        }
        
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Host Allocator returning "+suitableHosts.size() +" suitable hosts");
        }
        
        return suitableHosts;
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

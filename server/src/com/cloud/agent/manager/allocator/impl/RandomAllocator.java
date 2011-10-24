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
import java.util.List;
import java.util.Map;

import javax.ejb.Local;

import org.apache.log4j.Logger;

import com.cloud.agent.manager.allocator.HostAllocator;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.deploy.DeploymentPlanner.ExcludeList;
import com.cloud.host.Host;
import com.cloud.host.Host.Type;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.offering.ServiceOffering;
import com.cloud.resource.ResourceManager;
import com.cloud.uservm.UserVm;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;

@Local(value=HostAllocator.class)
public class RandomAllocator implements HostAllocator {
    private static final Logger s_logger = Logger.getLogger(RandomAllocator.class);
    private String _name;
    private HostDao _hostDao;
    private ResourceManager _resourceMgr;

    @Override
    public List<Host> allocateTo(VirtualMachineProfile<? extends VirtualMachine> vmProfile, DeploymentPlan plan, Type type,
            ExcludeList avoid, int returnUpTo) {
        return allocateTo(vmProfile, plan, type, avoid, returnUpTo, true);
    }
    
    @Override
    public List<Host> allocateTo(VirtualMachineProfile<? extends VirtualMachine> vmProfile, DeploymentPlan plan, Type type,
			ExcludeList avoid, int returnUpTo, boolean considerReservedCapacity) {

		long dcId = plan.getDataCenterId();
		Long podId = plan.getPodId();
		Long clusterId = plan.getClusterId();
		ServiceOffering offering = vmProfile.getServiceOffering();
    	
    	List<Host> suitableHosts = new ArrayList<Host>();
    	
        if (type == Host.Type.Storage) {
            return suitableHosts;
        }

        String hostTag = offering.getHostTag();
        if(hostTag != null){
        	s_logger.debug("Looking for hosts in dc: " + dcId + "  pod:" + podId + "  cluster:" + clusterId + " having host tag:" + hostTag);
        }else{
        	s_logger.debug("Looking for hosts in dc: " + dcId + "  pod:" + podId + "  cluster:" + clusterId);
        }

        // list all computing hosts, regardless of whether they support routing...it's random after all
        List<? extends Host> hosts = new ArrayList<HostVO>();
        if(hostTag != null){
        	hosts = _hostDao.listByHostTag(type, clusterId, podId, dcId, hostTag);
        }else{
        	hosts = _resourceMgr.listAllUpAndEnabledHosts(type, clusterId, podId, dcId);
        }
        
        s_logger.debug("Random Allocator found " + hosts.size() + "  hosts");
        
        if (hosts.size() == 0) {
            return suitableHosts;
        }


        Collections.shuffle(hosts);
        for (Host host : hosts) {
        	if(suitableHosts.size() == returnUpTo){
        		break;
        	}
        	
            if (!avoid.shouldAvoid(host)) {
            	suitableHosts.add(host);
            }else{
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Host name: " + host.getName() + ", hostId: "+ host.getId() +" is in avoid set, skipping this and trying other available hosts");
                }
            }
        }
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Random Host Allocator returning "+suitableHosts.size() +" suitable hosts");
        }
        return suitableHosts;
    }

    @Override
    public boolean isVirtualMachineUpgradable(UserVm vm, ServiceOffering offering) {
        // currently we do no special checks to rule out a VM being upgradable to an offering, so
        // return true
        return true;
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) {
        ComponentLocator locator = ComponentLocator.getCurrentLocator();
        _hostDao = locator.getDao(HostDao.class);
        _resourceMgr = locator.getManager(ResourceManager.class);
        if (_hostDao == null) {
            s_logger.error("Unable to get host dao.");
            return false;
        }
        _name=name;
        
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

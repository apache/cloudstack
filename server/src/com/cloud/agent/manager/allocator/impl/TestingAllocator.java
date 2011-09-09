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
import java.util.List;
import java.util.Map;

import javax.ejb.Local;

import com.cloud.agent.manager.allocator.HostAllocator;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.deploy.DeploymentPlanner.ExcludeList;
import com.cloud.host.Host;
import com.cloud.host.Host.Type;
import com.cloud.host.dao.HostDao;
import com.cloud.offering.ServiceOffering;
import com.cloud.uservm.UserVm;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;

/**
 * @author ahuang
 *
 */
@Local(value={HostAllocator.class})
public class TestingAllocator implements HostAllocator {
    HostDao _hostDao;
    Long _computingHost;
    Long _storageHost;
    Long _routingHost;
    String _name;

    @Override
    public List<Host> allocateTo(VirtualMachineProfile<? extends VirtualMachine> vmProfile, DeploymentPlan plan, Type type,
            ExcludeList avoid, int returnUpTo) {
        return allocateTo(vmProfile, plan, type, avoid, returnUpTo, true);
    }
    
    @Override
    public List<Host> allocateTo(VirtualMachineProfile<? extends VirtualMachine> vmProfile, DeploymentPlan plan, Type type,
			ExcludeList avoid, int returnUpTo, boolean considerReservedCapacity) {
    	List<Host> availableHosts = new ArrayList<Host>();
    	Host host = null;    	
        if (type == Host.Type.Routing && _routingHost != null) {
        	host = _hostDao.findById(_routingHost);
        } else if (type == Host.Type.Storage && _storageHost != null) {
        	host = _hostDao.findById(_storageHost);
        }
        if(host != null){
        	availableHosts.add(host);
        }
        return availableHosts;
    }

    @Override
    public boolean isVirtualMachineUpgradable(UserVm vm, ServiceOffering offering) {
        // currently we do no special checks to rule out a VM being upgradable to an offering, so
        // return true
        return true;
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) {
        String value = (String)params.get(Host.Type.Routing.toString());
        _routingHost = (value != null) ? Long.parseLong(value) : null;

        value = (String)params.get(Host.Type.Storage.toString());
        _storageHost = (value != null) ? Long.parseLong(value) : null;
        
        ComponentLocator _locator = ComponentLocator.getCurrentLocator();
        _hostDao = _locator.getDao(HostDao.class);
        
        _name = name;
        
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

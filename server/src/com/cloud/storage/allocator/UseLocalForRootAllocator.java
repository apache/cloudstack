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

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import com.cloud.configuration.Config;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.deploy.DeploymentPlanner.ExcludeList;
import com.cloud.host.Host;
import com.cloud.storage.StoragePool;
import com.cloud.storage.Volume.Type;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.vm.DiskProfile;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;

@Local(value=StoragePoolAllocator.class)
public class UseLocalForRootAllocator extends LocalStoragePoolAllocator implements StoragePoolAllocator {
    boolean _useLocalStorage;

    @Override
    public List<StoragePool> allocateToPool(DiskProfile dskCh, VirtualMachineProfile<? extends VirtualMachine> vmProfile, DeploymentPlan plan, ExcludeList avoid, int returnUpTo) {
        if (!_useLocalStorage) {
            return null;
        }
        
        return super.allocateToPool(dskCh, vmProfile, plan, avoid, returnUpTo);
    }

    @Override
    public String chooseStorageIp(VirtualMachine vm, Host host, Host storage) {
        return null;
    }
    
    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);
        
        ComponentLocator locator = ComponentLocator.getCurrentLocator();
        ConfigurationDao configDao = locator.getDao(ConfigurationDao.class);
        Map<String, String> dbParams = configDao.getConfiguration(params);
        
        _useLocalStorage = Boolean.parseBoolean(dbParams.get(Config.UseLocalStorage.toString()));
        
        return true;
    }
    
    @Override
    protected boolean localStorageAllocationNeeded(DiskProfile dskCh) {
        if (dskCh.getType() == Type.ROOT) {
            return true;
        } else if (dskCh.getType() == Type.DATADISK) {
            return false;
        } else {
            return super.localStorageAllocationNeeded(dskCh);
        }
    }
    
    protected UseLocalForRootAllocator() {
    }
}

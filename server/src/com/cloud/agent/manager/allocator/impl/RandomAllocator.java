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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.Local;

import org.apache.log4j.Logger;

import com.cloud.agent.manager.allocator.HostAllocator;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.HostPodVO;
import com.cloud.host.Host;
import com.cloud.host.dao.HostDao;
import com.cloud.service.ServiceOffering;
import com.cloud.storage.StoragePoolVO;
import com.cloud.storage.VMTemplateVO;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.vm.UserVm;
import com.cloud.vm.VmCharacteristics;

@Local(value=HostAllocator.class)
public class RandomAllocator implements HostAllocator {
    private static final Logger s_logger = Logger.getLogger(RandomAllocator.class);
    private String _name;
    private HostDao _hostDao;

    @Override
    public Host allocateTo(VmCharacteristics vm, ServiceOffering offering, Host.Type type, DataCenterVO dc, HostPodVO pod,
    		StoragePoolVO sp, VMTemplateVO template, Set<Host> avoid) {
        if (type == Host.Type.Storage) {
            return null;
        }

        // list all computing hosts, regardless of whether they support routing...it's random after all
        List<? extends Host> hosts = _hostDao.listBy(type, sp.getClusterId(), sp.getPodId(), sp.getDataCenterId());
        if (hosts.size() == 0) {
            return null;
        }
        s_logger.debug("Random Allocator found " + hosts.size() + "  hosts");

        Collections.shuffle(hosts);
        for (Host host : hosts) {
            if (!avoid.contains(host)) {
                return host;
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

    @Override
    public boolean configure(String name, Map<String, Object> params) {
        ComponentLocator locator = ComponentLocator.getCurrentLocator();
        _hostDao = locator.getDao(HostDao.class);
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

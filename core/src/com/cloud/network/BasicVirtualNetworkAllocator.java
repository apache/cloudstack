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
package com.cloud.network;

import java.util.Map;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import com.cloud.dc.dao.DataCenterDao;
import com.cloud.host.HostVO;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.user.AccountVO;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.vm.VMInstanceVO;

@Local(value=VirtualNetworkAllocator.class)
public class BasicVirtualNetworkAllocator implements VirtualNetworkAllocator {
    DataCenterDao _dcDao;
    String _name;
    
    public BasicVirtualNetworkAllocator() {
    }

    @Override
    public String allocateTag(AccountVO account, HostVO host, VMInstanceVO vm, ServiceOfferingVO so) {
        return _dcDao.allocateVnet(host.getDataCenterId(), account.getId());
    }

    @Override
    public void releaseTag(String tag, HostVO host, AccountVO account, VMInstanceVO vm) {
        _dcDao.releaseVnet(tag, host.getDataCenterId(), account.getId());
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        ComponentLocator locator = ComponentLocator.getCurrentLocator();
        _dcDao = locator.getDao(DataCenterDao.class);
        if (_dcDao == null) {
            throw new ConfigurationException("Unable to get DataCenterDao");
        }
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

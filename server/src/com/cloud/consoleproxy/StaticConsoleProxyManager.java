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
package com.cloud.consoleproxy;

import java.util.List;
import java.util.Map;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.host.Host.Type;
import com.cloud.host.HostVO;
import com.cloud.info.ConsoleProxyInfo;
import com.cloud.resource.ResourceManager;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.utils.component.Inject;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.dao.ConsoleProxyDao;

@Local(value={ConsoleProxyManager.class})
public class StaticConsoleProxyManager extends AgentBasedConsoleProxyManager implements ConsoleProxyManager {
    String _ip = null;
    @Inject ConsoleProxyDao _proxyDao;
    @Inject ResourceManager _resourceMgr;
    
    @Override
    protected HostVO findHost(VMInstanceVO vm) {
        
        List<HostVO> hosts = _resourceMgr.listAllUpAndEnabledHostsInOneZoneByType(Type.ConsoleProxy, vm.getDataCenterIdToDeployIn());
        
        return hosts.isEmpty() ? null : hosts.get(0);
    }
    
    @Override
    public ConsoleProxyInfo assignProxy(long dataCenterId, long userVmId) {
        return new ConsoleProxyInfo(false, _ip, _consoleProxyPort, _consoleProxyUrlPort, _consoleProxyUrlDomain);
    }
    
    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);
        
        ComponentLocator locator = ComponentLocator.getCurrentLocator();
        
        ConfigurationDao configDao = locator.getDao(ConfigurationDao.class);
        Map<String, String> dbParams = configDao.getConfiguration("ManagementServer", params);
        
        _ip = dbParams.get("public.ip");
        if (_ip == null) {
            _ip = "127.0.0.1";
        }
        
        return true;
    }
}

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
import com.cloud.host.HostVO;
import com.cloud.host.Host.Type;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.utils.component.Inject;
import com.cloud.vm.ConsoleProxyVO;
import com.cloud.vm.State;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.dao.ConsoleProxyDao;

@Local(value={ConsoleProxyManager.class})
public class StaticConsoleProxyManager extends AgentBasedConsoleProxyManager implements ConsoleProxyManager {
    String _ip = null;
    @Inject ConsoleProxyDao _proxyDao;
    
    @Override
    protected HostVO findHost(VMInstanceVO vm) {
        
        List<HostVO> hosts = _hostDao.listBy(Type.ConsoleProxy, vm.getDataCenterId());
        
        return hosts.isEmpty() ? null : hosts.get(0);
    }
    
    @Override
    public ConsoleProxyVO assignProxy(long dataCenterId, long userVmId) {
        ConsoleProxyVO proxy =  new ConsoleProxyVO(1l, "EmbeddedProxy", State.Running, null, null, null,
                "02:02:02:02:02:02",
                "127.0.0.1",
                "255.255.255.0",
                1l,
                1l,
                "03:03:03:03:03:03",
                _ip,
                "255.255.255.0",
                null,
                "untagged",
                1l,
                dataCenterId,
                "0.0.0.0",
                0L,
                "dns1",
                "dn2",
                "domain",
                0,
                0);
        
		proxy.setPort(_consoleProxyUrlPort);
		proxy.setSslEnabled(false);
		return proxy;
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
    
    @Override
    public ConsoleProxyVO get(long id) {
        return _proxyDao.findById(id);
    }
}

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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.consoleproxy.ConsoleProxyAllocator;
import com.cloud.consoleproxy.ConsoleProxyManager;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.vm.ConsoleProxyVO;

@Local(value={ConsoleProxyAllocator.class})
public class ConsoleProxyBalanceAllocator implements ConsoleProxyAllocator {
	
    private String _name;
    private int _maxSessionCount = ConsoleProxyManager.DEFAULT_PROXY_CAPACITY;
    private final Random _rand = new Random(System.currentTimeMillis());
   
    @Override
	public ConsoleProxyVO allocProxy(List<ConsoleProxyVO> candidates, Map<Long, Integer> loadInfo, long dataCenterId) {
    	if(candidates != null) {
    		
    		List<ConsoleProxyVO> allocationList = new ArrayList<ConsoleProxyVO>();
    		
    		for(ConsoleProxyVO proxy : candidates) {
    			Integer load = loadInfo.get(proxy.getId());
    			if(load == null || load < _maxSessionCount) {
    				allocationList.add(proxy);
    			}
    		}
    		
    		if(allocationList.size() > 0)
    			return allocationList.get(_rand.nextInt(allocationList.size()));
    	}
    	return null;
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        _name = name;
        ComponentLocator locator = ComponentLocator.getCurrentLocator();
        ConfigurationDao configDao = locator.getDao(ConfigurationDao.class);
        if (configDao == null) {
            throw new ConfigurationException("Unable to get the configuration dao.");
        }
        
        Map<String, String> configs = configDao.getConfiguration();
        
        String value = configs.get("consoleproxy.session.max");
        _maxSessionCount = NumbersUtil.parseInt(value, ConsoleProxyManager.DEFAULT_PROXY_CAPACITY);
        
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

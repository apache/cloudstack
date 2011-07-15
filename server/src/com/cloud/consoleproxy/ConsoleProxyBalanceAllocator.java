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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.vm.ConsoleProxyVO;

import edu.emory.mathcs.backport.java.util.Collections;

@Local(value={ConsoleProxyAllocator.class})
public class ConsoleProxyBalanceAllocator implements ConsoleProxyAllocator {
	
    private String _name;
    private final Random _rand = new Random(System.currentTimeMillis());
   
    @Override
	public ConsoleProxyVO allocProxy(List<ConsoleProxyVO> candidates, final Map<Long, Integer> loadInfo, long dataCenterId) {
    	if(candidates != null) {
    		
    		List<ConsoleProxyVO> allocationList = new ArrayList<ConsoleProxyVO>();
    		for(ConsoleProxyVO proxy : candidates) {
				allocationList.add(proxy);
    		}
    		
    		Collections.sort(candidates, new Comparator<ConsoleProxyVO> () {
				@Override
				public int compare(ConsoleProxyVO x, ConsoleProxyVO y) {
					Integer loadOfX = loadInfo.get(x.getId());
					Integer loadOfY = loadInfo.get(y.getId());

					if(loadOfX != null && loadOfY != null) {
						if(loadOfX < loadOfY)
							return -1;
						else if(loadOfX > loadOfY)
							return 1;
						return 0;
					} else if(loadOfX == null && loadOfY == null) {
						return 0;
					} else {
						if(loadOfX == null)
							return -1;
						return 1;
					}
				}
    		});
    		
    		if(allocationList.size() > 0)
    			return allocationList.get(0);
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

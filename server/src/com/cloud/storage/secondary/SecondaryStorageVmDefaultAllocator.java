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
package com.cloud.storage.secondary;

import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import com.cloud.storage.secondary.SecondaryStorageVmAllocator;
import com.cloud.vm.SecondaryStorageVmVO;

@Local(value={SecondaryStorageVmAllocator.class})
public class SecondaryStorageVmDefaultAllocator implements SecondaryStorageVmAllocator {
	
    private String _name;
    private Random _rand = new Random(System.currentTimeMillis());

    @Override
	public SecondaryStorageVmVO allocSecondaryStorageVm(List<SecondaryStorageVmVO> candidates, Map<Long, Integer> loadInfo, long dataCenterId) {
    	if(candidates.size() > 0)
			return candidates.get(_rand.nextInt(candidates.size()));
    	return null;
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
      /*  _name = name;
        ComponentLocator locator = ComponentLocator.getCurrentLocator();
        ConfigurationDao configDao = locator.getDao(ConfigurationDao.class);
        if (configDao == null) {
            throw new ConfigurationException("Unable to get the configuration dao.");
        }
               */
        
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

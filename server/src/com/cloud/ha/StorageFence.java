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
package com.cloud.ha;

import java.util.Map;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.ha.FenceBuilder;
import com.cloud.host.HostVO;
import com.cloud.storage.StorageManager;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.vm.VMInstanceVO;

@Local(value=FenceBuilder.class)
public class StorageFence implements FenceBuilder {
	private final static Logger s_logger = Logger.getLogger(StorageFence.class);
	
	String _name;
	
	StorageManager _storageMgr;
	
	public StorageFence() {
	}
	
	@Override
	public Boolean fenceOff(VMInstanceVO vm, HostVO host) {
		s_logger.debug("Asking storage server to unshare " + vm.toString());
		return _storageMgr.unshare(vm, host) != null;
	}

	@Override
	public boolean configure(String name, Map<String, Object> params)
			throws ConfigurationException {
		_name = name;
		
		ComponentLocator locator = ComponentLocator.getCurrentLocator();
		if (locator == null) {
			throw new ConfigurationException("Unable to locate the locator!");
		}
		
		_storageMgr = locator.getManager(StorageManager.class);
		if (_storageMgr == null) {
			throw new ConfigurationException("Unable to get " + StorageManager.class);
		}
		
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

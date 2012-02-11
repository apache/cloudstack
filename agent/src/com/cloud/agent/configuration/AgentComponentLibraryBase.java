/**
 *  Copyright (C) 2011 Cloud.com.  All rights reserved.
 *
 * This software is licensed under the GNU General Public License v3 or later.
 *
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later
version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.cloud.agent.configuration;

import java.util.List;
import java.util.Map;

import com.cloud.utils.component.Adapter;
import com.cloud.utils.component.ComponentLibraryBase;
import com.cloud.utils.component.ComponentLocator.ComponentInfo;
import com.cloud.utils.component.Manager;
import com.cloud.utils.component.PluggableService;
import com.cloud.utils.db.GenericDao;

public class AgentComponentLibraryBase extends ComponentLibraryBase {
	@Override
	public Map<String, ComponentInfo<GenericDao<?, ?>>> getDaos() {
		return null;
	}

	@Override
	public Map<String, ComponentInfo<Manager>> getManagers() {
		if (_managers.size() == 0) {
			populateManagers();
		}
		return _managers;
	}

	@Override
	public Map<String, List<ComponentInfo<Adapter>>> getAdapters() {
		if (_adapters.size() == 0) {
			populateAdapters();
		}
		return _adapters;
	}

	@Override
	public Map<Class<?>, Class<?>> getFactories() {
		return null;
	}

	protected void populateManagers() {
		// addManager("StackMaidManager", StackMaidManagerImpl.class);
	}

	protected void populateAdapters() {

	}

	protected void populateServices() {

	}

	@Override
	public Map<String, ComponentInfo<PluggableService>> getPluggableServices() {
		if (_pluggableServices.size() == 0) {
			populateServices();
		}
		return _pluggableServices;
	}

}

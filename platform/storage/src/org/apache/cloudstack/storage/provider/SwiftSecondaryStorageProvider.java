/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cloudstack.storage.provider;

import java.util.List;
import java.util.Map;

import javax.naming.ConfigurationException;

import org.apache.cloudstack.platform.subsystem.api.storage.DataStore;
import org.apache.cloudstack.platform.subsystem.api.storage.DataStoreConfigurator;
import org.apache.cloudstack.platform.subsystem.api.storage.StorageProvider;
import org.apache.cloudstack.platform.subsystem.api.storage.DataStore.StoreType;

import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.storage.StoragePool;

public class SwiftSecondaryStorageProvider implements StorageProvider {

	public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
		// TODO Auto-generated method stub
		return false;
	}

	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean start() {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean stop() {
		// TODO Auto-generated method stub
		return false;
	}

	public List<HypervisorType> supportedHypervisors() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getProviderName() {
		// TODO Auto-generated method stub
		return null;
	}

	public List<StoreType> supportedStoreTypes() {
		// TODO Auto-generated method stub
		return null;
	}

	public void configure(Map<String, String> storeProviderInfo) {
		// TODO Auto-generated method stub

	}

	public DataStore addDataStore(StoragePool sp, String uri, Map<String, String> params) {
		// TODO Auto-generated method stub
		return null;
	}

	public DataStore getDataStore(StoragePool pool) {
		// TODO Auto-generated method stub
		return null;
	}

	public Map<HypervisorType, Map<String, DataStoreConfigurator>> getDataStoreConfigs() {
		// TODO Auto-generated method stub
		return null;
	}

}

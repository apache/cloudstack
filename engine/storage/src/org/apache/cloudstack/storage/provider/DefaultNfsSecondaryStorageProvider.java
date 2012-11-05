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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.naming.ConfigurationException;

import org.apache.cloudstack.platform.subsystem.api.storage.DataStore;
import org.apache.cloudstack.platform.subsystem.api.storage.DataStoreConfigurator;
import org.apache.cloudstack.platform.subsystem.api.storage.StorageProvider;
import org.apache.cloudstack.platform.subsystem.api.storage.DataStore.StoreType;
import org.apache.cloudstack.storage.datastoreconfigurator.NfsSecondaryStorageConfigurator;
import org.apache.cloudstack.storage.datastoreconfigurator.XenNfsDataStoreConfigurator;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.storage.StoragePool;

public class DefaultNfsSecondaryStorageProvider implements StorageProvider {
	private String _name = DefaultPrimaryStorageProvider.class.toString();
	protected Map<HypervisorType, Map<String, DataStoreConfigurator>> _supportedProtocols;
	public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
		Map<String, DataStoreConfigurator> dscs = new HashMap<String, DataStoreConfigurator>();
		DataStoreConfigurator nfsdc = new NfsSecondaryStorageConfigurator();
		dscs.put(nfsdc.getProtocol(), nfsdc);
	
		_supportedProtocols.put(HypervisorType.XenServer, dscs);
		_supportedProtocols.put(HypervisorType.KVM, dscs);
		_supportedProtocols.put(HypervisorType.VMware, dscs);
		_supportedProtocols.put(HypervisorType.Ovm, dscs);
		return true;
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
		List<HypervisorType> hypervisors = new ArrayList<HypervisorType>();
		Set<HypervisorType> hyps = _supportedProtocols.keySet();
		
		for (HypervisorType hy : hyps) {
			hypervisors.add(hy);
		}
		
		return hypervisors;
	}

	public String getProviderName() {
		// TODO Auto-generated method stub
		return null;
	}

	public void configure(Map<String, String> storeProviderInfo) {
		// TODO Auto-generated method stub

	}

	public DataStore addDataStore(StoragePool sp, String url, Map<String, String> params) {
		URI uri;
		try {
			uri = new URI(url);
		} catch (URISyntaxException e) {
			throw new InvalidParameterValueException("invalide url" + url);
		}
		
		String protocol = uri.getScheme();
		if (protocol == null) {
			throw new InvalidParameterValueException("the protocol can't be null");
		}
		
		DataStoreConfigurator dscf = _supportedProtocols.get(HypervisorType.XenServer).get(protocol);
		Map<String, String> configs = dscf.getConfigs(uri, params);
		dscf.validate(configs);
		DataStore ds = dscf.getDataStore(sp);
		return ds;
	}

	public DataStore getDataStore(StoragePool pool) {
		// TODO Auto-generated method stub
		return null;
	}

	public Map<HypervisorType, Map<String, DataStoreConfigurator>> getDataStoreConfigs() {
		// TODO Auto-generated method stub
		return null;
	}

	public List<StoreType> supportedStoreTypes() {
		List<StoreType> types = new ArrayList<StoreType>();
		types.add(StoreType.Image);
		types.add(StoreType.Backup);
		return types;
	}

}

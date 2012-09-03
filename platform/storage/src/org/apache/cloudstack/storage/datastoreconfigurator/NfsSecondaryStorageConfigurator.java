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
package org.apache.cloudstack.storage.datastoreconfigurator;

import java.util.Map;

import org.apache.cloudstack.platform.subsystem.api.storage.DataStore;
import org.apache.cloudstack.platform.subsystem.api.storage.DataStore.StoreType;
import org.apache.cloudstack.storage.datastore.DefaultDataStore;
import org.apache.cloudstack.storage.driver.DefaultNfsSecondaryDriver;
import org.apache.cloudstack.storage.epselector.DefaultNfsSecondaryEndPointSelector;

import org.apache.cloudstack.storage.lifecycle.DefaultNfsSecondaryLifeCycle;

import org.apache.cloudstack.storage.strategy.DefaultTemplateStratey;


import com.cloud.storage.StoragePool;

public class NfsSecondaryStorageConfigurator extends NfsDataStoreConfigurator {
	@Override
	public DataStore getDataStore(StoragePool pool) {
		DefaultDataStore ds = new DefaultDataStore();
		ds.setEndPointSelector(new DefaultNfsSecondaryEndPointSelector(ds));
		ds.setId(pool.getId());
		ds.setType(StoreType.Image);
		ds.setURI(pool.getHostAddress() + "/" + pool.getPath());
		ds.setUUID(pool.getUuid());
		ds.setDataStoreDriver(new DefaultNfsSecondaryDriver(ds));
		ds.setTemplateStrategy(new DefaultTemplateStratey(ds));
		ds.setLifeCycle(new DefaultNfsSecondaryLifeCycle(ds));
		return ds;
	}
	public StoragePool getStoragePool(Map<String, String> configs) {
		// TODO Auto-generated method stub
		return null;
	}

}

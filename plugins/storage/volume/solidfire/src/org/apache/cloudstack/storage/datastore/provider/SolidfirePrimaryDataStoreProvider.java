// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package org.apache.cloudstack.storage.datastore.provider;

import java.util.List;

import org.apache.cloudstack.storage.datastore.DefaultPrimaryDataStore;
import org.apache.cloudstack.storage.datastore.PrimaryDataStore;
import org.apache.cloudstack.storage.datastore.configurator.PrimaryDataStoreConfigurator;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreVO;
import org.apache.cloudstack.storage.datastore.driver.SolidfirePrimaryDataStoreDriver;
import org.apache.cloudstack.storage.datastore.lifecycle.DefaultPrimaryDataStoreLifeCycleImpl;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class SolidfirePrimaryDataStoreProvider extends
	DefaultPrimaryDatastoreProviderImpl {
	private final String name = "Solidfre Primary Data Store Provider";


	public SolidfirePrimaryDataStoreProvider(@Qualifier("solidfire") List<PrimaryDataStoreConfigurator> configurators) {
	    super(configurators);
		
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public String getName() {
		return name;
	}
	
	@Override
	public PrimaryDataStore getDataStore(long dataStoreId) {
		PrimaryDataStoreVO dsv = dataStoreDao.findById(dataStoreId);
        if (dsv == null) {
            return null;
        }

        DefaultPrimaryDataStore pds = DefaultPrimaryDataStore.createDataStore(dsv);
        SolidfirePrimaryDataStoreDriver driver = new SolidfirePrimaryDataStoreDriver();
        pds.setDriver(driver);

        
        DefaultPrimaryDataStoreLifeCycleImpl lifeCycle = new DefaultPrimaryDataStoreLifeCycleImpl(dataStoreDao);

        pds.setLifeCycle(lifeCycle);
        return pds;
    }
}

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
package org.apache.cloudstack.storage.datastore.configurator;

import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreLifeCycle;
import org.apache.cloudstack.storage.datastore.DefaultPrimaryDataStore;
import org.apache.cloudstack.storage.datastore.PrimaryDataStore;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreVO;
import org.apache.cloudstack.storage.datastore.driver.PrimaryDataStoreDriver;
import com.cloud.utils.exception.CloudRuntimeException;

public abstract class AbstractPrimaryDataStoreConfigurator implements PrimaryDataStoreConfigurator {
    @Inject
    protected PrimaryDataStoreDao dataStoreDao;
    
    protected abstract PrimaryDataStoreLifeCycle getLifeCycle();
    
    protected abstract PrimaryDataStoreDriver getDriver();
    
    protected abstract boolean isLocalStorageSupported();

    @Override
    public PrimaryDataStore getDataStore(long dataStoreId) {
        PrimaryDataStoreVO dataStoreVO = dataStoreDao.findById(dataStoreId);
        if (dataStoreVO == null) {
            throw new CloudRuntimeException("Can't find primary data store: " + dataStoreId);
        }
        
        DefaultPrimaryDataStore dataStore = DefaultPrimaryDataStore.createDataStore(dataStoreVO);
        dataStore.setDriver(getDriver());
        dataStore.setLifeCycle(getLifeCycle());
        dataStore.setSupportedHypervisor(getSupportedHypervisor());
        dataStore.setLocalStorageFlag(isLocalStorageSupported());
        dataStore.setProtocolTransFormer(getProtocolTransformer());
        return dataStore;
    }
}

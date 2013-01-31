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
package org.apache.cloudstack.storage.datastore.manager;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreProvider;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreProviderManager;
import org.apache.cloudstack.engine.subsystem.api.storage.HypervisorHostListener;
import org.apache.cloudstack.storage.datastore.DefaultPrimaryDataStore;
import org.apache.cloudstack.storage.datastore.PrimaryDataStore;
import org.apache.cloudstack.storage.datastore.PrimaryDataStoreProviderManager;
import org.apache.cloudstack.storage.datastore.db.DataStoreProviderDao;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.volume.PrimaryDataStoreDriver;
import org.springframework.stereotype.Component;

@Component
public class DefaultPrimaryDataStoreProviderManagerImpl implements PrimaryDataStoreProviderManager {
    @Inject
    DataStoreProviderDao dataStoreProviderDao;
    @Inject
    DataStoreProviderManager providerManager;
    @Inject
    PrimaryDataStoreDao dataStoreDao;
    Map<String, PrimaryDataStoreDriver> driverMaps;

    @PostConstruct
    public void config() {
        driverMaps = new HashMap<String, PrimaryDataStoreDriver>();
    }
    
    @Override
    public PrimaryDataStore getPrimaryDataStore(long dataStoreId) {
        StoragePoolVO dataStoreVO = dataStoreDao.findById(dataStoreId);
        long providerId = dataStoreVO.getStorageProviderId();
        DataStoreProvider provider = providerManager.getDataStoreProviderById(providerId);
        DefaultPrimaryDataStore dataStore = DefaultPrimaryDataStore.createDataStore(dataStoreVO, driverMaps.get(provider.getUuid()), provider);
        return dataStore;
    }
    
    @Override
    public boolean registerDriver(String uuid, PrimaryDataStoreDriver driver) {
        if (driverMaps.get(uuid) != null) {
            return false;
        }
        driverMaps.put(uuid, driver);
        return true;
    }

    @Override
    public PrimaryDataStore getPrimaryDataStore(String uuid) {
        StoragePoolVO dataStoreVO = dataStoreDao.findByUuid(uuid);
        return getPrimaryDataStore(dataStoreVO.getId());
    }

    @Override
    public boolean registerHostListener(String uuid, HypervisorHostListener listener) {
        // TODO Auto-generated method stub
        return false;
    }
}

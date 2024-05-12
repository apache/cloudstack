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
package org.apache.cloudstack.storage.object.manager;

import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreProviderManager;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectStoreProvider;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.storage.datastore.db.ObjectStoreDao;
import org.apache.cloudstack.storage.datastore.db.ObjectStoreVO;
import org.apache.cloudstack.storage.object.ObjectStoreDriver;
import org.apache.cloudstack.storage.object.ObjectStoreEntity;
import org.apache.cloudstack.storage.object.datastore.ObjectStoreProviderManager;
import org.apache.cloudstack.storage.object.store.ObjectStoreImpl;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ObjectStoreProviderManagerImpl implements ObjectStoreProviderManager, Configurable {
    @Inject
    ObjectStoreDao objectStoreDao;

    @Inject
    DataStoreProviderManager providerManager;

    Map<String, ObjectStoreDriver> driverMaps;

    @PostConstruct
    public void config() {
        driverMaps = new HashMap<String, ObjectStoreDriver>();
    }

    @Override
    public ObjectStoreEntity getObjectStore(long objectStoreId) {
        ObjectStoreVO objectStore = objectStoreDao.findById(objectStoreId);
        String providerName = objectStore.getProviderName();
        ObjectStoreProvider provider = (ObjectStoreProvider)providerManager.getDataStoreProvider(providerName);
        ObjectStoreEntity objStore = ObjectStoreImpl.getDataStore(objectStore, driverMaps.get(provider.getName()), provider);
        return objStore;
    }

    @Override
    public boolean registerDriver(String providerName, ObjectStoreDriver driver) {
        if (driverMaps.containsKey(providerName)) {
            return false;
        }
        driverMaps.put(providerName, driver);
        return true;
    }

    @Override
    public ObjectStoreEntity getObjectStore(String uuid) {
        ObjectStoreVO objectStore = objectStoreDao.findByUuid(uuid);
        return getObjectStore(objectStore.getId());
    }

    @Override
    public List<DataStore> listObjectStores() {
        List<ObjectStoreVO> stores = objectStoreDao.listObjectStores();
        List<DataStore> ObjectStores = new ArrayList<DataStore>();
        for (ObjectStoreVO store : stores) {
            ObjectStores.add(getObjectStore(store.getId()));
        }
        return ObjectStores;
    }

    @Override
    public List<DataStore> listObjectStoreByProvider(String provider) {
        List<ObjectStoreVO> stores = objectStoreDao.findByProvider(provider);
        List<DataStore> ObjectStores = new ArrayList<DataStore>();
        for (ObjectStoreVO store : stores) {
            ObjectStores.add(getObjectStore(store.getId()));
        }
        return ObjectStores;
    }

    @Override
    public String getConfigComponentName() {
        return ObjectStoreProviderManager.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[] {  };
    }
}

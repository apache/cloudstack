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
package org.apache.cloudstack.storage.image.manager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreProviderManager;
import org.apache.cloudstack.storage.datastore.provider.ImageDataStoreProvider;
import org.apache.cloudstack.storage.image.ImageDataStoreDriver;
import org.apache.cloudstack.storage.image.datastore.ImageDataStore;
import org.apache.cloudstack.storage.image.datastore.ImageDataStoreManager;
import org.apache.cloudstack.storage.image.db.ImageDataStoreDao;
import org.apache.cloudstack.storage.image.db.ImageDataStoreVO;
import org.apache.cloudstack.storage.image.store.DefaultImageDataStoreImpl;
import org.springframework.stereotype.Component;

import com.cloud.storage.dao.VMTemplateDao;

@Component
public class ImageDataStoreManagerImpl implements ImageDataStoreManager {
    @Inject
    ImageDataStoreDao dataStoreDao;
    @Inject
    VMTemplateDao imageDataDao;
    @Inject
    DataStoreProviderManager providerManager;
    Map<String, ImageDataStoreDriver> driverMaps;

    @PostConstruct
    public void config() {
        driverMaps = new HashMap<String, ImageDataStoreDriver>();
    }
    
    @Override
    public ImageDataStore getImageDataStore(long dataStoreId) {
        ImageDataStoreVO dataStore = dataStoreDao.findById(dataStoreId);
        long providerId = dataStore.getProvider();
        ImageDataStoreProvider provider = (ImageDataStoreProvider)providerManager.getDataStoreProviderById(providerId);
        ImageDataStore imgStore = DefaultImageDataStoreImpl.getDataStore(dataStore, 
                driverMaps.get(provider.getUuid()), provider
                );
        // TODO Auto-generated method stub
        return imgStore;
    }

    @Override
    public boolean registerDriver(String uuid, ImageDataStoreDriver driver) {
        if (driverMaps.containsKey(uuid)) {
            return false;
        }
        driverMaps.put(uuid, driver);
        return true;
    }

    @Override
    public ImageDataStore getImageDataStore(String uuid) {
        ImageDataStoreVO dataStore = dataStoreDao.findByUuid(uuid);
        return getImageDataStore(dataStore.getId());
    }

    @Override
    public List<DataStore> getList() {
        List<ImageDataStoreVO> stores = dataStoreDao.listAll();
        List<DataStore> imageStores = new ArrayList<DataStore>();
        for (ImageDataStoreVO store : stores) {
            imageStores.add(getImageDataStore(store.getId()));
        }
        return imageStores;
    }

}

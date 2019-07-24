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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreProviderManager;
import org.apache.cloudstack.engine.subsystem.api.storage.ImageStoreProvider;
import org.apache.cloudstack.engine.subsystem.api.storage.Scope;
import org.apache.cloudstack.engine.subsystem.api.storage.ZoneScope;
import org.apache.cloudstack.storage.datastore.db.ImageStoreDao;
import org.apache.cloudstack.storage.datastore.db.ImageStoreVO;
import org.apache.cloudstack.storage.image.ImageStoreDriver;
import org.apache.cloudstack.storage.image.datastore.ImageStoreEntity;
import org.apache.cloudstack.storage.image.datastore.ImageStoreProviderManager;
import org.apache.cloudstack.storage.image.store.ImageStoreImpl;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.server.StatsCollector;
import com.cloud.storage.ScopeType;
import com.cloud.storage.dao.VMTemplateDao;

@Component
public class ImageStoreProviderManagerImpl implements ImageStoreProviderManager {
    private static final Logger s_logger = Logger.getLogger(ImageStoreProviderManagerImpl.class);
    @Inject
    ImageStoreDao dataStoreDao;
    @Inject
    VMTemplateDao imageDataDao;
    @Inject
    DataStoreProviderManager providerManager;
    @Inject
    StatsCollector _statsCollector;
    Map<String, ImageStoreDriver> driverMaps;

    @PostConstruct
    public void config() {
        driverMaps = new HashMap<String, ImageStoreDriver>();
    }

    @Override
    public ImageStoreEntity getImageStore(long dataStoreId) {
        ImageStoreVO dataStore = dataStoreDao.findById(dataStoreId);
        String providerName = dataStore.getProviderName();
        ImageStoreProvider provider = (ImageStoreProvider)providerManager.getDataStoreProvider(providerName);
        ImageStoreEntity imgStore = ImageStoreImpl.getDataStore(dataStore, driverMaps.get(provider.getName()), provider);
        return imgStore;
    }

    @Override
    public boolean registerDriver(String providerName, ImageStoreDriver driver) {
        if (driverMaps.containsKey(providerName)) {
            return false;
        }
        driverMaps.put(providerName, driver);
        return true;
    }

    @Override
    public ImageStoreEntity getImageStore(String uuid) {
        ImageStoreVO dataStore = dataStoreDao.findByUuid(uuid);
        return getImageStore(dataStore.getId());
    }

    @Override
    public List<DataStore> listImageStores() {
        List<ImageStoreVO> stores = dataStoreDao.listImageStores();
        List<DataStore> imageStores = new ArrayList<DataStore>();
        for (ImageStoreVO store : stores) {
            imageStores.add(getImageStore(store.getId()));
        }
        return imageStores;
    }

    @Override
    public List<DataStore> listImageCacheStores() {
        List<ImageStoreVO> stores = dataStoreDao.listImageCacheStores();
        List<DataStore> imageStores = new ArrayList<DataStore>();
        for (ImageStoreVO store : stores) {
            imageStores.add(getImageStore(store.getId()));
        }
        return imageStores;
    }

    @Override
    public List<DataStore> listImageStoresByScope(ZoneScope scope) {
        List<ImageStoreVO> stores = dataStoreDao.findByScope(scope);
        List<DataStore> imageStores = new ArrayList<DataStore>();
        for (ImageStoreVO store : stores) {
            imageStores.add(getImageStore(store.getId()));
        }
        return imageStores;
    }

    @Override
    public List<DataStore> listImageStoreByProvider(String provider) {
        List<ImageStoreVO> stores = dataStoreDao.findByProvider(provider);
        List<DataStore> imageStores = new ArrayList<DataStore>();
        for (ImageStoreVO store : stores) {
            imageStores.add(getImageStore(store.getId()));
        }
        return imageStores;
    }

    @Override
    public List<DataStore> listImageCacheStores(Scope scope) {
        if (scope.getScopeType() != ScopeType.ZONE) {
            s_logger.debug("only support zone wide image cache stores");
            return null;
        }
        List<ImageStoreVO> stores = dataStoreDao.findImageCacheByScope(new ZoneScope(scope.getScopeId()));
        List<DataStore> imageStores = new ArrayList<DataStore>();
        for (ImageStoreVO store : stores) {
            imageStores.add(getImageStore(store.getId()));
        }
        return imageStores;
    }

    @Override
    public DataStore getRandomImageStore(List<DataStore> imageStores) {
        if (imageStores.size() > 1) {
            Collections.shuffle(imageStores);
        }
        return imageStores.get(0);
    }

    @Override
    public DataStore getImageStoreWithFreeCapacity(List<DataStore> imageStores) {
        if (imageStores.size() > 1) {
            imageStores.sort(new Comparator<DataStore>() { // Sort data stores based on free capacity
                @Override
                public int compare(DataStore store1, DataStore store2) {
                    return Long.compare(_statsCollector.imageStoreCurrentFreeCapacity(store1),
                            _statsCollector.imageStoreCurrentFreeCapacity(store2));
                }
            });
            for (DataStore imageStore : imageStores) {
                // Return image store if used percentage is less then threshold value i.e. 90%.
                if (_statsCollector.imageStoreHasEnoughCapacity(imageStore)) {
                    return imageStore;
                }
            }
        } else if (imageStores.size() == 1) {
            if (_statsCollector.imageStoreHasEnoughCapacity(imageStores.get(0))) {
                return imageStores.get(0);
            }
        }

        // No store with space found
        s_logger.error(String.format("Can't find an image storage in zone with less than %d usage",
                Math.round(_statsCollector.getImageStoreCapacityThreshold()*100)));
        return null;
    }

    @Override
    public List<DataStore> listImageStoresWithFreeCapacity(List<DataStore> imageStores) {
        List<DataStore> stores = new ArrayList<>();
        for (DataStore imageStore : imageStores) {
            // Return image store if used percentage is less then threshold value i.e. 90%.
            if (_statsCollector.imageStoreHasEnoughCapacity(imageStore)) {
                stores.add(imageStore);
            }
        }

        // No store with space found
        if (stores.isEmpty()) {
            s_logger.error(String.format("Can't find image storage in zone with less than %d usage",
                    Math.round(_statsCollector.getImageStoreCapacityThreshold() * 100)));
        }
        return stores;
    }
}

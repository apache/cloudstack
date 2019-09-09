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
package org.apache.cloudstack.storage.datastore;

import java.util.List;

import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.ZoneScope;
import org.apache.cloudstack.engine.subsystem.api.storage.Scope;
import org.springframework.stereotype.Component;

import org.apache.cloudstack.storage.image.datastore.ImageStoreProviderManager;

import com.cloud.storage.DataStoreRole;
import com.cloud.storage.ScopeType;
import com.cloud.utils.exception.CloudRuntimeException;

@Component
public class DataStoreManagerImpl implements DataStoreManager {
    @Inject
    PrimaryDataStoreProviderManager primaryStoreMgr;
    @Inject
    ImageStoreProviderManager imageDataStoreMgr;

    @Override
    public DataStore getDataStore(long storeId, DataStoreRole role) {
        try {
            if (role == DataStoreRole.Primary) {
                return primaryStoreMgr.getPrimaryDataStore(storeId);
            } else if (role == DataStoreRole.Image) {
                return imageDataStoreMgr.getImageStore(storeId);
            } else if (role == DataStoreRole.ImageCache) {
                return imageDataStoreMgr.getImageStore(storeId);
            }
        } catch (CloudRuntimeException e) {
            throw e;
        }
        throw new CloudRuntimeException("un recognized type" + role);
    }

    @Override
    public DataStore getDataStore(String uuid, DataStoreRole role) {
        if (role == DataStoreRole.Primary) {
            return primaryStoreMgr.getPrimaryDataStore(uuid);
        } else if (role == DataStoreRole.Image) {
            return imageDataStoreMgr.getImageStore(uuid);
        }
        throw new CloudRuntimeException("un recognized type" + role);
    }

    @Override
    public List<DataStore> getImageStoresByScope(ZoneScope scope) {
        return imageDataStoreMgr.listImageStoresByScope(scope);
    }

    @Override
    public DataStore getRandomImageStore(long zoneId) {
        List<DataStore> stores = getImageStoresByScope(new ZoneScope(zoneId));
        if (stores == null || stores.size() == 0) {
            return null;
        }
        return imageDataStoreMgr.getRandomImageStore(stores);
    }

    @Override
    public DataStore getImageStoreWithFreeCapacity(long zoneId) {
        List<DataStore> stores = getImageStoresByScope(new ZoneScope(zoneId));
        if (stores == null || stores.size() == 0) {
            return null;
        }
        return imageDataStoreMgr.getImageStoreWithFreeCapacity(stores);
    }

    @Override
    public List<DataStore> listImageStoresWithFreeCapacity(long zoneId) {
        List<DataStore> stores = getImageStoresByScope(new ZoneScope(zoneId));
        if (stores == null || stores.size() == 0) {
            return null;
        }
        return imageDataStoreMgr.listImageStoresWithFreeCapacity(stores);
    }

    @Override
    public boolean isRegionStore(DataStore store) {
        if (store.getScope().getScopeType() == ScopeType.ZONE && store.getScope().getScopeId() == null)
            return true;
        else
            return false;
    }

    @Override
    public DataStore getPrimaryDataStore(long storeId) {
        return primaryStoreMgr.getPrimaryDataStore(storeId);
    }

    @Override
    public DataStore getPrimaryDataStore(String storeUuid) {
        return primaryStoreMgr.getPrimaryDataStore(storeUuid);
    }

    @Override
    public List<DataStore> getImageCacheStores(Scope scope) {
        return imageDataStoreMgr.listImageCacheStores(scope);
    }

    @Override
    public DataStore getImageCacheStore(long zoneId) {
        List<DataStore> stores = getImageCacheStores(new ZoneScope(zoneId));
        if (stores == null || stores.size() == 0) {
            return null;
        }
        return imageDataStoreMgr.getImageStoreWithFreeCapacity(stores);
    }

    @Override
    public List<DataStore> listImageStores() {
        return imageDataStoreMgr.listImageStores();
    }

    @Override
    public List<DataStore> listImageCacheStores() {
        return imageDataStoreMgr.listImageCacheStores();
    }

    public void setPrimaryStoreMgr(PrimaryDataStoreProviderManager primaryStoreMgr) {
        this.primaryStoreMgr = primaryStoreMgr;
    }

    public void setImageDataStoreMgr(ImageStoreProviderManager imageDataStoreMgr) {
        this.imageDataStoreMgr = imageDataStoreMgr;
    }
}

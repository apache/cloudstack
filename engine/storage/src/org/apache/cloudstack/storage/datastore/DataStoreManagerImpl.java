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
import java.util.Map;

import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.Scope;
import org.apache.cloudstack.engine.subsystem.api.storage.ZoneScope;
import org.apache.cloudstack.storage.image.datastore.ImageStoreProviderManager;
import org.springframework.stereotype.Component;

import com.cloud.storage.DataStoreRole;
import com.cloud.utils.exception.CloudRuntimeException;

@Component
public class DataStoreManagerImpl implements DataStoreManager {
    @Inject
    PrimaryDataStoreProviderManager primaryStorMgr;
    @Inject
    ImageStoreProviderManager imageDataStoreMgr;

    @Override
    public DataStore getDataStore(long storeId, DataStoreRole role) {
        if (role == DataStoreRole.Primary) {
            return primaryStorMgr.getPrimaryDataStore(storeId);
        } else if (role == DataStoreRole.Image) {
            return imageDataStoreMgr.getImageStore(storeId);
        } else if (role == DataStoreRole.ImageCache) {
            return imageDataStoreMgr.getImageStore(storeId);
        }
        throw new CloudRuntimeException("un recognized type" + role);
    }
    @Override
    public DataStore registerDataStore(Map<String, String> params,
            String providerUuid) {
        return null;
    }
    @Override
    public DataStore getDataStore(String uuid, DataStoreRole role) {
        if (role == DataStoreRole.Primary) {
            return primaryStorMgr.getPrimaryDataStore(uuid);
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
    public List<DataStore> getImageStoresByProvider(String provider) {
        return imageDataStoreMgr.listImageStoreByProvider(provider);
    }

    @Override
    public DataStore getPrimaryDataStore(long storeId) {
        return primaryStorMgr.getPrimaryDataStore(storeId);
    }
    @Override
    public List<DataStore> getImageCacheStores(Scope scope) {
    	return imageDataStoreMgr.listImageCacheStores(scope);
    }
}

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
package org.apache.cloudstack.storage.cache.allocator;

import java.util.List;

import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import org.apache.cloudstack.engine.subsystem.api.storage.DataObject;
import org.apache.cloudstack.engine.subsystem.api.storage.DataObjectInStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine;
import org.apache.cloudstack.engine.subsystem.api.storage.Scope;
import org.apache.cloudstack.storage.datastore.ObjectInDataStoreManager;
import org.apache.cloudstack.storage.image.datastore.ImageStoreProviderManager;

import com.cloud.server.StatsCollector;
import com.cloud.storage.ScopeType;

@Component
public class StorageCacheRandomAllocator implements StorageCacheAllocator {
    private static final Logger s_logger = Logger.getLogger(StorageCacheRandomAllocator.class);
    @Inject
    DataStoreManager dataStoreMgr;
    @Inject
    ObjectInDataStoreManager objectInStoreMgr;
    @Inject
    ImageStoreProviderManager imageStoreMgr;
    @Inject
    StatsCollector statsCollector;

    @Override
    public DataStore getCacheStore(Scope scope) {
        if (scope.getScopeType() != ScopeType.ZONE) {
            s_logger.debug("Can only support zone wide cache storage");
            return null;
        }

        List<DataStore> cacheStores = dataStoreMgr.getImageCacheStores(scope);
        if ((cacheStores == null) || (cacheStores.size() <= 0)) {
            s_logger.debug("Can't find staging storage in zone: " + scope.getScopeId());
            return null;
        }

        return imageStoreMgr.getImageStoreWithFreeCapacity(cacheStores);
    }

    @Override
    public DataStore getCacheStore(DataObject data, Scope scope) {
        if (scope.getScopeType() != ScopeType.ZONE) {
            s_logger.debug("Can only support zone wide cache storage");
            return null;
        }

        List<DataStore> cacheStores = dataStoreMgr.getImageCacheStores(scope);
        if (cacheStores.size() <= 0) {
            s_logger.debug("Can't find staging storage in zone: " + scope.getScopeId());
            return null;
        }

        // if there are multiple cache stores, we give priority to the one where data is already there
        if (cacheStores.size() > 1) {
            for (DataStore store : cacheStores) {
                DataObjectInStore obj = objectInStoreMgr.findObject(data, store);
                if (obj != null && obj.getState() == ObjectInDataStoreStateMachine.State.Ready && statsCollector.imageStoreHasEnoughCapacity(store)) {
                    s_logger.debug("pick the cache store " + store.getId() + " where data is already there");
                    return store;
                }
            }
        }
        return imageStoreMgr.getImageStoreWithFreeCapacity(cacheStores);
    }
}

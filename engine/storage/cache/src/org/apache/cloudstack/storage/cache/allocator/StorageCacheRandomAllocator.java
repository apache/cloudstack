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

import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.Scope;

import com.cloud.storage.ScopeType;
import com.cloud.utils.exception.CloudRuntimeException;

import edu.emory.mathcs.backport.java.util.Collections;

public class StorageCacheRandomAllocator implements StorageCacheAllocator {
    @Inject
    DataStoreManager dataStoreMgr;
    @Override
    public DataStore getCacheStore(Scope scope) {
        if (scope.getScopeType() != ScopeType.ZONE) {
            throw new CloudRuntimeException("Can only support zone wide cache storage");
        }
       
        List<DataStore> cacheStores = dataStoreMgr.getImageCacheStores(scope);
        if (cacheStores.size() <= 0) {
            throw new CloudRuntimeException("Can't find cache storage in zone: " + scope.getScopeId());
        }
        
        Collections.shuffle(cacheStores);
        return cacheStores.get(0);
    }
}

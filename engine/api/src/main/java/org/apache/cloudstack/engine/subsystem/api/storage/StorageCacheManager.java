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
package org.apache.cloudstack.engine.subsystem.api.storage;

import org.apache.cloudstack.framework.config.ConfigKey;

public interface StorageCacheManager {

    ConfigKey<Boolean> StorageCacheReplacementEnabled = new ConfigKey<>("Storage", Boolean.class,
            "storage.cache.replacement.enabled", "true",
            "enable or disable cache storage replacement algorithm.", true);

    ConfigKey<Integer> StorageCacheReplacementInterval = new ConfigKey<>("Storage", Integer.class,
            "storage.cache.replacement.interval", "86400",
            "time interval between cache replacement threads (in seconds).", true);

    ConfigKey<Integer> StorageCacheReplacementLRUTimeInterval = new ConfigKey<>("Storage", Integer.class,
            "storage.cache.replacement.lru.interval", "30",
            "time interval for unused data on cache storage (in days).", true);

    DataStore getCacheStorage(Scope scope);

    DataStore getCacheStorage(DataObject data, Scope scope);

    DataObject createCacheObject(DataObject data, Scope scope);

    /**
     * only create cache object in db
     *
     * @param data
     * @param scope
     * @return
     */
    DataObject getCacheObject(DataObject data, Scope scope);

    boolean deleteCacheObject(DataObject data);

    boolean releaseCacheObject(DataObject data);

    DataObject createCacheObject(DataObject data, DataStore store);
}

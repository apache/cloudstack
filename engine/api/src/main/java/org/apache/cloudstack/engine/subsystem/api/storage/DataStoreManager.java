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

import java.util.List;

import com.cloud.storage.DataStoreRole;

public interface DataStoreManager {
    DataStore getDataStore(long storeId, DataStoreRole role);

    DataStore getPrimaryDataStore(long storeId);

    DataStore getPrimaryDataStore(String storeUuid);

    DataStore getDataStore(String uuid, DataStoreRole role);

    List<DataStore> getImageStoresByScope(ZoneScope scope);

    DataStore getImageStore(long zoneId);

    List<DataStore> getImageCacheStores(Scope scope);

    DataStore getImageCacheStore(long zoneId);

    List<DataStore> listImageStores();

    List<DataStore> listImageCacheStores();

    boolean isRegionStore(DataStore store);
}

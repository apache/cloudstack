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
package org.apache.cloudstack.storage.datastore.db;

import java.util.List;

import com.cloud.storage.DataStoreRole;
import org.apache.cloudstack.engine.subsystem.api.storage.ZoneScope;

import com.cloud.utils.db.GenericDao;

public interface ImageStoreDao extends GenericDao<ImageStoreVO, Long> {
    ImageStoreVO findByName(String name);

    List<ImageStoreVO> findByProvider(String provider);

    List<ImageStoreVO> findByZone(ZoneScope scope, Boolean readonly);

    List<ImageStoreVO> findRegionImageStores();

    List<ImageStoreVO> findImageCacheByScope(ZoneScope scope);

    Integer countAllImageStores();

    List<ImageStoreVO> listImageStores();

    List<ImageStoreVO> listImageCacheStores();

    List<ImageStoreVO> listStoresByZoneId(long zoneId);

    List<ImageStoreVO> listAllStoresInZone(Long zoneId, String provider, DataStoreRole role);

    List<ImageStoreVO> findByProtocol(String protocol);

    ImageStoreVO findOneByZoneAndProtocol(long zoneId, String protocol);
}

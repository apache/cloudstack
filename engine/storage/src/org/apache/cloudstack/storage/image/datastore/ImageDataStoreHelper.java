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
package org.apache.cloudstack.storage.image.datastore;

import java.util.Map;

import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.ScopeType;
import org.apache.cloudstack.storage.image.db.ImageDataStoreDao;
import org.apache.cloudstack.storage.image.db.ImageDataStoreVO;
import org.springframework.stereotype.Component;

import com.cloud.utils.exception.CloudRuntimeException;

@Component
public class ImageDataStoreHelper {
    @Inject
    ImageDataStoreDao imageStoreDao;
    public ImageDataStoreVO createImageDataStore(Map<String, Object> params) {
        ImageDataStoreVO store = imageStoreDao.findByUuid((String)params.get("uuid"));
        if (store != null) {
            return store;
        }
        store = new ImageDataStoreVO();
        store.setName((String)params.get("name"));
        store.setProtocol((String)params.get("protocol"));
        store.setProvider((Long)params.get("provider"));
        store.setScope((ScopeType)params.get("scope"));
        store.setUuid((String)params.get("uuid"));
        store = imageStoreDao.persist(store);
        return store;
    }
    
    public boolean deleteImageDataStore(long id) {
        ImageDataStoreVO store = imageStoreDao.findById(id);
        if (store == null) {
            throw new CloudRuntimeException("can't find image store:" + id);
        }
        
        imageStoreDao.remove(id);
        return true;
    }
}

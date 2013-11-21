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

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.storage.datastore.db.ImageStoreDao;
import org.apache.cloudstack.storage.datastore.db.ImageStoreDetailVO;
import org.apache.cloudstack.storage.datastore.db.ImageStoreDetailsDao;
import org.apache.cloudstack.storage.datastore.db.ImageStoreVO;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreDao;

import com.cloud.storage.DataStoreRole;
import com.cloud.storage.ScopeType;
import com.cloud.utils.exception.CloudRuntimeException;

@Component
public class ImageStoreHelper {
    @Inject
    ImageStoreDao imageStoreDao;
    @Inject
    ImageStoreDetailsDao imageStoreDetailsDao;
    @Inject
    SnapshotDataStoreDao snapshotStoreDao;

    public ImageStoreVO createImageStore(Map<String, Object> params) {
        ImageStoreVO store = imageStoreDao.findByName((String)params.get("name"));
        if (store != null) {
            return store;
        }
        store = new ImageStoreVO();
        store.setProtocol((String)params.get("protocol"));
        store.setProviderName((String)params.get("providerName"));
        store.setScope((ScopeType)params.get("scope"));
        store.setDataCenterId((Long)params.get("zoneId"));
        String uuid = (String)params.get("uuid");
        if (uuid != null) {
            store.setUuid(uuid);
        } else {
            store.setUuid(UUID.randomUUID().toString());
        }
        store.setName((String)params.get("name"));
        if (store.getName() == null) {
            store.setName(store.getUuid());
        }
        store.setUrl((String)params.get("url"));
        store.setRole((DataStoreRole)params.get("role"));
        store = imageStoreDao.persist(store);
        return store;
    }

    public ImageStoreVO createImageStore(Map<String, Object> params, Map<String, String> details) {
        ImageStoreVO store = imageStoreDao.findByName((String)params.get("name"));
        if (store != null) {
            return store;
        }
        store = new ImageStoreVO();
        store.setProtocol((String)params.get("protocol"));
        store.setProviderName((String)params.get("providerName"));
        store.setScope((ScopeType)params.get("scope"));
        store.setDataCenterId((Long)params.get("zoneId"));
        String uuid = (String)params.get("uuid");
        if (uuid != null) {
            store.setUuid(uuid);
        } else {
            store.setUuid(UUID.randomUUID().toString());
        }
        store.setUrl((String)params.get("url"));
        store.setName((String)params.get("name"));
        if (store.getName() == null) {
            store.setName(store.getUuid());
        }
        store.setRole((DataStoreRole)params.get("role"));
        store = imageStoreDao.persist(store);

        // persist details
        if (details != null) {
            Iterator<String> keyIter = details.keySet().iterator();
            while (keyIter.hasNext()) {
                String key = keyIter.next().toString();
                ImageStoreDetailVO detail = new ImageStoreDetailVO();
                detail.setStoreId(store.getId());
                detail.setName(key);
                detail.setValue(details.get(key));
                imageStoreDetailsDao.persist(detail);
            }
        }
        return store;
    }

    public boolean deleteImageStore(long id) {
        ImageStoreVO store = imageStoreDao.findById(id);
        if (store == null) {
            throw new CloudRuntimeException("can't find image store:" + id);
        }

        imageStoreDao.remove(id);
        return true;
    }

    /**
     * Convert current NFS secondary storage to Staging store to be ready to migrate to S3 object store.
     * @param store NFS image store.
     * @return true if successful.
     */
    public boolean convertToStagingStore(DataStore store) {
        ImageStoreVO nfsStore = imageStoreDao.findById(store.getId());
        nfsStore.setRole(DataStoreRole.ImageCache);
        imageStoreDao.update(store.getId(), nfsStore);
        // clear snapshot entry on primary store to make next snapshot become full snapshot
        snapshotStoreDao.deleteSnapshotRecordsOnPrimary();
        return true;
    }
}

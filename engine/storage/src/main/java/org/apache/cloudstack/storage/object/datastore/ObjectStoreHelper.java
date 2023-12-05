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
package org.apache.cloudstack.storage.object.datastore;

import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.storage.datastore.db.ObjectStoreDao;
import org.apache.cloudstack.storage.datastore.db.ObjectStoreDetailVO;
import org.apache.cloudstack.storage.datastore.db.ObjectStoreDetailsDao;
import org.apache.cloudstack.storage.datastore.db.ObjectStoreVO;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

@Component
public class ObjectStoreHelper {
    @Inject
    ObjectStoreDao ObjectStoreDao;
    @Inject
    ObjectStoreDetailsDao ObjectStoreDetailsDao;

    public ObjectStoreVO createObjectStore(Map<String, Object> params, Map<String, String> details) {
        ObjectStoreVO store = new ObjectStoreVO();

        store.setProviderName((String)params.get("providerName"));
        store.setUuid(UUID.randomUUID().toString());
        store.setUrl((String)params.get("url"));
        store.setName((String)params.get("name"));

        store = ObjectStoreDao.persist(store);

        // persist details
        if (details != null) {
            Iterator<String> keyIter = details.keySet().iterator();
            while (keyIter.hasNext()) {
                String key = keyIter.next().toString();
                String value = details.get(key);
                ObjectStoreDetailVO detail = new ObjectStoreDetailVO(store.getId(), key, value);
                ObjectStoreDetailsDao.persist(detail);
            }
        }
        return store;
    }

    public boolean deleteObjectStore(long id) {
        ObjectStoreVO store = ObjectStoreDao.findById(id);
        if (store == null) {
            throw new CloudRuntimeException("can't find Object store:" + id);
        }

        ObjectStoreDao.remove(id);
        return true;
    }
}

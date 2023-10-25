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

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import org.apache.cloudstack.api.response.ObjectStoreResponse;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.naming.ConfigurationException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class ObjectStoreDaoImpl extends GenericDaoBase<ObjectStoreVO, Long> implements ObjectStoreDao {
    private SearchBuilder<ObjectStoreVO> nameSearch;
    private SearchBuilder<ObjectStoreVO> providerSearch;
    @Inject
    private ConfigurationDao _configDao;
    private final SearchBuilder<ObjectStoreVO> osSearch;

    private SearchBuilder<ObjectStoreVO> urlSearch;

    protected ObjectStoreDaoImpl() {
        osSearch = createSearchBuilder();
        osSearch.and("idIN", osSearch.entity().getId(), SearchCriteria.Op.IN);
        osSearch.done();
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);

        nameSearch = createSearchBuilder();
        nameSearch.and("name", nameSearch.entity().getName(), SearchCriteria.Op.EQ);
        nameSearch.done();

        providerSearch = createSearchBuilder();
        providerSearch.and("providerName", providerSearch.entity().getProviderName(), SearchCriteria.Op.EQ);
        providerSearch.done();

        urlSearch = createSearchBuilder();
        urlSearch.and("url", urlSearch.entity().getUrl(), SearchCriteria.Op.EQ);
        urlSearch.done();

        return true;
    }

    @Override
    public ObjectStoreVO findByName(String name) {
        SearchCriteria<ObjectStoreVO> sc = nameSearch.create();
        sc.setParameters("name", name);
        return findOneBy(sc);
    }

    @Override
    public List<ObjectStoreVO> findByProvider(String provider) {
        SearchCriteria<ObjectStoreVO> sc = providerSearch.create();
        sc.setParameters("providerName", provider);
        return listBy(sc);
    }

    @Override
    public ObjectStoreVO findByUrl(String url) {
        SearchCriteria<ObjectStoreVO> sc = urlSearch.create();
        sc.setParameters("url", url);
        return findOneBy(sc);
    }

    @Override
    public List<ObjectStoreVO> listObjectStores() {
        SearchCriteria<ObjectStoreVO> sc = createSearchCriteria();
        return listBy(sc);
    }

    @Override
    public List<ObjectStoreVO> searchByIds(Long[] osIds) {
        // set detail batch query size
        int DETAILS_BATCH_SIZE = 2000;
        String batchCfg = _configDao.getValue("detail.batch.query.size");
        if (batchCfg != null) {
            DETAILS_BATCH_SIZE = Integer.parseInt(batchCfg);
        }
        // query details by batches
        List<ObjectStoreVO> osList = new ArrayList<>();
        // query details by batches
        int curr_index = 0;
        if (osIds.length > DETAILS_BATCH_SIZE) {
            while ((curr_index + DETAILS_BATCH_SIZE) <= osIds.length) {
                Long[] ids = new Long[DETAILS_BATCH_SIZE];
                for (int k = 0, j = curr_index; j < curr_index + DETAILS_BATCH_SIZE; j++, k++) {
                    ids[k] = osIds[j];
                }
                SearchCriteria<ObjectStoreVO> sc = osSearch.create();
                sc.setParameters("idIN", ids);
                List<ObjectStoreVO> stores = searchIncludingRemoved(sc, null, null, false);
                if (stores != null) {
                    osList.addAll(stores);
                }
                curr_index += DETAILS_BATCH_SIZE;
            }
        }
        if (curr_index < osIds.length) {
            int batch_size = (osIds.length - curr_index);
            // set the ids value
            Long[] ids = new Long[batch_size];
            for (int k = 0, j = curr_index; j < curr_index + batch_size; j++, k++) {
                ids[k] = osIds[j];
            }
            SearchCriteria<ObjectStoreVO> sc = osSearch.create();
            sc.setParameters("idIN", ids);
            List<ObjectStoreVO> stores = searchIncludingRemoved(sc, null, null, false);
            if (stores != null) {
                osList.addAll(stores);
            }
        }
        return osList;
    }

    @Override
    public ObjectStoreResponse newObjectStoreResponse(ObjectStoreVO store) {
        ObjectStoreResponse osResponse = new ObjectStoreResponse();
        osResponse.setId(store.getUuid());
        osResponse.setName(store.getName());
        osResponse.setProviderName(store.getProviderName());
        String url = store.getUrl();
        osResponse.setUrl(url);
        osResponse.setObjectName("objectstore");
        return osResponse;
    }

    @Override
    public ObjectStoreResponse setObjectStoreResponse(ObjectStoreResponse storeData, ObjectStoreVO store) {
        return storeData;
    }

    @Override
    public Integer countAllObjectStores() {
        SearchCriteria<ObjectStoreVO> sc = createSearchCriteria();
        return getCount(sc);
    }
}

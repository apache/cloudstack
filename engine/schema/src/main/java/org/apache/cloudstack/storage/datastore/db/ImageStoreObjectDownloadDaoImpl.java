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
import org.springframework.stereotype.Component;

import javax.naming.ConfigurationException;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Component
public class ImageStoreObjectDownloadDaoImpl extends GenericDaoBase<ImageStoreObjectDownloadVO, Long> implements ImageStoreObjectDownloadDao {
    private SearchBuilder<ImageStoreObjectDownloadVO> storeIdPathSearch;

    private SearchBuilder<ImageStoreObjectDownloadVO> createdSearch;

    public ImageStoreObjectDownloadDaoImpl() {
    }
    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);

        storeIdPathSearch = createSearchBuilder();
        storeIdPathSearch.and("store_id", storeIdPathSearch.entity().getStoreId(), SearchCriteria.Op.EQ);
        storeIdPathSearch.and("path", storeIdPathSearch.entity().getPath(), SearchCriteria.Op.EQ);
        storeIdPathSearch.done();

        createdSearch = createSearchBuilder();
        createdSearch.and("created", createdSearch.entity().getCreated(), SearchCriteria.Op.LTEQ);
        createdSearch.done();

        return true;
    }

    @Override
    public ImageStoreObjectDownloadVO findByStoreIdAndPath(long storeId, String path) {
        SearchCriteria<ImageStoreObjectDownloadVO> sc = storeIdPathSearch.create();
        sc.setParameters("store_id", storeId);
        sc.setParameters("path", path);
        return findOneBy(sc);
    }

    @Override
    public List<ImageStoreObjectDownloadVO> listToExpire(Date date) {
        SearchCriteria<ImageStoreObjectDownloadVO> sc = createdSearch.create();
        sc.setParameters("created", date);
        return listBy(sc);
    }
}

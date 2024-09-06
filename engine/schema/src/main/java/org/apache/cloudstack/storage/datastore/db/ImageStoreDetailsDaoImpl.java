// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package org.apache.cloudstack.storage.datastore.db;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.ConfigKey.Scope;
import org.apache.cloudstack.framework.config.ScopedConfigStorage;
import org.apache.cloudstack.resourcedetail.ResourceDetailsDaoBase;
import org.springframework.stereotype.Component;

import com.cloud.utils.crypt.DBEncryptionUtil;
import com.cloud.utils.db.QueryBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.db.TransactionLegacy;

@Component
public class ImageStoreDetailsDaoImpl extends ResourceDetailsDaoBase<ImageStoreDetailVO> implements ImageStoreDetailsDao, ScopedConfigStorage {

    protected final SearchBuilder<ImageStoreDetailVO> storeSearch;

    public ImageStoreDetailsDaoImpl() {
        super();
        storeSearch = createSearchBuilder();
        storeSearch.and("store", storeSearch.entity().getResourceId(), SearchCriteria.Op.EQ);
        storeSearch.done();
    }

    @Override
    public void update(long storeId, Map<String, String> details) {
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        SearchCriteria<ImageStoreDetailVO> sc = storeSearch.create();
        sc.setParameters("store", storeId);

        txn.start();
        expunge(sc);
        for (Map.Entry<String, String> entry : details.entrySet()) {
            ImageStoreDetailVO detail = new ImageStoreDetailVO(storeId, entry.getKey(), entry.getValue(), true);
            persist(detail);
        }
        txn.commit();
    }

    @Override
    public Map<String, String> getDetails(long storeId) {
        SearchCriteria<ImageStoreDetailVO> sc = storeSearch.create();
        sc.setParameters("store", storeId);

        List<ImageStoreDetailVO> details = listBy(sc);
        Map<String, String> detailsMap = new HashMap<String, String>();
        for (ImageStoreDetailVO detail : details) {
            String name = detail.getName();
            String value = detail.getValue();
            if (name.equals(ApiConstants.KEY) || name.equals(ApiConstants.S3_SECRET_KEY)) {
                value = DBEncryptionUtil.decrypt(value);
            }
            detailsMap.put(name, value);
        }

        return detailsMap;
    }

    @Override
    public void deleteDetails(long storeId) {
        SearchCriteria<ImageStoreDetailVO> sc = storeSearch.create();
        sc.setParameters("store", storeId);

        List<ImageStoreDetailVO> results = search(sc, null);
        for (ImageStoreDetailVO result : results) {
            remove(result.getId());
        }
    }

    @Override
    public Scope getScope() {
        return ConfigKey.Scope.ImageStore;
    }

    @Override
    public ImageStoreDetailVO findDetail(long storeId, String name) {
        QueryBuilder<ImageStoreDetailVO> sc = QueryBuilder.create(ImageStoreDetailVO.class);
        sc.and(sc.entity().getResourceId(), Op.EQ, storeId);
        sc.and(sc.entity().getName(), Op.EQ, name);
        return sc.find();
    }

    @Override
    public String getConfigValue(long id, String key) {
        ImageStoreDetailVO vo = findDetail(id, key);
        return vo == null ? null : vo.getValue();
    }

    @Override
    public void addDetail(long resourceId, String key, String value, boolean display) {
        super.addDetail(new ImageStoreDetailVO(resourceId, key, value, display));
    }

}

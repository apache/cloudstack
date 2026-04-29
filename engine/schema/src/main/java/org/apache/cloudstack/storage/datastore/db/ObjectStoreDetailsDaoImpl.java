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

import com.cloud.utils.crypt.DBEncryptionUtil;
import com.cloud.utils.db.QueryBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.db.TransactionLegacy;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.resourcedetail.ResourceDetailsDaoBase;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ObjectStoreDetailsDaoImpl extends ResourceDetailsDaoBase<ObjectStoreDetailVO> implements ObjectStoreDetailsDao {

    protected final SearchBuilder<ObjectStoreDetailVO> storeSearch;

    public ObjectStoreDetailsDaoImpl() {
        super();
        storeSearch = createSearchBuilder();
        storeSearch.and("store", storeSearch.entity().getResourceId(), Op.EQ);
        storeSearch.done();
    }

    @Override
    public void update(long storeId, Map<String, String> details) {
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        SearchCriteria<ObjectStoreDetailVO> sc = storeSearch.create();
        sc.setParameters("store", storeId);

        txn.start();
        expunge(sc);
        for (Map.Entry<String, String> entry : details.entrySet()) {
            ObjectStoreDetailVO detail = new ObjectStoreDetailVO(storeId, entry.getKey(), entry.getValue());
            persist(detail);
        }
        txn.commit();
    }

    @Override
    public Map<String, String> getDetails(long storeId) {
        SearchCriteria<ObjectStoreDetailVO> sc = storeSearch.create();
        sc.setParameters("store", storeId);

        List<ObjectStoreDetailVO> details = listBy(sc);
        Map<String, String> detailsMap = new HashMap<String, String>();
        for (ObjectStoreDetailVO detail : details) {
            String name = detail.getName();
            String value = detail.getValue();
            if (name.equals(ApiConstants.KEY)) {
                value = DBEncryptionUtil.decrypt(value);
            }
            detailsMap.put(name, value);
        }

        return detailsMap;
    }

    @Override
    public void deleteDetails(long storeId) {
        SearchCriteria<ObjectStoreDetailVO> sc = storeSearch.create();
        sc.setParameters("store", storeId);

        List<ObjectStoreDetailVO> results = search(sc, null);
        for (ObjectStoreDetailVO result : results) {
            remove(result.getId());
        }
    }

    @Override
    public ObjectStoreDetailVO findDetail(long storeId, String name) {
        QueryBuilder<ObjectStoreDetailVO> sc = QueryBuilder.create(ObjectStoreDetailVO.class);
        sc.and(sc.entity().getResourceId(), Op.EQ, storeId);
        sc.and(sc.entity().getName(), Op.EQ, name);
        return sc.find();
    }

    @Override
    public void addDetail(long resourceId, String key, String value, boolean display) {
        // ToDo: Add Display
        super.addDetail(new ObjectStoreDetailVO(resourceId, key, value));
    }

}

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
package com.cloud.storage.dao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.Local;

import org.apache.cloudstack.storage.datastore.db.ImageDataStoreDetailVO;
import org.apache.cloudstack.storage.datastore.db.ImageDataStoreDetailsDao;
import org.springframework.stereotype.Component;

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;

@Component
@Local(value=ImageDataStoreDetailsDao.class)
public class ImageDataStoreDaoImpl extends GenericDaoBase<ImageDataStoreDetailVO, Long> implements ImageDataStoreDetailsDao {

    protected final SearchBuilder<ImageDataStoreDetailVO> storeSearch;

    protected ImageDataStoreDaoImpl() {
        super();
        storeSearch = createSearchBuilder();
        storeSearch.and("store", storeSearch.entity().getStoreId(), SearchCriteria.Op.EQ);
        storeSearch.done();
    }

    @Override
    public void update(long storeId, Map<String, String> details) {
        Transaction txn = Transaction.currentTxn();
        SearchCriteria<ImageDataStoreDetailVO> sc = storeSearch.create();
        sc.setParameters("store", storeId);

        txn.start();
        expunge(sc);
        for (Map.Entry<String, String> entry : details.entrySet()) {
            ImageDataStoreDetailVO detail = new ImageDataStoreDetailVO(storeId, entry.getKey(), entry.getValue());
            persist(detail);
        }
        txn.commit();
    }

    @Override
    public Map<String, String> getDetails(long storeId) {
    	SearchCriteria<ImageDataStoreDetailVO> sc = storeSearch.create();
    	sc.setParameters("store", storeId);

    	List<ImageDataStoreDetailVO> details = listBy(sc);
    	Map<String, String> detailsMap = new HashMap<String, String>();
    	for (ImageDataStoreDetailVO detail : details) {
    		detailsMap.put(detail.getName(), detail.getValue());
    	}

    	return detailsMap;
    }
}

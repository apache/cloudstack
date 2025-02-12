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

import java.util.ArrayList;
import java.util.List;

import com.cloud.storage.StoragePoolAndAccessGroupMapVO;

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.TransactionLegacy;

public class StoragePoolAndAccessGroupMapDaoImpl extends GenericDaoBase<StoragePoolAndAccessGroupMapVO, Long> implements StoragePoolAndAccessGroupMapDao {

    protected final SearchBuilder<StoragePoolAndAccessGroupMapVO> StoragePoolAccessGroupSearch;

    public StoragePoolAndAccessGroupMapDaoImpl() {
        StoragePoolAccessGroupSearch = createSearchBuilder();
        StoragePoolAccessGroupSearch.and("poolId", StoragePoolAccessGroupSearch.entity().getPoolId(), SearchCriteria.Op.EQ);
        StoragePoolAccessGroupSearch.done();
    }

    @Override
    public void persist(long poolId, List<String> storageAccessGroups) {
        TransactionLegacy txn = TransactionLegacy.currentTxn();

        txn.start();
        SearchCriteria<StoragePoolAndAccessGroupMapVO> sc = StoragePoolAccessGroupSearch.create();
        sc.setParameters("poolId", poolId);
        expunge(sc);

        for (String sag : storageAccessGroups) {
            sag = sag.trim();
            if (sag.length() > 0) {
                StoragePoolAndAccessGroupMapVO vo = new StoragePoolAndAccessGroupMapVO(poolId, sag);
                persist(vo);
            }
        }
        txn.commit();
    }

    @Override
    public List<String> getStorageAccessGroups(long poolId) {
        SearchCriteria<StoragePoolAndAccessGroupMapVO> sc = StoragePoolAccessGroupSearch.create();
        sc.setParameters("poolId", poolId);

        List<StoragePoolAndAccessGroupMapVO> results = search(sc, null);
        List<String> storagePoolAccessGroups = new ArrayList<String>(results.size());
        for (StoragePoolAndAccessGroupMapVO result : results) {
            storagePoolAccessGroups.add(result.getStorageAccessGroup());
        }

        return storagePoolAccessGroups;
    }

    @Override
    public void deleteStorageAccessGroups(long poolId) {
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        txn.start();
        SearchCriteria<StoragePoolAndAccessGroupMapVO> sc = StoragePoolAccessGroupSearch.create();
        sc.setParameters("poolId", poolId);
        expunge(sc);
        txn.commit();
    }

    @Override
    public List<String> listDistinctStorageAccessGroups(String name, String keyword) {
        GenericSearchBuilder<StoragePoolAndAccessGroupMapVO, String> searchBuilder = createSearchBuilder(String.class);

        searchBuilder.select(null, SearchCriteria.Func.DISTINCT, searchBuilder.entity().getStorageAccessGroup());
        searchBuilder.and("name", searchBuilder.entity().getStorageAccessGroup(), SearchCriteria.Op.EQ);
        searchBuilder.and("keyword", searchBuilder.entity().getStorageAccessGroup(), SearchCriteria.Op.LIKE);
        searchBuilder.done();

        SearchCriteria<String> sc = searchBuilder.create();

        if (name != null) {
            sc.setParameters("name", name);
        }

        if (keyword != null) {
            sc.setParameters("keyword", "%" + keyword + "%");
        }

        return customSearch(sc, null);
    }

}

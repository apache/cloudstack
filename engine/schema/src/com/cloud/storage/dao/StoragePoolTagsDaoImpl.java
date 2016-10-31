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
import com.cloud.storage.StoragePoolTagVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.TransactionLegacy;

public class StoragePoolTagsDaoImpl extends GenericDaoBase<StoragePoolTagVO, Long> implements StoragePoolTagsDao {

    protected final SearchBuilder<StoragePoolTagVO> StoragePoolSearch;

    public StoragePoolTagsDaoImpl() {
        StoragePoolSearch = createSearchBuilder();
        StoragePoolSearch.and("poolId", StoragePoolSearch.entity().getPoolId(), SearchCriteria.Op.EQ);
        StoragePoolSearch.done();
    }

    @Override
    public void persist(long poolId, List<String> storagePoolTags) {
        TransactionLegacy txn = TransactionLegacy.currentTxn();

        txn.start();
        SearchCriteria<StoragePoolTagVO> sc = StoragePoolSearch.create();
        sc.setParameters("poolId", poolId);
        expunge(sc);

        for (String tag : storagePoolTags) {
            tag = tag.trim();
            if (tag.length() > 0) {
                StoragePoolTagVO vo = new StoragePoolTagVO(poolId, tag);
                persist(vo);
            }
        }
        txn.commit();
    }

    @Override
    public List<String> getStoragePoolTags(long poolId) {
        SearchCriteria<StoragePoolTagVO> sc = StoragePoolSearch.create();
        sc.setParameters("poolId", poolId);

        List<StoragePoolTagVO> results = search(sc, null);
        List<String> storagePoolTags = new ArrayList<String>(results.size());
        for (StoragePoolTagVO result : results) {
            storagePoolTags.add(result.getTag());
        }

        return storagePoolTags;
    }

    @Override
    public void deleteTags(long poolId) {
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        txn.start();
        SearchCriteria<StoragePoolTagVO> sc = StoragePoolSearch.create();
        sc.setParameters("poolId", poolId);
        expunge(sc);
        txn.commit();
    }

}

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
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.cloudstack.api.response.StorageTagResponse;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;

import com.cloud.storage.StoragePoolTagVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallbackNoReturn;
import com.cloud.utils.db.TransactionLegacy;
import com.cloud.utils.db.TransactionStatus;

public class StoragePoolTagsDaoImpl extends GenericDaoBase<StoragePoolTagVO, Long> implements StoragePoolTagsDao {

    @Inject
    private ConfigurationDao _configDao;

    protected final SearchBuilder<StoragePoolTagVO> StoragePoolSearch;
    private final SearchBuilder<StoragePoolTagVO> StoragePoolIdsSearch;

    private static final int DEFAULT_BATCH_QUERY_SIZE = 2000;

    public StoragePoolTagsDaoImpl() {
        StoragePoolSearch = createSearchBuilder();
        StoragePoolSearch.and("poolId", StoragePoolSearch.entity().getPoolId(), SearchCriteria.Op.EQ);
        StoragePoolSearch.done();
        StoragePoolIdsSearch = createSearchBuilder();
        StoragePoolIdsSearch.and("idIN", StoragePoolIdsSearch.entity().getId(), SearchCriteria.Op.IN);
        StoragePoolIdsSearch.done();
    }

    @Override
    public void persist(long poolId, List<String> storagePoolTags, Boolean isTagARule) {
        TransactionLegacy txn = TransactionLegacy.currentTxn();

        txn.start();
        SearchCriteria<StoragePoolTagVO> sc = StoragePoolSearch.create();
        sc.setParameters("poolId", poolId);
        expunge(sc);

        for (String tag : storagePoolTags) {
            tag = tag.trim();
            if (tag.length() > 0) {
                StoragePoolTagVO vo = new StoragePoolTagVO(poolId, tag, isTagARule);
                persist(vo);
            }
        }
        txn.commit();
    }

    public void persist(List<StoragePoolTagVO> storagePoolTags) {
        Transaction.execute(TransactionLegacy.CLOUD_DB, new TransactionCallbackNoReturn() {
            @Override public void doInTransactionWithoutResult(TransactionStatus status) {
                for (StoragePoolTagVO storagePoolTagVO : storagePoolTags) {
                    persist(storagePoolTagVO);
                }
            }
        });
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

    @Override
    public List<StoragePoolTagVO> searchByIds(Long... stIds) {
        final int detailsBatchSize = getDetailsBatchSize();

        // query details by batches
        List<StoragePoolTagVO> uvList = new ArrayList<StoragePoolTagVO>();
        int curr_index = 0;

        while ((curr_index + detailsBatchSize) <= stIds.length) {
            searchForStoragePoolIdsInternal(curr_index, detailsBatchSize, stIds, uvList);
            curr_index += detailsBatchSize;
        }

        if (curr_index < stIds.length) {
            int batch_size = (stIds.length - curr_index);
            searchForStoragePoolIdsInternal(curr_index, batch_size, stIds, uvList);
        }

        return uvList;
    }

    /**
     * Search for storage pools based on their IDs.
     * The search is executed in batch, this means that we will load a batch of size {@link StoragePoolTagsDaoImpl#getDetailsBatchSize()}
     * {@link StoragePoolTagVO} at each time.
     * The loaded storage pools are added in the pools parameter.
     * @param currIndex current index
     * @param batchSize batch size
     * @param stIds storage tags array
     * @param pools list in which storage pools are added
     */
    protected void searchForStoragePoolIdsInternal(int currIndex, int batchSize, Long[] stIds, List<StoragePoolTagVO> pools) {
        Long[] ids = new Long[batchSize];
        for (int k = 0, j = currIndex; j < currIndex + batchSize; j++, k++) {
            ids[k] = stIds[j];
        }
        SearchCriteria<StoragePoolTagVO> sc = StoragePoolIdsSearch.create();
        sc.setParameters("idIN", (Object[])ids);
        List<StoragePoolTagVO> vms = searchIncludingRemoved(sc, null, null, false);
        if (vms != null) {
            pools.addAll(vms);
        }
    }

    /**
     * Retrieve {@code detail.batch.query.size} configuration value. If not available, return default value {@link StoragePoolTagsDaoImpl#DEFAULT_BATCH_QUERY_SIZE}
     * @return detail.batch.query.size configuration value
     */
    protected int getDetailsBatchSize() {
        String batchCfg = _configDao.getValue("detail.batch.query.size");
        return batchCfg != null ? Integer.parseInt(batchCfg) : DEFAULT_BATCH_QUERY_SIZE;
    }

    @Override
    public StorageTagResponse newStorageTagResponse(StoragePoolTagVO tag) {
        StorageTagResponse tagResponse = new StorageTagResponse();

        tagResponse.setName(tag.getTag());
        tagResponse.setPoolId(tag.getPoolId());

        tagResponse.setObjectName("storagetag");

        return tagResponse;
    }

    @Override
    public List<StoragePoolTagVO> findStoragePoolTags(long poolId) {
        SearchCriteria<StoragePoolTagVO> sc = StoragePoolSearch.create();
        sc.setParameters("poolId", poolId);

        return search(sc, null);
    }

    @Override
    public List<Long> listPoolIdsByTag(String tag) {
        SearchBuilder<StoragePoolTagVO> sb = createSearchBuilder();
        sb.and("tag", sb.entity().getTag(), SearchCriteria.Op.EQ);
        sb.done();
        SearchCriteria<StoragePoolTagVO> sc = sb.create();
        sc.setParameters("tag", tag);
        List<StoragePoolTagVO> poolRefs = search(sc, null);
        return poolRefs.stream().map(StoragePoolTagVO::getPoolId).collect(Collectors.toList());
    }

}

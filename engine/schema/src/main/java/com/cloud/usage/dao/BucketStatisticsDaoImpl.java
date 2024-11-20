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
package com.cloud.usage.dao;

import com.cloud.usage.BucketStatisticsVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BucketStatisticsDaoImpl extends GenericDaoBase<BucketStatisticsVO, Long> implements BucketStatisticsDao {
    private final SearchBuilder<BucketStatisticsVO> AllFieldsSearch;
    private final SearchBuilder<BucketStatisticsVO> AccountSearch;

    public BucketStatisticsDaoImpl() {
        AccountSearch = createSearchBuilder();
        AccountSearch.and("account", AccountSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        AccountSearch.done();

        AllFieldsSearch = createSearchBuilder();
        AllFieldsSearch.and("account", AllFieldsSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("bucket", AllFieldsSearch.entity().getBucketId(), SearchCriteria.Op.EQ);
        AllFieldsSearch.done();
    }

    @Override
    public BucketStatisticsVO findBy(long accountId, long bucketId) {
        SearchCriteria<BucketStatisticsVO> sc = AllFieldsSearch.create();
        sc.setParameters("account", accountId);
        sc.setParameters("bucket", bucketId);
        return findOneBy(sc);
    }

    @Override
    public BucketStatisticsVO lock(long accountId, long bucketId) {
        SearchCriteria<BucketStatisticsVO> sc = AllFieldsSearch.create();
        sc.setParameters("account", accountId);
        sc.setParameters("bucket", bucketId);
        return lockOneRandomRow(sc, true);
    }

    @Override
    public List<BucketStatisticsVO> listBy(long accountId) {
        SearchCriteria<BucketStatisticsVO> sc = AccountSearch.create();
        sc.setParameters("account", accountId);
        return search(sc, null);
    }
}

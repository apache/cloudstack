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

import com.cloud.storage.BucketCredentialVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import org.springframework.stereotype.Component;

import javax.naming.ConfigurationException;
import java.util.List;
import java.util.Map;

@Component
public class BucketCredentialDaoImpl extends GenericDaoBase<BucketCredentialVO, Long> implements BucketCredentialDao {

    private SearchBuilder<BucketCredentialVO> bucketIdSearch;
    private GenericSearchBuilder<BucketCredentialVO, Long> allBucketIdsSearch;

    private static final String BUCKET_ID = "bucket_id";

    protected BucketCredentialDaoImpl() {
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);

        bucketIdSearch = createSearchBuilder();
        bucketIdSearch.and(BUCKET_ID, bucketIdSearch.entity().getBucketId(), SearchCriteria.Op.EQ);
        bucketIdSearch.done();

        allBucketIdsSearch = createSearchBuilder(Long.class);
        allBucketIdsSearch.selectFields(allBucketIdsSearch.entity().getBucketId());
        allBucketIdsSearch.done();

        return true;
    }

    @Override
    public List<Long> listBucketIdsWithCredential() {
        return customSearch(allBucketIdsSearch.create(), null);
    }

    @Override
    public BucketCredentialVO findByBucketId(long bucketId) {
        SearchCriteria<BucketCredentialVO> sc = bucketIdSearch.create();
        sc.setParameters(BUCKET_ID, bucketId);
        return findOneBy(sc);
    }
}

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

import com.cloud.storage.BucketCredentialKeyVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import org.springframework.stereotype.Component;

import javax.naming.ConfigurationException;
import java.util.List;
import java.util.Map;

@Component
public class BucketCredentialKeyDaoImpl extends GenericDaoBase<BucketCredentialKeyVO, Long> implements BucketCredentialKeyDao {

    private SearchBuilder<BucketCredentialKeyVO> credentialIdSearch;
    private SearchBuilder<BucketCredentialKeyVO> credentialIdSlotSearch;

    private static final String CREDENTIAL_ID = "bucket_credential_id";
    private static final String KEY_SLOT = "key_slot";

    protected BucketCredentialKeyDaoImpl() {
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);

        credentialIdSearch = createSearchBuilder();
        credentialIdSearch.and(CREDENTIAL_ID, credentialIdSearch.entity().getBucketCredentialId(), SearchCriteria.Op.EQ);
        credentialIdSearch.done();

        credentialIdSlotSearch = createSearchBuilder();
        credentialIdSlotSearch.and(CREDENTIAL_ID, credentialIdSlotSearch.entity().getBucketCredentialId(), SearchCriteria.Op.EQ);
        credentialIdSlotSearch.and(KEY_SLOT, credentialIdSlotSearch.entity().getKeySlot(), SearchCriteria.Op.EQ);
        credentialIdSlotSearch.done();

        return true;
    }

    @Override
    public List<BucketCredentialKeyVO> listByCredentialId(long bucketCredentialId) {
        SearchCriteria<BucketCredentialKeyVO> sc = credentialIdSearch.create();
        sc.setParameters(CREDENTIAL_ID, bucketCredentialId);
        return listBy(sc);
    }

    @Override
    public BucketCredentialKeyVO findByCredentialIdAndSlot(long bucketCredentialId, int keySlot) {
        SearchCriteria<BucketCredentialKeyVO> sc = credentialIdSlotSearch.create();
        sc.setParameters(CREDENTIAL_ID, bucketCredentialId);
        sc.setParameters(KEY_SLOT, keySlot);
        return findOneBy(sc);
    }
}

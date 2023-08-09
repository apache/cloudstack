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

import com.cloud.storage.BucketVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import org.apache.cloudstack.api.response.BucketResponse;
import org.apache.cloudstack.storage.datastore.db.ObjectStoreDao;
import org.apache.cloudstack.storage.datastore.db.ObjectStoreVO;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.naming.ConfigurationException;
import java.util.List;
import java.util.Map;

@Component
public class BucketDaoImpl extends GenericDaoBase<BucketVO, Long> implements BucketDao {
    public static final Logger s_logger = Logger.getLogger(BucketDaoImpl.class.getName());
    private SearchBuilder<BucketVO> searchFilteringStoreId;

    private static final String STORE_ID = "store_id";
    private static final String STATE = "state";
    private static final String ACCOUNT_ID = "account_id";

    @Inject
    ObjectStoreDao _objectStoreDao;

    protected BucketDaoImpl() {

    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);

        searchFilteringStoreId = createSearchBuilder();
        searchFilteringStoreId.and(STORE_ID, searchFilteringStoreId.entity().getObjectStoreId(), SearchCriteria.Op.EQ);
        searchFilteringStoreId.and(ACCOUNT_ID, searchFilteringStoreId.entity().getAccountId(), SearchCriteria.Op.EQ);
        searchFilteringStoreId.and(STATE, searchFilteringStoreId.entity().getState(), SearchCriteria.Op.NEQ);
        searchFilteringStoreId.done();

        return true;
    }
    @Override
    public List<BucketVO> listByObjectStoreId(long objectStoreId) {
        SearchCriteria<BucketVO> sc = searchFilteringStoreId.create();
        sc.setParameters(STORE_ID, objectStoreId);
        sc.setParameters(STATE, BucketVO.State.Destroyed);
        return listBy(sc);
    }

    @Override
    public List<BucketVO> listByObjectStoreIdAndAccountId(long objectStoreId, long accountId) {
        SearchCriteria<BucketVO> sc = searchFilteringStoreId.create();
        sc.setParameters(STORE_ID, objectStoreId);
        sc.setParameters(ACCOUNT_ID, accountId);
        sc.setParameters(STATE, BucketVO.State.Destroyed);
        return listBy(sc);
    }

    @Override
    public BucketResponse newBucketResponse(BucketVO bucket) {
        BucketResponse bucketResponse = new BucketResponse();
        bucketResponse.setName(bucket.getName());
        bucketResponse.setId(bucket.getUuid());
        bucketResponse.setCreated(bucket.getCreated());
        bucketResponse.setSize(bucket.getSize());
        if(bucket.getQuota() != null) {
            bucketResponse.setQuota(bucket.getQuota());
        }
        bucketResponse.setVersioning(bucket.isVersioning());
        bucketResponse.setEncryption(bucket.isEncryption());
        bucketResponse.setObjectLock(bucket.isObjectLock());
        bucketResponse.setPolicy(bucket.getPolicy());
        bucketResponse.setBucketURL(bucket.getBucketURL());
        bucketResponse.setAccessKey(bucket.getAccessKey());
        bucketResponse.setSecretKey(bucket.getSecretKey());
        ObjectStoreVO objectStoreVO = _objectStoreDao.findById(bucket.getObjectStoreId());
        bucketResponse.setObjectStoragePoolId(objectStoreVO.getUuid());
        bucketResponse.setObjectName("bucket");
        return bucketResponse;
    }
}

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
package com.cloud.bridge.persist.dao;


import javax.ejb.Local;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.bridge.model.BucketPolicyVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.TransactionLegacy;

@Component
@Local(value={BucketPolicyDao.class})
public class BucketPolicyDaoImpl extends GenericDaoBase<BucketPolicyVO, Long> implements BucketPolicyDao{
    public static final Logger logger = Logger.getLogger(BucketPolicyDaoImpl.class);
    public BucketPolicyDaoImpl(){ }

    /**
     * Since a bucket policy can exist before its bucket we also need to keep the policy's owner
     * so we can restrict who modifies it (because of the "s3:CreateBucket" action).
     */
    @Override
    public BucketPolicyVO getByName( String bucketName ) {
        SearchBuilder <BucketPolicyVO> searchByBucket = createSearchBuilder();
        searchByBucket.and("BucketName", searchByBucket.entity().getBucketName(), SearchCriteria.Op.EQ);
        TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.AWSAPI_DB);
        try {
            txn.start();
            SearchCriteria<BucketPolicyVO> sc = searchByBucket.create();
            sc.setParameters("BucketName", bucketName);
            return findOneBy(sc);

        }finally {
            txn.close();
        }

    }

    @Override
    public void deletePolicy( String bucketName ) {
        SearchBuilder <BucketPolicyVO> deleteByBucket = createSearchBuilder();
        deleteByBucket.and("BucketName", deleteByBucket.entity().getBucketName(), SearchCriteria.Op.EQ);
        TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.AWSAPI_DB);
        try {
            txn.start();
            SearchCriteria<BucketPolicyVO> sc = deleteByBucket.create();
            sc.setParameters("BucketName", bucketName);
            remove(sc);

        }finally {
            txn.close();
        }

    }
}

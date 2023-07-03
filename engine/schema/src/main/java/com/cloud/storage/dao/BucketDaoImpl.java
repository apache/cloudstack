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
import org.apache.cloudstack.api.response.BucketResponse;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BucketDaoImpl extends GenericDaoBase<BucketVO, Long> implements BucketDao {
    public static final Logger s_logger = Logger.getLogger(BucketDaoImpl.class.getName());

    public BucketDaoImpl() {

    }
    @Override
    public List<BucketVO> listByObjectStoreId(long objectStoreId) {
        return null;
    }

    @Override
    public BucketResponse newBucketResponse(BucketVO bucket) {
        BucketResponse bucketResponse = new BucketResponse();
        bucketResponse.setName(bucket.getName());
        bucketResponse.setId(bucket.getUuid());
        //ToDo change to UUID
        bucketResponse.setObjectStoragePoolId(""+bucket.getObjectStoreId());
        bucketResponse.setObjectName("bucket");
        return bucketResponse;
    }
}
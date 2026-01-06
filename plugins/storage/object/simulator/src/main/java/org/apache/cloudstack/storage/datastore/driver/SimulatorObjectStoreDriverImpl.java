/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cloudstack.storage.datastore.driver;

import com.amazonaws.services.s3.model.AccessControlList;
import com.amazonaws.services.s3.model.BucketPolicy;
import com.cloud.agent.api.to.BucketTO;
import com.cloud.agent.api.to.DataStoreTO;
import org.apache.cloudstack.storage.object.Bucket;
import com.cloud.storage.BucketVO;
import com.cloud.storage.dao.BucketDao;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.storage.datastore.db.ObjectStoreDao;
import org.apache.cloudstack.storage.datastore.db.ObjectStoreVO;
import org.apache.cloudstack.storage.object.BaseObjectStoreDriverImpl;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SimulatorObjectStoreDriverImpl extends BaseObjectStoreDriverImpl {

    @Inject
    ObjectStoreDao _storeDao;

    @Inject
    BucketDao _bucketDao;

    private static final String ACCESS_KEY = "accesskey";
    private static final String SECRET_KEY = "secretkey";

    @Override
    public DataStoreTO getStoreTO(DataStore store) {
        return null;
    }

    @Override
    public Bucket createBucket(Bucket bucket, boolean objectLock) {
        String bucketName = bucket.getName();
        long storeId = bucket.getObjectStoreId();
        ObjectStoreVO store = _storeDao.findById(storeId);
        BucketVO bucketVO = _bucketDao.findById(bucket.getId());
        bucketVO.setAccessKey(ACCESS_KEY);
        bucketVO.setSecretKey(SECRET_KEY);
        bucketVO.setBucketURL(store.getUrl()+"/"+bucketName);
        _bucketDao.update(bucket.getId(), bucketVO);
        return bucket;
    }

    @Override
    public List<Bucket> listBuckets(long storeId) {
        List<Bucket> bucketsList = new ArrayList<>();
        return bucketsList;
    }

    @Override
    public boolean deleteBucket(BucketTO bucket, long storeId) {
        return true;
    }

    @Override
    public AccessControlList getBucketAcl(BucketTO bucket, long storeId) {
        return null;
    }

    @Override
    public void setBucketAcl(BucketTO bucket, AccessControlList acl, long storeId) {

    }

    @Override
    public void setBucketPolicy(BucketTO bucket, String policy, long storeId) {

    }

    @Override
    public BucketPolicy getBucketPolicy(BucketTO bucket, long storeId) {
        return null;
    }

    @Override
    public void deleteBucketPolicy(BucketTO bucket, long storeId) {

    }

    @Override
    public boolean createUser(long accountId, long storeId) {
        return true;
    }

    @Override
    public boolean setBucketEncryption(BucketTO bucket, long storeId) {
        return true;
    }

    @Override
    public boolean deleteBucketEncryption(BucketTO bucket, long storeId) {
        return true;
    }

    @Override
    public boolean setBucketVersioning(BucketTO bucket, long storeId) {
        return true;
    }

    @Override
    public boolean deleteBucketVersioning(BucketTO bucket, long storeId) {
        return true;
    }

    @Override
    public void setBucketQuota(BucketTO bucket, long storeId, long size) {

    }

    @Override
    public Map<String, Long> getAllBucketsUsage(long storeId) {
        return new HashMap<String, Long>();
    }
}

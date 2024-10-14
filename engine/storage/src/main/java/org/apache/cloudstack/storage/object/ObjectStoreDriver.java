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
package org.apache.cloudstack.storage.object;

import com.amazonaws.services.s3.model.AccessControlList;
import com.amazonaws.services.s3.model.BucketPolicy;
import com.cloud.agent.api.to.BucketTO;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreDriver;

import java.util.List;
import java.util.Map;

public interface ObjectStoreDriver extends DataStoreDriver {
    Bucket createBucket(Bucket bucket, boolean objectLock);

    List<Bucket> listBuckets(long storeId);

    boolean deleteBucket(BucketTO bucket, long storeId);

    AccessControlList getBucketAcl(BucketTO bucket, long storeId);

    void setBucketAcl(BucketTO bucket, AccessControlList acl, long storeId);

    void setBucketPolicy(BucketTO bucket, String policyType, long storeId);

    BucketPolicy getBucketPolicy(BucketTO bucket, long storeId);

    void deleteBucketPolicy(BucketTO bucket, long storeId);

    boolean createUser(long accountId, long storeId);

    boolean setBucketEncryption(BucketTO bucket, long storeId);

    boolean deleteBucketEncryption(BucketTO bucket, long storeId);


    boolean setBucketVersioning(BucketTO bucket, long storeId);

    boolean deleteBucketVersioning(BucketTO bucket, long storeId);

    void setBucketQuota(BucketTO bucket, long storeId, long size);

    Map<String, Long> getAllBucketsUsage(long storeId);
}

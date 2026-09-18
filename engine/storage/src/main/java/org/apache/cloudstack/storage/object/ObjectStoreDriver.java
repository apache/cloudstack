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
import com.cloud.agent.api.to.BucketCredentialTO;
import com.cloud.agent.api.to.BucketKeyTO;
import com.cloud.agent.api.to.BucketTO;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreDriver;

import java.util.List;
import java.util.Map;
import java.util.Set;

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

    /**
     * Whether this store can provision a dedicated credential per bucket at all
     * (backend capability, independent of any account).
     */
    boolean supportsBucketCredentials(long storeId);

    /**
     * Why this store cannot provide per-bucket credentials, for an administrator who has just
     * been refused; {@code null} when it can, or when the provider has nothing to add beyond
     * "this provider does not offer them".
     */
    default String bucketCredentialsUnsupportedReason(long storeId) {
        return null;
    }

    /**
     * Whether buckets of the given account can be provisioned with dedicated
     * credentials right now: the store supports them and the account's backend
     * identity is in the required state (for example, already migrated to a
     * backend account container where the provider needs one).
     */
    boolean accountSupportsBucketCredentials(long accountId, long storeId);

    /**
     * Explicit, administrator-triggered migration of an account's backend
     * identity into the state required for per-bucket credentials. May be
     * irreversible on the backend; never invoked implicitly. Idempotent.
     */
    boolean migrateAccountForBucketCredentials(long accountId, long storeId);

    /**
     * Provision a dedicated backend identity for the bucket, granted access to
     * that bucket only, with its initial key pair. Idempotent on the backend.
     */
    BucketCredentialTO createBucketCredential(BucketTO bucket, long storeId);

    /**
     * Create an additional key pair on the bucket's identity and return exactly
     * the new pair. {@code knownAccessKeys} lists the access keys CloudStack
     * already tracks, for providers whose key-create call returns the full set.
     */
    BucketKeyTO createBucketCredentialKey(BucketTO bucket, long storeId, Set<String> knownAccessKeys);

    /** Remove one key pair from the bucket's identity. Key not found counts as success. */
    boolean removeBucketCredentialKey(BucketTO bucket, long storeId, String accessKey);

    /** Remove the bucket's identity and everything attached to it. Identity not found counts as success. */
    boolean deleteBucketCredential(BucketTO bucket, long storeId);

    /**
     * Replace the account-level key CloudStack uses on this store with a fresh one and revoke
     * the old one on the backend. Only valid once no bucket of the account still relies on the
     * account key. Returns the new key pair.
     */
    BucketKeyTO rotateAccountKey(long accountId, long storeId);

    /**
     * Whether the account was migrated by adopting a pre-existing identity whose key may be
     * held outside CloudStack, and that key has not been rotated away yet.
     */
    boolean isAccountKeyRotationPending(long accountId, long storeId);
}

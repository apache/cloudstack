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

import com.cloud.exception.ResourceAllocationException;
import com.cloud.user.Account;
import org.apache.cloudstack.api.command.admin.storage.MigrateObjectStoreAccountCmd;
import org.apache.cloudstack.api.command.admin.storage.RotateObjectStoreAccountKeyCmd;
import org.apache.cloudstack.api.command.user.bucket.CreateBucketCmd;
import org.apache.cloudstack.api.command.user.bucket.MigrateBucketCredentialCmd;
import org.apache.cloudstack.api.command.user.bucket.RevokeBucketKeyCmd;
import org.apache.cloudstack.api.command.user.bucket.RotateBucketKeyCmd;
import org.apache.cloudstack.api.command.user.bucket.UpdateBucketCmd;
import org.apache.cloudstack.framework.config.ConfigKey;

import java.util.List;

public interface BucketApiService {

    ConfigKey<Boolean> PerBucketCredentials = new ConfigKey<Boolean>("Advanced", Boolean.class,
            "object.storage.per.bucket.credentials",
            "true",
            "How an account is set up the first time it uses an object store that supports per-bucket credentials. When true, it is set up so that each of its buckets gets its own credential with rotatable keys. When false, it is set up to share one credential across its buckets, as in earlier releases, and an administrator can migrate it later. Accounts that have already been migrated always get per-bucket credentials, whatever this is set to.",
            true,
            ConfigKey.Scope.Global,
            null);

    ConfigKey<Long> DefaultMaxAccountBuckets = new ConfigKey<Long>("Account Defaults", Long.class,
            "max.account.buckets",
            "20",
            "The default maximum number of buckets that can be created for an account",
            false,
            ConfigKey.Scope.Global,
            null);

    ConfigKey<Long> DefaultMaxAccountObjectStorage = new ConfigKey<Long>("Account Defaults", Long.class,
            "max.account.object.storage",
            "400",
            "The default maximum object storage space (in GiB) that can be used for an account",
            false,
            ConfigKey.Scope.Global,
            null);

    ConfigKey<Long> DefaultMaxProjectBuckets = new ConfigKey<Long>("Project Defaults", Long.class,
            "max.project.buckets",
            "20",
            "The default maximum number of buckets that can be created for a project",
            false,
            ConfigKey.Scope.Global,
            null);

    ConfigKey<Long> DefaultMaxProjectObjectStorage = new ConfigKey<Long>("Project Defaults", Long.class,
            "max.project.object.storage",
            "400",
            "The default maximum object storage space (in GiB) that can be used for a project",
            false,
            ConfigKey.Scope.Global,
            null);

    ConfigKey<Long> DefaultMaxDomainBuckets = new ConfigKey<Long>("Domain Defaults", Long.class,
            "max.domain.buckets",
            "20",
            "The default maximum number of buckets that can be created for a domain",
            false,
            ConfigKey.Scope.Global,
            null);

    ConfigKey<Long> DefaultMaxDomainObjectStorage = new ConfigKey<Long>("Domain Defaults", Long.class,
            "max.domain.object.storage",
            "400",
            "The default maximum object storage space (in GiB) that can be used for a domain",
            false,
            ConfigKey.Scope.Global,
            null);

    /**
     * Creates the database object for a Bucket based on the given criteria
     *
     * @param cmd
     *            the API command wrapping the criteria (account/domainId [admin only], zone, diskOffering, snapshot,
     *            name)
     * @return the Bucket object
     */
    Bucket allocBucket(CreateBucketCmd cmd) throws ResourceAllocationException;

    /**
     * Creates the Bucket based on the given criteria
     *
     * @param cmd
     *            the API command wrapping the criteria (account/domainId [admin only], zone, diskOffering, snapshot,
     *            name)
     * @return the Bucket object
     */
    Bucket createBucket(CreateBucketCmd cmd);

    boolean deleteBucket(long bucketId, Account caller) throws ResourceAllocationException;

    boolean updateBucket(UpdateBucketCmd cmd, Account caller) throws ResourceAllocationException;

    /**
     * Create a new key pair in one slot of the bucket's dedicated credential, replacing
     * whatever the slot held. The other slot is untouched.
     */
    BucketCredentialKey rotateBucketKey(RotateBucketKeyCmd cmd, Account caller);

    /**
     * Revoke the key pair in one slot. The last active key of a credential cannot be revoked.
     */
    boolean revokeBucketKey(RevokeBucketKeyCmd cmd, Account caller);

    /**
     * Give an existing bucket that still uses the account credential a dedicated credential.
     */
    Bucket migrateBucketCredential(MigrateBucketCredentialCmd cmd, Account caller);

    /**
     * Explicitly migrate an account's identity on an object store into the state its provider
     * requires for per-bucket credentials. Irreversible on some providers.
     */
    boolean migrateObjectStoreAccount(MigrateObjectStoreAccountCmd cmd, Account caller);

    /**
     * Rotate the account-level key CloudStack holds for the account on the object store, once
     * no bucket of that account on the store still uses it. Revokes the old key.
     */
    boolean rotateObjectStoreAccountKey(RotateObjectStoreAccountKeyCmd cmd, Account caller);

    /**
     * Number of buckets of the account on the store that still use the account-level key.
     */
    long countAccountScopedBuckets(long accountId, long objectStoreId);

    /**
     * The key slots of the bucket's dedicated credential, or null if the bucket uses the
     * account credential.
     */
    List<? extends BucketCredentialKey> listBucketKeys(long bucketId);

    void getBucketUsage();
}

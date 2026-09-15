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
// SPDX-License-Identifier: Apache-2.0
package org.apache.cloudstack.storage.datastore.driver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.storage.datastore.db.ObjectStoreDao;
import org.apache.cloudstack.storage.datastore.db.ObjectStoreDetailsDao;
import org.apache.cloudstack.storage.datastore.db.ObjectStoreVO;
import org.apache.cloudstack.storage.datastore.util.SeaweedFSObjectStoreUtil;
import org.apache.cloudstack.storage.object.BaseObjectStoreDriverImpl;
import org.apache.cloudstack.storage.object.Bucket;
import org.apache.cloudstack.storage.object.BucketObject;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.model.AccessKey;
import com.amazonaws.services.identitymanagement.model.AccessKeyMetadata;
import com.amazonaws.services.identitymanagement.model.CreateAccessKeyRequest;
import com.amazonaws.services.identitymanagement.model.CreateAccessKeyResult;
import com.amazonaws.services.identitymanagement.model.CreateUserRequest;
import com.amazonaws.services.identitymanagement.model.DeleteAccessKeyRequest;
import com.amazonaws.services.identitymanagement.model.EntityAlreadyExistsException;
import com.amazonaws.services.identitymanagement.model.ListAccessKeysRequest;
import com.amazonaws.services.identitymanagement.model.PutUserPolicyRequest;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AccessControlList;
import com.amazonaws.services.s3.model.BucketPolicy;
import com.amazonaws.services.s3.model.BucketVersioningConfiguration;
import com.amazonaws.services.s3.model.CreateBucketRequest;
import com.amazonaws.services.s3.model.DeleteBucketPolicyRequest;
import com.amazonaws.services.s3.model.BucketCrossOriginConfiguration;
import com.amazonaws.services.s3.model.CORSRule;
import com.amazonaws.services.s3.model.GetBucketPolicyRequest;
import com.amazonaws.services.s3.model.SSEAlgorithm;
import com.amazonaws.services.s3.model.ServerSideEncryptionByDefault;
import com.amazonaws.services.s3.model.ServerSideEncryptionConfiguration;
import com.amazonaws.services.s3.model.ServerSideEncryptionRule;
import com.amazonaws.services.s3.model.SetBucketCrossOriginConfigurationRequest;
import com.amazonaws.services.s3.model.SetBucketEncryptionRequest;
import com.amazonaws.services.s3.model.SetBucketVersioningConfigurationRequest;
import com.cloud.agent.api.to.BucketTO;
import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.storage.BucketVO;
import com.cloud.storage.dao.BucketDao;
import com.cloud.user.Account;
import com.cloud.user.AccountDetailsDao;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.exception.CloudRuntimeException;

/**
 * SeaweedFS object store driver.
 *
 * Bucket operations use the AWS S3 SDK v1 (path-style access, endpoint-pinned).
 * User/credential management uses the AWS IAM SDK v1, since SeaweedFS exposes a
 * standard AWS IAM-compatible API. No proprietary admin client is needed.
 *
 * Modeled on CloudianHyperStoreObjectStoreDriverImpl, which uses the same
 * S3 + IAM SDK pair.
 */
public class SeaweedFSObjectStoreDriverImpl extends BaseObjectStoreDriverImpl {

    @Inject
    AccountDao _accountDao;

    @Inject
    AccountDetailsDao _accountDetailsDao;

    @Inject
    ObjectStoreDao _storeDao;

    @Inject
    BucketDao _bucketDao;

    @Inject
    ObjectStoreDetailsDao _storeDetailsDao;

    private static final String ACS_PREFIX = "acs";

    /**
     * DB-backed global lock name prefix for serializing IAM provisioning and
     * policy refreshes per store+account. Uses {@link GlobalLock} so the
     * critical section is serialized across management servers in a
     * clustered deployment, not just within a single JVM.
     */
    private static final String IAM_LOCK_PREFIX = "seaweedfs.iam.";

    private static String getIamLockName(long storeId, long accountId) {
        return IAM_LOCK_PREFIX + storeId + "." + accountId;
    }

    /**
     * Acquire a DB-backed global lock for IAM operations on the given
     * store+account. Returns a {@link GlobalLock} that the caller must
     * {@link GlobalLock#unlock()} in a {@code finally} block, or {@code null}
     * if the lock could not be acquired within the timeout.
     *
     * <p>Protected so tests can override with a no-op lock (the DB-backed
     * {@link GlobalLock} requires a real transaction context).
     */
    protected GlobalLock acquireIamLock(long storeId, long accountId) {
        GlobalLock lock = GlobalLock.getInternLock(getIamLockName(storeId, accountId));
        if (!lock.lock(300)) {
            logger.warn("Failed to acquire IAM lock for store {} account {}", storeId, accountId);
            lock.releaseRef();
            return null;
        }
        return lock;
    }

    @Override
    public DataStoreTO getStoreTO(DataStore store) {
        return null;
    }

    /**
     * Get the SeaweedFS IAM user name for the given CloudStack account and
     * store. The store ID is included so that two CloudStack pools pointing
     * at the same SeaweedFS IAM service do not collide on the same
     * {@code acs-<uuid>} user and overwrite each other's policy and access
     * keys.
     */
    protected String getUserNameForAccount(Account account, long storeId) {
        return String.format("%s-%d-%s", ACS_PREFIX, storeId, account.getUuid());
    }

    /**
     * Create the IAM user for the CloudStack account if it doesn't exist,
     * attach the restricted S3 policy, and ensure the account has a usable
     * IAM access key persisted in its account details.
     *
     * <p>If a previously stored access key is still present in IAM, it is
     * reused rather than rotated. A new key is only created when no stored
     * key exists or the stored key is no longer found in IAM; in the latter
     * case any unmanaged (leftover) keys for the user are deleted first to
     * avoid hitting IAM access-key limits. This keeps bucket records that
     * reference the stored credentials valid across repeated calls.
     *
     * @return true if the user exists or was created, false on failure.
     */
    @Override
    public boolean createUser(long accountId, long storeId) {
        Account account = _accountDao.findById(accountId);
        if (account == null) {
            logger.error("Account {} not found", accountId);
            return false;
        }
        String userName = getUserNameForAccount(account, storeId);
        AmazonIdentityManagement iamClient = getIAMClient(storeId);

        // Serialize per store+account across management servers so two
        // concurrent bucket requests do not both rotate credentials and leave
        // bucket rows with mismatched key pairs.
        GlobalLock lock = acquireIamLock(storeId, accountId);
        if (lock == null) {
            return false;
        }
        try {

        // Create the IAM user if it doesn't already exist
        try {
            iamClient.createUser(new CreateUserRequest(userName));
            logger.info("Created IAM user {} for account {}", userName, account.getAccountName());
        } catch (EntityAlreadyExistsException e) {
            logger.debug("IAM user {} already exists", userName);
        }

        // Attach a scoped IAM policy that allows access only to this
        // account's own buckets (the tenant boundary). Refreshed whenever
        // buckets are created or deleted.
        updateAccountIAMPolicy(iamClient, storeId, accountId, null);

        // Reuse the stored access key only if both the access key id and the
        // secret key are present and the key is still Active in IAM; otherwise
        // create a replacement.
        Map<String, String> details = _accountDetailsDao.findDetails(accountId);
        String accessKeyDetailKey = SeaweedFSObjectStoreUtil.keyAccessKey(storeId);
        String secretKeyDetailKey = SeaweedFSObjectStoreUtil.keySecretKey(storeId);
        String storedAccessKeyId = details.get(accessKeyDetailKey);
        String storedSecretKey = details.get(secretKeyDetailKey);
        if (storedAccessKeyId != null && storedSecretKey != null
                && iamAccessKeyExists(iamClient, userName, storedAccessKeyId)) {
            logger.debug("Reusing existing IAM access key {} for user {}", storedAccessKeyId, userName);
            return true;
        }

        // The stored key is missing, inactive, or no longer in IAM. Clean up
        // ALL keys (including the inactive stored one) before creating a
        // replacement so we do not accumulate keys and hit IAM limits.
        deleteUnmanagedAccessKeys(iamClient, userName, null);

        CreateAccessKeyResult result = iamClient.createAccessKey(
                new CreateAccessKeyRequest().withUserName(userName));
        AccessKey key = result.getAccessKey();

        // Update existing bucket records for this account/store with the new
        // credentials BEFORE persisting the new key in account details. If a
        // bucket update fails, the stored key remains the old one and a retry
        // will re-enter the replacement path; if we persisted first, a retry
        // would see the new stored key and return without repairing the
        // remaining buckets.
        updateAccountBucketCredentials(storeId, accountId, key);

        // Persist the credentials in the account details (namespaced by storeId)
        details.put(accessKeyDetailKey, key.getAccessKeyId());
        details.put(secretKeyDetailKey, key.getSecretAccessKey());
        _accountDetailsDao.persist(accountId, details);

        logger.info("Created IAM credentials {} for user {}", key.getAccessKeyId(), userName);
        return true;
        } finally {
            lock.unlock();
            lock.releaseRef();
        }
    }

    /**
     * Update the IAM credentials on all BucketVO rows for this store/account
     * so previously created buckets reflect the new (rotated) key pair.
     * Mirrors CloudianHyperStoreObjectStoreDriverImpl.updateAccountBucketCredentials.
     */
    private void updateAccountBucketCredentials(long storeId, long accountId, AccessKey iamCredential) {
        List<BucketVO> bucketList = _bucketDao.listByObjectStoreIdAndAccountId(storeId, accountId);
        for (BucketVO bucketVO : bucketList) {
            logger.info("Updating accountId={} bucket {} with new IAM credentials", accountId, bucketVO.getName());
            bucketVO.setAccessKey(iamCredential.getAccessKeyId());
            bucketVO.setSecretKey(iamCredential.getSecretAccessKey());
            _bucketDao.update(bucketVO.getId(), bucketVO);
        }
    }

    /**
     * Refresh the per-account IAM user policy so it grants S3 access only to
     * the account's current buckets (optionally excluding one, e.g. a bucket
     * being deleted). This is the tenant boundary: each account's IAM
     * credentials can only operate on that account's own buckets.
     *
     * @param iamClient the IAM client
     * @param storeId the object store
     * @param accountId the CloudStack account
     * @param excludeBucket a bucket name to omit (e.g. a bucket being deleted),
     *                      or null to include all of the account's buckets
     */
    protected void updateAccountIAMPolicy(AmazonIdentityManagement iamClient, long storeId, long accountId, String excludeBucket) {
        GlobalLock lock = acquireIamLock(storeId, accountId);
        if (lock == null) {
            throw new CloudRuntimeException("Failed to acquire IAM lock for store " + storeId + " account " + accountId);
        }
        try {
            Account account = _accountDao.findById(accountId);
            if (account == null) {
                return;
            }
            String userName = getUserNameForAccount(account, storeId);
            List<BucketVO> buckets = _bucketDao.listByObjectStoreIdAndAccountId(storeId, accountId);
            List<String> bucketNames = new ArrayList<>();
            for (BucketVO bvo : buckets) {
                if (excludeBucket != null && excludeBucket.equals(bvo.getName())) {
                    continue;
                }
                bucketNames.add(bvo.getName());
            }
            String policy = SeaweedFSObjectStoreUtil.buildAccountIAMPolicy(bucketNames);
            iamClient.putUserPolicy(new PutUserPolicyRequest(userName,
                    SeaweedFSObjectStoreUtil.IAM_USER_POLICY_NAME, policy));
        } finally {
            lock.unlock();
            lock.releaseRef();
        }
    }

    /**
     * Check whether the given access key id is still listed and Active in IAM
     * for the user. Listing failures are propagated rather than swallowed so
     * a transient IAM outage does not send createUser into the replacement
     * path (which would overwrite stored credentials and invalidate bucket
     * records).
     */
    private boolean iamAccessKeyExists(AmazonIdentityManagement iamClient, String userName, String accessKeyId) {
        for (AccessKeyMetadata metadata :
                iamClient.listAccessKeys(new ListAccessKeysRequest()
                        .withUserName(userName)).getAccessKeyMetadata()) {
            if (accessKeyId.equals(metadata.getAccessKeyId())) {
                return "Active".equalsIgnoreCase(metadata.getStatus());
            }
        }
        return false;
    }

    /**
     * Delete access keys for the user other than the (optionally) preserved
     * key id. Used to clean up unmanaged leftover keys before creating a
     * replacement so repeated calls do not hit IAM access-key limits.
     */
    private void deleteUnmanagedAccessKeys(AmazonIdentityManagement iamClient, String userName, String preserveAccessKeyId) {
        try {
            for (AccessKeyMetadata metadata :
                    iamClient.listAccessKeys(new ListAccessKeysRequest()
                            .withUserName(userName)).getAccessKeyMetadata()) {
                String keyId = metadata.getAccessKeyId();
                if (preserveAccessKeyId != null && preserveAccessKeyId.equals(keyId)) {
                    continue;
                }
                DeleteAccessKeyRequest deleteReq =
                        new DeleteAccessKeyRequest()
                                .withUserName(userName)
                                .withAccessKeyId(keyId);
                logger.info("Deleting un-managed IAM access key {} for user {}", keyId, userName);
                iamClient.deleteAccessKey(deleteReq);
            }
        } catch (AmazonClientException e) {
            // Propagate so the caller does not proceed to create a replacement
            // key while stale unmanaged keys remain (which could hit IAM key
            // limits or leave orphaned credentials).
            throw new CloudRuntimeException("Failed to clean up IAM access keys for user " + userName, e);
        }
    }

    @Override
    public Bucket createBucket(Bucket bucket, boolean objectLock) {
        String bucketName = bucket.getName();
        long storeId = bucket.getObjectStoreId();
        long accountId = bucket.getAccountId();

        // Use the store's admin credentials to create the bucket
        AmazonS3 s3client = getS3ClientByStoreId(storeId);

        // Check if the bucket already exists
        try {
            if (s3client.doesBucketExistV2(bucketName)) {
                throw new CloudRuntimeException("Bucket already exists with name " + bucketName);
            }
        } catch (AmazonClientException e) {
            throw new CloudRuntimeException(e);
        }

        // Create the bucket
        try {
            CreateBucketRequest request = new CreateBucketRequest(bucketName);
            if (objectLock) {
                request.setObjectLockEnabledForBucket(true);
            }
            s3client.createBucket(request);
        } catch (AmazonClientException e) {
            logger.error("Create bucket failed", e);
            throw new CloudRuntimeException(e);
        }

        // Configure permissive CORS so the CloudStack S3 bucket browser
        // (which performs list/upload/delete from the browser) can function.
        // SeaweedFS supports the standard PutBucketCors operation.
        configureBucketCORS(s3client, bucketName);

        // Step 2: update the bucket record with the account's IAM credentials.
        // If this fails, clean up the remote bucket so a retry does not find
        // it already existing — mirroring the Cloudian createBucket pattern.
        try {
            Map<String, String> accountDetails = _accountDetailsDao.findDetails(accountId);
            String accessKey = accountDetails.get(SeaweedFSObjectStoreUtil.keyAccessKey(storeId));
            String secretKey = accountDetails.get(SeaweedFSObjectStoreUtil.keySecretKey(storeId));
            if (accessKey == null || secretKey == null) {
                throw new CloudRuntimeException("No IAM credentials found for account " + accountId
                        + " on store " + storeId + ". Run createUser before creating a bucket.");
            }

            String s3Url = getS3Url(storeId);
            BucketVO bucketVO = _bucketDao.findById(bucket.getId());
            bucketVO.setAccessKey(accessKey);
            bucketVO.setSecretKey(secretKey);
            bucketVO.setBucketURL(s3Url + "/" + bucketName);
            _bucketDao.update(bucket.getId(), bucketVO);

            // Refresh the account's IAM policy to include the new bucket
            AmazonIdentityManagement iamClient = getIAMClient(storeId);
            updateAccountIAMPolicy(iamClient, storeId, accountId, null);

            return bucket;
        } catch (Exception e) {
            logger.error("Post-create bucket record update failed for {}; cleaning up remote bucket", bucketName, e);
            try {
                s3client.deleteBucket(bucketName);
                logger.info("Cleanup of bucket {} succeeded", bucketName);
            } catch (AmazonClientException cleanupEx) {
                logger.error("Cleanup of bucket {} also failed", bucketName, cleanupEx);
            }
            // Revoke the IAM policy grant for the new bucket so the account's
            // credentials cannot access a bucket that no longer exists. If the
            // policy PUT succeeded before the DB update failed, the grant
            // would otherwise persist and could be reused if another account
            // later creates the same bucket name.
            try {
                AmazonIdentityManagement iamClient = getIAMClient(storeId);
                updateAccountIAMPolicy(iamClient, storeId, accountId, bucketName);
            } catch (Exception policyEx) {
                logger.warn("Failed to revoke IAM policy for bucket {} after cleanup: {}", bucketName, policyEx.getMessage());
            }
            throw new CloudRuntimeException(e);
        }
    }

    /**
     * Configure a permissive CORS policy on the bucket so the CloudStack
     * S3 bucket browser (which performs list/upload/delete from the
     * browser) can function. Mirrors the Cloudian configureBucketCORS.
     */
    private void configureBucketCORS(AmazonS3 s3client, String bucketName) {
        logger.debug("Configuring CORS for bucket {}", bucketName);
        List<CORSRule> corsRules = new ArrayList<>();
        CORSRule allowAnyRule = new CORSRule().withId("AllowAny");
        allowAnyRule.setAllowedOrigins("*");
        allowAnyRule.setAllowedHeaders("*");
        allowAnyRule.setAllowedMethods(
            CORSRule.AllowedMethods.HEAD,
            CORSRule.AllowedMethods.GET,
            CORSRule.AllowedMethods.PUT,
            CORSRule.AllowedMethods.POST,
            CORSRule.AllowedMethods.DELETE);
        corsRules.add(allowAnyRule);
        BucketCrossOriginConfiguration corsConfig = new BucketCrossOriginConfiguration();
        corsConfig.setRules(corsRules);
        SetBucketCrossOriginConfigurationRequest corsRequest = new SetBucketCrossOriginConfigurationRequest(bucketName, corsConfig);
        s3client.setBucketCrossOriginConfiguration(corsRequest);
        logger.info("Successfully configured CORS for bucket {}", bucketName);
    }

    @Override
    public List<Bucket> listBuckets(long storeId) {
        AmazonS3 s3client = getS3ClientByStoreId(storeId);
        List<Bucket> bucketsList = new ArrayList<>();
        try {
            List<com.amazonaws.services.s3.model.Bucket> s3Buckets = s3client.listBuckets();
            for (com.amazonaws.services.s3.model.Bucket s3Bucket : s3Buckets) {
                Bucket bucket = new BucketObject();
                bucket.setName(s3Bucket.getName());
                bucketsList.add(bucket);
            }
        } catch (AmazonClientException e) {
            throw new CloudRuntimeException(e);
        }
        return bucketsList;
    }

    @Override
    public boolean deleteBucket(BucketTO bucket, long storeId) {
        String bucketName = bucket.getName();
        long accountId = bucket.getAccountId();
        AmazonS3 s3client = getS3ClientByStoreId(storeId);
        // If the bucket is already gone (e.g. from a previous partial
        // failure where the S3 delete succeeded but the IAM policy refresh
        // failed), skip the S3 delete and proceed to policy reconciliation
        // so the retry is idempotent.
        try {
            if (s3client.doesBucketExistV2(bucketName)) {
                s3client.deleteBucket(bucketName);
            }
        } catch (AmazonClientException e) {
            throw new CloudRuntimeException(e);
        }
        // Refresh the account's IAM policy to drop the deleted bucket. This
        // must succeed: bucket names are reusable, so a stale grant would
        // let the old account access a new tenant's bucket with the same
        // name. The policy is refreshed after the remote delete so a policy
        // refresh failure does not leave an orphaned remote bucket; if it
        // fails, the caller sees the exception and can reconcile the IAM
        // policy while the CloudStack BucketVO is removed.
        AmazonIdentityManagement iamClient = getIAMClient(storeId);
        updateAccountIAMPolicy(iamClient, storeId, accountId, bucketName);
        return true;
    }

    @Override
    public AccessControlList getBucketAcl(BucketTO bucket, long storeId) {
        AmazonS3 s3client = getS3ClientByStoreId(storeId);
        try {
            return s3client.getBucketAcl(bucket.getName());
        } catch (AmazonClientException e) {
            throw new CloudRuntimeException(e);
        }
    }

    @Override
    public void setBucketAcl(BucketTO bucket, AccessControlList acl, long storeId) {
        AmazonS3 s3client = getS3ClientByStoreId(storeId);
        try {
            s3client.setBucketAcl(bucket.getName(), acl);
        } catch (AmazonClientException e) {
            throw new CloudRuntimeException(e);
        }
    }

    @Override
    public void setBucketPolicy(BucketTO bucket, String policy, long storeId) {
        if ("private".equalsIgnoreCase(policy)) {
            deleteBucketPolicy(bucket, storeId);
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"Version\": \"2012-10-17\",\n");
        sb.append("  \"Statement\": [\n");
        sb.append("    {\n");
        sb.append("      \"Sid\": \"PublicReadForObjects\",\n");
        sb.append("      \"Effect\": \"Allow\",\n");
        sb.append("      \"Principal\": \"*\",\n");
        sb.append("      \"Action\": \"s3:GetObject\",\n");
        sb.append("      \"Resource\": \"arn:aws:s3:::%s/*\"\n");
        sb.append("    }\n");
        sb.append("  ]\n");
        sb.append("}\n");

        String jsonPolicy = String.format(sb.toString(), bucket.getName());
        AmazonS3 s3client = getS3ClientByStoreId(storeId);
        try {
            s3client.setBucketPolicy(bucket.getName(), jsonPolicy);
        } catch (AmazonClientException e) {
            throw new CloudRuntimeException(e);
        }
    }

    @Override
    public BucketPolicy getBucketPolicy(BucketTO bucket, long storeId) {
        AmazonS3 s3client = getS3ClientByStoreId(storeId);
        try {
            return s3client.getBucketPolicy(new GetBucketPolicyRequest(bucket.getName()));
        } catch (AmazonClientException e) {
            throw new CloudRuntimeException(e);
        }
    }

    @Override
    public void deleteBucketPolicy(BucketTO bucket, long storeId) {
        AmazonS3 s3client = getS3ClientByStoreId(storeId);
        try {
            s3client.deleteBucketPolicy(new DeleteBucketPolicyRequest(bucket.getName()));
        } catch (AmazonClientException e) {
            throw new CloudRuntimeException(e);
        }
    }

    @Override
    public boolean setBucketEncryption(BucketTO bucket, long storeId) {
        AmazonS3 s3client = getS3ClientByStoreId(storeId);
        try {
            SetBucketEncryptionRequest eRequest = new SetBucketEncryptionRequest();
            eRequest.setBucketName(bucket.getName());

            ServerSideEncryptionByDefault sseByDefault = new ServerSideEncryptionByDefault();
            sseByDefault.setSSEAlgorithm(SSEAlgorithm.AES256.toString());

            ServerSideEncryptionRule sseRule = new ServerSideEncryptionRule();
            sseRule.setApplyServerSideEncryptionByDefault(sseByDefault);

            List<ServerSideEncryptionRule> sseRules = new ArrayList<>();
            sseRules.add(sseRule);

            ServerSideEncryptionConfiguration sseConf = new ServerSideEncryptionConfiguration();
            sseConf.setRules(sseRules);

            eRequest.setServerSideEncryptionConfiguration(sseConf);
            s3client.setBucketEncryption(eRequest);
            return true;
        } catch (AmazonClientException e) {
            throw new CloudRuntimeException(e);
        }
    }

    @Override
    public boolean deleteBucketEncryption(BucketTO bucket, long storeId) {
        AmazonS3 s3client = getS3ClientByStoreId(storeId);
        try {
            s3client.deleteBucketEncryption(bucket.getName());
            return true;
        } catch (AmazonClientException e) {
            throw new CloudRuntimeException(e);
        }
    }

    @Override
    public boolean setBucketVersioning(BucketTO bucket, long storeId) {
        AmazonS3 s3client = getS3ClientByStoreId(storeId);
        try {
            BucketVersioningConfiguration vConf = new BucketVersioningConfiguration(BucketVersioningConfiguration.ENABLED);
            s3client.setBucketVersioningConfiguration(
                    new SetBucketVersioningConfigurationRequest(bucket.getName(), vConf));
            return true;
        } catch (AmazonClientException e) {
            throw new CloudRuntimeException(e);
        }
    }

    @Override
    public boolean deleteBucketVersioning(BucketTO bucket, long storeId) {
        AmazonS3 s3client = getS3ClientByStoreId(storeId);
        try {
            BucketVersioningConfiguration vConf = new BucketVersioningConfiguration(BucketVersioningConfiguration.SUSPENDED);
            s3client.setBucketVersioningConfiguration(
                    new SetBucketVersioningConfigurationRequest(bucket.getName(), vConf));
            return true;
        } catch (AmazonClientException e) {
            throw new CloudRuntimeException(e);
        }
    }

    /**
     * Set the bucket quota via the SeaweedFS S3 extension
     * ({@code PUT /{bucket}?seaweedfs-quota}), signed with the store admin
     * S3 credentials.
     *
     * SeaweedFS enforces bucket quota server-side by setting a read-only flag
     * when usage exceeds the configured limit. The quota is configured via the
     * SeaweedFS S3 extension endpoint PUT /{bucket}?seaweedfs-quota,
     * authenticated via standard S3 SigV4 and authorized via the
     * s3:PutBucketQuota IAM permission.
     *
     * @param size the GiB size to set the quota to. 0 disables quota.
     * @throws CloudRuntimeException if the S3 endpoint or credentials are missing or the request fails.
     */
    @Override
    public void setBucketQuota(BucketTO bucket, long storeId, long size) {
        String s3Url = getS3Url(storeId);
        String accessKey = getAccessKey(storeId);
        String secretKey = getSecretKey(storeId);
        if (s3Url == null || s3Url.isEmpty() || accessKey == null || accessKey.isEmpty() || secretKey == null || secretKey.isEmpty()) {
            throw new CloudRuntimeException("SeaweedFS S3 URL and credentials are required to set bucket quota. " +
                    "Configure 's3Url', 'accesskey', and 'secretkey' in the object store details.");
        }
        SeaweedFSObjectStoreUtil.setBucketQuotaViaS3Extension(s3Url, accessKey, secretKey, bucket.getName(), size, getS3ExtensionHttpClient());
    }

    /**
     * Returns the HTTP client used to send SeaweedFS S3 extension requests
     * (e.g. PUT /{bucket}?seaweedfs-quota). Exposed as a protected seam so
     * tests can inject a mock client and assert the signed request without
     * touching the network.
     */
    protected java.net.http.HttpClient getS3ExtensionHttpClient() {
        return SeaweedFSObjectStoreUtil.newS3ExtensionHttpClient();
    }

    @Override
    public Map<String, Long> getAllBucketsUsage(long storeId) {
        Map<String, Long> bucketUsage = new HashMap<>();
        List<BucketVO> bucketList = _bucketDao.listByObjectStoreId(storeId);
        if (bucketList.isEmpty()) {
            return bucketUsage;
        }

        // List objects per bucket via S3 (no admin API needed).
        // SeaweedFS also publishes per-bucket Prometheus metrics and an SOSAPI
        // capacity.xml response; operators who need scalable usage reporting
        // should consume those instead of S3 list-based aggregation.
        AmazonS3 s3client = getS3ClientByStoreId(storeId);
        for (BucketVO bucket : bucketList) {
            try {
                long size = 0L;
                com.amazonaws.services.s3.model.ListObjectsV2Result result;
                String continuationToken = null;
                do {
                    com.amazonaws.services.s3.model.ListObjectsV2Request req =
                            new com.amazonaws.services.s3.model.ListObjectsV2Request()
                                    .withBucketName(bucket.getName())
                                    .withMaxKeys(1000);
                    if (continuationToken != null) {
                        req.setContinuationToken(continuationToken);
                    }
                    result = s3client.listObjectsV2(req);
                    for (com.amazonaws.services.s3.model.S3ObjectSummary summary : result.getObjectSummaries()) {
                        size += summary.getSize();
                    }
                    continuationToken = result.getNextContinuationToken();
                } while (result.isTruncated());
                bucketUsage.put(bucket.getName(), size);
            } catch (AmazonClientException e) {
                // Propagate the failure so BucketApiServiceImpl does not
                // overwrite objectStoreVO.usedSize with a partial total.
                // Returning only the successful buckets would under-report
                // store usage and trigger false capacity alerts.
                throw new CloudRuntimeException("Failed to get usage for bucket " + bucket.getName(), e);
            }
        }
        return bucketUsage;
    }

    // ---- Client builders ----

    protected String getS3Url(long storeId) {
        // Read the configured S3 endpoint from the persisted details first
        // (it may differ from the generic ObjectStoreVO.url), falling back to
        // the store URL only if the detail is missing. This matches the
        // Cloudian HyperStore pattern.
        Map<String, String> storeDetails = _storeDetailsDao.getDetails(storeId);
        String s3Url = storeDetails.get(SeaweedFSObjectStoreUtil.STORE_DETAILS_KEY_S3_URL);
        if (s3Url == null || s3Url.isEmpty()) {
            ObjectStoreVO store = _storeDao.findById(storeId);
            if (store != null) {
                s3Url = store.getUrl();
            }
        }
        return s3Url;
    }

    protected String getIAMUrl(long storeId) {
        Map<String, String> storeDetails = _storeDetailsDao.getDetails(storeId);
        String iamUrl = storeDetails.get(SeaweedFSObjectStoreUtil.STORE_DETAILS_KEY_IAM_URL);
        if (iamUrl == null || iamUrl.isEmpty()) {
            // iamUrl was not explicitly configured; SeaweedFS serves the IAM
            // API from the same endpoint as S3 by default, so fall back to
            // the current S3 endpoint. This also keeps IAM provisioning on the
            // live endpoint after updateObjectStore changes the store URL.
            iamUrl = getS3Url(storeId);
        }
        return iamUrl;
    }

    protected String getAccessKey(long storeId) {
        Map<String, String> storeDetails = _storeDetailsDao.getDetails(storeId);
        return storeDetails.get(SeaweedFSObjectStoreUtil.STORE_DETAILS_KEY_ACCESS_KEY);
    }

    protected String getSecretKey(long storeId) {
        Map<String, String> storeDetails = _storeDetailsDao.getDetails(storeId);
        return storeDetails.get(SeaweedFSObjectStoreUtil.STORE_DETAILS_KEY_SECRET_KEY);
    }

    protected AmazonS3 getS3ClientByStoreId(long storeId) {
        String s3Url = getS3Url(storeId);
        Map<String, String> storeDetails = _storeDetailsDao.getDetails(storeId);
        String accessKey = storeDetails.get(SeaweedFSObjectStoreUtil.STORE_DETAILS_KEY_ACCESS_KEY);
        String secretKey = storeDetails.get(SeaweedFSObjectStoreUtil.STORE_DETAILS_KEY_SECRET_KEY);
        return SeaweedFSObjectStoreUtil.getS3Client(s3Url, accessKey, secretKey);
    }

    protected AmazonIdentityManagement getIAMClient(long storeId) {
        String iamUrl = getIAMUrl(storeId);
        Map<String, String> storeDetails = _storeDetailsDao.getDetails(storeId);
        String accessKey = storeDetails.get(SeaweedFSObjectStoreUtil.STORE_DETAILS_KEY_ACCESS_KEY);
        String secretKey = storeDetails.get(SeaweedFSObjectStoreUtil.STORE_DETAILS_KEY_SECRET_KEY);
        return SeaweedFSObjectStoreUtil.getIAMClient(iamUrl, accessKey, secretKey);
    }
}

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
import com.amazonaws.services.identitymanagement.model.CreateAccessKeyRequest;
import com.amazonaws.services.identitymanagement.model.CreateAccessKeyResult;
import com.amazonaws.services.identitymanagement.model.CreateUserRequest;
import com.amazonaws.services.identitymanagement.model.EntityAlreadyExistsException;
import com.amazonaws.services.identitymanagement.model.PutUserPolicyRequest;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AccessControlList;
import com.amazonaws.services.s3.model.BucketPolicy;
import com.amazonaws.services.s3.model.BucketVersioningConfiguration;
import com.amazonaws.services.s3.model.CreateBucketRequest;
import com.amazonaws.services.s3.model.DeleteBucketPolicyRequest;
import com.amazonaws.services.s3.model.GetBucketPolicyRequest;
import com.amazonaws.services.s3.model.SSEAlgorithm;
import com.amazonaws.services.s3.model.ServerSideEncryptionByDefault;
import com.amazonaws.services.s3.model.ServerSideEncryptionConfiguration;
import com.amazonaws.services.s3.model.ServerSideEncryptionRule;
import com.amazonaws.services.s3.model.SetBucketEncryptionRequest;
import com.amazonaws.services.s3.model.SetBucketVersioningConfigurationRequest;
import com.cloud.agent.api.to.BucketTO;
import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.storage.BucketVO;
import com.cloud.storage.dao.BucketDao;
import com.cloud.user.Account;
import com.cloud.user.AccountDetailsDao;
import com.cloud.user.dao.AccountDao;
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

    @Override
    public DataStoreTO getStoreTO(DataStore store) {
        return null;
    }

    /**
     * Get the SeaweedFS IAM user name for the given CloudStack account.
     * Uses the account UUID prefixed with "acs-" for namespacing.
     */
    protected String getUserNameForAccount(Account account) {
        return String.format("%s-%s", ACS_PREFIX, account.getUuid());
    }

    /**
     * Create the IAM user for the CloudStack account if it doesn't exist,
     * attach the restricted S3 policy, create an access key, and persist the
     * credentials in the account details.
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
        String userName = getUserNameForAccount(account);
        AmazonIdentityManagement iamClient = getIAMClient(storeId);

        // Create the IAM user if it doesn't already exist
        try {
            iamClient.createUser(new CreateUserRequest(userName));
            logger.info("Created IAM user {} for account {}", userName, account.getAccountName());
        } catch (EntityAlreadyExistsException e) {
            logger.debug("IAM user {} already exists", userName);
        }

        // Attach the restricted S3 policy (idempotent — overwrites if present)
        iamClient.putUserPolicy(new PutUserPolicyRequest(userName,
                "CloudStackPolicy", SeaweedFSObjectStoreUtil.IAM_USER_POLICY));

        // Create a new access key for this user
        CreateAccessKeyResult result = iamClient.createAccessKey(
                new CreateAccessKeyRequest().withUserName(userName));
        AccessKey key = result.getAccessKey();

        // Persist the credentials in the account details
        Map<String, String> details = _accountDetailsDao.findDetails(accountId);
        details.put(SeaweedFSObjectStoreUtil.KEY_ACCESS_KEY, key.getAccessKeyId());
        details.put(SeaweedFSObjectStoreUtil.KEY_SECRET_KEY, key.getSecretAccessKey());
        _accountDetailsDao.persist(accountId, details);

        logger.info("Created IAM credentials {} for user {}", key.getAccessKeyId(), userName);
        return true;
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

        // Update the bucket record with the account's IAM credentials
        Map<String, String> accountDetails = _accountDetailsDao.findDetails(accountId);
        String accessKey = accountDetails.get(SeaweedFSObjectStoreUtil.KEY_ACCESS_KEY);
        String secretKey = accountDetails.get(SeaweedFSObjectStoreUtil.KEY_SECRET_KEY);
        if (accessKey == null || secretKey == null) {
            logger.warn("No IAM credentials found for account {}. Bucket will be created without per-account credentials.", accountId);
        }

        ObjectStoreVO store = _storeDao.findById(storeId);
        String s3Url = getS3Url(storeId);
        BucketVO bucketVO = _bucketDao.findById(bucket.getId());
        bucketVO.setAccessKey(accessKey);
        bucketVO.setSecretKey(secretKey);
        bucketVO.setBucketURL(s3Url + "/" + bucketName);
        _bucketDao.update(bucket.getId(), bucketVO);
        return bucket;
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
        AmazonS3 s3client = getS3ClientByStoreId(storeId);
        try {
            if (! s3client.doesBucketExistV2(bucket.getName())) {
                throw new CloudRuntimeException("Bucket doesn't exist: " + bucket.getName());
            }
        } catch (AmazonClientException e) {
            throw new CloudRuntimeException(e);
        }
        try {
            s3client.deleteBucket(bucket.getName());
        } catch (AmazonClientException e) {
            throw new CloudRuntimeException(e);
        }
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
     * Set the bucket quota via the SeaweedFS admin REST API.
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
        SeaweedFSObjectStoreUtil.setBucketQuotaViaS3Extension(s3Url, accessKey, secretKey, bucket.getName(), size);
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
                logger.warn("Failed to get usage for bucket {}: {}", bucket.getName(), e.getMessage());
                bucketUsage.put(bucket.getName(), 0L);
            }
        }
        return bucketUsage;
    }

    // ---- Client builders ----

    protected String getS3Url(long storeId) {
        Map<String, String> storeDetails = _storeDetailsDao.getDetails(storeId);
        String s3Url = storeDetails.get(SeaweedFSObjectStoreUtil.STORE_DETAILS_KEY_S3_URL);
        if (s3Url == null || s3Url.isEmpty()) {
            ObjectStoreVO store = _storeDao.findById(storeId);
            s3Url = store.getUrl();
        }
        return s3Url;
    }

    protected String getIAMUrl(long storeId) {
        Map<String, String> storeDetails = _storeDetailsDao.getDetails(storeId);
        return storeDetails.get(SeaweedFSObjectStoreUtil.STORE_DETAILS_KEY_IAM_URL);
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

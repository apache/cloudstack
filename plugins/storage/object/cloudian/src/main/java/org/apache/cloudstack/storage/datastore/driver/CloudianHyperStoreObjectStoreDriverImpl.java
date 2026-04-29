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

import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.cloudian.client.CloudianClient;
import org.apache.cloudstack.cloudian.client.CloudianCredential;
import org.apache.cloudstack.cloudian.client.CloudianGroup;
import org.apache.cloudstack.cloudian.client.CloudianUser;
import org.apache.cloudstack.cloudian.client.CloudianUserBucketUsage;
import org.apache.cloudstack.cloudian.client.CloudianUserBucketUsage.CloudianBucketUsage;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.storage.datastore.db.ObjectStoreDao;
import org.apache.cloudstack.storage.datastore.db.ObjectStoreDetailsDao;
import org.apache.cloudstack.storage.datastore.db.ObjectStoreVO;
import org.apache.cloudstack.storage.datastore.util.CloudianHyperStoreUtil;
import org.apache.cloudstack.storage.object.BaseObjectStoreDriverImpl;
import org.apache.cloudstack.storage.object.Bucket;
import org.apache.cloudstack.storage.object.BucketObject;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.model.AccessKey;
import com.amazonaws.services.identitymanagement.model.AccessKeyMetadata;
import com.amazonaws.services.identitymanagement.model.CreateAccessKeyRequest;
import com.amazonaws.services.identitymanagement.model.CreateUserRequest;
import com.amazonaws.services.identitymanagement.model.DeleteAccessKeyRequest;
import com.amazonaws.services.identitymanagement.model.EntityAlreadyExistsException;
import com.amazonaws.services.identitymanagement.model.ListAccessKeysRequest;
import com.amazonaws.services.identitymanagement.model.ListAccessKeysResult;
import com.amazonaws.services.identitymanagement.model.NoSuchEntityException;
import com.amazonaws.services.identitymanagement.model.PutUserPolicyRequest;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AccessControlList;
import com.amazonaws.services.s3.model.BucketCrossOriginConfiguration;
import com.amazonaws.services.s3.model.BucketPolicy;
import com.amazonaws.services.s3.model.BucketVersioningConfiguration;
import com.amazonaws.services.s3.model.CORSRule;
import com.amazonaws.services.s3.model.CreateBucketRequest;
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
import com.cloud.domain.Domain;
import com.cloud.domain.dao.DomainDao;
import com.cloud.user.Account;
import com.cloud.user.AccountDetailsDao;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.exception.CloudRuntimeException;

public class CloudianHyperStoreObjectStoreDriverImpl extends BaseObjectStoreDriverImpl {
    @Inject
    AccountDao _accountDao;

    @Inject
    AccountDetailsDao _accountDetailsDao;

    @Inject
    DomainDao _domainDao;

    @Inject
    ObjectStoreDao _storeDao;

    @Inject
    BucketDao _bucketDao;

    @Inject
    ObjectStoreDetailsDao _storeDetailsDao;

    @Override
    public DataStoreTO getStoreTO(DataStore store) {
        return null;
    }

    /**
     * Get the HyperStore user id for the current account.
     * @param account the current account
     * @return the userId based on the CloudStack account uuid.
     */
    protected String getHyperStoreUserId(Account account) {
        return account.getUuid();
    }

    /**
     * Get the HyperStore tenant/group id for the current domain.
     * @param domain the current domain
     * @return the groupId based on the CloudStack domain uuid
     */
    protected String getHyperStoreGroupId(Domain domain) {
        return domain.getUuid();
    }

    /**
     * Create the HyperStore user resources matching this account if it doesn't exist.
     *
     * The following resources are created for the account:
     * - HyperStore Group to match the CloudStack Domain UUID
     * - HyperStore User to match the CloudStack Account UUID
     * - HyperStore Root User Credentials to manage Account Buckets etc (kept private to this plugin)
     * - HyperStore IAM User with IAM policy granting all S3 actions except create/delete buckets.
     * - HyperStore IAM User Credentials (visible to end user as part of Bucket Details)
     *
     * @param accountId the CloudStack account
     * @param storeId the object store.
     *
     * @return true if user exists or was created, false if there was some issue creating it.
     * @throws CloudRuntimeException on errors checking if the user exists or if the HyperStore user or group is disabled.
     */
    @Override
    public boolean createUser(long accountId, long storeId) {
        Account account = _accountDao.findById(accountId);
        Domain domain = _domainDao.findById(account.getDomainId());
        String hsUserId = getHyperStoreUserId(account);
        String hsGroupId = getHyperStoreGroupId(domain);

        CloudianClient client = getCloudianClientByStoreId(storeId);
        logger.debug("Checking if user id={} group id={} exists.", hsGroupId, hsUserId);
        CloudianUser user = client.listUser(hsUserId, hsGroupId);
        if (user == null) {
            // Create the group if it doesn't already exist
            createHSGroup(client, hsGroupId, domain);
            // Create the user under the group.
            user = createHSUser(client, hsUserId, hsGroupId, account);
            if (user == null) {
                return false; // already logged.
            }
        } else if (! user.getActive()) {
            // Normally this would be true unless an administrator has explicitly disabled the user account.
            String msg = String.format("The User id=%s group id=%s is Disabled. Consult your HyperStore Administrator.", hsUserId, hsGroupId);
            logger.error(msg);
            throw new CloudRuntimeException(msg);
        } else {
            // User exists and is active. We know that the group therefore exists but
            // we should ensure that it is active or it will lead to unknown access key errors
            // which might confuse the administrator. Checking is clearer.
            CloudianGroup group = client.listGroup(hsGroupId);
            if (group != null && ! group.getActive()) {
                String msg = String.format("The group id=%s is Disabled. Consult your HyperStore Administrator.", hsGroupId);
                logger.error(msg);
                throw new CloudRuntimeException(msg);
            }
        }

        // We either created a new account or found an existing one.
        CloudianCredential credential = createHSCredential(client, hsUserId, hsGroupId);

        // Next, ensure we the IAM User Credentials exist. These are available
        // to the user as part of the bucket details instead of the Root credentials.
        Map<String, String> details = _accountDetailsDao.findDetails(accountId);
        AccessKey iamCredential = createIAMCredentials(storeId, details, credential);

        // persist the root and iam credentials in the database and update all bucket details.
        persistCredentials(storeId, accountId, details, credential, iamCredential);
        return true;
    }

    /**
     * Create IAM credentials if required.
     *
     * When the HyperStore user is first created, this method will create an IAM User with an appropriate
     * permission policy and a set of credentials which will be returned.
     * After the first run, the IAM resources should already be in place in which case we just ensure
     * the credentials we are using are still available and if not, it tries to recreate the IAM resources.
     *
     * @param storeId the store
     * @param details a map of existing account details that we know about including any saved IAM credentials.
     * @param credential the account Root User credentials (to manage the IAM resources).
     * @return an AccessKey object for newly created IAM credentials or null if existing credentials were ok
     *    and nothing was created.
     */
    protected AccessKey createIAMCredentials(long storeId, Map<String, String> details, CloudianCredential credential) {
        AmazonIdentityManagement iamClient = getIAMClientByStoreId(storeId, credential);
        final String iamUser = CloudianHyperStoreUtil.IAM_USER_USERNAME;

        // If an accessKeyId is known to us, check IAM still has it.
        String iamAccessKeyId = details.get(CloudianHyperStoreUtil.KEY_IAM_ACCESS_KEY);
        if (iamAccessKeyId != null) {
            try {
                logger.debug("Looking for IAM credential {} for IAM User {}", iamAccessKeyId, iamUser);
                ListAccessKeysResult listAccessKeyResult = iamClient.listAccessKeys(new ListAccessKeysRequest().withUserName(iamUser));
                for (AccessKeyMetadata accessKeyMetadata : listAccessKeyResult.getAccessKeyMetadata()) {
                    if (iamAccessKeyId.equals(accessKeyMetadata.getAccessKeyId())) {
                        return null;  // The IAM AccessKeyId still exists (as expected). return null.
                    }
                    // Usually, there will only be 1 credential that we manage, but an error persisting
                    // credentials might leave an un-managed credential which we can just delete. It is better
                    // to delete as otherwise, we may hit a max credential limit for this IAM user.
                    deleteIAMCredential(iamClient, iamUser, accessKeyMetadata.getAccessKeyId());
                }
            } catch (NoSuchEntityException e) {
                // No IAM User. Ignore and fix this below.
            }
        }

        // If we get here, a usable credential does not yet exist so create it.
        // Before creating it, we also need to ensure the IAM User that will own it exists.
        boolean createdUser = false;
        try {
            iamClient.createUser(new CreateUserRequest(iamUser));
            logger.info("Created IAM user {} for account", iamUser);
            createdUser = true;
        } catch (EntityAlreadyExistsException e) {
            // User already exists. Ignore and continue.
        }

        // Always Add or Update the IAM policy
        iamClient.putUserPolicy(new PutUserPolicyRequest(iamUser, CloudianHyperStoreUtil.IAM_USER_POLICY_NAME, CloudianHyperStoreUtil.IAM_USER_POLICY));

        if (! createdUser && iamAccessKeyId == null) {
            // User already exists but we never saved any access key before. We should try clean up
            logger.debug("Looking for any un-managed IAM credentials for IAM User {}", iamUser);
            ListAccessKeysResult listRes = iamClient.listAccessKeys(new ListAccessKeysRequest().withUserName(iamUser));
            for (AccessKeyMetadata accessKeyMetadata : listRes.getAccessKeyMetadata()) {
                deleteIAMCredential(iamClient, iamUser, accessKeyMetadata.getAccessKeyId());
            }
        }

        // Create and return the new IAM credentials for this user.
        AccessKey iamAccessKey = iamClient.createAccessKey(new CreateAccessKeyRequest(iamUser)).getAccessKey();
        logger.info("Created IAM Credential {} for IAM User {}", iamAccessKey.getAccessKeyId(), iamUser);
        return iamAccessKey;
    }

    /**
     * Delete an IAM Credential.
     *
     * @param iamClient a valid iam connection
     * @param iamUser the IAM user that owns the credential to delete.
     * @param accessKeyId The IAM credential to delete
     */
    protected void deleteIAMCredential(AmazonIdentityManagement iamClient, String iamUser, String accessKeyId) {
        DeleteAccessKeyRequest deleteAccessKeyRequest = new DeleteAccessKeyRequest();
        deleteAccessKeyRequest.setUserName(iamUser);
        deleteAccessKeyRequest.setAccessKeyId(accessKeyId);
        logger.info("Deleting un-managed IAM AccessKeyId {} for IAM User {}", accessKeyId, iamUser);
        iamClient.deleteAccessKey(deleteAccessKeyRequest);
    }

    /**
     * Persist the Root and IAM user credentials with the Account as required.
     * @param storeId the store
     * @param accountId the CloudStack account the credential belongs to
     * @param details the Account details map containing any pre-existing credential entries
     * @param credential the HyperStore credential assigned to this account.
     * @param iamCredential the new IAM credential or null if nothing new to persist.
     */
    private void persistCredentials(long storeId, long accountId, Map<String, String> details, CloudianCredential credential, AccessKey iamCredential) {
        boolean persist = false;

        String rootAccessKey = details.get(CloudianHyperStoreUtil.KEY_ROOT_ACCESS_KEY);
        if (! credential.getAccessKey().equals(rootAccessKey)) {
            // Persist the new (possibly rotated) credential pair
            details.put(CloudianHyperStoreUtil.KEY_ROOT_ACCESS_KEY, credential.getAccessKey());
            details.put(CloudianHyperStoreUtil.KEY_ROOT_SECRET_KEY, credential.getSecretKey());
            persist = true;
        }

        if (iamCredential != null) {
            // Persist the new IAM credentials
            details.put(CloudianHyperStoreUtil.KEY_IAM_ACCESS_KEY, iamCredential.getAccessKeyId());
            details.put(CloudianHyperStoreUtil.KEY_IAM_SECRET_KEY, iamCredential.getSecretAccessKey());
            updateAccountBucketCredentials(storeId, accountId, iamCredential);
            persist = true;
        }

        if (persist) {
            logger.debug("Persisting new credential information for accountId={}", accountId);
            _accountDetailsDao.persist(accountId, details);
        }
    }

    /**
     * Update bucket details associated with this store/account to use the new IAM credentials.
     *
     * @param storeId the store
     * @param accountId the user account
     * @param iamCredential the IAM credentials to associate with any existing buckets.
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
     * Create a HyperStore credential for the user if one does not already exist.
     * @param client ADMIN API connection
     * @param hsUserId HyperStore userId
     * @param hsGroupId HyperStore groupId
     *
     * @return a Root Credential (never null)
     * @throws ServerApiException if any error is encountered
     */
    protected CloudianCredential createHSCredential(CloudianClient client, String hsUserId, String hsGroupId) {
        // find the oldest active Root credential in the account.
        List<CloudianCredential> credentials = client.listCredentials(hsUserId, hsGroupId);
        CloudianCredential credential = null;
        for (CloudianCredential candidate : credentials) {
            if (! candidate.getActive()) {
                continue;
            }
            if (credential == null || credential.isNewerThan(candidate)) {
                credential = candidate;
            }
        }

        if (credential == null) {
            // nothing found, create one
            logger.debug("No active credentials found for groupId={} userId={}. Creating one.", hsGroupId, hsUserId);
            credential = client.createCredential(hsUserId, hsGroupId);
            logger.info("Created Root credentials for groupId={} userId={}.", hsGroupId, hsUserId);
        }

        // Either found or successfully created a credential.
        return credential;
    }

    /**
     * Create the HyperStore Group if it does not already exist.
     * @param client a CloudianClient connection
     * @param hsGroupId the name of the HyperStore group to create.
     * @param domain the domain that is being mapped to the HyperStore group.
     * @throws CloudRuntimeException if the group cannot be created or the group exists but is disabled.
     */
    private void createHSGroup(CloudianClient client, String hsGroupId, Domain domain) {
        // The group will usually exist so lets look for it before trying to add it.
        logger.debug("Checking if group {} exists.", hsGroupId);
        CloudianGroup group = client.listGroup(hsGroupId);
        if (group == null) {
            group = new CloudianGroup();
            group.setGroupId(hsGroupId);
            group.setActive(Boolean.TRUE);
            group.setGroupName(domain.getPath());
            client.addGroup(group);
            logger.info("Created group {} for domain {} successfully.", hsGroupId, domain.getPath());
            return;
        }

        // Group exists. Confirm that it is usable.
        if (! group.getActive()) {
            String msg = String.format("The group %s is Disabled. Consult your HyperStore Administrator.", hsGroupId);
            logger.error(msg);
            throw new CloudRuntimeException(msg);
        }

        // Group exists and is enabled. Nothing to log.
        return;
    }

    /**
     * Create a new HyperStore user
     *
     * @param client admin api client
     * @param hsUserId the user to create
     * @param hsGroupId the group to add him to
     * @param account the account the user represents
     * @return user object if successfully created, null otherwise
     * @throws ServerAPIException if on other other.
     */
    private CloudianUser createHSUser(CloudianClient client, String hsUserId, String hsGroupId, Account account) {
        CloudianUser user = new CloudianUser();
        user.setActive(Boolean.TRUE);
        user.setGroupId(hsGroupId);
        user.setUserId(hsUserId);
        user.setUserType(CloudianUser.USER);
        user.setFullName(account.getAccountName());

        if (! client.addUser(user)) {
            // The failure shouldn't be that the user already exists at this point so its something else.
            logger.error("Failed to add user id={} groupId={}", hsUserId, hsGroupId);
            return null;
        } else {
            logger.info("Created new user id={} groupId={}", hsUserId, hsGroupId);
            return user;
        }

    }

    /**
     * Create a bucket in HyperStore under the Account listed in the bucket argument.
     *
     * @param bucket the bucket to create.
     * @param objectLock set to true to enable ObjectLock (requires an ObjectLock license), false for a normal bucket.
     *
     * @throws CloudRuntimeException if ObjectLock was requested but the feature is disabled due to license or any
     *         other failure.
     */
    @Override
    public Bucket createBucket(Bucket bucket, boolean objectLock) {
        String bucketName = bucket.getName();
        long storeId = bucket.getObjectStoreId();
        long accountId = bucket.getAccountId();

        // get an s3client using Account Root User Credentials
        Map<String, String> storeDetails = _storeDetailsDao.getDetails(storeId);
        String s3url = storeDetails.get(CloudianHyperStoreUtil.STORE_DETAILS_KEY_S3_URL);
        Map<String, String> accountDetails = _accountDetailsDao.findDetails(accountId);
        String accessKey = accountDetails.get(CloudianHyperStoreUtil.KEY_ROOT_ACCESS_KEY);
        String secretKey = accountDetails.get(CloudianHyperStoreUtil.KEY_ROOT_SECRET_KEY);
        String iamAccessKey = accountDetails.get(CloudianHyperStoreUtil.KEY_IAM_ACCESS_KEY);
        String iamSecretKey = accountDetails.get(CloudianHyperStoreUtil.KEY_IAM_SECRET_KEY);
        AmazonS3 s3client = getS3Client(s3url, accessKey, secretKey);

        // Step 1: Create the bucket
        try {
            // Create the bucket with ObjectLock if requested
            logger.info("Creating bucket {}", bucketName);
            CreateBucketRequest cbRequest = new CreateBucketRequest(bucketName);
            cbRequest.setObjectLockEnabledForBucket(objectLock);
            s3client.createBucket(cbRequest);
        } catch (AmazonClientException e) {
            logger.error("Create bucket failed", e);
            throw new CloudRuntimeException(e);
        }

        // Step 2: Any Exception here, we try to delete the bucket.
        // If deletion fails, it is not the end of the world as the
        // user can try again to create the bucket which if he is
        // already the owner, it will succeed.
        try {
            // Enable a permissive CORS configuration
            configureBucketCORS(s3client, bucketName);

            // Update the Bucket Information (for Bucket details page etc)
            BucketVO bucketVO = _bucketDao.findById(bucket.getId());
            bucketVO.setAccessKey(iamAccessKey);
            bucketVO.setSecretKey(iamSecretKey);
            bucketVO.setBucketURL(s3url + "/" + bucketName);
            _bucketDao.update(bucket.getId(), bucketVO);
            return bucketVO;
        } catch (Exception e) {
            // Error with DB or CORS. Delete the bucket from S3
            logger.error("There was a failure after bucket creation. Trying to clean up", e);
            try {
                s3client.deleteBucket(bucketName);
                logger.info("cleanup succeeded.");
            } catch (AmazonClientException e1) {
                logger.error("Cleanup for create bucket also failed with", e);
            }
            throw new CloudRuntimeException(e);
        }
    }

    /**
     * Configure a permissive CrossOrigin setting on the given bucket.
     *
     * Cloudian does not enable CORS by default. The CORS configuration
     * is required by CloudStack so that the Javascript S3 bucket
     * browser can function properly.
     *
     * This method does not catch any exceptions which should be caught
     * by the calling method.
     *
     * @param s3client bucket owner s3client
     * @param bucketName the bucket name.
     *
     * @throws AmazonClientException and derivatives
     */
    private void configureBucketCORS(AmazonS3 s3client, String bucketName) {
        logger.debug("Configuring CORS for bucket {}", bucketName);

        List<CORSRule> corsRules = new ArrayList<CORSRule>();
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

    /**
     * This API seems to be called by the StorageManagementImpl to validate that the
     * main Object Store URL (in our case the Admin API endpoint) is correct. As
     * such, let's return all buckets owned by accounts managed by this object
     * store using the same API as the bucket usage as that uses the ADMIN API.
     *
     * @return a list of Bucket objects where only the bucketName is set.
     */
    @Override
    public List<Bucket> listBuckets(long storeId) {
        Map<String, Long> bucketUsage = getAllBucketsUsage(storeId);
        List<Bucket> bucketList = new ArrayList<Bucket>();
        for (String bucketName : bucketUsage.keySet()) {
            Bucket bucket = new BucketObject();
            bucket.setName(bucketName);
            bucketList.add(bucket);
        }
        return bucketList;
    }

    /**
     * Delete an empty bucket.
     * This operation fails if the bucket is not empty.
     * @param bucket the bucket to delete
     * @param storeId the store the bucket belongs to.
     * @returns true on success or throws an exception.
     * @throws CloudRuntimeException if the bucket deletion fails
     */
    @Override
    public boolean deleteBucket(BucketTO bucket, long storeId) {
        AmazonS3 s3client = getS3ClientByBucketAndStore(bucket, storeId);
        logger.debug("Deleting bucket {}", bucket.getName());
        try {
            s3client.deleteBucket(bucket.getName());
            logger.info("Successfully deleted bucket {}", bucket.getName());
            return true;
        } catch (AmazonClientException e) {
            logger.error("Failed to delete bucket " + bucket.getName(), e);
            throw new CloudRuntimeException(e);
        }
    }

    @Override
    public AccessControlList getBucketAcl(BucketTO bucket, long storeId) {
        AmazonS3 s3client = getS3ClientByBucketAndStore(bucket, storeId);
        logger.debug("Getting the bucket ACL for {}", bucket.getName());
        try {
            AccessControlList acl = s3client.getBucketAcl(bucket.getName());
            logger.info("Successfully got the bucket ACL for {}", bucket.getName());
            return acl;
        } catch (AmazonClientException e) {
            logger.error("Failed to get the bucket ACL for " + bucket.getName(), e);
            throw new CloudRuntimeException(e);
        }
    }

    @Override
    public void setBucketAcl(BucketTO bucket, AccessControlList acl, long storeId) {
        AmazonS3 s3client = getS3ClientByBucketAndStore(bucket, storeId);
        logger.debug("Setting the bucket ACL for {}", bucket.getName());
        try {
            s3client.setBucketAcl(bucket.getName(), acl);
            logger.info("Successfully set the bucket ACL for {}", bucket.getName());
            return;
        } catch (AmazonClientException e) {
            logger.error("Failed to set the bucket ACL for " + bucket.getName(), e);
            throw new CloudRuntimeException(e);
        }
    }

    /**
     * Set the bucket policy to either "public" or "private".
     * If set to private, we delete any existing policy.
     * For public, we allow objects to be read but not listed.
     */
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

        AmazonS3 s3client = getS3ClientByBucketAndStore(bucket, storeId);
        logger.debug("Setting the bucket policy to {} for {}", policy, bucket.getName());
        try {
            s3client.setBucketPolicy(bucket.getName(), jsonPolicy);
            logger.info("Successfully set the bucket policy to {} for {}", policy, bucket.getName());
            return;
        } catch (AmazonClientException e) {
            logger.error("Failed to set the bucket policy for " + bucket.getName(), e);
            throw new CloudRuntimeException(e);
        }
    }

    @Override
    public BucketPolicy getBucketPolicy(BucketTO bucket, long storeId) {
        AmazonS3 s3client = getS3ClientByBucketAndStore(bucket, storeId);
        logger.debug("Getting the bucket policy for {}", bucket.getName());
        try {
            BucketPolicy bp = s3client.getBucketPolicy(bucket.getName());
            logger.info("Successfully got the bucket policy for {}", bucket.getName());
            return bp;
        } catch (AmazonClientException e) {
            logger.error("Failed to get the bucket policy for " + bucket.getName(), e);
            throw new CloudRuntimeException(e);
        }
    }

    @Override
    public void deleteBucketPolicy(BucketTO bucket, long storeId) {
        AmazonS3 s3client = getS3ClientByBucketAndStore(bucket, storeId);
        logger.debug("Deleting bucket policy for {}", bucket.getName());
        try {
            s3client.deleteBucketPolicy(bucket.getName());
            logger.info("Successfully deleted bucket policy for {}", bucket.getName());
            return;
        } catch (AmazonClientException e) {
            logger.error("Failed to delete bucket policy for " + bucket.getName(), e);
            throw new CloudRuntimeException(e);
        }
    }

    @Override
    public boolean setBucketEncryption(BucketTO bucket, long storeId) {
        AmazonS3 s3client = getS3ClientByBucketAndStore(bucket, storeId);
        logger.debug("Enabling bucket encryption configuration for {}", bucket.getName());
        try {
            SetBucketEncryptionRequest eRequest = new SetBucketEncryptionRequest();
            eRequest.setBucketName(bucket.getName());

            ServerSideEncryptionByDefault sseByDefault = new ServerSideEncryptionByDefault();
            sseByDefault.setSSEAlgorithm(SSEAlgorithm.AES256.toString());

            ServerSideEncryptionRule sseRule = new ServerSideEncryptionRule();
            sseRule.setApplyServerSideEncryptionByDefault(sseByDefault);

            List<ServerSideEncryptionRule> sseRules = new ArrayList<ServerSideEncryptionRule>();
            sseRules.add(sseRule);

            ServerSideEncryptionConfiguration sseConf = new ServerSideEncryptionConfiguration();
            sseConf.setRules(sseRules);

            eRequest.setServerSideEncryptionConfiguration(sseConf);
            s3client.setBucketEncryption(eRequest);

            logger.info("Successfully enabled bucket encryption configuration for {}", bucket.getName());
            return true;
        } catch (AmazonClientException e) {
            logger.error("Failed to enable bucket encryption configuration for " + bucket.getName(), e);
            throw new CloudRuntimeException(e);
        }
    }

    @Override
    public boolean deleteBucketEncryption(BucketTO bucket, long storeId) {
        AmazonS3 s3client = getS3ClientByBucketAndStore(bucket, storeId);
        logger.debug("Deleting bucket encryption configuration for {}", bucket.getName());
        try {
            s3client.deleteBucketEncryption(bucket.getName());
            logger.info("Successfully deleted bucket encryption configuration for {}", bucket.getName());
            return true;
        } catch (AmazonClientException e) {
            logger.error("Failed to delete bucket encryption configuration for " + bucket.getName(), e);
            throw new CloudRuntimeException(e);
        }
    }

    @Override
    public boolean setBucketVersioning(BucketTO bucket, long storeId) {
        AmazonS3 s3client = getS3ClientByBucketAndStore(bucket, storeId);
        logger.debug("Enabling versioning for bucket {}", bucket.getName());
        try {
            BucketVersioningConfiguration vConf = new BucketVersioningConfiguration(BucketVersioningConfiguration.ENABLED);
            SetBucketVersioningConfigurationRequest vRequest = new SetBucketVersioningConfigurationRequest(bucket.getName(), vConf);
            s3client.setBucketVersioningConfiguration(vRequest);
            logger.info("Successfully enabled versioning for bucket {}", bucket.getName());
            return true;
        } catch (AmazonClientException e) {
            logger.error("Failed to enable versioning for bucket " + bucket.getName(), e);
            throw new CloudRuntimeException(e);
        }
    }

    @Override
    public boolean deleteBucketVersioning(BucketTO bucket, long storeId) {
        AmazonS3 s3client = getS3ClientByBucketAndStore(bucket, storeId);
        logger.debug("Suspending versioning for bucket {}", bucket.getName());
        try {
            BucketVersioningConfiguration vConf = new BucketVersioningConfiguration(BucketVersioningConfiguration.SUSPENDED);
            SetBucketVersioningConfigurationRequest vRequest = new SetBucketVersioningConfigurationRequest(bucket.getName(), vConf);
            s3client.setBucketVersioningConfiguration(vRequest);
            logger.info("Successfully suspended versioning for bucket {}", bucket.getName());
            return true;
        } catch (AmazonClientException e) {
            logger.error("Failed to suspend versioning for bucket " + bucket.getName(), e);
            throw new CloudRuntimeException(e);
        }
    }

    /**
     * Set the bucket quota to a size limit specified in GiB.
     *
     * Cloudian HyperStore does not currently support bucket quota limits.
     * CloudStack itself requires a quota to be set. HyperStore may add
     * Bucket Quota support in a future version. Currently, we only support
     * setting the quota to zero to indicate no quota.
     *
     * @param bucket the bucket
     * @param storeId the store
     * @param size the GiB (1024^3) size to set the quota to. Only 0 is supported.
     * @throws CloudRuntimeException is thrown for any other value other than 0.
     */
    @Override
    public void setBucketQuota(BucketTO bucket, long storeId, long size) {
        if (size == 0) {
            logger.debug("Bucket \"{}\" quota set to 0 (no quota).", bucket.getName());
            return;
        }
        // Any other setting, throw an exception.
        logger.warn("Unable to set quota for bucket \"{}\" to {}GiB. Only 0 is supported.", bucket.getName(), size);
        throw new CloudRuntimeException("This bucket does not support a quota. Use 0 to specify no quota.");
    }

    /**
     * Return a map of bucket names managed by this store and their sizes (in bytes).
     *
     * Note: Bucket Usage Statistics in HyperStore are disabled by default. They
     * can be enabled by the HyperStore Administrator by setting of the configuration
     * 's3.qos.bucketLevel=true'. If this is not enabled, the values returned will
     * either be 0 or out of date.
     *
     * @return map of bucket names to usage bytes.
     */
    @Override
    public Map<String, Long> getAllBucketsUsage(long storeId) {
        Map<String, Long> bucketUsage = new HashMap<String, Long>();
        List<BucketVO> bucketList = _bucketDao.listByObjectStoreId(storeId);
        if (bucketList.isEmpty()) {
            return bucketUsage;
        }

        // Create an unique list of domains from the bucket list
        // and add all the bucket names to the bucketUsage map with value -1 as a marker
        // to know which buckets CloudStack cares about. The -1 will be replaced later.
        List<Long> domainIds = new ArrayList<Long>();
        for (BucketVO bucket : bucketList) {
            long bucketDomainId = bucket.getDomainId();
            if (! domainIds.contains(bucketDomainId)) {
                domainIds.add(bucketDomainId);
            }
            bucketUsage.put(bucket.getName(), -1L);
        }

        // Ask for bucket usages per domain (ie. per HyperStore Group)
        CloudianClient client = getCloudianClientByStoreId(storeId);
        for (long domainId : domainIds) {
            Domain domain = _domainDao.findById(domainId);
            final String hsGroupId = getHyperStoreGroupId(domain);
            List<CloudianUserBucketUsage> groupBucketUsages = client.getUserBucketUsages(hsGroupId, null, null);
            for (CloudianUserBucketUsage userBucketUsages : groupBucketUsages) {
                for (CloudianBucketUsage cbu : userBucketUsages.getBuckets()) {
                    if (cbu.getByteCount() >= 0L) {
                        // Update the -1 entry to actual byteCount.
                        bucketUsage.replace(cbu.getBucketName(), cbu.getByteCount());
                    } else {
                        // Replace with 0 instead of actual value. Race condition can cause this and it
                        // should be fixed automatically by a repair job.
                        bucketUsage.replace(cbu.getBucketName(), 0L);
                        logger.info("Ignoring negative bucket usage for \"{}\": {}", cbu.getBucketName(), cbu.getByteCount());
                    }
                }
            }
        }

        // Remove any remaining -1 entries. These would probably be buckets that were
        // deleted outside of CloudStack control. A missing entry might be better than
        // returning the bucket name with -1 or 0.
        bucketUsage.entrySet().removeIf(entry -> entry.getValue() == -1);

        return bucketUsage;
    }

    /**
     * Get a connection to the Cloudian HyperStore ADMIN API Service.
     * @param storeId the object store containing connection info for HyperStore
     * @return a connection object (never null)
     * @throws CloudRuntimeException if the connection fails
     */
    protected CloudianClient getCloudianClientByStoreId(long storeId) {
        ObjectStoreVO store = _storeDao.findById(storeId);
        String url = store.getUrl();
        Map<String, String> storeDetails = _storeDetailsDao.getDetails(storeId);
        String adminUsername = storeDetails.get(CloudianHyperStoreUtil.STORE_DETAILS_KEY_USER_NAME);
        String adminPassword = storeDetails.get(CloudianHyperStoreUtil.STORE_DETAILS_KEY_PASSWORD);
        String strValidateSSL = storeDetails.get(CloudianHyperStoreUtil.STORE_DETAILS_KEY_VALIDATE_SSL);
        boolean validateSSL = Boolean.parseBoolean(strValidateSSL);

        return CloudianHyperStoreUtil.getCloudianClient(url, adminUsername, adminPassword, validateSSL);
    }

    /**
     * Returns an S3 connection for the store and account identified by the bucket.
     * NOTE: https connections must use a trusted certificate.
     *
     * @param store the object store of the S3 service to connect to
     * @param bucket bucket information identifying the account which identifies the credentials to use.
     * @return an S3 connection (never null)
     * @throws CloudRuntimeException on failure.
     */
    protected AmazonS3 getS3ClientByBucketAndStore(BucketTO bucket, long storeId) {
        // Find the S3 Root user credentials of the Account Owner rather than using the
        // credentials stored with the bucket which may be IAM User Credentials.
        for (BucketVO bvo : _bucketDao.listByObjectStoreId(storeId)) {
            if (bvo.getName().equals(bucket.getName())) {
                long accountId = bvo.getAccountId();
                Map<String, String> storeDetails = _storeDetailsDao.getDetails(storeId);
                String s3url = storeDetails.get(CloudianHyperStoreUtil.STORE_DETAILS_KEY_S3_URL);
                String accessKey = _accountDetailsDao.findDetail(accountId, CloudianHyperStoreUtil.KEY_ROOT_ACCESS_KEY).getValue();
                String secretKey = _accountDetailsDao.findDetail(accountId, CloudianHyperStoreUtil.KEY_ROOT_SECRET_KEY).getValue();
                logger.debug("Creating S3 connection to {} for {} ", s3url, accessKey);
                return CloudianHyperStoreUtil.getS3Client(s3url, accessKey, secretKey);
            }
        }
        throw new CloudRuntimeException(String.format("Bucket Name not found: %s", bucket.getName()));
    }

     /**
     * Returns an S3 connection for the given endpoint and credentials.
     * NOTE: https connections must use a trusted certificate.
     * NOTE: The only reason this wrapper method is here is for unit test mocking.
     *
     * @param s3url the url of the S3 service
     * @param accessKey the credentials to use for the S3 connection.
     * @param secretKey the matching secret key.
     * @return an S3 connection (never null)
     * @throws CloudRuntimeException on failure.
     */
    protected AmazonS3 getS3Client(String s3url, String accessKey, String secretKey) {
        return CloudianHyperStoreUtil.getS3Client(s3url, accessKey, secretKey);
    }

    /**
     * Returns an IAM connection for the given store using the given credentials.
     * NOTE: if the store uses https, it must use a trusted certificate.
     * NOTE: HyperStore IAM service is usually found on ports 16080/16443.
     *
     * @param storeId the object store
     * @param credential the credential pair to use for the iam connection.
     * @return an IAM connection (never null)
     * @throws CloudRuntimeException on failure.
     */
    protected AmazonIdentityManagement getIAMClientByStoreId(long storeId, CloudianCredential credential) {
        Map<String, String> storeDetails = _storeDetailsDao.getDetails(storeId);
        String iamUrl = storeDetails.get(CloudianHyperStoreUtil.STORE_DETAILS_KEY_IAM_URL);
        logger.debug("Creating a new IAM connection to {} for {}", iamUrl, credential.getAccessKey());

        return CloudianHyperStoreUtil.getIAMClient(iamUrl, credential.getAccessKey(), credential.getSecretKey());
    }

}

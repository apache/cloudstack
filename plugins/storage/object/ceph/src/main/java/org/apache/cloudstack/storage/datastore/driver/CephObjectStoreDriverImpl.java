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

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClientBuilder;
import com.amazonaws.services.identitymanagement.model.AccessKey;
import com.amazonaws.services.identitymanagement.model.AccessKeyMetadata;
import com.amazonaws.services.identitymanagement.model.CreateAccessKeyRequest;
import com.amazonaws.services.identitymanagement.model.CreateUserRequest;
import com.amazonaws.services.identitymanagement.model.DeleteAccessKeyRequest;
import com.amazonaws.services.identitymanagement.model.DeleteUserPolicyRequest;
import com.amazonaws.services.identitymanagement.model.DeleteUserRequest;
import com.amazonaws.services.identitymanagement.model.EntityAlreadyExistsException;
import com.amazonaws.services.identitymanagement.model.ListAccessKeysRequest;
import com.amazonaws.services.identitymanagement.model.ListUserPoliciesRequest;
import com.amazonaws.services.identitymanagement.model.NoSuchEntityException;
import com.amazonaws.services.identitymanagement.model.PutUserPolicyRequest;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.AccessControlList;
import com.amazonaws.services.s3.model.BucketPolicy;
import com.amazonaws.services.s3.model.BucketVersioningConfiguration;
import com.amazonaws.services.s3.model.DeleteBucketPolicyRequest;
import com.amazonaws.services.s3.model.SetBucketPolicyRequest;
import com.amazonaws.services.s3.model.GetBucketPolicyRequest;
import com.amazonaws.services.s3.model.SetBucketVersioningConfigurationRequest;
import com.cloud.agent.api.to.BucketCredentialTO;
import com.cloud.agent.api.to.BucketKeyTO;
import com.cloud.agent.api.to.BucketTO;
import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.utils.crypt.DBEncryptionUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.cloudstack.storage.object.Bucket;
import org.apache.cloudstack.storage.object.ObjectStore;
import com.cloud.storage.BucketVO;
import com.cloud.storage.dao.BucketDao;
import com.cloud.user.Account;
import com.cloud.user.AccountDetailVO;
import com.cloud.user.AccountDetailsDao;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.storage.datastore.db.ObjectStoreDao;
import org.apache.cloudstack.storage.datastore.db.ObjectStoreDetailsDao;
import org.apache.cloudstack.storage.datastore.db.ObjectStoreVO;
import org.apache.cloudstack.storage.datastore.util.RgwAccountClient;
import org.apache.cloudstack.storage.datastore.util.RgwIamSigner;
import org.apache.cloudstack.storage.object.BaseObjectStoreDriverImpl;
import org.apache.cloudstack.storage.object.BucketApiService;
import org.apache.cloudstack.storage.object.BucketObject;
import org.twonote.rgwadmin4j.RgwAdmin;
import org.twonote.rgwadmin4j.RgwAdminBuilder;
import org.twonote.rgwadmin4j.impl.RgwAdminException;
import org.twonote.rgwadmin4j.model.BucketInfo;
import org.twonote.rgwadmin4j.model.S3Credential;
import org.twonote.rgwadmin4j.model.User;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class CephObjectStoreDriverImpl extends BaseObjectStoreDriverImpl {

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

    private static final String ACCESS_KEY = "accesskey";
    private static final String SECRET_KEY = "secretkey";

    // Legacy account-scoped RGW user: uid = CloudStack account UUID, keys stored in clear.
    private static final String CEPH_ACCESS_KEY = "ceph-rgw-accesskey";
    private static final String CEPH_SECRET_KEY = "ceph-rgw-secretkey";

    // RGW account mode (Squid+): the CloudStack account maps to an RGW account whose root user
    // is the same uid as the legacy user. Presence of the account id is the never-fall-back marker.
    // The rows are keyed per object store ("objectstore-<storeId>-account-id" and so on): an account
    // can be migrated on one Ceph store and still be legacy on another. That namespace is
    // CloudStack's own (see ObjectStore.ACCOUNT_DETAIL_PREFIX) and is kept out of API responses,
    // unlike the legacy rows above, which existing behaviour still exposes.
    protected static final String CEPH_ACCOUNT_ID = "account-id";
    protected static final String CEPH_ROOT_ACCESS_KEY = "root-accesskey";
    protected static final String CEPH_ROOT_SECRET_KEY = "root-secretkey";
    // set once the pre-migration account key has been rotated away; a brand-new account is
    // created with a key nobody else ever held, so it is marked rotated from the start
    protected static final String CEPH_ROOT_KEY_ROTATED = "root-key-rotated";

    // Bounds on the IAM calls, which run under the per-account-per-store lock. The SDK's own
    // defaults are a 50s socket timeout with three retries and no ceiling on the call as a whole.
    private static final int IAM_SOCKET_TIMEOUT_MILLIS = 20000;
    private static final int IAM_MAX_ERROR_RETRY = 1;
    private static final int IAM_CALL_TIMEOUT_MILLIS = 30000;

    protected static String detailKey(long storeId, String name) {
        return ObjectStore.accountDetailKey(storeId, name);
    }

    /** The account-mode rows for this store, with the store prefix stripped from the keys. */
    protected Map<String, String> accountModeDetails(long accountId, long storeId) {
        String prefix = ObjectStore.accountDetailPrefix(storeId);
        Map<String, String> scoped = new HashMap<>();
        for (Map.Entry<String, String> entry : _accountDetailsDao.findDetails(accountId).entrySet()) {
            if (entry.getKey().startsWith(prefix)) {
                scoped.put(entry.getKey().substring(prefix.length()), entry.getValue());
            }
        }
        return scoped;
    }

    /**
     * Merge the store's account-mode rows into the account's details. {@code update} keeps every
     * other row (other stores, unrelated details); {@code persist} would replace the whole map.
     */
    protected void persistAccountModeDetails(long accountId, long storeId, Map<String, String> details) {
        Map<String, String> prefixed = new HashMap<>();
        for (Map.Entry<String, String> entry : details.entrySet()) {
            prefixed.put(detailKey(storeId, entry.getKey()), entry.getValue());
        }
        _accountDetailsDao.update(accountId, prefixed);
    }

    protected static final String BUCKET_POLICY_NAME = "cloudstack-bucket-access";
    private static final long ACCOUNT_SUPPORT_CACHE_MILLIS = 5 * 60 * 1000L;

    private final Map<Long, long[]> accountSupportCache = new ConcurrentHashMap<>();
    private final Map<Long, String[]> unsupportedReasonCache = new ConcurrentHashMap<>();

    @Override
    public DataStoreTO getStoreTO(DataStore store) {
        return null;
    }

    @Override
    public Bucket createBucket(Bucket bucket, boolean objectLock) {
        String bucketName = bucket.getName();
        long storeId = bucket.getObjectStoreId();
        long accountId = bucket.getAccountId();
        AmazonS3 s3client = getS3Client(storeId, accountId);

        try {
            if (s3client.doesBucketExistV2(bucketName)) {
                throw new CloudRuntimeException("Bucket already exists with the name: " + bucketName);
            }
        } catch (AmazonS3Exception e) {
            if (e.getStatusCode() != 404) {
                throw new CloudRuntimeException(e);
            }
        } catch (Exception e) {
            throw new CloudRuntimeException(e);
        }
        try {
            s3client.createBucket(bucketName);
            ObjectStoreVO store = _storeDao.findById(storeId);
            BucketVO bucketVO = _bucketDao.findById(bucket.getId());
            if (accountSupportsBucketCredentials(accountId, storeId)) {
                // The bucket is about to get a credential of its own, which the service mirrors
                // onto the row. The account's root key must never touch the row even briefly:
                // every user of the account can list it there, and that key opens every bucket.
                logger.debug("Bucket {} of a migrated account is created without the account key; its own credential follows", bucketName);
            } else {
                BucketKeyTO accountKey = getAccountKey(accountId, storeId);
                bucketVO.setAccessKey(accountKey.getAccessKey());
                bucketVO.setSecretKey(accountKey.getSecretKey());
            }
            bucketVO.setBucketURL(store.getUrl() + "/" + bucketName);
            _bucketDao.update(bucket.getId(), bucketVO);
            return bucketVO;
        } catch (Exception e) {
            throw new CloudRuntimeException(e);
        }
    }

    @Override
    public List<Bucket> listBuckets(long storeId) {
        RgwAdmin rgwAdmin = getRgwAdminClient(storeId);
        List<Bucket> bucketsList = new ArrayList<>();
        try {
            List<String> buckets = rgwAdmin.listBucket();
            for(String name : buckets) {
                Bucket bucket = new BucketObject();
                bucket.setName(name);
                bucketsList.add(bucket);
            }
        } catch (Exception e) {
            throw new CloudRuntimeException(e);
        }
        return bucketsList;
    }

    @Override
    public boolean deleteBucket(BucketTO bucket, long storeId) {
        RgwAdmin rgwAdmin = getRgwAdminClient(storeId);

        try {
            rgwAdmin.removeBucket(bucket.getName());
        } catch (RgwAdminException e) {
            if (e.status() == 404) {
                logger.info("Bucket {} no longer exists in Ceph RGW; treating removal as done", bucket.getName());
                return true;
            }
            throw new CloudRuntimeException(e);
        } catch (Exception e) {
            throw new CloudRuntimeException(e);
        }
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
        writeBucketPolicy(bucket, storeId, policy, bucket.getProviderCredentialId());
    }

    /**
     * The bucket policy is shared between two features: CloudStack's public/private access
     * setting and the grant that lets a bucket's dedicated IAM user reach objects the account
     * root wrote before the account migration (an identity policy alone does not cover those,
     * because they are still owned by the legacy user id). Always regenerate the whole document
     * from both inputs so neither feature overwrites the other.
     */
    protected void writeBucketPolicy(BucketTO bucket, long storeId, String policy, String credentialUserName) {
        JsonArray statements = new JsonArray();
        String bucketArn = "arn:aws:s3:::" + bucket.getName();
        if (policy != null && policy.equalsIgnoreCase("public")) {
            logger.debug("Setting public policy on bucket " + bucket.getName());
            statements.add(statement("*", new String[] {"s3:GetBucketLocation", "s3:ListBucket"}, new String[] {bucketArn}));
            statements.add(statement("*", new String[] {"s3:GetObject"}, new String[] {bucketArn + "/*"}));
        } else {
            logger.debug("Setting private policy on bucket " + bucket.getName());
        }
        if (credentialUserName != null) {
            String accountId = accountModeDetails(bucket.getAccountId(), storeId).get(CEPH_ACCOUNT_ID);
            String principal = "arn:aws:iam::" + accountId + ":user/" + credentialUserName;
            statements.add(statement(principal, new String[] {"s3:*"}, new String[] {bucketArn, bucketArn + "/*"}));
        }
        JsonObject document = new JsonObject();
        document.addProperty("Version", "2012-10-17");
        document.add("Statement", statements);

        AmazonS3 client = getS3Client(storeId, bucket.getAccountId());
        client.setBucketPolicy(new SetBucketPolicyRequest(bucket.getName(), document.toString()));
    }

    private static JsonObject statement(String principal, String[] actions, String[] resources) {
        JsonObject statement = new JsonObject();
        statement.addProperty("Effect", "Allow");
        if ("*".equals(principal)) {
            statement.addProperty("Principal", "*");
        } else {
            JsonObject aws = new JsonObject();
            aws.addProperty("AWS", principal);
            statement.add("Principal", aws);
        }
        JsonArray actionArray = new JsonArray();
        for (String action : actions) {
            actionArray.add(action);
        }
        statement.add("Action", actionArray);
        JsonArray resourceArray = new JsonArray();
        for (String resource : resources) {
            resourceArray.add(resource);
        }
        statement.add("Resource", resourceArray);
        return statement;
    }

    @Override
    public BucketPolicy getBucketPolicy(BucketTO bucket, long storeId) {
        AmazonS3 client = getS3Client(getStoreURL(storeId), bucket.getAccessKey(), bucket.getSecretKey());
        return client.getBucketPolicy(new GetBucketPolicyRequest(bucket.getName()));
    }

    @Override
    public void deleteBucketPolicy(BucketTO bucket, long storeId) {
        AmazonS3 client = getS3Client(getStoreURL(storeId), bucket.getAccessKey(), bucket.getSecretKey());
        client.deleteBucketPolicy(new DeleteBucketPolicyRequest(bucket.getName()));
    }

    /**
     * Ensure the account's backend identity exists. An account already in RGW account mode, or
     * one that already has a legacy user, keeps what it has. A brand-new account is created in
     * account mode when the store and the global setting allow it, otherwise as a legacy user.
     * Existing legacy accounts are never moved into account mode here; that is the explicit,
     * irreversible {@link #migrateAccountForBucketCredentials} operation.
     */
    @Override
    public boolean createUser(long accountId, long storeId) {
        if (accountModeDetails(accountId, storeId).containsKey(CEPH_ACCOUNT_ID)) {
            return true;
        }
        if (_accountDetailsDao.findDetails(accountId).containsKey(CEPH_ACCESS_KEY)) {
            return ensureLegacyUser(accountId, storeId);
        }
        if (BucketApiService.PerBucketCredentials.value() && supportsBucketCredentials(storeId)) {
            return migrateAccountForBucketCredentials(accountId, storeId);
        }
        return ensureLegacyUser(accountId, storeId);
    }

    private boolean ensureLegacyUser(long accountId, long storeId) {
        Account account = _accountDao.findById(accountId);
        RgwAdmin rgwAdmin = getRgwAdminClient(storeId);
        String username = account.getUuid();

        logger.debug("Attempting to create Ceph RGW user for account {} with UUID {}", account, username);
        try {
            Optional<User> user = rgwAdmin.getUserInfo(username);
            if (user.isPresent()) {
                logger.info("User already exists in Ceph RGW: " + username);
                return true;
            } else {
                logger.debug("User does not exist. Creating user in Ceph RGW: " + username);
            }
        } catch (Exception e) {
            logger.debug("Get user info failed for user {} with exception {}. Proceeding with user creation.",  username, e.getMessage());
        }

        try {
            rgwAdmin.createUser(username);
            User newUser = rgwAdmin.getUserInfo(username).get();
            S3Credential credentials = newUser.getS3Credentials().get(0);

            Map<String, String> details = new HashMap<>();
            details.put(CEPH_ACCESS_KEY, credentials.getAccessKey());
            details.put(CEPH_SECRET_KEY, credentials.getSecretKey());
            _accountDetailsDao.persist(accountId, details);
            return true;
        } catch (Exception e) {
            throw new CloudRuntimeException(e);
        }
    }

    @Override
    public boolean supportsBucketCredentials(long storeId) {
        long[] cached = accountSupportCache.get(storeId);
        long now = System.currentTimeMillis();
        if (cached != null && cached[0] == 1 && now - cached[1] < ACCOUNT_SUPPORT_CACHE_MILLIS) {
            return true;
        }
        if (cached != null && cached[0] == 0 && now - cached[1] < ACCOUNT_SUPPORT_CACHE_MILLIS) {
            return false;
        }
        boolean supported;
        try {
            RgwAccountClient accountClient = getRgwAccountClient(storeId);
            supported = accountClient.isAvailable() && accountClient.hasAccountsWriteCapability();
        } catch (Exception e) {
            logger.debug("Ceph RGW account API probe failed for store {}: {}", storeId, e.getMessage());
            supported = false;
        }
        logger.debug("Ceph RGW store {} {} the account API needed for per-bucket credentials", storeId, supported ? "supports" : "does not support");
        accountSupportCache.put(storeId, new long[] {supported ? 1 : 0, now});
        return supported;
    }

    @Override
    public String bucketCredentialsUnsupportedReason(long storeId) {
        String[] cached = unsupportedReasonCache.get(storeId);
        long now = System.currentTimeMillis();
        if (cached != null && now - Long.parseLong(cached[1]) < ACCOUNT_SUPPORT_CACHE_MILLIS) {
            return cached[0].isEmpty() ? null : cached[0];
        }
        String reason;
        try {
            reason = getRgwAccountClient(storeId).unsupportedReason();
        } catch (Exception e) {
            reason = "the object store's admin API could not be reached: " + e.getMessage();
        }
        // listing the stores asks this of every one of them, so keep it as briefly as the probe
        unsupportedReasonCache.put(storeId, new String[] {reason == null ? "" : reason, Long.toString(now)});
        return reason;
    }

    @Override
    public boolean isAccountKeyRotationPending(long accountId, long storeId) {
        Map<String, String> details = accountModeDetails(accountId, storeId);
        return details.containsKey(CEPH_ACCOUNT_ID) && !details.containsKey(CEPH_ROOT_KEY_ROTATED);
    }

    /**
     * Whether this account has been migrated to an RGW account on this store. Migration is
     * permanent on the backend, so the recorded marker wins over the live support probe: a probe
     * that fails or flaps during a gateway upgrade must never downgrade a migrated account, which
     * would put its new buckets back on the shared account key. The gateway still overrules a
     * stale record, but only when it answers; when it cannot be asked, the record stands.
     */
    @Override
    public boolean accountSupportsBucketCredentials(long accountId, long storeId) {
        AccountDetailVO accountDetail = _accountDetailsDao.findDetail(accountId, detailKey(storeId, CEPH_ACCOUNT_ID));
        if (accountDetail == null) {
            return false;
        }
        Account account = _accountDao.findById(accountId);
        if (account == null) {
            return false;
        }
        try {
            boolean rootOnGateway = getRgwAccountClient(storeId).isAccountRootUser(account.getUuid());
            if (!rootOnGateway) {
                logger.warn("Account {} is recorded as migrated on Ceph RGW store {} but its user is not an account root on the gateway; treating it as not migrated", account, storeId);
            }
            return rootOnGateway;
        } catch (Exception e) {
            logger.warn("Unable to confirm with Ceph RGW store {} whether account {} is an account root; keeping its recorded migrated state", storeId, account, e);
            return true;
        }
    }

    /**
     * Create the RGW account for the CloudStack account and make its RGW user the account root.
     * An existing legacy user is adopted (its buckets move to the account, its keys keep working);
     * a missing one is created directly in the account. Idempotent; permanent on the backend.
     */
    @Override
    public boolean migrateAccountForBucketCredentials(long accountId, long storeId) {
        if (accountModeDetails(accountId, storeId).containsKey(CEPH_ACCOUNT_ID)) {
            logger.debug("Account {} is already in Ceph RGW account mode on store {}", accountId, storeId);
            return true;
        }
        if (!supportsBucketCredentials(storeId)) {
            throw new CloudRuntimeException("The Ceph RGW object store does not support accounts (requires Ceph Squid or later and the accounts=write admin capability)");
        }
        Account account = _accountDao.findById(accountId);
        String uid = account.getUuid();
        RgwAdmin rgwAdmin = getRgwAdminClient(storeId);
        RgwAccountClient accountClient = getRgwAccountClient(storeId);

        logger.info("Ensuring Ceph RGW account for CloudStack account {}", account);
        RgwAccountClient.RgwAccount rgwAccount = accountClient.createAccount(RgwAccountClient.accountIdFor(uid), uid);

        Map<String, String> userParams = new HashMap<>();
        userParams.put("account-id", rgwAccount.getId());
        userParams.put("account-root", "true");
        User rootUser;
        boolean adopted = false;
        try {
            Optional<User> existing = rgwAdmin.getUserInfo(uid);
            if (existing.isPresent()) {
                logger.info("Adopting Ceph RGW user {} into account {} as its root user", uid, rgwAccount.getId());
                rootUser = rgwAdmin.modifyUser(uid, userParams);
                adopted = true;
            } else {
                logger.info("Creating Ceph RGW root user {} in account {}", uid, rgwAccount.getId());
                userParams.put("display-name", account.getAccountName());
                rootUser = rgwAdmin.createUser(uid, userParams);
            }
            if (rootUser == null || rootUser.getS3Credentials() == null || rootUser.getS3Credentials().isEmpty()) {
                rootUser = rgwAdmin.getUserInfo(uid).orElseThrow(() -> new CloudRuntimeException("Ceph RGW root user " + uid + " not found after creation"));
            }
        } catch (RgwAdminException e) {
            throw new CloudRuntimeException("Unable to set up the Ceph RGW root user for account " + account.getAccountName() + ": " + e.getMessage(), e);
        }

        S3Credential rootKey = rootUser.getS3Credentials().get(0);
        Map<String, String> newDetails = new HashMap<>();
        newDetails.put(CEPH_ACCOUNT_ID, rgwAccount.getId());
        newDetails.put(CEPH_ROOT_ACCESS_KEY, rootKey.getAccessKey());
        newDetails.put(CEPH_ROOT_SECRET_KEY, DBEncryptionUtil.encrypt(rootKey.getSecretKey()));
        if (!adopted) {
            newDetails.put(CEPH_ROOT_KEY_ROTATED, "true");
        }
        persistAccountModeDetails(accountId, storeId, newDetails);
        return true;
    }

    @Override
    public BucketCredentialTO createBucketCredential(BucketTO bucket, long storeId) {
        String userName = bucket.getUuid();
        AmazonIdentityManagement iam = getIamClient(storeId, bucket.getAccountId());
        boolean created = false;
        try {
            iam.createUser(new CreateUserRequest(userName));
            created = true;
            logger.info("Created IAM user {} for bucket {}", userName, bucket.getName());
        } catch (EntityAlreadyExistsException e) {
            logger.debug("IAM user {} for bucket {} already exists", userName, bucket.getName());
        }
        iam.putUserPolicy(new PutUserPolicyRequest(userName, BUCKET_POLICY_NAME, bucketPolicy(bucket.getName())));
        if (!created) {
            // CloudStack tracks no keys for this identity, so anything on it is an orphan from an
            // earlier failed attempt; clear it so the new key is the only one.
            for (AccessKeyMetadata orphan : iam.listAccessKeys(new ListAccessKeysRequest().withUserName(userName)).getAccessKeyMetadata()) {
                logger.info("Removing untracked access key {} from IAM user {}", orphan.getAccessKeyId(), userName);
                iam.deleteAccessKey(new DeleteAccessKeyRequest(userName, orphan.getAccessKeyId()));
            }
        }
        AccessKey key = iam.createAccessKey(new CreateAccessKeyRequest(userName)).getAccessKey();

        BucketVO bucketVO = _bucketDao.findByUuid(bucket.getUuid());
        writeBucketPolicy(bucket, storeId, bucketVO != null ? bucketVO.getPolicy() : null, userName);
        return new BucketCredentialTO(userName, Collections.singletonList(new BucketKeyTO(key.getAccessKeyId(), key.getSecretAccessKey())));
    }

    @Override
    public BucketKeyTO createBucketCredentialKey(BucketTO bucket, long storeId, Set<String> knownAccessKeys) {
        String userName = requireCredentialId(bucket);
        AmazonIdentityManagement iam = getIamClient(storeId, bucket.getAccountId());
        AccessKey key = iam.createAccessKey(new CreateAccessKeyRequest(userName)).getAccessKey();
        return new BucketKeyTO(key.getAccessKeyId(), key.getSecretAccessKey());
    }

    @Override
    public boolean removeBucketCredentialKey(BucketTO bucket, long storeId, String accessKey) {
        String userName = requireCredentialId(bucket);
        AmazonIdentityManagement iam = getIamClient(storeId, bucket.getAccountId());
        try {
            iam.deleteAccessKey(new DeleteAccessKeyRequest(userName, accessKey));
        } catch (NoSuchEntityException e) {
            logger.info("Access key {} of IAM user {} no longer exists; treating removal as done", accessKey, userName);
        }
        return true;
    }

    @Override
    public boolean deleteBucketCredential(BucketTO bucket, long storeId) {
        String userName = requireCredentialId(bucket);
        AmazonIdentityManagement iam = getIamClient(storeId, bucket.getAccountId());
        try {
            for (AccessKeyMetadata key : iam.listAccessKeys(new ListAccessKeysRequest().withUserName(userName)).getAccessKeyMetadata()) {
                iam.deleteAccessKey(new DeleteAccessKeyRequest(userName, key.getAccessKeyId()));
            }
            for (String policyName : iam.listUserPolicies(new ListUserPoliciesRequest().withUserName(userName)).getPolicyNames()) {
                iam.deleteUserPolicy(new DeleteUserPolicyRequest(userName, policyName));
            }
            iam.deleteUser(new DeleteUserRequest(userName));
            logger.info("Deleted IAM user {} of bucket {}", userName, bucket.getName());
        } catch (NoSuchEntityException e) {
            logger.info("IAM user {} of bucket {} no longer exists; treating removal as done", userName, bucket.getName());
        }
        return true;
    }

    /**
     * Issue a fresh key pair for the account's RGW root user, persist it and revoke the old one.
     * Create-before-remove, so CloudStack never loses its own management access: if persisting
     * the new key fails the new key is removed again and the old one stays in force.
     */
    @Override
    public BucketKeyTO rotateAccountKey(long accountId, long storeId) {
        Map<String, String> details = accountModeDetails(accountId, storeId);
        if (!details.containsKey(CEPH_ACCOUNT_ID)) {
            throw new CloudRuntimeException("Account " + accountId + " has not been migrated to a Ceph RGW account on this store yet");
        }
        Account account = _accountDao.findById(accountId);
        // trust the gateway over our own records: the user must really be an RGW account root here
        if (!getRgwAccountClient(storeId).isAccountRootUser(account.getUuid())) {
            throw new CloudRuntimeException("Account " + account.getAccountName() + " is not the root of a Ceph RGW account on this store; migrate the account on this store first");
        }
        String oldAccessKey = details.get(CEPH_ROOT_ACCESS_KEY);
        AmazonIdentityManagement iam = getIamClient(storeId, accountId);

        // An RGW account root is not a listed IAM user (it does not appear in ListUsers and IAM
        // operations addressed to it by UserName - uid or display name alike - fail with
        // NoSuchEntity), so its own keys can only be managed via the self-referential, no-UserName
        // form of these calls, authenticated as the root itself.
        AccessKey newKey = iam.createAccessKey(new CreateAccessKeyRequest()).getAccessKey();
        try {
            Map<String, String> newDetails = new HashMap<>();
            newDetails.put(CEPH_ROOT_ACCESS_KEY, newKey.getAccessKeyId());
            newDetails.put(CEPH_ROOT_SECRET_KEY, DBEncryptionUtil.encrypt(newKey.getSecretAccessKey()));
            newDetails.put(CEPH_ROOT_KEY_ROTATED, "true");
            persistAccountModeDetails(accountId, storeId, newDetails);
        } catch (RuntimeException e) {
            logger.warn("Failed to record the new root key for account {}; removing it from Ceph RGW", account, e);
            try {
                AmazonIdentityManagement iamWithNewKey = getIamClient(storeId, accountId);
                iamWithNewKey.deleteAccessKey(new DeleteAccessKeyRequest().withAccessKeyId(newKey.getAccessKeyId()));
            } catch (Exception cleanup) {
                logger.warn("Failed to remove access key {} of RGW root user of account {}; it needs manual cleanup", newKey.getAccessKeyId(), account, cleanup);
            }
            throw e;
        }

        // the new key is recorded: from here on CloudStack authenticates with it
        AmazonIdentityManagement iamWithNewKey = getIamClient(storeId, accountId);
        if (oldAccessKey != null && !oldAccessKey.equals(newKey.getAccessKeyId())) {
            try {
                iamWithNewKey.deleteAccessKey(new DeleteAccessKeyRequest().withAccessKeyId(oldAccessKey));
            } catch (NoSuchEntityException e) {
                logger.info("Old root access key {} of account {} no longer exists", oldAccessKey, account);
            }
        }
        // Only the key CloudStack issued is removed. Anything else on the root user was put there
        // by someone else, or left by a rotation that was interrupted before it recorded its key,
        // and either way it still opens every bucket of the account. Report it rather than delete
        // it: removing a credential CloudStack did not create could break whatever depends on it.
        try {
            List<String> untracked = new ArrayList<>();
            for (AccessKeyMetadata existing : iamWithNewKey.listAccessKeys(new ListAccessKeysRequest()).getAccessKeyMetadata()) {
                if (!existing.getAccessKeyId().equals(newKey.getAccessKeyId())) {
                    untracked.add(existing.getAccessKeyId());
                }
            }
            if (!untracked.isEmpty()) {
                logger.warn("Account {} still has {} access key(s) on its object store root user that CloudStack did not issue ({}) on store {}. "
                        + "They keep full access to every bucket of the account and have to be reviewed and removed on the gateway.",
                        account, untracked.size(), String.join(", ", untracked), storeId);
            }
        } catch (Exception e) {
            logger.debug("Unable to check for other access keys on the root user of account {}", account, e);
        }
        // the legacy rows held the pre-migration key material, which is now revoked: drop them
        // (persist replaces the whole detail map; update would only merge)
        Map<String, String> allDetails = _accountDetailsDao.findDetails(accountId);
        if (allDetails.containsKey(CEPH_ACCESS_KEY) || allDetails.containsKey(CEPH_SECRET_KEY)) {
            Map<String, String> remaining = new HashMap<>(allDetails);
            remaining.remove(CEPH_ACCESS_KEY);
            remaining.remove(CEPH_SECRET_KEY);
            _accountDetailsDao.persist(accountId, remaining);
        }
        logger.info("Rotated the Ceph RGW root key of account {} on store {}", account, storeId);
        return new BucketKeyTO(newKey.getAccessKeyId(), newKey.getSecretAccessKey());
    }

    @Override
    public boolean setBucketEncryption(BucketTO bucket, long storeId) {
        return false;
    }

    @Override
    public boolean deleteBucketEncryption(BucketTO bucket, long storeId) {
        return false;
    }

    @Override
    public boolean setBucketVersioning(BucketTO bucket, long storeId) {
        AmazonS3 client = getS3Client(getStoreURL(storeId), bucket.getAccessKey(), bucket.getSecretKey());
        try {
            BucketVersioningConfiguration configuration =
                    new BucketVersioningConfiguration().withStatus("Enabled");

            SetBucketVersioningConfigurationRequest setBucketVersioningConfigurationRequest =
                    new SetBucketVersioningConfigurationRequest(bucket.getName(), configuration);

            client.setBucketVersioningConfiguration(setBucketVersioningConfigurationRequest);
            return true;
        } catch (AmazonS3Exception e) {
            throw new CloudRuntimeException(e);
        }
    }

    @Override
    public boolean deleteBucketVersioning(BucketTO bucket, long storeId) {
        AmazonS3 client = getS3Client(getStoreURL(storeId), bucket.getAccessKey(), bucket.getSecretKey());
        try {
            BucketVersioningConfiguration configuration =
                    new BucketVersioningConfiguration().withStatus("Suspended");

            SetBucketVersioningConfigurationRequest setBucketVersioningConfigurationRequest =
                    new SetBucketVersioningConfigurationRequest(bucket.getName(), configuration);

            client.setBucketVersioningConfiguration(setBucketVersioningConfigurationRequest);
            return true;
        } catch (AmazonS3Exception e) {
            throw new CloudRuntimeException(e);
        }
    }

    @Override
    public void setBucketQuota(BucketTO bucket, long storeId, long size) {
        RgwAdmin rgwAdmin = getRgwAdminClient(storeId);

        try {
            rgwAdmin.setIndividualBucketQuota(null, bucket.getName(), -1, size * 1024 * 1024);
        } catch (Exception e) {
            throw new CloudRuntimeException(e);
        }
    }

    @Override
    public Map<String, Long> getAllBucketsUsage(long storeId) {
        RgwAdmin rgwAdmin = getRgwAdminClient(storeId);
        try {
            List<BucketInfo> bucketinfo = rgwAdmin.listBucketInfo();
            Map<String, Long> bucketsusage = new HashMap<String, Long>();
            for (BucketInfo bucket: bucketinfo) {
                BucketInfo.Usage usage = bucket.getUsage();
                bucketsusage.put(bucket.getBucket(), usage.getRgwMain().getSize_kb());
            }
            return bucketsusage;
        } catch (Exception e) {
            throw new CloudRuntimeException(e);
        }
    }

    protected static String bucketPolicy(String bucketName) {
        return "{\"Version\":\"2012-10-17\",\"Statement\":[{\"Effect\":\"Allow\",\"Action\":\"s3:*\","
                + "\"Resource\":[\"arn:aws:s3:::" + bucketName + "\",\"arn:aws:s3:::" + bucketName + "/*\"]}]}";
    }

    private static String requireCredentialId(BucketTO bucket) {
        if (bucket.getProviderCredentialId() == null) {
            throw new CloudRuntimeException("Bucket " + bucket.getName() + " has no dedicated credential");
        }
        return bucket.getProviderCredentialId();
    }

    /**
     * The key pair CloudStack itself uses on behalf of the account: the RGW account root's in
     * account mode, the legacy user's otherwise.
     */
    protected BucketKeyTO getAccountKey(long accountId, long storeId) {
        Map<String, String> accountMode = accountModeDetails(accountId, storeId);
        if (accountMode.containsKey(CEPH_ACCOUNT_ID)) {
            return new BucketKeyTO(accountMode.get(CEPH_ROOT_ACCESS_KEY), DBEncryptionUtil.decrypt(accountMode.get(CEPH_ROOT_SECRET_KEY)));
        }
        Map<String, String> details = _accountDetailsDao.findDetails(accountId);
        if (!details.containsKey(CEPH_ACCESS_KEY)) {
            throw new CloudRuntimeException("No Ceph RGW credential is recorded for account " + accountId);
        }
        return new BucketKeyTO(details.get(CEPH_ACCESS_KEY), details.get(CEPH_SECRET_KEY));
    }

    protected RgwAdmin getRgwAdminClient(long storeId) {
        ObjectStoreVO store = _storeDao.findById(storeId);
        Map<String, String> storeDetails = _storeDetailsDao.getDetails(storeId);
        String url = store.getUrl();
        String accessKey = storeDetails.get(ACCESS_KEY);
        String secretKey = storeDetails.get(SECRET_KEY);
        RgwAdmin admin = new RgwAdminBuilder()
                .accessKey(accessKey)
                .secretKey(secretKey)
                .endpoint(url + "/admin")
                .build();
        if (admin == null) {
            throw new CloudRuntimeException("Error while creating Ceph RGW client");
        }
        return admin;
    }

    protected RgwAccountClient getRgwAccountClient(long storeId) {
        ObjectStoreVO store = _storeDao.findById(storeId);
        Map<String, String> storeDetails = _storeDetailsDao.getDetails(storeId);
        return new RgwAccountClient(store.getUrl() + "/admin", storeDetails.get(ACCESS_KEY), storeDetails.get(SECRET_KEY));
    }

    /**
     * IAM client authenticated as the account's RGW root user. RGW serves the IAM API on the
     * same endpoint as S3.
     *
     * Account migration and account key rotation run under a lock for that account on that store,
     * and each makes several of these calls, so the SDK defaults (50s socket timeout, three
     * retries, no ceiling on the call as a whole) would let one unresponsive gateway hold that
     * lock for minutes. These are sub-second calls in normal operation.
     */
    protected AmazonIdentityManagement getIamClient(long storeId, long accountId) {
        if (!accountModeDetails(accountId, storeId).containsKey(CEPH_ACCOUNT_ID)) {
            throw new CloudRuntimeException("Account " + accountId + " has not been migrated to a Ceph RGW account on this store yet");
        }
        BucketKeyTO rootKey = getAccountKey(accountId, storeId);
        ClientConfiguration clientConfig = new ClientConfiguration()
                .withSignerOverride(RgwIamSigner.register())
                .withSocketTimeout(IAM_SOCKET_TIMEOUT_MILLIS)
                .withMaxErrorRetry(IAM_MAX_ERROR_RETRY)
                .withClientExecutionTimeout(IAM_CALL_TIMEOUT_MILLIS);
        return AmazonIdentityManagementClientBuilder.standard()
                .withClientConfiguration(clientConfig)
                .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(rootKey.getAccessKey(), rootKey.getSecretKey())))
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(getStoreURL(storeId), "us-east-1"))
                .build();
    }

    private String getStoreURL(long storeId) {
        ObjectStoreVO store = _storeDao.findById(storeId);
        String url = store.getUrl();
        return url;
    }

    protected AmazonS3 getS3Client(long storeId, long accountId) {
        String url = getStoreURL(storeId);
        BucketKeyTO accountKey = getAccountKey(accountId, storeId);
        return this.getS3Client(url, accountKey.getAccessKey(), accountKey.getSecretKey());
    }
    protected AmazonS3 getS3Client(String url, String accessKey, String secretKey) {
        AmazonS3 client = AmazonS3ClientBuilder.standard()
                .enablePathStyleAccess()
                .withCredentials(
                        new AWSStaticCredentialsProvider(
                                new BasicAWSCredentials(accessKey, secretKey)))
                .withEndpointConfiguration(
                        new AwsClientBuilder.EndpointConfiguration(url, "us-east-1"))
                .build();

        if (client == null) {
            throw new CloudRuntimeException("Error while creating Ceph RGW S3 client");
        }
        return client;
    }
}

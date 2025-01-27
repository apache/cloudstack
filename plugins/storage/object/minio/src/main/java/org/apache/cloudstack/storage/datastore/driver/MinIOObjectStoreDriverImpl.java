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

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.storage.datastore.db.ObjectStoreDao;
import org.apache.cloudstack.storage.datastore.db.ObjectStoreDetailsDao;
import org.apache.cloudstack.storage.datastore.db.ObjectStoreVO;
import org.apache.cloudstack.storage.object.BaseObjectStoreDriverImpl;
import org.apache.cloudstack.storage.object.Bucket;
import org.apache.cloudstack.storage.object.BucketObject;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;

import com.amazonaws.services.s3.model.AccessControlList;
import com.amazonaws.services.s3.model.BucketPolicy;
import com.cloud.agent.api.to.BucketTO;
import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.storage.BucketVO;
import com.cloud.storage.dao.BucketDao;
import com.cloud.user.Account;
import com.cloud.user.AccountDetailsDao;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.exception.CloudRuntimeException;

import io.minio.BucketExistsArgs;
import io.minio.DeleteBucketEncryptionArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.RemoveBucketArgs;
import io.minio.SetBucketEncryptionArgs;
import io.minio.SetBucketPolicyArgs;
import io.minio.SetBucketVersioningArgs;
import io.minio.admin.MinioAdminClient;
import io.minio.admin.QuotaUnit;
import io.minio.admin.UserInfo;
import io.minio.admin.messages.DataUsageInfo;
import io.minio.messages.SseConfiguration;
import io.minio.messages.VersioningConfiguration;

public class MinIOObjectStoreDriverImpl extends BaseObjectStoreDriverImpl {
    protected static final String ACS_PREFIX = "acs";

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

    protected static final String MINIO_ACCESS_KEY = "minio-accesskey";
    protected static final String MINIO_SECRET_KEY = "minio-secretkey";

    @Override
    public DataStoreTO getStoreTO(DataStore store) {
        return null;
    }

    protected String getUserOrAccessKeyForAccount(Account account) {
        return String.format("%s-%s", ACS_PREFIX, account.getUuid());
    }

    @Override
    public Bucket createBucket(Bucket bucket, boolean objectLock) {
        //ToDo Client pool mgmt
        String bucketName = bucket.getName();
        long storeId = bucket.getObjectStoreId();
        long accountId = bucket.getAccountId();
        MinioClient minioClient = getMinIOClient(storeId);
        Account account = _accountDao.findById(accountId);

        if ((_accountDetailsDao.findDetail(accountId, MINIO_ACCESS_KEY) == null)
                || (_accountDetailsDao.findDetail(accountId, MINIO_SECRET_KEY) == null)) {
            throw new CloudRuntimeException("Bucket access credentials unavailable for account: "+account.getAccountName());
        }

        try {
            if(minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build())) {
                throw new CloudRuntimeException("Bucket already exists with name "+ bucketName);
            }
        } catch (Exception e) {
            throw new CloudRuntimeException(e);
        }
        try {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).objectLock(objectLock).build());
        } catch (Exception e) {
            throw new CloudRuntimeException(e);
        }

        List<BucketVO> buckets = _bucketDao.listByObjectStoreIdAndAccountId(storeId, accountId);
        StringBuilder resources_builder = new StringBuilder();
        for(BucketVO exitingBucket : buckets) {
            resources_builder.append("\"arn:aws:s3:::"+exitingBucket.getName()+"/*\",\n");
        }
        resources_builder.append("\"arn:aws:s3:::"+bucketName+"/*\"\n");

        String policy = " {\n" +
                "     \"Statement\": [\n" +
                "         {\n" +
                "             \"Action\": \"s3:*\",\n" +
                "             \"Effect\": \"Allow\",\n" +
                "             \"Principal\": \"*\",\n" +
                "             \"Resource\": ["+resources_builder+"]" +
                "         }\n" +
                "     ],\n" +
                "     \"Version\": \"2012-10-17\"\n" +
                " }";
        MinioAdminClient minioAdminClient = getMinIOAdminClient(storeId);
        String policyName = getUserOrAccessKeyForAccount(account) + "-policy";
        String userName = getUserOrAccessKeyForAccount(account);
        try {
            minioAdminClient.addCannedPolicy(policyName, policy);
            minioAdminClient.setPolicy(userName, false, policyName);
        } catch (Exception e) {
            throw new CloudRuntimeException(e);
        }
        String accessKey = _accountDetailsDao.findDetail(accountId, MINIO_ACCESS_KEY).getValue();
        String secretKey = _accountDetailsDao.findDetail(accountId, MINIO_SECRET_KEY).getValue();
        ObjectStoreVO store = _storeDao.findById(storeId);
        BucketVO bucketVO = _bucketDao.findById(bucket.getId());
        bucketVO.setAccessKey(accessKey);
        bucketVO.setSecretKey(secretKey);
        bucketVO.setBucketURL(store.getUrl()+"/"+bucketName);
        _bucketDao.update(bucket.getId(), bucketVO);
        return bucket;
    }

    @Override
    public List<Bucket> listBuckets(long storeId) {
        MinioClient minioClient = getMinIOClient(storeId);
        List<Bucket> bucketsList = new ArrayList<>();
        try {
            List<io.minio.messages.Bucket> minIOBuckets =  minioClient.listBuckets();
            for(io.minio.messages.Bucket minIObucket : minIOBuckets) {
                Bucket bucket = new BucketObject();
                bucket.setName(minIObucket.name());
                bucketsList.add(bucket);
            }
        } catch (Exception e) {
            throw new CloudRuntimeException(e);
        }
        return bucketsList;
    }

    @Override
    public boolean deleteBucket(BucketTO bucket, long storeId) {
        String bucketName = bucket.getName();
        MinioClient minioClient = getMinIOClient(storeId);
        try {
            if(!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build())) {
                throw new CloudRuntimeException("Bucket doesn't exist: "+ bucketName);
            }
        } catch (Exception e) {
            throw new CloudRuntimeException(e);
        }
        //ToDo: check bucket empty
        try {
            minioClient.removeBucket(RemoveBucketArgs.builder().bucket(bucketName).build());
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
        String bucketName = bucket.getName();
        String privatePolicy = "{\"Version\":\"2012-10-17\",\"Statement\":[]}";

        StringBuilder builder = new StringBuilder();
        builder.append("{\n");
        builder.append("    \"Statement\": [\n");
        builder.append("        {\n");
        builder.append("            \"Action\": [\n");
        builder.append("                \"s3:GetBucketLocation\",\n");
        builder.append("                \"s3:ListBucket\"\n");
        builder.append("            ],\n");
        builder.append("            \"Effect\": \"Allow\",\n");
        builder.append("            \"Principal\": \"*\",\n");
        builder.append("            \"Resource\": \"arn:aws:s3:::"+bucketName+"\"\n");
        builder.append("        },\n");
        builder.append("        {\n");
        builder.append("            \"Action\": \"s3:GetObject\",\n");
        builder.append("            \"Effect\": \"Allow\",\n");
        builder.append("            \"Principal\": \"*\",\n");
        builder.append("            \"Resource\": \"arn:aws:s3:::"+bucketName+"/*\"\n");
        builder.append("        }\n");
        builder.append("    ],\n");
        builder.append("    \"Version\": \"2012-10-17\"\n");
        builder.append("}\n");

        String publicPolicy = builder.toString();

        //ToDo Support custom policy
        String policyConfig = (policy.equalsIgnoreCase("public"))? publicPolicy : privatePolicy;

        MinioClient minioClient = getMinIOClient(storeId);
        try {
            minioClient.setBucketPolicy(
                    SetBucketPolicyArgs.builder().bucket(bucketName).config(policyConfig).build());
        } catch (Exception e) {
            throw new CloudRuntimeException(e);
        }
    }

    @Override
    public BucketPolicy getBucketPolicy(BucketTO bucket, long storeId) {
        return null;
    }

    @Override
    public void deleteBucketPolicy(BucketTO bucket, long storeId) {

    }

    protected void updateAccountCredentials(final long accountId, final String accessKey, final String secretKey, final boolean checkIfNotPresent) {
        Map<String, String> details = _accountDetailsDao.findDetails(accountId);
        boolean updateNeeded = false;
        if (!checkIfNotPresent || StringUtils.isBlank(details.get(MINIO_ACCESS_KEY))) {
            details.put(MINIO_ACCESS_KEY, accessKey);
            updateNeeded = true;
        }
        if (StringUtils.isAllBlank(secretKey, details.get(MINIO_SECRET_KEY))) {
            logger.error(String.format("Failed to retrieve secret key for MinIO user: %s from store and account details", accessKey));
        }
        if (StringUtils.isNotBlank(secretKey) && (!checkIfNotPresent || StringUtils.isBlank(details.get(MINIO_SECRET_KEY)))) {
            details.put(MINIO_SECRET_KEY, secretKey);
            updateNeeded = true;
        }
        if (!updateNeeded) {
            return;
        }
        _accountDetailsDao.persist(accountId, details);
    }

    @Override
    public boolean createUser(long accountId, long storeId) {
        Account account = _accountDao.findById(accountId);
        MinioAdminClient minioAdminClient = getMinIOAdminClient(storeId);
        String accessKey = getUserOrAccessKeyForAccount(account);
        // Check user exists
        try {
            UserInfo userInfo = minioAdminClient.getUserInfo(accessKey);
            if(userInfo != null) {
                if (logger.isDebugEnabled()) {
                    logger.debug(String.format("Skipping user creation as the user already exists in MinIO store: %s", accessKey));
                }
                updateAccountCredentials(accountId, accessKey, userInfo.secretKey(), true);
                return true;
            }
        } catch (NoSuchAlgorithmException | IOException | InvalidKeyException e) {
            logger.error(String.format("Error encountered while retrieving user: %s for existing MinIO store user check", accessKey), e);
            return false;
        } catch (RuntimeException e) { // MinIO lib may throw RuntimeException with code: XMinioAdminNoSuchUser
            if (logger.isDebugEnabled()) {
                logger.debug(String.format("Ignoring error encountered while retrieving user: %s for existing MinIO store user check", accessKey));
            }
            logger.trace("Exception during MinIO user check", e);
        }
        if (logger.isDebugEnabled()) {
            logger.debug(String.format("MinIO store user does not exist. Creating user: %s", accessKey));
        }
        KeyGenerator generator = null;
        try {
            generator = KeyGenerator.getInstance("HmacSHA1");
        } catch (NoSuchAlgorithmException e) {
            throw new CloudRuntimeException(e);
        }
        SecretKey key = generator.generateKey();
        String secretKey = Base64.encodeBase64URLSafeString(key.getEncoded());
        try {
            minioAdminClient.addUser(accessKey, UserInfo.Status.ENABLED, secretKey, "", new ArrayList<String>());
        } catch (Exception e) {
            throw new CloudRuntimeException(e);
        }
        // Store user credentials
        updateAccountCredentials(accountId, accessKey, secretKey, false);
        return true;
    }

    @Override
    public boolean setBucketEncryption(BucketTO bucket, long storeId) {
        MinioClient minioClient = getMinIOClient(storeId);
        try {
            minioClient.setBucketEncryption(SetBucketEncryptionArgs.builder()
                    .bucket(bucket.getName())
                    .config(SseConfiguration.newConfigWithSseS3Rule())
                    .build()
            );
        } catch (Exception e) {
            throw new CloudRuntimeException(e);
        }
        return true;
    }

    @Override
    public boolean deleteBucketEncryption(BucketTO bucket, long storeId) {
        MinioClient minioClient = getMinIOClient(storeId);
        try {
            minioClient.deleteBucketEncryption(DeleteBucketEncryptionArgs.builder()
                    .bucket(bucket.getName())
                    .build()
            );
        } catch (Exception e) {
            throw new CloudRuntimeException(e);
        }
        return true;
    }

    @Override
    public boolean setBucketVersioning(BucketTO bucket, long storeId) {
        MinioClient minioClient = getMinIOClient(storeId);
        try {
            minioClient.setBucketVersioning(SetBucketVersioningArgs.builder()
                    .bucket(bucket.getName())
                    .config(new VersioningConfiguration(VersioningConfiguration.Status.ENABLED, null))
                    .build()
            );
        } catch (Exception e) {
            throw new CloudRuntimeException(e);
        }
        return true;
    }

    @Override
    public boolean deleteBucketVersioning(BucketTO bucket, long storeId) {
        MinioClient minioClient = getMinIOClient(storeId);
        try {
            minioClient.setBucketVersioning(SetBucketVersioningArgs.builder()
                    .bucket(bucket.getName())
                    .config(new VersioningConfiguration(VersioningConfiguration.Status.SUSPENDED, null))
                    .build()
            );
        } catch (Exception e) {
            throw new CloudRuntimeException(e);
        }
        return true;
    }

    @Override
    public void setBucketQuota(BucketTO bucket, long storeId, long size) {

        MinioAdminClient minioAdminClient = getMinIOAdminClient(storeId);
        try {
            minioAdminClient.setBucketQuota(bucket.getName(), size, QuotaUnit.GB);
        } catch (Exception e) {
            throw new CloudRuntimeException(e);
        }
    }

    @Override
    public Map<String, Long> getAllBucketsUsage(long storeId) {
        MinioAdminClient minioAdminClient = getMinIOAdminClient(storeId);
        try {
            DataUsageInfo dataUsageInfo = minioAdminClient.getDataUsageInfo();
            return dataUsageInfo.bucketsSizes();
        } catch (Exception e) {
            throw new CloudRuntimeException(e);
        }
    }

    protected MinioClient getMinIOClient(long storeId) {
        ObjectStoreVO store = _storeDao.findById(storeId);
        Map<String, String> storeDetails = _storeDetailsDao.getDetails(storeId);
        String url = store.getUrl();
        String accessKey = storeDetails.get(ACCESS_KEY);
        String secretKey = storeDetails.get(SECRET_KEY);
        MinioClient minioClient =
                MinioClient.builder()
                        .endpoint(url)
                        .credentials(accessKey,secretKey)
                        .build();
        if(minioClient == null){
            throw new CloudRuntimeException("Error while creating MinIO client");
        }
        return minioClient;
    }

    protected MinioAdminClient getMinIOAdminClient(long storeId) {
        ObjectStoreVO store = _storeDao.findById(storeId);
        Map<String, String> storeDetails = _storeDetailsDao.getDetails(storeId);
        String url = store.getUrl();
        String accessKey = storeDetails.get(ACCESS_KEY);
        String secretKey = storeDetails.get(SECRET_KEY);
        MinioAdminClient minioAdminClient =
                MinioAdminClient.builder()
                        .endpoint(url)
                        .credentials(accessKey,secretKey)
                        .build();
        if(minioAdminClient == null){
            throw new CloudRuntimeException("Error while creating MinIO client");
        }
        return minioAdminClient;
    }
}

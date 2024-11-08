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

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
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
import com.cloud.agent.api.to.BucketTO;
import com.cloud.agent.api.to.DataStoreTO;
import org.apache.cloudstack.storage.object.Bucket;
import com.cloud.storage.BucketVO;
import com.cloud.storage.dao.BucketDao;
import com.cloud.user.Account;
import com.cloud.user.AccountDetailsDao;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.storage.datastore.db.ObjectStoreDao;
import org.apache.cloudstack.storage.datastore.db.ObjectStoreDetailsDao;
import org.apache.cloudstack.storage.datastore.db.ObjectStoreVO;
import org.apache.cloudstack.storage.object.BaseObjectStoreDriverImpl;
import org.apache.cloudstack.storage.object.BucketObject;
import org.twonote.rgwadmin4j.RgwAdmin;
import org.twonote.rgwadmin4j.RgwAdminBuilder;
import org.twonote.rgwadmin4j.model.BucketInfo;
import org.twonote.rgwadmin4j.model.S3Credential;
import org.twonote.rgwadmin4j.model.User;

import javax.inject.Inject;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Map;
import java.util.HashMap;

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

    private static final String CEPH_ACCESS_KEY = "ceph-rgw-accesskey";
    private static final String CEPH_SECRET_KEY = "ceph-rgw-secretkey";

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
            if (s3client.getBucketAcl(bucketName) != null) {
                throw new CloudRuntimeException("Bucket already exists with name " + bucketName);
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
            String accessKey = _accountDetailsDao.findDetail(accountId, CEPH_ACCESS_KEY).getValue();
            String secretKey = _accountDetailsDao.findDetail(accountId, CEPH_SECRET_KEY).getValue();
            ObjectStoreVO store = _storeDao.findById(storeId);
            BucketVO bucketVO = _bucketDao.findById(bucket.getId());
            bucketVO.setAccessKey(accessKey);
            bucketVO.setSecretKey(secretKey);
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
        String policyConfig;

        if (policy.equalsIgnoreCase("public")) {
            logger.debug("Setting public policy on bucket " + bucket.getName());
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
            builder.append("            \"Resource\": \"arn:aws:s3:::" + bucket.getName() + "\"\n");
            builder.append("        },\n");
            builder.append("        {\n");
            builder.append("            \"Action\": \"s3:GetObject\",\n");
            builder.append("            \"Effect\": \"Allow\",\n");
            builder.append("            \"Principal\": \"*\",\n");
            builder.append("            \"Resource\": \"arn:aws:s3:::" + bucket.getName() + "/*\"\n");
            builder.append("        }\n");
            builder.append("    ],\n");
            builder.append("    \"Version\": \"2012-10-17\"\n");
            builder.append("}\n");
            policyConfig = builder.toString();
        } else {
            logger.debug("Setting private policy on bucket " + bucket.getName());
            policyConfig = "{\"Version\":\"2012-10-17\",\"Statement\":[]}";
        }

        AmazonS3 client = getS3Client(getStoreURL(storeId), bucket.getAccessKey(), bucket.getAccessKey());
        client.setBucketPolicy(new SetBucketPolicyRequest(bucket.getName(), policyConfig));
    }

    @Override
    public BucketPolicy getBucketPolicy(BucketTO bucket, long storeId) {
        AmazonS3 client = getS3Client(getStoreURL(storeId), bucket.getAccessKey(), bucket.getAccessKey());
        return client.getBucketPolicy(new GetBucketPolicyRequest(bucket.getName()));
    }

    @Override
    public void deleteBucketPolicy(BucketTO bucket, long storeId) {
        AmazonS3 client = getS3Client(getStoreURL(storeId), bucket.getAccessKey(), bucket.getAccessKey());
        client.deleteBucketPolicy(new DeleteBucketPolicyRequest(bucket.getName()));
    }

    @Override
    public boolean createUser(long accountId, long storeId) {
        Account account = _accountDao.findById(accountId);
        RgwAdmin rgwAdmin = getRgwAdminClient(storeId);
        String username = account.getUuid();

        logger.debug("Attempting to create Ceph RGW user for account " + account.getAccountName() + " with UUID " + username);
        try {
            Optional<User> user = rgwAdmin.getUserInfo(username);
            if (user.isPresent()) {
                logger.info("User already exists in Ceph RGW: " + username);
                return true;
            }
        } catch (Exception e) {
            logger.debug("User does not exist. Creating user in Ceph RGW: " + username);
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
    public boolean setBucketEncryption(BucketTO bucket, long storeId) {
        return false;
    }

    @Override
    public boolean deleteBucketEncryption(BucketTO bucket, long storeId) {
        return false;
    }

    @Override
    public boolean setBucketVersioning(BucketTO bucket, long storeId) {
        AmazonS3 client = getS3Client(getStoreURL(storeId), bucket.getAccessKey(), bucket.getAccessKey());
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
        AmazonS3 client = getS3Client(getStoreURL(storeId), bucket.getAccessKey(), bucket.getAccessKey());
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
            rgwAdmin.setBucketQuota(bucket.getName(), -1, size);
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

    private String getStoreURL(long storeId) {
        ObjectStoreVO store = _storeDao.findById(storeId);
        String url = store.getUrl();
        return url;
    }

    protected AmazonS3 getS3Client(long storeId, long accountId) {
        String url = getStoreURL(storeId);
        String accessKey = _accountDetailsDao.findDetail(accountId, CEPH_ACCESS_KEY).getValue();
        String secretKey = _accountDetailsDao.findDetail(accountId, CEPH_SECRET_KEY).getValue();
        return this.getS3Client(url, accessKey, secretKey);
    }
    protected AmazonS3 getS3Client(String url, String accessKey, String secretKey) {
        AmazonS3 client = AmazonS3ClientBuilder.standard()
                .enablePathStyleAccess()
                .withCredentials(
                        new AWSStaticCredentialsProvider(
                                new BasicAWSCredentials(accessKey, secretKey)))
                .withEndpointConfiguration(
                        new AwsClientBuilder.EndpointConfiguration(url, "auto"))
                .build();

        if (client == null) {
            throw new CloudRuntimeException("Error while creating Ceph RGW S3 client");
        }
        return client;
    }
}

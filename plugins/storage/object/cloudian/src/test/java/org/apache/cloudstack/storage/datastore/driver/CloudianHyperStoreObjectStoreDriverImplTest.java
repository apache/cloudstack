// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
// SPDX-License-Identifier: Apache-2.0
package org.apache.cloudstack.storage.datastore.driver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.cloudian.client.CloudianClient;
import org.apache.cloudstack.cloudian.client.CloudianCredential;
import org.apache.cloudstack.cloudian.client.CloudianGroup;
import org.apache.cloudstack.cloudian.client.CloudianUser;
import org.apache.cloudstack.cloudian.client.CloudianUserBucketUsage;
import org.apache.cloudstack.cloudian.client.CloudianUserBucketUsage.CloudianBucketUsage;
import org.apache.cloudstack.storage.datastore.db.ObjectStoreDao;
import org.apache.cloudstack.storage.datastore.db.ObjectStoreDetailsDao;
import org.apache.cloudstack.storage.datastore.db.ObjectStoreVO;
import org.apache.cloudstack.storage.datastore.util.CloudianHyperStoreUtil;
import org.apache.cloudstack.storage.object.Bucket;

import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.model.AccessKey;
import com.amazonaws.services.identitymanagement.model.CreateAccessKeyRequest;
import com.amazonaws.services.identitymanagement.model.CreateAccessKeyResult;
import com.amazonaws.services.identitymanagement.model.CreateUserRequest;
import com.amazonaws.services.identitymanagement.model.ListAccessKeysRequest;
import com.amazonaws.services.identitymanagement.model.PutUserPolicyRequest;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CreateBucketRequest;
import com.amazonaws.services.s3.model.SetBucketCrossOriginConfigurationRequest;
import com.cloud.agent.api.to.BucketTO;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.storage.BucketVO;
import com.cloud.storage.dao.BucketDao;
import com.cloud.user.AccountDetailsDao;
import com.cloud.user.AccountVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.exception.CloudRuntimeException;

@RunWith(MockitoJUnitRunner.class)
public class CloudianHyperStoreObjectStoreDriverImplTest {

    @Spy
    CloudianHyperStoreObjectStoreDriverImpl cloudianHyperStoreObjectStoreDriverImpl = new CloudianHyperStoreObjectStoreDriverImpl();

    @Mock
    AmazonS3 s3Client;
    @Mock
    CloudianClient cloudianClient;
    @Mock
    AmazonIdentityManagement iamClient;
    @Mock
    ObjectStoreDao objectStoreDao;
    @Mock
    ObjectStoreVO objectStoreVO;
    @Mock
    ObjectStoreDetailsDao objectStoreDetailsDao;
    @Mock
    AccountDao accountDao;
    @Mock
    BucketDao bucketDao;
    @Mock
    DomainDao domainDao;
    @Mock
    AccountDetailsDao accountDetailsDao;

    @Mock
    AccountVO account;
    @Mock
    DomainVO domain;

    BucketVO bucketVo;
    Map<String, String> StoreDetailsMap;
    Map<String, String> AccountDetailsMap;

    static long TEST_STORE_ID = 1010L;
    static long TEST_ACCOUNT_ID = 2010L;
    static long TEST_DOMAIN_ID = 3010L;
    static String TEST_BUCKET_NAME = "testbucketname";
    static String TEST_ROOT_AK = "root_access_key";
    static String TEST_ROOT_SK = "root_secret_key";
    static String TEST_IAM_AK = "iam_access_key";
    static String TEST_IAM_SK = "iam_secret_key";
    static String TEST_S3_URL = "http://s3-endpoint";
    static String TEST_IAM_URL = "http://iam-endpoint:16080";
    static String TEST_BUCKET_URL = TEST_S3_URL + "/" + TEST_BUCKET_NAME;

    private AutoCloseable closeable;

    @Before
    public void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
        cloudianHyperStoreObjectStoreDriverImpl._storeDao = objectStoreDao;
        cloudianHyperStoreObjectStoreDriverImpl._storeDetailsDao = objectStoreDetailsDao;
        cloudianHyperStoreObjectStoreDriverImpl._accountDao = accountDao;
        cloudianHyperStoreObjectStoreDriverImpl._bucketDao = bucketDao;
        cloudianHyperStoreObjectStoreDriverImpl._accountDetailsDao = accountDetailsDao;
        cloudianHyperStoreObjectStoreDriverImpl._domainDao = domainDao;

        // The StoreDetailMap has Endpoint info and Admin Credentials
        StoreDetailsMap = new HashMap<String, String>();
        StoreDetailsMap.put(CloudianHyperStoreUtil.KEY_S3_ENDPOINT_URL, TEST_S3_URL);
        StoreDetailsMap.put(CloudianHyperStoreUtil.KEY_IAM_ENDPOINT_URL, TEST_IAM_URL);
        when(objectStoreDetailsDao.getDetails(TEST_STORE_ID)).thenReturn(StoreDetailsMap);

        // The AccountDetailsMap has credentials for operating on the account.
        AccountDetailsMap = new HashMap<String, String>();
        AccountDetailsMap.put(CloudianHyperStoreUtil.KEY_ROOT_ACCESS_KEY, TEST_ROOT_AK);
        AccountDetailsMap.put(CloudianHyperStoreUtil.KEY_ROOT_SECRET_KEY, TEST_ROOT_SK);
        AccountDetailsMap.put(CloudianHyperStoreUtil.KEY_IAM_ACCESS_KEY, TEST_IAM_AK);
        AccountDetailsMap.put(CloudianHyperStoreUtil.KEY_IAM_SECRET_KEY, TEST_IAM_SK);
        when(accountDetailsDao.findDetails(TEST_ACCOUNT_ID)).thenReturn(AccountDetailsMap);

        // Useful test bucket info
        bucketVo = new BucketVO(TEST_ACCOUNT_ID, TEST_DOMAIN_ID, TEST_STORE_ID, TEST_BUCKET_NAME, null, false, false, false, null);
    }

    @After
    public void tearDown() throws Exception {
        closeable.close();
    }

    @Test
    public void testCreateBucket() throws Exception {
        doReturn(s3Client).when(cloudianHyperStoreObjectStoreDriverImpl).getS3Client(anyString(), anyString(), anyString());
        when(bucketDao.findById(anyLong())).thenReturn(bucketVo);

        // Actual Test
        Bucket bucketRet = cloudianHyperStoreObjectStoreDriverImpl.createBucket(bucketVo, false);
        assertEquals(TEST_BUCKET_NAME, bucketRet.getName());

        // Capture the bucket info that was saved to the DB
        ArgumentCaptor<BucketVO> argument = ArgumentCaptor.forClass(BucketVO.class);
        verify(bucketDao, times(1)).update(any(), argument.capture());
        BucketVO UpdatedBucketVO = argument.getValue();
        assertEquals(TEST_IAM_AK, UpdatedBucketVO.getAccessKey());
        assertEquals(TEST_IAM_SK, UpdatedBucketVO.getSecretKey());
        assertEquals(TEST_BUCKET_URL, UpdatedBucketVO.getBucketURL());

        verify(s3Client, times(1)).createBucket(any(CreateBucketRequest.class));
        verify(s3Client, times(1))
                .setBucketCrossOriginConfiguration(any(SetBucketCrossOriginConfigurationRequest.class));
        verify(s3Client, never()).deleteBucket(anyString());
    }

    @Test
    public void testCreateHSCredential() throws Exception {
        cloudianClient = mock(CloudianClient.class);
        List<CloudianCredential> CredList = new ArrayList<CloudianCredential>();
        CloudianCredential c1 = new CloudianCredential();
        c1.setActive(false);
        c1.setCreateDate(new Date(1L));   // oldest but inactive
        CloudianCredential c2 = new CloudianCredential();
        c2.setAccessKey(TEST_ROOT_AK);
        c2.setSecretKey(TEST_ROOT_SK);
        c2.setActive(true);
        c2.setCreateDate(new Date(2L));   // 2nd oldest
        CloudianCredential c3 = new CloudianCredential();
        c3.setActive(true);
        c3.setCreateDate(new Date(2L));   // newest
        CredList.add(c1);
        CredList.add(c2);
        CredList.add(c3);
        when(cloudianClient.listCredentials(anyString(), anyString())).thenReturn(CredList);

        // Test expects c2 which is the oldest active credential.
        CloudianCredential actual = cloudianHyperStoreObjectStoreDriverImpl.createHSCredential(cloudianClient, "user", "group");
        assertTrue(actual.getActive());
        assertEquals(TEST_ROOT_AK, actual.getAccessKey());
        assertEquals(TEST_ROOT_SK, actual.getSecretKey());
        verify(cloudianClient, never()).createCredential(anyString(), anyString());
    }

    @Test
    public void testGetAllBucketsUsageNoBuckets() throws Exception {
        when(bucketDao.listByObjectStoreId(TEST_STORE_ID)).thenReturn(new ArrayList<BucketVO>());
        Map<String, Long> emptyMap = cloudianHyperStoreObjectStoreDriverImpl.getAllBucketsUsage(TEST_STORE_ID);
        assertNotNull(emptyMap);
        assertEquals(0, emptyMap.size());
    }

    @Test
    public void testGetAllBucketsUsageTwoDomains() {
        // Prepare Buckets the store knows about.
        BucketVO b1 = new BucketVO(TEST_ACCOUNT_ID, 1L, TEST_STORE_ID, "b1", null, false, false, false, null);
        BucketVO b2 = new BucketVO(TEST_ACCOUNT_ID, 1L, TEST_STORE_ID, "b2", null, false, false, false, null);
        BucketVO b3 = new BucketVO(TEST_ACCOUNT_ID, 2L, TEST_STORE_ID, "b3", null, false, false, false, null);
        BucketVO b4 = new BucketVO(TEST_ACCOUNT_ID, 2L, TEST_STORE_ID, "b4", null, false, false, false, null);
        List<BucketVO> BucketList = new ArrayList<BucketVO>();
        BucketList.add(b1);    // b1 owned by domain 1, exists
        BucketList.add(b2);    // b2 owned by domain 1, deleted in object store (so no usage info)
        BucketList.add(b3);    // b3 owned by domain 2, exists
        BucketList.add(b4);    // b4 owned by domain 2, exists
        when(bucketDao.listByObjectStoreId(TEST_STORE_ID)).thenReturn(BucketList);

        final String hsGroupId1 = "domain1";
        final String hsGroupId2 = "domain2";

        // Setup both domains d1 and d2 with uuids that will become hsGroupId
        DomainVO d1 = mock(DomainVO.class);
        when(d1.getUuid()).thenReturn(hsGroupId1);
        DomainVO d2 = mock(DomainVO.class);
        when(d2.getUuid()).thenReturn(hsGroupId2);
        when(domainDao.findById(1L)).thenReturn(d1);
        when(domainDao.findById(2L)).thenReturn(d2);

        // Setup Bucket Usage Data returned for b1, b3, b4, b5 by CloudianClient
        // where b2 is missing, b4 usage is negative and b5 is unknown.
        CloudianBucketUsage bu1 = new CloudianBucketUsage();
        bu1.setBucketName("b1");
        bu1.setByteCount(1L);
        CloudianBucketUsage bu3 = new CloudianBucketUsage();
        bu3.setBucketName("b3");
        bu3.setByteCount(3L);
        CloudianBucketUsage bu4 = new CloudianBucketUsage();
        bu4.setBucketName("b4");
        bu4.setByteCount(-55555L);
        CloudianBucketUsage bu5 = new CloudianBucketUsage();
        bu5.setBucketName("b5");
        bu5.setByteCount(5L);
        List<CloudianBucketUsage> d1bucketList = new ArrayList<CloudianBucketUsage>();
        d1bucketList.add(bu1);
        List<CloudianBucketUsage> d2bucketList = new ArrayList<CloudianBucketUsage>();
        d2bucketList.add(bu3);
        d2bucketList.add(bu4);
        d2bucketList.add(bu5);
        CloudianUserBucketUsage d1U1Usage = mock(CloudianUserBucketUsage.class);
        when(d1U1Usage.getBuckets()).thenReturn(d1bucketList);
        CloudianUserBucketUsage d2U1Usage = mock(CloudianUserBucketUsage.class);
        when(d2U1Usage.getBuckets()).thenReturn(d2bucketList);
        List<CloudianUserBucketUsage> d1Usage = new ArrayList<CloudianUserBucketUsage>();
        d1Usage.add(d1U1Usage);
        List<CloudianUserBucketUsage> d2Usage = new ArrayList<CloudianUserBucketUsage>();
        d2Usage.add(d2U1Usage);

        doReturn(cloudianClient).when(cloudianHyperStoreObjectStoreDriverImpl).getCloudianClientByStoreId(TEST_STORE_ID);
        when(cloudianClient.getUserBucketUsages(hsGroupId1, null, null)).thenReturn(d1Usage);
        when(cloudianClient.getUserBucketUsages(hsGroupId2, null, null)).thenReturn(d2Usage);

        // Test Details:
        // The CloudStack DB knows about 4 buckets: b1, b2, b3, b4
        // The actual Object Store knows about 4 buckets: b1, b3, b4, b5
        // Bucket usage in Object Store is: b1:1, b3:3, b4:-55555, b5:5
        // Expected Response: Usage for 3 buckets, b1, b3 and b4 where
        // b4 usage is returns as 0 instead of actual negative value and
        // b5 is ignored as it is not known by the store.
        Map<String, Long> usageMap = cloudianHyperStoreObjectStoreDriverImpl.getAllBucketsUsage(TEST_STORE_ID);
        assertNotNull(usageMap);
        assertEquals(3, usageMap.size());
        assertEquals(1L, usageMap.get("b1").longValue());
        assertEquals(3L, usageMap.get("b3").longValue());
        assertEquals(0L, usageMap.get("b4").longValue());
    }

    @Test
    public void testCreateUser() throws Exception {
        // ensure no account credentials are returned in the account details for new user.
        Mockito.reset(accountDetailsDao);
        when(accountDetailsDao.findDetails(TEST_ACCOUNT_ID)).thenReturn(new HashMap<String, String>());

        String hsUserId = "user1";
        String hsGroupId = "group1";
        when(accountDao.findById(TEST_ACCOUNT_ID)).thenReturn(account);
        when(account.getDomainId()).thenReturn(TEST_DOMAIN_ID);
        when(account.getUuid()).thenReturn(hsUserId);
        when(domainDao.findById(TEST_DOMAIN_ID)).thenReturn(domain);
        when(domain.getUuid()).thenReturn(hsGroupId);

        doReturn(cloudianClient).when(cloudianHyperStoreObjectStoreDriverImpl).getCloudianClientByStoreId(TEST_STORE_ID);

        // Setup the user and group as not found.
        when(cloudianClient.listUser(hsUserId, hsGroupId)).thenReturn(null);
        when(cloudianClient.listGroup(hsGroupId)).thenReturn(null);
        when(cloudianClient.addUser(any(CloudianUser.class))).thenReturn(true);
        // lets assume no credentials added, so we add new ones.
        when(cloudianClient.listCredentials(hsUserId, hsGroupId)).thenReturn(new ArrayList<CloudianCredential>());
        CloudianCredential credential = new CloudianCredential();
        credential.setAccessKey(TEST_ROOT_AK);
        credential.setSecretKey(TEST_ROOT_SK);
        when(cloudianClient.createCredential(hsUserId, hsGroupId)).thenReturn(credential);

        // Setup IAM for user, policy and credential creation.
        doReturn(iamClient).when(cloudianHyperStoreObjectStoreDriverImpl).getIAMClientByStoreId(TEST_STORE_ID, credential);
        AccessKey accessKey = mock(AccessKey.class);
        CreateAccessKeyResult accessKeyResult = mock(CreateAccessKeyResult.class);
        when(accessKey.getAccessKeyId()).thenReturn(TEST_IAM_AK);
        when(accessKey.getSecretAccessKey()).thenReturn(TEST_IAM_SK);
        when(accessKeyResult.getAccessKey()).thenReturn(accessKey);
        when(iamClient.createAccessKey(any(CreateAccessKeyRequest.class))).thenReturn(accessKeyResult);

        // Next Check what will be persisted in DB after everything created.
        // No buckets to update the IAM keys for
        when(bucketDao.listByObjectStoreIdAndAccountId(TEST_STORE_ID, TEST_ACCOUNT_ID)).thenReturn(new ArrayList<BucketVO>());

        // Test: The user should be created which involves:
        // creating the group, user and root credentials
        // creating the iam user, its policy and iam credentials
        // finally persisting the root and iam credentials in account details.
        boolean created = cloudianHyperStoreObjectStoreDriverImpl.createUser(TEST_ACCOUNT_ID, TEST_STORE_ID);
        assertTrue(created);

        // THe HyperStore group, user and credentials
        verify(cloudianClient, times(1)).addGroup(any(CloudianGroup.class));
        verify(cloudianClient, times(1)).addUser(any(CloudianUser.class));
        verify(cloudianClient, times(1)).createCredential(hsUserId, hsGroupId);

        // not expecting IAM list access keys for a new user.
        verify(iamClient, never()).listAccessKeys(any(ListAccessKeysRequest.class));
        // We do expect IAM user creation with policy and access keys though.
        verify(iamClient, times(1)).createUser(any(CreateUserRequest.class));
        verify(iamClient, times(1)).putUserPolicy(any(PutUserPolicyRequest.class));
        verify(iamClient, times(1)).createAccessKey(any(CreateAccessKeyRequest.class));

        // Now let's verify that the correct account details were persisted.
        ArgumentCaptor<Map<String,String>> detailsArg = ArgumentCaptor.forClass((Class<Map<String, String>>) (Class<?>) Map.class);
        verify(accountDetailsDao, times(1)).persist(anyLong(), detailsArg.capture());
        Map<String, String> updatedDetails = detailsArg.getValue();
        assertEquals(4, updatedDetails.size());
        assertEquals(TEST_IAM_AK, updatedDetails.get(CloudianHyperStoreUtil.KEY_IAM_ACCESS_KEY));
        assertEquals(TEST_IAM_SK, updatedDetails.get(CloudianHyperStoreUtil.KEY_IAM_SECRET_KEY));
        assertEquals(TEST_ROOT_AK, updatedDetails.get(CloudianHyperStoreUtil.KEY_ROOT_ACCESS_KEY));
        assertEquals(TEST_ROOT_SK, updatedDetails.get(CloudianHyperStoreUtil.KEY_ROOT_SECRET_KEY));
    }

    @Test
    public void testDeleteBucket() throws Exception {
        doReturn(s3Client).when(cloudianHyperStoreObjectStoreDriverImpl).getS3ClientByBucketAndStore(any(), anyLong());
        BucketTO bucket = mock(BucketTO.class);
        when(bucket.getName()).thenReturn(TEST_BUCKET_NAME);
        boolean deleted = cloudianHyperStoreObjectStoreDriverImpl.deleteBucket(bucket, TEST_STORE_ID);
        assertTrue(deleted);
        verify(s3Client, times(1)).deleteBucket(TEST_BUCKET_NAME);
    }

    @Test
    public void testSetBucketPolicyPrivate() throws Exception {
        doReturn(s3Client).when(cloudianHyperStoreObjectStoreDriverImpl).getS3ClientByBucketAndStore(any(), anyLong());
        BucketTO bucket = mock(BucketTO.class);
        when(bucket.getName()).thenReturn(TEST_BUCKET_NAME);
        cloudianHyperStoreObjectStoreDriverImpl.setBucketPolicy(bucket, "private", TEST_STORE_ID);
        // private policy is equivalent to deleting any bucket policy
        verify(s3Client, times(1)).deleteBucketPolicy(TEST_BUCKET_NAME);
        verify(s3Client, never()).setBucketPolicy(anyString(), anyString());
    }

    @Test
    public void testSetBucketPolicyPublic() throws Exception {
        doReturn(s3Client).when(cloudianHyperStoreObjectStoreDriverImpl).getS3ClientByBucketAndStore(any(), anyLong());
        BucketTO bucket = mock(BucketTO.class);
        when(bucket.getName()).thenReturn(TEST_BUCKET_NAME);
        cloudianHyperStoreObjectStoreDriverImpl.setBucketPolicy(bucket, "public", TEST_STORE_ID);
        verify(s3Client, times(1)).setBucketPolicy(anyString(), anyString());
        verify(s3Client, never()).deleteBucketPolicy(TEST_BUCKET_NAME);
    }

    @Test(expected = CloudRuntimeException.class)
    public void testSetBucketQuota() {
        BucketTO bucket = mock(BucketTO.class);
        when(bucket.getName()).thenReturn(TEST_BUCKET_NAME);
        // Quota is not implemented by HyperStore, we throw an CloudRuntimeException.
        cloudianHyperStoreObjectStoreDriverImpl.setBucketQuota(bucket, TEST_STORE_ID, 5000L);
    }
}

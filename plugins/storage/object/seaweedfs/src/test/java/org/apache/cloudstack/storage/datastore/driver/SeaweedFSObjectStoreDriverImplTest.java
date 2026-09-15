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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;

import java.io.ByteArrayOutputStream;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import org.apache.cloudstack.storage.datastore.db.ObjectStoreDao;
import org.apache.cloudstack.storage.datastore.db.ObjectStoreDetailsDao;
import org.apache.cloudstack.storage.datastore.db.ObjectStoreVO;
import org.apache.cloudstack.storage.datastore.util.SeaweedFSObjectStoreUtil;
import org.apache.cloudstack.storage.object.Bucket;

import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.model.AccessKey;
import com.amazonaws.services.identitymanagement.model.AccessKeyMetadata;
import com.amazonaws.services.identitymanagement.model.CreateAccessKeyRequest;
import com.amazonaws.services.identitymanagement.model.CreateAccessKeyResult;
import com.amazonaws.services.identitymanagement.model.CreateUserRequest;
import com.amazonaws.services.identitymanagement.model.DeleteAccessKeyRequest;
import com.amazonaws.services.identitymanagement.model.EntityAlreadyExistsException;
import com.amazonaws.services.identitymanagement.model.ListAccessKeysRequest;
import com.amazonaws.services.identitymanagement.model.ListAccessKeysResult;
import com.amazonaws.services.identitymanagement.model.PutUserPolicyRequest;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.BucketVersioningConfiguration;
import com.amazonaws.services.s3.model.CreateBucketRequest;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.s3.model.SetBucketVersioningConfigurationRequest;
import com.cloud.agent.api.to.BucketTO;
import com.cloud.storage.BucketVO;
import com.cloud.storage.dao.BucketDao;
import com.cloud.user.AccountDetailsDao;
import com.cloud.user.AccountVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.exception.CloudRuntimeException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.Silent.class)
public class SeaweedFSObjectStoreDriverImplTest {

    @Spy
    SeaweedFSObjectStoreDriverImpl driver = new SeaweedFSObjectStoreDriverImpl();

    @Mock
    AmazonS3 s3Client;
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
    AccountDetailsDao accountDetailsDao;
    @Mock
    AccountVO account;

    BucketVO bucketVo;
    Map<String, String> storeDetailsMap;
    Map<String, String> accountDetailsMap;

    static long TEST_STORE_ID = 1010L;
    static long TEST_ACCOUNT_ID = 2010L;
    static long TEST_DOMAIN_ID = 3010L;
    static String TEST_ACCESS_KEY = "test_access_key";
    static String TEST_SECRET_KEY = "test_secret_key";
    static String TEST_BUCKET_NAME = "testbucketname";
    static String TEST_S3_URL = "http://s3-endpoint";
    static String TEST_IAM_URL = "http://iam-endpoint";
    static String TEST_AK = "user_access_key";
    static String TEST_SK = "user_secret_key";
    static String TEST_BUCKET_URL = TEST_S3_URL + "/" + TEST_BUCKET_NAME;
    static String TEST_ACCOUNT_UUID = "account-uuid-1234";

    private AutoCloseable closeable;

    @Before
    public void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
        driver._storeDao = objectStoreDao;
        driver._storeDetailsDao = objectStoreDetailsDao;
        driver._accountDao = accountDao;
        driver._bucketDao = bucketDao;
        driver._accountDetailsDao = accountDetailsDao;

        lenient().when(objectStoreDao.findById(TEST_STORE_ID)).thenReturn(objectStoreVO);
        lenient().when(objectStoreVO.getUrl()).thenReturn(TEST_S3_URL);

        storeDetailsMap = new HashMap<>();
        storeDetailsMap.put(SeaweedFSObjectStoreUtil.STORE_DETAILS_KEY_ACCESS_KEY, TEST_ACCESS_KEY);
        storeDetailsMap.put(SeaweedFSObjectStoreUtil.STORE_DETAILS_KEY_SECRET_KEY, TEST_SECRET_KEY);
        storeDetailsMap.put(SeaweedFSObjectStoreUtil.STORE_DETAILS_KEY_S3_URL, TEST_S3_URL);
        storeDetailsMap.put(SeaweedFSObjectStoreUtil.STORE_DETAILS_KEY_IAM_URL, TEST_IAM_URL);
        lenient().when(objectStoreDetailsDao.getDetails(TEST_STORE_ID)).thenReturn(storeDetailsMap);

        accountDetailsMap = new HashMap<>();
        accountDetailsMap.put(SeaweedFSObjectStoreUtil.keyAccessKey(TEST_STORE_ID), TEST_AK);
        accountDetailsMap.put(SeaweedFSObjectStoreUtil.keySecretKey(TEST_STORE_ID), TEST_SK);
        lenient().when(accountDetailsDao.findDetails(TEST_ACCOUNT_ID)).thenReturn(accountDetailsMap);

        bucketVo = new BucketVO(TEST_ACCOUNT_ID, TEST_DOMAIN_ID, TEST_STORE_ID, TEST_BUCKET_NAME, null, false, false, false, null);

        // Stub the DB-backed IAM lock with a no-op mock so tests don't
        // require a real transaction context.
        com.cloud.utils.db.GlobalLock mockIamLock = mock(com.cloud.utils.db.GlobalLock.class);
        lenient().doReturn(mockIamLock).when(driver).acquireIamLock(anyLong(), anyLong());
        lenient().when(mockIamLock.unlock()).thenReturn(true);
    }

    @After
    public void tearDown() throws Exception {
        closeable.close();
    }

    @Test
    public void testGetStoreTO() {
        assertNull(driver.getStoreTO(null));
    }

    @Test
    public void testCreateBucket() throws Exception {
        doReturn(s3Client).when(driver).getS3ClientByStoreId(TEST_STORE_ID);
        when(s3Client.doesBucketExistV2(TEST_BUCKET_NAME)).thenReturn(false);
        when(bucketDao.findById(anyLong())).thenReturn(bucketVo);

        Bucket result = driver.createBucket(bucketVo, false);

        assertEquals(TEST_BUCKET_NAME, result.getName());

        ArgumentCaptor<BucketVO> captor = ArgumentCaptor.forClass(BucketVO.class);
        verify(bucketDao, times(1)).update(any(), captor.capture());
        BucketVO updated = captor.getValue();
        assertEquals(TEST_AK, updated.getAccessKey());
        assertEquals(TEST_SK, updated.getSecretKey());
        assertEquals(TEST_BUCKET_URL, updated.getBucketURL());

        verify(s3Client, times(1)).createBucket(any(CreateBucketRequest.class));
    }

    @Test
    public void testCreateBucketAlreadyExists() throws Exception {
        doReturn(s3Client).when(driver).getS3ClientByStoreId(TEST_STORE_ID);
        when(s3Client.doesBucketExistV2(TEST_BUCKET_NAME)).thenReturn(true);

        assertThrows(CloudRuntimeException.class, () -> driver.createBucket(bucketVo, false));
        verify(s3Client, never()).createBucket(any(CreateBucketRequest.class));
    }

    @Test
    public void testListBuckets() throws Exception {
        doReturn(s3Client).when(driver).getS3ClientByStoreId(TEST_STORE_ID);
        List<com.amazonaws.services.s3.model.Bucket> s3Buckets = new ArrayList<>();
        s3Buckets.add(new com.amazonaws.services.s3.model.Bucket("bucket1"));
        s3Buckets.add(new com.amazonaws.services.s3.model.Bucket("bucket2"));
        when(s3Client.listBuckets()).thenReturn(s3Buckets);

        List<Bucket> result = driver.listBuckets(TEST_STORE_ID);

        assertEquals(2, result.size());
        assertEquals("bucket1", result.get(0).getName());
        assertEquals("bucket2", result.get(1).getName());
    }

    @Test
    public void testDeleteBucket() throws Exception {
        doReturn(s3Client).when(driver).getS3ClientByStoreId(TEST_STORE_ID);
        BucketTO bucketTO = mock(BucketTO.class);
        when(bucketTO.getName()).thenReturn(TEST_BUCKET_NAME);
        when(s3Client.doesBucketExistV2(TEST_BUCKET_NAME)).thenReturn(true);

        assertTrue(driver.deleteBucket(bucketTO, TEST_STORE_ID));
        verify(s3Client, times(1)).deleteBucket(TEST_BUCKET_NAME);
    }

    @Test
    public void testDeleteBucketRemovesRowInsideLock() throws Exception {
        doReturn(s3Client).when(driver).getS3ClientByStoreId(TEST_STORE_ID);
        BucketTO bucketTO = mock(BucketTO.class);
        when(bucketTO.getName()).thenReturn(TEST_BUCKET_NAME);
        when(bucketTO.getAccountId()).thenReturn(TEST_ACCOUNT_ID);
        when(s3Client.doesBucketExistV2(TEST_BUCKET_NAME)).thenReturn(true);

        BucketVO existing = new BucketVO(TEST_ACCOUNT_ID, TEST_DOMAIN_ID, TEST_STORE_ID, TEST_BUCKET_NAME,
                null, false, false, false, null);
        List<BucketVO> buckets = new ArrayList<>();
        buckets.add(existing);
        when(bucketDao.listByObjectStoreIdAndAccountId(TEST_STORE_ID, TEST_ACCOUNT_ID)).thenReturn(buckets);

        assertTrue(driver.deleteBucket(bucketTO, TEST_STORE_ID));
        // The row must be removed by the driver (inside the IAM lock, after the
        // policy refresh) so a concurrent policy rebuild cannot observe the
        // stale row and re-add the deleted bucket ARN.
        verify(bucketDao, times(1)).remove(existing.getId());
    }

    @Test
    public void testDeleteBucketNotFound() throws Exception {
        doReturn(s3Client).when(driver).getS3ClientByStoreId(TEST_STORE_ID);
        BucketTO bucketTO = mock(BucketTO.class);
        when(bucketTO.getName()).thenReturn(TEST_BUCKET_NAME);
        when(bucketTO.getAccountId()).thenReturn(TEST_ACCOUNT_ID);
        when(s3Client.doesBucketExistV2(TEST_BUCKET_NAME)).thenReturn(false);

        // Idempotent: if the bucket is already gone (e.g. from a previous
        // partial failure), skip the S3 delete and proceed to policy refresh.
        assertTrue(driver.deleteBucket(bucketTO, TEST_STORE_ID));
        verify(s3Client, never()).deleteBucket(TEST_BUCKET_NAME);
    }

    @Test
    public void testSetBucketVersioning() throws Exception {
        doReturn(s3Client).when(driver).getS3ClientByStoreId(TEST_STORE_ID);
        BucketTO bucketTO = mock(BucketTO.class);
        when(bucketTO.getName()).thenReturn(TEST_BUCKET_NAME);

        assertTrue(driver.setBucketVersioning(bucketTO, TEST_STORE_ID));
        verify(s3Client, times(1)).setBucketVersioningConfiguration(any(SetBucketVersioningConfigurationRequest.class));
    }

    @Test
    public void testDeleteBucketVersioning() throws Exception {
        doReturn(s3Client).when(driver).getS3ClientByStoreId(TEST_STORE_ID);
        BucketTO bucketTO = mock(BucketTO.class);
        when(bucketTO.getName()).thenReturn(TEST_BUCKET_NAME);

        assertTrue(driver.deleteBucketVersioning(bucketTO, TEST_STORE_ID));
        ArgumentCaptor<SetBucketVersioningConfigurationRequest> captor =
                ArgumentCaptor.forClass(SetBucketVersioningConfigurationRequest.class);
        verify(s3Client, times(1)).setBucketVersioningConfiguration(captor.capture());
        assertEquals(BucketVersioningConfiguration.SUSPENDED, captor.getValue().getVersioningConfiguration().getStatus());
    }

    @Test
    public void testSetBucketQuotaZero() throws Exception {
        BucketTO bucketTO = mock(BucketTO.class);
        when(bucketTO.getName()).thenReturn(TEST_BUCKET_NAME);
        doReturn(TEST_S3_URL).when(driver).getS3Url(TEST_STORE_ID);
        doReturn("access-key").when(driver).getAccessKey(TEST_STORE_ID);
        doReturn("secret-key").when(driver).getSecretKey(TEST_STORE_ID);

        HttpClient mockHttpClient = mock(HttpClient.class);
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn("");
        when(mockHttpClient.send(ArgumentMatchers.<HttpRequest>any(),
                ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenReturn(mockResponse);
        doReturn(mockHttpClient).when(driver).getS3ExtensionHttpClient();

        driver.setBucketQuota(bucketTO, TEST_STORE_ID, 0);

        ArgumentCaptor<HttpRequest> reqCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(mockHttpClient, times(1)).send(reqCaptor.capture(),
                ArgumentMatchers.<HttpResponse.BodyHandler<String>>any());
        HttpRequest sent = reqCaptor.getValue();
        assertEquals("PUT", sent.method());
        assertEquals("/" + TEST_BUCKET_NAME, sent.uri().getPath());
        assertTrue("query must carry the seaweedfs-quota subresource",
                sent.uri().getQuery().contains("seaweedfs-quota"));
        assertNotNull("request must be SigV4-signed", sent.headers().firstValue("Authorization"));
        assertEquals("{\"quota_size\":0,\"quota_unit\":\"B\",\"quota_enabled\":false}", extractBody(sent));
    }

    @Test
    public void testSetBucketQuotaNegativeRejected() {
        BucketTO bucketTO = mock(BucketTO.class);
        when(bucketTO.getName()).thenReturn(TEST_BUCKET_NAME);
        doReturn(TEST_S3_URL).when(driver).getS3Url(TEST_STORE_ID);
        doReturn("access-key").when(driver).getAccessKey(TEST_STORE_ID);
        doReturn("secret-key").when(driver).getSecretKey(TEST_STORE_ID);
        // Negative quotas must be rejected, not treated as a disable
        assertThrows(CloudRuntimeException.class, () -> driver.setBucketQuota(bucketTO, TEST_STORE_ID, -1));
    }

    @Test
    public void testSetBucketQuotaNonZero() throws Exception {
        BucketTO bucketTO = mock(BucketTO.class);
        when(bucketTO.getName()).thenReturn(TEST_BUCKET_NAME);
        doReturn(TEST_S3_URL).when(driver).getS3Url(TEST_STORE_ID);
        doReturn("access-key").when(driver).getAccessKey(TEST_STORE_ID);
        doReturn("secret-key").when(driver).getSecretKey(TEST_STORE_ID);

        HttpClient mockHttpClient = mock(HttpClient.class);
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn("");
        when(mockHttpClient.send(ArgumentMatchers.<HttpRequest>any(),
                ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenReturn(mockResponse);
        doReturn(mockHttpClient).when(driver).getS3ExtensionHttpClient();

        driver.setBucketQuota(bucketTO, TEST_STORE_ID, 10);

        ArgumentCaptor<HttpRequest> reqCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(mockHttpClient, times(1)).send(reqCaptor.capture(),
                ArgumentMatchers.<HttpResponse.BodyHandler<String>>any());
        HttpRequest sent = reqCaptor.getValue();
        assertEquals("PUT", sent.method());
        assertEquals("/" + TEST_BUCKET_NAME, sent.uri().getPath());
        assertTrue(sent.uri().getQuery().contains("seaweedfs-quota"));
        assertNotNull(sent.headers().firstValue("Authorization"));
        assertEquals("{\"quota_size\":10,\"quota_unit\":\"GB\",\"quota_enabled\":true}", extractBody(sent));
    }

    @Test
    public void testSetBucketQuotaPropagatesFailure() throws Exception {
        BucketTO bucketTO = mock(BucketTO.class);
        when(bucketTO.getName()).thenReturn(TEST_BUCKET_NAME);
        doReturn(TEST_S3_URL).when(driver).getS3Url(TEST_STORE_ID);
        doReturn("access-key").when(driver).getAccessKey(TEST_STORE_ID);
        doReturn("secret-key").when(driver).getSecretKey(TEST_STORE_ID);

        HttpClient mockHttpClient = mock(HttpClient.class);
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(403);
        when(mockResponse.body()).thenReturn("forbidden");
        when(mockHttpClient.send(ArgumentMatchers.<HttpRequest>any(),
                ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenReturn(mockResponse);
        doReturn(mockHttpClient).when(driver).getS3ExtensionHttpClient();

        assertThrows(CloudRuntimeException.class, () -> driver.setBucketQuota(bucketTO, TEST_STORE_ID, 10));
    }

    @Test
    public void testSetBucketQuotaZeroTolerates404() throws Exception {
        BucketTO bucketTO = mock(BucketTO.class);
        when(bucketTO.getName()).thenReturn(TEST_BUCKET_NAME);
        doReturn(TEST_S3_URL).when(driver).getS3Url(TEST_STORE_ID);
        doReturn("access-key").when(driver).getAccessKey(TEST_STORE_ID);
        doReturn("secret-key").when(driver).getSecretKey(TEST_STORE_ID);

        HttpClient mockHttpClient = mock(HttpClient.class);
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(404);
        when(mockResponse.body()).thenReturn("not found");
        when(mockHttpClient.send(ArgumentMatchers.<HttpRequest>any(),
                ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenReturn(mockResponse);
        doReturn(mockHttpClient).when(driver).getS3ExtensionHttpClient();

        // Quota 0 (disable) tolerates 404 so bucket creation works on
        // deployments without the SeaweedFS quota extension.
        driver.setBucketQuota(bucketTO, TEST_STORE_ID, 0);
    }

    @Test
    public void testSetBucketQuotaClearExistingPropagates404() throws Exception {
        BucketTO bucketTO = mock(BucketTO.class);
        when(bucketTO.getName()).thenReturn(TEST_BUCKET_NAME);
        when(bucketTO.getAccountId()).thenReturn(TEST_ACCOUNT_ID);
        doReturn(TEST_S3_URL).when(driver).getS3Url(TEST_STORE_ID);
        doReturn("access-key").when(driver).getAccessKey(TEST_STORE_ID);
        doReturn("secret-key").when(driver).getSecretKey(TEST_STORE_ID);

        // The bucket already has a positive quota, so a quota 0 request is a
        // clear of an existing quota. A 404 must NOT be tolerated: reporting
        // success would lower CloudStack accounting while SeaweedFS keeps the
        // old quota and read-only state.
        List<BucketVO> buckets = new ArrayList<>();
        buckets.add(new BucketVO(TEST_ACCOUNT_ID, TEST_DOMAIN_ID, TEST_STORE_ID, TEST_BUCKET_NAME, 100, false, false, false, null));
        when(bucketDao.listByObjectStoreIdAndAccountId(TEST_STORE_ID, TEST_ACCOUNT_ID)).thenReturn(buckets);

        HttpClient mockHttpClient = mock(HttpClient.class);
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(404);
        when(mockResponse.body()).thenReturn("not found");
        when(mockHttpClient.send(ArgumentMatchers.<HttpRequest>any(),
                ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenReturn(mockResponse);
        doReturn(mockHttpClient).when(driver).getS3ExtensionHttpClient();

        assertThrows(CloudRuntimeException.class, () -> driver.setBucketQuota(bucketTO, TEST_STORE_ID, 0));
    }

    @Test
    public void testSetBucketQuotaRejects3xx() throws Exception {
        BucketTO bucketTO = mock(BucketTO.class);
        when(bucketTO.getName()).thenReturn(TEST_BUCKET_NAME);
        doReturn(TEST_S3_URL).when(driver).getS3Url(TEST_STORE_ID);
        doReturn("access-key").when(driver).getAccessKey(TEST_STORE_ID);
        doReturn("secret-key").when(driver).getSecretKey(TEST_STORE_ID);

        HttpClient mockHttpClient = mock(HttpClient.class);
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        // 3xx must NOT be treated as success — the mutation was not applied
        when(mockResponse.statusCode()).thenReturn(302);
        when(mockResponse.body()).thenReturn("redirect");
        when(mockHttpClient.send(ArgumentMatchers.<HttpRequest>any(),
                ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenReturn(mockResponse);
        doReturn(mockHttpClient).when(driver).getS3ExtensionHttpClient();

        assertThrows(CloudRuntimeException.class, () -> driver.setBucketQuota(bucketTO, TEST_STORE_ID, 10));
    }

    /**
     * Deterministic SigV4 signature-verification test.
     *
     * Signs the same request through the AWS SDK v1 AWSS3V4Signer (the same
     * signer the production code uses) and asserts that the Authorization
     * header, signed headers, x-amz-content-sha256, and x-amz-date produced
     * by the driver's request match. This catches signing regressions (e.g.
     * the query parameter not being in the canonical query string) that a
     * mere "header exists" check would miss.
     */
    @Test
    public void testSetBucketQuotaSigV4SignatureVerification() throws Exception {
        String accessKey = "AKIAIOSFODNN7EXAMPLE";
        String secretKey = "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY";
        String bucketName = "quota-sig-test";
        String s3Url = "http://s3.example.com:8333";
        long quotaGiB = 5;

        BucketTO bucketTO = mock(BucketTO.class);
        when(bucketTO.getName()).thenReturn(bucketName);
        doReturn(s3Url).when(driver).getS3Url(TEST_STORE_ID);
        doReturn(accessKey).when(driver).getAccessKey(TEST_STORE_ID);
        doReturn(secretKey).when(driver).getSecretKey(TEST_STORE_ID);

        HttpClient mockHttpClient = mock(HttpClient.class);
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn("");
        when(mockHttpClient.send(ArgumentMatchers.<HttpRequest>any(),
                ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenReturn(mockResponse);
        doReturn(mockHttpClient).when(driver).getS3ExtensionHttpClient();

        driver.setBucketQuota(bucketTO, TEST_STORE_ID, quotaGiB);

        ArgumentCaptor<HttpRequest> reqCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(mockHttpClient, times(1)).send(reqCaptor.capture(),
                ArgumentMatchers.<HttpResponse.BodyHandler<String>>any());
        HttpRequest sent = reqCaptor.getValue();

        // Build the expected signed request the same way the production code does
        String expectedBody = String.format("{\"quota_size\":%d,\"quota_unit\":\"GB\",\"quota_enabled\":true}", quotaGiB);
        byte[] bodyBytes = expectedBody.getBytes(StandardCharsets.UTF_8);

        com.amazonaws.DefaultRequest<?> expectedRequest = new com.amazonaws.DefaultRequest<>("s3");
        expectedRequest.setEndpoint(java.net.URI.create(s3Url));
        expectedRequest.setHttpMethod(com.amazonaws.http.HttpMethodName.PUT);
        expectedRequest.setResourcePath("/" + bucketName);
        expectedRequest.addParameter("seaweedfs-quota", "");
        expectedRequest.setContent(new java.io.ByteArrayInputStream(bodyBytes));
        expectedRequest.getHeaders().put("Content-Length", String.valueOf(bodyBytes.length));
        expectedRequest.getHeaders().put("Content-Type", "application/json");

        // Fix the signing timestamp to match the production request so the
        // test is deterministic and does not intermittently fail when the two
        // signings straddle a one-second boundary.
        String productionDate = sent.headers().firstValue("x-amz-date").orElse(null);
        assertNotNull("production request must carry x-amz-date", productionDate);
        expectedRequest.getHeaders().put("x-amz-date", productionDate);

        com.amazonaws.auth.AWSCredentials credentials = new com.amazonaws.auth.BasicAWSCredentials(accessKey, secretKey);
        com.amazonaws.services.s3.internal.AWSS3V4Signer signer = new com.amazonaws.services.s3.internal.AWSS3V4Signer();
        signer.setServiceName("s3");
        signer.setRegionName("us-east-1");
        signer.sign(expectedRequest, credentials);

        // The Authorization header must match exactly — proves the canonical
        // query string (including seaweedfs-quota), payload hash, and signed
        // headers all match the independently signed reference request.
        String expectedAuth = expectedRequest.getHeaders().get("Authorization");
        String actualAuth = sent.headers().firstValue("Authorization").orElse(null);
        assertNotNull("Authorization header must be present", actualAuth);
        assertEquals("SigV4 Authorization header must match the reference signature", expectedAuth, actualAuth);

        // The payload hash must be present and match
        String expectedContentSha = expectedRequest.getHeaders().get("x-amz-content-sha256");
        String actualContentSha = sent.headers().firstValue("x-amz-content-sha256").orElse(null);
        assertEquals("x-amz-content-sha256 must match", expectedContentSha, actualContentSha);

        // The signed headers list must include the query-signing-relevant headers
        String expectedDate = expectedRequest.getHeaders().get("x-amz-date");
        String actualDate = sent.headers().firstValue("x-amz-date").orElse(null);
        assertEquals("x-amz-date must match", expectedDate, actualDate);

        // The query string must carry the subresource
        assertNotNull("URI must have a query string", sent.uri().getQuery());
        assertTrue("query must carry seaweedfs-quota", sent.uri().getQuery().contains("seaweedfs-quota"));
    }

    @Test
    public void testSetBucketQuotaNoS3ConfigThrows() {
        BucketTO bucketTO = mock(BucketTO.class);
        when(bucketTO.getName()).thenReturn(TEST_BUCKET_NAME);
        // Clear store details so no S3 URL/credentials are configured.
        // Without this, setUp() stubs valid values and the exception would
        // come from a real network call rather than the missing-config check.
        storeDetailsMap.clear();
        lenient().when(objectStoreDao.findById(TEST_STORE_ID)).thenReturn(null);
        assertThrows(CloudRuntimeException.class, () -> driver.setBucketQuota(bucketTO, TEST_STORE_ID, 10));
    }

    @Test
    public void testBuildAccountIAMPolicyEmptyBuckets() throws Exception {
        String policy = SeaweedFSObjectStoreUtil.buildAccountIAMPolicy(java.util.Collections.emptyList());
        // Empty bucket list: deny all S3 access
        assertTrue(policy.contains("\"Sid\": \"DenyAllS3\""));
        assertTrue(policy.contains("\"Effect\": \"Deny\""));
        assertTrue(policy.contains("\"Action\": [\"s3:*\"]"));
        assertTrue(policy.contains("\"arn:aws:s3:::*\""));
        // Must still deny bucket lifecycle and quota
        assertTrue(policy.contains("\"s3:PutBucketQuota\""));
        assertFalse(policy.contains("\"AllowAccountBuckets\""));
    }

    @Test
    public void testBuildAccountIAMPolicyPopulatedBuckets() throws Exception {
        String policy = SeaweedFSObjectStoreUtil.buildAccountIAMPolicy(
                java.util.Arrays.asList("bucket-a", "bucket-b"));
        // Allow access to both bucket and object ARNs
        assertTrue(policy.contains("\"Sid\": \"AllowAccountBuckets\""));
        assertTrue(policy.contains("\"arn:aws:s3:::bucket-a\""));
        assertTrue(policy.contains("\"arn:aws:s3:::bucket-a/*\""));
        assertTrue(policy.contains("\"arn:aws:s3:::bucket-b\""));
        assertTrue(policy.contains("\"arn:aws:s3:::bucket-b/*\""));
        // Must deny bucket lifecycle and quota
        assertTrue(policy.contains("\"Sid\": \"DenyBucketLifecycleAndQuota\""));
        assertTrue(policy.contains("\"s3:CreateBucket\""));
        assertTrue(policy.contains("\"s3:DeleteBucket\""));
        assertTrue(policy.contains("\"s3:PutBucketQuota\""));
        assertFalse(policy.contains("\"DenyAllS3\""));
    }

    /**
     * Extract the request body from an HttpRequest.BodyPublisher so tests can
     * assert the JSON payload sent to the SeaweedFS S3 extension.
     */
    private static String extractBody(HttpRequest request) throws Exception {
        return request.bodyPublisher()
                .map(SeaweedFSObjectStoreDriverImplTest::readBodyPublisher)
                .orElse(null);
    }

    private static String readBodyPublisher(HttpRequest.BodyPublisher publisher) {
        CompletableFuture<String> future = new CompletableFuture<>();
        publisher.subscribe(new Flow.Subscriber<ByteBuffer>() {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            @Override public void onSubscribe(Flow.Subscription s) { s.request(Long.MAX_VALUE); }
            @Override public void onNext(ByteBuffer b) {
                byte[] arr = new byte[b.remaining()];
                b.get(arr);
                baos.write(arr, 0, arr.length);
            }
            @Override public void onError(Throwable t) { future.completeExceptionally(t); }
            @Override public void onComplete() { future.complete(baos.toString(StandardCharsets.UTF_8)); }
        });
        return future.join();
    }

    @Test
    public void testCreateUserNew() throws Exception {
        when(accountDao.findById(TEST_ACCOUNT_ID)).thenReturn(account);
        when(account.getUuid()).thenReturn(TEST_ACCOUNT_UUID);
        when(account.getAccountName()).thenReturn("testaccount");
        doReturn(iamClient).when(driver).getIAMClient(TEST_STORE_ID);

        // No stored credentials yet
        accountDetailsMap.clear();
        // No existing access keys to clean up
        when(iamClient.listAccessKeys(any(ListAccessKeysRequest.class)))
                .thenReturn(listAccessKeysResult());

        // Access key creation
        AccessKey accessKey = mock(AccessKey.class);
        CreateAccessKeyResult accessKeyResult = mock(CreateAccessKeyResult.class);
        when(accessKey.getAccessKeyId()).thenReturn(TEST_AK);
        when(accessKey.getSecretAccessKey()).thenReturn(TEST_SK);
        when(accessKeyResult.getAccessKey()).thenReturn(accessKey);
        when(iamClient.createAccessKey(any(CreateAccessKeyRequest.class))).thenReturn(accessKeyResult);

        boolean created = driver.createUser(TEST_ACCOUNT_ID, TEST_STORE_ID);
        assertTrue(created);

        verify(iamClient, times(1)).createUser(any(CreateUserRequest.class));
        verify(iamClient, times(1)).putUserPolicy(any(PutUserPolicyRequest.class));
        verify(iamClient, times(1)).createAccessKey(any(CreateAccessKeyRequest.class));

        ArgumentCaptor<Map<String, String>> detailsCaptor = ArgumentCaptor.forClass((Class<Map<String, String>>) (Class<?>) Map.class);
        verify(accountDetailsDao, times(1)).persist(anyLong(), detailsCaptor.capture());
        Map<String, String> persisted = detailsCaptor.getValue();
        assertEquals(TEST_AK, persisted.get(SeaweedFSObjectStoreUtil.keyAccessKey(TEST_STORE_ID)));
        assertEquals(TEST_SK, persisted.get(SeaweedFSObjectStoreUtil.keySecretKey(TEST_STORE_ID)));
    }

    @Test
    public void testCreateUserReusesStoredKey() throws Exception {
        when(accountDao.findById(TEST_ACCOUNT_ID)).thenReturn(account);
        when(account.getUuid()).thenReturn(TEST_ACCOUNT_UUID);
        when(account.getAccountName()).thenReturn("testaccount");
        doReturn(iamClient).when(driver).getIAMClient(TEST_STORE_ID);

        // Stored credential still exists in IAM -> must be reused, not rotated
        when(iamClient.listAccessKeys(any(ListAccessKeysRequest.class)))
                .thenReturn(listAccessKeysResult(TEST_AK));

        boolean created = driver.createUser(TEST_ACCOUNT_ID, TEST_STORE_ID);
        assertTrue(created);

        verify(iamClient, times(1)).putUserPolicy(any(PutUserPolicyRequest.class));
        verify(iamClient, never()).createAccessKey(any(CreateAccessKeyRequest.class));
        verify(iamClient, never()).deleteAccessKey(any(DeleteAccessKeyRequest.class));
        verify(accountDetailsDao, never()).persist(anyLong(), ArgumentMatchers.<Map<String, String>>any());
    }

    @Test
    public void testCreateUserRotatesInactiveKey() throws Exception {
        when(accountDao.findById(TEST_ACCOUNT_ID)).thenReturn(account);
        when(account.getUuid()).thenReturn(TEST_ACCOUNT_UUID);
        when(account.getAccountName()).thenReturn("testaccount");
        doReturn(iamClient).when(driver).getIAMClient(TEST_STORE_ID);

        // Stored key exists in IAM but is Inactive -> must rotate, not reuse
        when(iamClient.listAccessKeys(any(ListAccessKeysRequest.class)))
                .thenReturn(listAccessKeysResultInactive(TEST_AK));

        AccessKey accessKey = mock(AccessKey.class);
        CreateAccessKeyResult accessKeyResult = mock(CreateAccessKeyResult.class);
        when(accessKey.getAccessKeyId()).thenReturn("new-ak");
        when(accessKey.getSecretAccessKey()).thenReturn("new-sk");
        when(accessKeyResult.getAccessKey()).thenReturn(accessKey);
        when(iamClient.createAccessKey(any(CreateAccessKeyRequest.class))).thenReturn(accessKeyResult);

        boolean created = driver.createUser(TEST_ACCOUNT_ID, TEST_STORE_ID);
        assertTrue(created);

        // The inactive key must be cleaned up and a new one created
        verify(iamClient, times(1)).deleteAccessKey(any(DeleteAccessKeyRequest.class));
        verify(iamClient, times(1)).createAccessKey(any(CreateAccessKeyRequest.class));
    }

    @Test
    public void testCreateUserStoredKeyMissingCreatesReplacement() throws Exception {
        when(accountDao.findById(TEST_ACCOUNT_ID)).thenReturn(account);
        when(account.getUuid()).thenReturn(TEST_ACCOUNT_UUID);
        when(account.getAccountName()).thenReturn("testaccount");
        doReturn(iamClient).when(driver).getIAMClient(TEST_STORE_ID);

        // Stored key is gone from IAM; an unmanaged leftover key is present
        when(iamClient.listAccessKeys(any(ListAccessKeysRequest.class)))
                .thenReturn(listAccessKeysResult("unmanaged-key"));

        AccessKey accessKey = mock(AccessKey.class);
        CreateAccessKeyResult accessKeyResult = mock(CreateAccessKeyResult.class);
        when(accessKey.getAccessKeyId()).thenReturn("new-ak");
        when(accessKey.getSecretAccessKey()).thenReturn("new-sk");
        when(accessKeyResult.getAccessKey()).thenReturn(accessKey);
        when(iamClient.createAccessKey(any(CreateAccessKeyRequest.class))).thenReturn(accessKeyResult);

        boolean created = driver.createUser(TEST_ACCOUNT_ID, TEST_STORE_ID);
        assertTrue(created);

        // The unmanaged leftover key must be cleaned up before creating a replacement
        verify(iamClient, times(1)).deleteAccessKey(any(DeleteAccessKeyRequest.class));
        verify(iamClient, times(1)).createAccessKey(any(CreateAccessKeyRequest.class));

        ArgumentCaptor<Map<String, String>> detailsCaptor = ArgumentCaptor.forClass((Class<Map<String, String>>) (Class<?>) Map.class);
        verify(accountDetailsDao, times(1)).persist(anyLong(), detailsCaptor.capture());
        Map<String, String> persisted = detailsCaptor.getValue();
        assertEquals("new-ak", persisted.get(SeaweedFSObjectStoreUtil.keyAccessKey(TEST_STORE_ID)));
        assertEquals("new-sk", persisted.get(SeaweedFSObjectStoreUtil.keySecretKey(TEST_STORE_ID)));
    }

    @Test
    public void testCreateUserAlreadyExists() throws Exception {
        when(accountDao.findById(TEST_ACCOUNT_ID)).thenReturn(account);
        when(account.getUuid()).thenReturn(TEST_ACCOUNT_UUID);
        when(account.getAccountName()).thenReturn("testaccount");
        doReturn(iamClient).when(driver).getIAMClient(TEST_STORE_ID);

        // IAM user already exists; no stored credential, no leftover keys
        accountDetailsMap.clear();
        lenient().when(iamClient.createUser(any(CreateUserRequest.class)))
                .thenThrow(new EntityAlreadyExistsException("user exists"));
        when(iamClient.listAccessKeys(any(ListAccessKeysRequest.class)))
                .thenReturn(listAccessKeysResult());

        AccessKey accessKey = mock(AccessKey.class);
        CreateAccessKeyResult accessKeyResult = mock(CreateAccessKeyResult.class);
        when(accessKey.getAccessKeyId()).thenReturn(TEST_AK);
        when(accessKey.getSecretAccessKey()).thenReturn(TEST_SK);
        when(accessKeyResult.getAccessKey()).thenReturn(accessKey);
        when(iamClient.createAccessKey(any(CreateAccessKeyRequest.class))).thenReturn(accessKeyResult);

        boolean created = driver.createUser(TEST_ACCOUNT_ID, TEST_STORE_ID);
        assertTrue(created);

        // Policy and access key should still be applied even if user already existed
        verify(iamClient, times(1)).putUserPolicy(any(PutUserPolicyRequest.class));
        verify(iamClient, times(1)).createAccessKey(any(CreateAccessKeyRequest.class));
    }

    private static ListAccessKeysResult listAccessKeysResult(String... accessKeyIds) {
        ListAccessKeysResult result = new ListAccessKeysResult();
        List<AccessKeyMetadata> metadata = new ArrayList<>();
        for (String keyId : accessKeyIds) {
            metadata.add(new AccessKeyMetadata().withAccessKeyId(keyId).withStatus("Active"));
        }
        result.setAccessKeyMetadata(metadata);
        return result;
    }

    private static ListAccessKeysResult listAccessKeysResultInactive(String... accessKeyIds) {
        ListAccessKeysResult result = new ListAccessKeysResult();
        List<AccessKeyMetadata> metadata = new ArrayList<>();
        for (String keyId : accessKeyIds) {
            metadata.add(new AccessKeyMetadata().withAccessKeyId(keyId).withStatus("Inactive"));
        }
        result.setAccessKeyMetadata(metadata);
        return result;
    }

    @Test
    public void testCreateUserAccountNotFound() {
        when(accountDao.findById(TEST_ACCOUNT_ID)).thenReturn(null);
        boolean created = driver.createUser(TEST_ACCOUNT_ID, TEST_STORE_ID);
        assertFalse(created);
    }

    @Test
    public void testGetAllBucketsUsageEmpty() {
        when(bucketDao.listByObjectStoreId(TEST_STORE_ID)).thenReturn(new ArrayList<>());
        Map<String, Long> usage = driver.getAllBucketsUsage(TEST_STORE_ID);
        assertNotNull(usage);
        assertTrue(usage.isEmpty());
    }

    @Test
    public void testGetAllBucketsUsage() throws Exception {
        doReturn(s3Client).when(driver).getS3ClientByStoreId(TEST_STORE_ID);

        List<BucketVO> buckets = new ArrayList<>();
        buckets.add(new BucketVO(TEST_ACCOUNT_ID, TEST_DOMAIN_ID, TEST_STORE_ID, "b1", null, false, false, false, null));
        buckets.add(new BucketVO(TEST_ACCOUNT_ID, TEST_DOMAIN_ID, TEST_STORE_ID, "b2", null, false, false, false, null));
        when(bucketDao.listByObjectStoreId(TEST_STORE_ID)).thenReturn(buckets);

        // b1: two objects of 100 and 200 bytes
        ListObjectsV2Result b1Result = mock(ListObjectsV2Result.class);
        S3ObjectSummary s1 = new S3ObjectSummary(); s1.setSize(100L);
        S3ObjectSummary s2 = new S3ObjectSummary(); s2.setSize(200L);
        List<S3ObjectSummary> b1Summaries = new ArrayList<>(); b1Summaries.add(s1); b1Summaries.add(s2);
        when(b1Result.getObjectSummaries()).thenReturn(b1Summaries);
        when(b1Result.isTruncated()).thenReturn(false);

        // b2: one object of 500 bytes
        ListObjectsV2Result b2Result = mock(ListObjectsV2Result.class);
        S3ObjectSummary s3 = new S3ObjectSummary(); s3.setSize(500L);
        List<S3ObjectSummary> b2Summaries = new ArrayList<>(); b2Summaries.add(s3);
        when(b2Result.getObjectSummaries()).thenReturn(b2Summaries);
        when(b2Result.isTruncated()).thenReturn(false);

        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class)))
                .thenReturn(b1Result)
                .thenReturn(b2Result);

        Map<String, Long> usage = driver.getAllBucketsUsage(TEST_STORE_ID);
        assertNotNull(usage);
        assertEquals(2, usage.size());
        assertEquals(300L, usage.get("b1").longValue());
        assertEquals(500L, usage.get("b2").longValue());
    }

    @Test
    public void testGetAllBucketsUsageFromMetrics() throws Exception {
        doReturn("http://metrics.local:9327").when(driver).getMetricsUrl(TEST_STORE_ID);

        List<BucketVO> buckets = new ArrayList<>();
        buckets.add(new BucketVO(TEST_ACCOUNT_ID, TEST_DOMAIN_ID, TEST_STORE_ID, "b1", null, false, false, false, null));
        buckets.add(new BucketVO(TEST_ACCOUNT_ID, TEST_DOMAIN_ID, TEST_STORE_ID, "b2", null, false, false, false, null));
        when(bucketDao.listByObjectStoreId(TEST_STORE_ID)).thenReturn(buckets);

        // Mock the HTTP client to return a Prometheus text exposition response
        String metricsBody = "# HELP SeaweedFS_s3_bucket_size_bytes Current size\n" +
                "SeaweedFS_s3_bucket_size_bytes{bucket=\"b1\"} 12345678\n" +
                "SeaweedFS_s3_bucket_size_bytes{bucket=\"b2\"} 87654321\n" +
                "SeaweedFS_s3_bucket_size_bytes{bucket=\"other\"} 999\n";
        HttpClient mockHttpClient = mock(HttpClient.class);
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn(metricsBody);
        when(mockHttpClient.send(ArgumentMatchers.<HttpRequest>any(),
                ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
                .thenReturn(mockResponse);
        doReturn(mockHttpClient).when(driver).getS3ExtensionHttpClient();

        Map<String, Long> usage = driver.getAllBucketsUsage(TEST_STORE_ID);
        assertNotNull(usage);
        assertEquals(2, usage.size());
        assertEquals(12345678L, usage.get("b1").longValue());
        assertEquals(87654321L, usage.get("b2").longValue());
        // "other" bucket is not managed by CloudStack and must be filtered out
        assertFalse(usage.containsKey("other"));
        // S3 ListObjectsV2 must not be called when metricsUrl is configured
        verify(s3Client, never()).listObjectsV2(any(ListObjectsV2Request.class));
    }

    @Test
    public void testGetAllBucketsUsageMetricsFailureFallsBackToList() throws Exception {
        doReturn("http://metrics.local:9327").when(driver).getMetricsUrl(TEST_STORE_ID);
        doReturn(s3Client).when(driver).getS3ClientByStoreId(TEST_STORE_ID);

        List<BucketVO> buckets = new ArrayList<>();
        buckets.add(new BucketVO(TEST_ACCOUNT_ID, TEST_DOMAIN_ID, TEST_STORE_ID, "b1", null, false, false, false, null));
        when(bucketDao.listByObjectStoreId(TEST_STORE_ID)).thenReturn(buckets);

        // Metrics scrape returns HTTP 503 -> fallback to ListObjectsV2
        HttpClient mockHttpClient = mock(HttpClient.class);
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(503);
        when(mockResponse.body()).thenReturn("Service Unavailable");
        when(mockHttpClient.send(ArgumentMatchers.<HttpRequest>any(),
                ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
                .thenReturn(mockResponse);
        doReturn(mockHttpClient).when(driver).getS3ExtensionHttpClient();

        ListObjectsV2Result b1Result = mock(ListObjectsV2Result.class);
        S3ObjectSummary s1 = new S3ObjectSummary(); s1.setSize(42L);
        List<S3ObjectSummary> summaries = new ArrayList<>(); summaries.add(s1);
        when(b1Result.getObjectSummaries()).thenReturn(summaries);
        when(b1Result.isTruncated()).thenReturn(false);
        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class))).thenReturn(b1Result);

        Map<String, Long> usage = driver.getAllBucketsUsage(TEST_STORE_ID);
        assertNotNull(usage);
        assertEquals(1, usage.size());
        assertEquals(42L, usage.get("b1").longValue());
    }

    @Test
    public void testGetAllBucketsUsageMetricsFloatValueParsed() throws Exception {
        doReturn("http://metrics.local:9327").when(driver).getMetricsUrl(TEST_STORE_ID);

        List<BucketVO> buckets = new ArrayList<>();
        buckets.add(new BucketVO(TEST_ACCOUNT_ID, TEST_DOMAIN_ID, TEST_STORE_ID, "b1", null, false, false, false, null));
        when(bucketDao.listByObjectStoreId(TEST_STORE_ID)).thenReturn(buckets);

        // Prometheus gauges are floating point and may use scientific notation
        String metricsBody = "SeaweedFS_s3_bucket_size_bytes{bucket=\"b1\"} 1.2345678e+07\n";
        HttpClient mockHttpClient = mock(HttpClient.class);
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn(metricsBody);
        when(mockHttpClient.send(ArgumentMatchers.<HttpRequest>any(),
                ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
                .thenReturn(mockResponse);
        doReturn(mockHttpClient).when(driver).getS3ExtensionHttpClient();

        Map<String, Long> usage = driver.getAllBucketsUsage(TEST_STORE_ID);
        assertEquals(12345678L, usage.get("b1").longValue());
        verify(s3Client, never()).listObjectsV2(any(ListObjectsV2Request.class));
    }

    @Test
    public void testGetAllBucketsUsageUnparseableMetricFallsBackToList() throws Exception {
        doReturn("http://metrics.local:9327").when(driver).getMetricsUrl(TEST_STORE_ID);
        doReturn(s3Client).when(driver).getS3ClientByStoreId(TEST_STORE_ID);

        List<BucketVO> buckets = new ArrayList<>();
        buckets.add(new BucketVO(TEST_ACCOUNT_ID, TEST_DOMAIN_ID, TEST_STORE_ID, "b1", null, false, false, false, null));
        when(bucketDao.listByObjectStoreId(TEST_STORE_ID)).thenReturn(buckets);

        // An unparseable sample value must be treated as a scrape failure so
        // the S3 fallback runs, rather than reporting the bucket as zero.
        String metricsBody = "SeaweedFS_s3_bucket_size_bytes{bucket=\"b1\"} not-a-number\n";
        HttpClient mockHttpClient = mock(HttpClient.class);
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn(metricsBody);
        when(mockHttpClient.send(ArgumentMatchers.<HttpRequest>any(),
                ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
                .thenReturn(mockResponse);
        doReturn(mockHttpClient).when(driver).getS3ExtensionHttpClient();

        ListObjectsV2Result b1Result = mock(ListObjectsV2Result.class);
        S3ObjectSummary s1 = new S3ObjectSummary(); s1.setSize(77L);
        List<S3ObjectSummary> summaries = new ArrayList<>(); summaries.add(s1);
        when(b1Result.getObjectSummaries()).thenReturn(summaries);
        when(b1Result.isTruncated()).thenReturn(false);
        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class))).thenReturn(b1Result);

        Map<String, Long> usage = driver.getAllBucketsUsage(TEST_STORE_ID);
        assertEquals(77L, usage.get("b1").longValue());
    }

    @Test
    public void testGetAllBucketsUsageWrongMetricsEndpointFallsBackToList() throws Exception {
        doReturn("http://prometheus.local:9090").when(driver).getMetricsUrl(TEST_STORE_ID);
        doReturn(s3Client).when(driver).getS3ClientByStoreId(TEST_STORE_ID);

        List<BucketVO> buckets = new ArrayList<>();
        buckets.add(new BucketVO(TEST_ACCOUNT_ID, TEST_DOMAIN_ID, TEST_STORE_ID, "b1", null, false, false, false, null));
        when(bucketDao.listByObjectStoreId(TEST_STORE_ID)).thenReturn(buckets);

        // metricsUrl pointing at a Prometheus server returns HTTP 200 with its
        // own internal metrics, not the SeaweedFS bucket series. This must be
        // detected so the S3 fallback runs instead of reporting zero.
        String metricsBody = "prometheus_build_info{version=\"2.0\"} 1\n" +
                "go_goroutines 42\n";
        HttpClient mockHttpClient = mock(HttpClient.class);
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn(metricsBody);
        when(mockHttpClient.send(ArgumentMatchers.<HttpRequest>any(),
                ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
                .thenReturn(mockResponse);
        doReturn(mockHttpClient).when(driver).getS3ExtensionHttpClient();

        ListObjectsV2Result b1Result = mock(ListObjectsV2Result.class);
        S3ObjectSummary s1 = new S3ObjectSummary(); s1.setSize(88L);
        List<S3ObjectSummary> summaries = new ArrayList<>(); summaries.add(s1);
        when(b1Result.getObjectSummaries()).thenReturn(summaries);
        when(b1Result.isTruncated()).thenReturn(false);
        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class))).thenReturn(b1Result);

        Map<String, Long> usage = driver.getAllBucketsUsage(TEST_STORE_ID);
        assertEquals(88L, usage.get("b1").longValue());
    }

    @Test
    public void testGetAllBucketsUsageMetadataOnlyFallsBackToList() throws Exception {
        doReturn("http://metrics.local:9327").when(driver).getMetricsUrl(TEST_STORE_ID);
        doReturn(s3Client).when(driver).getS3ClientByStoreId(TEST_STORE_ID);

        List<BucketVO> buckets = new ArrayList<>();
        buckets.add(new BucketVO(TEST_ACCOUNT_ID, TEST_DOMAIN_ID, TEST_STORE_ID, "b1", null, false, false, false, null));
        when(bucketDao.listByObjectStoreId(TEST_STORE_ID)).thenReturn(buckets);

        // A non-leader SeaweedFS S3 instance declares the metric family in its
        // HELP/TYPE metadata but exports no samples. Matching the metadata
        // alone would report every bucket as zero, so the parser must require
        // an actual sample line and fall back to the S3 listing.
        String metricsBody = "# HELP SeaweedFS_s3_bucket_size_bytes Current size of each S3 bucket in bytes.\n" +
                "# TYPE SeaweedFS_s3_bucket_size_bytes gauge\n";
        HttpClient mockHttpClient = mock(HttpClient.class);
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn(metricsBody);
        when(mockHttpClient.send(ArgumentMatchers.<HttpRequest>any(),
                ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
                .thenReturn(mockResponse);
        doReturn(mockHttpClient).when(driver).getS3ExtensionHttpClient();

        ListObjectsV2Result b1Result = mock(ListObjectsV2Result.class);
        S3ObjectSummary s1 = new S3ObjectSummary(); s1.setSize(99L);
        List<S3ObjectSummary> summaries = new ArrayList<>(); summaries.add(s1);
        when(b1Result.getObjectSummaries()).thenReturn(summaries);
        when(b1Result.isTruncated()).thenReturn(false);
        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class))).thenReturn(b1Result);

        Map<String, Long> usage = driver.getAllBucketsUsage(TEST_STORE_ID);
        assertEquals(99L, usage.get("b1").longValue());
    }

    @Test
    public void testGetAllBucketsUsageMissingSampleFallsBackToList() throws Exception {
        doReturn("http://metrics.local:9327").when(driver).getMetricsUrl(TEST_STORE_ID);
        doReturn(s3Client).when(driver).getS3ClientByStoreId(TEST_STORE_ID);

        List<BucketVO> buckets = new ArrayList<>();
        buckets.add(new BucketVO(TEST_ACCOUNT_ID, TEST_DOMAIN_ID, TEST_STORE_ID, "b1", null, false, false, false, null));
        buckets.add(new BucketVO(TEST_ACCOUNT_ID, TEST_DOMAIN_ID, TEST_STORE_ID, "b2", null, false, false, false, null));
        when(bucketDao.listByObjectStoreId(TEST_STORE_ID)).thenReturn(buckets);

        // Only b1 has a sample. Reporting b2 as zero would under-report store
        // usage, so a missing sample must trigger the S3 fallback.
        String metricsBody = "SeaweedFS_s3_bucket_size_bytes{bucket=\"b1\"} 100\n";
        HttpClient mockHttpClient = mock(HttpClient.class);
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn(metricsBody);
        when(mockHttpClient.send(ArgumentMatchers.<HttpRequest>any(),
                ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
                .thenReturn(mockResponse);
        doReturn(mockHttpClient).when(driver).getS3ExtensionHttpClient();

        ListObjectsV2Result result = mock(ListObjectsV2Result.class);
        S3ObjectSummary s1 = new S3ObjectSummary(); s1.setSize(11L);
        List<S3ObjectSummary> summaries = new ArrayList<>(); summaries.add(s1);
        when(result.getObjectSummaries()).thenReturn(summaries);
        when(result.isTruncated()).thenReturn(false);
        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class))).thenReturn(result);

        Map<String, Long> usage = driver.getAllBucketsUsage(TEST_STORE_ID);
        assertEquals(2, usage.size());
        assertEquals(11L, usage.get("b1").longValue());
        assertEquals(11L, usage.get("b2").longValue());
    }

    @Test
    public void testGetAllBucketsUsageMetricsUrlTrailingSlashNormalized() throws Exception {
        doReturn("http://metrics.local:9327/").when(driver).getMetricsUrl(TEST_STORE_ID);

        List<BucketVO> buckets = new ArrayList<>();
        buckets.add(new BucketVO(TEST_ACCOUNT_ID, TEST_DOMAIN_ID, TEST_STORE_ID, "b1", null, false, false, false, null));
        when(bucketDao.listByObjectStoreId(TEST_STORE_ID)).thenReturn(buckets);

        String metricsBody = "SeaweedFS_s3_bucket_size_bytes{bucket=\"b1\"} 555\n";
        HttpClient mockHttpClient = mock(HttpClient.class);
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn(metricsBody);
        when(mockHttpClient.send(ArgumentMatchers.<HttpRequest>any(),
                ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
                .thenReturn(mockResponse);
        doReturn(mockHttpClient).when(driver).getS3ExtensionHttpClient();

        Map<String, Long> usage = driver.getAllBucketsUsage(TEST_STORE_ID);
        assertEquals(555L, usage.get("b1").longValue());

        // A trailing slash must not produce '//metrics', which can redirect
        // or 404 and silently force the O(total objects) S3 scan.
        ArgumentCaptor<HttpRequest> reqCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(mockHttpClient, times(1)).send(reqCaptor.capture(),
                ArgumentMatchers.<HttpResponse.BodyHandler<String>>any());
        assertEquals("/metrics", reqCaptor.getValue().uri().getPath());
    }
}

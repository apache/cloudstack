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
package org.apache.cloudstack.storage.datastore.driver;

import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.model.AccessKey;
import com.amazonaws.services.identitymanagement.model.AccessKeyMetadata;
import com.amazonaws.services.identitymanagement.model.CreateAccessKeyRequest;
import com.amazonaws.services.identitymanagement.model.CreateAccessKeyResult;
import com.amazonaws.services.identitymanagement.model.CreateUserRequest;
import com.amazonaws.services.identitymanagement.model.DeleteAccessKeyRequest;
import com.amazonaws.services.identitymanagement.model.DeleteUserPolicyRequest;
import com.amazonaws.services.identitymanagement.model.DeleteUserRequest;
import com.amazonaws.services.identitymanagement.model.EntityAlreadyExistsException;
import com.amazonaws.services.identitymanagement.model.ListAccessKeysRequest;
import com.amazonaws.services.identitymanagement.model.ListAccessKeysResult;
import com.amazonaws.services.identitymanagement.model.ListUserPoliciesRequest;
import com.amazonaws.services.identitymanagement.model.ListUserPoliciesResult;
import com.amazonaws.services.identitymanagement.model.NoSuchEntityException;
import com.amazonaws.services.identitymanagement.model.PutUserPolicyRequest;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.SetBucketPolicyRequest;
import com.cloud.agent.api.to.BucketCredentialTO;
import com.cloud.agent.api.to.BucketKeyTO;
import com.cloud.agent.api.to.BucketTO;
import com.cloud.storage.BucketVO;
import com.cloud.storage.dao.BucketDao;
import com.cloud.user.AccountDetailVO;
import com.cloud.user.AccountDetailsDao;
import com.cloud.user.AccountVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.storage.object.ObjectStore;
import org.apache.cloudstack.storage.datastore.db.ObjectStoreDao;
import org.apache.cloudstack.storage.datastore.db.ObjectStoreDetailsDao;
import org.apache.cloudstack.storage.datastore.db.ObjectStoreVO;
import org.apache.cloudstack.storage.datastore.util.RgwAccountClient;
import org.apache.cloudstack.storage.object.Bucket;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.twonote.rgwadmin4j.RgwAdmin;
import org.twonote.rgwadmin4j.impl.RgwAdminException;
import org.twonote.rgwadmin4j.model.S3Credential;
import org.twonote.rgwadmin4j.model.User;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CephObjectStoreDriverImplTest {

    private static final long STORE_ID = 1L;
    private static final long ACCOUNT_ID = 7L;
    private static final String ACCOUNT_UUID = "7c0e3d2a-account-uuid";
    private static final String BUCKET_UUID = "b0b0b0b0-bucket-uuid";
    private static final String RGW_ACCOUNT_ID = "RGW12345678901234567";

    @Spy
    CephObjectStoreDriverImpl cephObjectStoreDriverImpl = new CephObjectStoreDriverImpl();

    @Mock
    AmazonS3 rgwClient;
    @Mock
    RgwAdmin rgwAdmin;
    @Mock
    RgwAccountClient rgwAccountClient;
    @Mock
    AmazonIdentityManagement iam;
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
    AccountVO account;
    @Mock
    AccountDetailsDao accountDetailsDao;

    Bucket bucket;
    BucketTO bucketTO;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        cephObjectStoreDriverImpl._storeDao = objectStoreDao;
        cephObjectStoreDriverImpl._storeDetailsDao = objectStoreDetailsDao;
        cephObjectStoreDriverImpl._accountDao = accountDao;
        cephObjectStoreDriverImpl._bucketDao = bucketDao;
        cephObjectStoreDriverImpl._accountDetailsDao = accountDetailsDao;
        bucket = new BucketVO();
        bucket.setName("test-bucket");
        BucketVO bucketVO = new BucketVO();
        bucketVO.setName("test-bucket");
        bucketVO.setUuid(BUCKET_UUID);
        bucketTO = new BucketTO(bucketVO);
        bucketTO.setProviderCredentialId(BUCKET_UUID);
        when(objectStoreVO.getUrl()).thenReturn("http://localhost:8000");
        when(objectStoreDao.findById(any())).thenReturn(objectStoreVO);
        when(account.getUuid()).thenReturn(ACCOUNT_UUID);
        when(account.getAccountName()).thenReturn("tenant");
        when(accountDao.findById(anyLong())).thenReturn(account);
    }

    private Map<String, String> legacyDetails() {
        Map<String, String> details = new HashMap<>();
        details.put("ceph-rgw-accesskey", "abc");
        details.put("ceph-rgw-secretkey", "def");
        return details;
    }

    private static String key(String name) {
        return CephObjectStoreDriverImpl.detailKey(STORE_ID, name);
    }

    private Map<String, String> accountModeDetails() {
        Map<String, String> details = new HashMap<>();
        details.put(key(CephObjectStoreDriverImpl.CEPH_ACCOUNT_ID), RGW_ACCOUNT_ID);
        details.put(key(CephObjectStoreDriverImpl.CEPH_ROOT_ACCESS_KEY), "root-ak");
        details.put(key(CephObjectStoreDriverImpl.CEPH_ROOT_SECRET_KEY), "root-sk");
        return details;
    }

    private User userWithKey(String accessKey, String secretKey) {
        // the library's model classes are populated by Gson and expose getters only
        S3Credential credential = mock(S3Credential.class);
        when(credential.getAccessKey()).thenReturn(accessKey);
        when(credential.getSecretKey()).thenReturn(secretKey);
        User user = mock(User.class);
        when(user.getS3Credentials()).thenReturn(Collections.singletonList(credential));
        return user;
    }

    @Test
    public void testCreateBucket() throws Exception {
        doReturn(rgwClient).when(cephObjectStoreDriverImpl).getS3Client(anyLong(), anyLong());
        when(accountDetailsDao.findDetails(anyLong())).thenReturn(legacyDetails());
        when(bucketDao.findById(anyLong())).thenReturn(new BucketVO(bucket.getName()));
        Bucket bucketRet = cephObjectStoreDriverImpl.createBucket(bucket, false);
        assertEquals(bucketRet.getName(), bucket.getName());
        assertEquals("abc", bucketRet.getAccessKey());
        verify(rgwClient, times(1)).doesBucketExistV2(anyString());
        verify(rgwClient, times(1)).createBucket(anyString());
    }

    @Test
    public void testDeleteBucket() throws Exception {
        String bucketName = "test-bucket";
        BucketTO bucket = new BucketTO(bucketName);
        doReturn(rgwAdmin).when(cephObjectStoreDriverImpl).getRgwAdminClient(anyLong());
        boolean success = cephObjectStoreDriverImpl.deleteBucket(bucket, 1L);
        assertTrue(success);
        verify(rgwAdmin, times(1)).removeBucket(anyString());
    }

    @Test
    public void testDeleteBucketTreatsMissingBucketAsRemoved() {
        BucketTO bucket = new BucketTO("test-bucket");
        doReturn(rgwAdmin).when(cephObjectStoreDriverImpl).getRgwAdminClient(anyLong());
        doThrow(new RgwAdminException(404, "NoSuchBucket")).when(rgwAdmin).removeBucket(anyString());
        assertTrue(cephObjectStoreDriverImpl.deleteBucket(bucket, 1L));
    }

    @Test
    public void testDeleteBucketPropagatesOtherErrors() {
        BucketTO bucket = new BucketTO("test-bucket");
        doReturn(rgwAdmin).when(cephObjectStoreDriverImpl).getRgwAdminClient(anyLong());
        doThrow(new RgwAdminException(403, "AccessDenied")).when(rgwAdmin).removeBucket(anyString());
        try {
            cephObjectStoreDriverImpl.deleteBucket(bucket, 1L);
            fail("expected a non-404 error to propagate");
        } catch (CloudRuntimeException expected) {
            // propagated
        }
    }

    @Test
    public void testCreateUserKeepsAccountModeAccount() {
        when(accountDetailsDao.findDetails(ACCOUNT_ID)).thenReturn(accountModeDetails());
        assertTrue(cephObjectStoreDriverImpl.createUser(ACCOUNT_ID, STORE_ID));
        verify(cephObjectStoreDriverImpl, never()).getRgwAdminClient(anyLong());
    }

    @Test
    public void testCreateUserKeepsLegacyAccountLegacy() {
        when(accountDetailsDao.findDetails(ACCOUNT_ID)).thenReturn(legacyDetails());
        doReturn(rgwAdmin).when(cephObjectStoreDriverImpl).getRgwAdminClient(anyLong());
        User legacyUser = userWithKey("abc", "def");
        when(rgwAdmin.getUserInfo(ACCOUNT_UUID)).thenReturn(Optional.of(legacyUser));
        assertTrue(cephObjectStoreDriverImpl.createUser(ACCOUNT_ID, STORE_ID));
        verify(rgwAdmin, never()).modifyUser(anyString(), any());
        verify(cephObjectStoreDriverImpl, never()).migrateAccountForBucketCredentials(anyLong(), anyLong());
    }

    @Test
    public void testCreateUserNewAccountOnUnsupportedStoreCreatesLegacyUser() {
        when(accountDetailsDao.findDetails(ACCOUNT_ID)).thenReturn(new HashMap<>());
        doReturn(false).when(cephObjectStoreDriverImpl).supportsBucketCredentials(STORE_ID);
        doReturn(rgwAdmin).when(cephObjectStoreDriverImpl).getRgwAdminClient(anyLong());
        User createdUser = userWithKey("abc", "def");
        when(rgwAdmin.getUserInfo(ACCOUNT_UUID)).thenReturn(Optional.empty()).thenReturn(Optional.of(createdUser));
        assertTrue(cephObjectStoreDriverImpl.createUser(ACCOUNT_ID, STORE_ID));
        verify(rgwAdmin, times(1)).createUser(ACCOUNT_UUID);
        ArgumentCaptor<Map<String, String>> captor = ArgumentCaptor.forClass(Map.class);
        verify(accountDetailsDao).persist(eq(ACCOUNT_ID), captor.capture());
        assertEquals("abc", captor.getValue().get("ceph-rgw-accesskey"));
        assertFalse(captor.getValue().containsKey(key(CephObjectStoreDriverImpl.CEPH_ACCOUNT_ID)));
    }

    @Test
    public void testCreateUserNewAccountOnSupportedStoreCreatesAccountRoot() {
        when(accountDetailsDao.findDetails(ACCOUNT_ID)).thenReturn(new HashMap<>());
        doReturn(true).when(cephObjectStoreDriverImpl).supportsBucketCredentials(STORE_ID);
        doReturn(rgwAdmin).when(cephObjectStoreDriverImpl).getRgwAdminClient(anyLong());
        doReturn(rgwAccountClient).when(cephObjectStoreDriverImpl).getRgwAccountClient(anyLong());
        when(rgwAccountClient.createAccount(anyString(), eq(ACCOUNT_UUID))).thenReturn(new RgwAccountClient.RgwAccount(RGW_ACCOUNT_ID, ACCOUNT_UUID));
        when(rgwAdmin.getUserInfo(ACCOUNT_UUID)).thenReturn(Optional.empty());
        User rootUser = userWithKey("root-ak", "root-sk");
        when(rgwAdmin.createUser(eq(ACCOUNT_UUID), any())).thenReturn(rootUser);

        assertTrue(cephObjectStoreDriverImpl.createUser(ACCOUNT_ID, STORE_ID));

        ArgumentCaptor<Map<String, String>> userParams = ArgumentCaptor.forClass(Map.class);
        verify(rgwAdmin).createUser(eq(ACCOUNT_UUID), userParams.capture());
        assertEquals(RGW_ACCOUNT_ID, userParams.getValue().get("account-id"));
        assertEquals("true", userParams.getValue().get("account-root"));
        verify(rgwAdmin, never()).modifyUser(anyString(), any());
        ArgumentCaptor<Map<String, String>> details = ArgumentCaptor.forClass(Map.class);
        verify(accountDetailsDao).update(eq(ACCOUNT_ID), details.capture());
        assertEquals(RGW_ACCOUNT_ID, details.getValue().get(key(CephObjectStoreDriverImpl.CEPH_ACCOUNT_ID)));
        assertEquals("root-ak", details.getValue().get(key(CephObjectStoreDriverImpl.CEPH_ROOT_ACCESS_KEY)));
        assertNotNull(details.getValue().get(key(CephObjectStoreDriverImpl.CEPH_ROOT_SECRET_KEY)));
    }

    @Test
    public void testMigrateAccountAdoptsExistingLegacyUser() {
        when(accountDetailsDao.findDetails(ACCOUNT_ID)).thenReturn(legacyDetails());
        doReturn(true).when(cephObjectStoreDriverImpl).supportsBucketCredentials(STORE_ID);
        doReturn(rgwAdmin).when(cephObjectStoreDriverImpl).getRgwAdminClient(anyLong());
        doReturn(rgwAccountClient).when(cephObjectStoreDriverImpl).getRgwAccountClient(anyLong());
        when(rgwAccountClient.createAccount(eq(RgwAccountClient.accountIdFor(ACCOUNT_UUID)), eq(ACCOUNT_UUID))).thenReturn(new RgwAccountClient.RgwAccount(RGW_ACCOUNT_ID, ACCOUNT_UUID));
        User legacyUser = userWithKey("abc", "def");
        User adoptedUser = userWithKey("abc", "def");
        when(rgwAdmin.getUserInfo(ACCOUNT_UUID)).thenReturn(Optional.of(legacyUser));
        when(rgwAdmin.modifyUser(eq(ACCOUNT_UUID), any())).thenReturn(adoptedUser);

        assertTrue(cephObjectStoreDriverImpl.migrateAccountForBucketCredentials(ACCOUNT_ID, STORE_ID));

        verify(rgwAccountClient).createAccount(eq(RgwAccountClient.accountIdFor(ACCOUNT_UUID)), eq(ACCOUNT_UUID));
        ArgumentCaptor<Map<String, String>> userParams = ArgumentCaptor.forClass(Map.class);
        verify(rgwAdmin).modifyUser(eq(ACCOUNT_UUID), userParams.capture());
        assertEquals(RGW_ACCOUNT_ID, userParams.getValue().get("account-id"));
        assertEquals("true", userParams.getValue().get("account-root"));
        verify(rgwAdmin, never()).createUser(anyString(), any());
        ArgumentCaptor<Map<String, String>> details = ArgumentCaptor.forClass(Map.class);
        verify(accountDetailsDao).update(eq(ACCOUNT_ID), details.capture());
        assertEquals(RGW_ACCOUNT_ID, details.getValue().get(key(CephObjectStoreDriverImpl.CEPH_ACCOUNT_ID)));
        assertEquals("abc", details.getValue().get(key(CephObjectStoreDriverImpl.CEPH_ROOT_ACCESS_KEY)));
    }

    @Test
    public void testMigrateAccountIsIdempotent() {
        when(accountDetailsDao.findDetails(ACCOUNT_ID)).thenReturn(accountModeDetails());
        assertTrue(cephObjectStoreDriverImpl.migrateAccountForBucketCredentials(ACCOUNT_ID, STORE_ID));
        verify(cephObjectStoreDriverImpl, never()).getRgwAccountClient(anyLong());
        verify(accountDetailsDao, never()).update(anyLong(), any());
    }

    @Test
    public void testMigrateAccountRefusesUnsupportedStore() {
        when(accountDetailsDao.findDetails(ACCOUNT_ID)).thenReturn(legacyDetails());
        doReturn(false).when(cephObjectStoreDriverImpl).supportsBucketCredentials(STORE_ID);
        try {
            cephObjectStoreDriverImpl.migrateAccountForBucketCredentials(ACCOUNT_ID, STORE_ID);
            fail("expected failure on a store without account support");
        } catch (CloudRuntimeException expected) {
            assertTrue(expected.getMessage().contains("Squid"));
        }
        verify(accountDetailsDao, never()).update(anyLong(), any());
    }

    @Test
    public void testAccountSupportsBucketCredentialsRequiresGatewayAgreement() {
        when(accountDetailsDao.findDetail(ACCOUNT_ID, key(CephObjectStoreDriverImpl.CEPH_ACCOUNT_ID))).thenReturn(new AccountDetailVO(ACCOUNT_ID, key(CephObjectStoreDriverImpl.CEPH_ACCOUNT_ID), RGW_ACCOUNT_ID));
        stubRootUserOnGateway(false);
        assertFalse("stale rows without a root user on the gateway are not migrated", cephObjectStoreDriverImpl.accountSupportsBucketCredentials(ACCOUNT_ID, STORE_ID));
    }

    @Test
    public void testAccountSupportsBucketCredentialsRequiresMarker() {
        stubRootUserOnGateway(true);
        when(accountDetailsDao.findDetail(ACCOUNT_ID, key(CephObjectStoreDriverImpl.CEPH_ACCOUNT_ID))).thenReturn(null);
        assertFalse(cephObjectStoreDriverImpl.accountSupportsBucketCredentials(ACCOUNT_ID, STORE_ID));
        when(accountDetailsDao.findDetail(ACCOUNT_ID, key(CephObjectStoreDriverImpl.CEPH_ACCOUNT_ID))).thenReturn(new AccountDetailVO(ACCOUNT_ID, key(CephObjectStoreDriverImpl.CEPH_ACCOUNT_ID), RGW_ACCOUNT_ID));
        assertTrue(cephObjectStoreDriverImpl.accountSupportsBucketCredentials(ACCOUNT_ID, STORE_ID));
    }

    @Test
    public void testMigratedAccountSurvivesAnUnreachableGateway() {
        doReturn(rgwAccountClient).when(cephObjectStoreDriverImpl).getRgwAccountClient(anyLong());
        when(rgwAccountClient.isAccountRootUser(ACCOUNT_UUID)).thenThrow(new CloudRuntimeException("gateway down"));
        when(accountDetailsDao.findDetail(ACCOUNT_ID, key(CephObjectStoreDriverImpl.CEPH_ACCOUNT_ID)))
                .thenReturn(new AccountDetailVO(ACCOUNT_ID, key(CephObjectStoreDriverImpl.CEPH_ACCOUNT_ID), RGW_ACCOUNT_ID));
        // a momentary failure must not downgrade the account into sharing its account key again
        assertTrue(cephObjectStoreDriverImpl.accountSupportsBucketCredentials(ACCOUNT_ID, STORE_ID));
    }

    @Test
    public void testMigratedAccountDoesNotNeedTheSupportProbe() {
        stubRootUserOnGateway(true);
        when(accountDetailsDao.findDetail(ACCOUNT_ID, key(CephObjectStoreDriverImpl.CEPH_ACCOUNT_ID)))
                .thenReturn(new AccountDetailVO(ACCOUNT_ID, key(CephObjectStoreDriverImpl.CEPH_ACCOUNT_ID), RGW_ACCOUNT_ID));
        assertTrue(cephObjectStoreDriverImpl.accountSupportsBucketCredentials(ACCOUNT_ID, STORE_ID));
        verify(cephObjectStoreDriverImpl, never()).supportsBucketCredentials(anyLong());
    }

    @Test
    public void testUnsupportedReasonIsPassedThroughForTheAdministrator() {
        doReturn(rgwAccountClient).when(cephObjectStoreDriverImpl).getRgwAccountClient(anyLong());
        when(rgwAccountClient.unsupportedReason()).thenReturn("the object store's admin credential is missing the 'accounts' capability");
        assertEquals("the object store's admin credential is missing the 'accounts' capability",
                cephObjectStoreDriverImpl.bucketCredentialsUnsupportedReason(STORE_ID));
    }

    @Test
    public void testUnsupportedReasonSurvivesAnUnreachableGateway() {
        doReturn(rgwAccountClient).when(cephObjectStoreDriverImpl).getRgwAccountClient(anyLong());
        when(rgwAccountClient.unsupportedReason()).thenThrow(new CloudRuntimeException("connection refused"));
        assertTrue(cephObjectStoreDriverImpl.bucketCredentialsUnsupportedReason(STORE_ID).contains("could not be reached"));
    }

    @Test
    public void testSupportsBucketCredentialsProbesAndCaches() {
        doReturn(rgwAccountClient).when(cephObjectStoreDriverImpl).getRgwAccountClient(anyLong());
        when(rgwAccountClient.isAvailable()).thenReturn(true);
        when(rgwAccountClient.hasAccountsWriteCapability()).thenReturn(true);
        assertTrue(cephObjectStoreDriverImpl.supportsBucketCredentials(STORE_ID));
        assertTrue(cephObjectStoreDriverImpl.supportsBucketCredentials(STORE_ID));
        verify(rgwAccountClient, times(1)).isAvailable();
    }

    @Test
    public void testSupportsBucketCredentialsRequiresAccountsCapability() {
        doReturn(rgwAccountClient).when(cephObjectStoreDriverImpl).getRgwAccountClient(anyLong());
        when(rgwAccountClient.isAvailable()).thenReturn(true);
        when(rgwAccountClient.hasAccountsWriteCapability()).thenReturn(false);
        assertFalse(cephObjectStoreDriverImpl.supportsBucketCredentials(STORE_ID));
    }

    @Test
    public void testAccountIdDerivationIsStableAndWellFormed() {
        String id = RgwAccountClient.accountIdFor(ACCOUNT_UUID);
        assertEquals(id, RgwAccountClient.accountIdFor(ACCOUNT_UUID));
        assertTrue(id, id.matches("RGW[0-9]{17}"));
        assertFalse(id.equals(RgwAccountClient.accountIdFor("another-uuid")));
    }

    @Test
    public void testSupportsBucketCredentialsFalseWhenProbeFails() {
        doReturn(rgwAccountClient).when(cephObjectStoreDriverImpl).getRgwAccountClient(anyLong());
        when(rgwAccountClient.isAvailable()).thenThrow(new CloudRuntimeException("connection refused"));
        assertFalse(cephObjectStoreDriverImpl.supportsBucketCredentials(STORE_ID));
    }

    private void stubBucketPolicyWrite() {
        doReturn(rgwClient).when(cephObjectStoreDriverImpl).getS3Client(anyLong(), anyLong());
        when(accountDetailsDao.findDetails(anyLong())).thenReturn(accountModeDetails());
    }

    @Test
    public void testCreateBucketCredentialCreatesIamUserPolicyKeyAndBucketGrant() {
        doReturn(iam).when(cephObjectStoreDriverImpl).getIamClient(anyLong(), anyLong());
        stubBucketPolicyWrite();
        BucketVO publicBucket = new BucketVO("test-bucket");
        publicBucket.setPolicy("public");
        when(bucketDao.findByUuid(BUCKET_UUID)).thenReturn(publicBucket);
        when(iam.createAccessKey(any(CreateAccessKeyRequest.class))).thenReturn(new CreateAccessKeyResult().withAccessKey(new AccessKey().withAccessKeyId("AK1").withSecretAccessKey("SK1")));

        BucketCredentialTO credential = cephObjectStoreDriverImpl.createBucketCredential(bucketTO, STORE_ID);

        assertEquals(BUCKET_UUID, credential.getProviderCredentialId());
        assertEquals(1, credential.getKeys().size());
        assertEquals("AK1", credential.getKeys().get(0).getAccessKey());
        assertEquals("SK1", credential.getKeys().get(0).getSecretKey());
        verify(iam).createUser(new CreateUserRequest(BUCKET_UUID));
        ArgumentCaptor<PutUserPolicyRequest> policy = ArgumentCaptor.forClass(PutUserPolicyRequest.class);
        verify(iam).putUserPolicy(policy.capture());
        assertEquals(BUCKET_UUID, policy.getValue().getUserName());
        assertTrue(policy.getValue().getPolicyDocument().contains("arn:aws:s3:::test-bucket/*"));
        verify(iam, never()).listAccessKeys(any(ListAccessKeysRequest.class));
        ArgumentCaptor<SetBucketPolicyRequest> bucketPolicy = ArgumentCaptor.forClass(SetBucketPolicyRequest.class);
        verify(rgwClient).setBucketPolicy(bucketPolicy.capture());
        String document = bucketPolicy.getValue().getPolicyText();
        assertTrue("grant for the IAM user", document.contains("arn:aws:iam::" + RGW_ACCOUNT_ID + ":user/" + BUCKET_UUID));
        assertTrue("public statements preserved", document.contains("s3:GetObject") && document.contains("\"Principal\":\"*\""));
    }

    @Test
    public void testSetBucketPolicyPrivateKeepsCredentialGrant() {
        stubBucketPolicyWrite();
        cephObjectStoreDriverImpl.setBucketPolicy(bucketTO, "private", STORE_ID);
        ArgumentCaptor<SetBucketPolicyRequest> bucketPolicy = ArgumentCaptor.forClass(SetBucketPolicyRequest.class);
        verify(rgwClient).setBucketPolicy(bucketPolicy.capture());
        String document = bucketPolicy.getValue().getPolicyText();
        assertTrue(document.contains(":user/" + BUCKET_UUID));
        assertFalse(document.contains("\"Principal\":\"*\""));
    }

    @Test
    public void testSetBucketPolicyWithoutCredentialMatchesLegacyShape() {
        doReturn(rgwClient).when(cephObjectStoreDriverImpl).getS3Client(anyLong(), anyLong());
        cephObjectStoreDriverImpl.setBucketPolicy(new BucketTO("legacy-bucket"), "private", STORE_ID);
        ArgumentCaptor<SetBucketPolicyRequest> bucketPolicy = ArgumentCaptor.forClass(SetBucketPolicyRequest.class);
        verify(rgwClient).setBucketPolicy(bucketPolicy.capture());
        assertEquals("{\"Version\":\"2012-10-17\",\"Statement\":[]}", bucketPolicy.getValue().getPolicyText());
        verify(accountDetailsDao, never()).findDetails(anyLong());
    }

    @Test
    public void testCreateBucketCredentialClearsOrphanKeysOnRetry() {
        doReturn(iam).when(cephObjectStoreDriverImpl).getIamClient(anyLong(), anyLong());
        stubBucketPolicyWrite();
        when(iam.createUser(any(CreateUserRequest.class))).thenThrow(new EntityAlreadyExistsException("exists"));
        when(iam.listAccessKeys(any(ListAccessKeysRequest.class))).thenReturn(new ListAccessKeysResult().withAccessKeyMetadata(new AccessKeyMetadata().withAccessKeyId("ORPHAN")));
        when(iam.createAccessKey(any(CreateAccessKeyRequest.class))).thenReturn(new CreateAccessKeyResult().withAccessKey(new AccessKey().withAccessKeyId("AK2").withSecretAccessKey("SK2")));

        BucketCredentialTO credential = cephObjectStoreDriverImpl.createBucketCredential(bucketTO, STORE_ID);

        assertEquals("AK2", credential.getKeys().get(0).getAccessKey());
        verify(iam).deleteAccessKey(new DeleteAccessKeyRequest(BUCKET_UUID, "ORPHAN"));
        verify(iam).putUserPolicy(any(PutUserPolicyRequest.class));
    }

    @Test
    public void testCreateBucketCredentialKeyReturnsNewPair() {
        doReturn(iam).when(cephObjectStoreDriverImpl).getIamClient(anyLong(), anyLong());
        when(iam.createAccessKey(new CreateAccessKeyRequest(BUCKET_UUID))).thenReturn(new CreateAccessKeyResult().withAccessKey(new AccessKey().withAccessKeyId("AK3").withSecretAccessKey("SK3")));
        BucketKeyTO key = cephObjectStoreDriverImpl.createBucketCredentialKey(bucketTO, STORE_ID, new HashSet<>(Collections.singletonList("AK1")));
        assertEquals("AK3", key.getAccessKey());
        assertEquals("SK3", key.getSecretKey());
    }

    @Test
    public void testRemoveBucketCredentialKeyTreatsMissingAsRemoved() {
        doReturn(iam).when(cephObjectStoreDriverImpl).getIamClient(anyLong(), anyLong());
        when(iam.deleteAccessKey(any(DeleteAccessKeyRequest.class))).thenThrow(new NoSuchEntityException("gone"));
        assertTrue(cephObjectStoreDriverImpl.removeBucketCredentialKey(bucketTO, STORE_ID, "AK1"));
    }

    @Test
    public void testDeleteBucketCredentialRemovesKeysPolicyAndUser() {
        doReturn(iam).when(cephObjectStoreDriverImpl).getIamClient(anyLong(), anyLong());
        when(iam.listAccessKeys(any(ListAccessKeysRequest.class))).thenReturn(new ListAccessKeysResult().withAccessKeyMetadata(new AccessKeyMetadata().withAccessKeyId("AK1"), new AccessKeyMetadata().withAccessKeyId("AK2")));
        when(iam.listUserPolicies(any(ListUserPoliciesRequest.class))).thenReturn(new ListUserPoliciesResult().withPolicyNames(CephObjectStoreDriverImpl.BUCKET_POLICY_NAME));

        assertTrue(cephObjectStoreDriverImpl.deleteBucketCredential(bucketTO, STORE_ID));

        verify(iam).deleteAccessKey(new DeleteAccessKeyRequest(BUCKET_UUID, "AK1"));
        verify(iam).deleteAccessKey(new DeleteAccessKeyRequest(BUCKET_UUID, "AK2"));
        verify(iam).deleteUserPolicy(new DeleteUserPolicyRequest(BUCKET_UUID, CephObjectStoreDriverImpl.BUCKET_POLICY_NAME));
        verify(iam).deleteUser(new DeleteUserRequest(BUCKET_UUID));
    }

    @Test
    public void testDeleteBucketCredentialTreatsMissingUserAsRemoved() {
        doReturn(iam).when(cephObjectStoreDriverImpl).getIamClient(anyLong(), anyLong());
        when(iam.listAccessKeys(any(ListAccessKeysRequest.class))).thenThrow(new NoSuchEntityException("gone"));
        assertTrue(cephObjectStoreDriverImpl.deleteBucketCredential(bucketTO, STORE_ID));
        verify(iam, never()).deleteUser(any(DeleteUserRequest.class));
    }

    private void stubRootUserOnGateway(boolean isRoot) {
        doReturn(rgwAccountClient).when(cephObjectStoreDriverImpl).getRgwAccountClient(anyLong());
        when(rgwAccountClient.isAccountRootUser(ACCOUNT_UUID)).thenReturn(isRoot);
    }

    @Test
    public void testRotateAccountKeyRefusedWhenGatewayUserIsNotAccountRoot() {
        when(accountDetailsDao.findDetails(ACCOUNT_ID)).thenReturn(accountModeDetails());
        stubRootUserOnGateway(false);
        try {
            cephObjectStoreDriverImpl.rotateAccountKey(ACCOUNT_ID, STORE_ID);
            fail("expected refusal when RGW does not know the user as an account root");
        } catch (CloudRuntimeException expected) {
            assertTrue(expected.getMessage().contains("migrate the account on this store first"));
        }
        verify(iam, never()).createAccessKey(any(CreateAccessKeyRequest.class));
    }

    @Test
    public void testRotateAccountKeyCreatesPersistsThenRemovesOld() {
        stubRootUserOnGateway(true);
        Map<String, String> details = accountModeDetails();
        details.put("ceph-rgw-accesskey", "legacy-ak");
        details.put("ceph-rgw-secretkey", "legacy-sk");
        when(accountDetailsDao.findDetails(ACCOUNT_ID)).thenReturn(details);
        doReturn(iam).when(cephObjectStoreDriverImpl).getIamClient(anyLong(), anyLong());
        when(iam.createAccessKey(new CreateAccessKeyRequest())).thenReturn(new CreateAccessKeyResult().withAccessKey(new AccessKey().withAccessKeyId("root-ak-2").withSecretAccessKey("root-sk-2")));
        // the root holds the key we recorded plus one left behind by an interrupted rotation
        when(iam.listAccessKeys(new ListAccessKeysRequest())).thenReturn(new ListAccessKeysResult()
                .withAccessKeyMetadata(new AccessKeyMetadata().withAccessKeyId("root-ak"),
                        new AccessKeyMetadata().withAccessKeyId("untracked-ak"),
                        new AccessKeyMetadata().withAccessKeyId("root-ak-2")));

        BucketKeyTO key = cephObjectStoreDriverImpl.rotateAccountKey(ACCOUNT_ID, STORE_ID);

        assertEquals("root-ak-2", key.getAccessKey());
        assertEquals("root-sk-2", key.getSecretKey());
        org.mockito.InOrder order = org.mockito.Mockito.inOrder(iam, accountDetailsDao);
        order.verify(iam).createAccessKey(new CreateAccessKeyRequest());
        order.verify(accountDetailsDao).update(eq(ACCOUNT_ID), any());
        order.verify(iam).deleteAccessKey(new DeleteAccessKeyRequest().withAccessKeyId("root-ak"));
        // a key CloudStack did not issue is reported, never deleted: it may be someone's own
        verify(iam, never()).deleteAccessKey(new DeleteAccessKeyRequest().withAccessKeyId("untracked-ak"));
        verify(iam, never()).deleteAccessKey(new DeleteAccessKeyRequest().withAccessKeyId("root-ak-2"));
        ArgumentCaptor<Map<String, String>> captor = ArgumentCaptor.forClass(Map.class);
        verify(accountDetailsDao, times(1)).persist(eq(ACCOUNT_ID), captor.capture());
        Map<String, String> remaining = captor.getValue();
        assertFalse("legacy access key row removed", remaining.containsKey("ceph-rgw-accesskey"));
        assertFalse("legacy secret key row removed", remaining.containsKey("ceph-rgw-secretkey"));
    }

    @Test
    public void testRotateAccountKeyCompensatesWhenPersistFails() {
        stubRootUserOnGateway(true);
        when(accountDetailsDao.findDetails(ACCOUNT_ID)).thenReturn(accountModeDetails());
        doReturn(iam).when(cephObjectStoreDriverImpl).getIamClient(anyLong(), anyLong());
        when(iam.createAccessKey(new CreateAccessKeyRequest())).thenReturn(new CreateAccessKeyResult().withAccessKey(new AccessKey().withAccessKeyId("root-ak-2").withSecretAccessKey("root-sk-2")));
        doThrow(new RuntimeException("db down")).when(accountDetailsDao).update(eq(ACCOUNT_ID), any());
        try {
            cephObjectStoreDriverImpl.rotateAccountKey(ACCOUNT_ID, STORE_ID);
            fail("expected the persist failure to propagate");
        } catch (RuntimeException expected) {
            assertEquals("db down", expected.getMessage());
        }
        verify(iam).deleteAccessKey(new DeleteAccessKeyRequest().withAccessKeyId("root-ak-2"));
        verify(iam, never()).deleteAccessKey(new DeleteAccessKeyRequest().withAccessKeyId("root-ak"));
    }

    @Test
    public void testRotateAccountKeyRequiresAccountMode() {
        when(accountDetailsDao.findDetails(ACCOUNT_ID)).thenReturn(legacyDetails());
        try {
            cephObjectStoreDriverImpl.rotateAccountKey(ACCOUNT_ID, STORE_ID);
            fail("expected refusal for a legacy account");
        } catch (CloudRuntimeException expected) {
            assertTrue(expected.getMessage().contains("has not been migrated"));
        }
    }

    @Test
    public void testAdoptedAccountHasKeyRotationPendingUntilRotated() {
        when(accountDetailsDao.findDetails(ACCOUNT_ID)).thenReturn(accountModeDetails());
        assertTrue("adopted account (no marker) is pending", cephObjectStoreDriverImpl.isAccountKeyRotationPending(ACCOUNT_ID, STORE_ID));
        Map<String, String> rotated = accountModeDetails();
        rotated.put(key(CephObjectStoreDriverImpl.CEPH_ROOT_KEY_ROTATED), "true");
        when(accountDetailsDao.findDetails(ACCOUNT_ID)).thenReturn(rotated);
        assertFalse("rotated account is not pending", cephObjectStoreDriverImpl.isAccountKeyRotationPending(ACCOUNT_ID, STORE_ID));
        when(accountDetailsDao.findDetails(ACCOUNT_ID)).thenReturn(legacyDetails());
        assertFalse("legacy account is not pending", cephObjectStoreDriverImpl.isAccountKeyRotationPending(ACCOUNT_ID, STORE_ID));
    }

    @Test
    public void testFreshAccountIsCreatedWithRotatedMarker() {
        when(accountDetailsDao.findDetails(ACCOUNT_ID)).thenReturn(new HashMap<>());
        doReturn(true).when(cephObjectStoreDriverImpl).supportsBucketCredentials(STORE_ID);
        doReturn(rgwAdmin).when(cephObjectStoreDriverImpl).getRgwAdminClient(anyLong());
        doReturn(rgwAccountClient).when(cephObjectStoreDriverImpl).getRgwAccountClient(anyLong());
        when(rgwAccountClient.createAccount(anyString(), eq(ACCOUNT_UUID))).thenReturn(new RgwAccountClient.RgwAccount(RGW_ACCOUNT_ID, ACCOUNT_UUID));
        when(rgwAdmin.getUserInfo(ACCOUNT_UUID)).thenReturn(Optional.empty());
        User rootUser = userWithKey("root-ak", "root-sk");
        when(rgwAdmin.createUser(eq(ACCOUNT_UUID), any())).thenReturn(rootUser);
        cephObjectStoreDriverImpl.migrateAccountForBucketCredentials(ACCOUNT_ID, STORE_ID);
        ArgumentCaptor<Map<String, String>> details = ArgumentCaptor.forClass(Map.class);
        verify(accountDetailsDao).update(eq(ACCOUNT_ID), details.capture());
        assertEquals("true", details.getValue().get(key(CephObjectStoreDriverImpl.CEPH_ROOT_KEY_ROTATED)));
    }

    @Test
    public void testAdoptedAccountIsNotMarkedRotated() {
        when(accountDetailsDao.findDetails(ACCOUNT_ID)).thenReturn(legacyDetails());
        doReturn(true).when(cephObjectStoreDriverImpl).supportsBucketCredentials(STORE_ID);
        doReturn(rgwAdmin).when(cephObjectStoreDriverImpl).getRgwAdminClient(anyLong());
        doReturn(rgwAccountClient).when(cephObjectStoreDriverImpl).getRgwAccountClient(anyLong());
        when(rgwAccountClient.createAccount(anyString(), eq(ACCOUNT_UUID))).thenReturn(new RgwAccountClient.RgwAccount(RGW_ACCOUNT_ID, ACCOUNT_UUID));
        User legacyUser = userWithKey("abc", "def");
        User adoptedUser = userWithKey("abc", "def");
        when(rgwAdmin.getUserInfo(ACCOUNT_UUID)).thenReturn(Optional.of(legacyUser));
        when(rgwAdmin.modifyUser(eq(ACCOUNT_UUID), any())).thenReturn(adoptedUser);
        cephObjectStoreDriverImpl.migrateAccountForBucketCredentials(ACCOUNT_ID, STORE_ID);
        ArgumentCaptor<Map<String, String>> details = ArgumentCaptor.forClass(Map.class);
        verify(accountDetailsDao).update(eq(ACCOUNT_ID), details.capture());
        assertFalse(details.getValue().containsKey(key(CephObjectStoreDriverImpl.CEPH_ROOT_KEY_ROTATED)));
    }

    @Test
    public void testAccountModeIsScopedPerStore() {
        stubRootUserOnGateway(true);
        when(accountDetailsDao.findDetails(ACCOUNT_ID)).thenReturn(accountModeDetails());
        when(accountDetailsDao.findDetail(ACCOUNT_ID, key(CephObjectStoreDriverImpl.CEPH_ACCOUNT_ID))).thenReturn(new AccountDetailVO(ACCOUNT_ID, key(CephObjectStoreDriverImpl.CEPH_ACCOUNT_ID), RGW_ACCOUNT_ID));
        assertTrue("migrated on store 1", cephObjectStoreDriverImpl.accountSupportsBucketCredentials(ACCOUNT_ID, STORE_ID));
        assertFalse("legacy on store 2", cephObjectStoreDriverImpl.accountSupportsBucketCredentials(ACCOUNT_ID, 2L));
        assertFalse("nothing pending on store 2", cephObjectStoreDriverImpl.isAccountKeyRotationPending(ACCOUNT_ID, 2L));
        try {
            cephObjectStoreDriverImpl.rotateAccountKey(ACCOUNT_ID, 2L);
            fail("rotation on a store the account is not migrated on must be refused");
        } catch (CloudRuntimeException expected) {
            assertTrue(expected.getMessage().contains("on this store"));
        }
    }

    @Test
    public void testMigratingOnOneStoreKeepsOtherStoresRows() {
        Map<String, String> existing = new HashMap<>();
        existing.put(CephObjectStoreDriverImpl.detailKey(9L, CephObjectStoreDriverImpl.CEPH_ACCOUNT_ID), "RGW00000000000000009");
        existing.put("unrelated-detail", "keep-me");
        when(accountDetailsDao.findDetails(ACCOUNT_ID)).thenReturn(existing);
        doReturn(true).when(cephObjectStoreDriverImpl).supportsBucketCredentials(STORE_ID);
        doReturn(rgwAdmin).when(cephObjectStoreDriverImpl).getRgwAdminClient(anyLong());
        doReturn(rgwAccountClient).when(cephObjectStoreDriverImpl).getRgwAccountClient(anyLong());
        when(rgwAccountClient.createAccount(anyString(), eq(ACCOUNT_UUID))).thenReturn(new RgwAccountClient.RgwAccount(RGW_ACCOUNT_ID, ACCOUNT_UUID));
        when(rgwAdmin.getUserInfo(ACCOUNT_UUID)).thenReturn(Optional.empty());
        User rootUser = userWithKey("root-ak", "root-sk");
        when(rgwAdmin.createUser(eq(ACCOUNT_UUID), any())).thenReturn(rootUser);

        cephObjectStoreDriverImpl.migrateAccountForBucketCredentials(ACCOUNT_ID, STORE_ID);

        // rows are merged, never replaced: the DAO's update keeps everything not in the map
        verify(accountDetailsDao, never()).persist(anyLong(), any());
        ArgumentCaptor<Map<String, String>> written = ArgumentCaptor.forClass(Map.class);
        verify(accountDetailsDao).update(eq(ACCOUNT_ID), written.capture());
        assertTrue(written.getValue().keySet().stream().allMatch(k -> k.startsWith(ObjectStore.accountDetailPrefix(STORE_ID))));
    }

    @Test
    public void testBucketCredentialOperationsRequireCredentialId() {
        BucketTO withoutCredential = new BucketTO("orphan");
        try {
            cephObjectStoreDriverImpl.createBucketCredentialKey(withoutCredential, STORE_ID, new HashSet<>());
            fail("expected failure without a provider credential id");
        } catch (CloudRuntimeException expected) {
            assertTrue(expected.getMessage().contains("no dedicated credential"));
        }
    }
}

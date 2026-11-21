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

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CreateBucketRequest;
import com.amazonaws.services.s3.model.SetBucketEncryptionRequest;
import com.amazonaws.services.s3.model.SetBucketPolicyRequest;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.utils.exception.CloudRuntimeException;
import org.mockito.ArgumentCaptor;
import org.twonote.rgwadmin4j.model.S3Credential;
import org.twonote.rgwadmin4j.model.User;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import com.cloud.agent.api.to.BucketTO;
import com.cloud.storage.BucketVO;
import com.cloud.storage.dao.BucketDao;
import com.cloud.user.AccountDetailVO;
import com.cloud.user.AccountDetailsDao;
import com.cloud.user.AccountVO;
import com.cloud.user.dao.AccountDao;
import org.apache.cloudstack.storage.datastore.db.ObjectStoreDao;
import org.apache.cloudstack.storage.datastore.db.ObjectStoreDetailsDao;
import org.apache.cloudstack.storage.datastore.db.ObjectStoreVO;
import org.apache.cloudstack.storage.object.Bucket;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.twonote.rgwadmin4j.RgwAdmin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CephObjectStoreDriverImplTest {

    @Spy
    CephObjectStoreDriverImpl cephObjectStoreDriverImpl = new CephObjectStoreDriverImpl();

    @Mock
    AmazonS3 rgwClient;
    @Mock
    RgwAdmin rgwAdmin;
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
        when(objectStoreVO.getUrl()).thenReturn("http://localhost:8000");
        when(objectStoreDao.findById(any())).thenReturn(objectStoreVO);
    }

    @Test
    public void testCreateBucket() throws Exception {
        doReturn(rgwClient).when(cephObjectStoreDriverImpl).getS3Client(anyLong(), anyLong());
        when(accountDetailsDao.findDetail(anyLong(),anyString())).
                thenReturn(new AccountDetailVO(1L, "abc","def"));
        when(bucketDao.findById(anyLong())).thenReturn(new BucketVO(bucket.getName()));
        Bucket bucketRet = cephObjectStoreDriverImpl.createBucket(bucket, false);
        assertEquals(bucketRet.getName(), bucket.getName());
        verify(rgwClient, times(1)).doesBucketExistV2(anyString());
        verify(rgwClient, times(1)).createBucket(any(CreateBucketRequest.class));
    }

    @Test
    public void testDeleteBucket() throws Exception {
        String bucketName = "test-bucket";
        BucketTO bucket = new BucketTO(bucketName);
        doReturn(rgwClient).when(cephObjectStoreDriverImpl).getS3Client(any(), any(), any());
        boolean success = cephObjectStoreDriverImpl.deleteBucket(bucket, 1L);
        assertTrue(success);
        verify(rgwClient, times(1)).deleteBucket(anyString());
    }

    private User mockRgwUser(boolean existing) {
        User user = mock(User.class);
        S3Credential credential = mock(S3Credential.class);
        when(credential.getAccessKey()).thenReturn("access");
        when(credential.getSecretKey()).thenReturn("secret");
        when(user.getS3Credentials()).thenReturn(List.of(credential));
        when(account.getUuid()).thenReturn("uuid");
        when(account.getAccountName()).thenReturn("name");
        when(accountDao.findById(1L)).thenReturn(account);
        doReturn(rgwAdmin).when(cephObjectStoreDriverImpl).getRgwAdminClient(anyLong());
        when(rgwAdmin.getUserInfo("uuid")).thenReturn(existing ? Optional.of(user) : Optional.empty());
        return user;
    }

    @Test
    public void testCreateUserStoresKeysPerStoreWithoutReplacingDetails() {
        User user = mockRgwUser(false);
        when(rgwAdmin.createUser(eq("uuid"), any())).thenReturn(user);
        assertTrue(cephObjectStoreDriverImpl.createUser(1L, 2L));
        verify(accountDetailsDao).update(1L, Map.of("ceph-rgw-accesskey-2", "access", "ceph-rgw-secretkey-2", "secret"));
        verify(accountDetailsDao, never()).persist(anyLong(), any());
    }

    @Test
    public void testCreateUserStoresKeysOfExistingUser() {
        mockRgwUser(true);
        assertTrue(cephObjectStoreDriverImpl.createUser(1L, 2L));
        verify(rgwAdmin, never()).createUser(any(), any());
        verify(accountDetailsDao).update(eq(1L), any());
    }

    @Test(expected = CloudRuntimeException.class)
    public void testCreateUserFailsWhenLookupFails() {
        mockRgwUser(true);
        when(rgwAdmin.getUserInfo("uuid")).thenThrow(new RuntimeException("connection refused"));
        cephObjectStoreDriverImpl.createUser(1L, 2L);
    }

    @Test
    public void testGetAccountKeyFallsBackToKeyWithoutStore() {
        when(accountDetailsDao.findDetail(1L, "ceph-rgw-secretkey")).thenReturn(new AccountDetailVO(1L, "ceph-rgw-secretkey", "old"));
        assertEquals("old", cephObjectStoreDriverImpl.getAccountKey(1L, 2L, "ceph-rgw-secretkey"));
    }

    @Test(expected = CloudRuntimeException.class)
    public void testGetAccountKeyFailsWithoutKey() {
        cephObjectStoreDriverImpl.getAccountKey(1L, 2L, "ceph-rgw-secretkey");
    }

    @Test(expected = CloudRuntimeException.class)
    public void testCreateBucketFailsWhenBucketExists() {
        doReturn(rgwClient).when(cephObjectStoreDriverImpl).getS3Client(anyLong(), anyLong());
        when(rgwClient.doesBucketExistV2(anyString())).thenReturn(true);
        cephObjectStoreDriverImpl.createBucket(bucket, false);
    }

    @Test
    public void testPublicPolicyDoesNotAllowListing() {
        doReturn(rgwClient).when(cephObjectStoreDriverImpl).getS3Client(any(), any(), any());
        cephObjectStoreDriverImpl.setBucketPolicy(new BucketTO("test-bucket"), "public", 1L);
        ArgumentCaptor<SetBucketPolicyRequest> request = ArgumentCaptor.forClass(SetBucketPolicyRequest.class);
        verify(rgwClient).setBucketPolicy(request.capture());
        assertTrue(request.getValue().getPolicyText().contains("s3:GetObject"));
        assertFalse(request.getValue().getPolicyText().contains("s3:ListBucket"));
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testUnknownPolicyIsRefused() {
        cephObjectStoreDriverImpl.setBucketPolicy(new BucketTO("test-bucket"), "everyone", 1L);
    }

    @Test
    public void testSetBucketEncryption() {
        doReturn(rgwClient).when(cephObjectStoreDriverImpl).getS3Client(any(), any(), any());
        assertTrue(cephObjectStoreDriverImpl.setBucketEncryption(new BucketTO("test-bucket"), 1L));
        verify(rgwClient).setBucketEncryption(any(SetBucketEncryptionRequest.class));
    }
}

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
package org.apache.cloudstack.storage.object;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.apache.cloudstack.api.command.admin.storage.MigrateObjectStoreAccountCmd;
import org.apache.cloudstack.api.command.admin.storage.RotateObjectStoreAccountKeyCmd;
import org.apache.cloudstack.api.command.user.bucket.CreateBucketCmd;
import org.apache.cloudstack.api.command.user.bucket.MigrateBucketCredentialCmd;
import org.apache.cloudstack.api.command.user.bucket.RevokeBucketKeyCmd;
import org.apache.cloudstack.api.command.user.bucket.RotateBucketKeyCmd;
import org.apache.cloudstack.api.command.user.bucket.UpdateBucketCmd;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.reservation.ReservationVO;
import org.apache.cloudstack.reservation.dao.ReservationDao;
import org.apache.cloudstack.storage.datastore.db.ObjectStoreDao;
import org.apache.cloudstack.storage.datastore.db.ObjectStoreVO;
import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloud.agent.api.to.BucketCredentialTO;
import com.cloud.agent.api.to.BucketKeyTO;
import com.cloud.agent.api.to.BucketTO;
import com.cloud.configuration.Resource;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.resourcelimit.ResourceLimitManagerImpl;
import com.cloud.storage.BucketCredentialKeyVO;
import com.cloud.storage.BucketCredentialVO;
import com.cloud.storage.BucketVO;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.dao.BucketCredentialDao;
import com.cloud.storage.dao.BucketCredentialKeyDao;
import com.cloud.storage.dao.BucketDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountVO;
import com.cloud.user.User;
import com.cloud.utils.db.DbUtil;

@RunWith(MockitoJUnitRunner.class)
public class BucketApiServiceImplTest {
    @Spy
    @InjectMocks
    BucketApiServiceImpl bucketApiService;

    @Mock
    AccountManager accountManager;

    @Mock
    ObjectStoreDao objectStoreDao;

    @Mock
    DataStoreManager dataStoreMgr;

    @Mock
    private ResourceLimitManagerImpl resourceLimitManager;

    @Mock
    private BucketDao bucketDao;

    @Mock
    ReservationDao reservationDao;

    @Mock
    private AccountVO mockAccountVO;

    @Mock
    private BucketCredentialDao bucketCredentialDao;

    @Mock
    private BucketCredentialKeyDao bucketCredentialKeyDao;

    private MockedStatic<DbUtil> dbUtilMockedStatic;
    private final List<String> mockedGlobalLocks = new ArrayList<>();
    private static final long ACCOUNT_ID = 1001L;
    private static final long DOMAIN_ID = 10L;

    @Before
    public void setup() {
        when(accountManager.getActiveAccountById(ACCOUNT_ID)).thenReturn(mockAccountVO);
        when(mockAccountVO.getDomainId()).thenReturn(DOMAIN_ID);
        when(reservationDao.persist(any(ReservationVO.class)))
                .thenAnswer((Answer<ReservationVO>) invocation -> {
                    ReservationVO reservationVO = (ReservationVO)invocation.getArguments()[0];
                    ReflectionTestUtils.setField(reservationVO, "id", 10L);
                    return reservationVO;
                });
        dbUtilMockedStatic = Mockito.mockStatic(DbUtil.class);
        dbUtilMockedStatic.when(() -> DbUtil.getGlobalLock(anyString(), anyInt()))
                .thenAnswer((Answer<Boolean>) invocation -> {
            String lockName = invocation.getArgument(0);
            if (!StringUtils.isBlank(lockName) && !mockedGlobalLocks.contains(lockName)) {
                mockedGlobalLocks.add(lockName);
                return true;
            }
            return false;
        });
        dbUtilMockedStatic.when(() -> DbUtil.releaseGlobalLock(anyString()))
                .thenAnswer((Answer<Boolean>) invocation -> {
            String lockName = invocation.getArgument(0);
            if (!StringUtils.isBlank(lockName)) {
                mockedGlobalLocks.remove(lockName);
            }
            return true;
        });

        Account account = mock(Account.class);
        User user = mock(User.class);
        CallContext.register(user, account);
    }

    @After
    public void tearDown() throws Exception {
        dbUtilMockedStatic.close();
        CallContext.unregister();
    }

    @Test
    public void testAllocBucket() throws ResourceAllocationException {
        String bucketName = "bucket1";
        Long poolId = 2L;
        Long objectStoreId = 3L;
        int quota = 1;

        CreateBucketCmd cmd = Mockito.mock(CreateBucketCmd.class);
        Mockito.when(cmd.getBucketName()).thenReturn(bucketName);
        Mockito.when(cmd.getEntityOwnerId()).thenReturn(ACCOUNT_ID);
        Mockito.when(cmd.getObjectStoragePoolId()).thenReturn(poolId);
        Mockito.when(cmd.getQuota()).thenReturn(quota);

        ObjectStoreVO objectStoreVO = Mockito.mock(ObjectStoreVO.class);
        Mockito.when(objectStoreVO.getId()).thenReturn(objectStoreId);
        Mockito.when(objectStoreDao.findById(poolId)).thenReturn(objectStoreVO);
        ObjectStoreEntity objectStore = Mockito.mock(ObjectStoreEntity.class);
        Mockito.when(dataStoreMgr.getDataStore(objectStoreId, DataStoreRole.Object)).thenReturn(objectStore);
        Mockito.when(objectStore.createUser(ACCOUNT_ID)).thenReturn(true);

        bucketApiService.allocBucket(cmd);

        long size = quota * Resource.ResourceType.bytesToGiB;
        Mockito.verify(resourceLimitManager, Mockito.times(1))
                .checkResourceLimitWithTag(mockAccountVO, DOMAIN_ID, true,
                        Resource.ResourceType.bucket, null, 1L);
        Mockito.verify(resourceLimitManager, Mockito.times(1))
                .checkResourceLimitWithTag(mockAccountVO, DOMAIN_ID, true,
                        Resource.ResourceType.object_storage, null, size);
        Mockito.verify(resourceLimitManager, Mockito.times(1))
                .incrementResourceCount(ACCOUNT_ID, Resource.ResourceType.bucket);
        Mockito.verify(resourceLimitManager, Mockito.times(1))
                .incrementResourceCount(ACCOUNT_ID, Resource.ResourceType.object_storage, size);
    }

    @Test
    public void testCreateBucket() {
        Long objectStoreId = 1L;
        Long poolId = 2L;
        Long bucketId = 3L;
        String bucketName = "bucket1";
        int quota = 3;

        CreateBucketCmd cmd = Mockito.mock(CreateBucketCmd.class);
        Mockito.when(cmd.getObjectStoragePoolId()).thenReturn(poolId);
        Mockito.when(cmd.getEntityId()).thenReturn(bucketId);
        Mockito.when(cmd.getQuota()).thenReturn(quota);

        BucketVO bucket = new BucketVO(bucketName);
        Mockito.when(bucketDao.findById(bucketId)).thenReturn(bucket);
        ReflectionTestUtils.setField(bucket, "accountId", ACCOUNT_ID);

        ObjectStoreVO objectStoreVO = Mockito.mock(ObjectStoreVO.class);
        Mockito.when(objectStoreVO.getId()).thenReturn(objectStoreId);
        Mockito.when(objectStoreVO.getTotalSize()).thenReturn(10 * Resource.ResourceType.bytesToGiB);
        Mockito.when(objectStoreDao.findById(poolId)).thenReturn(objectStoreVO);
        ObjectStoreEntity objectStore = Mockito.mock(ObjectStoreEntity.class);
        Mockito.when(dataStoreMgr.getDataStore(objectStoreId, DataStoreRole.Object)).thenReturn(objectStore);
        Mockito.when(objectStore.createBucket(bucket, false)).thenReturn(bucket);

        bucketApiService.createBucket(cmd);

        Assert.assertEquals(Bucket.State.Created, bucket.getState());
    }

    @Test
    public void testDeleteBucket() throws ResourceAllocationException {
        Long bucketId = 1L;
        Long objectStoreId = 3L;
        String bucketName = "bucket1";
        int quota = 2;

        BucketVO bucket = mock(BucketVO.class);
        when(bucket.getName()).thenReturn(bucketName);
        when(bucket.getObjectStoreId()).thenReturn(objectStoreId);
        when(bucket.getQuota()).thenReturn(quota);
        when(bucket.getAccountId()).thenReturn(ACCOUNT_ID);
        when(accountManager.getAccount(ACCOUNT_ID)).thenReturn(mock(AccountVO.class));
        Mockito.when(bucketDao.findById(bucketId)).thenReturn(bucket);

        ObjectStoreVO objectStoreVO = Mockito.mock(ObjectStoreVO.class);
        Mockito.when(objectStoreVO.getId()).thenReturn(objectStoreId);
        Mockito.when(objectStoreDao.findById(objectStoreId)).thenReturn(objectStoreVO);
        ObjectStoreEntity objectStore = Mockito.mock(ObjectStoreEntity.class);
        Mockito.when(dataStoreMgr.getDataStore(objectStoreId, DataStoreRole.Object)).thenReturn(objectStore);
        Mockito.when(objectStore.deleteBucket(Mockito.any(BucketTO.class))).thenReturn(true);

        bucketApiService.deleteBucket(bucketId, null);

        Mockito.verify(resourceLimitManager, Mockito.times(1))
                .decrementResourceCount(ACCOUNT_ID, Resource.ResourceType.bucket);
        Mockito.verify(resourceLimitManager, Mockito.times(1))
                .decrementResourceCount(ACCOUNT_ID, Resource.ResourceType.object_storage,
                        quota * Resource.ResourceType.bytesToGiB);
    }

    @Test
    public void testUpdateBucket() throws ResourceAllocationException {
        Long bucketId = 1L;
        Long objectStoreId = 2L;
        Integer bucketQuota = 2;
        Integer cmdQuota = 1;
        String bucketName = "bucket1";

        UpdateBucketCmd cmd = Mockito.mock(UpdateBucketCmd.class);
        Mockito.when(cmd.getId()).thenReturn(bucketId);
        Mockito.when(cmd.getQuota()).thenReturn(cmdQuota);

        BucketVO bucket = new BucketVO(bucketName);
        ReflectionTestUtils.setField(bucket, "quota", bucketQuota);
        ReflectionTestUtils.setField(bucket, "accountId", ACCOUNT_ID);
        ReflectionTestUtils.setField(bucket, "objectStoreId", objectStoreId);
        Mockito.when(bucketDao.findById(bucketId)).thenReturn(bucket);

        ObjectStoreVO objectStoreVO = Mockito.mock(ObjectStoreVO.class);
        Mockito.when(objectStoreVO.getId()).thenReturn(objectStoreId);
        Mockito.when(objectStoreDao.findById(objectStoreId)).thenReturn(objectStoreVO);
        ObjectStoreEntity objectStore = Mockito.mock(ObjectStoreEntity.class);
        Mockito.when(dataStoreMgr.getDataStore(objectStoreId, DataStoreRole.Object)).thenReturn(objectStore);

        bucketApiService.updateBucket(cmd, null);

        Mockito.verify(resourceLimitManager, Mockito.times(1))
                .decrementResourceCount(ACCOUNT_ID, Resource.ResourceType.object_storage,
                        (bucketQuota - cmdQuota) * Resource.ResourceType.bytesToGiB);
    }

    // ---- per-bucket credentials ----

    private static final long BUCKET_ID = 42L;
    private static final long CREDENTIAL_ID = 77L;
    private static final long OBJECT_STORE_ID = 5L;

    private BucketVO dedicatedBucket() {
        BucketVO bucket = new BucketVO("bucket1");
        ReflectionTestUtils.setField(bucket, "id", BUCKET_ID);
        ReflectionTestUtils.setField(bucket, "accountId", ACCOUNT_ID);
        ReflectionTestUtils.setField(bucket, "objectStoreId", OBJECT_STORE_ID);
        ReflectionTestUtils.setField(bucket, "state", Bucket.State.Created);
        bucket.setAccessKey("AK1");
        bucket.setSecretKey("SK1");
        Mockito.when(bucketDao.findById(BUCKET_ID)).thenReturn(bucket);
        return bucket;
    }

    private ObjectStoreEntity objectStoreFor(BucketVO bucket) {
        ObjectStoreVO objectStoreVO = Mockito.mock(ObjectStoreVO.class);
        Mockito.when(objectStoreVO.getId()).thenReturn(OBJECT_STORE_ID);
        Mockito.when(objectStoreDao.findById(OBJECT_STORE_ID)).thenReturn(objectStoreVO);
        ObjectStoreEntity objectStore = Mockito.mock(ObjectStoreEntity.class);
        Mockito.when(dataStoreMgr.getDataStore(OBJECT_STORE_ID, DataStoreRole.Object)).thenReturn(objectStore);
        return objectStore;
    }

    private BucketCredentialVO credentialFor(BucketVO bucket, BucketCredentialKeyVO... keys) {
        BucketCredentialVO credential = new BucketCredentialVO(bucket.getId(), "iam-user");
        ReflectionTestUtils.setField(credential, "id", CREDENTIAL_ID);
        Mockito.when(bucketCredentialDao.findByBucketId(bucket.getId())).thenReturn(credential);
        List<BucketCredentialKeyVO> keyList = new ArrayList<>();
        for (BucketCredentialKeyVO key : keys) {
            keyList.add(key);
        }
        Mockito.when(bucketCredentialKeyDao.listByCredentialId(CREDENTIAL_ID)).thenReturn(keyList);
        return credential;
    }

    private BucketCredentialKeyVO key(int slot, String accessKey, BucketCredentialKey.State state, long createdOffsetMillis) {
        BucketCredentialKeyVO key = new BucketCredentialKeyVO(CREDENTIAL_ID, slot, accessKey, "secret-" + accessKey);
        ReflectionTestUtils.setField(key, "id", (long) slot);
        key.setState(state);
        key.setCreated(new java.util.Date(1_000_000L + createdOffsetMillis));
        return key;
    }

    private RotateBucketKeyCmd rotateCmd(Integer slot) {
        RotateBucketKeyCmd cmd = Mockito.mock(RotateBucketKeyCmd.class);
        Mockito.when(cmd.getId()).thenReturn(BUCKET_ID);
        Mockito.when(cmd.getKeySlot()).thenReturn(slot);
        return cmd;
    }

    @Test
    public void testCreateBucketProvisionsDedicatedCredentialAndMirrorsKey() {
        CreateBucketCmd cmd = Mockito.mock(CreateBucketCmd.class);
        Mockito.when(cmd.getObjectStoragePoolId()).thenReturn(OBJECT_STORE_ID);
        Mockito.when(cmd.getEntityId()).thenReturn(BUCKET_ID);

        BucketVO bucket = dedicatedBucket();
        bucket.setAccessKey("account-ak");
        bucket.setSecretKey("account-sk");
        ObjectStoreEntity objectStore = objectStoreFor(bucket);
        Mockito.when(objectStore.createBucket(bucket, false)).thenReturn(bucket);
        Mockito.when(objectStore.accountSupportsBucketCredentials(ACCOUNT_ID)).thenReturn(true);
        Mockito.when(objectStore.createBucketCredential(any(BucketTO.class)))
                .thenReturn(new BucketCredentialTO("iam-user", List.of(new BucketKeyTO("AK1", "SK1"))));
        Mockito.when(bucketCredentialDao.persist(any(BucketCredentialVO.class))).thenAnswer(invocation -> {
            BucketCredentialVO credential = invocation.getArgument(0);
            ReflectionTestUtils.setField(credential, "id", CREDENTIAL_ID);
            return credential;
        });

        bucketApiService.createBucket(cmd);

        Assert.assertEquals(Bucket.State.Created, bucket.getState());
        Assert.assertEquals("AK1", bucket.getAccessKey());
        Assert.assertEquals("SK1", bucket.getSecretKey());
        Mockito.verify(bucketCredentialDao).persist(any(BucketCredentialVO.class));
        Mockito.verify(bucketCredentialKeyDao).persist(any(BucketCredentialKeyVO.class));
    }

    @Test
    public void testCreateBucketSkipsCredentialWhenAccountNotSupported() {
        CreateBucketCmd cmd = Mockito.mock(CreateBucketCmd.class);
        Mockito.when(cmd.getObjectStoragePoolId()).thenReturn(OBJECT_STORE_ID);
        Mockito.when(cmd.getEntityId()).thenReturn(BUCKET_ID);

        BucketVO bucket = dedicatedBucket();
        ObjectStoreEntity objectStore = objectStoreFor(bucket);
        Mockito.when(objectStore.createBucket(bucket, false)).thenReturn(bucket);
        Mockito.when(objectStore.accountSupportsBucketCredentials(ACCOUNT_ID)).thenReturn(false);

        bucketApiService.createBucket(cmd);

        Mockito.verify(objectStore, Mockito.never()).createBucketCredential(any(BucketTO.class));
        Mockito.verify(bucketCredentialDao, Mockito.never()).persist(any(BucketCredentialVO.class));
    }

    @Test
    public void testCreateBucketRollbackRemovesCredential() {
        CreateBucketCmd cmd = Mockito.mock(CreateBucketCmd.class);
        Mockito.when(cmd.getObjectStoragePoolId()).thenReturn(OBJECT_STORE_ID);
        Mockito.when(cmd.getEntityId()).thenReturn(BUCKET_ID);
        Mockito.when(cmd.isVersioning()).thenReturn(true);

        BucketVO bucket = dedicatedBucket();
        ObjectStoreEntity objectStore = objectStoreFor(bucket);
        Mockito.when(objectStore.createBucket(bucket, false)).thenReturn(bucket);
        Mockito.when(objectStore.accountSupportsBucketCredentials(ACCOUNT_ID)).thenReturn(true);
        Mockito.when(objectStore.createBucketCredential(any(BucketTO.class)))
                .thenReturn(new BucketCredentialTO("iam-user", List.of(new BucketKeyTO("AK1", "SK1"))));
        BucketCredentialVO credential = credentialFor(bucket, key(1, "AK1", BucketCredentialKey.State.Active, 0));
        Mockito.when(bucketCredentialDao.persist(any(BucketCredentialVO.class))).thenReturn(credential);
        Mockito.when(objectStore.setBucketVersioning(any(BucketTO.class))).thenThrow(new RuntimeException("versioning failed"));

        try {
            bucketApiService.createBucket(cmd);
            Assert.fail("expected the bucket creation to fail");
        } catch (Exception expected) {
            // rollback path under test
        }

        Mockito.verify(objectStore).deleteBucketCredential(any(BucketTO.class));
        Mockito.verify(bucketCredentialDao).expunge(CREDENTIAL_ID);
        Mockito.verify(objectStore).deleteBucket(any(BucketTO.class));
        Mockito.verify(bucketDao).remove(BUCKET_ID);
    }

    @Test
    public void testDeleteBucketRemovesDedicatedCredential() throws ResourceAllocationException {
        BucketVO bucket = dedicatedBucket();
        Mockito.when(accountManager.getAccount(ACCOUNT_ID)).thenReturn(mock(AccountVO.class));
        ObjectStoreEntity objectStore = objectStoreFor(bucket);
        Mockito.when(objectStore.deleteBucket(any(BucketTO.class))).thenReturn(true);
        credentialFor(bucket, key(1, "AK1", BucketCredentialKey.State.Active, 0));

        bucketApiService.deleteBucket(BUCKET_ID, null);

        Mockito.verify(objectStore).deleteBucketCredential(any(BucketTO.class));
        Mockito.verify(bucketCredentialKeyDao).expunge(1L);
        Mockito.verify(bucketCredentialDao).expunge(CREDENTIAL_ID);
        Mockito.verify(bucketDao).remove(BUCKET_ID);
    }

    @Test
    public void testRotateIntoFreeSlotCreatesBeforeRemovingNothing() {
        BucketVO bucket = dedicatedBucket();
        ObjectStoreEntity objectStore = objectStoreFor(bucket);
        credentialFor(bucket, key(1, "AK1", BucketCredentialKey.State.Active, 0));
        Mockito.when(objectStore.createBucketCredentialKey(any(BucketTO.class), any())).thenReturn(new BucketKeyTO("AK2", "SK2"));
        Mockito.when(bucketCredentialKeyDao.persist(any(BucketCredentialKeyVO.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BucketCredentialKey newKey = bucketApiService.rotateBucketKey(rotateCmd(null), null);

        Assert.assertEquals(2, newKey.getKeySlot());
        Assert.assertEquals("AK2", newKey.getAccessKey());
        Mockito.verify(objectStore, Mockito.never()).removeBucketCredentialKey(any(BucketTO.class), anyString());
        Mockito.verify(objectStore).createBucketCredentialKey(any(BucketTO.class), any());
    }

    @Test
    public void testRotateOccupiedSlotWithOtherActiveRemovesFirst() {
        BucketVO bucket = dedicatedBucket();
        ObjectStoreEntity objectStore = objectStoreFor(bucket);
        BucketCredentialKeyVO slot1 = key(1, "AK1", BucketCredentialKey.State.Active, 0);
        BucketCredentialKeyVO slot2 = key(2, "AK2", BucketCredentialKey.State.Active, 10);
        credentialFor(bucket, slot1, slot2);
        Mockito.when(objectStore.createBucketCredentialKey(any(BucketTO.class), any())).thenReturn(new BucketKeyTO("AK3", "SK3"));

        BucketCredentialKey newKey = bucketApiService.rotateBucketKey(rotateCmd(1), null);

        Assert.assertEquals(1, newKey.getKeySlot());
        Assert.assertEquals("AK3", newKey.getAccessKey());
        Mockito.verify(objectStore).removeBucketCredentialKey(any(BucketTO.class), Mockito.eq("AK1"));
        Mockito.verify(objectStore).createBucketCredentialKey(any(BucketTO.class), any());
        Assert.assertEquals("AK3", bucket.getAccessKey());
    }

    @Test
    public void testRotateOnlyActiveKeyCreatesFirstThenRemovesOld() {
        BucketVO bucket = dedicatedBucket();
        ObjectStoreEntity objectStore = objectStoreFor(bucket);
        BucketCredentialKeyVO slot1 = key(1, "AK1", BucketCredentialKey.State.Active, 0);
        BucketCredentialKeyVO slot2 = key(2, "AK2", BucketCredentialKey.State.Revoked, 10);
        credentialFor(bucket, slot1, slot2);
        Mockito.when(objectStore.createBucketCredentialKey(any(BucketTO.class), any())).thenReturn(new BucketKeyTO("AK3", "SK3"));

        bucketApiService.rotateBucketKey(rotateCmd(1), null);

        org.mockito.InOrder order = Mockito.inOrder(objectStore);
        order.verify(objectStore).createBucketCredentialKey(any(BucketTO.class), any());
        order.verify(objectStore).removeBucketCredentialKey(any(BucketTO.class), Mockito.eq("AK1"));
        Assert.assertEquals("AK3", bucket.getAccessKey());
    }

    @Test
    public void testRotateRejectsAmbiguousSlot() {
        BucketVO bucket = dedicatedBucket();
        objectStoreFor(bucket);
        credentialFor(bucket, key(1, "AK1", BucketCredentialKey.State.Active, 0), key(2, "AK2", BucketCredentialKey.State.Active, 10));
        try {
            bucketApiService.rotateBucketKey(rotateCmd(null), null);
            Assert.fail("expected rejection when both slots are active and none is named");
        } catch (InvalidParameterValueException expected) {
            Assert.assertTrue(expected.getMessage().contains("specify the slot"));
        }
    }

    @Test
    public void testRotateCompensatesWhenRecordingFails() {
        BucketVO bucket = dedicatedBucket();
        ObjectStoreEntity objectStore = objectStoreFor(bucket);
        credentialFor(bucket, key(1, "AK1", BucketCredentialKey.State.Active, 0));
        Mockito.when(objectStore.createBucketCredentialKey(any(BucketTO.class), any())).thenReturn(new BucketKeyTO("AK2", "SK2"));
        Mockito.when(bucketCredentialKeyDao.persist(any(BucketCredentialKeyVO.class))).thenThrow(new RuntimeException("db down"));

        try {
            bucketApiService.rotateBucketKey(rotateCmd(null), null);
            Assert.fail("expected the DB failure to propagate");
        } catch (RuntimeException expected) {
            Assert.assertEquals("db down", expected.getMessage());
        }
        Mockito.verify(objectStore).removeBucketCredentialKey(any(BucketTO.class), Mockito.eq("AK2"));
    }

    @Test
    public void testRotateRequiresDedicatedCredential() {
        BucketVO bucket = dedicatedBucket();
        Mockito.when(bucketCredentialDao.findByBucketId(BUCKET_ID)).thenReturn(null);
        try {
            bucketApiService.rotateBucketKey(rotateCmd(null), null);
            Assert.fail("expected rejection for an account-scoped bucket");
        } catch (InvalidParameterValueException expected) {
            Assert.assertTrue(expected.getMessage().contains("per-bucket credential first"));
        }
    }

    @Test
    public void testRevokeKey() {
        BucketVO bucket = dedicatedBucket();
        ObjectStoreEntity objectStore = objectStoreFor(bucket);
        BucketCredentialKeyVO slot1 = key(1, "AK1", BucketCredentialKey.State.Active, 0);
        BucketCredentialKeyVO slot2 = key(2, "AK2", BucketCredentialKey.State.Active, 10);
        credentialFor(bucket, slot1, slot2);
        RevokeBucketKeyCmd cmd = Mockito.mock(RevokeBucketKeyCmd.class);
        Mockito.when(cmd.getId()).thenReturn(BUCKET_ID);
        Mockito.when(cmd.getKeySlot()).thenReturn(1);

        Assert.assertTrue(bucketApiService.revokeBucketKey(cmd, null));

        Mockito.verify(objectStore).removeBucketCredentialKey(any(BucketTO.class), Mockito.eq("AK1"));
        Assert.assertEquals(BucketCredentialKey.State.Revoked, slot1.getState());
        Assert.assertNull(slot1.getSecretKey());
        Assert.assertEquals("AK2", bucket.getAccessKey());
    }

    @Test
    public void testRevokeLastActiveKeyIsRejected() {
        BucketVO bucket = dedicatedBucket();
        ObjectStoreEntity objectStore = objectStoreFor(bucket);
        credentialFor(bucket, key(1, "AK1", BucketCredentialKey.State.Active, 0), key(2, "AK2", BucketCredentialKey.State.Revoked, 10));
        RevokeBucketKeyCmd cmd = Mockito.mock(RevokeBucketKeyCmd.class);
        Mockito.when(cmd.getId()).thenReturn(BUCKET_ID);
        Mockito.when(cmd.getKeySlot()).thenReturn(1);
        try {
            bucketApiService.revokeBucketKey(cmd, null);
            Assert.fail("expected rejection of revoking the only active key");
        } catch (InvalidParameterValueException expected) {
            Assert.assertTrue(expected.getMessage().contains("only active key"));
        }
        Mockito.verify(objectStore, Mockito.never()).removeBucketCredentialKey(any(BucketTO.class), anyString());
    }

    @Test
    public void testMigrateBucketCredentialRequiresMigratedAccount() {
        BucketVO bucket = dedicatedBucket();
        ObjectStoreEntity objectStore = objectStoreFor(bucket);
        Mockito.when(bucketCredentialDao.findByBucketId(BUCKET_ID)).thenReturn(null);
        Mockito.when(objectStore.supportsBucketCredentials()).thenReturn(true);
        Mockito.when(objectStore.accountSupportsBucketCredentials(ACCOUNT_ID)).thenReturn(false);
        MigrateBucketCredentialCmd cmd = Mockito.mock(MigrateBucketCredentialCmd.class);
        Mockito.when(cmd.getId()).thenReturn(BUCKET_ID);
        try {
            bucketApiService.migrateBucketCredential(cmd, null);
            Assert.fail("expected rejection while the account is not migrated");
        } catch (InvalidParameterValueException expected) {
            Assert.assertTrue(expected.getMessage().contains("Object Storage tab"));
        }
        Mockito.verify(objectStore, Mockito.never()).createBucketCredential(any(BucketTO.class));
    }

    @Test
    public void testMigrateBucketCredentialProvisionsCredential() {
        BucketVO bucket = dedicatedBucket();
        ObjectStoreEntity objectStore = objectStoreFor(bucket);
        Mockito.when(bucketCredentialDao.findByBucketId(BUCKET_ID)).thenReturn(null);
        Mockito.when(objectStore.supportsBucketCredentials()).thenReturn(true);
        Mockito.when(objectStore.accountSupportsBucketCredentials(ACCOUNT_ID)).thenReturn(true);
        Mockito.when(objectStore.createBucketCredential(any(BucketTO.class)))
                .thenReturn(new BucketCredentialTO("iam-user", List.of(new BucketKeyTO("AK9", "SK9"))));
        Mockito.when(bucketCredentialDao.persist(any(BucketCredentialVO.class))).thenAnswer(invocation -> {
            BucketCredentialVO credential = invocation.getArgument(0);
            ReflectionTestUtils.setField(credential, "id", CREDENTIAL_ID);
            return credential;
        });
        MigrateBucketCredentialCmd cmd = Mockito.mock(MigrateBucketCredentialCmd.class);
        Mockito.when(cmd.getId()).thenReturn(BUCKET_ID);

        bucketApiService.migrateBucketCredential(cmd, null);

        Assert.assertEquals("AK9", bucket.getAccessKey());
        Mockito.verify(bucketCredentialKeyDao).persist(any(BucketCredentialKeyVO.class));
    }

    private RotateObjectStoreAccountKeyCmd rotateAccountCmd() {
        RotateObjectStoreAccountKeyCmd cmd = Mockito.mock(RotateObjectStoreAccountKeyCmd.class);
        Mockito.when(cmd.getAccountId()).thenReturn(ACCOUNT_ID);
        Mockito.when(cmd.getObjectStoreId()).thenReturn(OBJECT_STORE_ID);
        return cmd;
    }

    private ObjectStoreEntity accountModeStore() {
        ObjectStoreVO objectStoreVO = Mockito.mock(ObjectStoreVO.class);
        Mockito.when(objectStoreVO.getId()).thenReturn(OBJECT_STORE_ID);
        Mockito.when(objectStoreVO.getName()).thenReturn("ceph");
        Mockito.when(objectStoreDao.findById(OBJECT_STORE_ID)).thenReturn(objectStoreVO);
        ObjectStoreEntity objectStore = Mockito.mock(ObjectStoreEntity.class);
        Mockito.when(dataStoreMgr.getDataStore(OBJECT_STORE_ID, DataStoreRole.Object)).thenReturn(objectStore);
        Mockito.when(objectStore.accountSupportsBucketCredentials(ACCOUNT_ID)).thenReturn(true);
        Mockito.when(mockAccountVO.getId()).thenReturn(ACCOUNT_ID);
        return objectStore;
    }

    @Test
    public void testRotateObjectStoreAccountKeyRefusedWhileLegacyBucketsRemain() {
        ObjectStoreEntity objectStore = accountModeStore();
        BucketVO legacy = new BucketVO("backups");
        ReflectionTestUtils.setField(legacy, "id", 9L);
        Mockito.when(bucketDao.listByObjectStoreIdAndAccountId(OBJECT_STORE_ID, ACCOUNT_ID)).thenReturn(List.of(legacy));
        Mockito.when(bucketCredentialDao.findByBucketId(9L)).thenReturn(null);
        try {
            bucketApiService.rotateObjectStoreAccountKey(rotateAccountCmd(), mock(Account.class));
            Assert.fail("expected refusal while a legacy bucket remains");
        } catch (InvalidParameterValueException expected) {
            Assert.assertTrue(expected.getMessage().contains("backups"));
        }
        Mockito.verify(objectStore, Mockito.never()).rotateAccountKey(ACCOUNT_ID);
    }

    @Test
    public void testRotateObjectStoreAccountKeyRotatesWhenAllBucketsMigrated() {
        ObjectStoreEntity objectStore = accountModeStore();
        BucketVO migrated = new BucketVO("data");
        ReflectionTestUtils.setField(migrated, "id", 9L);
        Mockito.when(bucketDao.listByObjectStoreIdAndAccountId(OBJECT_STORE_ID, ACCOUNT_ID)).thenReturn(List.of(migrated));
        Mockito.when(bucketCredentialDao.findByBucketId(9L)).thenReturn(new BucketCredentialVO(9L, "iam-user"));
        Mockito.when(objectStore.rotateAccountKey(ACCOUNT_ID)).thenReturn(new BucketKeyTO("AK", "SK"));
        Account caller = mock(Account.class);

        Assert.assertTrue(bucketApiService.rotateObjectStoreAccountKey(rotateAccountCmd(), caller));

        Mockito.verify(accountManager).checkAccess(caller, null, true, mockAccountVO);
        Mockito.verify(objectStore).rotateAccountKey(ACCOUNT_ID);
    }

    @Test
    public void testRotateObjectStoreAccountKeyRequiresMigratedAccount() {
        ObjectStoreEntity objectStore = accountModeStore();
        Mockito.when(objectStore.accountSupportsBucketCredentials(ACCOUNT_ID)).thenReturn(false);
        try {
            bucketApiService.rotateObjectStoreAccountKey(rotateAccountCmd(), mock(Account.class));
            Assert.fail("expected refusal for an unmigrated account");
        } catch (InvalidParameterValueException expected) {
            Assert.assertTrue(expected.getMessage().contains("Migrate the account on this store first"));
        }
    }

    @Test
    public void testCountAccountScopedBuckets() {
        BucketVO a = new BucketVO("a"); ReflectionTestUtils.setField(a, "id", 1L);
        BucketVO b = new BucketVO("b"); ReflectionTestUtils.setField(b, "id", 2L);
        BucketVO c = new BucketVO("c"); ReflectionTestUtils.setField(c, "id", 3L);
        Mockito.when(bucketDao.listByObjectStoreIdAndAccountId(OBJECT_STORE_ID, ACCOUNT_ID)).thenReturn(List.of(a, b, c));
        Mockito.when(bucketCredentialDao.findByBucketId(2L)).thenReturn(new BucketCredentialVO(2L, "iam-user"));
        Assert.assertEquals(2, bucketApiService.countAccountScopedBuckets(ACCOUNT_ID, OBJECT_STORE_ID));
    }

    @Test
    public void testMigrateObjectStoreAccountChecksAccessAndDelegates() {
        ObjectStoreVO objectStoreVO = Mockito.mock(ObjectStoreVO.class);
        Mockito.when(objectStoreVO.getId()).thenReturn(OBJECT_STORE_ID);
        Mockito.when(objectStoreDao.findById(OBJECT_STORE_ID)).thenReturn(objectStoreVO);
        ObjectStoreEntity objectStore = Mockito.mock(ObjectStoreEntity.class);
        Mockito.when(dataStoreMgr.getDataStore(OBJECT_STORE_ID, DataStoreRole.Object)).thenReturn(objectStore);
        Mockito.when(objectStore.supportsBucketCredentials()).thenReturn(true);
        Mockito.when(objectStore.migrateAccountForBucketCredentials(ACCOUNT_ID)).thenReturn(true);
        Mockito.when(mockAccountVO.getId()).thenReturn(ACCOUNT_ID);
        MigrateObjectStoreAccountCmd cmd = Mockito.mock(MigrateObjectStoreAccountCmd.class);
        Mockito.when(cmd.getAccountId()).thenReturn(ACCOUNT_ID);
        Mockito.when(cmd.getObjectStoreId()).thenReturn(OBJECT_STORE_ID);
        Account caller = mock(Account.class);

        Assert.assertTrue(bucketApiService.migrateObjectStoreAccount(cmd, caller));

        Mockito.verify(accountManager).checkAccess(caller, null, true, mockAccountVO);
        Mockito.verify(objectStore).migrateAccountForBucketCredentials(ACCOUNT_ID);
    }

    @Test
    public void testMigratedAccountNeverFallsBackToTheSharedKey() {
        ObjectStoreEntity objectStore = Mockito.mock(ObjectStoreEntity.class);
        Mockito.when(objectStore.accountSupportsBucketCredentials(ACCOUNT_ID)).thenReturn(true);
        // the global setting does not re-share a migrated account's key, whatever it is set to
        Assert.assertTrue(bucketApiService.isPerBucketCredentialsEnabled(objectStore, ACCOUNT_ID));
    }

    @Test
    public void testUnmigratedAccountKeepsTheSharedKey() {
        ObjectStoreEntity objectStore = Mockito.mock(ObjectStoreEntity.class);
        Mockito.when(objectStore.accountSupportsBucketCredentials(ACCOUNT_ID)).thenReturn(false);
        Assert.assertFalse(bucketApiService.isPerBucketCredentialsEnabled(objectStore, ACCOUNT_ID));
    }

    @Test
    public void testMigrateObjectStoreAccountRefusedOnUnsupportedStore() {
        ObjectStoreVO objectStoreVO = Mockito.mock(ObjectStoreVO.class);
        Mockito.when(objectStoreVO.getId()).thenReturn(OBJECT_STORE_ID);
        Mockito.when(objectStoreVO.getName()).thenReturn("reef-store");
        Mockito.when(objectStoreDao.findById(OBJECT_STORE_ID)).thenReturn(objectStoreVO);
        ObjectStoreEntity objectStore = Mockito.mock(ObjectStoreEntity.class);
        Mockito.when(dataStoreMgr.getDataStore(OBJECT_STORE_ID, DataStoreRole.Object)).thenReturn(objectStore);
        Mockito.when(objectStore.supportsBucketCredentials()).thenReturn(false);
        Mockito.when(objectStore.bucketCredentialsUnsupportedReason())
                .thenReturn("the object store's admin credential is missing the 'accounts' capability");
        MigrateObjectStoreAccountCmd cmd = Mockito.mock(MigrateObjectStoreAccountCmd.class);
        Mockito.when(cmd.getAccountId()).thenReturn(ACCOUNT_ID);
        Mockito.when(cmd.getObjectStoreId()).thenReturn(OBJECT_STORE_ID);
        Account caller = mock(Account.class);
        Mockito.when(caller.getId()).thenReturn(1L);
        Mockito.when(accountManager.isRootAdmin(1L)).thenReturn(true);

        try {
            bucketApiService.migrateObjectStoreAccount(cmd, caller);
            Assert.fail("expected refusal on a store without per-bucket credential support");
        } catch (InvalidParameterValueException expected) {
            Assert.assertTrue(expected.getMessage().contains("does not support per-bucket credentials"));
            // an operator is told which of the three possible causes applies
            Assert.assertTrue(expected.getMessage().contains("missing the 'accounts' capability"));
        }
        Mockito.verify(objectStore, Mockito.never()).migrateAccountForBucketCredentials(Mockito.anyLong());
    }

    @Test
    public void testMigrateRefusalHidesTheGatewayDetailFromADomainAdmin() {
        ObjectStoreVO objectStoreVO = Mockito.mock(ObjectStoreVO.class);
        Mockito.when(objectStoreVO.getId()).thenReturn(OBJECT_STORE_ID);
        Mockito.when(objectStoreVO.getName()).thenReturn("reef-store");
        Mockito.when(objectStoreDao.findById(OBJECT_STORE_ID)).thenReturn(objectStoreVO);
        ObjectStoreEntity objectStore = Mockito.mock(ObjectStoreEntity.class);
        Mockito.when(dataStoreMgr.getDataStore(OBJECT_STORE_ID, DataStoreRole.Object)).thenReturn(objectStore);
        Mockito.when(objectStore.supportsBucketCredentials()).thenReturn(false);
        MigrateObjectStoreAccountCmd cmd = Mockito.mock(MigrateObjectStoreAccountCmd.class);
        Mockito.when(cmd.getAccountId()).thenReturn(ACCOUNT_ID);
        Mockito.when(cmd.getObjectStoreId()).thenReturn(OBJECT_STORE_ID);
        Account caller = mock(Account.class);
        Mockito.when(caller.getId()).thenReturn(2L);
        Mockito.when(accountManager.isRootAdmin(2L)).thenReturn(false);

        try {
            bucketApiService.migrateObjectStoreAccount(cmd, caller);
            Assert.fail("expected refusal");
        } catch (InvalidParameterValueException expected) {
            // the gateway's own credential and capabilities are the platform's business
            Assert.assertTrue(expected.getMessage().contains("Contact your platform administrator"));
            Assert.assertFalse(expected.getMessage().contains("capability"));
        }
        Mockito.verify(objectStore, Mockito.never()).bucketCredentialsUnsupportedReason();
    }
}

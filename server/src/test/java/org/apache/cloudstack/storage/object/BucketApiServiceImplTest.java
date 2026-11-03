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

import org.apache.cloudstack.api.command.user.bucket.CreateBucketCmd;
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

import com.cloud.agent.api.to.BucketTO;
import com.cloud.configuration.Resource;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.resourcelimit.ResourceLimitManagerImpl;
import com.cloud.storage.BucketVO;
import com.cloud.storage.DataStoreRole;
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
}

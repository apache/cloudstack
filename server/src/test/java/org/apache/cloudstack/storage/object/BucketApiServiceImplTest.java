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

import org.apache.cloudstack.api.command.user.bucket.CreateBucketCmd;
import org.apache.cloudstack.api.command.user.bucket.UpdateBucketCmd;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.storage.datastore.db.ObjectStoreDao;
import org.apache.cloudstack.storage.datastore.db.ObjectStoreVO;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
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

    @Test
    public void testAllocBucket() throws ResourceAllocationException {
        String bucketName = "bucket1";
        Long accountId = 1L;
        Long poolId = 2L;
        Long objectStoreId = 3L;

        CreateBucketCmd cmd = Mockito.mock(CreateBucketCmd.class);
        Mockito.when(cmd.getBucketName()).thenReturn(bucketName);
        Mockito.when(cmd.getEntityOwnerId()).thenReturn(accountId);
        Mockito.when(cmd.getObjectStoragePoolId()).thenReturn(poolId);
        Mockito.when(cmd.getQuota()).thenReturn(1);

        Account account = Mockito.mock(Account.class);
        Mockito.when(accountManager.getActiveAccountById(accountId)).thenReturn(account);

        ObjectStoreVO objectStoreVO = Mockito.mock(ObjectStoreVO.class);
        Mockito.when(objectStoreVO.getId()).thenReturn(objectStoreId);
        Mockito.when(objectStoreDao.findById(poolId)).thenReturn(objectStoreVO);
        ObjectStoreEntity objectStore = Mockito.mock(ObjectStoreEntity.class);
        Mockito.when(dataStoreMgr.getDataStore(objectStoreId, DataStoreRole.Object)).thenReturn(objectStore);
        Mockito.when(objectStore.createUser(accountId)).thenReturn(true);

        bucketApiService.allocBucket(cmd);

        Mockito.verify(resourceLimitManager, Mockito.times(1)).checkResourceLimit(account, Resource.ResourceType.bucket);
        Mockito.verify(resourceLimitManager, Mockito.times(1)).checkResourceLimit(account, Resource.ResourceType.object_storage, 1 * Resource.ResourceType.bytesToGiB);
    }

    @Test
    public void testCreateBucket() {
        Long objectStoreId = 1L;
        Long poolId = 2L;
        Long bucketId = 3L;
        Long accountId = 4L;
        String bucketName = "bucket1";

        CreateBucketCmd cmd = Mockito.mock(CreateBucketCmd.class);
        Mockito.when(cmd.getObjectStoragePoolId()).thenReturn(poolId);
        Mockito.when(cmd.getEntityId()).thenReturn(bucketId);
        Mockito.when(cmd.getQuota()).thenReturn(1);

        BucketVO bucket = new BucketVO(bucketName);
        Mockito.when(bucketDao.findById(bucketId)).thenReturn(bucket);
        ReflectionTestUtils.setField(bucket, "accountId", accountId);

        ObjectStoreVO objectStoreVO = Mockito.mock(ObjectStoreVO.class);
        Mockito.when(objectStoreVO.getId()).thenReturn(objectStoreId);
        Mockito.when(objectStoreVO.getTotalSize()).thenReturn(2000000000L);
        Mockito.when(objectStoreDao.findById(poolId)).thenReturn(objectStoreVO);
        ObjectStoreEntity objectStore = Mockito.mock(ObjectStoreEntity.class);
        Mockito.when(dataStoreMgr.getDataStore(objectStoreId, DataStoreRole.Object)).thenReturn(objectStore);
        Mockito.when(objectStore.createBucket(bucket, false)).thenReturn(bucket);

        bucketApiService.createBucket(cmd);

        Mockito.verify(resourceLimitManager, Mockito.times(1)).incrementResourceCount(accountId, Resource.ResourceType.bucket);
        Mockito.verify(resourceLimitManager, Mockito.times(1)).incrementResourceCount(accountId, Resource.ResourceType.object_storage, 1 * Resource.ResourceType.bytesToGiB);
        Assert.assertEquals(bucket.getState(), Bucket.State.Created);
    }

    @Test
    public void testDeleteBucket() {
        Long bucketId = 1L;
        Long accountId = 2L;
        Long objectStoreId = 3L;
        String bucketName = "bucket1";

        BucketVO bucket = new BucketVO(bucketName);
        Mockito.when(bucketDao.findById(bucketId)).thenReturn(bucket);
        ReflectionTestUtils.setField(bucket, "objectStoreId", objectStoreId);
        ReflectionTestUtils.setField(bucket, "quota", 1);
        ReflectionTestUtils.setField(bucket, "accountId", accountId);

        ObjectStoreVO objectStoreVO = Mockito.mock(ObjectStoreVO.class);
        Mockito.when(objectStoreVO.getId()).thenReturn(objectStoreId);
        Mockito.when(objectStoreDao.findById(objectStoreId)).thenReturn(objectStoreVO);
        ObjectStoreEntity objectStore = Mockito.mock(ObjectStoreEntity.class);
        Mockito.when(dataStoreMgr.getDataStore(objectStoreId, DataStoreRole.Object)).thenReturn(objectStore);
        Mockito.when(objectStore.deleteBucket(Mockito.any(BucketTO.class))).thenReturn(true);

        bucketApiService.deleteBucket(bucketId, null);

        Mockito.verify(resourceLimitManager, Mockito.times(1)).decrementResourceCount(accountId, Resource.ResourceType.bucket);
        Mockito.verify(resourceLimitManager, Mockito.times(1)).decrementResourceCount(accountId, Resource.ResourceType.object_storage, 1 * Resource.ResourceType.bytesToGiB);
    }

    @Test
    public void testUpdateBucket() throws ResourceAllocationException {
        Long bucketId = 1L;
        Long objectStoreId = 2L;
        Long accountId = 3L;
        Integer bucketQuota = 2;
        Integer cmdQuota = 1;
        String bucketName = "bucket1";

        UpdateBucketCmd cmd = Mockito.mock(UpdateBucketCmd.class);
        Mockito.when(cmd.getId()).thenReturn(bucketId);
        Mockito.when(cmd.getQuota()).thenReturn(cmdQuota);

        BucketVO bucket = new BucketVO(bucketName);
        ReflectionTestUtils.setField(bucket, "quota", bucketQuota);
        ReflectionTestUtils.setField(bucket, "accountId", accountId);
        ReflectionTestUtils.setField(bucket, "objectStoreId", objectStoreId);
        Mockito.when(bucketDao.findById(bucketId)).thenReturn(bucket);

        Account account = Mockito.mock(Account.class);

        ObjectStoreVO objectStoreVO = Mockito.mock(ObjectStoreVO.class);
        Mockito.when(objectStoreVO.getId()).thenReturn(objectStoreId);
        Mockito.when(objectStoreDao.findById(objectStoreId)).thenReturn(objectStoreVO);
        ObjectStoreEntity objectStore = Mockito.mock(ObjectStoreEntity.class);
        Mockito.when(dataStoreMgr.getDataStore(objectStoreId, DataStoreRole.Object)).thenReturn(objectStore);

        bucketApiService.updateBucket(cmd, null);

        Mockito.verify(resourceLimitManager, Mockito.times(1)).decrementResourceCount(accountId, Resource.ResourceType.object_storage, (bucketQuota - cmdQuota) * Resource.ResourceType.bytesToGiB);
    }
}

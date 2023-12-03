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

import com.cloud.storage.BucketVO;
import com.cloud.storage.dao.BucketDao;
import com.cloud.user.AccountDetailVO;
import com.cloud.user.AccountDetailsDao;
import com.cloud.user.AccountVO;
import com.cloud.user.dao.AccountDao;
import io.minio.BucketExistsArgs;
import io.minio.MinioClient;
import io.minio.RemoveBucketArgs;
import io.minio.admin.MinioAdminClient;
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

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MinIOObjectStoreDriverImplTest {

    @Spy
    MinIOObjectStoreDriverImpl minioObjectStoreDriverImpl = new MinIOObjectStoreDriverImpl();

    @Mock
    MinioClient minioClient;
    @Mock
    MinioAdminClient minioAdminClient;
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
        minioObjectStoreDriverImpl._storeDao = objectStoreDao;
        minioObjectStoreDriverImpl._storeDetailsDao = objectStoreDetailsDao;
        minioObjectStoreDriverImpl._accountDao = accountDao;
        minioObjectStoreDriverImpl._bucketDao = bucketDao;
        minioObjectStoreDriverImpl._accountDetailsDao = accountDetailsDao;
        bucket = new BucketVO();
        bucket.setName("test-bucket");
        when(objectStoreVO.getUrl()).thenReturn("http://localhost:9000");
        when(objectStoreDao.findById(any())).thenReturn(objectStoreVO);
    }

    @Test
    public void testCreateBucket() throws Exception {
        doReturn(minioClient).when(minioObjectStoreDriverImpl).getMinIOClient(anyLong());
        doReturn(minioAdminClient).when(minioObjectStoreDriverImpl).getMinIOAdminClient(anyLong());
        when(bucketDao.listByObjectStoreIdAndAccountId(anyLong(), anyLong())).thenReturn(new ArrayList<BucketVO>());
        when(account.getAccountName()).thenReturn("admin");
        when(accountDao.findById(anyLong())).thenReturn(account);
        when(accountDetailsDao.findDetail(anyLong(),anyString())).
                thenReturn(new AccountDetailVO(1L, "abc","def"));
        when(bucketDao.findById(anyLong())).thenReturn(new BucketVO());
        Bucket bucketRet = minioObjectStoreDriverImpl.createBucket(bucket, false);
        assertEquals(bucketRet.getName(), bucket.getName());
        verify(minioClient, times(1)).bucketExists(any());
        verify(minioClient, times(1)).makeBucket(any());
    }

    @Test
    public void testDeleteBucket() throws Exception {
        String bucketName = "test-bucket";
        doReturn(minioClient).when(minioObjectStoreDriverImpl).getMinIOClient(anyLong());
        when(minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build())).thenReturn(true);
        doNothing().when(minioClient).removeBucket(RemoveBucketArgs.builder().bucket(bucketName).build());
        boolean success = minioObjectStoreDriverImpl.deleteBucket(bucketName, 1L);
        assertTrue(success);
        verify(minioClient, times(1)).bucketExists(any());
        verify(minioClient, times(1)).removeBucket(any());
    }
}

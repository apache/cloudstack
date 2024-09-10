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
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doReturn;
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
        verify(rgwClient, times(1)).getBucketAcl(anyString());
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
}

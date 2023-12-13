package org.apache.cloudstack.storage.datastore.driver;

import com.cloud.storage.BucketVO;
import com.cloud.storage.dao.BucketDao;
import com.cloud.user.AccountDetailVO;
import com.cloud.user.AccountDetailsDao;
import com.cloud.user.AccountVO;
import com.cloud.user.dao.AccountDao;
import com.obs.services.ObsClient;
import com.obs.services.model.CreateBucketRequest;
import org.apache.cloudstack.storage.datastore.db.ObjectStoreDao;
import org.apache.cloudstack.storage.datastore.db.ObjectStoreDetailsDao;
import org.apache.cloudstack.storage.datastore.db.ObjectStoreVO;
import org.apache.cloudstack.storage.object.Bucket;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.mockito.Mockito;

@RunWith(MockitoJUnitRunner.class)
public class HuaweiObsObjectStoreDriverImplTest {

    @Spy
    HuaweiObsObjectStoreDriverImpl huaweiObsObjectStoreDriverImpl = new HuaweiObsObjectStoreDriverImpl();

    @Mock
    ObsClient obsClient;
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
    String bucketName = "test-bucket";

    @Before
    public void setUp() {
        huaweiObsObjectStoreDriverImpl._storeDao = objectStoreDao;
        huaweiObsObjectStoreDriverImpl._storeDetailsDao = objectStoreDetailsDao;
        huaweiObsObjectStoreDriverImpl._accountDao = accountDao;
        huaweiObsObjectStoreDriverImpl._bucketDao = bucketDao;
        huaweiObsObjectStoreDriverImpl._accountDetailsDao = accountDetailsDao;
        bucket = new BucketVO(0, 0, 0, bucketName, 100, false, false, false, "public");
    }

    @Test
    public void testCreateBucket() throws Exception {
        Mockito.doReturn(obsClient).when(huaweiObsObjectStoreDriverImpl).getObsClient(Mockito.anyLong());
        Mockito.when(accountDao.findById(Mockito.anyLong())).thenReturn(account);
        Mockito.when(accountDetailsDao.findDetail(Mockito.anyLong(), Mockito.anyString())).thenReturn(new AccountDetailVO(1L, "abc", "def"));
        Mockito.when(obsClient.headBucket(bucketName)).thenReturn(false);
        CreateBucketRequest createBucketRequest = new CreateBucketRequest(bucketName);
        createBucketRequest.setAcl(com.obs.services.model.AccessControlList.REST_CANNED_PUBLIC_READ_WRITE);
        Mockito.when(bucketDao.findById(Mockito.anyLong())).thenReturn(new BucketVO(0, 0, 0, bucketName, 100, false, false, false, "public"));
        Mockito.when(objectStoreVO.getUrl()).thenReturn("http://test-bucket.localhost:9000");
        Mockito.when(objectStoreDao.findById(Mockito.any())).thenReturn(objectStoreVO);
        Bucket bucketRet = huaweiObsObjectStoreDriverImpl.createBucket(bucket, false);
        assertEquals(bucketRet.getName(), bucket.getName());
        Mockito.verify(obsClient, Mockito.times(1)).headBucket(Mockito.anyString());
        Mockito.verify(obsClient, Mockito.times(1)).createBucket(Mockito.any(CreateBucketRequest.class));
    }

    @Test
    public void testDeleteBucket() throws Exception {
        Mockito.doReturn(obsClient).when(huaweiObsObjectStoreDriverImpl).getObsClient(Mockito.anyLong());
        Mockito.when(obsClient.headBucket(bucketName)).thenReturn(true);
        boolean success = huaweiObsObjectStoreDriverImpl.deleteBucket(bucketName, 1L);
        assertTrue(success);
        Mockito.verify(obsClient, Mockito.times(1)).headBucket(Mockito.anyString());
        Mockito.verify(obsClient, Mockito.times(1)).deleteBucket(Mockito.anyString());
    }
}

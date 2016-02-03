package com.cloud.storage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.apache.cloudstack.storage.datastore.db.ImageStoreDao;
import org.apache.cloudstack.storage.datastore.db.ImageStoreDetailsDao;
import org.apache.cloudstack.storage.datastore.db.ImageStoreVO;
import org.junit.Before;
import org.junit.Test;

public class ImageStoreDetailsUtilTest {

    private final static long STORE_ID = 1l;
    private final static String STORE_UUID = "aaaa-aaaa-aaaa-aaaa";
    private final static String NFS_VERSION = "3";

    ImageStoreDetailsUtil imageStoreDetailsUtil = new ImageStoreDetailsUtil();

    ImageStoreDao imgStoreDao = mock(ImageStoreDao.class);
    ImageStoreDetailsDao imgStoreDetailsDao = mock(ImageStoreDetailsDao.class);

    @Before
    public void setup() throws Exception {
        Map<String, String> imgStoreDetails = new HashMap<String, String>();
        imgStoreDetails.put("nfs.version", NFS_VERSION);
        when(imgStoreDetailsDao.getDetails(STORE_ID)).thenReturn(imgStoreDetails);

        ImageStoreVO imgStoreVO = mock(ImageStoreVO.class);
        when(imgStoreVO.getId()).thenReturn(Long.valueOf(STORE_ID));
        when(imgStoreDao.findByUuid(STORE_UUID)).thenReturn(imgStoreVO);

        imageStoreDetailsUtil.imageStoreDao = imgStoreDao;
        imageStoreDetailsUtil.imageStoreDetailsDao = imgStoreDetailsDao;
    }

    @Test
    public void testGetNfsVersion(){
        String nfsVersion = imageStoreDetailsUtil.getNfsVersion(STORE_ID);
        assertEquals(NFS_VERSION, nfsVersion);
    }

    @Test
    public void testGetNfsVersionNotFound(){
        Map<String, String> imgStoreDetails = new HashMap<String, String>();
        imgStoreDetails.put("other.prop", "propValue");
        when(imgStoreDetailsDao.getDetails(STORE_ID)).thenReturn(imgStoreDetails);

        String nfsVersion = imageStoreDetailsUtil.getNfsVersion(STORE_ID);
        assertNull(nfsVersion);
    }

    @Test
    public void testGetNfsVersionNoDetails(){
        Map<String, String> imgStoreDetails = new HashMap<String, String>();
        when(imgStoreDetailsDao.getDetails(STORE_ID)).thenReturn(imgStoreDetails);

        String nfsVersion = imageStoreDetailsUtil.getNfsVersion(STORE_ID);
        assertNull(nfsVersion);
    }

    @Test
    public void testGetNfsVersionByUuid(){
        String nfsVersion = imageStoreDetailsUtil.getNfsVersionByUuid(STORE_UUID);
        assertEquals(NFS_VERSION, nfsVersion);
    }

    @Test
    public void testGetNfsVersionByUuidNoImgStore(){
        when(imgStoreDao.findByUuid(STORE_UUID)).thenReturn(null);
        String nfsVersion = imageStoreDetailsUtil.getNfsVersionByUuid(STORE_UUID);
        assertNull(nfsVersion);
    }
}
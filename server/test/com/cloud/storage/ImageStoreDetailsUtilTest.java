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
    private final static Integer NFS_VERSION = 3;

    ImageStoreDetailsUtil imageStoreDetailsUtil = new ImageStoreDetailsUtil();

    ImageStoreDao imgStoreDao = mock(ImageStoreDao.class);
    ImageStoreDetailsDao imgStoreDetailsDao = mock(ImageStoreDetailsDao.class);

    @Before
    public void setup() throws Exception {
        Map<String, String> imgStoreDetails = new HashMap<String, String>();
        imgStoreDetails.put("nfs.version", String.valueOf(NFS_VERSION));
        when(imgStoreDetailsDao.getDetails(STORE_ID)).thenReturn(imgStoreDetails);

        ImageStoreVO imgStoreVO = mock(ImageStoreVO.class);
        when(imgStoreVO.getId()).thenReturn(Long.valueOf(STORE_ID));
        when(imgStoreDao.findByUuid(STORE_UUID)).thenReturn(imgStoreVO);

        imageStoreDetailsUtil.imageStoreDao = imgStoreDao;
        imageStoreDetailsUtil.imageStoreDetailsDao = imgStoreDetailsDao;
    }

    @Test
    public void testGetNfsVersion(){
        Integer nfsVersion = imageStoreDetailsUtil.getNfsVersion(STORE_ID);
        assertEquals(NFS_VERSION, nfsVersion);
    }

    @Test
    public void testGetNfsVersionNotFound(){
        Map<String, String> imgStoreDetails = new HashMap<String, String>();
        imgStoreDetails.put("other.prop", "propValue");
        when(imgStoreDetailsDao.getDetails(STORE_ID)).thenReturn(imgStoreDetails);

        Integer nfsVersion = imageStoreDetailsUtil.getNfsVersion(STORE_ID);
        assertNull(nfsVersion);
    }

    @Test
    public void testGetNfsVersionNoDetails(){
        Map<String, String> imgStoreDetails = new HashMap<String, String>();
        when(imgStoreDetailsDao.getDetails(STORE_ID)).thenReturn(imgStoreDetails);

        Integer nfsVersion = imageStoreDetailsUtil.getNfsVersion(STORE_ID);
        assertNull(nfsVersion);
    }

    @Test
    public void testGetNfsVersionByUuid(){
        Integer nfsVersion = imageStoreDetailsUtil.getNfsVersionByUuid(STORE_UUID);
        assertEquals(NFS_VERSION, nfsVersion);
    }

    @Test
    public void testGetNfsVersionByUuidNoImgStore(){
        when(imgStoreDao.findByUuid(STORE_UUID)).thenReturn(null);
        Integer nfsVersion = imageStoreDetailsUtil.getNfsVersionByUuid(STORE_UUID);
        assertNull(nfsVersion);
    }
}
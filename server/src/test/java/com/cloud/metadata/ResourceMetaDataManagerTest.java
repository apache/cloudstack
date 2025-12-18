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

package com.cloud.metadata;

import com.cloud.exception.ResourceAllocationException;
import com.cloud.server.ResourceManagerUtil;
import com.cloud.server.ResourceTag;
import com.cloud.server.TaggedResourceService;
import com.cloud.storage.dao.VolumeDetailsDao;
import com.cloud.vm.dao.NicDetailsDao;
import org.apache.commons.collections.map.HashedMap;
import org.junit.After;
import org.junit.Before;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import javax.naming.ConfigurationException;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;

public class ResourceMetaDataManagerTest {

    @Spy
    ResourceMetaDataManagerImpl _resourceMetaDataMgr = new ResourceMetaDataManagerImpl();
    @Mock
    VolumeDetailsDao _volumeDetailDao;
    @Mock
    NicDetailsDao _nicDetailDao;
    @Mock
    TaggedResourceService _taggedResourceMgr;
    @Mock
    ResourceManagerUtil resourceManagerUtil;
    private AutoCloseable closeable;

    @Before
    public void setup() {
        closeable = MockitoAnnotations.openMocks(this);

        try {
            _resourceMetaDataMgr.configure(null, null);
        } catch (ConfigurationException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        _resourceMetaDataMgr._volumeDetailDao = _volumeDetailDao;
        _resourceMetaDataMgr._taggedResourceMgr = _taggedResourceMgr;
        _resourceMetaDataMgr._nicDetailDao = _nicDetailDao;

    }

    @After
    public void tearDown() throws Exception {
        closeable.close();
    }

    // Test removing details
    //@Test
    public void testResourceDetails() throws ResourceAllocationException {

        //when(_resourceMetaDataMgr.getResourceId(anyString(), eq(ResourceTag.TaggedResourceType.Volume))).thenReturn(1L);
        doReturn(1L).when(resourceManagerUtil).getResourceId(anyString(), eq(ResourceTag.ResourceObjectType.Volume));
        //           _volumeDetailDao.removeDetails(id, key);

        doNothing().when(_volumeDetailDao).removeDetail(anyLong(), anyString());
        doNothing().when(_nicDetailDao).removeDetail(anyLong(), anyString());
        _resourceMetaDataMgr.deleteResourceMetaData(anyString(), eq(ResourceTag.ResourceObjectType.Volume), anyString());

    }

    // Test adding details
    public void testAddResourceDetails() throws ResourceAllocationException {

        doReturn(1L).when(resourceManagerUtil).getResourceId("1", ResourceTag.ResourceObjectType.Volume);
        //           _volumeDetailDao.removeDetails(id, key);

        doNothing().when(_volumeDetailDao).removeDetail(anyLong(), anyString());
        doNothing().when(_nicDetailDao).removeDetail(anyLong(), anyString());
        Map<String, String> map = new HashedMap();
        map.put("key", "value");
        _resourceMetaDataMgr.addResourceMetaData("1", ResourceTag.ResourceObjectType.Volume, map, true);

    }

}

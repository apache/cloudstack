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

import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyFloat;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import com.cloud.server.TaggedResourceService;
import com.cloud.utils.db.DB;
import com.cloud.vm.dao.NicDetailDao;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.command.user.vm.RestoreVMCmd;
import org.apache.cloudstack.api.command.user.vm.ScaleVMCmd;
import org.apache.commons.collections.map.HashedMap;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import com.cloud.exception.ResourceAllocationException;
import com.cloud.metadata.ResourceMetaDataManager;
import com.cloud.metadata.ResourceMetaDataManagerImpl;
import com.cloud.server.ResourceTag;
import com.cloud.storage.Volume;
import com.cloud.storage.dao.VolumeDetailsDao;
import com.cloud.user.dao.UserDao;

import javax.naming.ConfigurationException;


public class ResourceMetaDataManagerTest {



    @Spy ResourceMetaDataManagerImpl _resourceMetaDataMgr = new ResourceMetaDataManagerImpl();
    @Mock VolumeDetailsDao _volumeDetailDao;
    @Mock
    NicDetailDao _nicDetailDao;
    @Mock TaggedResourceService _taggedResourceMgr;

    @Before
    public void setup(){
        MockitoAnnotations.initMocks(this);

        try {
            _resourceMetaDataMgr.configure(null,null);
        } catch (ConfigurationException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        _resourceMetaDataMgr._volumeDetailDao = _volumeDetailDao;
        _resourceMetaDataMgr._taggedResourceMgr = _taggedResourceMgr;
        _resourceMetaDataMgr._nicDetailDao = _nicDetailDao;


    }


    // Test removing details
    //@Test
    public void testResourceDetails() throws ResourceAllocationException {


        //when(_resourceMetaDataMgr.getResourceId(anyString(), eq(ResourceTag.TaggedResourceType.Volume))).thenReturn(1L);
        doReturn(1L).when(_taggedResourceMgr).getResourceId(anyString(), eq(ResourceTag.TaggedResourceType.Volume));
        //           _volumeDetailDao.removeDetails(id, key);

        doNothing().when(_volumeDetailDao).removeDetails(anyLong(), anyString());
        doNothing().when(_nicDetailDao).removeDetails(anyLong(), anyString());
        _resourceMetaDataMgr.deleteResourceMetaData(anyString(), eq(ResourceTag.TaggedResourceType.Volume), anyString());

    }


    // Test adding details
    public void testAddResourceDetails() throws ResourceAllocationException {



        doReturn(1L).when(_taggedResourceMgr).getResourceId("1", ResourceTag.TaggedResourceType.Volume);
        //           _volumeDetailDao.removeDetails(id, key);

        doNothing().when(_volumeDetailDao).removeDetails(anyLong(), anyString());
        doNothing().when(_nicDetailDao).removeDetails(anyLong(), anyString());
        Map<String, String> map = new HashedMap();
        map.put("key","value");
        _resourceMetaDataMgr.addResourceMetaData("1", ResourceTag.TaggedResourceType.Volume, map);

    }

}

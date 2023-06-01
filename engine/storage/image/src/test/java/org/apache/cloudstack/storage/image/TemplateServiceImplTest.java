/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cloudstack.storage.image;

import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreVO;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.storage.DataStoreRole;
import com.cloud.storage.Storage;
import com.cloud.storage.VMTemplateVO;

@RunWith(MockitoJUnitRunner.class)
public class TemplateServiceImplTest {

    @InjectMocks
    @Spy
    TemplateServiceImpl templateService;

    @Mock
    TemplateDataStoreDao templateDataStoreDao;

    @Test
    public void testIsSkipTemplateStoreDownloadPublicTemplate() {
        VMTemplateVO templateVO = Mockito.mock(VMTemplateVO.class);
        Mockito.when(templateVO.isPublicTemplate()).thenReturn(true);
        Assert.assertFalse(templateService.isSkipTemplateStoreDownload(templateVO, 1L));
    }

    @Test
    public void testIsSkipTemplateStoreDownloadFeaturedTemplate() {
        VMTemplateVO templateVO = Mockito.mock(VMTemplateVO.class);
        Mockito.when(templateVO.isFeatured()).thenReturn(true);
        Assert.assertFalse(templateService.isSkipTemplateStoreDownload(templateVO, 1L));
    }

    @Test
    public void testIsSkipTemplateStoreDownloadSystemTemplate() {
        VMTemplateVO templateVO = Mockito.mock(VMTemplateVO.class);
        Mockito.when(templateVO.getTemplateType()).thenReturn(Storage.TemplateType.SYSTEM);
        Assert.assertFalse(templateService.isSkipTemplateStoreDownload(templateVO, 1L));
    }

    @Test
    public void testIsSkipTemplateStoreDownloadPrivateNoRefTemplate() {
        VMTemplateVO templateVO = Mockito.mock(VMTemplateVO.class);
        long id = 1L;
        Mockito.when(templateVO.getId()).thenReturn(id);
        Mockito.when(templateDataStoreDao.findByTemplateZone(id, id, DataStoreRole.Image)).thenReturn(null);
        Assert.assertFalse(templateService.isSkipTemplateStoreDownload(templateVO, id));
    }

    @Test
    public void testIsSkipTemplateStoreDownloadPrivateExistingTemplate() {
        VMTemplateVO templateVO = Mockito.mock(VMTemplateVO.class);
        long id = 1L;
        Mockito.when(templateVO.getId()).thenReturn(id);
        Mockito.when(templateDataStoreDao.findByTemplateZone(id, id, DataStoreRole.Image)).thenReturn(Mockito.mock(TemplateDataStoreVO.class));
        Assert.assertTrue(templateService.isSkipTemplateStoreDownload(templateVO, id));
    }
}
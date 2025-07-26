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

import com.cloud.template.TemplateManager;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.ZoneScope;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreVO;
import org.junit.Assert;
import org.junit.Before;
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

    @Mock
    DataStore dataStoreMock;

    @Mock
    TemplateManager templateManagerMock;

    @Mock
    VMTemplateVO templateVoMock;

    @Mock
    ZoneScope zoneScopeMock;

    @Before
    public void setUp() {
        Mockito.doReturn(3L).when(dataStoreMock).getId();
        Mockito.doReturn(4L).when(templateVoMock).getId();
        Mockito.doReturn(zoneScopeMock).when(dataStoreMock).getScope();
    }

    @Test
    public void shouldDownloadTemplateToStoreTestSkipsTemplateDirectedToAnotherStorage() {
        DataStore destinedStore = Mockito.mock(DataStore.class);
        Mockito.doReturn(dataStoreMock.getId() + 1L).when(destinedStore).getId();
        Mockito.when(templateManagerMock.verifyHeuristicRulesForZone(templateVoMock, zoneScopeMock.getScopeId())).thenReturn(destinedStore);
        Assert.assertFalse(templateService.shouldDownloadTemplateToStore(templateVoMock, dataStoreMock));
    }

    @Test
    public void shouldDownloadTemplateToStoreTestDownloadsPublicTemplate() {
        Mockito.when(templateVoMock.isPublicTemplate()).thenReturn(true);
        Assert.assertTrue(templateService.shouldDownloadTemplateToStore(templateVoMock, dataStoreMock));
    }

    @Test
    public void shouldDownloadTemplateToStoreTestDownloadsFeaturedTemplate() {
        Mockito.when(templateVoMock.isFeatured()).thenReturn(true);
        Assert.assertTrue(templateService.shouldDownloadTemplateToStore(templateVoMock, dataStoreMock));
    }

    @Test
    public void shouldDownloadTemplateToStoreTestDownloadsSystemTemplate() {
        Mockito.when(templateVoMock.getTemplateType()).thenReturn(Storage.TemplateType.SYSTEM);
        Assert.assertTrue(templateService.shouldDownloadTemplateToStore(templateVoMock, dataStoreMock));
    }

    @Test
    public void shouldDownloadTemplateToStoreTestDownloadsPrivateNoRefTemplate() {
        Assert.assertTrue(templateService.shouldDownloadTemplateToStore(templateVoMock, dataStoreMock));
    }

    @Test
    public void shouldDownloadTemplateToStoreTestSkipsPrivateExistingTemplate() {
        Mockito.when(templateDataStoreDao.findByTemplateZone(templateVoMock.getId(), zoneScopeMock.getScopeId(), DataStoreRole.Image)).thenReturn(Mockito.mock(TemplateDataStoreVO.class));
        Assert.assertFalse(templateService.shouldDownloadTemplateToStore(templateVoMock, dataStoreMock));
    }
}

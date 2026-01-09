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

import com.cloud.storage.template.TemplateProp;
import com.cloud.template.TemplateManager;
import org.apache.cloudstack.engine.orchestration.service.StorageOrchestrationService;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.Scope;
import org.apache.cloudstack.framework.async.AsyncCallFuture;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreVO;
import org.apache.cloudstack.storage.image.store.TemplateObject;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(MockitoJUnitRunner.class)
public class TemplateServiceImplTest {

    @InjectMocks
    @Spy
    TemplateServiceImpl templateService;

    @Mock
    TemplateDataStoreDao templateDataStoreDao;

    @Mock
    TemplateDataFactoryImpl templateDataFactoryMock;

    @Mock
    DataStoreManager dataStoreManagerMock;

    @Mock
    VMTemplateVO tmpltMock;

    @Mock
    TemplateProp tmpltPropMock;

    @Mock
    TemplateObject templateInfoMock;

    @Mock
    DataStore dataStoreMock;

    @Mock
    DataStore sourceStoreMock;

    @Mock
    DataStore destStoreMock;

    @Mock
    Scope zoneScopeMock;

    @Mock
    StorageOrchestrationService storageOrchestrator;

    @Mock
    TemplateManager templateManagerMock;

    Map<String, TemplateProp> templatesInSourceStore = new HashMap<>();

    @Before
    public void setUp() {
        Long zoneId = 1L;
        Mockito.doReturn(2L).when(tmpltMock).getId();
        Mockito.doReturn("unique-name").when(tmpltMock).getUniqueName();
        Mockito.doReturn(zoneId).when(zoneScopeMock).getScopeId();
        Mockito.doReturn(zoneScopeMock).when(destStoreMock).getScope();
        Mockito.doReturn(List.of(sourceStoreMock, destStoreMock)).when(dataStoreManagerMock).getImageStoresByZoneIds(zoneId);
        Mockito.doReturn(templatesInSourceStore).when(templateService).listTemplate(sourceStoreMock);
        Mockito.doReturn(null).when(templateService).listTemplate(destStoreMock);
        Mockito.doReturn("install-path").when(templateInfoMock).getInstallPath();
        Mockito.doReturn(templateInfoMock).when(templateDataFactoryMock).getTemplate(2L, sourceStoreMock);
        Mockito.doReturn(3L).when(dataStoreMock).getId();
        Mockito.doReturn(zoneScopeMock).when(dataStoreMock).getScope();
    }

    @Test
    public void shouldDownloadTemplateToStoreTestSkipsTemplateDirectedToAnotherStorage() {
        DataStore destinedStore = Mockito.mock(DataStore.class);
        Mockito.doReturn(dataStoreMock.getId() + 1L).when(destinedStore).getId();
        Mockito.when(templateManagerMock.verifyHeuristicRulesForZone(tmpltMock, zoneScopeMock.getScopeId())).thenReturn(destinedStore);
        Assert.assertFalse(templateService.shouldDownloadTemplateToStore(tmpltMock, dataStoreMock));
    }

    @Test
    public void shouldDownloadTemplateToStoreTestDownloadsPublicTemplate() {
        Mockito.when(tmpltMock.isPublicTemplate()).thenReturn(true);
        Assert.assertTrue(templateService.shouldDownloadTemplateToStore(tmpltMock, dataStoreMock));
    }

    @Test
    public void shouldDownloadTemplateToStoreTestDownloadsFeaturedTemplate() {
        Mockito.when(tmpltMock.isFeatured()).thenReturn(true);
        Assert.assertTrue(templateService.shouldDownloadTemplateToStore(tmpltMock, dataStoreMock));
    }

    @Test
    public void shouldDownloadTemplateToStoreTestDownloadsSystemTemplate() {
        Mockito.when(tmpltMock.getTemplateType()).thenReturn(Storage.TemplateType.SYSTEM);
        Assert.assertTrue(templateService.shouldDownloadTemplateToStore(tmpltMock, dataStoreMock));
    }

    @Test
    public void shouldDownloadTemplateToStoreTestDownloadsPrivateNoRefTemplate() {
        Assert.assertTrue(templateService.shouldDownloadTemplateToStore(tmpltMock, dataStoreMock));
    }

    @Test
    public void shouldDownloadTemplateToStoreTestSkipsPrivateExistingTemplate() {
        Mockito.when(templateDataStoreDao.findByTemplateZone(tmpltMock.getId(), zoneScopeMock.getScopeId(), DataStoreRole.Image)).thenReturn(Mockito.mock(TemplateDataStoreVO.class));
        Assert.assertFalse(templateService.shouldDownloadTemplateToStore(tmpltMock, dataStoreMock));
    }

    @Test
    public void tryDownloadingTemplateToImageStoreTestDownloadsTemplateWhenUrlIsNotNull() {
        Mockito.doReturn("url").when(tmpltMock).getUrl();
        Mockito.doNothing().when(templateService).associateTemplateToZone(Mockito.anyLong(), Mockito.any(Long.class));

        templateService.tryDownloadingTemplateToImageStore(tmpltMock, destStoreMock);

        Mockito.verify(templateService).createTemplateAsync(Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    public void tryDownloadingTemplateToImageStoreTestDoesNothingWhenUrlIsNull() {
        templateService.tryDownloadingTemplateToImageStore(tmpltMock, destStoreMock);

        Mockito.verify(templateService, Mockito.never()).createTemplateAsync(Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    public void tryCopyingTemplateToImageStoreTestReturnsFalseWhenTemplateDoesNotExistOnAnotherImageStore() {
        boolean result = templateService.tryCopyingTemplateToImageStore(tmpltMock, destStoreMock);

        Assert.assertFalse(result);
        Mockito.verify(storageOrchestrator, Mockito.never()).orchestrateTemplateCopyToImageStore(Mockito.any(), Mockito.any());
    }

    @Test
    public void tryCopyingTemplateToImageStoreTestReturnsFalseWhenInstallPathIsNull() {
        templatesInSourceStore.put(tmpltMock.getUniqueName(), tmpltPropMock);
        Mockito.doReturn(null).when(templateInfoMock).getInstallPath();

        boolean result = templateService.tryCopyingTemplateToImageStore(tmpltMock, destStoreMock);

        Assert.assertFalse(result);
        Mockito.verify(storageOrchestrator, Mockito.never()).orchestrateTemplateCopyToImageStore(Mockito.any(), Mockito.any());
    }

    @Test
    public void tryCopyingTemplateToImageStoreTestReturnsTrueWhenTemplateExistsInAnotherStorageAndTaskWasScheduled() {
        templatesInSourceStore.put(tmpltMock.getUniqueName(), tmpltPropMock);
        Mockito.doReturn(new AsyncCallFuture<>()).when(storageOrchestrator).orchestrateTemplateCopyToImageStore(Mockito.any(), Mockito.any());

        boolean result = templateService.tryCopyingTemplateToImageStore(tmpltMock, destStoreMock);

        Assert.assertTrue(result);
        Mockito.verify(storageOrchestrator).orchestrateTemplateCopyToImageStore(Mockito.any(), Mockito.any());
    }
}

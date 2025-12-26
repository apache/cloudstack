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

import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.StorageUnavailableException;
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

    @Mock
    DataCenterDao _dcDao;

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

        Scope scopeMock = Mockito.mock(Scope.class);
        Mockito.doReturn(scopeMock).when(destStoreMock).getScope();
        Mockito.doReturn(1L).when(scopeMock).getScopeId();
        Mockito.doReturn(List.of(1L)).when(_dcDao).listAllIds();

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

    @Test
    public void tryCopyingTemplateToImageStoreTestReturnsTrueWhenTemplateExistsInAnotherZone() {
        Scope scopeMock = Mockito.mock(Scope.class);
        Mockito.doReturn(scopeMock).when(destStoreMock).getScope();
        Mockito.doReturn(1L).when(scopeMock).getScopeId();
        Mockito.doReturn(100L).when(tmpltMock).getId();
        Mockito.doReturn(List.of(sourceStoreMock)).when(dataStoreManagerMock).getImageStoresByZoneIds(1L);
        Mockito.doReturn(null).when(templateService).listTemplate(sourceStoreMock);
        Mockito.doReturn(List.of(1L, 2L)).when(_dcDao).listAllIds();

        DataStore otherZoneStoreMock = Mockito.mock(DataStore.class);
        Mockito.doReturn(List.of(otherZoneStoreMock)).when(dataStoreManagerMock).getImageStoresByZoneIds(2L);

        Map<String, TemplateProp> templatesInOtherZone = new HashMap<>();
        templatesInOtherZone.put(tmpltMock.getUniqueName(), tmpltPropMock);
        Mockito.doReturn(templatesInOtherZone).when(templateService).listTemplate(otherZoneStoreMock);

        TemplateObject sourceTmplMock = Mockito.mock(TemplateObject.class);
        Mockito.doReturn(sourceTmplMock).when(templateDataFactoryMock).getTemplate(100L, otherZoneStoreMock);
        Mockito.doReturn("/mnt/secondary/template.qcow2").when(sourceTmplMock).getInstallPath();

        DataCenterVO dstZoneMock = Mockito.mock(DataCenterVO.class);
        Mockito.doReturn(dstZoneMock).when(_dcDao).findById(1L);

        boolean result = templateService.tryCopyingTemplateToImageStore(tmpltMock, destStoreMock);

        Assert.assertTrue(result);
    }

    @Test
    public void tryCopyingTemplateToImageStoreTestReturnsFalseWhenDestinationZoneIsMissing() {
        Scope scopeMock = Mockito.mock(Scope.class);
        Mockito.doReturn(scopeMock).when(destStoreMock).getScope();
        Mockito.doReturn(1L).when(scopeMock).getScopeId();
        Mockito.doReturn(100L).when(tmpltMock).getId();
        Mockito.doReturn(List.of(1L, 2L)).when(_dcDao).listAllIds();
        Mockito.doReturn(List.of()).when(dataStoreManagerMock).getImageStoresByZoneIds(1L);

        DataStore otherZoneStoreMock = Mockito.mock(DataStore.class);
        Mockito.doReturn(List.of(otherZoneStoreMock)).when(dataStoreManagerMock).getImageStoresByZoneIds(2L);

        Map<String, TemplateProp> templates = new HashMap<>();
        templates.put(tmpltMock.getUniqueName(), tmpltPropMock);
        Mockito.doReturn(templates).when(templateService).listTemplate(otherZoneStoreMock);

        TemplateObject sourceTmplMock = Mockito.mock(TemplateObject.class);
        Mockito.doReturn(sourceTmplMock).when(templateDataFactoryMock).getTemplate(100L, otherZoneStoreMock);
        Mockito.doReturn("/mnt/secondary/template.qcow2").when(sourceTmplMock).getInstallPath();
        Mockito.doReturn(null).when(_dcDao).findById(1L);

        boolean result = templateService.tryCopyingTemplateToImageStore(tmpltMock, destStoreMock);

        Assert.assertFalse(result);
    }

    @Test
    public void tryCopyingTemplateToImageStoreTestReturnsTrueWhenCrossZoneCopyTaskIsScheduled() {
        Scope scopeMock = Mockito.mock(Scope.class);
        Mockito.doReturn(scopeMock).when(destStoreMock).getScope();
        Mockito.doReturn(1L).when(scopeMock).getScopeId();
        Mockito.doReturn(100L).when(tmpltMock).getId();
        Mockito.doReturn(List.of(1L, 2L)).when(_dcDao).listAllIds();
        Mockito.doReturn(List.of()).when(dataStoreManagerMock).getImageStoresByZoneIds(1L);

        DataStore otherZoneStoreMock = Mockito.mock(DataStore.class);
        Mockito.doReturn(List.of(otherZoneStoreMock)).when(dataStoreManagerMock).getImageStoresByZoneIds(2L);

        Map<String, TemplateProp> templates = new HashMap<>();
        templates.put(tmpltMock.getUniqueName(), tmpltPropMock);
        Mockito.doReturn(templates).when(templateService).listTemplate(otherZoneStoreMock);

        TemplateObject sourceTmplMock = Mockito.mock(TemplateObject.class);
        Mockito.doReturn(sourceTmplMock).when(templateDataFactoryMock).getTemplate(100L, otherZoneStoreMock);
        Mockito.doReturn("/mnt/secondary/template.qcow2").when(sourceTmplMock).getInstallPath();

        DataCenterVO dstZoneMock = Mockito.mock(DataCenterVO.class);
        Mockito.doReturn(dstZoneMock).when(_dcDao).findById(1L);

        boolean result = templateService.tryCopyingTemplateToImageStore(tmpltMock, destStoreMock);

        Assert.assertTrue(result);
    }

    @Test
    public void tryCopyingTemplateToImageStoreTestReturnsFalseWhenTemplateNotFoundInAnyZone() {
        Scope scopeMock = Mockito.mock(Scope.class);
        Mockito.doReturn(scopeMock).when(destStoreMock).getScope();
        Mockito.doReturn(1L).when(scopeMock).getScopeId();
        Mockito.doReturn(List.of(1L, 2L)).when(_dcDao).listAllIds();
        Mockito.doReturn(List.of(sourceStoreMock)).when(dataStoreManagerMock).getImageStoresByZoneIds(Mockito.anyLong());
        Mockito.doReturn(null).when(templateService).listTemplate(Mockito.any());

        boolean result = templateService.tryCopyingTemplateToImageStore(tmpltMock, destStoreMock);

        Assert.assertFalse(result);
    }
}

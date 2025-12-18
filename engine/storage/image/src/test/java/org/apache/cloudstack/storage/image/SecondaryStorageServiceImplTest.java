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
package org.apache.cloudstack.storage.image;

import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.storage.VMTemplateStorageResourceAssoc;
import com.cloud.utils.exception.CloudRuntimeException;
import junit.framework.TestCase;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine;
import org.apache.cloudstack.engine.subsystem.api.storage.TemplateInfo;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreVO;
import org.apache.cloudstack.storage.to.TemplateObjectTO;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SecondaryStorageServiceImplTest extends TestCase {

    @Spy
    @InjectMocks
    private SecondaryStorageServiceImpl secondaryStorageService;

    @Mock
    TemplateDataStoreDao templateDataStoreDaoMock;

    @Mock
    TemplateDataStoreVO templateDataStoreVoMock;

    @Mock
    TemplateInfo templateInfoMock;

    @Mock
    TemplateObjectTO templateObjectToMock;

    @Mock
    DataStore dataStoreMock;

    @Mock
    DataStoreTO dataStoreToMock;

    private void prepareForTemplateIsOnDestinationTests() {
        long dataStoreId = 1;
        long templateId = 2;

        Mockito.when(dataStoreMock.getId()).thenReturn(dataStoreId);
        Mockito.when(dataStoreMock.getTO()).thenReturn(dataStoreToMock);
        Mockito.when(templateInfoMock.getId()).thenReturn(templateId);
        Mockito.when(templateInfoMock.getTO()).thenReturn(templateObjectToMock);
        Mockito.doReturn(templateDataStoreVoMock).when(templateDataStoreDaoMock).findByStoreTemplate(dataStoreId, templateId);
        Mockito.when(templateDataStoreVoMock.getState()).thenReturn(ObjectInDataStoreStateMachine.State.Ready);
    }

    @Test
    public void templateIsOnDestinationTestReturnsFalseWhenTemplateStoreRefDoesNotExist() {
        prepareForTemplateIsOnDestinationTests();
        Mockito.doReturn(null).when(templateDataStoreDaoMock).findByStoreTemplate(Mockito.anyLong(), Mockito.anyLong());

        boolean result = secondaryStorageService.templateIsOnDestination(templateInfoMock, dataStoreMock);

        Assert.assertFalse(result);
    }

    @Test
    public void templateIsOnDestinationTestReturnsTrueWhenTemplateIsReady() {
        prepareForTemplateIsOnDestinationTests();

        boolean result = secondaryStorageService.templateIsOnDestination(templateInfoMock, dataStoreMock);

        Assert.assertTrue(result);
    }

    @Test
    public void templateIsOnDestinationTestReturnsFalseWhenTemplateIsNotReady() {
        prepareForTemplateIsOnDestinationTests();
        Mockito.when(templateDataStoreVoMock.getState()).thenReturn(ObjectInDataStoreStateMachine.State.Creating);

        boolean result = secondaryStorageService.templateIsOnDestination(templateInfoMock, dataStoreMock);

        Assert.assertFalse(result);
    }

    @Test
    public void templateIsOnDestinationTestReturnsTrueIfTemplateIsDownloadedSuccessfully() {
        prepareForTemplateIsOnDestinationTests();
        Mockito.when(templateDataStoreVoMock.getDownloadState()).thenReturn(VMTemplateStorageResourceAssoc.Status.DOWNLOAD_IN_PROGRESS);
        Mockito.doAnswer(I -> Mockito.when(templateDataStoreVoMock.getDownloadState()).thenReturn(VMTemplateStorageResourceAssoc.Status.DOWNLOADED)).when(secondaryStorageService).waitForTemplateDownload(Mockito.anyLong(), Mockito.anyString(), Mockito.anyString());

        boolean result = secondaryStorageService.templateIsOnDestination(templateInfoMock, dataStoreMock);

        Assert.assertTrue(result);
    }

    @Test
    public void templateIsOnDestinationTestReturnsFalseIfTemplateIsNotDownloadedSuccessfully() {
        prepareForTemplateIsOnDestinationTests();
        Mockito.when(templateDataStoreVoMock.getDownloadState()).thenReturn(VMTemplateStorageResourceAssoc.Status.DOWNLOAD_IN_PROGRESS);
        Mockito.doAnswer(I -> {
            Mockito.when(templateDataStoreVoMock.getDownloadState()).thenReturn(VMTemplateStorageResourceAssoc.Status.DOWNLOAD_ERROR);
            Mockito.when(templateDataStoreVoMock.getState()).thenReturn(ObjectInDataStoreStateMachine.State.Failed);
            return "mocked download fail";
        }).when(secondaryStorageService).waitForTemplateDownload(Mockito.anyLong(), Mockito.anyString(), Mockito.anyString());

        boolean result = secondaryStorageService.templateIsOnDestination(templateInfoMock, dataStoreMock);

        Assert.assertFalse(result);
    }

    @Test(expected = CloudRuntimeException.class)
    public void templateIsOnDestinationTestThrowsExceptionIfDownloadTimesOut() {
        prepareForTemplateIsOnDestinationTests();
        Mockito.when(templateDataStoreVoMock.getDownloadState()).thenReturn(VMTemplateStorageResourceAssoc.Status.DOWNLOAD_IN_PROGRESS);
        Mockito.doReturn(0L).when(secondaryStorageService).getTemplateDownloadTimeout();

        secondaryStorageService.templateIsOnDestination(templateInfoMock, dataStoreMock);
    }
}

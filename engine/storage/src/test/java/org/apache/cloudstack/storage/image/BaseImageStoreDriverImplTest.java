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

import java.util.Collections;

import org.apache.cloudstack.engine.subsystem.api.storage.CreateCmdResult;
import org.apache.cloudstack.engine.subsystem.api.storage.DataObject;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.framework.async.AsyncCallbackDispatcher;
import org.apache.cloudstack.framework.async.AsyncCompletionCallback;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreVO;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloud.agent.api.storage.DownloadAnswer;
import com.cloud.alert.AlertManager;
import com.cloud.storage.VMTemplateStorageResourceAssoc.Status;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.VMTemplateZoneVO;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VMTemplateZoneDao;

@RunWith(MockitoJUnitRunner.Silent.class)
public class BaseImageStoreDriverImplTest {

    @Mock
    private VMTemplateDao templateDao;
    @Mock
    private TemplateDataStoreDao templateStoreDao;
    @Mock
    private VMTemplateZoneDao vmTemplateZoneDao;
    @Mock
    private AlertManager alertManager;
    @Mock
    private DataObject data;
    @Mock
    private DataStore store;
    @Mock
    private AsyncCompletionCallback<CreateCmdResult> parentCallback;
    @Mock
    private AsyncCallbackDispatcher<BaseImageStoreDriverImpl, DownloadAnswer> callback;
    @Mock
    private TemplateDataStoreVO existingRef;
    @Mock
    private VMTemplateZoneVO templateZone;

    private BaseImageStoreDriverImpl driver;
    private TemplateDataStoreVO updateBuilder;

    @Before
    public void setUp() {
        driver = Mockito.mock(BaseImageStoreDriverImpl.class, Mockito.CALLS_REAL_METHODS);
        ReflectionTestUtils.setField(driver, "logger", org.apache.logging.log4j.LogManager.getLogger(BaseImageStoreDriverImpl.class));
        ReflectionTestUtils.setField(driver, "_templateDao", templateDao);
        ReflectionTestUtils.setField(driver, "_templateStoreDao", templateStoreDao);
        ReflectionTestUtils.setField(driver, "_vmTemplateZoneDao", vmTemplateZoneDao);
        ReflectionTestUtils.setField(driver, "_alertMgr", alertManager);

        Mockito.when(data.getId()).thenReturn(7L);
        Mockito.when(data.getDataStore()).thenReturn(store);
        Mockito.when(store.getId()).thenReturn(1L);
        Mockito.when(templateDao.findById(7L)).thenReturn(Mockito.mock(VMTemplateVO.class));
        Mockito.when(templateDao.createForUpdate()).thenReturn(Mockito.mock(VMTemplateVO.class));
        Mockito.when(existingRef.getId()).thenReturn(3L);
        Mockito.when(existingRef.getDownloadState()).thenReturn(Status.DOWNLOAD_IN_PROGRESS);
        Mockito.when(templateStoreDao.findByStoreTemplate(1L, 7L)).thenReturn(existingRef);
        updateBuilder = Mockito.mock(TemplateDataStoreVO.class);
        Mockito.when(templateStoreDao.createForUpdate()).thenReturn(updateBuilder);
        Mockito.when(templateZone.getZoneId()).thenReturn(2L);
        Mockito.when(vmTemplateZoneDao.listByTemplateId(7L)).thenReturn(Collections.singletonList(templateZone));
    }

    private void callWith(DownloadAnswer answer) {
        Mockito.when(callback.getResult()).thenReturn(answer);
        BaseImageStoreDriverImpl.CreateContext<CreateCmdResult> context = driver.new CreateContext<>(parentCallback, data);
        driver.createTemplateAsyncCallback(callback, context);
    }

    @Test
    public void testErrorAnswerWithoutInstallPathKeepsInstallPath() {
        DownloadAnswer answer = new DownloadAnswer("Timeout waiting for response from storage host", Status.DOWNLOAD_ERROR);

        callWith(answer);

        Mockito.verify(updateBuilder, Mockito.never()).setInstallPath(Mockito.any());
        Mockito.verify(updateBuilder).setDownloadState(Status.DOWNLOAD_ERROR);
        Mockito.verify(templateStoreDao).update(3L, updateBuilder);
    }

    @Test
    public void testAnswerWithInstallPathSetsInstallPath() {
        DownloadAnswer answer = new DownloadAnswer("job", 100, null, Status.DOWNLOADED, "/dl/path", "template/tmpl/2/7", 10L, 10L, "checksum");

        callWith(answer);

        Mockito.verify(updateBuilder).setInstallPath("template/tmpl/2/7");
        Mockito.verify(templateStoreDao).update(3L, updateBuilder);
    }
}

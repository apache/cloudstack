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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.apache.cloudstack.engine.subsystem.api.storage.CreateCmdResult;
import org.apache.cloudstack.engine.subsystem.api.storage.DataObject;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPoint;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPointSelector;
import org.apache.cloudstack.framework.async.AsyncCallbackDispatcher;
import org.apache.cloudstack.framework.async.AsyncCompletionCallback;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.storage.command.CommandResult;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.VolumeDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.VolumeDataStoreVO;
import org.apache.cloudstack.storage.endpoint.DefaultEndPointSelector;
import org.apache.cloudstack.storage.image.deployasis.DeployAsIsHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.storage.DownloadAnswer;
import com.cloud.agent.api.storage.GetDatadisksAnswer;
import com.cloud.agent.api.to.DataObjectType;
import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.agent.api.to.DataTO;
import com.cloud.agent.api.to.DatadiskTO;
import com.cloud.agent.api.to.NfsTO;
import com.cloud.alert.AlertManager;
import com.cloud.host.dao.HostDao;
import com.cloud.secstorage.CommandExecLogDao;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.VMTemplateStorageResourceAssoc;
import com.cloud.storage.VMTemplateZoneVO;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VMTemplateZoneDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.storage.download.DownloadMonitor;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.dao.SecondaryStorageVmDao;

@RunWith(MockitoJUnitRunner.class)
public class BaseImageStoreDriverImplTest {

    @Mock
    VMTemplateDao templateDao;
    @Mock
    DownloadMonitor downloadMonitor;
    @Mock
    VolumeDao volumeDao;
    @Mock
    VolumeDataStoreDao volumeStoreDao;
    @Mock
    TemplateDataStoreDao templateStoreDao;
    @Mock
    SnapshotDataStoreDao snapshotDataStoreDao;
    @Mock
    EndPointSelector epSelector;
    @Mock
    ConfigurationDao configDao;
    @Mock
    VMTemplateZoneDao vmTemplateZoneDao;
    @Mock
    AlertManager alertMgr;
    @Mock
    DefaultEndPointSelector defaultEpSelector;
    @Mock
    DeployAsIsHelper deployAsIsHelper;
    @Mock
    HostDao hostDao;
    @Mock
    CommandExecLogDao cmdExecLogDao;
    @Mock
    SecondaryStorageVmDao secStorageVmDao;
    @Mock
    AgentManager agentMgr;
    @Mock
    DataStoreManager dataStoreManager;

    @Mock
    DataObject dataObject;
    @Mock
    DataStore dataStore;

    private TestDriver driver;

    private static class TestDriver extends BaseImageStoreDriverImpl {
        @Override
        public String createEntityExtractUrl(DataStore store, String installPath, com.cloud.storage.Storage.ImageFormat format, DataObject dataObject) {
            return null;
        }

        @Override
        public com.cloud.agent.api.to.DataStoreTO getStoreTO(DataStore store) {
            return null;
        }
    }

    @Before
    public void setup() {
        driver = new TestDriver();
        ReflectionTestUtils.setField(driver, "_templateDao", templateDao);
        ReflectionTestUtils.setField(driver, "_downloadMonitor", downloadMonitor);
        ReflectionTestUtils.setField(driver, "volumeDao", volumeDao);
        ReflectionTestUtils.setField(driver, "_volumeStoreDao", volumeStoreDao);
        ReflectionTestUtils.setField(driver, "_templateStoreDao", templateStoreDao);
        ReflectionTestUtils.setField(driver, "snapshotDataStoreDao", snapshotDataStoreDao);
        ReflectionTestUtils.setField(driver, "_epSelector", epSelector);
        ReflectionTestUtils.setField(driver, "configDao", configDao);
        ReflectionTestUtils.setField(driver, "_vmTemplateZoneDao", vmTemplateZoneDao);
        ReflectionTestUtils.setField(driver, "_alertMgr", alertMgr);
        ReflectionTestUtils.setField(driver, "_defaultEpSelector", defaultEpSelector);
        ReflectionTestUtils.setField(driver, "deployAsIsHelper", deployAsIsHelper);
        ReflectionTestUtils.setField(driver, "hostDao", hostDao);
        ReflectionTestUtils.setField(driver, "_cmdExecLogDao", cmdExecLogDao);
        ReflectionTestUtils.setField(driver, "_secStorageVmDao", secStorageVmDao);
        ReflectionTestUtils.setField(driver, "agentMgr", agentMgr);
        ReflectionTestUtils.setField(driver, "dataStoreManager", dataStoreManager);
    }

    // ---------- createTemplateAsyncCallback ----------

    @SuppressWarnings("unchecked")
    private AsyncCallbackDispatcher<BaseImageStoreDriverImpl, DownloadAnswer> dispatcherReturning(DownloadAnswer answer) {
        AsyncCallbackDispatcher<BaseImageStoreDriverImpl, DownloadAnswer> dispatcher = mock(AsyncCallbackDispatcher.class);
        when(dispatcher.getResult()).thenReturn(answer);
        return dispatcher;
    }

    @SuppressWarnings("unchecked")
    private AsyncCompletionCallback<CreateCmdResult> mockParentCallback() {
        return mock(AsyncCompletionCallback.class);
    }

    @Test
    public void createTemplateAsyncCallbackSendsAlertOnErrorDownloadState() {
        when(dataObject.getId()).thenReturn(10L);
        when(dataObject.getDataStore()).thenReturn(dataStore);
        when(dataStore.getId()).thenReturn(20L);
        when(templateStoreDao.findByStoreTemplate(20L, 10L)).thenReturn(null);

        VMTemplateZoneVO zoneVO = mock(VMTemplateZoneVO.class);
        when(zoneVO.getZoneId()).thenReturn(5L);
        when(vmTemplateZoneDao.listByTemplateId(10L)).thenReturn(Collections.singletonList(zoneVO));

        DownloadAnswer answer = new DownloadAnswer("job-1", 0, "download failed", VMTemplateStorageResourceAssoc.Status.DOWNLOAD_ERROR,
                null, null, 0L, 0L, null);
        AsyncCallbackDispatcher<BaseImageStoreDriverImpl, DownloadAnswer> dispatcher = dispatcherReturning(answer);
        AsyncCompletionCallback<CreateCmdResult> parentCallback = mockParentCallback();
        BaseImageStoreDriverImpl.CreateContext<CreateCmdResult> context = driver.new CreateContext<>(parentCallback, dataObject);

        driver.createTemplateAsyncCallback(dispatcher, context);

        ArgumentCaptor<CreateCmdResult> resultCaptor = ArgumentCaptor.forClass(CreateCmdResult.class);
        verify(parentCallback).complete(resultCaptor.capture());
        assertFalse(resultCaptor.getValue().isSuccess());
        assertEquals("download failed", resultCaptor.getValue().getResult());

        ArgumentCaptor<String> msgCaptor = ArgumentCaptor.forClass(String.class);
        verify(alertMgr).sendAlert(eq(AlertManager.AlertType.ALERT_TYPE_UPLOAD_FAILED), eq(5L), eq((Long) null), msgCaptor.capture(), msgCaptor.capture());
        assertTrue(msgCaptor.getValue().contains("id: 10"));
        assertTrue(msgCaptor.getValue().contains("Failed to register template"));
    }

    @Test
    public void createTemplateAsyncCallbackUpdatesChecksumOnDownloaded() {
        when(dataObject.getId()).thenReturn(11L);
        when(dataObject.getDataStore()).thenReturn(dataStore);
        when(dataStore.getId()).thenReturn(21L);
        when(templateStoreDao.findByStoreTemplate(21L, 11L)).thenReturn(null);
        when(templateDao.createForUpdate()).thenReturn(new com.cloud.storage.VMTemplateVO());

        DownloadAnswer answer = new DownloadAnswer("job-2", 100, null, VMTemplateStorageResourceAssoc.Status.DOWNLOADED,
                "/path", "/install", 1024L, 1024L, "abcd1234");
        AsyncCallbackDispatcher<BaseImageStoreDriverImpl, DownloadAnswer> dispatcher = dispatcherReturning(answer);
        AsyncCompletionCallback<CreateCmdResult> parentCallback = mockParentCallback();
        BaseImageStoreDriverImpl.CreateContext<CreateCmdResult> context = driver.new CreateContext<>(parentCallback, dataObject);

        driver.createTemplateAsyncCallback(dispatcher, context);

        verify(templateDao).update(eq(11L), any(com.cloud.storage.VMTemplateVO.class));
        verify(parentCallback).complete(any(CreateCmdResult.class));
        verify(alertMgr, never()).sendAlert(any(AlertManager.AlertType.class), anyLong(), any(), anyString(), anyString());
    }

    // ---------- createVolumeAsyncCallback ----------

    @Test
    public void createVolumeAsyncCallbackSendsAlertWithVolStoreZoneIdOnError() {
        when(dataObject.getId()).thenReturn(30L);
        when(dataObject.getDataStore()).thenReturn(dataStore);
        when(dataStore.getId()).thenReturn(40L);

        VolumeDataStoreVO volStoreVO = mock(VolumeDataStoreVO.class);
        when(volStoreVO.getDownloadState()).thenReturn(VMTemplateStorageResourceAssoc.Status.DOWNLOAD_IN_PROGRESS);
        when(volStoreVO.getZoneId()).thenReturn(99L);
        when(volStoreVO.getId()).thenReturn(1L);
        when(volumeStoreDao.findByStoreVolume(40L, 30L)).thenReturn(volStoreVO);
        when(volumeStoreDao.createForUpdate()).thenReturn(new VolumeDataStoreVO());

        DownloadAnswer answer = new DownloadAnswer("job-3", 0, "upload failed", VMTemplateStorageResourceAssoc.Status.DOWNLOAD_ERROR,
                null, null, 0L, 0L, null);
        AsyncCallbackDispatcher<BaseImageStoreDriverImpl, DownloadAnswer> dispatcher = dispatcherReturning(answer);
        AsyncCompletionCallback<CreateCmdResult> parentCallback = mockParentCallback();
        BaseImageStoreDriverImpl.CreateContext<CreateCmdResult> context = driver.new CreateContext<>(parentCallback, dataObject);

        driver.createVolumeAsyncCallback(dispatcher, context);

        ArgumentCaptor<String> msgCaptor = ArgumentCaptor.forClass(String.class);
        verify(alertMgr).sendAlert(eq(AlertManager.AlertType.ALERT_TYPE_UPLOAD_FAILED), eq(99L), eq((Long) null), msgCaptor.capture(), msgCaptor.capture());
        assertTrue(msgCaptor.getValue().contains("id: 30"));
        assertTrue(msgCaptor.getValue().contains("Failed to upload volume"));
    }

    @Test
    public void createVolumeAsyncCallbackSendsAlertWithNegativeOneZoneIdWhenVolStoreNull() {
        when(dataObject.getId()).thenReturn(31L);
        when(dataObject.getDataStore()).thenReturn(dataStore);
        when(dataStore.getId()).thenReturn(41L);
        when(volumeStoreDao.findByStoreVolume(41L, 31L)).thenReturn(null);

        DownloadAnswer answer = new DownloadAnswer("job-4", 0, "upload failed again", VMTemplateStorageResourceAssoc.Status.DOWNLOAD_ERROR,
                null, null, 0L, 0L, null);
        AsyncCallbackDispatcher<BaseImageStoreDriverImpl, DownloadAnswer> dispatcher = dispatcherReturning(answer);
        AsyncCompletionCallback<CreateCmdResult> parentCallback = mockParentCallback();
        BaseImageStoreDriverImpl.CreateContext<CreateCmdResult> context = driver.new CreateContext<>(parentCallback, dataObject);

        driver.createVolumeAsyncCallback(dispatcher, context);

        verify(alertMgr).sendAlert(eq(AlertManager.AlertType.ALERT_TYPE_UPLOAD_FAILED), eq(-1L), eq((Long) null), any(), any());
    }

    // ---------- createSnapshotAsyncCallback ----------

    @Test
    public void createSnapshotAsyncCallbackSendsAlertUsingDataStoreManagerZoneId() {
        when(dataObject.getId()).thenReturn(50L);
        when(dataObject.getDataStore()).thenReturn(dataStore);
        when(dataStore.getId()).thenReturn(60L);
        when(dataStore.getRole()).thenReturn(DataStoreRole.Image);
        when(snapshotDataStoreDao.findByStoreSnapshot(DataStoreRole.Image, 60L, 50L)).thenReturn(null);
        when(dataStoreManager.getStoreZoneId(60L, DataStoreRole.Image)).thenReturn(7L);

        DownloadAnswer answer = new DownloadAnswer("job-5", 0, "copy failed", VMTemplateStorageResourceAssoc.Status.DOWNLOAD_ERROR,
                null, null, 0L, 0L, null);
        AsyncCallbackDispatcher<BaseImageStoreDriverImpl, DownloadAnswer> dispatcher = dispatcherReturning(answer);
        AsyncCompletionCallback<CreateCmdResult> parentCallback = mockParentCallback();
        BaseImageStoreDriverImpl.CreateContext<CreateCmdResult> context = driver.new CreateContext<>(parentCallback, dataObject);

        driver.createSnapshotAsyncCallback(dispatcher, context);

        ArgumentCaptor<String> msgCaptor = ArgumentCaptor.forClass(String.class);
        verify(alertMgr).sendAlert(eq(AlertManager.AlertType.ALERT_TYPE_UPLOAD_FAILED), eq(7L), eq((Long) null), msgCaptor.capture(), msgCaptor.capture());
        assertTrue(msgCaptor.getValue().contains("id: 50"));
        assertTrue(msgCaptor.getValue().contains("Failed to copy snapshot"));
    }

    @Test
    public void createSnapshotAsyncCallbackCompletesOnDownloaded() {
        when(dataObject.getId()).thenReturn(51L);
        when(dataObject.getDataStore()).thenReturn(dataStore);
        when(dataStore.getId()).thenReturn(61L);
        when(snapshotDataStoreDao.findByStoreSnapshot(DataStoreRole.Image, 61L, 51L)).thenReturn(null);

        DownloadAnswer answer = new DownloadAnswer("job-6", 100, null, VMTemplateStorageResourceAssoc.Status.DOWNLOADED,
                "/path", "/install", 100L, 100L, null);
        AsyncCallbackDispatcher<BaseImageStoreDriverImpl, DownloadAnswer> dispatcher = dispatcherReturning(answer);
        AsyncCompletionCallback<CreateCmdResult> parentCallback = mockParentCallback();
        BaseImageStoreDriverImpl.CreateContext<CreateCmdResult> context = driver.new CreateContext<>(parentCallback, dataObject);

        driver.createSnapshotAsyncCallback(dispatcher, context);

        verify(parentCallback).complete(any(CreateCmdResult.class));
        verify(alertMgr, never()).sendAlert(any(AlertManager.AlertType.class), anyLong(), any(), anyString(), anyString());
    }

    // ---------- canCopy ----------

    private DataObject nfsImageDataObject(DataObjectType type) {
        DataObject obj = mock(DataObject.class);
        DataStore store = mock(DataStore.class);
        NfsTO nfsTO = mock(NfsTO.class);
        when(store.getTO()).thenReturn(nfsTO);
        when(store.getRole()).thenReturn(DataStoreRole.Image);
        when(obj.getDataStore()).thenReturn(store);
        when(obj.getType()).thenReturn(type);
        return obj;
    }

    @Test
    public void canCopyReturnsTrueForMatchingNfsImageTemplates() {
        DataObject src = nfsImageDataObject(DataObjectType.TEMPLATE);
        DataObject dest = nfsImageDataObject(DataObjectType.TEMPLATE);

        assertTrue(driver.canCopy(src, dest));
    }

    @Test
    public void canCopyReturnsFalseForNonNfsDataStoreTO() {
        DataObject src = mock(DataObject.class);
        DataObject dest = nfsImageDataObject(DataObjectType.TEMPLATE);

        DataStore srcStore = mock(DataStore.class);
        DataStoreTO nonNfsTO = mock(DataStoreTO.class);
        when(srcStore.getTO()).thenReturn(nonNfsTO);
        when(src.getDataStore()).thenReturn(srcStore);

        assertFalse(driver.canCopy(src, dest));
    }

    @Test
    public void canCopyReturnsFalseForMismatchedRole() {
        DataObject src = mock(DataObject.class);
        DataObject dest = nfsImageDataObject(DataObjectType.TEMPLATE);

        DataStore srcStore = mock(DataStore.class);
        NfsTO nfsTO = mock(NfsTO.class);
        when(srcStore.getTO()).thenReturn(nfsTO);
        when(srcStore.getRole()).thenReturn(DataStoreRole.Primary);
        when(src.getDataStore()).thenReturn(srcStore);

        assertFalse(driver.canCopy(src, dest));
    }

    @Test
    public void canCopyReturnsFalseForMismatchedType() {
        DataObject src = nfsImageDataObject(DataObjectType.TEMPLATE);
        DataObject dest = nfsImageDataObject(DataObjectType.VOLUME);

        assertFalse(driver.canCopy(src, dest));
    }

    // ---------- deleteAsync ----------

    @SuppressWarnings("unchecked")
    @Test
    public void deleteAsyncReturnsErrorWhenNoEndpoint() {
        DataTO dataTO = mock(DataTO.class);
        when(dataObject.getTO()).thenReturn(dataTO);
        when(epSelector.select(dataObject)).thenReturn(null);

        AsyncCompletionCallback<CommandResult> callback = mock(AsyncCompletionCallback.class);

        driver.deleteAsync(dataStore, dataObject, callback);

        ArgumentCaptor<CommandResult> resultCaptor = ArgumentCaptor.forClass(CommandResult.class);
        verify(callback).complete(resultCaptor.capture());
        assertNotNull(resultCaptor.getValue().getResult());
        assertTrue(resultCaptor.getValue().getResult().contains("No remote endpoint"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void deleteAsyncReturnsFailureDetailsFromAnswer() {
        DataTO dataTO = mock(DataTO.class);
        when(dataObject.getTO()).thenReturn(dataTO);
        EndPoint ep = mock(EndPoint.class);
        when(epSelector.select(dataObject)).thenReturn(ep);
        Answer answer = new Answer(null, false, "delete failed on host");
        when(ep.sendMessage(any())).thenReturn(answer);

        AsyncCompletionCallback<CommandResult> callback = mock(AsyncCompletionCallback.class);

        driver.deleteAsync(dataStore, dataObject, callback);

        ArgumentCaptor<CommandResult> resultCaptor = ArgumentCaptor.forClass(CommandResult.class);
        verify(callback).complete(resultCaptor.capture());
        assertEquals("delete failed on host", resultCaptor.getValue().getResult());
    }

    // ---------- getDataDiskTemplates ----------

    @Test
    public void getDataDiskTemplatesReturnsListOnSuccess() {
        DataTO dataTO = mock(DataTO.class);
        when(dataObject.getTO()).thenReturn(dataTO);
        when(dataObject.getDataStore()).thenReturn(dataStore);
        EndPoint ep = mock(EndPoint.class);
        when(defaultEpSelector.select(dataStore)).thenReturn(ep);

        List<DatadiskTO> disks = Collections.singletonList(mock(DatadiskTO.class));
        GetDatadisksAnswer answer = new GetDatadisksAnswer(disks);
        when(ep.sendMessage(any())).thenReturn(answer);

        List<DatadiskTO> result = driver.getDataDiskTemplates(dataObject, "cfg-1");

        assertEquals(disks, result);
    }

    @Test
    public void getDataDiskTemplatesThrowsWhenNoEndpoint() {
        DataTO dataTO = mock(DataTO.class);
        when(dataObject.getTO()).thenReturn(dataTO);
        when(dataObject.getDataStore()).thenReturn(dataStore);
        when(defaultEpSelector.select(dataStore)).thenReturn(null);

        try {
            driver.getDataDiskTemplates(dataObject, "cfg-2");
            fail("Expected CloudRuntimeException");
        } catch (CloudRuntimeException expected) {
            assertTrue(expected.getMessage().contains("Get Data disk command failed"));
        }
    }

    @Test
    public void getDataDiskTemplatesThrowsWhenAnswerResultFalse() {
        DataTO dataTO = mock(DataTO.class);
        when(dataObject.getTO()).thenReturn(dataTO);
        when(dataObject.getDataStore()).thenReturn(dataStore);
        EndPoint ep = mock(EndPoint.class);
        when(defaultEpSelector.select(dataStore)).thenReturn(ep);
        Answer answer = new Answer(null, false, "disk listing failed");
        when(ep.sendMessage(any())).thenReturn(answer);

        try {
            driver.getDataDiskTemplates(dataObject, "cfg-3");
            fail("Expected CloudRuntimeException");
        } catch (CloudRuntimeException expected) {
            assertTrue(expected.getMessage().contains("disk listing failed"));
        }
    }

    // ---------- getHttpProxy ----------

    @Test
    public void getHttpProxyReturnsNullWhenProxyNotSet() {
        Object proxy = ReflectionTestUtils.invokeMethod(driver, "getHttpProxy");
        assertEquals(null, proxy);
    }

    @Test
    public void getHttpProxyReturnsProxyWhenValidUriSet() {
        ReflectionTestUtils.setField(driver, "_proxy", "http://proxyhost:3128");
        Object proxy = ReflectionTestUtils.invokeMethod(driver, "getHttpProxy");
        assertNotNull(proxy);
    }

    @Test
    public void getHttpProxyReturnsNullWhenUriInvalid() {
        ReflectionTestUtils.setField(driver, "_proxy", "http://invalid uri with spaces");
        Object proxy = ReflectionTestUtils.invokeMethod(driver, "getHttpProxy");
        assertEquals(null, proxy);
    }
}

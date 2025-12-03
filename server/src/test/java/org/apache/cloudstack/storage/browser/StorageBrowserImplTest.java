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

package org.apache.cloudstack.storage.browser;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.UnsupportedAnswer;
import com.cloud.api.query.dao.ImageStoreJoinDao;
import com.cloud.api.query.vo.ImageStoreJoinVO;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.Storage;
import com.cloud.storage.VMTemplateStoragePoolVO;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VMTemplatePoolDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.api.command.admin.storage.DownloadImageStoreObjectCmd;
import org.apache.cloudstack.api.command.admin.storage.ListImageStoreObjectsCmd;
import org.apache.cloudstack.api.command.admin.storage.ListStoragePoolObjectsCmd;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPoint;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPointSelector;
import org.apache.cloudstack.storage.command.browser.ListDataStoreObjectsAnswer;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreVO;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreVO;
import org.apache.cloudstack.storage.datastore.db.VolumeDataStoreDao;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StorageBrowserImplTest {

    @Mock
    ImageStoreJoinDao imageStoreJoinDao;

    @Mock
    DataStoreManager dataStoreMgr;

    @Mock
    TemplateDataStoreDao templateDataStoreDao;

    @Mock
    SnapshotDataStoreDao snapshotDataStoreDao;

    @Mock
    SnapshotDao snapshotDao;

    @Mock
    EndPointSelector endPointSelector;

    @Mock
    VMTemplatePoolDao templatePoolDao;

    @Mock
    VMTemplateDao templateDao;

    @Mock
    VolumeDao volumeDao;

    @Mock
    VolumeDataStoreDao volumeDataStoreDao;

    @InjectMocks
    @Spy
    private StorageBrowserImpl storageBrowser;

    private AutoCloseable closeable;

    @Before
    public void setUp() throws Exception {
        closeable = MockitoAnnotations.openMocks(this);
    }

    @After
    public void tearDown() throws Exception {
        closeable.close();
    }

    @Test
    public void testGetPathVolumeMapForPrimaryDSNoVolumes() {
        List<String> paths = List.of("volume1", "volume2");
        Mockito.when(volumeDao.listByPoolIdAndPaths(1, paths)).thenReturn(null);
        Map<String, VolumeVO> result = storageBrowser.getPathVolumeMapForPrimaryDS(1L, paths);
        Assert.assertTrue(result.isEmpty());
    }

    @Test
    public void testGetPathVolumeMapForPrimaryDS() {
        List<String> paths = List.of("volume1", "volume2");
        VolumeVO volume = Mockito.mock(VolumeVO.class);
        Mockito.when(volume.getPath()).thenReturn("volume1");

        Mockito.when(volumeDao.listByPoolIdAndPaths(1, paths)).thenReturn(List.of(volume));
        Map<String, VolumeVO> result = storageBrowser.getPathVolumeMapForPrimaryDS(1L, paths);
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(volume, result.get("volume1"));
    }

    @Test
    public void testGetPathTemplateMapForPrimaryDSNoTemplates() {
        List<String> paths = List.of("template1", "template2");
        Mockito.when(templatePoolDao.listByPoolIdAndInstallPath(1L, paths)).thenReturn(null);
        Map<String, VMTemplateVO> result = storageBrowser.getPathTemplateMapForPrimaryDS(1L, paths);
        Assert.assertTrue(result.isEmpty());
    }

    @Test
    public void testGetPathTemplateMapForPrimaryDS() {
        List<String> paths = List.of("template1", "template2");
        VMTemplateStoragePoolVO templatePool = Mockito.mock(VMTemplateStoragePoolVO.class);
        Mockito.when(templatePool.getTemplateId()).thenReturn(5L);
        Mockito.when(templatePool.getInstallPath()).thenReturn("template1");

        VMTemplateVO template = Mockito.mock(VMTemplateVO.class);
        Mockito.when(template.getId()).thenReturn(5L);

        Mockito.when(templateDao.listByIds(List.of(5L))).thenReturn(List.of(template));

        List<VMTemplateStoragePoolVO> templateStoragePoolList = List.of(templatePool);
        Mockito.when(templatePoolDao.listByPoolIdAndInstallPath(1L, paths)).thenReturn(templateStoragePoolList);
        Map<String, VMTemplateVO> result = storageBrowser.getPathTemplateMapForPrimaryDS(1L, paths);
        Assert.assertEquals(1L, result.size());
        Assert.assertEquals(template, result.get("template1"));
    }

    @Test
    public void testGetFormattedPaths() {
        List<String> paths = List.of("/path/to/file", "/path/to/file/", "path/to/file", "path/to/file/");
        List<String> expectedFormattedPaths = List.of("path/to/file", "path/to/file", "path/to/file", "path/to/file");

        List<String> formattedPaths = storageBrowser.getFormattedPaths(paths);

        Assert.assertEquals(expectedFormattedPaths, formattedPaths);
    }

    @Test
    public void testListImageStore() {
        ListImageStoreObjectsCmd cmd = Mockito.mock(ListImageStoreObjectsCmd.class);
        Long imageStoreId = 1L;
        String path = "path/to/image/store";
        Mockito.when(cmd.getStoreId()).thenReturn(imageStoreId);
        Mockito.when(cmd.getPath()).thenReturn(path);
        Mockito.when(cmd.getStartIndex()).thenReturn(0L);
        Mockito.when(cmd.getPageSizeVal()).thenReturn(10L);

        ImageStoreJoinVO imageStore = Mockito.mock(ImageStoreJoinVO.class);
        Mockito.when(imageStoreJoinDao.findById(imageStoreId)).thenReturn(imageStore);

        DataStore dataStore = Mockito.mock(DataStore.class);
        Mockito.when(dataStoreMgr.getDataStore(imageStoreId, imageStore.getRole())).thenReturn(dataStore);

        ListDataStoreObjectsAnswer answer = Mockito.mock(ListDataStoreObjectsAnswer.class);
        Mockito.doReturn(answer).when(storageBrowser).listObjectsInStore(dataStore, path, 0, 10);

        ListResponse<DataStoreObjectResponse> response = storageBrowser.listImageStoreObjects(cmd);

        Assert.assertNotNull(response);
    }

    @Test
    public void testListPrimaryStore() {
        ListStoragePoolObjectsCmd cmd = Mockito.mock(ListStoragePoolObjectsCmd.class);
        Long storeId = 1L;
        String path = "path/to/primary/store";
        Mockito.when(cmd.getStoreId()).thenReturn(storeId);
        Mockito.when(cmd.getPath()).thenReturn(path);
        Mockito.when(cmd.getStartIndex()).thenReturn(0L);
        Mockito.when(cmd.getPageSizeVal()).thenReturn(10L);

        DataStore dataStore = Mockito.mock(DataStore.class);
        Mockito.when(dataStoreMgr.getDataStore(storeId, DataStoreRole.Primary)).thenReturn(dataStore);

        ListDataStoreObjectsAnswer answer = Mockito.mock(ListDataStoreObjectsAnswer.class);
        Mockito.doReturn(answer).when(storageBrowser).listObjectsInStore(dataStore, path, 0, 10);

        ListResponse<DataStoreObjectResponse> response = storageBrowser.listPrimaryStoreObjects(cmd);

        Assert.assertNotNull(response);
        Assert.assertEquals(answer.getPaths().size(), response.getResponses().size());
    }

    @Test
    public void testGetCommands() {
        List<Class<?>> expectedCmdList = new ArrayList<>();
        expectedCmdList.add(ListImageStoreObjectsCmd.class);
        expectedCmdList.add(ListStoragePoolObjectsCmd.class);
        expectedCmdList.add(DownloadImageStoreObjectCmd.class);

        List<Class<?>> cmdList = storageBrowser.getCommands();

        Assert.assertNotNull(cmdList);
        Assert.assertEquals(expectedCmdList, cmdList);
    }

    @Test
    public void testListObjectsInStoreNoEndpoint() {
        DataStore dataStore = Mockito.mock(DataStore.class);
        String path = "path/to/store";
        int startIndex = 0;
        int pageSize = 10;

        Mockito.when(endPointSelector.select(dataStore)).thenReturn(null);

        try {
            storageBrowser.listObjectsInStore(dataStore, path, startIndex, pageSize);
        } catch (CloudRuntimeException exception) {
            Assert.assertEquals("No remote endpoint to send command", exception.getMessage());
        }
    }

    @Test
    public void testListObjectsInStoreBadCommand() {
        DataStore dataStore = Mockito.mock(DataStore.class);
        String path = "path/to/store";
        int startIndex = 0;
        int pageSize = 10;

        EndPoint ep = Mockito.mock(EndPoint.class);

        Mockito.when(endPointSelector.select(dataStore)).thenReturn(ep);
        Answer answer = Mockito.mock(UnsupportedAnswer.class);
        Mockito.when(ep.sendMessage(Mockito.any())).thenReturn(answer);

        try {
            storageBrowser.listObjectsInStore(dataStore, path, startIndex, pageSize);
        } catch (CloudRuntimeException exception) {
            Assert.assertEquals("Failed to list datastore objects", exception.getMessage());
        }
    }

    @Test
    public void testListObjectsInStorePathDoesNotExist() {
        DataStore dataStore = Mockito.mock(DataStore.class);
        String path = "path/to/store";
        int startIndex = 0;
        int pageSize = 10;

        EndPoint ep = Mockito.mock(EndPoint.class);

        Mockito.when(endPointSelector.select(dataStore)).thenReturn(ep);
        ListDataStoreObjectsAnswer answer = Mockito.mock(ListDataStoreObjectsAnswer.class);
        Mockito.when(ep.sendMessage(Mockito.any())).thenReturn(answer);
        Mockito.when(answer.getResult()).thenReturn(true);
        Mockito.when(answer.isPathExists()).thenReturn(false);
        Mockito.when(dataStore.getUuid()).thenReturn("uuid");

        try {
            storageBrowser.listObjectsInStore(dataStore, path, startIndex, pageSize);
        } catch (IllegalArgumentException exception) {
            Assert.assertEquals("Path " + path + " doesn't exist in store: " + dataStore.getUuid(), exception.getMessage());
        }
    }

    @Test
    public void testListObjectsInStore() {
        DataStore dataStore = Mockito.mock(DataStore.class);
        String path = "path/to/store";
        int startIndex = 0;
        int pageSize = 10;

        EndPoint ep = Mockito.mock(EndPoint.class);

        Mockito.when(endPointSelector.select(dataStore)).thenReturn(ep);
        ListDataStoreObjectsAnswer answer = Mockito.mock(ListDataStoreObjectsAnswer.class);
        Mockito.when(ep.sendMessage(Mockito.any())).thenReturn(answer);
        Mockito.when(answer.getResult()).thenReturn(true);
        Mockito.when(answer.isPathExists()).thenReturn(true);

        Assert.assertEquals(answer, storageBrowser.listObjectsInStore(dataStore, path, startIndex, pageSize));
    }

    @Test
    public void testGetPathTemplateMapForSecondaryDS() {
        long dataStoreId = 1L;
        List<String> paths = List.of("/path1", "/path2");

        TemplateDataStoreVO templateDataStore1 = Mockito.mock(TemplateDataStoreVO.class);
        Mockito.when(templateDataStore1.getInstallPath()).thenReturn("/path1");
        Mockito.when(templateDataStore1.getTemplateId()).thenReturn(1L);

        TemplateDataStoreVO templateDataStore2 = Mockito.mock(TemplateDataStoreVO.class);
        Mockito.when(templateDataStore2.getInstallPath()).thenReturn("/path2");
        Mockito.when(templateDataStore2.getTemplateId()).thenReturn(2L);

        List<TemplateDataStoreVO> templateList = List.of(templateDataStore1, templateDataStore2);

        VMTemplateVO template1 = Mockito.mock(VMTemplateVO.class);
        Mockito.when(template1.getId()).thenReturn(1L);

        VMTemplateVO template2 = Mockito.mock(VMTemplateVO.class);
        Mockito.when(template2.getId()).thenReturn(2L);

        List<VMTemplateVO> templates = List.of(template1, template2);

        Mockito.when(templateDataStoreDao.listByStoreIdAndInstallPaths(dataStoreId, paths)).thenReturn(templateList);
        Mockito.when(templateDao.listByIds(List.of(1L, 2L))).thenReturn(templates);

        Map<String, VMTemplateVO> expectedPathTemplateMap = new HashMap<>();
        expectedPathTemplateMap.put("/path1", template1);
        expectedPathTemplateMap.put("/path2", template2);

        Map<String, VMTemplateVO> actualPathTemplateMap = storageBrowser.getPathTemplateMapForSecondaryDS(dataStoreId, paths);

        Assert.assertEquals(expectedPathTemplateMap, actualPathTemplateMap);
    }

    @Test
    public void testGetPathSnapshotMapForSecondaryDS() {
        long dataStoreId = 1L;
        List<String> paths = List.of("/path1", "/path2");

        SnapshotDataStoreVO snapshotDataStore1 = Mockito.mock(SnapshotDataStoreVO.class);
        Mockito.when(snapshotDataStore1.getInstallPath()).thenReturn("/path1");
        Mockito.when(snapshotDataStore1.getSnapshotId()).thenReturn(1L);

        SnapshotDataStoreVO snapshotDataStore2 = Mockito.mock(SnapshotDataStoreVO.class);
        Mockito.when(snapshotDataStore2.getInstallPath()).thenReturn("/path2");
        Mockito.when(snapshotDataStore2.getSnapshotId()).thenReturn(2L);

        List<SnapshotDataStoreVO> snapshotDataStoreList = List.of(snapshotDataStore1, snapshotDataStore2);

        SnapshotVO snapshot1 = Mockito.mock(SnapshotVO.class);
        Mockito.when(snapshot1.getId()).thenReturn(1L);

        SnapshotVO snapshot2 = Mockito.mock(SnapshotVO.class);
        Mockito.when(snapshot2.getId()).thenReturn(2L);

        List<SnapshotVO> snapshots = List.of(snapshot1, snapshot2);

        Mockito.when(snapshotDataStoreDao.listByStoreAndInstallPaths(dataStoreId, DataStoreRole.Image, paths)).thenReturn(snapshotDataStoreList);
        Mockito.when(snapshotDao.listByIds(new Long[]{1L, 2L})).thenReturn(snapshots);

        Map<String, SnapshotVO> expectedSnapshotPathMap = new HashMap<>();
        expectedSnapshotPathMap.put("/path1", snapshot1);
        expectedSnapshotPathMap.put("/path2", snapshot2);

        Map<String, SnapshotVO> snapshotPathMap = storageBrowser.getPathSnapshotMapForSecondaryDS(dataStoreId, paths);

        Assert.assertEquals(expectedSnapshotPathMap, snapshotPathMap);
    }

    @Test
    public void testGetPathSnapshotMapForPrimaryDS() {
        long dataStoreId = 1L;
        List<String> paths = List.of("path1", "path2");
        List<String> absPaths = List.of("/mnt/path1", "/mnt/path2");

        SnapshotDataStoreVO snapshotDataStore1 = Mockito.mock(SnapshotDataStoreVO.class);
        Mockito.when(snapshotDataStore1.getInstallPath()).thenReturn("/mnt/path1");
        Mockito.when(snapshotDataStore1.getSnapshotId()).thenReturn(1L);

        SnapshotDataStoreVO snapshotDataStore2 = Mockito.mock(SnapshotDataStoreVO.class);
        Mockito.when(snapshotDataStore2.getInstallPath()).thenReturn("/mnt/path2");
        Mockito.when(snapshotDataStore2.getSnapshotId()).thenReturn(2L);

        List<SnapshotDataStoreVO> snapshotDataStoreList = List.of(snapshotDataStore1, snapshotDataStore2);

        SnapshotVO snapshot1 = Mockito.mock(SnapshotVO.class);
        Mockito.when(snapshot1.getId()).thenReturn(1L);

        SnapshotVO snapshot2 = Mockito.mock(SnapshotVO.class);
        Mockito.when(snapshot2.getId()).thenReturn(2L);

        List<SnapshotVO> snapshots = List.of(snapshot1, snapshot2);

        Mockito.when(snapshotDataStoreDao.listByStoreAndInstallPaths(dataStoreId, DataStoreRole.Primary, absPaths)).thenReturn(snapshotDataStoreList);
        Mockito.when(snapshotDao.listByIds(new Long[]{1L, 2L})).thenReturn(snapshots);

        Map<String, SnapshotVO> expectedSnapshotPathMap = new HashMap<>();
        expectedSnapshotPathMap.put("path1", snapshot1);
        expectedSnapshotPathMap.put("path2", snapshot2);

        Map<String, SnapshotVO> snapshotPathMap = storageBrowser.getPathSnapshotMapForPrimaryDS(dataStoreId, paths, absPaths);

        Assert.assertEquals(expectedSnapshotPathMap, snapshotPathMap);
    }

    @Test
    public void testGetResponse() {
        ListDataStoreObjectsAnswer answer = Mockito.mock(ListDataStoreObjectsAnswer.class);
        Mockito.when(answer.getPaths()).thenReturn(List.of("/path1", "/path2"));
        Mockito.when(answer.getAbsPaths()).thenReturn(List.of("/path1", "/path2"));
        Mockito.when(answer.getNames()).thenReturn(List.of("name1", "name2"));
        Mockito.when(answer.getIsDirs()).thenReturn(List.of(true, false));
        Mockito.when(answer.getSizes()).thenReturn(List.of(100L, 200L));
        Mockito.when(answer.getLastModified()).thenReturn(List.of((new Date()).getTime(), (new Date()).getTime()));

        List<String> paths = List.of("path1", "path2");
        List<String> absPaths = List.of("/path1", "/path2");

        Map<String, SnapshotVO> pathSnapshotMap = new HashMap<>();
        pathSnapshotMap.put("path1", Mockito.mock(SnapshotVO.class));

        VMTemplateVO template = Mockito.mock(VMTemplateVO.class);
        Map<String, VMTemplateVO> pathTemplateMap = new HashMap<>();
        pathTemplateMap.put("path2", template);
        Mockito.when(template.getFormat()).thenReturn(Storage.ImageFormat.ISO);

        Map<String, VolumeVO> pathVolumeMap = new HashMap<>();
        pathVolumeMap.put("path1", Mockito.mock(VolumeVO.class));

        DataStore dataStore = Mockito.mock(DataStore.class);

        Mockito.when(dataStore.getRole()).thenReturn(DataStoreRole.Primary);
        Mockito.when(dataStore.getId()).thenReturn(1L);
        Mockito.doReturn(pathTemplateMap).when(storageBrowser).getPathTemplateMapForPrimaryDS(1L, paths);
        Mockito.doReturn(pathSnapshotMap).when(storageBrowser).getPathSnapshotMapForPrimaryDS(1L, paths, absPaths);
        Mockito.doReturn(pathVolumeMap).when(storageBrowser).getPathVolumeMapForPrimaryDS(1L, paths);

        ListResponse<DataStoreObjectResponse> response = storageBrowser.getResponse(dataStore, answer);

        Assert.assertEquals(2, response.getResponses().size());
        Assert.assertEquals("name1", response.getResponses().get(0).getName());
        Assert.assertEquals(true, response.getResponses().get(0).isDirectory());
        Assert.assertEquals(100L, response.getResponses().get(0).getSize());
        Assert.assertNotNull(response.getResponses().get(0).getLastUpdated());

        Assert.assertEquals("name2", response.getResponses().get(1).getName());
        Assert.assertEquals(false, response.getResponses().get(1).isDirectory());
        Assert.assertEquals(200L, response.getResponses().get(1).getSize());
        Assert.assertNotNull(response.getResponses().get(1).getLastUpdated());
    }
}

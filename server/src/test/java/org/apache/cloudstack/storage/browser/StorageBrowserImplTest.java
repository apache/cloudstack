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

import com.cloud.api.query.dao.ImageStoreJoinDao;
import com.cloud.storage.VMTemplateStoragePoolVO;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VMTemplatePoolDao;
import com.cloud.storage.dao.VolumeDao;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPointSelector;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreDao;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StorageBrowserImplTest {

    @Inject
    ImageStoreJoinDao imageStoreJoinDao;

    @Inject
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

    @InjectMocks
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
        HashMap<String, VolumeVO> result = storageBrowser.getPathVolumeMapForPrimaryDS(1L, paths);
        Assert.assertTrue(result.isEmpty());
    }

    @Test
    public void testGetPathVolumeMapForPrimaryDS() {
        List<String> paths = List.of("volume1", "volume2");
        VolumeVO volume = Mockito.mock(VolumeVO.class);
        Mockito.when(volume.getPath()).thenReturn("volume1");

        Mockito.when(volumeDao.listByPoolIdAndPaths(1, paths)).thenReturn(List.of(volume));
        HashMap<String, VolumeVO> result = storageBrowser.getPathVolumeMapForPrimaryDS(1L, paths);
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

}

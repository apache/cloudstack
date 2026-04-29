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
package org.apache.cloudstack.storage.volume;

import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.storage.datastore.db.VolumeDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.VolumeDataStoreVO;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.storage.DataStoreRole;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VolumeDao;

@RunWith(MockitoJUnitRunner.class)
public class VolumeDataFactoryImplTest {

    private VolumeDataFactoryImpl volumeDataFactory;

    @Mock
    private VolumeDao volumeDao;
    @Mock
    private VolumeDataStoreDao volumeStoreDao;
    @Mock
    private DataStoreManager storeMgr;
    @Mock
    private VMTemplateDao templateDao;
    @Mock
    private VolumeVO volumeVO;
    @Mock
    private VolumeObject volumeObject;
    @Mock
    private VolumeDataStoreVO volumeDataStoreVO;
    @Mock
    private DataStore dataStore;
    @Mock
    private VMTemplateVO templateVO;

    @Before
    public void setUp() {
        volumeDataFactory = new VolumeDataFactoryImpl();
        volumeDataFactory.volumeDao = volumeDao;
        volumeDataFactory.volumeStoreDao = volumeStoreDao;
        volumeDataFactory.storeMgr = storeMgr;
        volumeDataFactory.templateDao = templateDao;
    }

    @Test
    public void getVolumeByExplicitStoreSetsDirectDownloadFlag() {
        Mockito.when(volumeDao.findById(42L)).thenReturn(volumeVO);
        Mockito.when(volumeObject.getTemplateId()).thenReturn(9L);
        Mockito.when(templateDao.findByIdIncludingRemoved(9L)).thenReturn(templateVO);
        Mockito.when(templateVO.isDirectDownload()).thenReturn(true);

        try (MockedStatic<VolumeObject> mockedStatic = Mockito.mockStatic(VolumeObject.class)) {
            mockedStatic.when(() -> VolumeObject.getVolumeObject(dataStore, volumeVO)).thenReturn(volumeObject);

            Assert.assertSame(volumeObject, volumeDataFactory.getVolume(42L, dataStore));
        }

        Mockito.verify(volumeObject).setDirectDownload(true);
    }

    @Test
    public void getVolumeByPrimaryStoreRoleSetsDirectDownloadFlag() {
        Mockito.when(volumeDao.findById(42L)).thenReturn(volumeVO);
        Mockito.when(volumeVO.getPoolId()).thenReturn(7L);
        Mockito.when(storeMgr.getDataStore(7L, DataStoreRole.Primary)).thenReturn(dataStore);
        Mockito.when(volumeObject.getTemplateId()).thenReturn(9L);
        Mockito.when(templateDao.findByIdIncludingRemoved(9L)).thenReturn(templateVO);
        Mockito.when(templateVO.isDirectDownload()).thenReturn(true);

        try (MockedStatic<VolumeObject> mockedStatic = Mockito.mockStatic(VolumeObject.class)) {
            mockedStatic.when(() -> VolumeObject.getVolumeObject(dataStore, volumeVO)).thenReturn(volumeObject);

            Assert.assertSame(volumeObject, volumeDataFactory.getVolume(42L, DataStoreRole.Primary));
        }

        Mockito.verify(volumeObject).setDirectDownload(true);
    }

    @Test
    public void getVolumeByImageStoreRoleSetsDirectDownloadFlag() {
        Mockito.when(volumeDao.findById(42L)).thenReturn(volumeVO);
        Mockito.when(volumeStoreDao.findByVolume(42L)).thenReturn(volumeDataStoreVO);
        Mockito.when(volumeDataStoreVO.getDataStoreId()).thenReturn(11L);
        Mockito.when(storeMgr.getDataStore(11L, DataStoreRole.Image)).thenReturn(dataStore);
        Mockito.when(volumeObject.getTemplateId()).thenReturn(9L);
        Mockito.when(templateDao.findByIdIncludingRemoved(9L)).thenReturn(templateVO);
        Mockito.when(templateVO.isDirectDownload()).thenReturn(true);

        try (MockedStatic<VolumeObject> mockedStatic = Mockito.mockStatic(VolumeObject.class)) {
            mockedStatic.when(() -> VolumeObject.getVolumeObject(dataStore, volumeVO)).thenReturn(volumeObject);

            Assert.assertSame(volumeObject, volumeDataFactory.getVolume(42L, DataStoreRole.Image));
        }

        Mockito.verify(volumeObject).setDirectDownload(true);
    }

    @Test
    public void getVolumeByExplicitStoreSkipsTemplateLookupWhenVolumeHasNoTemplate() {
        Mockito.when(volumeDao.findById(42L)).thenReturn(volumeVO);
        Mockito.when(volumeObject.getTemplateId()).thenReturn(null);

        try (MockedStatic<VolumeObject> mockedStatic = Mockito.mockStatic(VolumeObject.class)) {
            mockedStatic.when(() -> VolumeObject.getVolumeObject(dataStore, volumeVO)).thenReturn(volumeObject);

            Assert.assertSame(volumeObject, volumeDataFactory.getVolume(42L, dataStore));
        }

        Mockito.verifyNoInteractions(templateDao);
        Mockito.verify(volumeObject, Mockito.never()).setDirectDownload(Mockito.anyBoolean());
    }
}

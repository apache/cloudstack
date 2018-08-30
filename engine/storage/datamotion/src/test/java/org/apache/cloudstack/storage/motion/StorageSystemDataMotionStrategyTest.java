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
package org.apache.cloudstack.storage.motion;

import com.cloud.storage.DataStoreRole;
import com.cloud.storage.ImageStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataMotionStrategy;
import org.apache.cloudstack.engine.subsystem.api.storage.DataObject;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.StrategyPriority;
import org.apache.cloudstack.storage.datastore.PrimaryDataStoreImpl;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.image.store.ImageStoreImpl;
import org.apache.cloudstack.storage.volume.VolumeObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(MockitoJUnitRunner.class)
public class StorageSystemDataMotionStrategyTest {

    @Mock
    VolumeObject source;
    @Mock
    DataObject destination;
    @Mock
    PrimaryDataStore sourceStore;
    @Mock
    ImageStore destinationStore;

    @InjectMocks
    DataMotionStrategy strategy = new StorageSystemDataMotionStrategy();
    @Mock
    PrimaryDataStoreDao _storagePoolDao;

    @Before public void setUp() throws Exception {
        sourceStore = mock(PrimaryDataStoreImpl.class);
        destinationStore = mock(ImageStoreImpl.class);
        source = mock(VolumeObject.class);
        destination = mock(VolumeObject.class);

                initMocks(strategy);
    }

    @Test
    public void cantHandleSecondary() {
        doReturn(sourceStore).when(source).getDataStore();
        doReturn(DataStoreRole.Primary).when(sourceStore).getRole();
        doReturn(destinationStore).when(destination).getDataStore();
        doReturn(DataStoreRole.Image).when((DataStore)destinationStore).getRole();
        doReturn(sourceStore).when(source).getDataStore();
        doReturn(destinationStore).when(destination).getDataStore();
        StoragePoolVO storeVO = new StoragePoolVO();
        doReturn(storeVO).when(_storagePoolDao).findById(0l);

        assertTrue(strategy.canHandle(source,destination) == StrategyPriority.CANT_HANDLE);
    }
}
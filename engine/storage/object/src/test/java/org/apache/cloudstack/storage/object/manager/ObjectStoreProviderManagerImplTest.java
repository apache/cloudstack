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

package org.apache.cloudstack.storage.object.manager;

import junit.framework.TestCase;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreProviderManager;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectStoreProvider;
import org.apache.cloudstack.storage.datastore.db.ObjectStoreDao;
import org.apache.cloudstack.storage.datastore.db.ObjectStoreVO;
import org.apache.cloudstack.storage.object.ObjectStoreDriver;
import org.apache.cloudstack.storage.object.ObjectStoreEntity;
import org.apache.cloudstack.storage.object.store.ObjectStoreImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest(ObjectStoreImpl.class)
public class ObjectStoreProviderManagerImplTest extends TestCase{


    ObjectStoreProviderManagerImpl objectStoreProviderManagerImplSpy;

    @Mock
    ObjectStoreDao objectStoreDao;

    @Mock
    ObjectStoreVO objectStoreVO;

    @Mock
    DataStoreProviderManager providerManager;

    @Mock
    ObjectStoreProvider provider;

    @Mock
    Map<String, ObjectStoreDriver> driverMaps;

    @Mock
    ObjectStoreDriver objectStoreDriver;

    @Mock
    ObjectStoreEntity objectStoreEntity;

    @Before
    public void setup(){
        objectStoreProviderManagerImplSpy = PowerMockito.spy(new ObjectStoreProviderManagerImpl());
        objectStoreProviderManagerImplSpy.objectStoreDao = objectStoreDao;
        objectStoreProviderManagerImplSpy.providerManager = providerManager;
        objectStoreProviderManagerImplSpy.driverMaps = driverMaps;
        mockStatic(ObjectStoreImpl.class);
    }

    @Test
    public void getObjectStoreTest() {
        Mockito.doReturn(objectStoreVO).when(objectStoreDao).findById(Mockito.anyLong());
        Mockito.doReturn(provider).when(providerManager).getDataStoreProvider(Mockito.anyString());
        Mockito.doReturn(objectStoreDriver).when(driverMaps).get(Mockito.anyString());
        Mockito.doReturn("Simulator").when(provider).getName();
        Mockito.doReturn("Simulator").when(objectStoreVO).getProviderName();
        when(ObjectStoreImpl.getDataStore(Mockito.any(ObjectStoreVO.class), Mockito.any(ObjectStoreDriver.class),
                Mockito.any(ObjectStoreProvider.class))).thenReturn(objectStoreEntity);
        assertNotNull(objectStoreProviderManagerImplSpy.getObjectStore(1L));
    }

    @Test
    public void listObjectStoresTest() {
        List<ObjectStoreVO> stores = new ArrayList<>();
        stores.add(objectStoreVO);
        Mockito.doReturn(objectStoreVO).when(objectStoreDao).findById(Mockito.anyLong());
        Mockito.doReturn(provider).when(providerManager).getDataStoreProvider(Mockito.anyString());
        Mockito.doReturn(objectStoreDriver).when(driverMaps).get(Mockito.anyString());
        Mockito.doReturn("Simulator").when(provider).getName();
        Mockito.doReturn("Simulator").when(objectStoreVO).getProviderName();
        when(ObjectStoreImpl.getDataStore(Mockito.any(ObjectStoreVO.class), Mockito.any(ObjectStoreDriver.class),
                Mockito.any(ObjectStoreProvider.class))).thenReturn(objectStoreEntity);
        Mockito.doReturn(stores).when(objectStoreDao).listObjectStores();
        assertEquals(1, objectStoreProviderManagerImplSpy.listObjectStores().size());
    }

}

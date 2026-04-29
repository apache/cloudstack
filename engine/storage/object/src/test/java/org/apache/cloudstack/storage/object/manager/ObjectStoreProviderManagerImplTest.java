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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
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

    MockedStatic<ObjectStoreImpl> mockObjectStoreImpl;

    @Before
    public void setup(){
        objectStoreProviderManagerImplSpy = Mockito.spy(new ObjectStoreProviderManagerImpl());
        objectStoreProviderManagerImplSpy.objectStoreDao = objectStoreDao;
        objectStoreProviderManagerImplSpy.providerManager = providerManager;
        objectStoreProviderManagerImplSpy.driverMaps = driverMaps;
        mockObjectStoreImpl = mockStatic(ObjectStoreImpl.class);
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

    @Override
    @After
    public void tearDown() throws Exception {
        mockObjectStoreImpl.close();
        super.tearDown();
    }
}

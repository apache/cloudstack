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
package org.apache.cloudstack.storage.datastore.provider;

import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreProvider.DataStoreProviderType;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

import java.util.Set;

import static org.junit.Assert.assertEquals;

public class CephObjectStoreProviderImplTest {

    private CephObjectStoreProviderImpl cephObjectStoreProviderImpl;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        cephObjectStoreProviderImpl = new CephObjectStoreProviderImpl();
    }

    @Test
    public void testGetName() {
        String name = cephObjectStoreProviderImpl.getName();
        assertEquals("Ceph", name);
    }

    @Test
    public void testGetTypes() {
        Set<DataStoreProviderType> types = cephObjectStoreProviderImpl.getTypes();
        assertEquals(1, types.size());
        assertEquals("OBJECT", types.toArray()[0].toString());
    }
}

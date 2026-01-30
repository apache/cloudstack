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
package org.apache.cloudstack.storage.provider;

import com.cloud.utils.component.ComponentContext;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreDriver;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreLifeCycle;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreProvider.DataStoreProviderType;
import org.apache.cloudstack.engine.subsystem.api.storage.HypervisorHostListener;
import org.apache.cloudstack.storage.driver.OntapPrimaryDatastoreDriver;
import org.apache.cloudstack.storage.lifecycle.OntapPrimaryDatastoreLifecycle;
import org.apache.cloudstack.storage.listener.OntapHostListener;
import org.apache.cloudstack.storage.utils.Constants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class OntapPrimaryDatastoreProviderTest {

    private OntapPrimaryDatastoreProvider provider;

    @BeforeEach
    void setUp() {
        provider = new OntapPrimaryDatastoreProvider();
    }

    @Test
    public void testGetName() {
        String name = provider.getName();
        assertEquals(Constants.ONTAP_PLUGIN_NAME, name);
    }

    @Test
    public void testGetTypes() {
        Set<DataStoreProviderType> types = provider.getTypes();
        assertNotNull(types);
        assertEquals(1, types.size());
        assertTrue(types.contains(DataStoreProviderType.PRIMARY));
    }

    @Test
    public void testGetDataStoreLifeCycle_beforeConfigure() {
        DataStoreLifeCycle lifeCycle = provider.getDataStoreLifeCycle();
        assertNull(lifeCycle);
    }

    @Test
    public void testGetDataStoreDriver_beforeConfigure() {
        DataStoreDriver driver = provider.getDataStoreDriver();
        assertNull(driver);
    }

    @Test
    public void testGetHostListener_beforeConfigure() {
        HypervisorHostListener listener = provider.getHostListener();
        assertNull(listener);
    }

    @Test
    public void testConfigure() {
        OntapPrimaryDatastoreDriver mockDriver = mock(OntapPrimaryDatastoreDriver.class);
        OntapPrimaryDatastoreLifecycle mockLifecycle = mock(OntapPrimaryDatastoreLifecycle.class);
        OntapHostListener mockListener = mock(OntapHostListener.class);

        try (MockedStatic<ComponentContext> componentContext = Mockito.mockStatic(ComponentContext.class)) {
            componentContext.when(() -> ComponentContext.inject(OntapPrimaryDatastoreDriver.class))
                    .thenReturn(mockDriver);
            componentContext.when(() -> ComponentContext.inject(OntapPrimaryDatastoreLifecycle.class))
                    .thenReturn(mockLifecycle);
            componentContext.when(() -> ComponentContext.inject(OntapHostListener.class))
                    .thenReturn(mockListener);

            Map<String, Object> params = new HashMap<>();
            boolean result = provider.configure(params);

            assertTrue(result);
        }
    }

    @Test
    public void testGetDataStoreLifeCycle_afterConfigure() {
        OntapPrimaryDatastoreDriver mockDriver = mock(OntapPrimaryDatastoreDriver.class);
        OntapPrimaryDatastoreLifecycle mockLifecycle = mock(OntapPrimaryDatastoreLifecycle.class);
        OntapHostListener mockListener = mock(OntapHostListener.class);

        try (MockedStatic<ComponentContext> componentContext = Mockito.mockStatic(ComponentContext.class)) {
            componentContext.when(() -> ComponentContext.inject(OntapPrimaryDatastoreDriver.class))
                    .thenReturn(mockDriver);
            componentContext.when(() -> ComponentContext.inject(OntapPrimaryDatastoreLifecycle.class))
                    .thenReturn(mockLifecycle);
            componentContext.when(() -> ComponentContext.inject(OntapHostListener.class))
                    .thenReturn(mockListener);

            provider.configure(new HashMap<>());

            DataStoreLifeCycle lifeCycle = provider.getDataStoreLifeCycle();
            assertNotNull(lifeCycle);
            assertEquals(mockLifecycle, lifeCycle);
        }
    }

    @Test
    public void testGetDataStoreDriver_afterConfigure() {
        OntapPrimaryDatastoreDriver mockDriver = mock(OntapPrimaryDatastoreDriver.class);
        OntapPrimaryDatastoreLifecycle mockLifecycle = mock(OntapPrimaryDatastoreLifecycle.class);
        OntapHostListener mockListener = mock(OntapHostListener.class);

        try (MockedStatic<ComponentContext> componentContext = Mockito.mockStatic(ComponentContext.class)) {
            componentContext.when(() -> ComponentContext.inject(OntapPrimaryDatastoreDriver.class))
                    .thenReturn(mockDriver);
            componentContext.when(() -> ComponentContext.inject(OntapPrimaryDatastoreLifecycle.class))
                    .thenReturn(mockLifecycle);
            componentContext.when(() -> ComponentContext.inject(OntapHostListener.class))
                    .thenReturn(mockListener);

            provider.configure(new HashMap<>());

            DataStoreDriver driver = provider.getDataStoreDriver();
            assertNotNull(driver);
            assertEquals(mockDriver, driver);
        }
    }

    @Test
    public void testGetHostListener_afterConfigure() {
        OntapPrimaryDatastoreDriver mockDriver = mock(OntapPrimaryDatastoreDriver.class);
        OntapPrimaryDatastoreLifecycle mockLifecycle = mock(OntapPrimaryDatastoreLifecycle.class);
        OntapHostListener mockListener = mock(OntapHostListener.class);

        try (MockedStatic<ComponentContext> componentContext = Mockito.mockStatic(ComponentContext.class)) {
            componentContext.when(() -> ComponentContext.inject(OntapPrimaryDatastoreDriver.class))
                    .thenReturn(mockDriver);
            componentContext.when(() -> ComponentContext.inject(OntapPrimaryDatastoreLifecycle.class))
                    .thenReturn(mockLifecycle);
            componentContext.when(() -> ComponentContext.inject(OntapHostListener.class))
                    .thenReturn(mockListener);

            provider.configure(new HashMap<>());

            HypervisorHostListener listener = provider.getHostListener();
            assertNotNull(listener);
            assertEquals(mockListener, listener);
        }
    }

    @Test
    public void testConfigure_withNullParams() {
        OntapPrimaryDatastoreDriver mockDriver = mock(OntapPrimaryDatastoreDriver.class);
        OntapPrimaryDatastoreLifecycle mockLifecycle = mock(OntapPrimaryDatastoreLifecycle.class);
        OntapHostListener mockListener = mock(OntapHostListener.class);

        try (MockedStatic<ComponentContext> componentContext = Mockito.mockStatic(ComponentContext.class)) {
            componentContext.when(() -> ComponentContext.inject(OntapPrimaryDatastoreDriver.class))
                    .thenReturn(mockDriver);
            componentContext.when(() -> ComponentContext.inject(OntapPrimaryDatastoreLifecycle.class))
                    .thenReturn(mockLifecycle);
            componentContext.when(() -> ComponentContext.inject(OntapHostListener.class))
                    .thenReturn(mockListener);

            boolean result = provider.configure(null);

            assertTrue(result);
            assertNotNull(provider.getDataStoreDriver());
            assertNotNull(provider.getDataStoreLifeCycle());
            assertNotNull(provider.getHostListener());
        }
    }

    @Test
    public void testGetTypes_returnsOnlyPrimaryType() {
        Set<DataStoreProviderType> types = provider.getTypes();

        assertNotNull(types);
        assertEquals(1, types.size());
        assertTrue(types.contains(DataStoreProviderType.PRIMARY));

        // Verify it doesn't contain other types
        for (DataStoreProviderType type : types) {
            assertEquals(DataStoreProviderType.PRIMARY, type);
        }
    }
}

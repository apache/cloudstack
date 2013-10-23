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
package org.apache.cloudstack.engine.subsystem.api.storage;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotStrategy.SnapshotOperation;
import org.apache.cloudstack.storage.helper.StorageStrategyFactoryImpl;
import org.junit.Test;

import com.cloud.host.Host;
import com.cloud.storage.Snapshot;

public class StrategyPriorityTest {

    @Test
    public void testSortSnapshotStrategies() {
        SnapshotStrategy cantHandleStrategy = mock(SnapshotStrategy.class);
        SnapshotStrategy defaultStrategy = mock(SnapshotStrategy.class);
        SnapshotStrategy hyperStrategy = mock(SnapshotStrategy.class);
        SnapshotStrategy pluginStrategy = mock(SnapshotStrategy.class);
        SnapshotStrategy highestStrategy = mock(SnapshotStrategy.class);

        doReturn(StrategyPriority.CANT_HANDLE).when(cantHandleStrategy).canHandle(any(Snapshot.class), any(SnapshotOperation.class));
        doReturn(StrategyPriority.DEFAULT).when(defaultStrategy).canHandle(any(Snapshot.class), any(SnapshotOperation.class));
        doReturn(StrategyPriority.HYPERVISOR).when(hyperStrategy).canHandle(any(Snapshot.class), any(SnapshotOperation.class));
        doReturn(StrategyPriority.PLUGIN).when(pluginStrategy).canHandle(any(Snapshot.class), any(SnapshotOperation.class));
        doReturn(StrategyPriority.HIGHEST).when(highestStrategy).canHandle(any(Snapshot.class), any(SnapshotOperation.class));

        List<SnapshotStrategy> strategies = new ArrayList<SnapshotStrategy>(5);
        strategies.addAll(Arrays.asList(defaultStrategy, pluginStrategy, hyperStrategy, cantHandleStrategy, highestStrategy));

        StorageStrategyFactoryImpl factory = new StorageStrategyFactoryImpl();
        factory.setSnapshotStrategies(strategies);
        Iterator<SnapshotStrategy> iter = factory.getSnapshotStrategies(mock(Snapshot.class), SnapshotOperation.TAKE).iterator();

        assertEquals("Highest was not 1st.", highestStrategy, iter.next());
        assertEquals("Plugin was not 2nd.", pluginStrategy, iter.next());
        assertEquals("Hypervisor was not 3rd.", hyperStrategy, iter.next());
        assertEquals("Default was not 4th.", defaultStrategy, iter.next());
        assertTrue("Can't Handle was not 5th.", !iter.hasNext());
    }

    @Test
    public void testSortDataMotionStrategies() {
        DataMotionStrategy cantHandleStrategy = mock(DataMotionStrategy.class);
        DataMotionStrategy defaultStrategy = mock(DataMotionStrategy.class);
        DataMotionStrategy hyperStrategy = mock(DataMotionStrategy.class);
        DataMotionStrategy pluginStrategy = mock(DataMotionStrategy.class);
        DataMotionStrategy highestStrategy = mock(DataMotionStrategy.class);

        doReturn(StrategyPriority.CANT_HANDLE).when(cantHandleStrategy).canHandle(any(DataObject.class), any(DataObject.class));
        doReturn(StrategyPriority.DEFAULT).when(defaultStrategy).canHandle(any(DataObject.class), any(DataObject.class));
        doReturn(StrategyPriority.HYPERVISOR).when(hyperStrategy).canHandle(any(DataObject.class), any(DataObject.class));
        doReturn(StrategyPriority.PLUGIN).when(pluginStrategy).canHandle(any(DataObject.class), any(DataObject.class));
        doReturn(StrategyPriority.HIGHEST).when(highestStrategy).canHandle(any(DataObject.class), any(DataObject.class));

        List<DataMotionStrategy> strategies = new ArrayList<DataMotionStrategy>(5);
        strategies.addAll(Arrays.asList(defaultStrategy, pluginStrategy, hyperStrategy, cantHandleStrategy, highestStrategy));

        StorageStrategyFactoryImpl factory = new StorageStrategyFactoryImpl();
        factory.setDataMotionStrategies(strategies);
        Iterator<DataMotionStrategy> iter = factory.getDataMotionStrategies(mock(DataObject.class), mock(DataObject.class)).iterator();

        assertEquals("Highest was not 1st.", highestStrategy, iter.next());
        assertEquals("Plugin was not 2nd.", pluginStrategy, iter.next());
        assertEquals("Hypervisor was not 3rd.", hyperStrategy, iter.next());
        assertEquals("Default was not 4th.", defaultStrategy, iter.next());
        assertTrue("Can't Handle was not 5th.", !iter.hasNext());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSortDataMotionStrategies2() {
        DataMotionStrategy cantHandleStrategy = mock(DataMotionStrategy.class);
        DataMotionStrategy defaultStrategy = mock(DataMotionStrategy.class);
        DataMotionStrategy hyperStrategy = mock(DataMotionStrategy.class);
        DataMotionStrategy pluginStrategy = mock(DataMotionStrategy.class);
        DataMotionStrategy highestStrategy = mock(DataMotionStrategy.class);

        doReturn(StrategyPriority.CANT_HANDLE).when(cantHandleStrategy).canHandle(any(Map.class), any(Host.class), any(Host.class));
        doReturn(StrategyPriority.DEFAULT).when(defaultStrategy).canHandle(any(Map.class), any(Host.class), any(Host.class));
        doReturn(StrategyPriority.HYPERVISOR).when(hyperStrategy).canHandle(any(Map.class), any(Host.class), any(Host.class));
        doReturn(StrategyPriority.PLUGIN).when(pluginStrategy).canHandle(any(Map.class), any(Host.class), any(Host.class));
        doReturn(StrategyPriority.HIGHEST).when(highestStrategy).canHandle(any(Map.class), any(Host.class), any(Host.class));

        List<DataMotionStrategy> strategies = new ArrayList<DataMotionStrategy>(5);
        strategies.addAll(Arrays.asList(defaultStrategy, pluginStrategy, hyperStrategy, cantHandleStrategy, highestStrategy));

        StorageStrategyFactoryImpl factory = new StorageStrategyFactoryImpl();
        factory.setDataMotionStrategies(strategies);
        Iterator<DataMotionStrategy> iter = factory.getDataMotionStrategies(mock(Map.class), mock(Host.class), mock(Host.class)).iterator();

        assertEquals("Highest was not 1st.", highestStrategy, iter.next());
        assertEquals("Plugin was not 2nd.", pluginStrategy, iter.next());
        assertEquals("Hypervisor was not 3rd.", hyperStrategy, iter.next());
        assertEquals("Default was not 4th.", defaultStrategy, iter.next());
        assertTrue("Can't Handle was not 5th.", !iter.hasNext());
    }
}

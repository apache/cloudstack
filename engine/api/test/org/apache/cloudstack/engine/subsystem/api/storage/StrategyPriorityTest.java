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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.engine.subsystem.api.storage.StrategyPriority.Priority;
import org.junit.Test;

import com.cloud.host.Host;
import com.cloud.storage.Snapshot;

import static org.junit.Assert.assertEquals;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

public class StrategyPriorityTest {

    @Test
    public void testSortSnapshotStrategies() {
        SnapshotStrategy cantHandleStrategy = mock(SnapshotStrategy.class);
        SnapshotStrategy defaultStrategy = mock(SnapshotStrategy.class);
        SnapshotStrategy hyperStrategy = mock(SnapshotStrategy.class);
        SnapshotStrategy pluginStrategy = mock(SnapshotStrategy.class);
        SnapshotStrategy highestStrategy = mock(SnapshotStrategy.class);

        doReturn(Priority.CANT_HANDLE).when(cantHandleStrategy).canHandle(any(Snapshot.class));
        doReturn(Priority.DEFAULT).when(defaultStrategy).canHandle(any(Snapshot.class));
        doReturn(Priority.HYPERVISOR).when(hyperStrategy).canHandle(any(Snapshot.class));
        doReturn(Priority.PLUGIN).when(pluginStrategy).canHandle(any(Snapshot.class));
        doReturn(Priority.HIGHEST).when(highestStrategy).canHandle(any(Snapshot.class));

        List<SnapshotStrategy> strategies = new ArrayList<SnapshotStrategy>(5);
        strategies.addAll(Arrays.asList(defaultStrategy, pluginStrategy, hyperStrategy, cantHandleStrategy, highestStrategy));

        StrategyPriority.sortStrategies(strategies, mock(Snapshot.class));

        assertEquals("Highest was not 1st.", highestStrategy, strategies.get(0));
        assertEquals("Plugin was not 2nd.", pluginStrategy, strategies.get(1));
        assertEquals("Hypervisor was not 3rd.", hyperStrategy, strategies.get(2));
        assertEquals("Default was not 4th.", defaultStrategy, strategies.get(3));
        assertEquals("Can't Handle was not 5th.", cantHandleStrategy, strategies.get(4));
    }

    @Test
    public void testSortDataMotionStrategies() {
        DataMotionStrategy cantHandleStrategy = mock(DataMotionStrategy.class);
        DataMotionStrategy defaultStrategy = mock(DataMotionStrategy.class);
        DataMotionStrategy hyperStrategy = mock(DataMotionStrategy.class);
        DataMotionStrategy pluginStrategy = mock(DataMotionStrategy.class);
        DataMotionStrategy highestStrategy = mock(DataMotionStrategy.class);

        doReturn(Priority.CANT_HANDLE).when(cantHandleStrategy).canHandle(any(DataObject.class), any(DataObject.class));
        doReturn(Priority.DEFAULT).when(defaultStrategy).canHandle(any(DataObject.class), any(DataObject.class));
        doReturn(Priority.HYPERVISOR).when(hyperStrategy).canHandle(any(DataObject.class), any(DataObject.class));
        doReturn(Priority.PLUGIN).when(pluginStrategy).canHandle(any(DataObject.class), any(DataObject.class));
        doReturn(Priority.HIGHEST).when(highestStrategy).canHandle(any(DataObject.class), any(DataObject.class));

        List<DataMotionStrategy> strategies = new ArrayList<DataMotionStrategy>(5);
        strategies.addAll(Arrays.asList(defaultStrategy, pluginStrategy, hyperStrategy, cantHandleStrategy, highestStrategy));

        StrategyPriority.sortStrategies(strategies, mock(DataObject.class), mock(DataObject.class));

        assertEquals("Highest was not 1st.", highestStrategy, strategies.get(0));
        assertEquals("Plugin was not 2nd.", pluginStrategy, strategies.get(1));
        assertEquals("Hypervisor was not 3rd.", hyperStrategy, strategies.get(2));
        assertEquals("Default was not 4th.", defaultStrategy, strategies.get(3));
        assertEquals("Can't Handle was not 5th.", cantHandleStrategy, strategies.get(4));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSortDataMotionStrategies2() {
        DataMotionStrategy cantHandleStrategy = mock(DataMotionStrategy.class);
        DataMotionStrategy defaultStrategy = mock(DataMotionStrategy.class);
        DataMotionStrategy hyperStrategy = mock(DataMotionStrategy.class);
        DataMotionStrategy pluginStrategy = mock(DataMotionStrategy.class);
        DataMotionStrategy highestStrategy = mock(DataMotionStrategy.class);

        doReturn(Priority.CANT_HANDLE).when(cantHandleStrategy).canHandle(any(Map.class), any(Host.class), any(Host.class));
        doReturn(Priority.DEFAULT).when(defaultStrategy).canHandle(any(Map.class), any(Host.class), any(Host.class));
        doReturn(Priority.HYPERVISOR).when(hyperStrategy).canHandle(any(Map.class), any(Host.class), any(Host.class));
        doReturn(Priority.PLUGIN).when(pluginStrategy).canHandle(any(Map.class), any(Host.class), any(Host.class));
        doReturn(Priority.HIGHEST).when(highestStrategy).canHandle(any(Map.class), any(Host.class), any(Host.class));

        List<DataMotionStrategy> strategies = new ArrayList<DataMotionStrategy>(5);
        strategies.addAll(Arrays.asList(defaultStrategy, pluginStrategy, hyperStrategy, cantHandleStrategy, highestStrategy));

        StrategyPriority.sortStrategies(strategies, mock(Map.class), mock(Host.class), mock(Host.class));

        assertEquals("Highest was not 1st.", highestStrategy, strategies.get(0));
        assertEquals("Plugin was not 2nd.", pluginStrategy, strategies.get(1));
        assertEquals("Hypervisor was not 3rd.", hyperStrategy, strategies.get(2));
        assertEquals("Default was not 4th.", defaultStrategy, strategies.get(3));
        assertEquals("Can't Handle was not 5th.", cantHandleStrategy, strategies.get(4));
    }
}

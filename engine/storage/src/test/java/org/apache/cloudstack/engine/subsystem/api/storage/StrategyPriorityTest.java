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

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.storage.helper.StorageStrategyFactoryImpl;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.host.Host;
import com.cloud.storage.Snapshot;


@RunWith(MockitoJUnitRunner.class)
public class StrategyPriorityTest {

    @Test
    public void testSortSnapshotStrategies() {
        SnapshotStrategy cantHandleStrategy = mock(SnapshotStrategy.class);
        SnapshotStrategy defaultStrategy = mock(SnapshotStrategy.class);
        SnapshotStrategy hyperStrategy = mock(SnapshotStrategy.class);
        SnapshotStrategy highestStrategy = mock(SnapshotStrategy.class);

        doReturn(StrategyPriority.CANT_HANDLE).when(cantHandleStrategy).canHandle(any(Snapshot.class), Mockito.nullable(Long.class), any(SnapshotStrategy.SnapshotOperation.class));
        doReturn(StrategyPriority.DEFAULT).when(defaultStrategy).canHandle(any(Snapshot.class), Mockito.nullable(Long.class), any(SnapshotStrategy.SnapshotOperation.class));
        doReturn(StrategyPriority.HYPERVISOR).when(hyperStrategy).canHandle(any(Snapshot.class), Mockito.nullable(Long.class), any(SnapshotStrategy.SnapshotOperation.class));
        doReturn(StrategyPriority.HIGHEST).when(highestStrategy).canHandle(any(Snapshot.class), Mockito.nullable(Long.class), any(SnapshotStrategy.SnapshotOperation.class));

        List<SnapshotStrategy> strategies = new ArrayList<>(5);
        SnapshotStrategy strategy = null;

        StorageStrategyFactoryImpl factory = new StorageStrategyFactoryImpl();
        factory.setSnapshotStrategies(strategies);

        strategies.add(cantHandleStrategy);
        strategy = factory.getSnapshotStrategy(mock(Snapshot.class), SnapshotStrategy.SnapshotOperation.TAKE);
        assertEquals("A strategy was found when it shouldn't have been.", null, strategy);

        strategies.add(defaultStrategy);
        strategy = factory.getSnapshotStrategy(mock(Snapshot.class), SnapshotStrategy.SnapshotOperation.TAKE);
        assertEquals("Default strategy was not picked.", defaultStrategy, strategy);

        strategies.add(hyperStrategy);
        strategy = factory.getSnapshotStrategy(mock(Snapshot.class), SnapshotStrategy.SnapshotOperation.TAKE);
        assertEquals("Hypervisor strategy was not picked.", hyperStrategy, strategy);

        strategies.add(highestStrategy);
        strategy = factory.getSnapshotStrategy(mock(Snapshot.class), SnapshotStrategy.SnapshotOperation.TAKE);
        assertEquals("Highest strategy was not picked.", highestStrategy, strategy);
    }

    @Test
    public void testSortDataMotionStrategies() {
        DataMotionStrategy cantHandleStrategy = mock(DataMotionStrategy.class);
        DataMotionStrategy defaultStrategy = mock(DataMotionStrategy.class);
        DataMotionStrategy hyperStrategy = mock(DataMotionStrategy.class);
        DataMotionStrategy highestStrategy = mock(DataMotionStrategy.class);

        doReturn(StrategyPriority.CANT_HANDLE).when(cantHandleStrategy).canHandle(any(DataObject.class), any(DataObject.class));
        doReturn(StrategyPriority.DEFAULT).when(defaultStrategy).canHandle(any(DataObject.class), any(DataObject.class));
        doReturn(StrategyPriority.HYPERVISOR).when(hyperStrategy).canHandle(any(DataObject.class), any(DataObject.class));
        doReturn(StrategyPriority.HIGHEST).when(highestStrategy).canHandle(any(DataObject.class), any(DataObject.class));

        List<DataMotionStrategy> strategies = new ArrayList<DataMotionStrategy>(5);
        DataMotionStrategy strategy = null;

        StorageStrategyFactoryImpl factory = new StorageStrategyFactoryImpl();
        factory.setDataMotionStrategies(strategies);

        strategies.add(cantHandleStrategy);
        strategy = factory.getDataMotionStrategy(mock(DataObject.class), mock(DataObject.class));
        assertEquals("A strategy was found when it shouldn't have been.", null, strategy);

        strategies.add(defaultStrategy);
        strategy = factory.getDataMotionStrategy(mock(DataObject.class), mock(DataObject.class));
        assertEquals("Default strategy was not picked.", defaultStrategy, strategy);

        strategies.add(hyperStrategy);
        strategy = factory.getDataMotionStrategy(mock(DataObject.class), mock(DataObject.class));
        assertEquals("Hypervisor strategy was not picked.", hyperStrategy, strategy);

        strategies.add(highestStrategy);
        strategy = factory.getDataMotionStrategy(mock(DataObject.class), mock(DataObject.class));
        assertEquals("Highest strategy was not picked.", highestStrategy, strategy);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSortDataMotionStrategies2() {
        DataMotionStrategy cantHandleStrategy = mock(DataMotionStrategy.class);
        DataMotionStrategy defaultStrategy = mock(DataMotionStrategy.class);
        DataMotionStrategy hyperStrategy = mock(DataMotionStrategy.class);
        DataMotionStrategy highestStrategy = mock(DataMotionStrategy.class);

        doReturn(StrategyPriority.CANT_HANDLE).when(cantHandleStrategy).canHandle(any(Map.class), any(Host.class), any(Host.class));
        doReturn(StrategyPriority.DEFAULT).when(defaultStrategy).canHandle(any(Map.class), any(Host.class), any(Host.class));
        doReturn(StrategyPriority.HYPERVISOR).when(hyperStrategy).canHandle(any(Map.class), any(Host.class), any(Host.class));
        doReturn(StrategyPriority.HIGHEST).when(highestStrategy).canHandle(any(Map.class), any(Host.class), any(Host.class));

        List<DataMotionStrategy> strategies = new ArrayList<DataMotionStrategy>(5);
        DataMotionStrategy strategy = null;

        StorageStrategyFactoryImpl factory = new StorageStrategyFactoryImpl();
        factory.setDataMotionStrategies(strategies);

        strategies.add(cantHandleStrategy);
        strategy = factory.getDataMotionStrategy(mock(Map.class), mock(Host.class), mock(Host.class));
        assertEquals("A strategy was found when it shouldn't have been.", null, strategy);

        strategies.add(defaultStrategy);
        strategy = factory.getDataMotionStrategy(mock(Map.class), mock(Host.class), mock(Host.class));
        assertEquals("Default strategy was not picked.", defaultStrategy, strategy);

        strategies.add(hyperStrategy);
        strategy = factory.getDataMotionStrategy(mock(Map.class), mock(Host.class), mock(Host.class));
        assertEquals("Hypervisor strategy was not picked.", hyperStrategy, strategy);

        strategies.add(highestStrategy);
        strategy = factory.getDataMotionStrategy(mock(Map.class), mock(Host.class), mock(Host.class));
        assertEquals("Highest strategy was not picked.", highestStrategy, strategy);
    }
}

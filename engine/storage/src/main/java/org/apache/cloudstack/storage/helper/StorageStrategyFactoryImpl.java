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
package org.apache.cloudstack.storage.helper;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.DataMotionStrategy;
import org.apache.cloudstack.engine.subsystem.api.storage.DataObject;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotStrategy;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotStrategy.SnapshotOperation;
import org.apache.cloudstack.engine.subsystem.api.storage.StorageStrategyFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.StrategyPriority;
import org.apache.cloudstack.engine.subsystem.api.storage.VMSnapshotStrategy;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;

import com.cloud.host.Host;
import com.cloud.storage.Snapshot;
import com.cloud.vm.snapshot.VMSnapshot;

public class StorageStrategyFactoryImpl implements StorageStrategyFactory {

    List<SnapshotStrategy> snapshotStrategies;
    List<DataMotionStrategy> dataMotionStrategies;
    List<VMSnapshotStrategy> vmSnapshotStrategies;

    @Override
    public DataMotionStrategy getDataMotionStrategy(final DataObject srcData, final DataObject destData) {
        return bestMatch(dataMotionStrategies, new CanHandle<DataMotionStrategy>() {
            @Override
            public StrategyPriority canHandle(DataMotionStrategy strategy) {
                return strategy.canHandle(srcData, destData);
            }
        });
    }

    @Override
    public DataMotionStrategy getDataMotionStrategy(final Map<VolumeInfo, DataStore> volumeMap, final Host srcHost, final Host destHost) {
        return bestMatch(dataMotionStrategies, new CanHandle<DataMotionStrategy>() {
            @Override
            public StrategyPriority canHandle(DataMotionStrategy strategy) {
                return strategy.canHandle(volumeMap, srcHost, destHost);
            }
        });
    }

    @Override
    public SnapshotStrategy getSnapshotStrategy(final Snapshot snapshot, final SnapshotOperation op) {
        return bestMatch(snapshotStrategies, new CanHandle<SnapshotStrategy>() {
            @Override
            public StrategyPriority canHandle(SnapshotStrategy strategy) {
                return strategy.canHandle(snapshot, op);
            }
        });
    }

    @Override
    public VMSnapshotStrategy getVmSnapshotStrategy(final VMSnapshot vmSnapshot) {
        return bestMatch(vmSnapshotStrategies, new CanHandle<VMSnapshotStrategy>() {
            @Override
            public StrategyPriority canHandle(VMSnapshotStrategy strategy) {
                return strategy.canHandle(vmSnapshot);
            }
        });
    }

    @Override
    public VMSnapshotStrategy getVmSnapshotStrategy(final Long vmId, Long rootPoolId, boolean snapshotMemory) {
        return bestMatch(vmSnapshotStrategies, new CanHandle<VMSnapshotStrategy>() {
            @Override
            public StrategyPriority canHandle(VMSnapshotStrategy strategy) {
                return strategy.canHandle(vmId, rootPoolId, snapshotMemory);
            }
        });
    }

    private static <T> T bestMatch(Collection<T> collection, final CanHandle<T> canHandle) {
        if (collection.size() == 0)
            return null;

        StrategyPriority highestPriority = StrategyPriority.CANT_HANDLE;

        T strategyToUse = null;
        for (T strategy : collection) {
            StrategyPriority priority = canHandle.canHandle(strategy);
            if (priority.ordinal() > highestPriority.ordinal()) {
                highestPriority = priority;
                strategyToUse = strategy;
            }
        }

        return strategyToUse;
    }

    private static interface CanHandle<T> {
        StrategyPriority canHandle(T strategy);
    }

    public List<SnapshotStrategy> getSnapshotStrategies() {
        return snapshotStrategies;
    }

    @Inject
    public void setSnapshotStrategies(List<SnapshotStrategy> snapshotStrategies) {
        this.snapshotStrategies = snapshotStrategies;
    }

    public List<DataMotionStrategy> getDataMotionStrategies() {
        return dataMotionStrategies;
    }

    @Inject
    public void setDataMotionStrategies(List<DataMotionStrategy> dataMotionStrategies) {
        this.dataMotionStrategies = dataMotionStrategies;
    }

    @Inject
    public void setVmSnapshotStrategies(List<VMSnapshotStrategy> vmSnapshotStrategies) {
        this.vmSnapshotStrategies = vmSnapshotStrategies;
    }

    public List<VMSnapshotStrategy> getVmSnapshotStrategies() {
        return vmSnapshotStrategies;
    }

}

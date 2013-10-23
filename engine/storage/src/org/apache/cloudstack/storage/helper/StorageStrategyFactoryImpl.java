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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.DataMotionStrategy;
import org.apache.cloudstack.engine.subsystem.api.storage.DataObject;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotStrategy;
import org.apache.cloudstack.engine.subsystem.api.storage.StorageStrategyFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.StrategyPriority;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotStrategy.SnapshotOperation;

import com.cloud.host.Host;
import com.cloud.storage.Snapshot;

public class StorageStrategyFactoryImpl implements StorageStrategyFactory {

    List<SnapshotStrategy> snapshotStrategies;
    List<DataMotionStrategy> dataMotionStrategies;

    @Override
    public DataMotionStrategy getDataMotionStrategy(DataObject srcData, DataObject destData) {
        return first(getDataMotionStrategies(srcData, destData));
    }

    @Override
    public DataMotionStrategy getDataMotionStrategy(Map<VolumeInfo, DataStore> volumeMap, Host srcHost, Host destHost) {
        return first(getDataMotionStrategies(volumeMap, srcHost, destHost));
    }

    @Override
    public SnapshotStrategy getSnapshotStrategy(Snapshot snapshot, SnapshotOperation op) {
        return first(getSnapshotStrategies(snapshot, op));
    }

    @Override
    public Collection<DataMotionStrategy> getDataMotionStrategies(final DataObject srcData, final DataObject destData) {
        return sort(dataMotionStrategies, new CanHandle<DataMotionStrategy>() {
            @Override
            public StrategyPriority canHandle(DataMotionStrategy strategy) {
                return strategy.canHandle(srcData, destData);
            }
        });
    }

    @Override
    public Collection<DataMotionStrategy> getDataMotionStrategies(final Map<VolumeInfo, DataStore> volumeMap, final Host srcHost, final Host destHost) {
        return sort(dataMotionStrategies, new CanHandle<DataMotionStrategy>() {
            @Override
            public StrategyPriority canHandle(DataMotionStrategy strategy) {
                return strategy.canHandle(volumeMap, srcHost, destHost);
            }
        });
    }

    @Override
    public Collection<SnapshotStrategy> getSnapshotStrategies(final Snapshot snapshot, final SnapshotOperation op) {
        return sort(snapshotStrategies, new CanHandle<SnapshotStrategy>() {
            @Override
            public StrategyPriority canHandle(SnapshotStrategy strategy) {
                return strategy.canHandle(snapshot, op);
            }
        });
    }

    private static <T> Collection<T> sort(Collection<T> collection, final CanHandle<T> canHandle) {
        if (collection.size() == 0)
            return null;

        TreeSet<T> resultSet = new TreeSet<T>(new Comparator<T>() {
            @Override
            public int compare(T o1, T o2) {
                int i1 = canHandle.canHandle(o1).ordinal();
                int i2 = canHandle.canHandle(o2).ordinal();
                return new Integer(i2).compareTo(new Integer(i1));
            }
        });

        for ( T test : collection ) {
            if ( canHandle.canHandle(test) != StrategyPriority.CANT_HANDLE ) {
                resultSet.add(test);
            }
        }

        return resultSet;
    }
    
    private static <T> T first(Collection<T> resultSet) {
        return resultSet.size() == 0 ? null : resultSet.iterator().next();
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

}

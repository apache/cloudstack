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

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.cloud.host.Host;
import com.cloud.storage.Snapshot;

public class StrategyPriority {
    public enum Priority {
        CANT_HANDLE,
        DEFAULT,
        HYPERVISOR,
        PLUGIN,
        HIGHEST
    }

    public static void sortStrategies(List<SnapshotStrategy> strategies, Snapshot snapshot) {
        Collections.sort(strategies, new SnapshotStrategyComparator(snapshot));
    }

    public static void sortStrategies(List<DataMotionStrategy> strategies, DataObject srcData, DataObject destData) {
        Collections.sort(strategies, new DataMotionStrategyComparator(srcData, destData));
    }

    public static void sortStrategies(List<DataMotionStrategy> strategies, Map<VolumeInfo, DataStore> volumeMap, Host srcHost, Host destHost) {
        Collections.sort(strategies, new DataMotionStrategyHostComparator(volumeMap, srcHost, destHost));
    }

    static class SnapshotStrategyComparator implements Comparator<SnapshotStrategy> {

        Snapshot snapshot;

        public SnapshotStrategyComparator(Snapshot snapshot) {
            this.snapshot = snapshot;
        }

        @Override
        public int compare(SnapshotStrategy o1, SnapshotStrategy o2) {
            int i1 = o1.canHandle(snapshot).ordinal();
            int i2 = o2.canHandle(snapshot).ordinal();
            return Integer.compare(i2, i1);
        }
    }

    static class DataMotionStrategyComparator implements Comparator<DataMotionStrategy> {

        DataObject srcData, destData;

        public DataMotionStrategyComparator(DataObject srcData, DataObject destData) {
            this.srcData = srcData;
            this.destData = destData;
        }

        @Override
        public int compare(DataMotionStrategy o1, DataMotionStrategy o2) {
            int i1 = o1.canHandle(srcData, destData).ordinal();
            int i2 = o2.canHandle(srcData, destData).ordinal();
            return Integer.compare(i2, i1);
        }
    }

    static class DataMotionStrategyHostComparator implements Comparator<DataMotionStrategy> {

        Host srcHost, destHost;
        Map<VolumeInfo, DataStore> volumeMap;

        public DataMotionStrategyHostComparator(Map<VolumeInfo, DataStore> volumeMap, Host srcHost, Host destHost) {
            this.volumeMap = volumeMap;
            this.srcHost = srcHost;
            this.destHost = destHost;
        }

        @Override
        public int compare(DataMotionStrategy o1, DataMotionStrategy o2) {
            int i1 = o1.canHandle(volumeMap, srcHost, destHost).ordinal();
            int i2 = o2.canHandle(volumeMap, srcHost, destHost).ordinal();
            return Integer.compare(i2, i1);
        }
    }
}

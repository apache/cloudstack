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
package org.apache.cloudstack.engine.subsystem.api.storage;

import java.util.Map;

import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotStrategy.SnapshotOperation;

import com.cloud.host.Host;
import com.cloud.storage.Snapshot;
import com.cloud.vm.snapshot.VMSnapshot;

public interface StorageStrategyFactory {

    DataMotionStrategy getDataMotionStrategy(DataObject srcData, DataObject destData);

    DataMotionStrategy getDataMotionStrategy(Map<VolumeInfo, DataStore> volumeMap, Host srcHost, Host destHost);

    SnapshotStrategy getSnapshotStrategy(Snapshot snapshot, SnapshotOperation op);

    VMSnapshotStrategy getVmSnapshotStrategy(VMSnapshot vmSnapshot);

    /**
     * Used only for KVM hypervisors when allocating a VM snapshot
     * @param vmId the ID of the virtual machine
     * @param rootPoolId volume pool ID
     * @param snapshotMemory for VM snapshots with memory
     * @return VMSnapshotStrategy
     */
    VMSnapshotStrategy getVmSnapshotStrategy(Long vmId, Long rootPoolId, boolean snapshotMemory);
}

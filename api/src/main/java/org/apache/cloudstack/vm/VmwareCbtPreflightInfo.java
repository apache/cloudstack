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
package org.apache.cloudstack.vm;

import java.util.Collections;
import java.util.List;

public class VmwareCbtPreflightInfo {

    private final String sourceVmName;
    private final String sourceVmMor;
    private final Boolean changeTrackingSupported;
    private final Boolean changeTrackingEnabled;
    private final Boolean consolidationNeeded;
    private final Integer existingSnapshotCount;
    private final List<VmwareCbtPreflightDiskInfo> disks;
    private final Integer cpuCores;
    private final Integer cpuSpeed;
    private final Integer memoryMb;
    private final String operatingSystemId;
    private final String operatingSystem;

    public VmwareCbtPreflightInfo(String sourceVmName, String sourceVmMor,
                                  Boolean changeTrackingSupported, Boolean changeTrackingEnabled,
                                  Boolean consolidationNeeded, Integer existingSnapshotCount,
                                  List<VmwareCbtPreflightDiskInfo> disks,
                                  Integer cpuCores, Integer cpuSpeed, Integer memoryMb) {
        this(sourceVmName, sourceVmMor, changeTrackingSupported, changeTrackingEnabled, consolidationNeeded,
                existingSnapshotCount, disks, cpuCores, cpuSpeed, memoryMb, null, null);
    }

    public VmwareCbtPreflightInfo(String sourceVmName, String sourceVmMor,
                                  Boolean changeTrackingSupported, Boolean changeTrackingEnabled,
                                  Boolean consolidationNeeded, Integer existingSnapshotCount,
                                  List<VmwareCbtPreflightDiskInfo> disks,
                                  Integer cpuCores, Integer cpuSpeed, Integer memoryMb,
                                  String operatingSystemId, String operatingSystem) {
        this.sourceVmName = sourceVmName;
        this.sourceVmMor = sourceVmMor;
        this.changeTrackingSupported = changeTrackingSupported;
        this.changeTrackingEnabled = changeTrackingEnabled;
        this.consolidationNeeded = consolidationNeeded;
        this.existingSnapshotCount = existingSnapshotCount;
        this.disks = disks == null ? Collections.emptyList() : Collections.unmodifiableList(disks);
        this.cpuCores = cpuCores;
        this.cpuSpeed = cpuSpeed;
        this.memoryMb = memoryMb;
        this.operatingSystemId = operatingSystemId;
        this.operatingSystem = operatingSystem;
    }

    public String getSourceVmName() {
        return sourceVmName;
    }

    public String getSourceVmMor() {
        return sourceVmMor;
    }

    public Boolean getChangeTrackingSupported() {
        return changeTrackingSupported;
    }

    public Boolean getChangeTrackingEnabled() {
        return changeTrackingEnabled;
    }

    public Boolean getConsolidationNeeded() {
        return consolidationNeeded;
    }

    public Integer getExistingSnapshotCount() {
        return existingSnapshotCount;
    }

    public List<VmwareCbtPreflightDiskInfo> getDisks() {
        return disks;
    }

    public Integer getCpuCores() {
        return cpuCores;
    }

    public Integer getCpuSpeed() {
        return cpuSpeed;
    }

    public Integer getMemoryMb() {
        return memoryMb;
    }

    public String getOperatingSystemId() {
        return operatingSystemId;
    }

    public String getOperatingSystem() {
        return operatingSystem;
    }
}

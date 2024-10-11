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

package com.cloud.storage;

public class ResizeVolumePayload {
    public final Long newSize;
    public final Long newMinIops;
    public final Long newMaxIops;
    public Long newDiskOfferingId;
    public final Integer newHypervisorSnapshotReserve;
    public final boolean shrinkOk;
    public final String instanceName;
    public final long[] hosts;
    public final boolean isManaged;

    public ResizeVolumePayload(Long newSize, Long newMinIops, Long newMaxIops, Integer newHypervisorSnapshotReserve, boolean shrinkOk,
                               String instanceName, long[] hosts, boolean isManaged) {
        this.newSize = newSize;
        this.newMinIops = newMinIops;
        this.newMaxIops = newMaxIops;
        this.newHypervisorSnapshotReserve = newHypervisorSnapshotReserve;
        this.shrinkOk = shrinkOk;
        this.instanceName = instanceName;
        this.hosts = hosts;
        this.isManaged = isManaged;
        this.newDiskOfferingId = null;
    }

    public ResizeVolumePayload(Long newSize, Long newMinIops, Long newMaxIops, Long newDiskOfferingId, Integer newHypervisorSnapshotReserve, boolean shrinkOk,
            String instanceName, long[] hosts, boolean isManaged) {
        this(newSize, newMinIops, newMaxIops, newHypervisorSnapshotReserve, shrinkOk, instanceName, hosts, isManaged);
        this.newDiskOfferingId = newDiskOfferingId;
    }

    public Long getNewDiskOfferingId() {
        return newDiskOfferingId;
    }

    public void setNewDiskOfferingId(Long newDiskOfferingId) {
        this.newDiskOfferingId = newDiskOfferingId;
    }
}

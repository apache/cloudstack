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
package org.apache.cloudstack.storage.datastore.adapter;

public class ProviderVolumeStorageStats {
    /**
     * Total capacity in bytes currently physically used on the storage system within the scope of given API configuration
     */
    private long capacityInBytes;
    /**
     * Virtual amount of bytes allocated for use.  Typically what the users of the volume think they have before
     * any compression, deduplication, or thin-provisioning semantics are accounted for.
     */
    private Long virtualUsedInBytes;
    /**
     * Actual physical bytes used on the storage system within the scope of the given API configuration
     */
    private Long actualUsedInBytes;
    /**
     * Current IOPS
     */
    private Long iops;
    /**
     * Current raw throughput
     */
    private Long throughput;
    public Long getVirtualUsedInBytes() {
        return virtualUsedInBytes;
    }
    public void setVirtualUsedInBytes(Long virtualUsedInBytes) {
        this.virtualUsedInBytes = virtualUsedInBytes;
    }
    public Long getActualUsedInBytes() {
        return actualUsedInBytes;
    }
    public void setActualUsedInBytes(Long actualUsedInBytes) {
        this.actualUsedInBytes = actualUsedInBytes;
    }
    public Long getIops() {
        return iops;
    }
    public void setIops(Long iops) {
        this.iops = iops;
    }
    public Long getThroughput() {
        return throughput;
    }
    public void setThroughput(Long throughput) {
        this.throughput = throughput;
    }
    public Long getCapacityInBytes() {
        return capacityInBytes;
    }
    public void setCapacityInBytes(Long capacityInBytes) {
        this.capacityInBytes = capacityInBytes;
    }
}

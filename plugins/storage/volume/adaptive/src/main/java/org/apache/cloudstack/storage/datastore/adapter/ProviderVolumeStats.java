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

public class ProviderVolumeStats {
    private Long allocatedInBytes;
    private Long virtualUsedInBytes;
    private Long actualUsedInBytes;
    private Long iops;
    private Long throughput;
    public Long getAllocatedInBytes() {
        return allocatedInBytes;
    }
    public void setAllocatedInBytes(Long allocatedInBytes) {
        this.allocatedInBytes = allocatedInBytes;
    }
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
}

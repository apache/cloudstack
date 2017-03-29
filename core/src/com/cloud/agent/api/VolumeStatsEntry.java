//
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
//

package com.cloud.agent.api;

import com.cloud.storage.VolumeStats;

public class VolumeStatsEntry implements VolumeStats {
    String volumeUuid;
    long physicalsize = 0;
    long virtualSize = 0;

    public VolumeStatsEntry(String volumeUuid, long physicalsize, long virtualSize) {
        this.volumeUuid = volumeUuid;
        this.physicalsize = physicalsize;
        this.virtualSize = virtualSize;
    }

    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }

    public long getPhysicalSize() {
        return physicalsize;
    }

    public void setPhysicalSize(long size) {
        this.physicalsize = size;
    }

    public long getVirtualSize() {
        return virtualSize;
    }

    public void setVirtualSize(long virtualSize) {
        this.virtualSize = virtualSize;
    }

    @Override
    public String toString() {
        return "VolumeStatsEntry [volumeUuid=" + volumeUuid + ", size=" + physicalsize + ", virtualSize=" + virtualSize + "]";
    }

}

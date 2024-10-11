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

import com.cloud.agent.api.LogLevel.Log4jLevel;
import com.cloud.storage.Storage.StoragePoolType;

@LogLevel(Log4jLevel.Trace)
public class GetVolumeStatAnswer extends Answer {
    String poolUuid;
    StoragePoolType poolType;
    String volumePath;
    long size = 0;
    long virtualSize = 0;

    public GetVolumeStatAnswer(GetVolumeStatCommand cmd, long size, long virtualSize) {
        super(cmd, true, "");
        this.poolUuid = cmd.getPoolUuid();
        this.poolType = cmd.getPoolType();
        this.volumePath = cmd.getVolumePath();
        this.size = size;
        this.virtualSize = virtualSize;
    }

    public GetVolumeStatAnswer(GetVolumeStatCommand cmd, boolean success, String details) {
        super(cmd, success, details);
    }

    protected GetVolumeStatAnswer() {
        //no-args constructor for json serialization-deserialization
    }

    public String getPoolUuid() {
        return poolUuid;
    }

    public void setPoolUuid(String poolUuid) {
        this.poolUuid = poolUuid;
    }

    public StoragePoolType getPoolType() {
        return poolType;
    }

    public void setPoolType(StoragePoolType poolType) {
        this.poolType = poolType;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public long getVirtualSize() {
        return virtualSize;
    }

    public void setVirtualSize(long virtualSize) {
        this.virtualSize = virtualSize;
    }

    public String getString() {
        return "GetVolumeStatAnswer [poolUuid=" + poolUuid + ", poolType=" + poolType + ", volumePath=" + volumePath + ", size=" + size + ", virtualSize=" + virtualSize + "]";
    }
}

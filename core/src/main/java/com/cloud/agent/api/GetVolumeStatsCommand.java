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

import java.util.List;

import com.cloud.agent.api.LogLevel.Log4jLevel;
import com.cloud.storage.Storage.StoragePoolType;

@LogLevel(Log4jLevel.Trace)
public class GetVolumeStatsCommand extends Command {

    List<String> volumeUuids;
    StoragePoolType poolType;
    String poolUuid;

    protected GetVolumeStatsCommand() {
    }

    public GetVolumeStatsCommand(StoragePoolType poolType, String storeUuid, List<String> volumeUuids) {
        this.volumeUuids = volumeUuids;
        this.poolType = poolType;
        this.poolUuid = storeUuid;
    }

    public List<String> getVolumeUuids() {
        return volumeUuids;
    }

    public void setVolumeUuids(List<String> volumeUuids) {
        this.volumeUuids = volumeUuids;
    }

    public StoragePoolType getPoolType() {
        return poolType;
    }

    public void setPoolType(StoragePoolType poolType) {
        this.poolType = poolType;
    }

    public String getPoolUuid() {
        return poolUuid;
    }

    public void setPoolUuid(String storeUuid) {
        this.poolUuid = storeUuid;
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }

    public String getString() {
        return "GetVolumeStatsCommand [volumeUuids=" + volumeUuids + ", poolType=" + poolType + ", poolUuid=" + poolUuid + "]";
    }
}

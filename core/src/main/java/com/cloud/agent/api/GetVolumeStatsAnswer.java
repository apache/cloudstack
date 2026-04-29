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

import java.util.HashMap;

import com.cloud.agent.api.LogLevel.Log4jLevel;
import com.cloud.storage.Storage.StoragePoolType;

@LogLevel(Log4jLevel.Trace)
public class GetVolumeStatsAnswer extends Answer {

    String poolUuid;
    StoragePoolType poolType;
    HashMap<String, VolumeStatsEntry> volumeStats;

    public GetVolumeStatsAnswer(GetVolumeStatsCommand cmd, String details, HashMap<String, VolumeStatsEntry> volumeStats) {
        super(cmd, true, details);
        this.poolUuid = cmd.getPoolUuid();
        this.poolType = cmd.getPoolType();
        this.volumeStats = volumeStats;
    }

    protected GetVolumeStatsAnswer() {
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

    public HashMap<String, VolumeStatsEntry> getVolumeStats() {
        return volumeStats;
    }

    public void setVolumeStats(HashMap<String, VolumeStatsEntry> volumeStats) {
        this.volumeStats = volumeStats;
    }

    public String getString() {
        return "GetVolumeStatsAnswer [poolUuid=" + poolUuid + ", poolType=" + poolType + ", volumeStats=" + volumeStats + "]";
    }

}

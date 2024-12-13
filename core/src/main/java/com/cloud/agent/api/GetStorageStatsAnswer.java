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
import com.cloud.storage.StorageStats;

@LogLevel(Log4jLevel.Trace)
public class GetStorageStatsAnswer extends Answer implements StorageStats {
    protected GetStorageStatsAnswer() {
    }

    protected long usedBytes;

    protected long capacityBytes;

    protected Long capacityIops;

    protected Long usedIops;

    @Override
    public long getByteUsed() {
        return usedBytes;
    }

    @Override
    public long getCapacityBytes() {
        return capacityBytes;
    }

    @Override
    public Long getCapacityIops() {
        return capacityIops;
    }

    @Override
    public Long getUsedIops() {
        return usedIops;
    }

    public GetStorageStatsAnswer(GetStorageStatsCommand cmd, long capacityBytes, long usedBytes) {
        super(cmd, true, null);
        this.capacityBytes = capacityBytes;
        this.usedBytes = usedBytes;
    }

    public GetStorageStatsAnswer(GetStorageStatsCommand cmd, long capacityBytes, long usedBytes, Long capacityIops, Long usedIops) {
        super(cmd, true, null);
        this.capacityBytes = capacityBytes;
        this.usedBytes = usedBytes;
        this.capacityIops = capacityIops;
        this.usedIops = usedIops;
    }

    public GetStorageStatsAnswer(GetStorageStatsCommand cmd, String details) {
        super(cmd, false, details);
    }
}

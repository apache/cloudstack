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
package com.cloud.agent.api;

import java.util.List;

import com.cloud.agent.api.to.VmwareCbtDiskSyncResultTO;

public class VmwareCbtMigrationAnswer extends Answer {

    private String migrationUuid;
    private int cycleNumber;
    private long changedBytes;
    private long dirtyRateBytesPerSecond;
    private long durationSeconds;
    private boolean readyForCutover;
    private List<VmwareCbtDiskSyncResultTO> diskResults;

    public VmwareCbtMigrationAnswer() {
        super();
    }

    public VmwareCbtMigrationAnswer(Command command, boolean result, String details, String migrationUuid) {
        super(command, result, details);
        this.migrationUuid = migrationUuid;
    }

    public VmwareCbtMigrationAnswer(Command command, boolean result, String details, String migrationUuid, int cycleNumber,
                                    long changedBytes, long dirtyRateBytesPerSecond, long durationSeconds,
                                    boolean readyForCutover, List<VmwareCbtDiskSyncResultTO> diskResults) {
        super(command, result, details);
        this.migrationUuid = migrationUuid;
        this.cycleNumber = cycleNumber;
        this.changedBytes = changedBytes;
        this.dirtyRateBytesPerSecond = dirtyRateBytesPerSecond;
        this.durationSeconds = durationSeconds;
        this.readyForCutover = readyForCutover;
        this.diskResults = diskResults;
    }

    public String getMigrationUuid() {
        return migrationUuid;
    }

    public int getCycleNumber() {
        return cycleNumber;
    }

    public long getChangedBytes() {
        return changedBytes;
    }

    public long getDirtyRateBytesPerSecond() {
        return dirtyRateBytesPerSecond;
    }

    public long getDurationSeconds() {
        return durationSeconds;
    }

    public boolean getReadyForCutover() {
        return readyForCutover;
    }

    public List<VmwareCbtDiskSyncResultTO> getDiskResults() {
        return diskResults;
    }
}

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
package com.cloud.agent.api.to;

import java.io.Serializable;

public class VmwareCbtDiskSyncResultTO implements Serializable {

    private String diskId;
    private String targetPath;
    private String changeId;
    private String snapshotMor;
    private long changedBytes;
    private long durationSeconds;
    private boolean result;
    private String details;

    public VmwareCbtDiskSyncResultTO() {
    }

    public VmwareCbtDiskSyncResultTO(String diskId, String targetPath, String changeId, String snapshotMor,
                                    long changedBytes, long durationSeconds, boolean result, String details) {
        this.diskId = diskId;
        this.targetPath = targetPath;
        this.changeId = changeId;
        this.snapshotMor = snapshotMor;
        this.changedBytes = changedBytes;
        this.durationSeconds = durationSeconds;
        this.result = result;
        this.details = details;
    }

    public String getDiskId() {
        return diskId;
    }

    public String getTargetPath() {
        return targetPath;
    }

    public String getChangeId() {
        return changeId;
    }

    public String getSnapshotMor() {
        return snapshotMor;
    }

    public long getChangedBytes() {
        return changedBytes;
    }

    public long getDurationSeconds() {
        return durationSeconds;
    }

    public boolean getResult() {
        return result;
    }

    public String getDetails() {
        return details;
    }
}

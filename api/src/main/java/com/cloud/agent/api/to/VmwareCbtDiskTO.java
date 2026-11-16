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

public class VmwareCbtDiskTO implements Serializable {

    private String diskId;
    private Integer diskDeviceKey;
    private String sourceDiskPath;
    private String datastoreName;
    private String targetPath;
    private String targetFormat;
    private String changeId;
    private String snapshotMor;
    private long capacityBytes;

    public VmwareCbtDiskTO() {
    }

    public VmwareCbtDiskTO(String diskId, Integer diskDeviceKey, String sourceDiskPath, String datastoreName,
                          String targetPath, String targetFormat, String changeId, String snapshotMor,
                          long capacityBytes) {
        this.diskId = diskId;
        this.diskDeviceKey = diskDeviceKey;
        this.sourceDiskPath = sourceDiskPath;
        this.datastoreName = datastoreName;
        this.targetPath = targetPath;
        this.targetFormat = targetFormat;
        this.changeId = changeId;
        this.snapshotMor = snapshotMor;
        this.capacityBytes = capacityBytes;
    }

    public String getDiskId() {
        return diskId;
    }

    public Integer getDiskDeviceKey() {
        return diskDeviceKey;
    }

    public String getSourceDiskPath() {
        return sourceDiskPath;
    }

    public String getDatastoreName() {
        return datastoreName;
    }

    public String getTargetPath() {
        return targetPath;
    }

    public String getTargetFormat() {
        return targetFormat;
    }

    public String getChangeId() {
        return changeId;
    }

    public String getSnapshotMor() {
        return snapshotMor;
    }

    public long getCapacityBytes() {
        return capacityBytes;
    }
}

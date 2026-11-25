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
package org.apache.cloudstack.vm;

public class VmwareCbtDiskInfo {

    private final String sourceDiskId;
    private final Integer sourceDiskDeviceKey;
    private final String label;
    private final String sourceDiskPath;
    private final String datastoreName;
    private final Long capacityBytes;
    private final String changeId;

    public VmwareCbtDiskInfo(String sourceDiskId, Integer sourceDiskDeviceKey, String label, String sourceDiskPath,
                             String datastoreName, Long capacityBytes, String changeId) {
        this.sourceDiskId = sourceDiskId;
        this.sourceDiskDeviceKey = sourceDiskDeviceKey;
        this.label = label;
        this.sourceDiskPath = sourceDiskPath;
        this.datastoreName = datastoreName;
        this.capacityBytes = capacityBytes;
        this.changeId = changeId;
    }

    public String getSourceDiskId() {
        return sourceDiskId;
    }

    public Integer getSourceDiskDeviceKey() {
        return sourceDiskDeviceKey;
    }

    public String getLabel() {
        return label;
    }

    public String getSourceDiskPath() {
        return sourceDiskPath;
    }

    public String getDatastoreName() {
        return datastoreName;
    }

    public Long getCapacityBytes() {
        return capacityBytes;
    }

    public String getChangeId() {
        return changeId;
    }
}

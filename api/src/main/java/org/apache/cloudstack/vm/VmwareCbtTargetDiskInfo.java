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

public class VmwareCbtTargetDiskInfo {

    private final String sourceDiskId;
    private final String targetPath;
    private final String targetFormat;
    private final String changeId;
    private final String snapshotMor;

    public VmwareCbtTargetDiskInfo(String sourceDiskId, String targetPath, String targetFormat,
                                   String changeId, String snapshotMor) {
        this.sourceDiskId = sourceDiskId;
        this.targetPath = targetPath;
        this.targetFormat = targetFormat;
        this.changeId = changeId;
        this.snapshotMor = snapshotMor;
    }

    public String getSourceDiskId() {
        return sourceDiskId;
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
}

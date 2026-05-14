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

import java.util.Collections;
import java.util.List;

public class VmwareCbtChangedDiskInfo {

    private final String sourceDiskId;
    private final String nextChangeId;
    private final List<VmwareCbtChangedBlockInfo> changedBlocks;

    public VmwareCbtChangedDiskInfo(String sourceDiskId, String nextChangeId,
                                    List<VmwareCbtChangedBlockInfo> changedBlocks) {
        this.sourceDiskId = sourceDiskId;
        this.nextChangeId = nextChangeId;
        this.changedBlocks = changedBlocks == null ? Collections.emptyList() : changedBlocks;
    }

    public String getSourceDiskId() {
        return sourceDiskId;
    }

    public String getNextChangeId() {
        return nextChangeId;
    }

    public List<VmwareCbtChangedBlockInfo> getChangedBlocks() {
        return changedBlocks;
    }
}

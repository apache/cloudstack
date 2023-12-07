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

package org.apache.cloudstack.storage.command;

import org.apache.cloudstack.storage.to.SnapshotObjectTO;

/*
Command to get the list of snapshot files for copying a snapshot to a different zone
 */

public class QuerySnapshotZoneCopyCommand extends StorageSubSystemCommand {

    private SnapshotObjectTO snapshot;

    public QuerySnapshotZoneCopyCommand(final SnapshotObjectTO snapshot) {
        super();
        this.snapshot = snapshot;
    }

    public SnapshotObjectTO getSnapshot() {
        return snapshot;
    }

    public void setSnapshot(final SnapshotObjectTO snapshot) {
        this.snapshot = snapshot;
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }

    @Override
    public void setExecuteInSequence(boolean inSeq) {}
}

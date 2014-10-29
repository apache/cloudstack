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

import com.cloud.agent.api.to.StorageFilerTO;
import com.cloud.storage.StoragePool;

public class ManageSnapshotCommand extends Command {
    // XXX: Should be an enum
    // XXX: Anyway there is something called inheritance in Java
    public static final String CREATE_SNAPSHOT = "-c";
    public static final String DESTROY_SNAPSHOT = "-d";

    private String _commandSwitch;

    // Information about the volume that the snapshot is based on
    private String _volumePath = null;
    StorageFilerTO _pool;

    // Information about the snapshot
    private String _snapshotPath = null;
    private String _snapshotName = null;
    private long _snapshotId;
    private String _vmName = null;

    public ManageSnapshotCommand() {
    }

    public ManageSnapshotCommand(long snapshotId, String volumePath, StoragePool pool, String preSnapshotPath, String snapshotName, String vmName) {
        _commandSwitch = ManageSnapshotCommand.CREATE_SNAPSHOT;
        _volumePath = volumePath;
        _pool = new StorageFilerTO(pool);
        _snapshotPath = preSnapshotPath;
        _snapshotName = snapshotName;
        _snapshotId = snapshotId;
        _vmName = vmName;
    }

    public ManageSnapshotCommand(long snapshotId, String snapshotPath) {
        _commandSwitch = ManageSnapshotCommand.DESTROY_SNAPSHOT;
        _snapshotPath = snapshotPath;
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }

    public String getCommandSwitch() {
        return _commandSwitch;
    }

    public String getVolumePath() {
        return _volumePath;
    }

    public StorageFilerTO getPool() {
        return _pool;
    }

    public String getSnapshotPath() {
        return _snapshotPath;
    }

    public String getSnapshotName() {
        return _snapshotName;
    }

    public long getSnapshotId() {
        return _snapshotId;
    }

    public String getVmName() {
        return _vmName;
    }

}

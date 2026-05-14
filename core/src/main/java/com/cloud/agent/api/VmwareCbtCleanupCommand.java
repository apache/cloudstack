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

import com.cloud.agent.api.to.VmwareCbtDiskTO;

public class VmwareCbtCleanupCommand extends Command {

    private String migrationUuid;
    private List<VmwareCbtDiskTO> disks;
    private boolean removeTemporarySnapshots;
    private boolean removeNbdExports;
    private boolean removePartialTargetDisks;

    public VmwareCbtCleanupCommand() {
    }

    public VmwareCbtCleanupCommand(String migrationUuid, List<VmwareCbtDiskTO> disks, boolean removeTemporarySnapshots,
                                   boolean removeNbdExports, boolean removePartialTargetDisks) {
        this.migrationUuid = migrationUuid;
        this.disks = disks;
        this.removeTemporarySnapshots = removeTemporarySnapshots;
        this.removeNbdExports = removeNbdExports;
        this.removePartialTargetDisks = removePartialTargetDisks;
    }

    public String getMigrationUuid() {
        return migrationUuid;
    }

    public List<VmwareCbtDiskTO> getDisks() {
        return disks;
    }

    public boolean getRemoveTemporarySnapshots() {
        return removeTemporarySnapshots;
    }

    public boolean getRemoveNbdExports() {
        return removeNbdExports;
    }

    public boolean getRemovePartialTargetDisks() {
        return removePartialTargetDisks;
    }

    @Override
    public boolean executeInSequence() {
        return true;
    }
}

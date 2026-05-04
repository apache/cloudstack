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
package org.apache.cloudstack.backup;

import com.cloud.agent.api.Command;
import com.cloud.utils.Pair;
import org.apache.cloudstack.storage.to.BackupDeltaTO;
import org.apache.cloudstack.storage.to.VolumeObjectTO;

import java.util.Set;

public class RestoreKbossBackupCommand extends Command {

    private Set<BackupDeltaTO> deltasToRemove;

    private Set<Pair<BackupDeltaTO, VolumeObjectTO>> backupAndVolumePairs;

    private Set<String> secondaryStorageUrls;

    private boolean quickRestore;

    public RestoreKbossBackupCommand(Set<BackupDeltaTO> deltasToRemove, Set<Pair<BackupDeltaTO, VolumeObjectTO>> backupAndVolumePairs, Set<String> secondaryStorageUrls,
            boolean quickRestore) {
        this.deltasToRemove = deltasToRemove;
        this.backupAndVolumePairs = backupAndVolumePairs;
        this.secondaryStorageUrls = secondaryStorageUrls;
        this.quickRestore = quickRestore;
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }

    public Set<BackupDeltaTO> getDeltasToRemove() {
        return deltasToRemove;
    }

    public Set<Pair<BackupDeltaTO, VolumeObjectTO>> getBackupAndVolumePairs() {
        return backupAndVolumePairs;
    }

    public Set<String> getSecondaryStorageUrls() {
        return secondaryStorageUrls;
    }

    public boolean isQuickRestore() {
        return quickRestore;
    }
}

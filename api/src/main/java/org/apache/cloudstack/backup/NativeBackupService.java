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
package org.apache.cloudstack.backup;

import com.cloud.agent.api.Command;
import com.cloud.agent.api.to.DataTO;
import com.cloud.storage.Volume;
import com.cloud.uservm.UserVm;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.snapshot.VMSnapshot;
import org.apache.cloudstack.api.response.ExtractResponse;

import java.util.Set;

public interface NativeBackupService {

    void configureChainInfo(DataTO volumeTo, Command cmd);

    void cleanupBackupMetadata(long volumeId);

    void prepareVolumeForDetach(Volume volume, VirtualMachine virtualMachine);

    void prepareVolumeForMigration(Volume volume);

    void prepareVmForSnapshotRevert(VMSnapshot vmSnapshot);

    void updateVolumeId(long oldVolumeId, long newVolumeId);

    Set<String> getSecondaryStorageUrls(UserVm userVm);

    boolean startBackupCompression(long backupId, long hostId, long zoneId);

    boolean finalizeBackupCompression(long backupId, long hostId, long zoneId);

    boolean validateBackup(long backupId, long hostId, long zoneId);

    ExtractResponse downloadScreenshot(long backupId);
}

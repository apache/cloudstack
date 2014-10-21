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

import com.cloud.agent.api.LogLevel.Log4jLevel;
import com.cloud.agent.api.to.S3TO;
import com.cloud.agent.api.to.StorageFilerTO;
import com.cloud.agent.api.to.SwiftTO;
import com.cloud.storage.StoragePool;

/**
 * This currently assumes that both primary and secondary storage are mounted on the XenServer.
 */
public class BackupSnapshotCommand extends SnapshotCommand {
    private String prevSnapshotUuid;
    private String prevBackupUuid;
    private boolean isVolumeInactive;
    private String vmName;
    private Long snapshotId;
    @LogLevel(Log4jLevel.Off)
    private SwiftTO swift;
    private S3TO s3;
    StorageFilerTO pool;
    private Long secHostId;

    protected BackupSnapshotCommand() {

    }

    /**
     * @param primaryStoragePoolNameLabel   The UUID of the primary storage Pool
     * @param secondaryStoragePoolURL  This is what shows up in the UI when you click on Secondary storage.
     * @param snapshotUuid             The UUID of the snapshot which is going to be backed up
     * @param prevSnapshotUuid         The UUID of the previous snapshot for this volume. This will be destroyed on the primary storage.
     * @param prevBackupUuid           This is the UUID of the vhd file which was last backed up on secondary storage.
     * @param firstBackupUuid          This is the backup of the first ever snapshot taken by the volume.
     * @param isFirstSnapshotOfRootVolume true if this is the first snapshot of a root volume. Set the parent of the backup to null.
     * @param isVolumeInactive         True if the volume belongs to a VM that is not running or is detached.
     * @param secHostId                This is the Id of the secondary storage.
     */
    public BackupSnapshotCommand(String secondaryStoragePoolURL, Long dcId, Long accountId, Long volumeId, Long snapshotId, Long secHostId, String volumePath,
            StoragePool pool, String snapshotUuid, String snapshotName, String prevSnapshotUuid, String prevBackupUuid, boolean isVolumeInactive, String vmName, int wait) {
        super(pool, secondaryStoragePoolURL, snapshotUuid, snapshotName, dcId, accountId, volumeId);
        this.snapshotId = snapshotId;
        this.prevSnapshotUuid = prevSnapshotUuid;
        this.prevBackupUuid = prevBackupUuid;
        this.isVolumeInactive = isVolumeInactive;
        this.vmName = vmName;
        this.secHostId = secHostId;
        setVolumePath(volumePath);
        setWait(wait);
    }

    public String getPrevSnapshotUuid() {
        return prevSnapshotUuid;
    }

    public String getPrevBackupUuid() {
        return prevBackupUuid;
    }

    public boolean isVolumeInactive() {
        return isVolumeInactive;
    }

    public String getVmName() {
        return vmName;
    }

    public SwiftTO getSwift() {
        return swift;
    }

    public void setSwift(SwiftTO swift) {
        this.swift = swift;
    }

    public S3TO getS3() {
        return s3;
    }

    public void setS3(S3TO s3) {
        this.s3 = s3;
    }

    public Long getSnapshotId() {
        return snapshotId;
    }

    public Long getSecHostId() {
        return secHostId;
    }
}

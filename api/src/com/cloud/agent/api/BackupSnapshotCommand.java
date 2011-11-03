/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package com.cloud.agent.api;

import com.cloud.agent.api.LogLevel.Log4jLevel;
import com.cloud.agent.api.to.StorageFilerTO;
import com.cloud.agent.api.to.SwiftTO;
import com.cloud.storage.StoragePool;

/**
 * When a snapshot of a VDI is taken, it creates two new files,
 * a 'base copy' which contains all the new data since the time of the last snapshot and an 'empty snapshot' file.
 * Any new data is again written to the VDI with the same UUID. 
 * This class issues a command for copying the 'base copy' vhd file to secondary storage.
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
    StorageFilerTO pool;

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
     */
    public BackupSnapshotCommand(String primaryStoragePoolNameLabel,
                                 String secondaryStoragePoolURL,
                                 Long   dcId,
                                 Long   accountId,
                                 Long   volumeId,
                                 Long   snapshotId,
                                 String volumePath,
                                 StoragePool pool,
                                 String snapshotUuid,
                                 String snapshotName,
                                 String prevSnapshotUuid,
                                 String prevBackupUuid,
                                 boolean isVolumeInactive,
                                 String vmName,
                                 int wait) 
    {
        super(primaryStoragePoolNameLabel, secondaryStoragePoolURL, snapshotUuid, snapshotName, dcId, accountId, volumeId);
        this.snapshotId = snapshotId;
        this.prevSnapshotUuid = prevSnapshotUuid;
        this.prevBackupUuid = prevBackupUuid;
        this.isVolumeInactive = isVolumeInactive;
        this.vmName = vmName;
        this.pool = new StorageFilerTO(pool);
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

    public Long getSnapshotId() {
        return snapshotId;
    }

    public StorageFilerTO getPool() {
        return pool;
    }
}
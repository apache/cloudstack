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

/**
 * This class encapsulates the information required for creating a new Volume from the backup of a snapshot. 
 * This currently assumes that both primary and secondary storage are mounted on the XenServer.  
 */
public class CreateVolumeFromSnapshotCommand extends SnapshotCommand {
    private String templatePath;
    
    protected CreateVolumeFromSnapshotCommand() {
        
    }
    
    /**
     * Given the UUID of a backed up snapshot VHD file on the secondary storage, the execute of this command does
     * 1) Get the parent chain of this VHD all the way up to the root, say VHDList
     * 2) Copy all the files in the VHDlist to some temp location
     * 3) Coalesce all the VHDs to one VHD which contains all the data of the volume. This invokes the DeletePreviousBackupCommand for each VHD
     * 4) Rename the UUID of this VHD
     * 5) Move this VHD to primary storage
     * 6) Rename the UUID of this VHD to a new one
     * 7) Introduce a VDI with this new VHD and return it. 
     * @param primaryStoragePoolNameLabel   The primary storage Pool
     * @param secondaryStoragePoolURL This is what shows up in the UI when you click on Secondary storage. 
     *                                 In the code, it is present as: In the vmops.host_details table, there is a field mount.parent. This is the value of that field
     *                                 If you have better ideas on how to get it, you are welcome.
     * @param backedUpSnapshotUuid     This is the UUID of the vhd file corresponding to the snapshot id from which the data has to be restored.
     *                                 It may not be the UUID of the base copy of the snapshot, if no data was written since last snapshot.
     * @param templatePath             The install path of the template VHD on the secondary, if this a root volume                                 
     */
    public CreateVolumeFromSnapshotCommand(String primaryStoragePoolNameLabel,
                                           String secondaryStoragePoolURL,
                                           Long   dcId,
                                           Long   accountId,
                                           Long   volumeId,
                                           String backedUpSnapshotUuid,
                                           String templatePath) 
    {
        super(primaryStoragePoolNameLabel, secondaryStoragePoolURL, backedUpSnapshotUuid, dcId, accountId, volumeId);
        this.templatePath = templatePath;
    }

    /**
     * @return the templatePath
     */
    public String getTemplatePath() {
        return templatePath;
    }
    
}
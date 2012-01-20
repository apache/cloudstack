/**
 *  Copyright (C) 2012 Citrix.com, Inc.  All rights reserved.
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

import java.util.List;

public class CleanupSnapshotBackupCommand extends Command {
        private String secondaryStoragePoolURL;
        private Long   dcId;
        private Long   accountId;
        private Long   volumeId;
        private List<String> validBackupUUIDs;

    protected CleanupSnapshotBackupCommand() {
        
    }
                        
     /*       
     * @param secondaryStoragePoolURL    This is what shows up in the UI when you click on Secondary storage. 
     *                                    In the code, it is present as: In the vmops.host_details table, there is a field mount.parent. This is the value of that field
     *                                    If you have better ideas on how to get it, you are welcome. 
     * @param validBackupUUID             The VHD which are valid   
     */
    public CleanupSnapshotBackupCommand(String secondaryStoragePoolURL,
                                       Long   dcId,
                                       Long   accountId,
                                       Long   volumeId,
                                       List<String> validBackupUUIDs) 
    {
        this.secondaryStoragePoolURL = secondaryStoragePoolURL;
        this.dcId = dcId;
        this.accountId = accountId;
        this.volumeId = volumeId;
        this.validBackupUUIDs = validBackupUUIDs;
    }

    public String getSecondaryStoragePoolURL() {
        return secondaryStoragePoolURL;
    }

    public Long getDcId() {
        return dcId;
    }

    public Long getAccountId() {
        return accountId;
    }

    public Long getVolumeId() {
        return volumeId;
    }

    public List<String> getValidBackupUUIDs() {
        return validBackupUUIDs;
    }
    
    @Override
    public boolean executeInSequence() {
        return false;
    }
}
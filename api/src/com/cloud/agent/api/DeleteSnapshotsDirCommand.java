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
 * This command encapsulates a primitive operation which enables coalescing the backed up VHD snapshots on the secondary server
 * This currently assumes that the secondary storage are mounted on the XenServer.  
 */
public class DeleteSnapshotsDirCommand extends Command {
    String secondaryStoragePoolURL;
    Long dcId;
    Long accountId;
    Long volumeId;
    
    protected DeleteSnapshotsDirCommand() {
        
    }
    
    public DeleteSnapshotsDirCommand(String secondaryStoragePoolURL,
 Long dcId, Long accountId, Long volumeId)
    {
        this.secondaryStoragePoolURL = secondaryStoragePoolURL;
        this.dcId = dcId;
        this.accountId = accountId;
        this.volumeId = volumeId;
    }

    @Override
    public boolean executeInSequence() {
        return true;
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

}
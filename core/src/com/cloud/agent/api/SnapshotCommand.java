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
 * When a snapshot of a VDI is taken, it creates two new files,
 * a 'base copy' which contains all the new data since the time of the last snapshot and an 'empty snapshot' file.
 * Any new data is again written to the VDI with the same UUID. 
 * This class issues a command for copying the 'base copy' vhd file to secondary storage.
 * This currently assumes that both primary and secondary storage are mounted on the XenServer.  
 */
public class SnapshotCommand extends Command {
    private String primaryStoragePoolNameLabel;
    private String snapshotUuid;
    private String secondaryStoragePoolURL;
    private Long   dcId;
    private Long   accountId;
    private Long   volumeId;
    
    protected SnapshotCommand() {
        
    }
    
    /**
     * @param primaryStoragePoolNameLabel   The primary storage Pool
     * @param snapshotUuid             The UUID of the snapshot which is going to be backed up 
     * @param secondaryStoragePoolURL This is what shows up in the UI when you click on Secondary storage. 
     *                                 In the code, it is present as: In the vmops.host_details table, there is a field mount.parent. This is the value of that field
     *                                 If you have better ideas on how to get it, you are welcome.
     */
    public SnapshotCommand(String primaryStoragePoolNameLabel,
                           String secondaryStoragePoolURL,
                           String snapshotUuid,
                           Long   dcId,
                           Long   accountId,
                           Long   volumeId) 
    {
        this.primaryStoragePoolNameLabel = primaryStoragePoolNameLabel;
        this.snapshotUuid = snapshotUuid;
        this.secondaryStoragePoolURL = secondaryStoragePoolURL;
        this.dcId = dcId;
        this.accountId = accountId;
        this.volumeId = volumeId;
    }

    /**
     * @return the primaryStoragePoolNameLabel
     */
    public String getPrimaryStoragePoolNameLabel() {
        return primaryStoragePoolNameLabel;
    }

    /**
     * @return the snapshotUuid
     */
    public String getSnapshotUuid() {
        return snapshotUuid;
    }
    
    /**
     * @return the secondaryStoragePoolURL
     */
    public String getSecondaryStoragePoolURL() {
        return secondaryStoragePoolURL;
    }

    
    public Long getDataCenterId() {
        return dcId;
    }

    public Long getAccountId() {
        return accountId;
    }

    public Long getVolumeId() {
        return volumeId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean executeInSequence() {
        return false;
    }

}
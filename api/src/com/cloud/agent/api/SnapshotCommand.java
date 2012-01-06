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
    protected String primaryStoragePoolNameLabel;
    private String snapshotUuid;
    private String snapshotName;
    private String secondaryStorageUrl;
    private Long   dcId;
    private Long   accountId;
    private Long   volumeId;
    private String volumePath;
    
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
 String secondaryStorageUrl,
                           String snapshotUuid,
                           String snapshotName,
                           Long   dcId,
                           Long   accountId,
                           Long   volumeId) 
    {
        this.primaryStoragePoolNameLabel = primaryStoragePoolNameLabel;
        this.snapshotUuid = snapshotUuid;
        this.secondaryStorageUrl = secondaryStorageUrl;
        this.dcId = dcId;
        this.accountId = accountId;
        this.volumeId = volumeId;
        this.snapshotName = snapshotName;
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
    
    public String getSnapshotName() {
    	return snapshotName;
    }
    
    /**
     * @return the secondaryStoragePoolURL
     */
    public String getSecondaryStorageUrl() {
        return secondaryStorageUrl;
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
    
    public String getVolumePath() {
    	return volumePath;
    }
    
    public void setVolumePath(String path) {
    	volumePath = path;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean executeInSequence() {
        return false;
    }

}
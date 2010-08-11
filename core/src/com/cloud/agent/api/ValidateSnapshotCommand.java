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

public class ValidateSnapshotCommand extends Command {
    private String primaryStoragePoolNameLabel;
    private String volumeUuid;
    private String firstBackupUuid;
    private String previousSnapshotUuid;
    private String templateUuid;
    
    protected ValidateSnapshotCommand() {
        
    }
    
    /**
     * @param primaryStoragePoolNameLabel   The primary storage Pool Name Label
     * @param volumeUuid        The UUID of the volume for which the snapshot was taken
     * @param firstBackupUuid   This UUID of the first snapshot that was ever taken for this volume, even it was deleted.
     * @param previousSnapshotUuid The UUID of the previous snapshot on the primary.
     * @param templateUuid      If this is a root volume and no snapshot has been taken for it, 
     *                          this is the UUID of the template VDI.  
     */
    public ValidateSnapshotCommand(String primaryStoragePoolNameLabel,
                                                 String volumeUuid,
                                                 String firstBackupUuid,
                                                 String previousSnapshotUuid,
                                                 String templateUuid) 
    {
        this.primaryStoragePoolNameLabel = primaryStoragePoolNameLabel;
        this.volumeUuid                  = volumeUuid;
        this.firstBackupUuid             = firstBackupUuid;
        this.previousSnapshotUuid        = previousSnapshotUuid;
        this.templateUuid                = templateUuid;
    }

    public String getPrimaryStoragePoolNameLabel() {
        return primaryStoragePoolNameLabel;
    }
    
    /**
     * @return the volumeUuid
     */
    public String getVolumeUuid() {
        return volumeUuid;
    }

    /**
     * @return the firstBackupUuid
     */
    public String getFirstBackupUuid() {
        return firstBackupUuid;
    }
    
    public String getPreviousSnapshotUuid() {
        return previousSnapshotUuid;
    }
    
    /**
     * @return the templateUuid
     */
    public String getTemplateUuid() {
        return templateUuid;
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }
}
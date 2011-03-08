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
public class UpgradeSnapshotCommand extends SnapshotCommand {
    private String version;
    private Long templateId;
    private Long tmpltAccountId;
    
    protected UpgradeSnapshotCommand() {
        
    }
    
    /**
     * @param primaryStoragePoolNameLabel   The UUID of the primary storage Pool
     * @param secondaryStoragePoolURL  This is what shows up in the UI when you click on Secondary storage.
     * @param snapshotUuid             The UUID of the snapshot which is going to be upgraded
     * @param _version          version for this snapshot                                 
     */
    public UpgradeSnapshotCommand(String primaryStoragePoolNameLabel,
                                 String secondaryStoragePoolURL,
                                 Long   dcId,
                                 Long   accountId,
                                 Long   volumeId,
                                 Long   templateId,
                                 Long   tmpltAccountId,
                                 String volumePath,
                                 String snapshotUuid,
                                 String snapshotName,
                                 String version)
    {
        super(primaryStoragePoolNameLabel, secondaryStoragePoolURL, snapshotUuid, snapshotName, dcId, accountId, volumeId);
        this.version = version;
        this.templateId = templateId;
        this.tmpltAccountId = tmpltAccountId;
    }

    public String getVersion() {
        return version;
    }

    public Long getTemplateId() {
        return templateId;
    }

    public Long getTmpltAccountId() {
        return tmpltAccountId;
    }   
}

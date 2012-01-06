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

public class CreatePrivateTemplateFromVolumeCommand extends SnapshotCommand {
    private String _vmName;
    private String _volumePath;
    private String _userSpecifiedName;
    private String _uniqueName;
    private long _templateId;
    private long _accountId;
    // For XenServer
    private String _secondaryStorageUrl;

    public CreatePrivateTemplateFromVolumeCommand() {
    }

    public CreatePrivateTemplateFromVolumeCommand(String StoragePoolUUID, String secondaryStorageUrl, long templateId, long accountId, String userSpecifiedName, String uniqueName, String volumePath, String vmName, int wait) {
        _secondaryStorageUrl = secondaryStorageUrl;
        _templateId = templateId;
        _accountId = accountId;
        _userSpecifiedName = userSpecifiedName;
        _uniqueName = uniqueName;
        _volumePath = volumePath;
        _vmName = vmName;
        primaryStoragePoolNameLabel = StoragePoolUUID;
        setWait(wait);
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }

    public String getSecondaryStorageUrl() {
        return _secondaryStorageUrl;
    }

    public String getTemplateName() {
        return _userSpecifiedName;
    }

    public String getUniqueName() {
        return _uniqueName;
    }

    public long getTemplateId() {
        return _templateId;
    }

    public String getVmName() {
        return _vmName;
    }

    public void setVolumePath(String _volumePath) {
        this._volumePath = _volumePath;
    }

    public String getVolumePath() {
        return _volumePath;
    }

    public Long getAccountId() {
        return _accountId;
    }

    public void setTemplateId(long templateId) {
        _templateId = templateId;
    }
}

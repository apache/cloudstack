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

package com.cloud.agent.api.storage;

public class CreatePrivateTemplateCommand extends StorageCommand {
    private String _snapshotFolder;
    private String _snapshotPath;
    private String _userFolder;
    private String _userSpecifiedName;
    private String _uniqueName;
    private long _templateId;
    private long _accountId;
    
    // For XenServer
    private String _secondaryStorageURL;
    private String _snapshotName;

    public CreatePrivateTemplateCommand() {}

    public CreatePrivateTemplateCommand(String secondaryStorageURL, long templateId, long accountId, String userSpecifiedName, String uniqueName, String snapshotFolder, String snapshotPath, String snapshotName, String userFolder) {
    	_secondaryStorageURL = secondaryStorageURL;
    	_templateId = templateId;
    	_accountId = accountId;
    	_userSpecifiedName = userSpecifiedName;
        _uniqueName = uniqueName;
        _snapshotFolder = snapshotFolder;
        _snapshotPath = snapshotPath;
        _snapshotName = snapshotName;
        _userFolder = userFolder;
    }

    @Override
    public boolean executeInSequence() { 
        return false;
    }
    
    public String getSecondaryStorageURL() {
    	return _secondaryStorageURL;
    }

    public String getTemplateName() {
        return _userSpecifiedName;
    }

    public String getUniqueName() {
        return _uniqueName;
    }

    public String getSnapshotFolder() {
        return _snapshotFolder;
    }

    public String getSnapshotPath() {
        return _snapshotPath;
    }
    
    public String getSnapshotName() {
    	return _snapshotName;
    }
    
    public String getUserFolder() {
        return _userFolder;
    }
    
    public long getTemplateId() {
    	return _templateId;
    }
    
    public long getAccountId() {
    	return _accountId;
    }
    
    public void setTemplateId(long templateId) {
    	_templateId = templateId;
    }
}

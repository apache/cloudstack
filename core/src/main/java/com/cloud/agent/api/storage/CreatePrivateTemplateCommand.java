//
// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
//

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

    public CreatePrivateTemplateCommand() {
    }

    public CreatePrivateTemplateCommand(String secondaryStorageURL, long templateId, long accountId, String userSpecifiedName, String uniqueName, String snapshotFolder,
            String snapshotPath, String snapshotName, String userFolder) {
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

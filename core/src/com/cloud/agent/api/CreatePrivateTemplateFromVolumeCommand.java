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

package com.cloud.agent.api;

import com.cloud.agent.api.to.StorageFilerTO;
import com.cloud.storage.StoragePool;

public class CreatePrivateTemplateFromVolumeCommand extends SnapshotCommand {
    private String _vmName;
    private String _volumePath;
    private String _userSpecifiedName;
    private String _uniqueName;
    private long _templateId;
    private long _accountId;
    StorageFilerTO _primaryPool;
    // For XenServer
    private String _secondaryStorageUrl;

    public CreatePrivateTemplateFromVolumeCommand() {
    }

    public CreatePrivateTemplateFromVolumeCommand(StoragePool pool, String secondaryStorageUrl, long templateId, long accountId, String userSpecifiedName,
            String uniqueName, String volumePath, String vmName, int wait) {
        _secondaryStorageUrl = secondaryStorageUrl;
        _templateId = templateId;
        _accountId = accountId;
        _userSpecifiedName = userSpecifiedName;
        _uniqueName = uniqueName;
        _volumePath = volumePath;
        _vmName = vmName;
        primaryStoragePoolNameLabel = pool.getUuid();
        _primaryPool = new StorageFilerTO(pool);
        setWait(wait);
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }

    @Override
    public StorageFilerTO getPool() {
        return _primaryPool;
    }

    @Override
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

    @Override
    public void setVolumePath(String volumePath) {
        this._volumePath = volumePath;
    }

    @Override
    public String getVolumePath() {
        return _volumePath;
    }

    @Override
    public Long getAccountId() {
        return _accountId;
    }

    public void setTemplateId(long templateId) {
        _templateId = templateId;
    }
}

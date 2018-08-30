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

import com.cloud.storage.StoragePool;

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
    public UpgradeSnapshotCommand(StoragePool pool, String secondaryStoragePoolURL, Long dcId, Long accountId, Long volumeId, Long templateId, Long tmpltAccountId,
            String volumePath, String snapshotUuid, String snapshotName, String version) {
        super(pool, secondaryStoragePoolURL, snapshotUuid, snapshotName, dcId, accountId, volumeId);
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

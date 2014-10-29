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
    public ValidateSnapshotCommand(String primaryStoragePoolNameLabel, String volumeUuid, String firstBackupUuid, String previousSnapshotUuid, String templateUuid) {
        this.primaryStoragePoolNameLabel = primaryStoragePoolNameLabel;
        this.volumeUuid = volumeUuid;
        this.firstBackupUuid = firstBackupUuid;
        this.previousSnapshotUuid = previousSnapshotUuid;
        this.templateUuid = templateUuid;
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

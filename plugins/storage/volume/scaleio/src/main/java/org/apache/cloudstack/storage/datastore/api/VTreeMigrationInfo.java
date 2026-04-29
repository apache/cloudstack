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

package org.apache.cloudstack.storage.datastore.api;

import com.cloud.utils.EnumUtils;

public class VTreeMigrationInfo {
    public enum MigrationStatus {
        NotInMigration,
        MigrationNormal,
        PendingRetry,
        InternalPausing,
        GracefullyPausing,
        ForcefullyPausing,
        Paused,
        PendingMigration,
        PendingRebalance,
        None
    }

    String sourceStoragePoolId;
    String destinationStoragePoolId;
    MigrationStatus migrationStatus;
    Long migrationQueuePosition;

    public String getSourceStoragePoolId() {
        return sourceStoragePoolId;
    }

    public void setSourceStoragePoolId(String sourceStoragePoolId) {
        this.sourceStoragePoolId = sourceStoragePoolId;
    }

    public String getDestinationStoragePoolId() {
        return destinationStoragePoolId;
    }

    public void setDestinationStoragePoolId(String destinationStoragePoolId) {
        this.destinationStoragePoolId = destinationStoragePoolId;
    }

    public MigrationStatus getMigrationStatus() {
        return migrationStatus;
    }

    public void setMigrationStatus(String migrationStatus) {
        this.migrationStatus = EnumUtils.fromString(MigrationStatus.class, migrationStatus, MigrationStatus.None);
    }

    public void setMigrationStatus(MigrationStatus migrationStatus) {
        this.migrationStatus = migrationStatus;
    }

    public Long getMigrationQueuePosition() {
        return migrationQueuePosition;
    }

    public void setMigrationQueuePosition(Long migrationQueuePosition) {
        this.migrationQueuePosition = migrationQueuePosition;
    }
}

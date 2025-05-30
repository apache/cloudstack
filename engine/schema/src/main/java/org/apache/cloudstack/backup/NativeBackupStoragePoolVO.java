//Licensed to the Apache Software Foundation (ASF) under one
//or more contributor license agreements.  See the NOTICE file
//distributed with this work for additional information
//regarding copyright ownership.  The ASF licenses this file
//to you under the Apache License, Version 2.0 (the
//"License"); you may not use this file except in compliance
//the License.  You may obtain a copy of the License at
//
//http://www.apache.org/licenses/LICENSE-2.0
//
//Unless required by applicable law or agreed to in writing,
//software distributed under the License is distributed on an
//"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
//KIND, either express or implied.  See the License for the
//specific language governing permissions and limitations
//under the License.
package org.apache.cloudstack.backup;

import org.apache.cloudstack.api.InternalIdentity;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "native_backup_pool_ref")
public class NativeBackupStoragePoolVO implements InternalIdentity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "backup_id")
    private long backupId;

    @Column(name = "storage_pool_id")
    private long storagePoolId;

    @Column(name = "volume_id")
    private long volumeId;

    @Column(name = "backup_delta_path")
    private String backupDeltaPath;

    @Column(name = "backup_parent_path")
    private String backupDeltaParentPath;

    public NativeBackupStoragePoolVO() {
    }

    public NativeBackupStoragePoolVO(long backupId, long storagePoolId, long volumeId, String backupDeltaPath, String backupDeltaParentPath) {
        this.backupId = backupId;
        this.storagePoolId = storagePoolId;
        this.volumeId = volumeId;
        this.backupDeltaPath = backupDeltaPath;
        this.backupDeltaParentPath = backupDeltaParentPath;
    }

    @Override
    public long getId() {
        return id;
    }

    public long getBackupId() {
        return backupId;
    }

    public long getStoragePoolId() {
        return storagePoolId;
    }

    public long getVolumeId() {
        return volumeId;
    }

    public String getBackupDeltaPath() {
        return backupDeltaPath;
    }

    public String getBackupDeltaParentPath() {
        return backupDeltaParentPath;
    }

    public void setBackupDeltaPath(String backupDeltaPath) {
        this.backupDeltaPath = backupDeltaPath;
    }

    public void setBackupDeltaParentPath(String backupDeltaParentPath) {
        this.backupDeltaParentPath = backupDeltaParentPath;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.JSON_STYLE);
    }
}

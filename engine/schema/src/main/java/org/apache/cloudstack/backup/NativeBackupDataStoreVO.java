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
@Table(name = "native_backup_store_ref")
public class NativeBackupDataStoreVO implements InternalIdentity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "backup_id")
    private long backupId;

    @Column(name = "volume_id")
    private long volumeId;

    @Column (name = "device_id")
    private long deviceId;

    @Column(name = "path")
    private String backupPath;

    public NativeBackupDataStoreVO() {
    }

    public NativeBackupDataStoreVO(long backupId, long volumeId, long deviceId, String backupPath) {
        this.backupId = backupId;
        this.volumeId = volumeId;
        this.deviceId = deviceId;
        this.backupPath = backupPath;
    }

    @Override
    public long getId() {
        return id;
    }

    public long getBackupId() {
        return backupId;
    }

    public long getVolumeId() {
        return volumeId;
    }

    public long getDeviceId() {
        return deviceId;
    }

    public String getBackupPath() {
        return backupPath;
    }

    public void setVolumeId(long volumeId) {
        this.volumeId = volumeId;
    }

    public void setBackupPath(String backupPath) {
        this.backupPath = backupPath;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.JSON_STYLE);
    }
}

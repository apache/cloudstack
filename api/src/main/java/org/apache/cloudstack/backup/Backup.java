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

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.acl.ControlledEntity;
import org.apache.cloudstack.api.Identity;
import org.apache.cloudstack.api.InternalIdentity;
import org.apache.commons.lang3.StringUtils;

import com.cloud.storage.Volume;

public interface Backup extends ControlledEntity, InternalIdentity, Identity {

    enum Status {
        Allocated, Queued, BackingUp, BackedUp, Error, Failed, Restoring, Removed, Expunged
    }

    class Metric {
        private Long backupSize = 0L;
        private Long dataSize = 0L;

        public Metric(final Long backupSize, final Long dataSize) {
            this.backupSize = backupSize;
            this.dataSize = dataSize;
        }

        public Long getBackupSize() {
            return backupSize;
        }

        public Long getDataSize() {
            return dataSize;
        }

        public void setBackupSize(Long backupSize) {
            this.backupSize = backupSize;
        }

        public void setDataSize(Long dataSize) {
            this.dataSize = dataSize;
        }
    }

    class RestorePoint {
        private String id;
        private Date created;
        private String type;
        private Long backupSize = 0L;
        private Long dataSize = 0L;

        public RestorePoint(String id, Date created, String type) {
            this.id = id;
            this.created = created;
            this.type = type;
        }

        public RestorePoint(String id, Date created, String type, Long backupSize, Long dataSize) {
            this(id, created, type);
            this.backupSize = backupSize;
            this.dataSize = dataSize;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public Date getCreated() {
            return this.created;
        }

        public void setCreated(Date created) {
            this.created = created;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Long getBackupSize() {
            return backupSize;
        }

        public void setBackupSize(Long backupSize) {
            this.backupSize = backupSize;
        }

        public Long getDataSize() {
            return dataSize;
        }

        public void setDataSize(Long dataSize) {
            this.dataSize = dataSize;
        }
    }

    class VolumeInfo {
        private String uuid;
        private Volume.Type type;
        private Long size;
        private String path;
        private Long deviceId;
        private String diskOfferingId;
        private Long minIops;
        private Long maxIops;

        public VolumeInfo(String uuid, String path, Volume.Type type, Long size, Long deviceId, String diskOfferingId, Long minIops, Long maxIops) {
            this.uuid = uuid;
            this.type = type;
            this.size = size;
            this.path = path;
            this.deviceId = deviceId;
            this.diskOfferingId = diskOfferingId;
            this.minIops = minIops;
            this.maxIops = maxIops;
        }

        public String getUuid() {
            return uuid;
        }

        public Volume.Type getType() {
            return type;
        }

        public void setType(Volume.Type type) {
            this.type = type;
        }

        public String getPath() {
            return path;
        }

        public Long getSize() {
            return size;
        }

        public Long getDeviceId() {
            return deviceId;
        }

        public String getDiskOfferingId() {
            return diskOfferingId;
        }

        public Long getMinIops() {
            return minIops;
        }

        public Long getMaxIops() {
            return maxIops;
        }

        @Override
        public String toString() {
            return StringUtils.join(":", uuid, path, type, size, deviceId, diskOfferingId, minIops, maxIops);
        }
    }

    Long getVmId();
    long getBackupOfferingId();
    String getExternalId();
    String getType();
    Date getDate();
    Backup.Status getStatus();
    Long getSize();
    Long getProtectedSize();
    void setName(String name);
    String getDescription();
    void setDescription(String description);
    List<VolumeInfo> getBackedUpVolumes();
    long getZoneId();
    Map<String, String> getDetails();
    String getDetail(String name);
    Long getBackupScheduleId();
}

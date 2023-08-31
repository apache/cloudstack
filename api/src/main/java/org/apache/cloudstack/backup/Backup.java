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

        public RestorePoint(String id, Date created, String type) {
            this.id = id;
            this.created = created;
            this.type = type;
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
    }

    class VolumeInfo {
        private String uuid;
        private Volume.Type type;
        private Long size;
        private String path;

        public VolumeInfo(String uuid, String path, Volume.Type type, Long size) {
            this.uuid = uuid;
            this.type = type;
            this.size = size;
            this.path = path;
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

        @Override
        public String toString() {
            return StringUtils.join(":", uuid, path, type, size);
        }
    }

    long getVmId();
    String getExternalId();
    String getType();
    Date getDate();
    Backup.Status getStatus();
    Long getSize();
    Long getProtectedSize();
    long getZoneId();
}

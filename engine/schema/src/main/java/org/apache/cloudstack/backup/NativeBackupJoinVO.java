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

package org.apache.cloudstack.backup;

import com.google.gson.Gson;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.commons.lang3.StringUtils;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "native_backup_view")
public class NativeBackupJoinVO {

    @Id
    @Column(name="id")
    private long id;

    @Column(name = "uuid")
    private String uuid;

    @Column(name = "vm_id")
    private long vmId;

    @Column(name = "backed_volumes", length = 65535)
    private String backedUpVolumes;

    @Column(name = "backup_offering_id")
    private long backupOfferingId;

    @Column(name = "image_store_id")
    private long imageStoreId;

    @Column(name = "parent_id")
    private long parentId;

    @Column(name = "type")
    private String type;

    @Column(name = "date")
    @Temporal(value = TemporalType.DATE)
    private Date date;

    @Enumerated(value = EnumType.STRING)
    @Column(name = "status")
    private Backup.Status status;

    @Enumerated(value = EnumType.STRING)
    @Column(name = "compression_status")
    private Backup.CompressionStatus compressionStatus;

    @Column(name = "end_of_chain")
    private Boolean endOfChain;

    @Column(name = "current")
    private Boolean current;

    @Column(name = "image_store_path")
    private String imageStorePath;

    @Column(name = "zone_id")
    private long zoneId;

    @Column(name = "size")
    private long size;

    @Column(name = "protected_size")
    private long protectedSize;

    @Column(name = "volume_id")
    private long volumeId;

    @Column(name = "isolated")
    private Boolean isolated;

    public NativeBackupJoinVO() {
    }

    public long getId() {
        return id;
    }

    public String getUuid() {
        return uuid;
    }

    public long getVmId() {
        return vmId;
    }

    public List<Backup.VolumeInfo> getBackedUpVolumes() {
        if (StringUtils.isEmpty(this.backedUpVolumes)) {
            return Collections.emptyList();
        }
        return Arrays.asList(new Gson().fromJson(this.backedUpVolumes, Backup.VolumeInfo[].class));
    }

    public long getBackupOfferingId() {
        return backupOfferingId;
    }

    public long getImageStoreId() {
        return imageStoreId;
    }

    public long getParentId() {
        return parentId;
    }

    public String getType() {
        return type;
    }

    public Date getDate() {
        return date;
    }

    public Backup.Status getStatus() {
        return status;
    }

    public Boolean getEndOfChain() {
        return BooleanUtils.isTrue(endOfChain);
    }

    public Boolean getCurrent() {
        return BooleanUtils.isTrue(current);
    }

    public String getImageStorePath() {
        return imageStorePath;
    }

    public long getZoneId() {
        return zoneId;
    }

    public long getSize() {
        return size;
    }

    public long getProtectedSize() {
        return protectedSize;
    }

    public long getVolumeId() {
        return volumeId;
    }

    public Boolean getIsolated() {
        return BooleanUtils.isTrue(isolated);
    }

    public Backup.CompressionStatus getCompressionStatus() {
        return compressionStatus;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.JSON_STYLE);
    }
}

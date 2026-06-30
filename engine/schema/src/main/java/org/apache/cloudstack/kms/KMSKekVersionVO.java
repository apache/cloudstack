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

package org.apache.cloudstack.kms;

import com.cloud.utils.db.GenericDao;
import org.apache.cloudstack.utils.reflectiontostringbuilderutils.ReflectionToStringBuilderUtils;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.util.Date;
import java.util.UUID;

/**
 * Database entity for KEK versions.
 * Tracks multiple KEK versions per KMS key to support gradual rotation.
 * During rotation, a new version is created (status=Active) and old versions
 * are marked as Previous (still usable for decryption) or Archived (no longer used).
 */
@Entity
@Table(name = "kms_kek_versions")
public class KMSKekVersionVO {

    public enum Status {
        /**
         * Used for new encryption operations
         */
        Active,
        /**
         * Still usable for decryption during key rotation
         */
        Previous,
        /**
         * No longer used; all wrapped keys have been re-encrypted
         */
        Archived
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "uuid", nullable = false)
    private String uuid;

    @Column(name = "kms_key_id", nullable = false)
    private Long kmsKeyId;

    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;

    @Column(name = "kek_label", nullable = false)
    private String kekLabel;

    @Column(name = "status", nullable = false, length = 32)
    @Enumerated(EnumType.STRING)
    private Status status;

    @Column(name = "hsm_profile_id")
    private Long hsmProfileId;

    @Column(name = GenericDao.CREATED_COLUMN, nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date created;

    @Column(name = GenericDao.REMOVED_COLUMN)
    @Temporal(TemporalType.TIMESTAMP)
    private Date removed;

    public KMSKekVersionVO(Long kmsKeyId, Integer versionNumber, String kekLabel) {
        this();
        this.kmsKeyId = kmsKeyId;
        this.versionNumber = versionNumber;
        this.kekLabel = kekLabel;
        this.status = Status.Active;
    }

    public KMSKekVersionVO(Long kmsKeyId, String kekLabel) {
        this();
        this.kmsKeyId = kmsKeyId;
        this.kekLabel = kekLabel;
        this.status = Status.Active;
    }

    public KMSKekVersionVO() {
        this.uuid = UUID.randomUUID().toString();
        this.created = new Date();
        this.status = Status.Active;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public Long getKmsKeyId() {
        return kmsKeyId;
    }

    public void setKmsKeyId(Long kmsKeyId) {
        this.kmsKeyId = kmsKeyId;
    }

    public Integer getVersionNumber() {
        return versionNumber;
    }

    public void setVersionNumber(Integer versionNumber) {
        this.versionNumber = versionNumber;
    }

    public String getKekLabel() {
        return kekLabel;
    }

    public void setKekLabel(String kekLabel) {
        this.kekLabel = kekLabel;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Long getHsmProfileId() {
        return hsmProfileId;
    }

    public void setHsmProfileId(Long hsmProfileId) {
        this.hsmProfileId = hsmProfileId;
    }


    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public Date getRemoved() {
        return removed;
    }

    public void setRemoved(Date removed) {
        this.removed = removed;
    }

    @Override
    public String toString() {
        return String.format("KMSKekVersion %s",
                ReflectionToStringBuilderUtils.reflectOnlySelectedFields(
                        this, "id", "uuid", "kmsKeyId", "versionNumber", "status", "kekLabel"));
    }
}

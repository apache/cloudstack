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
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.util.Arrays;
import java.util.Date;
import java.util.UUID;

/**
 * Database entity for storing wrapped (encrypted) Data Encryption Keys.
 * Each entry represents a DEK that has been encrypted by a Key Encryption Key (KEK).
 * KEK metadata is stored in kms_keys table via the kms_key_id foreign key.
 */
@Entity
@Table(name = "kms_wrapped_key")
public class KMSWrappedKeyVO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "uuid", nullable = false)
    private String uuid;

    @Column(name = "kms_key_id")
    private Long kmsKeyId;

    @Column(name = "kek_version_id")
    private Long kekVersionId;

    @Column(name = "zone_id", nullable = false)
    private Long zoneId;

    @Column(name = "wrapped_blob", nullable = false)
    private byte[] wrappedBlob;

    @Column(name = GenericDao.CREATED_COLUMN, nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date created;

    @Column(name = GenericDao.REMOVED_COLUMN)
    @Temporal(TemporalType.TIMESTAMP)
    private Date removed;

    public KMSWrappedKeyVO(KMSKeyVO kmsKey, byte[] wrappedBlob) {
        this();
        this.kmsKeyId = kmsKey.getId();
        this.zoneId = kmsKey.getZoneId();
        this.wrappedBlob = wrappedBlob != null ? Arrays.copyOf(wrappedBlob, wrappedBlob.length) : null;
    }

    public KMSWrappedKeyVO() {
        this.uuid = UUID.randomUUID().toString();
        this.created = new Date();
    }

    public KMSWrappedKeyVO(KMSKeyVO kmsKey, Long kekVersionId, byte[] wrappedBlob) {
        this();
        this.kmsKeyId = kmsKey.getId();
        this.kekVersionId = kekVersionId;
        this.zoneId = kmsKey.getZoneId();
        this.wrappedBlob = wrappedBlob != null ? Arrays.copyOf(wrappedBlob, wrappedBlob.length) : null;
    }

    public KMSWrappedKeyVO(Long kmsKeyId, Long zoneId, byte[] wrappedBlob) {
        this();
        this.kmsKeyId = kmsKeyId;
        this.zoneId = zoneId;
        this.wrappedBlob = wrappedBlob != null ? Arrays.copyOf(wrappedBlob, wrappedBlob.length) : null;
    }

    public KMSWrappedKeyVO(Long kmsKeyId, Long kekVersionId, Long zoneId, byte[] wrappedBlob) {
        this();
        this.kmsKeyId = kmsKeyId;
        this.kekVersionId = kekVersionId;
        this.zoneId = zoneId;
        this.wrappedBlob = wrappedBlob != null ? Arrays.copyOf(wrappedBlob, wrappedBlob.length) : null;
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

    public Long getKekVersionId() {
        return kekVersionId;
    }

    public void setKekVersionId(Long kekVersionId) {
        this.kekVersionId = kekVersionId;
    }

    public Long getZoneId() {
        return zoneId;
    }

    public void setZoneId(Long zoneId) {
        this.zoneId = zoneId;
    }

    public byte[] getWrappedBlob() {
        return wrappedBlob != null ? Arrays.copyOf(wrappedBlob, wrappedBlob.length) : null;
    }

    public void setWrappedBlob(byte[] wrappedBlob) {
        this.wrappedBlob = wrappedBlob != null ? Arrays.copyOf(wrappedBlob, wrappedBlob.length) : null;
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
        return String.format("KMSWrappedKey %s",
                ReflectionToStringBuilderUtils.reflectOnlySelectedFields(
                        this, "id", "uuid", "kmsKeyId", "kekVersionId", "accountId", "zoneId", "state", "created",
                        "removed"));
    }
}

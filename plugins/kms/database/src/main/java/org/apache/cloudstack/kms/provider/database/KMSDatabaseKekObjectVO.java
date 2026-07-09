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

package org.apache.cloudstack.kms.provider.database;

import com.cloud.utils.db.GenericDao;
import org.apache.cloudstack.framework.kms.KeyPurpose;
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
 * Database entity for KEK objects stored by the database KMS provider.
 * Models PKCS#11 object attributes for cryptographic key storage.
 * <p>
 * This table stores KEKs (Key Encryption Keys) in a PKCS#11-compatible format,
 * allowing the database provider to mock PKCS#11 interface behavior.
 */
@Entity
@Table(name = "kms_database_kek_objects")
public class KMSDatabaseKekObjectVO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "uuid", nullable = false)
    private String uuid;

    // PKCS#11 Object Class (CKA_CLASS)
    @Column(name = "object_class", nullable = false, length = 32)
    private String objectClass = "CKO_SECRET_KEY";

    // PKCS#11 Label (CKA_LABEL) - human-readable identifier
    @Column(name = "label", nullable = false, length = 255)
    private String label;

    // PKCS#11 ID (CKA_ID) - application-defined identifier
    @Column(name = "object_id", length = 64)
    private byte[] objectId;

    // PKCS#11 Key Type (CKA_KEY_TYPE)
    @Column(name = "key_type", nullable = false, length = 32)
    private String keyType = "CKK_AES";

    // PKCS#11 Key Value (CKA_VALUE) - encrypted KEK material
    @Column(name = "key_material", nullable = false, length = 512)
    private byte[] keyMaterial;

    // PKCS#11 Boolean Attributes
    @Column(name = "is_sensitive", nullable = false)
    private Boolean isSensitive = true;

    @Column(name = "is_extractable", nullable = false)
    private Boolean isExtractable = false;

    @Column(name = "is_token", nullable = false)
    private Boolean isToken = true;

    @Column(name = "is_private", nullable = false)
    private Boolean isPrivate = true;

    @Column(name = "is_modifiable", nullable = false)
    private Boolean isModifiable = false;

    @Column(name = "is_copyable", nullable = false)
    private Boolean isCopyable = false;

    @Column(name = "is_destroyable", nullable = false)
    private Boolean isDestroyable = true;

    @Column(name = "always_sensitive", nullable = false)
    private Boolean alwaysSensitive = true;

    @Column(name = "never_extractable", nullable = false)
    private Boolean neverExtractable = true;

    // Key Metadata
    @Column(name = "purpose", nullable = false, length = 32)
    @Enumerated(EnumType.STRING)
    private KeyPurpose purpose;

    @Column(name = "key_bits", nullable = false)
    private Integer keyBits;

    @Column(name = "algorithm", nullable = false, length = 64)
    private String algorithm = "AES/GCM/NoPadding";

    // PKCS#11 Validity Dates
    @Column(name = "start_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date startDate;

    @Column(name = "end_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date endDate;

    // Lifecycle
    @Column(name = GenericDao.CREATED_COLUMN, nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date created;

    @Column(name = "last_used")
    @Temporal(TemporalType.TIMESTAMP)
    private Date lastUsed;

    @Column(name = GenericDao.REMOVED_COLUMN)
    @Temporal(TemporalType.TIMESTAMP)
    private Date removed;

    /**
     * Constructor for creating a new KEK object
     *
     * @param label       PKCS#11 label (CKA_LABEL)
     * @param purpose     key purpose
     * @param keyBits     key size in bits
     * @param keyMaterial encrypted key material (CKA_VALUE)
     */
    public KMSDatabaseKekObjectVO(String label, KeyPurpose purpose, Integer keyBits, byte[] keyMaterial) {
        this();
        this.label = label;
        this.purpose = purpose;
        this.keyBits = keyBits;
        this.keyMaterial = keyMaterial;
        this.objectId = label.getBytes(); // Use label as object ID by default
        this.startDate = new Date();
    }

    public KMSDatabaseKekObjectVO() {
        this.uuid = UUID.randomUUID().toString();
        this.created = new Date();
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

    public String getObjectClass() {
        return objectClass;
    }

    public void setObjectClass(String objectClass) {
        this.objectClass = objectClass;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public byte[] getObjectId() {
        return objectId;
    }

    public void setObjectId(byte[] objectId) {
        this.objectId = objectId;
    }

    public String getKeyType() {
        return keyType;
    }

    public void setKeyType(String keyType) {
        this.keyType = keyType;
    }

    public byte[] getKeyMaterial() {
        return keyMaterial;
    }

    public void setKeyMaterial(byte[] keyMaterial) {
        this.keyMaterial = keyMaterial;
    }

    public Boolean getIsSensitive() {
        return isSensitive;
    }

    public void setIsSensitive(Boolean isSensitive) {
        this.isSensitive = isSensitive;
    }

    public Boolean getIsExtractable() {
        return isExtractable;
    }

    public void setIsExtractable(Boolean isExtractable) {
        this.isExtractable = isExtractable;
    }

    public Boolean getIsToken() {
        return isToken;
    }

    public void setIsToken(Boolean isToken) {
        this.isToken = isToken;
    }

    public Boolean getIsPrivate() {
        return isPrivate;
    }

    public void setIsPrivate(Boolean isPrivate) {
        this.isPrivate = isPrivate;
    }

    public Boolean getIsModifiable() {
        return isModifiable;
    }

    public void setIsModifiable(Boolean isModifiable) {
        this.isModifiable = isModifiable;
    }

    public Boolean getIsCopyable() {
        return isCopyable;
    }

    public void setIsCopyable(Boolean isCopyable) {
        this.isCopyable = isCopyable;
    }

    public Boolean getIsDestroyable() {
        return isDestroyable;
    }

    public void setIsDestroyable(Boolean isDestroyable) {
        this.isDestroyable = isDestroyable;
    }

    public Boolean getAlwaysSensitive() {
        return alwaysSensitive;
    }

    public void setAlwaysSensitive(Boolean alwaysSensitive) {
        this.alwaysSensitive = alwaysSensitive;
    }

    public Boolean getNeverExtractable() {
        return neverExtractable;
    }

    public void setNeverExtractable(Boolean neverExtractable) {
        this.neverExtractable = neverExtractable;
    }

    public KeyPurpose getPurpose() {
        return purpose;
    }

    public void setPurpose(KeyPurpose purpose) {
        this.purpose = purpose;
    }

    public Integer getKeyBits() {
        return keyBits;
    }

    public void setKeyBits(Integer keyBits) {
        this.keyBits = keyBits;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public Date getLastUsed() {
        return lastUsed;
    }

    public void setLastUsed(Date lastUsed) {
        this.lastUsed = lastUsed;
    }

    public Date getRemoved() {
        return removed;
    }

    public void setRemoved(Date removed) {
        this.removed = removed;
    }

    @Override
    public String toString() {
        return String.format("KMSDatabaseKekObject %s", ReflectionToStringBuilderUtils.reflectOnlySelectedFields(
                this, "id", "uuid", "label", "purpose", "keyBits", "objectClass", "keyType", "algorithm"));
    }
}

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
 * Database entity for KMS Key (Key Encryption Key) metadata.
 * Tracks ownership, purpose, and lifecycle of KEKs used in envelope encryption.
 */
@Entity
@Table(name = "kms_keys")
public class KMSKeyVO implements KMSKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "uuid", nullable = false, unique = true)
    private String uuid;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", length = 1024)
    private String description;

    @Column(name = "kek_label", nullable = false)
    private String kekLabel;

    @Column(name = "purpose", nullable = false, length = 32)
    @Enumerated(EnumType.STRING)
    private KeyPurpose purpose;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "domain_id", nullable = false)
    private Long domainId;

    @Column(name = "zone_id", nullable = false)
    private Long zoneId;

    @Column(name = "provider_name", nullable = false, length = 64)
    private String providerName;

    @Column(name = "algorithm", nullable = false, length = 64)
    private String algorithm;

    @Column(name = "key_bits", nullable = false)
    private Integer keyBits;

    @Column(name = "state", nullable = false, length = 32)
    @Enumerated(EnumType.STRING)
    private State state;

    @Column(name = "hsm_profile_id")
    private Long hsmProfileId;

    @Column(name = GenericDao.CREATED_COLUMN, nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date created;

    @Column(name = GenericDao.REMOVED_COLUMN)
    @Temporal(TemporalType.TIMESTAMP)
    private Date removed;

    public KMSKeyVO() {
        this.uuid = UUID.randomUUID().toString();
        this.created = new Date();
        this.state = State.Enabled;
    }

    public KMSKeyVO(String name, String description, String kekLabel, KeyPurpose purpose,
                    Long accountId, Long domainId, Long zoneId, String providerName,
                    String algorithm, Integer keyBits) {
        this();
        this.name = name;
        this.description = description;
        this.kekLabel = kekLabel;
        this.purpose = purpose;
        this.accountId = accountId;
        this.domainId = domainId;
        this.zoneId = zoneId;
        this.providerName = providerName;
        this.algorithm = algorithm;
        this.keyBits = keyBits;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public String getUuid() {
        return uuid;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getKekLabel() {
        return kekLabel;
    }

    @Override
    public KeyPurpose getPurpose() {
        return purpose;
    }

    @Override
    public Long getZoneId() {
        return zoneId;
    }

    @Override
    public String getProviderName() {
        return providerName;
    }

    @Override
    public String getAlgorithm() {
        return algorithm;
    }

    @Override
    public Integer getKeyBits() {
        return keyBits;
    }

    @Override
    public State getState() {
        return state;
    }

    @Override
    public Date getCreated() {
        return created;
    }

    @Override
    public Date getRemoved() {
        return removed;
    }

    // ControlledEntity interface methods

    @Override
    public long getAccountId() {
        return accountId;
    }

    @Override
    public long getDomainId() {
        return domainId;
    }

    @Override
    public Class<?> getEntityType() {
        return KMSKey.class;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setKekLabel(String kekLabel) {
        this.kekLabel = kekLabel;
    }

    public void setPurpose(KeyPurpose purpose) {
        this.purpose = purpose;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public void setDomainId(Long domainId) {
        this.domainId = domainId;
    }

    public void setZoneId(Long zoneId) {
        this.zoneId = zoneId;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public void setKeyBits(Integer keyBits) {
        this.keyBits = keyBits;
    }

    public void setState(State state) {
        this.state = state;
    }

    public Long getHsmProfileId() {
        return hsmProfileId;
    }

    public void setHsmProfileId(Long hsmProfileId) {
        this.hsmProfileId = hsmProfileId;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public void setRemoved(Date removed) {
        this.removed = removed;
    }

    @Override
    public String toString() {
        return String.format("KMSKey %s",
                ReflectionToStringBuilderUtils.reflectOnlySelectedFields(this, "id", "uuid", "name", "purpose", "accountId", "zoneId", "state"));
    }
}

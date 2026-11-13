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
package com.cloud.storage;

import com.cloud.utils.db.Encrypt;
import com.cloud.utils.db.GenericDao;
import org.apache.cloudstack.storage.object.BucketCredentialKey;

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

@Entity
@Table(name = "bucket_credential_key")
public class BucketCredentialKeyVO implements BucketCredentialKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "uuid")
    private String uuid;

    @Column(name = "bucket_credential_id")
    private long bucketCredentialId;

    @Column(name = "key_slot")
    private int keySlot;

    @Column(name = "access_key")
    private String accessKey;

    @Encrypt
    @Column(name = "secret_key")
    private String secretKey;

    @Column(name = "state", updatable = true, nullable = false)
    @Enumerated(value = EnumType.STRING)
    private State state;

    @Column(name = GenericDao.CREATED_COLUMN)
    private Date created;

    @Column(name = "last_used")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date lastUsed;

    @Column(name = GenericDao.REMOVED_COLUMN)
    private Date removed;

    public BucketCredentialKeyVO() {
        this.uuid = UUID.randomUUID().toString();
    }

    public BucketCredentialKeyVO(long bucketCredentialId, int keySlot, String accessKey, String secretKey) {
        this.uuid = UUID.randomUUID().toString();
        this.bucketCredentialId = bucketCredentialId;
        this.keySlot = keySlot;
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.state = State.Active;
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
    public long getBucketCredentialId() {
        return bucketCredentialId;
    }

    @Override
    public int getKeySlot() {
        return keySlot;
    }

    @Override
    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    @Override
    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    @Override
    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    @Override
    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    @Override
    public Date getLastUsed() {
        return lastUsed;
    }

    public void setLastUsed(Date lastUsed) {
        this.lastUsed = lastUsed;
    }

    public Date getRemoved() {
        return removed;
    }
}

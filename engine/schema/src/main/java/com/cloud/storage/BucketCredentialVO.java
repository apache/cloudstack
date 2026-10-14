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

import com.cloud.utils.db.GenericDao;
import org.apache.cloudstack.storage.object.BucketCredential;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Date;
import java.util.UUID;

@Entity
@Table(name = "bucket_credential")
public class BucketCredentialVO implements BucketCredential {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "uuid")
    private String uuid;

    @Column(name = "bucket_id")
    private long bucketId;

    @Column(name = "provider_credential_id")
    private String providerCredentialId;

    @Column(name = "state", updatable = true, nullable = false)
    @Enumerated(value = EnumType.STRING)
    private State state;

    @Column(name = GenericDao.CREATED_COLUMN)
    private Date created;

    @Column(name = GenericDao.REMOVED_COLUMN)
    private Date removed;

    public BucketCredentialVO() {
        this.uuid = UUID.randomUUID().toString();
    }

    public BucketCredentialVO(long bucketId, String providerCredentialId) {
        this.uuid = UUID.randomUUID().toString();
        this.bucketId = bucketId;
        this.providerCredentialId = providerCredentialId;
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
    public long getBucketId() {
        return bucketId;
    }

    @Override
    public String getProviderCredentialId() {
        return providerCredentialId;
    }

    public void setProviderCredentialId(String providerCredentialId) {
        this.providerCredentialId = providerCredentialId;
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

    public Date getRemoved() {
        return removed;
    }
}
